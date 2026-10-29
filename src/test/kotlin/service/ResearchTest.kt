package service

import entity.CardSuit
import entity.CardValue
import entity.Faculty
import entity.FacultyGame
import entity.ResearchCard
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

/**
 * Tests valid research actions and invalid game states.
 */
class ResearchTest {

    /**
     * Creates a deterministic game with two faculties.
     * The active faculty starts with two action points.
     */
    private fun createRootService(): RootService {
        val rootService = RootService()
        val faculty = Faculty("Dortmund")
        val otherFaculty = Faculty("Bochum")

        faculty.resetActionPoints()
        rootService.currentGame = FacultyGame(
            listOf(faculty, otherFaculty)
        )

        return rootService
    }

    /**
     * Verifies that research moves the hand card onto the matching
     * research stack, draws literature and consumes one action point.
     */
    @Test
    fun testResearch() {
        val rootService = createRootService()
        val game = checkNotNull(rootService.currentGame)
        val faculty = game.faculties[game.facultyAtTurn]

        val card = ResearchCard(CardSuit.HEARTS, CardValue.ACE)
        val drawnCard = ResearchCard(CardSuit.CLUBS, CardValue.TWO)
        faculty.handCards.add(card)
        game.drawStack.add(drawnCard)

        rootService.playerActionService.research(card)

        assertFalse(card in faculty.handCards)
        assertEquals(
            listOf(card),
            faculty.researchActivity.getValue(card.suit).toList()
        )
        assertEquals(1, faculty.actionPoints)
        assertEquals(listOf(drawnCard), game.centralLiterature)
        assertEquals(0, game.drawStack.size)
        assertEquals(0, game.discardStack.size)
    }

    /**
     * Verifies that full central literature loses its oldest card
     * and receives the top card from the draw stack.
     */
    @Test
    fun testResearchWithFullLiterature() {
        val rootService = createRootService()
        val game = checkNotNull(rootService.currentGame)
        val faculty = game.faculties[game.facultyAtTurn]

        val card = ResearchCard(CardSuit.HEARTS, CardValue.ACE)
        faculty.handCards.add(card)

        val literature = CardValue.entries.take(6).map {
            ResearchCard(CardSuit.SPADES, it)
        }
        game.centralLiterature.addAll(literature)

        val bottomCard = ResearchCard(CardSuit.CLUBS, CardValue.TWO)
        val topCard = ResearchCard(CardSuit.DIAMONDS, CardValue.KING)
        game.drawStack.addAll(listOf(bottomCard, topCard))

        rootService.playerActionService.research(card)

        assertEquals(
            literature.drop(1) + topCard,
            game.centralLiterature
        )
        assertEquals(listOf(literature.first()), game.discardStack)
        assertEquals(listOf(bottomCard), game.drawStack)
    }

    /**
     * Verifies that research remains possible with an empty draw stack
     * and leaves the central literature unchanged.
     */
    @Test
    fun testResearchWithEmptyDrawStack() {
        val rootService = createRootService()
        val game = checkNotNull(rootService.currentGame)
        val faculty = game.faculties[game.facultyAtTurn]

        val card = ResearchCard(CardSuit.HEARTS, CardValue.ACE)
        faculty.handCards.add(card)

        val literature = CardValue.entries.take(6).map {
            ResearchCard(CardSuit.CLUBS, it)
        }
        game.centralLiterature.addAll(literature)

        rootService.playerActionService.research(card)

        assertFalse(card in faculty.handCards)
        assertSame(
            card,
            faculty.researchActivity.getValue(card.suit).peek()
        )
        assertEquals(1, faculty.actionPoints)
        assertEquals(literature, game.centralLiterature)
        assertEquals(0, game.discardStack.size)
    }

    /**
     * Verifies that research is rejected when no game is running.
     */
    @Test
    fun testResearchWithoutGame() {
        val rootService = RootService()
        val card = ResearchCard(CardSuit.HEARTS, CardValue.ACE)

        assertThrows(IllegalStateException::class.java) {
            rootService.playerActionService.research(card)
        }
    }

    /**
     * Verifies that research is rejected after the game has ended.
     */
    @Test
    fun testResearchAfterGameEnd() {
        val rootService = createRootService()
        val game = checkNotNull(rootService.currentGame)
        val faculty = game.faculties[game.facultyAtTurn]
        val card = ResearchCard(CardSuit.HEARTS, CardValue.ACE)

        faculty.handCards.add(card)
        game.endgameTurnsLeft = 0

        assertThrows(IllegalStateException::class.java) {
            rootService.playerActionService.research(card)
        }

        assertEquals(listOf(card), faculty.handCards)
        assertEquals(2, faculty.actionPoints)
        assertEquals(0, faculty.researchActivity.getValue(card.suit).size)
    }

    /**
     * Verifies that research is rejected without action points
     * and does not move the selected card.
     */
    @Test
    fun testResearchWithoutActionPoints() {
        val rootService = createRootService()
        val game = checkNotNull(rootService.currentGame)
        val faculty = game.faculties[game.facultyAtTurn]
        val card = ResearchCard(CardSuit.HEARTS, CardValue.ACE)

        faculty.handCards.add(card)
        faculty.actionPoints = 0

        assertThrows(IllegalStateException::class.java) {
            rootService.playerActionService.research(card)
        }

        assertEquals(listOf(card), faculty.handCards)
        assertEquals(0, faculty.actionPoints)
        assertEquals(0, faculty.researchActivity.getValue(card.suit).size)
    }

    /**
     * Verifies that a card outside the active faculty's hand
     * is rejected without consuming action points.
     */
    @Test
    fun testResearchWithCardNotInHand() {
        val rootService = createRootService()
        val game = checkNotNull(rootService.currentGame)
        val faculty = game.faculties[game.facultyAtTurn]
        val card = ResearchCard(CardSuit.HEARTS, CardValue.ACE)

        assertThrows(IllegalArgumentException::class.java) {
            rootService.playerActionService.research(card)
        }

        assertEquals(0, faculty.handCards.size)
        assertEquals(2, faculty.actionPoints)
        assertEquals(0, faculty.researchActivity.getValue(card.suit).size)
    }
}