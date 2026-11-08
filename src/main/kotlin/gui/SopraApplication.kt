
package gui

import service.Refreshable
import service.RootService
import service.FinalScore
import tools.aqua.bgw.core.BoardGameApplication

/**
 * Main application managing the start, game and end scenes.
 */
class SopraApplication :
    BoardGameApplication("Exzellente Informatik-Forschung"),
    Refreshable {

    /** Central service managing the game logic. */
    val rootService: RootService = RootService()

    /** Start menu for entering faculty names. */
    private val startScene = StartScene(this)

    /** Main game scene. */
    private val gameScene = GameScene(rootService)

    /** End scene displaying the final scores. */
    private val endGameScene = EndGameScene(
        onGameEnded = { showEndGameScene() },
        onBackToMenu = {
            rootService.currentGame = null
            startScene.clearFields()
            showGameScene(startScene) }
    )

    init {
        rootService.addRefreshables(
            this,
            gameScene,
            endGameScene
        )

        showGameScene(startScene)
    }

    /**
     * Starts a new game with the selected faculty names.
     */
    fun showMainGameScene(
        names: List<String>,
        randomOrder: Boolean = false
    ) {
        rootService.gameService.startNewGame(
            names,
            randomOrder
        )
    }

    /**
     * Displays the game scene after the game starts.
     */
    override fun refreshAfterStartGame() {
        showGameScene(gameScene)
    }

    /**
     * Displays the end scene when the game finishes.
     */
    override fun refreshAfterGameEnd(scores: List<FinalScore>) {
        showEndGameScene()
    }

    /**
     * Switches to the final result scene.
     */
    fun showEndGameScene() {
        showGameScene(endGameScene)
    }
}
