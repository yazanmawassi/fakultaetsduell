package service

import entity.CardSuit
import entity.Faculty
import entity.ResearchCard

/**
 * Service for all actions the active faculty can do in its turn.
 */
// TEMPORÄR: // Nur für Detekt-Warnungen für leere Methoden unterdrücken , damit ich die Klassen in Service Branch puschen kann , wir müssen das später löschen.
@Suppress("UnusedParameter", "UnusedPrivateProperty", "NotImplementedDeclaration")
class PlayerActionService(private val rootService: RootService) : AbstractRefreshingService() {

    /** Places [card] from the hand on the matching research stack ("Forschung vorantreiben"). */
    fun research(card: ResearchCard) {
    val game = checkNotNull(rootService.currentGame) {
        "Es läuft kein Spiel."
    }

    check(game.endgameTurnsLeft != 0) {
        "Das Spiel ist bereits beendet."
    }

    val faculty = game.faculties[game.facultyAtTurn]

    check(faculty.actionPoints >= 1) {
        "Nicht genügend Aktionspunkte."
    }

    require(card in faculty.handCards) {
        "Die Karte befindet sich nicht auf der Hand."
    }

    // Handkarte auf den passenden Forschungsstapel legen
    faculty.handCards.remove(card)
    faculty.addResearch(card)

    // Eine Karte zur zentralen Literatur hinzufügen
    if (game.drawStack.isNotEmpty()) {
        if (game.centralLiterature.size >= 6) {
            val oldestCard = game.centralLiterature.removeAt(0)
            game.discardStack.add(oldestCard)
        }

        val drawnCard = game.drawStack.removeLast()
        game.centralLiterature.add(drawnCard)
    }

    faculty.actionPoints -= 1

    onAllRefreshables {
        refreshAfterResearch(faculty)
    }

    onAllRefreshables {
        refreshAfterLog("${faculty.name} treibt Forschung voran.")
    }
}

    /**
     * Draws one card from the common draw stack and adds it to the active faculty's hand ("Neue Ideen gewinnen").
     * This operation consumes exactly 1 Action Point (AP) from the current player.
     *
     * This method strictly follows the mandatory 3-phase execution model:
     * 1. **Phase 1 (Prüfen):** Validates that a game is running, the active player has enough AP,
     *    the hand cards limit is not exceeded, and cards are available in the deck.
     * 2. **Phase 2 (Ausführen):** Modifies the game state by transferring the card and deducting the AP.
     * 3. **Phase 3 (Aktualisieren):** Triggers the user interface components to repaint the screen.
     * @throws IllegalStateException If no game is currently active ([RootService.currentGame] is null)
     *         or if the drawStack is completely empty.
     * @throws IllegalArgumentException If the active faculty has fewer than 1 [Faculty.actionPoints],
     *         or if the faculty has already reached the maximum hand cards limit of 6 cards.
     */

    fun drawIdea() {

        val game = checkNotNull(rootService.currentGame) {
            "Systemfehler: Es läuft aktuell kein aktives Spiel!"
        }

        check(game.endgameTurnsLeft != 0) {
            "Aktion nicht erlaubt: Das Spiel ist bereits beendet."
        }
        val activeFaculty = game.faculties[game.facultyAtTurn]

        // Validate that the current player has at least 1 Action Point left
        require(activeFaculty.actionPoints >= 1) {
            "Aktion nicht erlaubt: Du hast keine Aktionspunkte (AP) mehr übrig!"
        }

        require(activeFaculty.handCards.size < 6) {" Aktion nicht erlaubt: Das Handkartenlimit muss < 6 " }
        // Ensure that there is at least one card remaining in the shared draw stack
        check(game.drawStack.isNotEmpty()) {
            "Systemfehler: Der Nachziehstapel (drawStack) ist leer! Es können keine Ideen mehr gewonnen werden."
        }

        // 1. remove the top card off the mutable list representation of the deck
        val drawnCard = game.drawStack.removeAt(game.drawStack.lastIndex)

        // 2. Insert the popped card into the player's secret hand cards collection
        activeFaculty.handCards.add(drawnCard)

        // 3. Subtract exactly 1 action point as the operational cost
        activeFaculty.actionPoints -= 1

        onAllRefreshables {
            refreshAfterDrawIdea(activeFaculty)
        }

    }

