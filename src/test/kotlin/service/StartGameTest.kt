package service

import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Tests for [GameService.startNewGame]. [TestRefreshable] is used to check
 * the refresh calls even though no GUI is present.
 */
class StartGameTest {

    private lateinit var rootService: RootService
    private lateinit var refreshable: TestRefreshable

    /** Fresh RootService with a registered [TestRefreshable] before every test. */
    @BeforeTest
    fun setUp() {
        rootService = RootService()
        refreshable = TestRefreshable()
        rootService.addRefreshable(refreshable)
    }

    /** All faculties are created, the first one is at turn and the endgame has not started. */
    @Test
    fun `startNewGame creates all faculties`() {
        rootService.gameService.startNewGame(listOf("A", "B", "C"))

        val game = assertNotNull(rootService.currentGame)
        assertEquals(setOf("A", "B", "C"), game.faculties.map { it.name }.toSet())
        assertEquals(0, game.facultyAtTurn)
        assertEquals(-1, game.endgameTurnsLeft)
        assertTrue(refreshable.refreshAfterStartGameCalled)
    }

    /** Every faculty gets 5 hand cards. */
    @Test
    fun `startNewGame deals 5 cards to every faculty`() {
        rootService.gameService.startNewGame(listOf("A", "B", "C"))

        val game = assertNotNull(rootService.currentGame)
        game.faculties.forEach { assertEquals(5, it.handCards.size) }
    }

    /** The central literature starts with 6 cards. */
    @Test
    fun `startNewGame puts 6 cards into central literature`() {
        rootService.gameService.startNewGame(listOf("A", "B", "C"))

        val game = assertNotNull(rootService.currentGame)
        assertEquals(6, game.centralLiterature.size)
        assertEquals(52 - 3 * 5 - 6, game.drawStack.size)
    }

    /** All 52 cards exist exactly once. */
    @Test
    fun `startNewGame uses all 52 cards exactly once`() {
        rootService.gameService.startNewGame(listOf("A", "B"))

        val game = assertNotNull(rootService.currentGame)
        val allCards = game.drawStack + game.centralLiterature + game.faculties.flatMap { it.handCards }
        assertEquals(52, allCards.size)
        assertEquals(52, allCards.map { it.suit to it.value }.toSet().size)
    }

    /** The first faculty starts with 2 action points. */
    @Test
    fun `startNewGame gives first faculty 2 action points`() {
        rootService.gameService.startNewGame(listOf("A", "B"))

        val game = assertNotNull(rootService.currentGame)
        assertEquals(2, game.faculties[0].actionPoints)
    }

    /** Less than 2 or more than 4 faculties are rejected. */
    @Test
    fun `startNewGame fails with too few or too many faculties`() {
        assertFailsWith<IllegalArgumentException> {
            rootService.gameService.startNewGame(listOf("A"))
        }
        assertFailsWith<IllegalArgumentException> {
            rootService.gameService.startNewGame(listOf("A", "B", "C", "D", "E"))
        }
        assertNull(rootService.currentGame)
        assertFalse(refreshable.refreshAfterStartGameCalled)
    }

    /** Blank and duplicate names are rejected. */
    @Test
    fun `startNewGame fails with blank or duplicate names`() {
        assertFailsWith<IllegalArgumentException> {
            rootService.gameService.startNewGame(listOf("A", " "))
        }
        assertFailsWith<IllegalArgumentException> {
            rootService.gameService.startNewGame(listOf("A", "A"))
        }
    }

    /** Starting a game writes a log message. */
    @Test
    fun `startNewGame writes a log message`() {
        rootService.gameService.startNewGame(listOf("A", "B"))

        assertTrue(refreshable.logMessages.any { it.contains("beginnt") })
    }
}