package service

import entity.CardSuit
import entity.Faculty
import entity.ResearchCard

/**
 * [Refreshable] for tests: remembers which refresh methods were called.
 */
class TestRefreshable : Refreshable {

    var refreshAfterStartGameCalled = false
        private set
    var refreshAfterResearchCalled = false
        private set
    var refreshAfterDrawIdeaCalled = false
        private set
    var refreshAfterStrategyStartCalled = false
        private set
    var refreshAfterStrategyEndCalled = false
        private set
    var refreshAfterPublishCalled = false
        private set
    var refreshAfterSabotageCalled = false
        private set
    var refreshAfterUseLiteratureCalled = false
        private set
    var refreshAfterTurnChangeCalled = false
        private set
    var finalScores: List<FinalScore>? = null
        private set
    val logMessages = mutableListOf<String>()

    override fun refreshAfterStartGame() { refreshAfterStartGameCalled = true }
    override fun refreshAfterResearch(faculty: Faculty) { refreshAfterResearchCalled = true }
    override fun refreshAfterDrawIdea(faculty: Faculty) { refreshAfterDrawIdeaCalled = true }
    override fun refreshAfterStrategyStart(drawnCards: List<ResearchCard>) {
        refreshAfterStrategyStartCalled = true
    }
    override fun refreshAfterStrategyEnd(faculty: Faculty) { refreshAfterStrategyEndCalled = true }
    override fun refreshAfterPublish(faculty: Faculty, area: CardSuit, points: Int) {
        refreshAfterPublishCalled = true
    }
    override fun refreshAfterSabotage(target: Faculty, area: CardSuit) { refreshAfterSabotageCalled = true }
    override fun refreshAfterUseLiterature(faculty: Faculty, card: ResearchCard) {
        refreshAfterUseLiteratureCalled = true
    }
    override fun refreshAfterTurnChange(nextFaculty: Faculty) { refreshAfterTurnChangeCalled = true }
    override fun refreshAfterGameEnd(scores: List<FinalScore>) { finalScores = scores }
    override fun refreshAfterLog(message: String) { logMessages.add(message) }

    /** Resets all flags, e.g. between two steps of a test. */
    fun reset() {
        refreshAfterStartGameCalled = false
        refreshAfterResearchCalled = false
        refreshAfterDrawIdeaCalled = false
        refreshAfterStrategyStartCalled = false
        refreshAfterStrategyEndCalled = false
        refreshAfterPublishCalled = false
        refreshAfterSabotageCalled = false
        refreshAfterUseLiteratureCalled = false
        refreshAfterTurnChangeCalled = false
        finalScores = null
        logMessages.clear()
    }
}