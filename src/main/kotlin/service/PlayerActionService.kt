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
    fun sabotage(target: Faculty, area: CardSuit) { TODO() }

    /** Takes [card] from the central literature ("Literatur nutzen"). */
    fun useLiterature(card: ResearchCard) { TODO() }
}