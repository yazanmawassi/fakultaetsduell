package service

import entity.CardSuit
import entity.CardValue
import entity.Faculty
import entity.FacultyGame
import entity.ResearchCard

/**
 * Service for starting a game, changing turns and ending the game.
 */
class GameService(
    private val rootService: RootService
) : AbstractRefreshingService() {

    /**
     * Starts a new game with 2 to 4 faculties.
     *
     * @param facultyNames Unique, non-blank faculty names in the entered order.
     * @param randomOrder If true, the order of the faculties is shuffled;
     *                    otherwise the entered order is kept.
     * @throws IllegalArgumentException if the names are invalid
     */
    fun startNewGame(facultyNames: List<String>, randomOrder: Boolean = true) {
        require(facultyNames.size in MIN_FACULTIES..MAX_FACULTIES) {
            "Es müssen $MIN_FACULTIES bis $MAX_FACULTIES Fakultäten sein."
        }
        require(facultyNames.none { it.isBlank() }) { "Namen dürfen nicht leer sein." }
        require(facultyNames.distinct().size == facultyNames.size) { "Namen müssen eindeutig sein." }

        val names = if (randomOrder) facultyNames.shuffled() else facultyNames
        val game = FacultyGame(names.map { Faculty(it) })

        game.drawStack.addAll(createDrawStack())
        game.faculties.forEach { faculty ->
            repeat(START_HAND_SIZE) { faculty.handCards.add(game.drawStack.removeLast()) }
        }
        repeat(START_LITERATURE_SIZE) { game.centralLiterature.add(game.drawStack.removeLast()) }

        val first = game.faculties[game.facultyAtTurn]
        first.resetActionPoints()
        rootService.currentGame = game

        onAllRefreshables { refreshAfterStartGame() }
        //onAllRefreshables { refreshAfterLog("Spiel gestartet. ${first.name} beginnt.") }
    }

    /**
     * Ends the current faculty's turn.
     * Starts the next turn or finishes the game.
     *
     * @throws IllegalStateException if no game is running or the game is already over
     */
    fun endTurn() {
        val game = checkNotNull(rootService.currentGame) { "Es läuft kein Spiel." }
        check(game.endgameTurnsLeft != 0) { "Das Spiel ist bereits beendet." }
        check(!rootService.playerActionService.hasPossibleAction()) {
            "Der Zug kann erst beendet werden, wenn keine Aktion mehr möglich ist."
        }
        updateEndgameCounter(game)
        if (isGameOver(game)) {
            endGame()
            return
        }

        game.nextFaculty()
        val next = game.faculties[game.facultyAtTurn]
        next.resetActionPoints()

        onAllRefreshables { refreshAfterTurnChange(next) }
        //onAllRefreshables { refreshAfterLog("${next.name} ist am Zug.") }
        // Verklemmung: the new faculty cannot act at all, so it is skipped.
        if (!rootService.playerActionService.hasPossibleAction()) {
            onAllRefreshables { refreshAfterLog("${next.name} kann keine Aktion ausführen und wird übersprungen.") }
            endTurn()
        }
    }

    /** Creates all 52 cards in randomized order. */
    private fun createDrawStack(): List<ResearchCard> =
        CardSuit.entries.flatMap { suit ->
            CardValue.entries.map { value -> ResearchCard(suit, value) }
        }.shuffled()

    /**
     * Starts and updates the endgame countdown.
     * -1: endgame has not started, >0: remaining turns, 0: game has ended.
     * Rule: the game ends when the draw stack is empty and every other faculty had one more turn.
     */
    private fun updateEndgameCounter(game: FacultyGame) {
        if (game.endgameTurnsLeft == -1 && game.drawStack.isEmpty()) {
            game.endgameTurnsLeft = game.faculties.size - 1
            onAllRefreshables { refreshAfterLog("Nachziehstapel leer – letzte Runde beginnt.") }
        } else if (game.endgameTurnsLeft > 0) {
            game.endgameTurnsLeft--
        }
    }

    /** Returns true when the final turn has ended. */
    private fun isGameOver(game: FacultyGame): Boolean = game.endgameTurnsLeft == 0

    /** Calculates final scores and informs the GUI. */
    private fun endGame() {
        val scores = calculateFinalScores()
        onAllRefreshables { refreshAfterLog("Spiel beendet. ${scores[0].faculty.name} gewinnt.") }
        onAllRefreshables { refreshAfterGameEnd(scores) }
    }

    /**
     * Calculates the final score of every faculty.
     * Faculties are sorted by total score, highest first.
     */
    private fun calculateFinalScores(): List<FinalScore> {
        val game = checkNotNull(rootService.currentGame)
        val publishedCounts = game.faculties.associateWith { countPublicationsPerSuit(it) }

        return game.faculties.map { faculty ->
            val counts = publishedCounts.getValue(faculty)
            val publicationPoints = faculty.researchPoints
            val majorityBonus = majorityBonus(faculty, publishedCounts)
            val breadthBonus = if (counts.values.all { it > 0 }) BREADTH_BONUS else 0
            val focusBonus = if (counts.values.any { it >= FOCUS_MIN_CARDS }) FOCUS_BONUS else 0
            val unpublishedPoints = faculty.researchActivity.values.sumOf { it.size }

            FinalScore(
                faculty = faculty,
                publicationPoints = publicationPoints,
                majorityBonus = majorityBonus,
                breadthBonus = breadthBonus,
                focusBonus = focusBonus,
                unpublishedPoints = unpublishedPoints,
                total = publicationPoints + majorityBonus + breadthBonus + focusBonus + unpublishedPoints
            )
        }.sortedByDescending { it.total }
    }

    /** Counts the published cards of [faculty] for every suit. */
    private fun countPublicationsPerSuit(faculty: Faculty): Map<CardSuit, Int> =
        CardSuit.entries.associateWith { suit ->
            faculty.publications.count { it.suit == suit }
        }

    /**
     * Majority bonus per suit: [MAJORITY_FIRST] for the most published cards,
     * [MAJORITY_SECOND] for the second most. Equal counts share the place
     * (e.g. A=3, B=3, C=2 → A +5, B +5, C +2).
     */
    private fun majorityBonus(
        faculty: Faculty,
        publishedCounts: Map<Faculty, Map<CardSuit, Int>>
    ): Int = CardSuit.entries.sumOf { suit ->
        val ownCount = publishedCounts.getValue(faculty).getValue(suit)
        val ranking = publishedCounts.values
            .map { it.getValue(suit) }
            .filter { it > 0 }
            .distinct()
            .sortedDescending()

        when {
            ownCount == 0 -> 0
            ownCount == ranking.getOrNull(0) -> MAJORITY_FIRST
            ownCount == ranking.getOrNull(1) -> MAJORITY_SECOND
            else -> 0
        }
    }

    private companion object {
        const val MIN_FACULTIES = 2
        const val MAX_FACULTIES = 4
        const val START_HAND_SIZE = 5
        const val START_LITERATURE_SIZE = 6

        const val MAJORITY_FIRST = 5
        const val MAJORITY_SECOND = 2
        const val BREADTH_BONUS = 3
        const val FOCUS_MIN_CARDS = 8
        const val FOCUS_BONUS = 4
    }
}