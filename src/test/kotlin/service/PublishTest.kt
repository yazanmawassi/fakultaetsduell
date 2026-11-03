package service

import entity.CardSuit
import entity.CardValue
import entity.ResearchCard
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Comprehensive JUnit 5 tests for the [PlayerActionService.publish] method.
 *
 * This test class covers all 5 required paths (2 success paths and 3 exception paths)
 * to achieve complete code coverage for the publication action while adhering to the
 * team's structured testing style.
 */
class PublishTest {

    private lateinit var rootService: RootService
    private lateinit var refreshable: TestRefreshable

    /**
     * Initializes a fresh [RootService] instance and registers a [TestRefreshable]
     * listener before each individual test case executes.
     */
    @BeforeTest
    fun setUp() {
        rootService = RootService()
        refreshable = TestRefreshable()
        rootService.addRefreshable(refreshable)
    }

    /**
     * Helper function to instantiate a valid [ResearchCard] for testing purposes.
     * Maps perfectly to the entity constructor requiring both a suit and a value.
     *
     * @param suit The [CardSuit] color of the card.
     * @return A freshly created [ResearchCard] instance with a default value.
     */
    private fun createCard(suit: CardSuit): ResearchCard {
        return ResearchCard(suit, CardValue.TWO)
    }

    /**
     * Test 1: Tests a standard successful publication scenario without bonus.
     * Verifies that exactly 2 AP are deducted, 3 research points are awarded for 2 cards,
     * and the stack is cleared.
     */
    @Test
    fun testPublishNormalSuccess() {
        rootService.gameService.startNewGame(listOf("A", "B"))
        val game = assertNotNull(rootService.currentGame)
        val playerA = game.faculties[0]
        val playerB = game.faculties[1]

        playerA.actionPoints = 2
        val suit = CardSuit.HEARTS
        val stackA = playerA.researchActivity[suit]

        val testStack = assertNotNull(stackA)
        testStack.add(createCard(suit))
        testStack.add(createCard(suit))

        // Player B already published here to block the first-to-publish bonus
        playerB.publications.add(createCard(suit))

        rootService.playerActionService.publish(suit)

        assertEquals(0, playerA.actionPoints, "Exactly 2 AP must be deducted.")
        assertEquals(3, playerA.researchPoints, "Exactly 3 research points must be awarded for 2 cards.")
        assertTrue(testStack.isEmpty(), "The active research stack must be cleared after publication.")
    }

    /**
     * Test 1.2: Tests a standard successful publication scenario without bonus.
     * Verifies that exactly 2 AP are deducted, 5 research points are awarded for 3 cards,
     * and the stack is cleared.
     */
    @Test
    fun testPublishSuccessWithThreeCards(){
        rootService.gameService.startNewGame(listOf("A", "B"))
        val game = assertNotNull(rootService.currentGame)
        val playerA = game.faculties[0]
        val playerB = game.faculties[1]

        playerA.actionPoints = 2
        val suit = CardSuit.HEARTS
        val stackA = playerA.researchActivity[suit]

        val testStack = assertNotNull(stackA)
        testStack.add(createCard(suit))
        testStack.add(createCard(suit))
        testStack.add(createCard(suit))


        // Player B already published here to block the first-to-publish bonus
        playerB.publications.add(createCard(suit))

        rootService.playerActionService.publish(suit)

        assertEquals(0, playerA.actionPoints, "Exactly 2 AP must be deducted.")
        assertEquals(5, playerA.researchPoints, "Exactly 5 research points must be awarded for 3 cards.")
        assertTrue(testStack.isEmpty(), "The active research stack must be cleared after publication.")

    }

    /**
     * Test 1.3: Tests a standard successful publication scenario without bonus.
     * Verifies that exactly 2 AP are deducted, 7 research points are awarded for 4 cards,
     * and the stack is cleared.
     */
    @Test
    fun testPublishSuccessWithFourCards(){
        rootService.gameService.startNewGame(listOf("A", "B"))
        val game = assertNotNull(rootService.currentGame)
        val playerA = game.faculties[0]
        val playerB = game.faculties[1]

        playerA.actionPoints = 2
        val suit = CardSuit.HEARTS
        val stackA = playerA.researchActivity[suit]

        val testStack = assertNotNull(stackA)
        testStack.add(createCard(suit))
        testStack.add(createCard(suit))
        testStack.add(createCard(suit))
        testStack.add(createCard(suit))


        // Player B already published here to block the first-to-publish bonus
        playerB.publications.add(createCard(suit))

        rootService.playerActionService.publish(suit)

        assertEquals(0, playerA.actionPoints, "Exactly 2 AP must be deducted.")
        assertEquals(7, playerA.researchPoints, "Exactly 7 research points must be awarded for 4 cards.")
        assertTrue(testStack.isEmpty(), "The active research stack must be cleared after publication.")

    }

