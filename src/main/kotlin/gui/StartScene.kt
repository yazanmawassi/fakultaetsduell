package gui

import tools.aqua.bgw.components.uicomponents.Button
import tools.aqua.bgw.components.uicomponents.TextField
import tools.aqua.bgw.core.BoardGameScene
import tools.aqua.bgw.core.Color
import tools.aqua.bgw.util.Font
import tools.aqua.bgw.visual.ColorVisual
import tools.aqua.bgw.visual.ImageVisual

import tools.aqua.bgw.components.uicomponents.Label
import tools.aqua.bgw.style.BorderRadius


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

    private val nameRemoveButtons = mutableListOf<Button>()

    private val nameUpButtons = mutableListOf<Button>()
    private val nameDownButtons = mutableListOf<Button>()



    /** The lime color used for the selected option and start button. */
    private val lime = ColorVisual(197, 228, 43)

    /** The dark color used for input fields and unselected buttons. */
    private val dark = ColorVisual(14, 34, 41)

    /** The white font used for input fields and unselected buttons. */
    private val whiteFont = Font(size = 20, color = Color.WHITE)

    private val nameFieldColor = ColorVisual(27, 49, 56).apply {
        style.borderRadius = BorderRadius(8)
    }

    private val reorderHint = Label(
        width = 300,
        height = 40,
        posX = 1255,
        posY = 500,
        text = "Reihenfolge mit den Pfeilen ändern.",
        font = whiteFont
    ).apply {
        alignment = tools.aqua.bgw.core.Alignment.TOP_LEFT
    }

    /** The bold black font used for selected buttons and the start button. */
    private val blackBoldFont = Font(
        size = 22,
        color = Color.BLACK,
        fontWeight = Font.FontWeight.BOLD
    )

    /** Selects a random player order when clicked. */
    private val randomButton = Button(
        width = 289,
        height = 54,
        posX = 1260,
        posY = 404,
        text = "Zufällig",
        font = whiteFont
    )

    /** Selects the manual player order when clicked. */
    private val manualButton = Button(
        width = 285,
        height = 54,
        posX = 1560,
        posY = 404,
        text = "Manuell",
        font = whiteFont
    )

    private val randomBorder = Label(
        width = 303,
        height = 69,
        posX = 1250,
        posY = 396
    ).apply {
        visual = ColorVisual(45, 65, 70).apply {
            style.borderRadius = BorderRadius(14)
        }
    }


    private val manualBorder = Label(
        width = 303,
        height = 69,
        posX = 1550,
        posY = 396
    ).apply {
        visual = ColorVisual(45, 65, 70).apply {
            style.borderRadius = BorderRadius(14)
        }
    }



    /** Adds another player name field, up to the four-player limit. */
    private val addFacultyButton = Button(
        width = 250,
        height = 55,
        posX = 1250,
        posY = 820,
        text = "+ WEITERE FAKULTÄT",
        font = whiteFont
    ).apply {
        alignment = tools.aqua.bgw.core.Alignment.TOP_LEFT
    }

    /** Starts the game after validating the entered player names. */
    private val startButton = Button(
        width = 575,
        height = 80,
        posX = 1262,
        posY = 905,
        text = "SPIEL STARTEN         ",
        font = blackBoldFont
    )

    private val startArrow = Label(
        width = 50,
        height = 50,
        posX = 1750, // rechts im Startbutton; bei Bedarf anpassen
        posY = 919,
        text = "→",
        font = Font(size = 40, color = Color.BLACK)
    ).apply {
        visual = ColorVisual(197, 228, 43).apply {
            transparency = 0.0
        }
    }

    private val startButtonBorder = Label(
        width = 599,
        height = 99,
        posX = 1252,
        posY = 897
    ).apply {
        visual = ColorVisual(45, 65, 70).apply {
            style.borderRadius = BorderRadius(14)
        }
    }

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
            reorderHint ,
            randomBorder,
            manualBorder,
            randomButton,
            manualButton,
            addFacultyButton,
            startButtonBorder,
            startButton ,
            startArrow

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
        val rowY = 530.0 + fieldIndex * 70.0

        /** The new input field for a player's name. */
        val field = TextField(
            width = 520,
            height = 50,
            posX = 1253,
            posY = rowY ,
            text = initialText
        ).apply {
            visual = nameFieldColor
            font = whiteFont
        }

        val removeButton = Button(
            width = 40,
            height = 40,
            posX = 1720,
            posY = rowY + 7,
            text = "×",
            font = Font(size = 30, color = Color.WHITE)
        ).apply {
            visual = nameFieldColor
        }

        removeButton.onMouseClicked = {
            removeNameField(field, removeButton)
        }

        val upButton = Button(
            width = 40,
            height = 40,
            posX = 1640,
            posY = rowY + 5,
            text = "↑",
            font = Font(size = 22, color = Color.WHITE)
        ).apply {
            visual = nameFieldColor
        }

        val downButton = Button(
            width = 40,
            height = 40,
            posX = 1680,
            posY = rowY + 5,
            text = "↓",
            font = Font(size = 22, color = Color.WHITE)
        ).apply {
            visual = nameFieldColor
        }

        upButton.onMouseClicked = { moveNameField(field, -1) }
        downButton.onMouseClicked = { moveNameField(field, 1) }

        nameFields.add(field)
        nameRemoveButtons.add(removeButton)
        nameUpButtons.add(upButton)
        nameDownButtons.add(downButton)
        addComponents(field, removeButton  , upButton, downButton )

        addFacultyButton.isDisabled = nameFields.size >= 4
    }

    /**
     * Removes a player name field and all controls associated with it.
     *
     * At least two player fields must remain. After removal, the remaining rows
     * are repositioned to close the gap.
     *
     * @param field the player name field to remove
     * @param removeButton the remove button associated with [field]
     */
    private fun removeNameField(field: TextField, removeButton: Button) {
        // Mindestens zwei Namensfelder behalten
        if (nameFields.size <= 2) return

        removeComponents(field, removeButton)
        nameFields.remove(field)
        nameRemoveButtons.remove(removeButton)

        // Verbleibende Felder nach oben verschieben
        nameFields.forEachIndexed { index, nameField ->
            val rowY = 480.0 + index * 70.0
            nameField.posY = rowY
            nameRemoveButtons[index].posY = rowY + 9
        }

        addFacultyButton.isDisabled = nameFields.size >= 4
    }

    /**
     * Moves a player name field up or down by one position in the turn order.
     *
     * The field and its associated controls are swapped with the adjacent row.
     * The move is ignored if the field is already at the requested boundary.
     *
     * @param field the player name field to move
     * @param direction the number of positions to move: `-1` for up or `1` for down
     */
    private fun moveNameField(field: TextField, direction: Int) {
        val oldIndex = nameFields.indexOf(field)
        val newIndex = oldIndex + direction

        if (oldIndex == -1 || newIndex !in nameFields.indices) return

        nameFields[oldIndex] = nameFields[newIndex]
            .also { nameFields[newIndex] = nameFields[oldIndex] }

        nameRemoveButtons[oldIndex] = nameRemoveButtons[newIndex]
            .also { nameRemoveButtons[newIndex] = nameRemoveButtons[oldIndex] }

        nameUpButtons[oldIndex] = nameUpButtons[newIndex]
            .also { nameUpButtons[newIndex] = nameUpButtons[oldIndex] }

        nameDownButtons[oldIndex] = nameDownButtons[newIndex]
            .also { nameDownButtons[newIndex] = nameDownButtons[oldIndex] }

        repositionNameFields()
    }

    /**
     * Repositions every player field and its controls according to the current
     * turn order. The first row starts at y = 530, with 70 pixels between rows.
     */
    private fun repositionNameFields() {
        nameFields.forEachIndexed { index, field ->
            val rowY = 530.0 + index * 70.0

            field.posY = rowY
            nameUpButtons[index].posY = rowY + 5.0
            nameDownButtons[index].posY = rowY + 5.0
            nameRemoveButtons[index].posY = rowY + 5.0
        }
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