package entity

/**
 * Represents the whole game state of "Exzellente Informatik-Forschung".
 *
 * @property faculties the 2 to 4 participating faculties in turn order
 */
class FacultyGame(
    val faculties: List<Faculty>
) {
    /** Index in [faculties] of the faculty whose turn it is. */
    var facultyAtTurn: Int = 0

    /** Remaining turns in the final round, -1 if the final round has not started yet. */
    var endgameTurnsLeft: Int = -1

    /** Cards that can still be drawn. */
    val drawStack: MutableList<ResearchCard> = mutableListOf()

    /** Cards that were discarded. */
    val discardStack: MutableList<ResearchCard> = mutableListOf()

    /** Open cards in the middle that every faculty can use (max. 6, oldest first). */
    val centralLiterature: MutableList<ResearchCard> = mutableListOf()

    /** Passes the turn to the next faculty; after the last one, the first one starts again. */
    fun nextFaculty() {
        facultyAtTurn = (facultyAtTurn + 1) % faculties.size
    }
}