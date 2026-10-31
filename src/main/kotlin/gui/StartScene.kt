package gui

import tools.aqua.bgw.components.uicomponents.Button
import tools.aqua.bgw.components.uicomponents.TextField
import tools.aqua.bgw.core.BoardGameScene
import tools.aqua.bgw.core.Color
import tools.aqua.bgw.util.Font
import tools.aqua.bgw.visual.ColorVisual
import tools.aqua.bgw.visual.ImageVisual

/**
 * Displays the game setup screen.
 *
 * Players can enter between two and four names, select a turn-order option,
 * and start a game using the application.
 *
 * @property application the application that starts the game and switches scenes
 */
class StartScene(
    private val application: SopraApplication
) : BoardGameScene(width = 1920, height = 1080) {

    /**
     * The available methods for determining the player turn order.
     */
    private enum class TurnOrder {
        /** Randomizes the player order when the game starts. */
        RANDOM,

        /** Keeps players in the order in which their names were entered. */
        MANUAL
    }

    /** Stores the currently selected turn-order method. */
    private var turnOrder = TurnOrder.RANDOM

    /** Stores all player name fields currently displayed on the scene. */
    private val nameFields = mutableListOf<TextField>()

    /** The lime color used for the selected option and start button. */
    private val lime = ColorVisual(197, 228, 43)

    /** The dark color used for input fields and unselected buttons. */
    private val dark = ColorVisual(14, 34, 41)

    /** The white font used for input fields and unselected buttons. */
    private val whiteFont = Font(size = 16, color = Color.WHITE)

    /** The bold black font used for selected buttons and the start button. */
    private val blackBoldFont = Font(
        size = 16,
        color = Color.BLACK,
        fontWeight = Font.FontWeight.BOLD
    )

    /** Selects a random player order when clicked. */
    private val randomButton = Button(
        width = 292,
        height = 68,
        posX = 1225,
        posY = 396,
        text = "Zufällig",
        font = whiteFont
    )

    /** Selects the manual player order when clicked. */
    private val manualButton = Button(
        width = 343,
        height = 68,
        posX = 1517,
        posY = 396,
        text = "Manuell",
        font = whiteFont
    )

    /** Adds another player name field, up to the four-player limit. */
    private val addFacultyButton = Button(
        width = 585,
        height = 48,
        posX = 1225,
        posY = 830,
        text = "+ WEITERE FAKULTÄT",
        font = whiteFont
    )

    /** Starts the game after validating the entered player names. */
    private val startButton = Button(
        width = 625,
        height = 96,
        posX = 1225,
        posY = 895,
        text = "SPIEL STARTEN   →",
        font = blackBoldFont
    )

    /**
     * Loads the background, creates the initial player fields, adds the
     * controls, and connects their click handlers.
     */
    init {
        loadBackground()

        randomButton.visual = lime
        randomButton.font = blackBoldFont
        manualButton.visual = dark
        addFacultyButton.visual = dark
        startButton.visual = lime

        randomButton.onMouseClicked = {
            turnOrder = TurnOrder.RANDOM
            updateTurnOrderButtons()
        }

        manualButton.onMouseClicked = {
            turnOrder = TurnOrder.MANUAL
            updateTurnOrderButtons()
        }

        addFacultyButton.onMouseClicked = {
            addNameField()
        }

        startButton.onMouseClicked = {
            startGame()
        }


        addNameField("Ada")
        addNameField("Linus")

        addComponents(
            randomButton,
            manualButton,
            addFacultyButton,
            startButton
        )
    }

    /**
     * Loads the background image from the application's resources.
     *
     * The image must be located at
     * `src/main/resources/start_background.png` and should be 1920 × 1080 pixels.
     *
     * @throws IllegalStateException if the background image cannot be found
     */
    private fun loadBackground() {
        requireNotNull(javaClass.getResource("/start_background.png")) {
            "Resource /start_background.png wurde nicht gefunden"
        }

        background = ImageVisual(path = "start_background.png")
    }


    /**
     * Adds a player name field to the scene.
     *
     * @param initialText the initial text displayed in the new field
     */
    private fun addNameField(initialText: String = "") {
        if (nameFields.size >= 4) return

        /** The position index used to place the new field below existing fields. */
        val fieldIndex = nameFields.size

        /** The new input field for a player's name. */
        val field = TextField(
            width = 585,
            height = 54,
            posX = 1225,
            posY = 480 + fieldIndex * 70,
            text = initialText
        ).apply {
            visual = dark
            font = whiteFont
        }

        nameFields.add(field)
        addComponents(field)

        addFacultyButton.isDisabled = nameFields.size >= 4
    }

    /**
     * Updates the button appearance to indicate the selected turn-order option.
     */
    private fun updateTurnOrderButtons() {
        if (turnOrder == TurnOrder.RANDOM) {
            randomButton.visual = lime
            randomButton.font = blackBoldFont
            manualButton.visual = dark
            manualButton.font = whiteFont
        } else {
            manualButton.visual = lime
            manualButton.font = blackBoldFont
            randomButton.visual = dark
            randomButton.font = whiteFont
        }
    }

    /**
     * Validates the entered names, applies the selected turn order,
     * and asks the application to start the game.
     */
    private fun startGame() {
        /** The non-blank player names, with leading and trailing spaces removed. */
        val names = nameFields
            .map { it.text.trim() }
            .filter { it.isNotBlank() }

        if (names.size < 2) {
            println("Please enter at least two player names.")
            return
        }

        /** The player list in the order selected by the user. */
        val orderedNames = when (turnOrder) {
            TurnOrder.RANDOM -> names.shuffled()
            TurnOrder.MANUAL -> names
        }

        application.showMainGameScene(orderedNames)
    }
}