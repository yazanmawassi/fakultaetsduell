package service

/**
 * This interface provides a mechanism for the service layer classes to communicate
 * (usually to the GUI classes) that certain changes have been made to the entity
 * layer, so that the user interface can be updated accordingly.
 *
 * Default (empty) implementations are provided for all methods, so that implementing
 * GUI classes only need to react to events relevant to them.
 *
 * @see AbstractRefreshingService
 */

import entity.CardSuit
import entity.Faculty
import entity.ResearchCard


interface Refreshable {


    fun refreshAfterStartGame() {}


    fun refreshAfterResearch(faculty: Faculty) {}


    fun refreshAfterDrawIdea(faculty: Faculty) {}


    fun refreshAfterStrategyStart(drawnCards: List<ResearchCard>) {}

    fun refreshAfterStrategyEnd(faculty: Faculty) {}

    fun refreshAfterPublish(faculty: Faculty, area: CardSuit, points: Int) {}

    fun refreshAfterSabotage(target: Faculty, area: CardSuit) {}

    fun refreshAfterUseLiterature(faculty: Faculty, card: ResearchCard) {}

    fun refreshAfterTurnChange(nextFaculty: Faculty) {}

    fun refreshAfterGameEnd(scores: List<FinalScore>) {}

    fun refreshAfterLog(message: String) {}
}