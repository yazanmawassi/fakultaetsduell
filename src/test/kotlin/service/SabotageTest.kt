
package service

import entity.CardSuit
import entity.CardValue
import entity.Faculty
import entity.FacultyGame
import entity.ResearchCard
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class SabotageTest {

    @Test
    fun testSabotageSuccess() {

        // 1. Create two players
        val attacker = Faculty("Ahmed")
        val target = Faculty("Mohammad")

        // 2. Create game
        val game = FacultyGame(listOf(attacker, target))

        // 3. Initialize RootService
        val rootService = RootService()
        rootService.currentGame = game

        // 4. Give attacker 2 action points
        attacker.actionPoints = 2

        // 5. Add a card to opponent's research stack
        val card = ResearchCard(
            CardSuit.SPADES,
            CardValue.KING
        )
        target.addResearch(card)

        // 6. Execute sabotage
        rootService.playerActionService.sabotage(
            target,
            CardSuit.SPADES
        )

        // 7. Check action points
        assertEquals(0, attacker.actionPoints)

        // 8. Check research stack is empty
        assertTrue(
            target.researchActivity[CardSuit.SPADES]!!.isEmpty()
        )

        // 9. Check discarded card
        assertSame(card, game.discardStack.last())
    }
}
