package service

import entity.CardSuit
import entity.CardValue
import entity.ResearchCard
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull

/**
 *  tests for the [PlayerActionService.drawIdea] method.
 */
class DrawIdeaTest {

    private lateinit var rootService: RootService
    private lateinit var refreshable: TestRefreshable

    /** Fresh RootService with a registered [TestRefreshable] before every test. */
    @BeforeTest
    fun setUp() {
        rootService = RootService()
        refreshable = TestRefreshable()
        rootService.addRefreshable(refreshable)
    }

    /** Helper function to create a dummy card for testing. */
    private fun createCard(): ResearchCard {
        return ResearchCard(CardSuit.HEARTS, CardValue.TWO)
    }


    /**
     * Tests a successful draw scenario (Happy Path).
     *
     * Prepares Player A with 2 Action Points and an empty hand while ensuring the draw stack
     * contains cards. Verifies that exactly 1 AP is deducted, 1 card is added to the hand,
     * and the UI refresh framework is notified.
     */
    @Test
    fun testDrawIdeaSuccess() {
        rootService.gameService.startNewGame(listOf("A", "B"))
        val game = assertNotNull(rootService.currentGame)
        val playerA = game.faculties[0]

        playerA.actionPoints = 2
        playerA.handCards.clear()

        game.drawStack.add(createCard())

        rootService.playerActionService.drawIdea()

        assertEquals(1, playerA.actionPoints, "Exactly 1 AP must be deducted.")
        assertEquals(1, playerA.handCards.size, "Player must have exactly 1 card in hand.")
    }


    /**
     * Verifies that [PlayerActionService.drawIdea] throws an [IllegalArgumentException]
     * if the active player tries to draw with 0 Action Points.
     */
    @Test
    fun testDrawIdeaThrowsIfNotEnoughAP() {
        rootService.gameService.startNewGame(listOf("A", "B"))
        val game = assertNotNull(rootService.currentGame)
        val playerA = game.faculties[0]

        playerA.actionPoints = 0

        assertFailsWith<IllegalArgumentException> {
            rootService.playerActionService.drawIdea()
        }
    }


    /**
     * Verifies that [PlayerActionService.drawIdea] throws an [IllegalArgumentException]
     * if the active player's hand cards limit has already reached 6 cards.
     */
    @Test
    fun testDrawIdeaThrowsIfHandCardsLimitReached() {
        rootService.gameService.startNewGame(listOf("A", "B"))
        val game = assertNotNull(rootService.currentGame)
        val playerA = game.faculties[0]

        playerA.actionPoints = 2

        playerA.handCards.clear()
        playerA.handCards.add(createCard())
        playerA.handCards.add(createCard())
        playerA.handCards.add(createCard())
        playerA.handCards.add(createCard())
        playerA.handCards.add(createCard())
        playerA.handCards.add(createCard()) // Already has 6 cards

        assertFailsWith<IllegalArgumentException> {
            rootService.playerActionService.drawIdea()
        }
    }


    /**
     * Verifies that [PlayerActionService.drawIdea] throws an [IllegalStateException]
     * if the shared game draw stack is completely empty when a draw is attempted.
     */
    @Test
    fun testDrawIdeaThrowsIfDrawStackIsEmpty() {
        rootService.gameService.startNewGame(listOf("A", "B"))
        val game = assertNotNull(rootService.currentGame)

        val playerA = game.faculties[0]
        playerA.actionPoints = 2

        game.drawStack.clear()

        assertFailsWith<IllegalStateException> {
            rootService.playerActionService.drawIdea()
        }
    }


    /**
     * Test 5: Verifies that [PlayerActionService.drawIdea] throws an [IllegalStateException]
     * if it is called before any active game context exists.
     */
    @Test
    fun testDrawIdeaWithoutGameFails() {
        // We do NOT start a game here (rootService.currentGame remains null)

        assertFailsWith<IllegalStateException> {
            rootService.playerActionService.drawIdea()
        }
    }

    /**
     *  Verifies that [PlayerActionService.drawIdea] throws an [IllegalStateException]
     * if it is called after the game has officially ended.
     */
    @Test
    fun testDrawIdeaAfterGameEndFails() {
        rootService.gameService.startNewGame(listOf("A", "B"))
        val game = assertNotNull(rootService.currentGame)

        // Error state: Set the countdown to 0 to simulate that the game is over
        game.endgameTurnsLeft = 0

        assertFailsWith<IllegalStateException> {
            rootService.playerActionService.drawIdea()
        }
    }
}

