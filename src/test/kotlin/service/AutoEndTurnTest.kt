package service

import entity.FacultyGame
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Tests for deferred turn completion and the skipping of blocked faculties.
 */
class AutoEndTurnTest {

    private lateinit var rootService: RootService

    /** Starts a game with four faculties in fixed order. */
    @BeforeTest
    fun setUp() {
        rootService = RootService()
        rootService.gameService.startNewGame(listOf("A", "B", "C", "D"), randomOrder = false)
    }

    /** The turn does not end while action points are left. */
    @Test
    fun `turn continues while action points are left`() {
        val faculty = game().faculties[0]
        rootService.playerActionService.research(faculty.handCards.first())
        assertEquals(0, game().facultyAtTurn)
    }

    /** Exhausting AP waits for the GUI countdown before the turn is explicitly completed. */
    @Test
    fun `turn waits for countdown after last action point`() {
        val faculty = game().faculties[0]
        rootService.playerActionService.research(faculty.handCards.first())
        rootService.playerActionService.research(faculty.handCards.first())
        assertEquals(0, game().facultyAtTurn)
        assertEquals(0, faculty.actionPoints)
        rootService.gameService.endTurn()
        assertEquals(1, game().facultyAtTurn)
        assertEquals(2, game().faculties[1].actionPoints)
    }

    /** A faculty that cannot act at the start of its turn is skipped. */
    @Test
    fun `blocked faculty is skipped`() {
        game().drawStack.clear()
        game().centralLiterature.clear()
        game().faculties.forEach { f -> f.researchActivity.values.forEach { it.clear() } }
        game().faculties[1].handCards.clear()

        endTurnNow()

        assertEquals(2, game().facultyAtTurn)
        assertTrue(game().endgameTurnsLeft != 0)
    }

    private fun game(): FacultyGame = checkNotNull(rootService.currentGame)

    /** Uses up all action points of the active faculty and ends the turn. */
    private fun endTurnNow() {
        game().faculties[game().facultyAtTurn].actionPoints = 0
        rootService.gameService.endTurn()
    }
}