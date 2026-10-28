
package service

import entity.CardSuit
import entity.CardValue
import entity.Faculty
import entity.FacultyGame
import entity.ResearchCard
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class StrategyTest {
//Eine Handkarte wird abgeworfen, zwei Karten werden gezogen, eine wird behalten
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

    @Test
    //Aktion wird bei 0 AP abgelehnt
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

    @Test
    //Der Spieler darf keine fremde Karte abwerfen
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

    @Test
    //Spieler darf nur eine der zwei gezogenen Karten behalten
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
}
