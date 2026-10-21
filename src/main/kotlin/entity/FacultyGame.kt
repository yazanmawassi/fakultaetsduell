package entity

class FacultyGame(
    val faculties: List<Faculty>
) {
    var facultyAtTurn: Int = 0  // Index der aktuellen Fakultät
    var endgameTurnsLeft: Int = -1

    val drawStack: MutableList<ResearchCard> = mutableListOf()
    val discardStack: MutableList<ResearchCard> = mutableListOf()
    val centralLiterature: MutableList<ResearchCard> = mutableListOf()

    fun nextFaculty() {
    facultyAtTurn = (facultyAtTurn + 1) % faculties.size
    }
}