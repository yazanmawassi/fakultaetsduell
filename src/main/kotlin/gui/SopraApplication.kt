package gui

import tools.aqua.bgw.core.BoardGameApplication
import service.RootService

/**
 * Represents the main application for the SoPra board game.
 * The application initializes the [RootService] and displays the scenes.
 */
class SopraApplication : BoardGameApplication("SoPra Game") {

    /**
     * The root service instance. This is used to call service methods and access the entity layer.
     */
    val rootService: RootService = RootService()

    /**
     * The main game scene displayed in the application.
     */
    private val helloScene = HelloScene()

    /**
     * Initializes the application by displaying the [HelloScene].
     */
    init {
        this.showGameScene(helloScene)
    }

}

