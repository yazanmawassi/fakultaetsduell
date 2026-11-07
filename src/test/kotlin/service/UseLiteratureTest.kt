package service

import entity.CardSuit
import entity.CardValue
import entity.Faculty
import entity.FacultyGame
import entity.ResearchCard
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

/**
 * Test class for [PlayerActionService.useLiterature].
 * Ensures that the action correctly transfers cards from the central literature
 * to the faculty's research stacks and properly deducts Action Points (AP).
 */
class UseLiteratureTest {

    private lateinit var rootService: RootService
    private lateinit var playerActionService: PlayerActionService
    private lateinit var game: FacultyGame
    private lateinit var activeFaculty: Faculty
    private lateinit var targetCard: ResearchCard

    /**
     * Sets up a fresh game environment before each test.
     * Initializes the services, a game with two faculties, and a mock central literature.
     */
    @BeforeEach
    fun setUp() {
        rootService = RootService()
        playerActionService = PlayerActionService(rootService)

        activeFaculty = Faculty("A")
        val opponentFaculty = Faculty("B")

        game = FacultyGame(listOf(activeFaculty, opponentFaculty))
        rootService.currentGame = game

        // Set the active player and give them enough Action Points
        game.facultyAtTurn = 0
        activeFaculty.actionPoints = 2
        game.endgameTurnsLeft = -1

        // Initialize a target card and place it in the central literature
        val firstValue = CardValue.entries.first()
        targetCard = ResearchCard(CardSuit.CLUBS, firstValue)
        game.centralLiterature.add(targetCard)
    }

    /**
     * Tests the successful execution of the "use literature" action.
     * Verifies AP deduction, removal from central literature, and addition to research stacks.
     */
    @Test
    fun testUseLiteratureSuccess() {
        playerActionService.useLiterature(targetCard)

        // Verify that 2 AP were deducted
        assertEquals(0, activeFaculty.actionPoints, "Faculty should have 0 AP left after using 2 AP.")

        // Verify that the card was removed from central literature
        assertFalse(game.centralLiterature.contains(targetCard), "Card should be removed from central literature.")

        // Verify that the card was added to the matching research stack of the faculty
        val clubsStack = activeFaculty.researchActivity[CardSuit.CLUBS]
        assertNotNull(clubsStack, "Research stack for CLUBS should not be null.")
        assertEquals(targetCard, clubsStack?.peek(), "The target card should be on top of the research stack.")
    }

    /**
     * Tests that an [IllegalStateException] is thrown when no active game is running.
     */
    @Test
    fun testUseLiteratureFailsWithoutActiveGame() {
        rootService.currentGame = null

        val exception = assertThrows<IllegalStateException> {
            playerActionService.useLiterature(targetCard)
        }
        assertEquals("Es läuft kein Spiel.", exception.message)
    }

    /**
     * Tests that an [IllegalStateException] is thrown when the game has already ended.
     */
    @Test
    fun testUseLiteratureFailsWhenGameEnded() {
        game.endgameTurnsLeft = 0

        val exception = assertThrows<IllegalStateException> {
            playerActionService.useLiterature(targetCard)
        }
        assertEquals("Das Spiel ist bereits beendet.", exception.message)
    }

    /**
     * Tests that an [IllegalStateException] is thrown when the active faculty has less than 2 AP.
     */
    @Test
    fun testUseLiteratureFailsWithInsufficientAP() {
        activeFaculty.actionPoints = 1 // Only 1 AP, but 2 are needed

        val exception = assertThrows<IllegalStateException> {
            playerActionService.useLiterature(targetCard)
        }
        assertEquals("Nicht genügend Aktionspunkte. Literatur nutzen kostet 2 AP.", exception.message)
    }

    /**
     * Tests that an [IllegalArgumentException] is thrown when trying to use a card
     * that is not currently in the central literature.
     */
    @Test
    fun testUseLiteratureFailsWhenCardNotInCenter() {
        // Create a card that is NOT added to game.centralLiterature
        val invalidCard = ResearchCard(CardSuit.HEARTS, CardValue.entries.last())

        val exception = assertThrows<IllegalArgumentException> {
            playerActionService.useLiterature(invalidCard)
        }
        assertEquals("Die gewählte Karte befindet sich nicht in der zentralen Literatur.", exception.message)
    }
}