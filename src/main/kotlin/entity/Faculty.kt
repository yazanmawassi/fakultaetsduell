package entity

import java.util.Stack


/**
 * Represents a playable team or department ("Fakultät") within the game context.
 *
 * Each faculty manages its own resources, tracking its available Action Points (AP) per turn,
 * accumulated research points, active secret hand cards, secured publications, and the progress
 * of active research topics across various card suits.
 *
 * @property name The unique identifying name of this faculty (e.g., "Faculty A").
 */
class Faculty(val name: String)
{
    var actionPoints: Int = 0
    var researchPoints: Int = 0

    val researchActivity: MutableMap<CardSuit, Stack<ResearchCard>> = mutableMapOf()
    val handCards: MutableList<ResearchCard> = mutableListOf()
    val publications: MutableList<ResearchCard> = mutableListOf()

    // initialize map with empty stacks
    init {
        CardSuit.entries.forEach { suit ->
            researchActivity[suit] = Stack<ResearchCard>()
        }
    }

    /**
     * Resets the faculty's action pool to its standard maximum baseline capacity
     * of exactly 2 Action Points (AP) at the beginning of their turn.
     */
    fun resetActionPoints(){
        actionPoints = 2
    }

    /**
     * Appends a new [ResearchCard] onto the appropriate tracking stack based on its internal suit category.
     * Automatically retrieves the matching [Stack] from [researchActivity] and pushes the card to the top.
     *
     * @param research The [ResearchCard] instance to be placed on the corresponding research stack.
     */
    fun addResearch(research: ResearchCard){
        researchActivity[research.suit]?.push(research) // finds correct stack based on card suit and places card on top
    }

}