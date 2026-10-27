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

        // 1. Pop the top card off the mutable list representation of the deck
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
    fun startStrategy(discard: ResearchCard) { TODO() }

    /** Second step of "Strategie anpassen": [keep] goes to the hand, the other card is discarded. */
    fun finishStrategy(keep: ResearchCard) { TODO() }

    /** Publishes the research stack of [area] ("Publizieren"). */
    fun publish(area: CardSuit) { TODO() }

    /** Removes the top card of [target]'s stack in [area] ("Konkurrenz ausbremsen"). */
    fun sabotage(target: Faculty, area: CardSuit) { TODO() }

    /** Takes [card] from the central literature ("Literatur nutzen"). */
    fun useLiterature(card: ResearchCard) { TODO() }
}