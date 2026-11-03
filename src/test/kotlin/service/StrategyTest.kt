
package service

import entity.CardSuit
import entity.CardValue
import entity.Faculty
import entity.FacultyGame
import entity.ResearchCard
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

/**
 * Tests the strategy action of the PlayerActionService.
 *
 * Verifies successful strategy execution and the handling
 * of invalid actions during both strategy phases.
 */
class StrategyTest {

    /**
     * Tests successful execution of the strategy action.
     *
     * Verifies that one hand card is discarded, two cards
     * are drawn, one card is kept and the other is discarded.
     * Also verifies that only one action point is deducted.
     */
    @Test
    fun testStrategySuccess() {

        // 1. Create players and game
        val player = Faculty("Ahmed")
        val opponent = Faculty("Mohammad")
        val game = FacultyGame(listOf(player, opponent))

        // 2. Initialize RootService
        val rootService = RootService()
        rootService.currentGame = game

        // 3. Give player 2 AP
        player.actionPoints = 2

        // 4. Add a card to player's hand
        val oldCard = ResearchCard(
            CardSuit.SPADES, CardValue.KING
        )
        player.handCards.add(oldCard)

        // 5. Prepare two cards in drawStack
        val firstCard = ResearchCard(
            CardSuit.HEARTS, CardValue.KING
        )
        val secondCard = ResearchCard(
            CardSuit.CLUBS, CardValue.KING
        )

        game.drawStack.add(firstCard)
        game.drawStack.add(secondCard)

        // 6. Start strategy
        rootService.playerActionService.startStrategy(oldCard)

        // 7. Check first phase
        assertEquals(1, player.actionPoints)
        assertFalse(player.handCards.contains(oldCard))
        assertTrue(game.discardStack.contains(oldCard))
        assertEquals(2, game.pendingStrategyCards.size)
        assertTrue(game.drawStack.isEmpty())

        // 8. Finish strategy: keep firstCard
        rootService.playerActionService.finishStrategy(firstCard)

        // 9. Check second phase
        assertTrue(player.handCards.contains(firstCard))
        assertFalse(player.handCards.contains(secondCard))
        assertTrue(game.discardStack.contains(secondCard))
        assertTrue(game.pendingStrategyCards.isEmpty())

        // 10. AP must not be deducted twice
        assertEquals(1, player.actionPoints)
    }

    /**
     * Tests that the strategy action is rejected when
     * the current player has no action points left.
     *
     * Verifies that an IllegalArgumentException is thrown
     * and the player's hand and action points remain unchanged.
     */
    @Test
    fun testStrategyNotEnoughAP() {

        val player = Faculty("Ahmed")
        val game = FacultyGame(
            listOf(player, Faculty("Mohammad"))
        )

        val rootService = RootService()
        rootService.currentGame = game

        player.actionPoints = 0

        val card = ResearchCard(
            CardSuit.SPADES, CardValue.KING
        )
        player.handCards.add(card)

        assertThrows(IllegalArgumentException::class.java) {
            rootService.playerActionService.startStrategy(card)
        }

        assertTrue(player.handCards.contains(card))
        assertEquals(0, player.actionPoints)
    }

    /**
     * Tests that a player cannot discard a card
     * that is not present in their own hand.
     *
     * Verifies that the action is rejected without
     * changing the action points or discard stack.
     */
    @Test
    fun testStrategyInvalidCard() {

        val player = Faculty("Ahmed")
        val game = FacultyGame(
            listOf(player, Faculty("Mohammad"))
        )

        val rootService = RootService()
        rootService.currentGame = game

        player.actionPoints = 2

        val foreignCard = ResearchCard(
            CardSuit.DIAMONDS, CardValue.KING
        )

        assertThrows(IllegalArgumentException::class.java) {
            rootService.playerActionService.startStrategy(foreignCard)
        }

        assertEquals(2, player.actionPoints)
        assertTrue(game.discardStack.isEmpty())
    }

    /**
     * Tests that the player can only keep one of
     * the two cards drawn during the strategy action.
     *
     * Verifies that selecting an unrelated card throws
     * an IllegalArgumentException and does not modify
     * the pending strategy cards or the player's hand.
     */
    @Test
    fun testFinishStrategyInvalidCard() {

        val player = Faculty("Ahmed")
        val game = FacultyGame(
            listOf(player, Faculty("Mohammad"))
        )

        val rootService = RootService()
        rootService.currentGame = game

        val firstCard = ResearchCard(
            CardSuit.SPADES, CardValue.KING
        )
        val secondCard = ResearchCard(
            CardSuit.HEARTS, CardValue.KING
        )
        val invalidCard = ResearchCard(
            CardSuit.CLUBS, CardValue.KING
        )

        game.pendingStrategyCards.addAll(
            listOf(firstCard, secondCard)
        )

        assertThrows(IllegalArgumentException::class.java) {
            rootService.playerActionService.finishStrategy(invalidCard)
        }

        assertEquals(2, game.pendingStrategyCards.size)
        assertTrue(player.handCards.isEmpty())
    }

