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
 * Comprehensive JUnit 5 tests for the [PlayerActionService.drawIdea] method.
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
}