    /**
     * Test 1.4: Tests a standard successful publication scenario without bonus.
     * Verifies that exactly 2 AP are deducted, 9 research points are awarded for 5 cards,
     * and the stack is cleared.
     */
    @Test
    fun testPublishSuccessWithFiveCards(){
        rootService.gameService.startNewGame(listOf("A", "B"))
        val game = assertNotNull(rootService.currentGame)
        val playerA = game.faculties[0]
        val playerB = game.faculties[1]

        playerA.actionPoints = 2
        val suit = CardSuit.HEARTS
        val stackA = playerA.researchActivity[suit]

        val testStack = assertNotNull(stackA)
        testStack.add(createCard(suit))
        testStack.add(createCard(suit))
        testStack.add(createCard(suit))
        testStack.add(createCard(suit))
        testStack.add(createCard(suit))


        // Player B already published here to block the first-to-publish bonus
        playerB.publications.add(createCard(suit))

        rootService.playerActionService.publish(suit)

        assertEquals(0, playerA.actionPoints, "Exactly 2 AP must be deducted.")
        assertEquals(9, playerA.researchPoints, "Exactly 9 research points must be awarded for 5 cards.")
        assertTrue(testStack.isEmpty(), "The active research stack must be cleared after publication.")

    }


    /**
     * Test 2: Tests a successful publication that triggers the First-to-Publish bonus point.
     * Verifies that the player receives 4 points instead of 3 (3 base points + 1 bonus point).
     */
    @Test
    fun testPublishSuccessWithBonus() {
        rootService.gameService.startNewGame(listOf("A", "B"))
        val game = assertNotNull(rootService.currentGame)
        val playerA = game.faculties[0]

        playerA.actionPoints = 2
        val suit = CardSuit.SPADES // Untouched area to secure the bonus
        val stackA = playerA.researchActivity[suit]

        val testStack = assertNotNull(stackA)
        testStack.add(createCard(suit))
        testStack.add(createCard(suit))

        rootService.playerActionService.publish(suit)

        assertEquals(4, playerA.researchPoints, "Should receive 4 points (3 base + 1 bonus).")
    }

    /**
     * Test 3: Verifies that the publish action fails with an [IllegalStateException]
     * if it is called before a game has been started.
     */
    @Test
    fun testPublishWithoutGameFails() {
        assertFailsWith<IllegalStateException> {
            rootService.playerActionService.publish(CardSuit.HEARTS)
        }
    }

    // --- PHASE 1 PLAYER INVALID INPUTS (Exception Paths) ---

    /**
     * Test 4: Verifies that the system throws an [IllegalArgumentException]
     * if the active player possesses fewer than the required 2 Action Points (AP).
     */
    @Test
    fun testPublishThrowsIfNotEnoughAP() {
        rootService.gameService.startNewGame(listOf("A", "B"))
        val game = assertNotNull(rootService.currentGame)
        val playerA = game.faculties[0]

        // Error state: Only 1 AP instead of 2
        playerA.actionPoints = 1

        assertFailsWith<IllegalArgumentException> {
            rootService.playerActionService.publish(CardSuit.HEARTS)
        }
    }

    /**
     * Test 5: Verifies that the system throws an [IllegalArgumentException]
     * if the selected research stack contains fewer than the required 2 cards.
     */
    @Test
    fun testPublishThrowsIfStackTooSmall() {
        rootService.gameService.startNewGame(listOf("A", "B"))
        val game = assertNotNull(rootService.currentGame)
        val playerA = game.faculties[0]

        playerA.actionPoints = 2
        val suit = CardSuit.HEARTS
        val stackA = playerA.researchActivity[suit]

        val testStack = assertNotNull(stackA)
        // Error state: Only 1 card on the stack (minimum requirement is 2)
        testStack.add(createCard(suit))

        assertFailsWith<IllegalArgumentException> {
            rootService.playerActionService.publish(suit)
        }
    }

    /**
     * Test 6 : Tests that publishing fails with an [IllegalStateException] if the game
    * turns have already reached zero (endgame state reached).
    */
    @Test
    fun testPublishFailsWhenGameIsAlreadyEnded() {
        val rootService = RootService()
        val gameService = rootService.gameService

        // 1. Start a game to set up the initial configuration state
        gameService.startNewGame(listOf("Ada", "Linus"))
        val game = checkNotNull(rootService.currentGame)

        // 2. 🟢 خدعة التست: إجبار اللعبة على الوصول لشرط النهاية (تصفير الـ Turns)
        game.endgameTurnsLeft = 0

        // 3. التحقق من أن السيرفر سيرفض النشر ويرمي الـ Exception المطلوب لحماية اللعبة
        assertFailsWith<IllegalStateException> {
            rootService.playerActionService.publish(CardSuit.CLUBS)
        }
    }
}
