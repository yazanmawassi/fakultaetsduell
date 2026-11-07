package service

import entity.CardSuit
import entity.CardValue
import entity.Faculty
import entity.ResearchCard
import org.junit.jupiter.api.Assertions.assertDoesNotThrow
import org.junit.jupiter.api.Test

/**
 * Tests the default implementations of Refreshable.
 */
class RefreshableTest {

    /** Checks that all default methods of [Refreshable] can be called without throwing an exception. */
    @Test
    fun testDefaultMethods() {
        // Verwendet die leeren Methoden aus dem Interface.
        val refreshable = object : Refreshable {}

        val faculty = Faculty("Dortmund")
        val card = ResearchCard(CardSuit.HEARTS, CardValue.ACE)

        // Keine der Standardmethoden darf einen Fehler auslösen.
        assertDoesNotThrow {
            refreshable.refreshAfterStartGame()

            refreshable.refreshAfterResearch(faculty)

            refreshable.refreshAfterDrawIdea(faculty)

            refreshable.refreshAfterStrategyStart(listOf(card))

            refreshable.refreshAfterStrategyEnd(faculty)

            refreshable.refreshAfterPublish(
                faculty,
                CardSuit.HEARTS,
                3
            )

            refreshable.refreshAfterSabotage(
                faculty,
                CardSuit.HEARTS
            )

            refreshable.refreshAfterUseLiterature(faculty, card)

            refreshable.refreshAfterTurnChange(faculty)

            refreshable.refreshAfterGameEnd(emptyList())

            refreshable.refreshAfterLog("Testnachricht")
        }
    }
}