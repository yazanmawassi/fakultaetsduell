package service

import entity.CardSuit
import entity.Faculty
import entity.ResearchCard

/**
 * Service for all actions the active faculty can do in its turn.
 */
class PlayerActionService(private val rootService: RootService) : AbstractRefreshingService() {

    /**  ("Forschung vorantreiben"). */
    fun research(card: ResearchCard) { TODO() }

    /**  ("Neue Ideen gewinnen"). */
    fun drawIdea() { TODO() }

    /** First step of "Strategie anpassen"  */
    fun startStrategy(discard: ResearchCard) { TODO() }

    /** Second step of "Strategie anpassen" */
    fun finishStrategy(keep: ResearchCard) { TODO() }

    /**  ("Publizieren"). */
    fun publish(area: CardSuit) { TODO() }

    /**  ("Konkurrenz ausbremsen"). */
    fun sabotage(target: Faculty, area: CardSuit) { TODO() }

    /**  ("Literatur nutzen"). */
    fun useLiterature(card: ResearchCard) { TODO() }
}