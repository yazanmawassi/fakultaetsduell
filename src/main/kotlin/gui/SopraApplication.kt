package gui

import service.RootService
import tools.aqua.bgw.core.BoardGameApplication

/**
 * Main application class for the SoPra game.
 *
 * Creates the root service, displays the start scene, and provides
 * a placeholder for switching to the main game scene.
 */
class SopraApplication : BoardGameApplication("SoPra Game") {

    /** The root service shared by the application's scenes. */
    val rootService = RootService()

    /** The scene used to configure the game before it starts. */
    private val startScene = StartScene(this)

    init {
        showGameScene(startScene)
    }

    /**
     * Shows the main game scene after the players have been set up.
     *
     * This method currently prints the player names as a placeholder
     * until the main game scene is implemented.
     *
     * @param names the names of the players starting the game
     */
    fun showMainGameScene(names: List<String>) {
        println("Players: ${names.joinToString()}")
        // Display the main game scene here once it is implemented.
    }
}