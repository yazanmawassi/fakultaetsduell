package entity

import java.util.Stack

class Faculty(val name: String)
{
    var actionPoints: Int = 0
    var researchPoints: Int = 0

    var researchActivity: MutableMap<ResearchArea, Stack<ResearchCard>> = mutableMapOf()

    var handCards: MutableList<ResearchCard> = mutableListOf()
    var publications: MutableList<ResearchCard> = mutableListOf()

    fun resetActionPoints(){
        actionPoints = 2
    }

    fun addResearch(research: ResearchCard){
        //TODO
    }

}