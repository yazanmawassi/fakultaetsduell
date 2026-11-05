package gui

import service.FinalScore
import service.Refreshable
import tools.aqua.bgw.core.BoardGameScene
import tools.aqua.bgw.visual.ImageVisual
import tools.aqua.bgw.components.uicomponents.Label
import tools.aqua.bgw.util.Font
import tools.aqua.bgw.core.Color
import tools.aqua.bgw.components.uicomponents.Button
import tools.aqua.bgw.components.uicomponents.TextArea
import tools.aqua.bgw.components.layoutviews.Pane
import tools.aqua.bgw.visual.ColorVisual
import tools.aqua.bgw.core.Alignment
import tools.aqua.bgw.components.ComponentView

/**
 * Displays the end screen and final results.
 */
class EndGameScene(
    private val onGameEnded: () -> Unit = {},
    private val onBackToMenu: () -> Unit = {},
    private val onLogRequested: () -> List<String> = { emptyList() }
) : BoardGameScene(
    width = 1920,
    height = 1080
), Refreshable {

    private val gapCover = Label(
        posX = 810,
        posY = 945,
        width = 20,
        height = 85,
        text = "",
        visual = ColorVisual(24, 33, 37)
    )

    private val showLogsButton = Button(
        posX = 690,
        posY = 945,
        width = 120,
        height = 85,
        text = "LOGS",
        font = Font(20, Color(19, 35, 43), fontWeight = Font.FontWeight.BOLD),
        visual = ColorVisual(185, 230, 59)
    ).apply {
        onMouseClicked = { openLogOverlay() }
        onMouseEntered = {
            visual = ColorVisual(228, 255, 150)
        }
        onMouseExited = {
            visual = ColorVisual(185, 230, 59)
        }
    }

    private val backToMenuButton = Button(
        posX = 830,
        posY = 945,
        width = 400,
        height = 85,
        text = "ZUM HAUPTMENÜ                                              ➔",
        font = Font(20, Color(19, 35, 43), fontWeight = Font.FontWeight.BOLD),
        visual = ColorVisual(185, 230, 59)
    ).apply {
        onMouseClicked = {
            onBackToMenu()
        }
        onMouseEntered = {
            visual = ColorVisual(228, 255, 150)
        }
        onMouseExited = {
            visual = ColorVisual(185, 230, 59)
        }
    }

    private val congratsLabel = Label(
        posX = 560,
        posY = 322,
        width = 800,
        height = 70,
        text = "Glückwunsch",
        font = Font(size = 56, color = Color.WHITE, fontWeight = Font.FontWeight.BOLD)
    )

    private val winnerLabel = Label(
        posX = 310,
        posY = 392,
        width = 1300,
        height = 80,
        text = "",
        font = Font(size = 56, color = Color(185, 230, 59), fontWeight = Font.FontWeight.BOLD)
    )

    private val resultColor = Color(185, 230, 59)
    private val darkBackground = ColorVisual(19, 45, 49)
    private val overlayBackground = ColorVisual(5, 18, 22, 0.85)

    private val logOverlay = Pane<ComponentView>(
        posX = 0, posY = 0, width = 1920, height = 1080, visual = overlayBackground
    ).apply { isVisible = false }

    private val logPanel = Pane<ComponentView>(
        posX = 560, posY = 140, width = 800, height = 800, visual = darkBackground
    )

    private val logOverlayTitle = Label(
        posX = 0, posY = 30, width = 800, height = 50, text = "SPIEL-LOGS",
        font = Font(32, Color.WHITE, fontWeight = Font.FontWeight.BOLD)
    )

    private val logTextArea = TextArea(
        posX = 50, posY = 100, width = 700, height = 650, text = "",
        font = Font(16, Color.WHITE), visual = ColorVisual(13, 43, 43)
    ).apply {
        isReadonly = true
    }

    private val closeLogButton = Button(
        posX = 20, posY = 20, width = 50, height = 50, text = "X",
        font = Font(28, Color.WHITE, fontWeight = Font.FontWeight.BOLD),
        visual = ColorVisual.TRANSPARENT
    ).apply {
        onMouseClicked = { logOverlay.isVisible = false }
    }

    init {
        background = ImageVisual("Endbild.png")
        addComponents(gapCover, backToMenuButton, showLogsButton)
        addComponents(congratsLabel, winnerLabel)

        logPanel.add(logOverlayTitle)
        logPanel.add(logTextArea)
        logPanel.add(closeLogButton)
        logOverlay.add(logPanel)
        addComponents(logOverlay)
    }

    private fun openLogOverlay() {
        val logs = onLogRequested()
        if (logs.isEmpty()) {
            logTextArea.text = "Keine Logs verfügbar."
        } else {
            logTextArea.text = logs.joinToString("\n")
        }
        logOverlay.isVisible = true
    }

    /**
     * Receives the final scores after the game ends.
     */

    /**
     * Displays the final ranking when the game ends.
     */
    override fun refreshAfterGameEnd(scores: List<FinalScore>) {
        onGameEnded()

        val sortedScores = scores.sortedByDescending { it.total }

        val topScore = sortedScores.firstOrNull()?.total
        val winners = sortedScores.filter { it.total == topScore }
        winnerLabel.text = winners.joinToString(" & ") { it.faculty.name } + "!"

        sortedScores.forEachIndexed { index, score ->

            val yPosition = 650 + index * 65

            val rankLabel = Label(
                posX = 510,
                posY = yPosition,
                width = 100,
                height = 45,
                text = "${index + 1}.",
                font = Font(
                    size = 28,
                    color = Color(185, 230, 59),
                    fontWeight = Font.FontWeight.BOLD
                )
            )

            val facultyLabel = Label(
                posX = 660,
                posY = yPosition,
                width = 480,
                height = 45,
                text = score.faculty.name,
                font = Font(
                    size = 28,
                    color = Color(185, 230, 59),
                    fontWeight = Font.FontWeight.BOLD
                )
            )

            val pointsLabel = Label(
                posX = 1190,
                posY = yPosition,
                width = 160,
                height = 45,
                text = score.total.toString(),
                font = Font(
                    size = 28,
                    color = Color(185, 230, 59),
                    fontWeight = Font.FontWeight.BOLD
                )
            )

            addComponents(rankLabel, facultyLabel, pointsLabel)
        }

        logOverlay.toFront()
    }
}