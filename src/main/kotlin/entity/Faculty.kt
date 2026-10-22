package entity

import java.util.Stack

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

    fun resetActionPoints(){
        actionPoints = 2
    }

    fun addResearch(research: ResearchCard){
        researchActivity[research.suit]?.push(research) // finds correct stack based on card suit and places card on top
    }

}