package gui

import service.FinalScore
import service.Refreshable
import tools.aqua.bgw.core.BoardGameScene
import tools.aqua.bgw.visual.ImageVisual
import tools.aqua.bgw.components.uicomponents.Label
import tools.aqua.bgw.util.Font
import tools.aqua.bgw.core.Color
import tools.aqua.bgw.components.uicomponents.Button
import tools.aqua.bgw.visual.ColorVisual
/**
 * Displays the end screen and final results.
 */
class EndGameScene(
    private val onGameEnded: () -> Unit = {},
    private val onBackToMenu: () -> Unit = {}
) : BoardGameScene(
    width = 1920,
    height = 1080
), Refreshable {
    private val backToMenuButton = Button(
        posX = 690,
        posY = 945,
        width = 540,
        height = 85,
        text = "",
        visual = ColorVisual.TRANSPARENT
    ).apply {
        onMouseClicked = {
            onBackToMenu()
        }
    }
    private val resultColor = Color(185, 230, 59)
    init {
        background = ImageVisual("Endbild.png")
        addComponents(backToMenuButton)
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

        sortedScores.forEachIndexed { index, score ->

            val yPosition = 650 + index * 65

            // Ranking position
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

            // Faculty name
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

            // Total points
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
    }
}

