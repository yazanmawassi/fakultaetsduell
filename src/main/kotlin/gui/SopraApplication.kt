package gui

import service.Refreshable
import service.RootService
import tools.aqua.bgw.core.BoardGameApplication

/**
 * Main application: starts with the [StartScene], shows the [GameScene] as soon as a game was started.
 */
class SopraApplication : BoardGameApplication("Exzellente Informatik-Forschung"), Refreshable {

    /** The root service instance, used to call service methods and access the entity layer. */
    val rootService: RootService = RootService()

    /** Start menu for entering the faculty names. */
    private val startScene = StartScene(this)

    /** The main game scene. */
    private val gameScene = GameScene(rootService)

    init {
        rootService.addRefreshables(this, gameScene)
        showGameScene(startScene)
    }

    /**
     * Starts a new game with the given faculty names (called by the [StartScene]).
     * The [GameScene] is shown in [refreshAfterStartGame].
     *
     * @param names entered faculty names
     * @param randomOrder true: random order, false: order as entered
     * @throws IllegalArgumentException if the names are invalid
     */
    fun showMainGameScene(names: List<String>, randomOrder: Boolean = false) {
        rootService.gameService.startNewGame(names, randomOrder)
    }

    override fun refreshAfterStartGame() {
        showGameScene(gameScene)
    }
}