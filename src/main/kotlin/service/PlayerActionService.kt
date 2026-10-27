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
    fun research(card: ResearchCard) { TODO() }

    /** Draws one card from the draw stack to the hand ("Neue Ideen gewinnen"). */
    fun drawIdea() { TODO() }

    /** First step of "Strategie anpassen": discards [discard] and draws two cards. */
    fun startStrategy(discard: ResearchCard) { TODO() }

    /** Second step of "Strategie anpassen": [keep] goes to the hand, the other card is discarded. */
    fun finishStrategy(keep: ResearchCard) { TODO() }

    /** Publishes the research stack of [area] ("Publizieren"). */
    fun publish(area: CardSuit) { TODO() }

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

    /** Takes [card] from the central literature ("Literatur nutzen"). */
    fun useLiterature(card: ResearchCard) { TODO() }
}