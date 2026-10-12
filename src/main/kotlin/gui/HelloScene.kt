package gui

import service.Refreshable
import service.RootService
import tools.aqua.bgw.components.uicomponents.Label
import tools.aqua.bgw.core.BoardGameScene
import tools.aqua.bgw.util.Font
import tools.aqua.bgw.visual.ColorVisual

/**
 * Represents an example scene with a greeting label.
 * This scene has a default size of 1920x1080 pixels and a green background.
 */
class HelloScene : BoardGameScene(), Refreshable {

    /**
     * A label displaying the text "Hello, SoPra!" centered within the scene.
     */
    private val helloLabel = Label(
        width = 1920,
        height = 1080,
        posX = 0,
        posY = 0,
        text = "Hello, SoPra!",
        font = Font(size = 96)
    )

    /**
     * Initializes the scene by setting the background color and adding the label.
     */
    init {
        background = ColorVisual(108, 168, 59)
        addComponents(helloLabel)
    }

}