package gui
import tools.aqua.bgw.core.BoardGameApplication
import service.RootService
/**
 * Represents the main application for the SoPra board game.
 * The application initializes the [RootService] and displays the scenes.
 */
class SopraApplication : BoardGameApplication("SoPra Game") {
    /**
     * The root service instance. This is used to call service methods and access the entity layer.*/

    val rootService: RootService = RootService()
    /**
     * The main game scene displayed in the application.
     */
    private val helloScene = HelloScene()
    private val endGameScene = EndGameScene(
        onGameEnded = { showEndGameScene() },
        onBackToMenu = { showGameScene(helloScene) }
    )    /**
     * Initializes the application by displaying the [HelloScene].
     */
    /**
     * Displays the end game scene.

    init {
        rootService.addRefreshable(endGameScene)
        showGameScene(endGameScene)
    }*/

    init {
        rootService.addRefreshable(endGameScene)
        showGameScene(helloScene)
    }
    fun showEndGameScene() {
        showGameScene(endGameScene)
    }


}

