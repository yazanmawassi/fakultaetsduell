package service

import entity.CardSuit
import entity.Faculty
import entity.ResearchCard

/**
 * Service for all actions the active faculty can do in its turn.
 */
// TEMPORÄR: // Nur für Detekt-Warnungen für leere Methoden unterdrücken , damit ich die Klassen in Service Branch puschen kann , wir müssen das später löschen.
@Suppress("UnusedParameter", "UnusedPrivateProperty", "NotImplementedDeclaration")
class PlayerActionService(private val rootService: RootService) : AbstractRefreshingService() {

    /** Places [card] from the hand on the matching research stack ("Forschung vorantreiben"). */
    fun research(card: ResearchCard) {
    val game = checkNotNull(rootService.currentGame) {
        "Es läuft kein Spiel."
    }

    check(game.endgameTurnsLeft != 0) {
        "Das Spiel ist bereits beendet."
    }

    val faculty = game.faculties[game.facultyAtTurn]

    check(faculty.actionPoints >= 1) {
        "Nicht genügend Aktionspunkte."
    }

    require(card in faculty.handCards) {
        "Die Karte befindet sich nicht auf der Hand."
    }

    // Handkarte auf den passenden Forschungsstapel legen
    faculty.handCards.remove(card)
    faculty.addResearch(card)

    // Eine Karte zur zentralen Literatur hinzufügen
    if (game.drawStack.isNotEmpty()) {
        if (game.centralLiterature.size >= 6) {
            val oldestCard = game.centralLiterature.removeAt(0)
            game.discardStack.add(oldestCard)
        }

        val drawnCard = game.drawStack.removeLast()
        game.centralLiterature.add(drawnCard)
    }

    faculty.actionPoints -= 1

    onAllRefreshables {
        refreshAfterResearch(faculty)
    }

    onAllRefreshables {
        refreshAfterLog("${faculty.name} treibt Forschung voran.")
    }
}

    /** Draws one card from the draw stack to the hand ("Neue Ideen gewinnen"). */
    fun drawIdea() { TODO() }

    /** First step of "Strategie anpassen": discards [discard] and draws two cards. */
    fun startStrategy(discard: ResearchCard) { TODO() }

    /** Second step of "Strategie anpassen": [keep] goes to the hand, the other card is discarded. */
    fun finishStrategy(keep: ResearchCard) { TODO() }

    /** Publishes the research stack of [area] ("Publizieren"). */
    fun publish(area: CardSuit) { TODO() }

    /** Removes the top card of [target]'s stack in [area] ("Konkurrenz ausbremsen"). */
    fun sabotage(target: Faculty, area: CardSuit) { TODO() }

    /** Takes [card] from the central literature ("Literatur nutzen"). */
    fun useLiterature(card: ResearchCard) { TODO() }
}