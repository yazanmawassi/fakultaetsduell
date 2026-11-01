package service

import entity.CardSuit
import entity.CardValue
import entity.Faculty
import entity.FacultyGame
import entity.ResearchCard
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Tests for [PlayerActionService.hasPossibleAction].
 */
class HasPossibleActionTest {

    private lateinit var rootService: RootService

    /** Starts a new game with two faculties in fixed order before every test. */
    @BeforeTest
    fun setUp() {
        rootService = RootService()
        rootService.gameService.startNewGame(listOf("A", "B"), randomOrder = false)
    }

    /** At the start of the game the first faculty can always act. */
    @Test
    fun `action possible at start`() {
        assertTrue(rootService.playerActionService.hasPossibleAction())
    }

    /** Without action points no action is possible. */
    @Test
    fun `no action without action points`() {
        current().actionPoints = 0
        assertFalse(rootService.playerActionService.hasPossibleAction())
    }

    /** Action points left, but nothing on the table: deadlock. */
    @Test
    fun `no action in deadlock`() {
        createDeadlock()
        assertFalse(rootService.playerActionService.hasPossibleAction())
    }

    /** One hand card is enough for research (1 AP). */
    @Test
    fun `research possible with one hand card`() {
        createDeadlock()
        current().handCards.add(card(CardSuit.HEARTS))
        assertTrue(rootService.playerActionService.hasPossibleAction())
    }

    /** One card on the draw stack is enough for "Neue Ideen" (1 AP). */
    @Test
    fun `draw idea possible with one card on draw stack`() {
        createDeadlock()
        game().drawStack.add(card(CardSuit.SPADES))
        assertTrue(rootService.playerActionService.hasPossibleAction())
    }

    /** "Literatur nutzen" needs 2 AP. */
    @Test
    fun `use literature needs two action points`() {
        createDeadlock()
        game().centralLiterature.add(card(CardSuit.CLUBS))

        current().actionPoints = 1
        assertFalse(rootService.playerActionService.hasPossibleAction())

        current().actionPoints = 2
        assertTrue(rootService.playerActionService.hasPossibleAction())
    }

    /** Publishing needs at least 2 cards on one of the own research stacks. */
    @Test
    fun `publish needs two cards on own stack`() {
        createDeadlock()
        current().addResearch(card(CardSuit.DIAMONDS))
        assertFalse(rootService.playerActionService.hasPossibleAction())

        current().addResearch(card(CardSuit.DIAMONDS))
        assertTrue(rootService.playerActionService.hasPossibleAction())
    }

    /** Sabotage is possible if an opponent has at least one research card. */
    @Test
    fun `sabotage possible if opponent has research`() {
        createDeadlock()
        opponent().addResearch(card(CardSuit.HEARTS))
        assertTrue(rootService.playerActionService.hasPossibleAction())
    }

    /** While a strategy is running, finishing it is still possible. */
    @Test
    fun `action possible while strategy is pending`() {
        createDeadlock()
        game().pendingStrategyCards.add(card(CardSuit.HEARTS))
        game().pendingStrategyCards.add(card(CardSuit.CLUBS))
        assertTrue(rootService.playerActionService.hasPossibleAction())
    }

    /** Without a running game an exception is thrown. */
    @Test
    fun `fails without game`() {
        rootService.currentGame = null
        assertFailsWith<IllegalStateException> {
            rootService.playerActionService.hasPossibleAction()
        }
    }

    // ---------- Helper ----------

    private fun game(): FacultyGame = checkNotNull(rootService.currentGame)

    private fun current(): Faculty = game().faculties[game().facultyAtTurn]

    private fun opponent(): Faculty = game().faculties.first { it !== current() }

    private fun card(suit: CardSuit): ResearchCard = ResearchCard(suit, CardValue.entries.first())

    /** Empties every card area, so that only the 2 AP are left. */
    private fun createDeadlock() {
        game().drawStack.clear()
        game().centralLiterature.clear()
        game().pendingStrategyCards.clear()
        game().faculties.forEach { faculty ->
            faculty.handCards.clear()
            faculty.researchActivity.values.forEach { it.clear() }
        }
        current().actionPoints = 2
    }
}