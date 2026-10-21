package entity

class FacultyGame(
    val faculties: List<Faculty>
) {
    var facultyAtTurn: Int = 0
    var endgameTurnsLeft: Int = -1

    val drawStack: MutableList<ResearchCard> = mutableListOf()
    val discardStack: MutableList<ResearchCard> = mutableListOf()
    val centralLiterature: MutableList<ResearchCard> = mutableListOf()
}