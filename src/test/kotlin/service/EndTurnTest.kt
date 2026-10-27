package service

import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Tests for [GameService.endTurn], including the end of the game.
 */
class EndTurnTest {

    private lateinit var rootService: RootService
    private lateinit var refreshable: TestRefreshable

    /** Fresh RootService with a registered [TestRefreshable] before every test. */
    @BeforeTest
    fun setUp() {
        rootService = RootService()
        refreshable = TestRefreshable()
        rootService.addRefreshable(refreshable)
    }

    /** Without a running game, endTurn is not allowed. */
    @Test
    fun `endTurn without game fails`() {
        assertFailsWith<IllegalStateException> { rootService.gameService.endTurn() }
    }

    /** The next faculty gets the turn and 2 new action points. */
    @Test
    fun `endTurn switches to next faculty and resets action points`() {
        rootService.gameService.startNewGame(listOf("A", "B"))
        val game = assertNotNull(rootService.currentGame)
        game.faculties[1].actionPoints = 0
        refreshable.reset()

        rootService.gameService.endTurn()

        assertEquals(1, game.facultyAtTurn)
        assertEquals(2, game.faculties[1].actionPoints)
        assertTrue(refreshable.refreshAfterTurnChangeCalled)
        assertTrue(refreshable.logMessages.any { it.contains("ist am Zug") })
        assertNull(refreshable.finalScores)
    }

    /** After the last faculty, the first one is at turn again. */
    @Test
    fun `endTurn after last faculty starts again with first`() {
        rootService.gameService.startNewGame(listOf("A", "B"))
        val game = assertNotNull(rootService.currentGame)

        rootService.gameService.endTurn()
        rootService.gameService.endTurn()

        assertEquals(0, game.facultyAtTurn)
    }

    /** When the draw stack is empty, every other faculty plays once more, then the game ends. */
    @Test
    fun `game ends after every other faculty had one more turn`() {
        rootService.gameService.startNewGame(listOf("A", "B", "C"))
        val game = assertNotNull(rootService.currentGame)
        game.drawStack.clear()

        rootService.gameService.endTurn()
        assertEquals(1, game.facultyAtTurn)
        assertEquals(2, game.endgameTurnsLeft)
        assertNull(refreshable.finalScores)

        rootService.gameService.endTurn()
        assertEquals(2, game.facultyAtTurn)
        assertNull(refreshable.finalScores)

        rootService.gameService.endTurn()
        assertNotNull(refreshable.finalScores)
    }

    /** After the game has ended, endTurn is not allowed anymore. */
    @Test
    fun `endTurn after game end fails`() {
        rootService.gameService.startNewGame(listOf("A", "B"))
        val game = assertNotNull(rootService.currentGame)
        game.drawStack.clear()
        game.endgameTurnsLeft = 1
        rootService.gameService.endTurn()

        assertFailsWith<IllegalStateException> { rootService.gameService.endTurn() }
    }
}