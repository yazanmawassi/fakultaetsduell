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
     * Starts a new game with 2 to 4 faculties in random order.
     *
     * @param facultyNames Unique, non-blank faculty names.
     * @throws IllegalArgumentException if the names are invalid
     */
    fun startNewGame(facultyNames: List<String>) {
        require(facultyNames.size in MIN_FACULTIES..MAX_FACULTIES) {
            "Es müssen $MIN_FACULTIES bis $MAX_FACULTIES Fakultäten sein."
        }
        require(facultyNames.none { it.isBlank() }) { "Namen dürfen nicht leer sein." }
        require(facultyNames.distinct().size == facultyNames.size) { "Namen müssen eindeutig sein." }

        val game = FacultyGame(facultyNames.shuffled().map { Faculty(it) })

        game.drawStack.addAll(createDrawStack())
        game.faculties.forEach { faculty ->
            repeat(START_HAND_SIZE) { faculty.handCards.add(game.drawStack.removeLast()) }
        }
        repeat(START_LITERATURE_SIZE) { game.centralLiterature.add(game.drawStack.removeLast()) }

        val first = game.faculties[game.facultyAtTurn]
        first.resetActionPoints()
        rootService.currentGame = game

        onAllRefreshables { refreshAfterStartGame() }
        onAllRefreshables { refreshAfterLog("Spiel gestartet. ${first.name} beginnt.") }
    }

    /**
     * Ends the current faculty's turn , and Starts the next turn or finishes the game.
     * @throws IllegalStateException if no game is running or the game is already over
     */
    fun endTurn() {
        val game = checkNotNull(rootService.currentGame) { "Es läuft kein Spiel." }
        check(game.endgameTurnsLeft != 0) { "Das Spiel ist bereits beendet." }

        updateEndgameCounter(game)
        if (isGameOver(game)) {
            endGame()
            return
        }

        game.nextFaculty()
        val next = game.faculties[game.facultyAtTurn]
        next.resetActionPoints()

        onAllRefreshables { refreshAfterTurnChange(next) }
        onAllRefreshables { refreshAfterLog("${next.name} ist am Zug.") }
    }

    /** Creates all 52 cards in randomized order. */
    private fun createDrawStack(): List<ResearchCard> =
        CardSuit.entries.flatMap { suit ->
            CardValue.entries.map { value -> ResearchCard(suit, value) }
        }.shuffled()

    /**
     * -1: endgame has not started, >0: remaining turns, 0: game has ended.
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
    * Calculates the cumulative majority bonus points for a single [Faculty] across all available research topics.
    *
    * Iterates through every existing [CardSuit] to determine if the given faculty holds either the
    * highest (first place) or the second-highest (second place) publication volume globally compared to competitors.
    * Only counts active non-zero publications toward the ranking thresholds.
    *
    * @param faculty The [Faculty] instance whose majority bonuses are being evaluated.
    * @param publishedCounts A pre-calculated mapping containing the total number of published cards categorized
    *        by each [Faculty] and their corresponding [CardSuit] counts.
    * @return The total aggregated integer score earned from first and second-place majority achievements.
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

    /**
     * Internal structural settings and mathematical constants governing game constraints and end-game scoring evaluation.
     */
    private companion object {
        const val MIN_FACULTIES = 2
        const val MAX_FACULTIES = 4
        const val START_HAND_SIZE = 5
        const val START_LITERATURE_SIZE = 6

        const val MAJORITY_FIRST = 5
        const val MAJORITY_SECOND = 2
        const val BREADTH_BONUS = 3
        const val FOCUS_BONUS = 4
        const val FOCUS_MIN_CARDS = 8
    }
}