    /** First step of "Strategie anpassen": discards [discard] and draws two cards. */
    fun startStrategy(discard: ResearchCard) {

        // 1. Get current game
        val game = rootService.currentGame
            ?: throw IllegalStateException("No active game")

        // 2. Get active faculty
        val currentFaculty = game.faculties[game.facultyAtTurn]

        // 3. Check action points
        require(currentFaculty.actionPoints >= 1) {
            "Not enough action points"
        }

        // 4. Check whether card belongs to player
        require(currentFaculty.handCards.any { it === discard }) {
            "Card is not in player's hand"
        }

        // 5. Check draw stack
        require(game.drawStack.size >= 2) {
            "Not enough cards in draw stack"
        }

        // 6. Prevent starting another strategy
        require(game.pendingStrategyCards.isEmpty()) {
            "Strategy already in progress"
        }

        // 7. Remove card from player's hand
        currentFaculty.handCards.remove(discard)

        // 8. Add discarded card to discard stack
        game.discardStack.add(discard)

        // 9. Draw two cards
        val firstCard = game.drawStack.removeAt(
            game.drawStack.lastIndex
        )

        val secondCard = game.drawStack.removeAt(
            game.drawStack.lastIndex
        )

        val drawnCards = listOf(firstCard, secondCard)

        // 10. Store cards until player chooses
        game.pendingStrategyCards.addAll(drawnCards)

        // 11. Deduct 1 action point
        currentFaculty.actionPoints -= 1

        // 12. Notify GUI
        onAllRefreshables {
            refreshAfterStrategyStart(drawnCards)
        }
    }

    /** Second step of "Strategie anpassen": [keep] goes to the hand, the other card is discarded. */

    fun finishStrategy(keep: ResearchCard) {

        // 1. Get current game
        val game = rootService.currentGame
            ?: throw IllegalStateException("No active game")

        // 2. Get active player
        val currentFaculty = game.faculties[game.facultyAtTurn]

        // 3. Check if strategy is in progress
        require(game.pendingStrategyCards.size == 2) {
            "No strategy in progress"
        }

        // 4. Check selected card
        require(game.pendingStrategyCards.any { it === keep }) {
            "Selected card is not one of the drawn cards"
        }

        // 5. Find the other card
        val discard = game.pendingStrategyCards.first {
            it !== keep
        }

        // 6. Keep selected card
        currentFaculty.handCards.add(keep)

        // 7. Discard other card
        game.discardStack.add(discard)

        // 8. Clear temporary cards
        game.pendingStrategyCards.clear()

        // 9. Update GUI
        onAllRefreshables {
            refreshAfterStrategyEnd(currentFaculty)
        }
    }




    /**
     * Publishes the active research stack of a specific [CardSuit] ("Publizieren").
     *
     * This operation secures the research cards from opponents' sabotage, awards research points
     * based on the stack size, checks for the first-to-publish bonus, and deducts exactly 2 Action Points (AP).
     *
     * This method strictly complies with the university's 3-phase execution model:
     * 1. **Phase 1 (Prüfen):** Validates the active game state and ensures the player has at least 2 AP
     *    and the selected research stack contains at least 2 cards.
     * 2. **Phase 2 (Ausführen):** Awards calculated points to the active faculty, updates the entity state
     *    by moving all cards to the publications list, and deducts the operational cost (2 AP).
     * 3. **Phase 3 (Aktualisieren):** Notifies all registered UI observers via the refresh interface.
     *
     * @param area The [CardSuit] (e.g., HEARTS, SPADES) of the research stack to be published.
     * @throws IllegalStateException If no game is active or if the game has already ended.
     * @throws IllegalArgumentException If the active faculty has fewer than 2 AP, or if the chosen
     *         research stack contains fewer than 2 cards.
     */
    fun publish(area: CardSuit) {


        val game = checkNotNull(rootService.currentGame) {
            "Systemfehler: Es läuft aktuell kein aktives Spiel!"
        }
        check(game.endgameTurnsLeft != 0) {
            "Das Spiel ist bereits beendet."
        }
        val activeFaculty = game.faculties[game.facultyAtTurn]

        require(activeFaculty.actionPoints >= 2)
        { "Fehler: Nicht genug Aktionspunkte !" }

        val researchStack = activeFaculty.researchActivity[area]
        require(researchStack != null && researchStack.size >= 2)
        { "Fehler: Es müssen mindestens 2 Karten auf dem Forschungsstapel liegen," +
                "um zu veröffentlichen!"
        }

        // 1. Remember the number of cards on the stack
        val cardCount = researchStack.size

        // 2. Calculate points based on the card count [2=3 Pkt, 3=5 Pkt, 4=7 Pkt, 5+=9 Pkt]
        val earnedPoints = when {
            cardCount == 2 -> 3
            cardCount == 3 -> 5
            cardCount == 4 -> 7
            cardCount >= 5 -> 9
            else -> 0
        }
        activeFaculty.researchPoints += earnedPoints

        // 3. Check for First-to-Publish bonus (+1 point if no one else has published in this suit yet)
        val alreadyPublished = game.faculties.any { faculty ->
            faculty.publications.any { card -> card.suit == area }
        }

        // If it is the first publication in this area, award +1 bonus point
        if (!alreadyPublished) {
            activeFaculty.researchPoints += 1
        }

        // 4. Move ALL cards from this research stack into the publications list
        activeFaculty.publications.addAll(researchStack)

        // 5. Clear the active research stack
        researchStack.clear()


        // 6. Deduct exactly 2 action points
        activeFaculty.actionPoints -= 2

        // 7. Phase 3: Notify the user interface with the calculated total points
        onAllRefreshables {
            refreshAfterPublish(activeFaculty, area, earnedPoints)
        }


    }
    /** Removes the top card of [target]'s stack in [area] ("Konkurrenz ausbremsen"). */

