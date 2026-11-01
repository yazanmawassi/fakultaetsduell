package service

import entity.CardSuit
import entity.CardValue
import entity.Faculty
import entity.ResearchCard
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

/**
 * Tests for the final scoring that [GameService] calculates at the end of the game.
 */
class EndScoringTest {

    private lateinit var rootService: RootService
    private lateinit var refreshable: TestRefreshable

    /** Fresh RootService with a registered [TestRefreshable] before every test. */
    @BeforeTest
    fun setUp() {
        rootService = RootService()
        refreshable = TestRefreshable()
        rootService.addRefreshable(refreshable)
    }

    /** The faculty with the most points is first in the list. */
    @Test
    fun `final scores are sorted with best faculty first`() {
        rootService.gameService.startNewGame(listOf("A", "B"))
        faculty("A").researchPoints = 3
        faculty("B").researchPoints = 10

        finishGame()

        val scores = assertNotNull(refreshable.finalScores)
        assertEquals("B", scores[0].faculty.name)
        assertEquals(10, scores[0].total)
    }

    /** Most published cards in a suit +5, second most +2. */
    @Test
    fun `majority bonus gives 5 for most and 2 for second most`() {
        rootService.gameService.startNewGame(listOf("A", "B", "C"))
        repeat(3) { faculty("A").publications.add(card(CardSuit.HEARTS)) }
        faculty("B").publications.add(card(CardSuit.HEARTS))

        finishGame()

        assertEquals(5, scoreOf("A").majorityBonus)
        assertEquals(2, scoreOf("B").majorityBonus)
        assertEquals(0, scoreOf("C").majorityBonus)
    }

    /** Tie: both share first place, the next count is second place (confirmed by PO). */
    @Test
    fun `majority bonus with tie - both get 5, next one gets 2`() {
        rootService.gameService.startNewGame(listOf("A", "B", "C"))
        repeat(3) { faculty("A").publications.add(card(CardSuit.HEARTS)) }
        repeat(3) { faculty("B").publications.add(card(CardSuit.HEARTS)) }
        repeat(2) { faculty("C").publications.add(card(CardSuit.HEARTS)) }

        finishGame()

        assertEquals(5, scoreOf("A").majorityBonus)
        assertEquals(5, scoreOf("B").majorityBonus)
        assertEquals(2, scoreOf("C").majorityBonus)
    }

    /** Publications in all 4 suits give +3. */
    @Test
    fun `breadth bonus needs publications in all 4 suits`() {
        rootService.gameService.startNewGame(listOf("A", "B"))
        CardSuit.entries.forEach { faculty("A").publications.add(card(it)) }
        faculty("B").publications.add(card(CardSuit.SPADES))

        finishGame()

        assertEquals(3, scoreOf("A").breadthBonus)
        assertEquals(0, scoreOf("B").breadthBonus)
    }

    /** At least 8 published cards in one suit give +4. */
    @Test
    fun `focus bonus needs at least 8 cards in one suit`() {
        rootService.gameService.startNewGame(listOf("A", "B"))
        repeat(8) { faculty("A").publications.add(card(CardSuit.SPADES)) }
        repeat(7) { faculty("B").publications.add(card(CardSuit.SPADES)) }

        finishGame()

        assertEquals(4, scoreOf("A").focusBonus)
        assertEquals(0, scoreOf("B").focusBonus)
    }

    /** Every card on a research stack gives +1. */
    @Test
    fun `unpublished research cards give 1 point each`() {
        rootService.gameService.startNewGame(listOf("A", "B"))
        faculty("A").addResearch(card(CardSuit.CLUBS))
        faculty("A").addResearch(card(CardSuit.DIAMONDS))

        finishGame()

        assertEquals(2, scoreOf("A").unpublishedPoints)
        assertEquals(0, scoreOf("B").unpublishedPoints)
    }

    /** The total is the sum of all parts. */
    @Test
    fun `total is sum of all parts`() {
        rootService.gameService.startNewGame(listOf("A", "B"))
        faculty("A").researchPoints = 7
        repeat(2) { faculty("A").publications.add(card(CardSuit.HEARTS)) }
        faculty("A").addResearch(card(CardSuit.CLUBS))

        finishGame()

        val a = scoreOf("A")
        assertEquals(
            a.publicationPoints + a.majorityBonus + a.breadthBonus + a.focusBonus + a.unpublishedPoints,
            a.total
        )
        assertEquals(7 + 5 + 1, a.total)
    }

    private fun card(suit: CardSuit) = ResearchCard(suit, CardValue.TWO)

    /** Finds a faculty by name, because the order is random. */
    private fun faculty(name: String): Faculty =
        assertNotNull(rootService.currentGame).faculties.first { it.name == name }

    /** Brings the running game to its end (last turn, no action points left). */
    private fun finishGame() {
        val game = assertNotNull(rootService.currentGame)
        game.drawStack.clear()
        game.endgameTurnsLeft = 1
        game.faculties[game.facultyAtTurn].actionPoints = 0
        rootService.gameService.endTurn()
    }

    private fun scoreOf(name: String): FinalScore =
        assertNotNull(refreshable.finalScores).first { it.faculty.name == name }
}