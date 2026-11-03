package service

import entity.CardSuit
import entity.CardValue
import entity.Faculty
import entity.FacultyGame
import entity.ResearchCard
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertSame

/** Verifies that exhausted AP request a countdown rather than switching players immediately. */
class ActionPointsDepletedTest {
    private val rootService = RootService()
    private val first = Faculty("Ada").apply { actionPoints = 1 }
    private val second = Faculty("Linus")
    private val game = FacultyGame(listOf(first, second))
    private val requests = mutableListOf<Faculty>()

    init {
        rootService.currentGame = game
        rootService.addRefreshable(object : Refreshable {
            override fun refreshAfterActionPointsDepleted(faculty: Faculty) {
                requests.add(faculty)
            }
        })
    }

    /** Zero AP notify the GUI, but the next player only starts when endTurn is explicitly called. */
    @Test
    fun researchWaitsForCountdown() {
        val card = ResearchCard(CardSuit.HEARTS, CardValue.ACE)
        first.handCards.add(card)
        game.drawStack.addAll(listOf(card, card))

        rootService.playerActionService.research(card)

        assertEquals(0, first.actionPoints)
        assertEquals(listOf(first), requests)
        assertEquals(0, game.facultyAtTurn)
        rootService.gameService.endTurn()
        assertSame(second, game.faculties[game.facultyAtTurn])
    }

    /** A deadlock with AP remaining waits for manual turn completion without a countdown. */
    @Test
    fun remainingActionPointsDoNotRequestCountdown() {
        first.actionPoints = 2
        val card = ResearchCard(CardSuit.HEARTS, CardValue.ACE)
        first.handCards.add(card)

        rootService.playerActionService.research(card)

        assertEquals(1, first.actionPoints)
        assertFalse(rootService.playerActionService.hasPossibleAction())
        assertEquals(emptyList(), requests)
        assertEquals(0, game.facultyAtTurn)
    }

    /** Spending the last AP on strategy waits for the card choice before requesting the countdown. */
    @Test
    fun strategySelectionMustFinishBeforeCountdown() {
        val card = ResearchCard(CardSuit.HEARTS, CardValue.ACE)
        first.handCards.add(card)
        game.drawStack.addAll(listOf(
            ResearchCard(CardSuit.CLUBS, CardValue.TWO),
            ResearchCard(CardSuit.SPADES, CardValue.THREE)
        ))

        rootService.playerActionService.startStrategy(card)

        assertEquals(0, first.actionPoints)
        assertEquals(emptyList(), requests)
        assertEquals(0, game.facultyAtTurn)
        rootService.playerActionService.finishStrategy(game.pendingStrategyCards.first())
        assertEquals(listOf(first), requests)
        assertEquals(0, game.facultyAtTurn)
    }

    /** The final turn also waits for explicit completion before the game-end event. */
    @Test
    fun finalTurnWaitsForCountdownBeforeEndingGame() {
        game.endgameTurnsLeft = 1
        val card = ResearchCard(CardSuit.HEARTS, CardValue.ACE)
        first.handCards.add(card)
        var ended = false
        rootService.addRefreshable(object : Refreshable {
            override fun refreshAfterGameEnd(scores: List<FinalScore>) { ended = true }
        })

        rootService.playerActionService.research(card)

        assertFalse(ended)
        assertEquals(listOf(first), requests)
        rootService.gameService.endTurn()
        assertEquals(true, ended)
        assertEquals(0, game.endgameTurnsLeft)
    }
}
