package service

import entity.Faculty
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

/**
 * This class tests the functionality of the
 * [FinalScore] data class.
 */
class FinalScoreTest {

    /**
     * This faculty is initialized in the
     * [setUp] function hence it is a late-initialized property.
     */
    private lateinit var testFaculty: Faculty

    /**
     * This finalScore is initialized in the
     * [setUp] function hence it is a late-initialized property.
     */
    private lateinit var testFinalScore: FinalScore

    /**
     * Initialize service to set up the test environment. This function is executed before every test.
     */
    @BeforeEach
    fun setUp() {
        // The Faculty class only requires a string name in the constructor
        testFaculty = Faculty("Informatik")

        testFinalScore = FinalScore(
            faculty = testFaculty,
            publicationPoints = 15,
            majorityBonus = 5,
            breadthBonus = 3,
            focusBonus = 2,
            unpublishedPoints = 4,
            total = 29
        )
    }

    /**
     * Tests if the constructor correctly initializes all properties and the getters return the expected values.
     */
    @Test
    fun testProperties() {
        assertEquals(testFaculty, testFinalScore.faculty)
        assertEquals(15, testFinalScore.publicationPoints)
        assertEquals(5, testFinalScore.majorityBonus)
        assertEquals(3, testFinalScore.breadthBonus)
        assertEquals(2, testFinalScore.focusBonus)
        assertEquals(4, testFinalScore.unpublishedPoints)
        assertEquals(29, testFinalScore.total)
    }

    /**
     * Tests the auto-generated data class methods: equals, hashCode, toString, and copy.
     */
    @Test
    fun testDataClassMethods() {
        // tests copy()
        val copiedScore = testFinalScore.copy(total = 30)
        assertEquals(30, copiedScore.total)
        assertEquals(testFinalScore.faculty, copiedScore.faculty)
        assertEquals(testFinalScore.publicationPoints, copiedScore.publicationPoints)

        // tests equals() und hashCode()
        val identicalScore = testFinalScore.copy()
        val differentScore = testFinalScore.copy(publicationPoints = 0)

        assertEquals(testFinalScore, identicalScore)
        assertEquals(testFinalScore.hashCode(), identicalScore.hashCode())

        assertNotEquals(testFinalScore, differentScore)
        assertNotEquals(testFinalScore.hashCode(), differentScore.hashCode())
        assertNotEquals(testFinalScore, null)
        assertNotEquals(testFinalScore, "Ein anderer Typ")

        // tests toString()
        val toStringResult = testFinalScore.toString()
        assertTrue(toStringResult.contains("FinalScore"))
        assertTrue(toStringResult.contains("publicationPoints=15"))
        assertTrue(toStringResult.contains("total=29"))
    }
}