    /**
     * Konkurrenz ausbremsen (2 AP).
     * Removes the top research card of another faculty.
     */
    fun sabotage(target: Faculty, area: CardSuit) {

        // 1. Get current game
        val game = rootService.currentGame
            ?: throw IllegalStateException("No active game")

        // 2. Get active faculty
        val currentFaculty = game.faculties[game.facultyAtTurn]

        // 3. Check action points
        require(currentFaculty.actionPoints >= 2) {
            "Not enough action points"
        }

        // 4. Prevent self-sabotage
        require(target !== currentFaculty) {
            "Cannot sabotage yourself"
        }

        // 5. Check target faculty
        require(game.faculties.any { it === target }) {
            "Target faculty is not part of the game"
        }

        // 6. Get target's research stack
        val stack = target.researchActivity[area]
            ?: throw IllegalStateException("Research stack not found")

        // 7. Check whether stack is empty
        require(stack.isNotEmpty()) {
            "Research stack is empty"
        }

        // 8. Remove the top card
        val removedCard = stack.pop()

        // 9. Add card to discard stack
        game.discardStack.add(removedCard)

        // 10. Deduct 2 action points
        currentFaculty.actionPoints -= 2

        // 11. Notify GUI
        onAllRefreshables {
            refreshAfterSabotage(target, area)
        }
    }

    /**
     * Executes the "Literatur nutzen" (Use Literature) action for the active faculty.
     *
     * The active faculty takes a specific [card] from the central literature area and places it
     * directly onto their matching research stack. This action consumes exactly 2 Action Points (AP).
     *
     * @param card The [ResearchCard] to be removed from the central literature and added to the faculty's research.
     * @throws IllegalStateException if no active game is running, if the game has already ended,
     *         or if the active faculty has fewer than 2 action points.
     * @throws IllegalArgumentException if the specified [card] is not currently present in the central literature.
     */
    fun useLiterature(card: ResearchCard) {
        val game = checkNotNull(rootService.currentGame) {
            "Es läuft kein Spiel."
        }

        check(game.endgameTurnsLeft != 0) {
            "Das Spiel ist bereits beendet."
        }

        val faculty = game.faculties[game.facultyAtTurn]

        check(faculty.actionPoints >= 2) {
            "Nicht genügend Aktionspunkte. Literatur nutzen kostet 2 AP."
        }

        require(card in game.centralLiterature) {
            "Die gewählte Karte befindet sich nicht in der zentralen Literatur."
        }

        game.centralLiterature.remove(card)
        faculty.addResearch(card)
        faculty.actionPoints -= 2

        onAllRefreshables {
            refreshAfterLog("${faculty.name} nutzt die zentrale Literatur.")
        }
    }
    /**
     * Checks whether the active faculty can still perform at least one action.
     *
     * Used to end a turn automatically and to detect a deadlock in the last round
     * (action points left, but no action possible).
     *
     * @return true if at least one action is allowed, false otherwise
     * @throws IllegalStateException if no game is running
     */
    fun hasPossibleAction(): Boolean {
        val game = checkNotNull(rootService.currentGame) { "Es läuft kein Spiel." }

        // A started strategy must be finished first, so an action is still possible.
        if (game.pendingStrategyCards.isNotEmpty()) return true

        val faculty = game.faculties[game.facultyAtTurn]
        val ap = faculty.actionPoints
        val hand = faculty.handCards

        // 1 AP
        val canResearch = ap >= 1 && hand.isNotEmpty()
        val canDrawIdea = ap >= 1 && game.drawStack.isNotEmpty() && hand.size <= MAX_HAND_BEFORE_DRAW
        val canAdjustStrategy = ap >= 1 && hand.isNotEmpty() && game.drawStack.size >= 2

        // 2 AP
        val canPublish = ap >= 2 && faculty.researchActivity.values.any { it.size >= 2 }
        val canSabotage = ap >= 2 && game.faculties.any { other ->
            other !== faculty && other.researchActivity.values.any { it.isNotEmpty() }
        }
        val canUseLiterature = ap >= 2 && game.centralLiterature.isNotEmpty()

        return canResearch || canDrawIdea || canAdjustStrategy ||
                canPublish || canSabotage || canUseLiterature
    }

    private companion object {
        /** "Neue Ideen" is only allowed with at most 5 hand cards (hand limit 6). */
        const val MAX_HAND_BEFORE_DRAW = 5
    }
}