    /**
     * Tests that starting a strategy without an active game
     * throws an IllegalStateException.
     */
    @Test
    fun testStartStrategyWithoutGame() {
        val rootService = RootService()
        val card = ResearchCard(CardSuit.SPADES, CardValue.KING)

        assertThrows(IllegalStateException::class.java) {
            rootService.playerActionService.startStrategy(card)
        }
    }

    /**
     * Tests that a strategy cannot be started
     * when fewer than two cards are available.
     */
    @Test
    fun testStrategyNotEnoughDrawCards() {
        val player = Faculty("Ahmed")
        val opponent = Faculty("Mohammad")
        val game = FacultyGame(listOf(player, opponent))
        val rootService = RootService()
        rootService.currentGame = game

        player.actionPoints = 2

        val card = ResearchCard(CardSuit.SPADES, CardValue.KING)
        player.handCards.add(card)

        assertThrows(IllegalArgumentException::class.java) {
            rootService.playerActionService.startStrategy(card)
        }

        assertTrue(player.handCards.contains(card))
        assertEquals(2, player.actionPoints)
    }

    /**
     * Tests that another strategy cannot be started
     * while a strategy is already in progress.
     */
    @Test
    fun testStrategyAlreadyInProgress() {
        val player = Faculty("Ahmed")
        val opponent = Faculty("Mohammad")
        val game = FacultyGame(listOf(player, opponent))
        val rootService = RootService()
        rootService.currentGame = game

        player.actionPoints = 2

        val card = ResearchCard(CardSuit.SPADES, CardValue.KING)
        player.handCards.add(card)

        game.drawStack.add(
            ResearchCard(CardSuit.HEARTS, CardValue.KING)
        )
        game.drawStack.add(
            ResearchCard(CardSuit.CLUBS, CardValue.KING)
        )

        game.pendingStrategyCards.add(
            ResearchCard(CardSuit.DIAMONDS, CardValue.KING)
        )

        assertThrows(IllegalArgumentException::class.java) {
            rootService.playerActionService.startStrategy(card)
        }

        assertTrue(player.handCards.contains(card))
        assertEquals(2, player.actionPoints)
    }

    /**
     * Tests that finishing a strategy without an active game
     * throws an IllegalStateException.
     */
    @Test
    fun testFinishStrategyWithoutGame() {
        val rootService = RootService()
        val card = ResearchCard(CardSuit.SPADES, CardValue.KING)

        assertThrows(IllegalStateException::class.java) {
            rootService.playerActionService.finishStrategy(card)
        }
    }

    /**
     * Tests that a strategy cannot be finished
     * when no strategy is in progress.
     */
    @Test
    fun testFinishStrategyNotStarted() {
        val player = Faculty("Ahmed")
        val opponent = Faculty("Mohammad")
        val game = FacultyGame(listOf(player, opponent))
        val rootService = RootService()
        rootService.currentGame = game

        val card = ResearchCard(CardSuit.SPADES, CardValue.KING)

        assertThrows(IllegalArgumentException::class.java) {
            rootService.playerActionService.finishStrategy(card)
        }

        assertTrue(game.pendingStrategyCards.isEmpty())
        assertTrue(player.handCards.isEmpty())
    }

    /**
     * Tests that registered refreshables are notified
     * when a strategy starts and finishes.
     */
    @Test
    fun testStrategyRefreshable() {
        val player = Faculty("Ahmed")
        val opponent = Faculty("Mohammad")
        val game = FacultyGame(listOf(player, opponent))
        val rootService = RootService()
        rootService.currentGame = game

        player.actionPoints = 2

        val oldCard = ResearchCard(CardSuit.SPADES, CardValue.KING)
        val firstCard = ResearchCard(CardSuit.HEARTS, CardValue.KING)
        val secondCard = ResearchCard(CardSuit.CLUBS, CardValue.KING)

        player.handCards.add(oldCard)
        game.drawStack.add(firstCard)
        game.drawStack.add(secondCard)

        var startCalled = false
        var endCalled = false
        var receivedCards: List<ResearchCard> = emptyList()
        var receivedFaculty: Faculty? = null

        val refreshable = object : Refreshable {
            override fun refreshAfterStrategyStart(
                drawnCards: List<ResearchCard>
            ) {
                startCalled = true
                receivedCards = drawnCards
            }

            override fun refreshAfterStrategyEnd(
                faculty: Faculty
            ) {
                endCalled = true
                receivedFaculty = faculty
            }
        }

        rootService.addRefreshable(refreshable)

        rootService.playerActionService.startStrategy(oldCard)

        assertTrue(startCalled)
        assertEquals(2, receivedCards.size)
        assertTrue(receivedCards.contains(firstCard))
        assertTrue(receivedCards.contains(secondCard))

        rootService.playerActionService.finishStrategy(firstCard)

        assertTrue(endCalled)
        assertSame(player, receivedFaculty)
    }

}
