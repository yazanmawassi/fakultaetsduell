package gui

import service.RootService
import tools.aqua.bgw.core.BoardGameApplication

/**
 * Die Hauptanwendungsklasse für das SoPra-Brettspiel.
 *
 * Diese Klasse initialisiert den zentralen [RootService], stellt die initiale [StartScene] bereit
 * und bietet die offiziellen Koordinations-Methoden für den Wechsel zwischen dem Setup-Menü,
 * dem aktiven Spielfeld und dem finalen Ergebnis-Bildschirm.
 */
class SopraApplication : BoardGameApplication("SoPra Game") {

    /** Der zentrale RootService, der von allen Szenen der Anwendung gemeinsam genutzt wird. */
    val rootService = RootService()

    /** Das Start-Menü, das zur Konfiguration der Spielernamen vor dem Spielstart dient. */
    private val startScene = StartScene(this)

    init {
        // Die Anwendung startet standardmäßig mit der Konfigurations-Ansicht (StartScene)
        showGameScene(startScene)
    }

    /**
     * Schaltet auf die Hauptspiel-Ansicht (GameScene) um, nachdem die Spieler eingerichtet wurden.
     *
     * Diese Methode initialisiert die Live-Instanz der [GameScene] dynamisch mit den eingegebenen
     * Namen, registriert das neue Spielfeld im [RootService] als [service.Refreshable] für automatische
     * UI-Updates und blendet das Spielfeld für die Benutzer ein.
     *
     * @param names Die Liste der bereinigten Spielernamen, die das Spiel starten.
     */
    fun showMainGameScene(names: List<String>) {
        println("Spieler starten das Spiel: ${names.joinToString()}")

        // 1. Instanziierung des echten, interaktiven Spielfelds (GameScene)
        val gameScene = GameScene(this)

        // 2. Registrierung der Szene im Service-Layer, damit Karten-Updates live gerendert werden
        rootService.addRefreshable(gameScene)

        // 3. Sichtbarer Wechsel des Fensters auf die aktive Spieloberfläche
        showGameScene(gameScene)
    }

    /**
     * Schaltet auf den finalen Ergebnis-Bildschirm (EndScene) um, sobald die Auswertung
     * des Semesters ansteht und die Endrunden-Bedingungen erfüllt sind.
     *
     * Erstellt eine frische Instanz der [EndScene], um die finalen Punkteberechnungen,
     * Mehrheitsboni und das endgültige Ranking fehlerfrei anzuzeigen.
     */
    fun showEndScene() {
        // 4. Instanziierung und Anzeige des Sieger- und Scoreboard-Bildschirms
        val endScene = EndScene(this)
        showGameScene(endScene)
    }
}
