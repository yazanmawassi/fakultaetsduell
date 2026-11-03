
package service

import entity.CardSuit
import entity.CardValue
import entity.Faculty
import entity.FacultyGame
import entity.ResearchCard
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.util.Stack

/**
 * Tests successful and invalid sabotage actions.
 */
class SabotageTest {

    /**
     * Tests a successful sabotage action.
     */
    @Test
    fun testSabotageSuccess() {
        val attacker = Faculty("Ahmed")
        val target = Faculty("Mohammad")
        val game = FacultyGame(listOf(attacker, target))

        val rootService = RootService()
        rootService.currentGame = game

        attacker.actionPoints = 2

        val card = ResearchCard(
            CardSuit.SPADES,
            CardValue.KING
        )
        target.addResearch(card)

        rootService.playerActionService.sabotage(
            target,
            CardSuit.SPADES
        )

        assertEquals(0, attacker.actionPoints)
        assertTrue(
            target.researchActivity[CardSuit.SPADES]!!.isEmpty()
        )
        assertSame(card, game.discardStack.last())
    }

    /**
     * Tests sabotage without an active game.
     */
    @Test
    fun testSabotageWithoutGame() {
        val rootService = RootService()
        val target = Faculty("Mohammad")

        assertThrows(IllegalStateException::class.java) {
            rootService.playerActionService.sabotage(
                target,
                CardSuit.SPADES
            )
        }
    }

    /**
     * Tests sabotage with insufficient action points.
     */
    @Test
    fun testSabotageNotEnoughAP() {
        val attacker = Faculty("Ahmed")
        val target = Faculty("Mohammad")
        val game = FacultyGame(listOf(attacker, target))

        val rootService = RootService()
        rootService.currentGame = game

        attacker.actionPoints = 1

        assertThrows(IllegalArgumentException::class.java) {
            rootService.playerActionService.sabotage(
                target,
                CardSuit.SPADES
            )
        }

        assertEquals(1, attacker.actionPoints)
    }

    /**
     * Tests that a player cannot sabotage themselves.
     */
    @Test
    fun testSabotageYourself() {
        val attacker = Faculty("Ahmed")
        val target = Faculty("Mohammad")
        val game = FacultyGame(listOf(attacker, target))

        val rootService = RootService()
        rootService.currentGame = game

        attacker.actionPoints = 2

        assertThrows(IllegalArgumentException::class.java) {
            rootService.playerActionService.sabotage(
                attacker,
                CardSuit.SPADES
            )
        }

        assertEquals(2, attacker.actionPoints)
    }

    /**
     * Tests that sabotage rejects a faculty
     * that does not belong to the current game.
     */
    @Test
    fun testSabotageInvalidTarget() {
        val attacker = Faculty("Ahmed")
        val opponent = Faculty("Mohammad")
        val foreignTarget = Faculty("Yazan")

        val game = FacultyGame(listOf(attacker, opponent))

        val rootService = RootService()
        rootService.currentGame = game

        attacker.actionPoints = 2

        assertThrows(IllegalArgumentException::class.java) {
            rootService.playerActionService.sabotage(
                foreignTarget,
                CardSuit.SPADES
            )
        }

        assertEquals(2, attacker.actionPoints)
    }

    /**
     * Tests that an empty research stack
     * cannot be sabotaged.
     */
    @Test
    fun testSabotageEmptyStack() {
        val attacker = Faculty("Ahmed")
        val target = Faculty("Mohammad")
        val game = FacultyGame(listOf(attacker, target))

        val rootService = RootService()
        rootService.currentGame = game

        attacker.actionPoints = 2

        // Create an empty stack for this research area
        target.researchActivity[CardSuit.SPADES] =
            Stack<ResearchCard>()

        assertThrows(IllegalArgumentException::class.java) {
            rootService.playerActionService.sabotage(
                target,
                CardSuit.SPADES
            )
        }

        assertEquals(2, attacker.actionPoints)
        assertTrue(game.discardStack.isEmpty())
    }

    /**
     * Tests that a successful sabotage action
     * notifies all registered GUI refreshables.
     *
     * Verifies that refreshAfterSabotage is called
     * with the correct target faculty and research area.
     */
    @Test
    fun testSabotageRefreshable() {
        val attacker = Faculty("Ahmed")
        val target = Faculty("Mohammad")
        val game = FacultyGame(listOf(attacker, target))

        val rootService = RootService()
        rootService.currentGame = game

        attacker.actionPoints = 2

        val card = ResearchCard(
            CardSuit.SPADES,
            CardValue.KING
        )
        target.addResearch(card)

        var refreshCalled = false
        var receivedTarget: Faculty? = null
        var receivedArea: CardSuit? = null

        val refreshable = object : Refreshable {
            override fun refreshAfterSabotage(
                target: Faculty,
                area: CardSuit
            ) {
                refreshCalled = true
                receivedTarget = target
                receivedArea = area
            }
        }

        rootService.addRefreshable(refreshable)

        rootService.playerActionService.sabotage(
            target,
            CardSuit.SPADES
        )

        assertTrue(refreshCalled)
        assertSame(target, receivedTarget)
        assertEquals(CardSuit.SPADES, receivedArea)
    }

    /**
     * Tests that sabotage fails when the target faculty
     * has no research stack for the selected area.
     *
     * Verifies that an IllegalStateException is thrown
     * and that action points remain unchanged.
     */
    @Test
    fun testSabotageMissingStack() {
        val attacker = Faculty("Ahmed")
        val target = Faculty("Mohammad")
        val game = FacultyGame(listOf(attacker, target))

        val rootService = RootService()
        rootService.currentGame = game

        attacker.actionPoints = 2

        // Remove the research stack completely
        target.researchActivity.remove(CardSuit.SPADES)

        assertThrows(IllegalStateException::class.java) {
            rootService.playerActionService.sabotage(
                target,
                CardSuit.SPADES
            )
        }

        assertEquals(2, attacker.actionPoints)
        assertTrue(game.discardStack.isEmpty())
    }


}
