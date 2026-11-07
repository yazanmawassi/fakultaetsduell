package gui
import entity.CardSuit
import entity.Faculty
import entity.ResearchCard
import service.Refreshable
import service.RootService
import service.FinalScore
import tools.aqua.bgw.animation.DelayAnimation
import tools.aqua.bgw.animation.FadeAnimation
import tools.aqua.bgw.components.ComponentView
import tools.aqua.bgw.components.layoutviews.Pane
import tools.aqua.bgw.components.uicomponents.Button
import tools.aqua.bgw.components.uicomponents.Label
import tools.aqua.bgw.core.Alignment
import tools.aqua.bgw.core.BoardGameApplication
import tools.aqua.bgw.core.BoardGameScene
import tools.aqua.bgw.core.Color
import tools.aqua.bgw.util.Font
import tools.aqua.bgw.visual.ColorVisual
import tools.aqua.bgw.visual.Visual
import tools.aqua.bgw.components.uicomponents.TextArea
import java.util.Timer
import java.util.TimerTask
import kotlin.math.abs

/**
 * Main game screen: faculties and the action "Strategie anpassen" on the left,
 * opponents, central literature, own research and hand in the middle,
 * action points, log and "Zug beenden" on the right.
 *
 * Actions are animated: cards fly to their target, points float up after publishing and the
 * hand fans out while choosing a card for "Strategie anpassen". The small animations run on a
 * background timer and change the components on the GUI thread. The turn overlay waits until
 * the animation of the last action is finished and only then locks the input.
 */
@Suppress("TooManyFunctions") // Refreshable callbacks keep the scene in sync with service events.
class GameScene(private val rootService: RootService) :
    BoardGameScene(1920, 1080, background = ColorVisual(13, 43, 43)), Refreshable {

    /** What a click on a hand card currently means. */
    private enum class Mode { NORMAL, STRATEGY_DISCARD, STRATEGY_KEEP }

    /** A position on the scene (absolute coordinates). */
    private data class Point(val x: Double, val y: Double)

    private val cardImages = CardImageLoader()
    private var mode = Mode.NORMAL
    private var turnsPlayed = 0
    private var turnTransitionActive = false
    private var switchingTurnAfterCountdown = false
    private var transitionGeneration = 0
    private val logMessages = mutableListOf<String>()
    private var pointsAtTurnStart = 0
    private val actionsThisTurn = mutableListOf<String>()

    /** Flying cards and floating texts; removed when their animation ends or when the turn overlay appears. */
    private val effectLabels = mutableListOf<Label>()

    /** How long the animation of the current action takes; the turn overlay waits for it. */
    private var pendingEffectMillis = 0

    /** Background timer for the small GUI animations (cards flying, points floating up, fades). */
    private val effectTimer = Timer("gui-effects", true)

    // ===================== Farben & Schriften =====================

    private val white = Color(255, 255, 255)
    private val dark = Color(19, 35, 43)
    private val grey = Color(110, 125, 130)
    private val muted = Color(160, 185, 185)
    private val lime = Color(200, 255, 60)
    private val pointsGreen = Color(46, 125, 50)
    private val sabotageRed = Color(211, 47, 47)

    private val cardVisual = ColorVisual(17, 52, 52)
    private val activeCardVisual = ColorVisual(28, 72, 56)
    private val boxVisual = ColorVisual(13, 43, 43)
    private val lightVisual = ColorVisual(250, 252, 251)
    private val paperVisual = ColorVisual(255, 255, 255)

    // Hover-Farben
    private val sabotageHoverVisual = ColorVisual(255, 214, 214)
    private val strategyHoverVisual = ColorVisual(40, 95, 70)
    private val strategyDisabledVisual = ColorVisual(12, 32, 34)
    private val endTurnVisual = ColorVisual(200, 255, 60)
    private val endTurnHoverVisual = ColorVisual(228, 255, 150)
    private val endTurnDisabledVisual = ColorVisual(70, 95, 95)

    // Strategie-Modus: Glow um die Handkarten
    private val glowOuterVisual = ColorVisual(255, 214, 0, 0.25)
    private val glowInnerVisual = ColorVisual(255, 214, 0, 0.6)

   //private fun font(size: Int, color: Color = white, bold: Boolean = false) =
     //   Font(size = size, color = color, fontWeight = if (bold) Font.FontWeight.BOLD else Font.FontWeight.NORMAL)

    // ===================== Kopf =====================

    private val logo = Label(
        posX = 20, posY = 18, width = 46, height = 46, text = "♦♥\n♠♣",
        font = font(13, white, true), isWrapText = true, visual = cardVisual
    )

    private val titleSopra = Label(
        posX = 78, posY = 22, width = 100, height = 38, text = "SOPRA",
        font = font(24, lime, true), alignment = Alignment.CENTER_LEFT
    )

    private val titleYear = Label(
        posX = 178, posY = 22, width = 120, height = 38, text = "2026",
        font = font(24, white, true), alignment = Alignment.CENTER_LEFT
    )

    private val roundLabel = Label(
        posX = 760, posY = 22, width = 220, height = 40, text = "", font = font(18, white, true)
    )

    private val drawStackTopLabel = Label(
        posX = 980, posY = 22, width = 320, height = 40, text = "", font = font(18, white, true)
    )

    // ===================== Links =====================

    private val facultiesTitle = Label(
        posX = 20, posY = 90, width = 280, height = 25, text = "FAKULTÄTEN",
        font = font(14, muted, true), alignment = Alignment.CENTER_LEFT
    )

    private val facultiesPane = Pane<ComponentView>(posX = 20, posY = 125, width = 280, height = 735)

    private val divider = Label(posX = 20, posY = 872, width = 280, height = 1, visual = ColorVisual(40, 80, 80))

    private val actionTitle = Label(
        posX = 20, posY = 885, width = 280, height = 25, text = "AKTION",
        font = font(14, muted, true), alignment = Alignment.CENTER_LEFT
    )

    private val strategyButton = Button(
        posX = 20, posY = 920, width = 280, height = 72, text = "", visual = cardVisual
    ).apply { onMouseClicked = { onStrategyClicked() } }

    private val strategyName = Label(
        posX = 20, posY = 920, width = 280, height = 72, text = "Strategie anpassen",
        font = font(17, white, true), alignment = Alignment.CENTER
    ).apply { onMouseClicked = { onStrategyClicked() } }

    // ===================== Mitte =====================

    private val centerArea = Label(
        posX = 320, posY = 80, width = 1220, height = 1000, visual = ColorVisual(220, 243, 236)
    )

    private val opponentsPane = Pane<ComponentView>(posX = 320, posY = 95, width = 1220, height = 90)

    private val literatureBox = Label(posX = 500, posY = 200, width = 880, height = 250, visual = lightVisual)

    private val literatureTitle = Label(
        posX = 525, posY = 215, width = 400, height = 25, text = "ZENTRALE LITERATUR",
        font = font(18, dark, true), alignment = Alignment.CENTER_LEFT
    )

    private val literatureSubtitle = Label(
        posX = 525, posY = 242, width = 400, height = 20, text = "Öffentlich: älteste links",
        font = font(13, grey), alignment = Alignment.CENTER_LEFT
    )

    private val literatureCount = Label(
        posX = 1180, posY = 215, width = 180, height = 25, text = "",
        font = font(14, dark, true), alignment = Alignment.CENTER_RIGHT
    )

    private val literaturePane = Pane<ComponentView>(posX = 525, posY = 280, width = 680, height = 150)

    /** Draw stack that allows the player to gain a new idea when permitted. */
    private val drawStackPane = Pane<ComponentView>(
        posX = 1225, posY = 272, width = 135, height = 165
    ).apply { onMouseClicked = { onDrawStackClicked() } }

    private val drawStackCaption = Label(
        posX = 1215, posY = 432, width = 155, height = 18, text = "NACHZIEHSTAPEL",
        font = font(11, dark, true)
    )

    private val researchTitle = Label(
        posX = 360, posY = 470, width = 400, height = 25, text = "DEINE FORSCHUNG",
        font = font(18, dark, true), alignment = Alignment.CENTER_LEFT
    )

    private val researchSubtitle = Label(
        posX = 360, posY = 497, width = 500, height = 20, text = "Offene, noch nicht publizierte Ergebnisse",
        font = font(13, grey), alignment = Alignment.CENTER_LEFT
    )

    private val researchHint = Label(
        posX = 1100, posY = 497, width = 400, height = 20, text = "Stapel anklicken, um zu publizieren.",
        font = font(13, grey), alignment = Alignment.CENTER_RIGHT
    )

    private val researchColumns = SUITS.indices.map { i ->
        Pane<ComponentView>(posX = 360 + i * 292, posY = 530, width = 280, height = 195, visual = lightVisual)
            .apply {
                // US "Publizieren":
                onMouseClicked = { runAction { rootService.playerActionService.publish(SUITS[i]) } }
            }
    }

    private val handTitle = Label(
        posX = 680, posY = 745, width = 500, height = 25, text = "", font = font(15, dark, true)
    )

    private val handPane = Pane<ComponentView>(posX = 320, posY = 775, width = HAND_PANE_WIDTH, height = 175)

    private val infoLabel = Label(
        posX = 630, posY = 975, width = 600, height = 40, text = "", font = font(14, dark), visual = lightVisual
    )

    private val strategyPane = Pane<ComponentView>(
        posX = 690, posY = 300, width = 560, height = 230, visual = paperVisual
    ).apply { isVisible = false }

    /** Dims the board above the hand while "Strategie anpassen" is running; a click cancels it. */
    private val spotlightOverlay = Label(
        posX = 320, posY = 80, width = 1220, height = 655, visual = ColorVisual(0, 0, 0, 0.5)
    ).apply {
        isVisible = false
        onMouseClicked = { if (mode == Mode.STRATEGY_DISCARD) onStrategyClicked() }
    }

    /** Dims the public table while the transition panel is visible. Private cards are hidden separately. */
    private val turnOverlay = Pane<ComponentView>(
        posX = 0, posY = 0, width = 1920, height = 1080, visual = ColorVisual(5, 18, 22, 0.65)
    ).apply { isVisible = false }

    private val turnOverlayPanel = Pane<ComponentView>(
        posX = 560, posY = 300, width = 800, height = 480, visual = ColorVisual(19, 45, 49)
    )

    private val turnOverlayTitle = Label(
        posX = 460, posY = 200, width = 1000, height = 70,
        text = "Dein Zug ist beendet!", font = font(42, white, true)
    )

    private val turnOverlaySummaryTitle = Label(
        posX = 360, posY = 300, width = 1200, height = 40,
        text = "Aktionen in diesem Zug:", font = font(28, white)
    )

    private val turnOverlaySummaryText = Label(
        posX = 360, posY = 350, width = 1200, height = 80,
        text = "", font = font(24, white), isWrapText = true
    )

    private val turnOverlayPoints = Label(
        posX = 460, posY = 460, width = 1000, height = 50,
        text = "", font = font(32, lime, true)
    )

    private val turnOverlayNextTitle = Label(
        posX = 360, posY = 580, width = 1200, height = 40,
        text = "", font = font(28, white)
    )

    private val turnOverlayNextName = Label(
        posX = 360, posY = 630, width = 1200, height = 50,
        text = "", font = font(38, lime, true)
    )

    private val turnOverlayCountdown = Label(
        posX = 760, posY = 740, width = 400, height = 120,
        text = "", font = font(72, lime, true)
    )

    // ===================== Rechts =====================

    private val apBox = Label(posX = 1565, posY = 100, width = 330, height = 80, visual = cardVisual)

    private val apTitle = Label(
        posX = 1580, posY = 106, width = 300, height = 25, text = "AKTIONSPUNKTE",
        font = font(14, muted, true), alignment = Alignment.CENTER_LEFT
    )

    private val apValue = Label(
        posX = 1580, posY = 134, width = 300, height = 35, text = "",
        font = font(20, lime, true), alignment = Alignment.CENTER_LEFT
    )

    private val logTitle = Label(
        posX = 1565, posY = 200, width = 330, height = 25, text = "LOGS",
        font = font(14, muted, true), alignment = Alignment.CENTER_LEFT
    )

    private val logLabel = TextArea(
        posX = 1565, posY = 230, width = 330, height = 640, text = "",
        font = font(13, white), visual = cardVisual
    ).apply {
        isReadonly = true
    }

    private val endTurnButton = Button(
        posX = 1565, posY = 900, width = 330, height = 70, text = "ZUG BEENDEN  →",
        font = font(20, dark, true), visual = endTurnVisual
    ).apply { onMouseClicked = { runAction { rootService.gameService.endTurn() } } }

    init {
        addComponents(
            centerArea,
            logo, titleSopra, titleYear, roundLabel, drawStackTopLabel,
            facultiesTitle, facultiesPane, divider, actionTitle,
            strategyButton, strategyName,
            opponentsPane, literatureBox, literatureTitle, literatureSubtitle, literatureCount,
            literaturePane, drawStackPane, drawStackCaption, researchTitle, researchSubtitle, researchHint,
            handTitle, handPane, infoLabel,
            apBox, apTitle, apValue, logTitle, logLabel, endTurnButton
        )
        researchColumns.forEach { addComponents(it) }
        // The spotlight lies above the board, the strategy window above the spotlight.
        addComponents(spotlightOverlay, strategyPane)
        turnOverlay.add(turnOverlayTitle)
        turnOverlay.add(turnOverlaySummaryTitle)
        turnOverlay.add(turnOverlaySummaryText)
        turnOverlay.add(turnOverlayPoints)
        turnOverlay.add(turnOverlayNextTitle)
        turnOverlay.add(turnOverlayNextName)
        turnOverlay.add(turnOverlayCountdown)
        addComponents(turnOverlay)
        setUpHoverEffects()
    }

    /** Hover effects for the elements that exist the whole time (not rebuilt in [redraw]). */
    private fun setUpHoverEffects() {
        liftOnHover(drawStackPane) { canDrawIdea() }

        researchColumns.forEachIndexed { i, column ->
            highlightOnHover(column, suitLightVisual(SUITS[i]), { lightVisual }) { canPublish(SUITS[i]) }
        }

        strategyName.onMouseEntered = {
            if (canStartStrategy()) strategyButton.visual = strategyHoverVisual
        }
        strategyName.onMouseExited = { updateStrategyButton() }

        endTurnButton.onMouseEntered = {
            if (!endTurnButton.isDisabled) {
                endTurnButton.visual = endTurnHoverVisual
                endTurnButton.scale = BUTTON_HOVER_SCALE
            }
        }

        endTurnButton.onMouseExited = {
            endTurnButton.visual = if (endTurnButton.isDisabled) endTurnDisabledVisual else endTurnVisual
            endTurnButton.scale = 1.0
        }
    }


    private fun font(size: Int, color: Color = white, bold: Boolean = false) =
        Font(size = size, color = color, fontWeight = if (bold) Font.FontWeight.BOLD else Font.FontWeight.NORMAL)

    // ===================== Refreshable =====================

    override fun refreshAfterStartGame() {
        cancelTurnTransition()
        mode = Mode.NORMAL
        turnsPlayed = 0
        strategyPane.isVisible = false
        logMessages.clear()
        logLabel.text = ""
        actionsThisTurn.clear()
        pointsAtTurnStart = rootService.currentGame?.faculties?.get(0)?.researchPoints ?: 0

        val game = rootService.currentGame
        val firstFaculty = game?.faculties?.get(0)

        addLog("SPIEL GESTARTET ")
        if (firstFaculty != null) {
            addLog("═══ ${firstFaculty.name} BEGINNT ═══")
        }
        redraw()
    }

    override fun refreshAfterResearch(faculty: Faculty) {
        actionsThisTurn.add("Forschung vorangetrieben")
        redraw()
    }

    override fun refreshAfterDrawIdea(faculty: Faculty) {
        actionsThisTurn.add("Neue Idee gewonnen")
        redraw()
    }

    override fun refreshAfterPublish(faculty: Faculty, area: CardSuit, points: Int) {
        actionsThisTurn.add("Publiziert ($area)")
        redraw()
        val column = researchColumns[SUITS.indexOf(area)]
        floatText("+$points", Point(column.posX + (column.width - FLOAT_WIDTH) / 2, column.posY), pointsGreen)
    }

    override fun refreshAfterSabotage(target: Faculty, area: CardSuit) {
        actionsThisTurn.add("Konkurrenz ausgebremst")
        redraw()
        val center = Point(opponentsPane.posX + (opponentsPane.width - FLOAT_WIDTH) / 2, opponentsPane.posY)
        floatText("−1 bei ${target.name}", center, sabotageRed)
    }

    override fun refreshAfterUseLiterature(faculty: Faculty, card: ResearchCard) {
        actionsThisTurn.add("Literatur genutzt")
        redraw()
    }

    override fun refreshAfterTurnChange(nextFaculty: Faculty) {
        if (!switchingTurnAfterCountdown) cancelTurnTransition()
        turnsPlayed++
        mode = Mode.NORMAL
        strategyPane.isVisible = false
        actionsThisTurn.clear()
        pointsAtTurnStart = nextFaculty.researchPoints
        addLog("")
        addLog("═══ ${nextFaculty.name} IST AM ZUG ═══")
        redraw()
    }

    override fun refreshAfterStrategyStart(drawnCards: List<ResearchCard>) {
        mode = Mode.STRATEGY_KEEP
        strategyPane.clear()
        strategyPane.add(
            Label(
                posX = 0, posY = 10, width = 560, height = 30, text = "Eine der beiden Karten behalten:",
                font = font(16, dark, true)
            )
        )
        drawnCards.forEachIndexed { i, card ->
            strategyPane.add(cardLabel(card, 150 + i * 150, 55, 98, 150, { true }) {
                runAction { rootService.playerActionService.finishStrategy(card) }
            })
        }
        redraw()
        // The window with the two drawn cards fades in.
        strategyPane.opacity = 0.0
        strategyPane.isVisible = true
        animate(STRATEGY_FADE_MILLIS, onStep = { t -> strategyPane.opacity = t })
    }

    override fun refreshAfterStrategyEnd(faculty: Faculty) {
        mode = Mode.NORMAL
        strategyPane.isVisible = false
        actionsThisTurn.add("Strategie angepasst")
        redraw()
    }

    override fun refreshAfterLog(message: String) = addLog(message)

    override fun refreshAfterGameEnd(scores: List<FinalScore>) {
        cancelTurnTransition()
    }

    /** Shows a single countdown after the last AP has been spent and strategy selection is complete. */
    override fun refreshAfterActionPointsDepleted(faculty: Faculty) {
        val game = rootService.currentGame ?: return
        if (turnTransitionActive || activeFaculty() !== faculty || faculty.actionPoints != 0) return
        if (game.pendingStrategyCards.isNotEmpty() || game.endgameTurnsLeft == 0) return

        turnTransitionActive = true
        val generation = ++transitionGeneration
        val next = game.faculties[(game.facultyAtTurn + 1) % game.faculties.size]

        val pointsGained = faculty.researchPoints - pointsAtTurnStart
        turnOverlayPoints.text = "+$pointsGained Punkte in diesem Zug"

        if (actionsThisTurn.isEmpty()) {
            turnOverlaySummaryText.text = "Keine"
        } else {
            turnOverlaySummaryText.text = actionsThisTurn.joinToString("  •  ")
        }

        if (game.endgameTurnsLeft == 1) {
            turnOverlayNextTitle.text = "Das Spiel ist beendet."
            turnOverlayNextName.text = "Gleich erscheint die Auswertung."
            turnOverlayNextName.font = font(28, white)
        } else {
            turnOverlayNextTitle.text = "Nächster Spieler:"
            turnOverlayNextName.text = next.name
            turnOverlayNextName.font = font(38, lime, true)
        }

        endTurnButton.isDisabled = true
        endTurnButton.visual = endTurnDisabledVisual

        // Let the animation of the last action finish before the overlay appears and the input is locked.
        val delay = pendingEffectMillis
        pendingEffectMillis = 0
        if (delay > 0) {
            playAnimation(DelayAnimation(delay).apply {
                onFinished = { onGui { fadeInTurnOverlay(generation, faculty) } }
            })
        } else {
            fadeInTurnOverlay(generation, faculty)
        }
    }

    /** Locks the input, removes leftover effects, fades the turn overlay in and starts the countdown. */
    private fun fadeInTurnOverlay(generation: Int, faculty: Faculty) {
        if (!turnTransitionActive || generation != transitionGeneration) return
        lock()
        effectLabels.toList().forEach { removeEffect(it) }
        spotlightOverlay.isVisible = false
        turnOverlay.isVisible = true
        playAnimation(FadeAnimation(turnOverlay, fromOpacity = 0.0, toOpacity = 1.0,
            duration = TURN_FADE_MILLIS).apply {
            onFinished = {
                if (turnTransitionActive && generation == transitionGeneration && activeFaculty() === faculty) {
                    countDownTurn(TURN_COUNTDOWN_SECONDS, generation, faculty)
                }
            }
        })
    }

    /** Schedules one non-blocking countdown step and ignores callbacks from an obsolete transition. */
    private fun countDownTurn(seconds: Int, generation: Int, faculty: Faculty) {
        turnOverlayCountdown.text = seconds.toString()
        playAnimation(DelayAnimation(COUNTDOWN_STEP_MILLIS).apply {
            onFinished = {
                if (turnTransitionActive && generation == transitionGeneration && activeFaculty() === faculty) {
                    if (seconds > 1) {
                        countDownTurn(seconds - 1, generation, faculty)
                    } else {
                        finishTurnCountdown(generation)
                    }
                }
            }
        })
    }

    /** Changes the public table under the overlay, then reveals the next hand after fading out. */
    private fun finishTurnCountdown(generation: Int) {
        switchingTurnAfterCountdown = true
        try {
            runAction(allowDuringTransition = true) { rootService.gameService.endTurn() }
        } finally {
            switchingTurnAfterCountdown = false
        }
        if (!turnTransitionActive || generation != transitionGeneration) return
        playAnimation(FadeAnimation(turnOverlay, fromOpacity = 1.0, toOpacity = 0.0,
            duration = TURN_FADE_MILLIS).apply {
            onFinished = {
                if (turnTransitionActive && generation == transitionGeneration) {
                    cancelTurnTransition()
                    redraw()
                }
            }
        })
    }

    /** Invalidates pending callbacks when a new game, another turn or the end screen is shown. */
    private fun cancelTurnTransition() {
        transitionGeneration++
        turnTransitionActive = false
        turnOverlay.isVisible = false
        turnOverlay.opacity = 0.0
        handPane.isVisible = true
        handTitle.isVisible = true
        unlock()
        effectLabels.toList().forEach { removeEffect(it) }
    }

    // ===================== Zeichnen =====================

    /** Rebuilds the whole table from the current game state. */
    private fun redraw() {
        val game = rootService.currentGame ?: return
        val current = game.faculties[game.facultyAtTurn]

        roundLabel.text = "RUNDE  %02d".format(turnsPlayed / game.faculties.size + 1)
        drawStackTopLabel.text = "NACHZIEHSTAPEL  ${game.drawStack.size}"
        apValue.text = "${current.actionPoints} von 2 AP verfügbar"
        literatureCount.text = "${game.centralLiterature.size} / 6 OFFEN"
        updateStrategyButton()
        spotlightOverlay.isVisible = mode != Mode.NORMAL && !turnTransitionActive

        drawFaculties(game.faculties, current)
        drawOpponents(game.faculties.filter { it !== current })
        drawLiterature(game.centralLiterature)
        drawDrawStack(game.drawStack.size)
        drawResearch(current)
        drawHand(current)
        infoLabel.text = infoText(current)
        endTurnButton.isDisabled = turnTransitionActive || mode == Mode.STRATEGY_KEEP ||
                rootService.playerActionService.hasPossibleAction()
        endTurnButton.visual = if (endTurnButton.isDisabled) endTurnDisabledVisual else endTurnVisual
        endTurnButton.scale = 1.0
    }

    private fun drawFaculties(faculties: List<Faculty>, current: Faculty) {
        facultiesPane.clear()
        faculties.forEachIndexed { i, faculty ->
            facultiesPane.add(facultyCard(faculty, faculty === current, i * 182))
        }
    }

    private fun facultyCard(faculty: Faculty, isCurrent: Boolean, y: Int): Pane<ComponentView> {
        val card = Pane<ComponentView>(
            posX = 0, posY = y, width = 280, height = 172,
            visual = if (isCurrent) activeCardVisual else cardVisual
        )
        card.add(
            Label(
                posX = 14, posY = 14, width = 50, height = 50, text = initials(faculty),
                font = font(16, dark, true),
                visual = if (isCurrent) ColorVisual(200, 255, 60) else ColorVisual(60, 95, 95)
            )
        )
        card.add(
            Label(
                posX = 76, posY = 14, width = 130, height = 26, text = faculty.name,
                font = font(18, white, true), alignment = Alignment.CENTER_LEFT
            )
        )
        if (isCurrent) {
            card.add(
                Label(
                    posX = 76, posY = 40, width = 150, height = 18, text = "DU BIST AM ZUG",
                    font = font(11, lime, true), alignment = Alignment.CENTER_LEFT
                )
            )
        }
        card.add(
            Label(
                posX = 196, posY = 10, width = 70, height = 32, text = "${faculty.researchPoints}",
                font = font(26, white, true), alignment = Alignment.CENTER_RIGHT
            )
        )
        card.add(
            Label(
                posX = 196, posY = 42, width = 70, height = 18, text = "Punkte",
                font = font(13, muted), alignment = Alignment.CENTER_RIGHT
            )
        )
        card.add(
            Label(
                posX = 14, posY = 76, width = 252, height = 18, text = "PUBLIKATIONSSTAPEL",
                font = font(11, muted, true), alignment = Alignment.CENTER_LEFT
            )
        )
        SUITS.forEachIndexed { k, suit ->
            val x = 14 + k * 64
            card.add(Label(posX = x, posY = 98, width = 58, height = 62, visual = boxVisual))
            card.add(
                Label(
                    posX = x, posY = 100, width = 58, height = 28, text = suit.toString(),
                    font = font(20, suitColor(suit), true)
                )
            )
            card.add(
                Label(
                    posX = x, posY = 128, width = 58, height = 28,
                    text = "${faculty.publications.count { it.suit == suit }}",
                    font = font(17, white, true)
                )
            )
        }
        return card
    }

    private fun drawOpponents(opponents: List<Faculty>) {
        opponentsPane.clear()
        val chipWidth = 330
        val gap = 20
        val total = opponents.size * chipWidth + (opponents.size - 1).coerceAtLeast(0) * gap
        val startX = (1220 - total) / 2
        opponents.forEachIndexed { i, opponent ->
            val chip = Pane<ComponentView>(
                posX = startX + i * (chipWidth + gap), posY = 0, width = chipWidth, height = 85, visual = lightVisual
            )
            chip.add(
                Label(
                    posX = 12, posY = 17, width = 50, height = 50, text = initials(opponent),
                    font = font(15, dark, true), visual = ColorVisual(205, 225, 220)
                )
            )
            chip.add(
                Label(
                    posX = 72, posY = 14, width = 140, height = 28, text = opponent.name,
                    font = font(16, dark, true), alignment = Alignment.CENTER_LEFT
                )
            )
            chip.add(
                Label(
                    posX = 72, posY = 44, width = 140, height = 22, text = "${opponent.handCards.size} KARTEN",
                    font = font(12, grey, true), alignment = Alignment.CENTER_LEFT
                )
            )
            SUITS.forEachIndexed { k, suit ->
                val stack = opponent.researchActivity.getValue(suit)
                val stackLabel = Label(
                    posX = 214 + (k % 2) * 56, posY = 8 + (k / 2) * 36, width = 50, height = 32,
                    text = "$suit ${stack.size}", font = font(14, suitColor(suit), true), visual = paperVisual
                ).apply {
                    onMouseClicked = {
                        if (hasActionPoints(2) && stack.isNotEmpty()) {
                            runAction {
                                rootService.playerActionService.sabotage(opponent, suit)
                            }
                        } else {
                            addLog("Sabotage nicht möglich: 2 AP und gegnerische Forschungskarten erforderlich.")
                        }
                    }
                }
                highlightOnHover(stackLabel, sabotageHoverVisual, { paperVisual }) {
                    hasActionPoints(2) && stack.isNotEmpty()
                }
                chip.add(stackLabel)
            }
            opponentsPane.add(chip)
        }
    }

    /**
     * Renders cards in the central literature and connects the "Use Literature" action (2 AP).
     *
     * Clicking a card moves it to the active faculty's matching research stack;
     * a copy of the card flies there.
     *
     * @param cards Currently available [ResearchCard]s in central literature.
     */
    private fun drawLiterature(cards: List<ResearchCard>) {
        literaturePane.clear()
        cards.forEachIndexed { i, card ->
            val label = cardLabel(card, i * 113, 5, 91, 140, { hasActionPoints(2) }) {}
            label.onMouseClicked = { onLiteratureCardClicked(card, label) }
            literaturePane.add(label)
        }
    }

    /** Draws the draw stack: the card back image with a shadow card behind it. */
    private fun drawDrawStack(size: Int) {
        drawStackPane.clear()
        if (size == 0) {
            drawStackPane.add(
                Label(
                    posX = 8, posY = 8, width = 94, height = 145, text = "leer",
                    font = font(14, grey, true), visual = ColorVisual(232, 240, 238)
                )
            )
            return
        }
        drawStackPane.add(
            Label(posX = 16, posY = 14, width = 94, height = 145, visual = ColorVisual(8, 34, 36))
        )
        drawStackPane.add(
            Label(posX = 8, posY = 6, width = 94, height = 145, visual = cardImages.backImage)
        )
        drawStackPane.add(
            Label(
                posX = 50, posY = 118, width = 52, height = 26, text = "$size",
                font = font(18, white, true), alignment = Alignment.CENTER_RIGHT
            )
        )
    }

    private fun drawResearch(faculty: Faculty) {
        researchColumns.forEachIndexed { i, column ->
            val suit = SUITS[i]
            val stack = faculty.researchActivity.getValue(suit)
            column.clear()
            column.add(Label(posX = 0, posY = 0, width = 280, height = 6, visual = suitVisual(suit)))
            column.add(
                Label(
                    posX = 14, posY = 14, width = 200, height = 26, text = "$suit  ${SHORT_NAMES[i]}",
                    font = font(16, dark, true), alignment = Alignment.CENTER_LEFT
                )
            )
            column.add(
                Label(
                    posX = 14, posY = 40, width = 200, height = 20, text = LONG_NAMES[i],
                    font = font(12, grey), alignment = Alignment.CENTER_LEFT
                )
            )
            column.add(
                Label(
                    posX = 220, posY = 14, width = 46, height = 26, text = "${stack.size}",
                    font = font(16, dark, true), alignment = Alignment.CENTER_RIGHT
                )
            )
            val cardWidth = 62
            val step = if (stack.size > 1) minOf(32, 200 / (stack.size - 1)) else 0
            val total = cardWidth + (stack.size - 1).coerceAtLeast(0) * step
            val startX = (280 - total) / 2
            stack.forEachIndexed { k, card ->
                // No own click and no lift: the whole column is clickable and lights up.
                column.add(cardLabel(card, startX + k * step, 78, cardWidth, 95, { false }) {})
            }
        }
    }

    private fun drawHand(faculty: Faculty) {
        handPane.clear()
        val hand = faculty.handCards
        val discardMode = mode == Mode.STRATEGY_DISCARD
        handTitle.text = if (discardMode) "✦ WÄHLE EINE KARTE ZUM ABWERFEN ✦" else "DEINE HAND   ${hand.size} / 6"
        val startX = handStartX(hand.size)
        val middle = (hand.size - 1) / 2.0

        hand.forEachIndexed { i, card ->
            // In strategy mode the cards are lifted and fanned out like a real hand.
            val offset = i - middle
            val y = if (discardMode) HAND_CARD_Y - STRATEGY_LIFT + (abs(offset) * FAN_DROP).toInt() else HAND_CARD_Y
            val canClick = { mode == Mode.STRATEGY_DISCARD || hasActionPoints(1) }
            val label = cardLabel(card, startX + i * HAND_STEP, y, HAND_CARD_WIDTH, HAND_CARD_HEIGHT, canClick) {}
            label.onMouseClicked = { onHandCardClicked(card, label) }
            if (discardMode) {
                label.rotation = offset * FAN_ANGLE
                handPane.add(glowBehind(label, GLOW_OUTER, glowOuterVisual))
                handPane.add(glowBehind(label, GLOW_INNER, glowInnerVisual))
            }
            handPane.add(label)
        }
    }

    /** X position of the first hand card, so that [count] cards are centered in the hand. */
    private fun handStartX(count: Int): Int {
        val total = HAND_CARD_WIDTH + (count - 1).coerceAtLeast(0) * HAND_STEP
        return (HAND_PANE_WIDTH - total) / 2
    }

    /** A colored frame behind [card] with the same rotation, so the card seems to glow. */
    private fun glowBehind(card: Label, margin: Int, visual: ColorVisual) = Label(
        posX = card.posX - margin, posY = card.posY - margin,
        width = card.width + 2 * margin, height = card.height + 2 * margin,
        visual = visual
    ).apply { rotation = card.rotation }

    private fun infoText(current: Faculty) = when {
        mode == Mode.STRATEGY_DISCARD -> "Strategie: Wähle eine Handkarte zum Abwerfen."
        mode == Mode.STRATEGY_KEEP -> "Strategie: Wähle eine der zwei gezogenen Karten."
        current.actionPoints == 0 -> "Keine Aktionspunkte mehr. Beende deinen Zug."
        else -> "Hand = forschen · Literatur = nutzen · Stapel = publizieren · Nachziehstapel = neue Idee"
    }

    // ===================== Klicks =====================

    /**
     * Handles a hand card click depending on the current interaction mode.
     *
     * In normal mode, uses the card for research; a copy of the card flies to its research column.
     * During strategy adjustment, discards the selected card. If the player must choose one of the
     * two drawn cards, displays a reminder instead.
     *
     * @param card the hand card clicked by the player
     * @param label the label that shows this card in the hand
     */
    private fun onHandCardClicked(card: ResearchCard, label: Label) {
        when (mode) {
            Mode.NORMAL -> {
                val from = absolute(handPane, label)
                flyCard(cardImages.frontImageFor(card.suit, card.value), from, researchTarget(card.suit)) {
                    runAction { rootService.playerActionService.research(card) }
                }
            }
            Mode.STRATEGY_DISCARD -> runAction {
                rootService.playerActionService.startStrategy(card)
            }
            Mode.STRATEGY_KEEP -> addLog(
                "Bitte zuerst eine der zwei gezogenen Karten wählen."
            )
        }
    }

    /** "Literatur nutzen": the card moves to its research column and a copy flies there. */
    private fun onLiteratureCardClicked(card: ResearchCard, label: Label) {
        val from = absolute(literaturePane, label)
        flyCard(cardImages.frontImageFor(card.suit, card.value), from, researchTarget(card.suit)) {
            runAction { rootService.playerActionService.useLiterature(card) }
        }
    }

    /** "Neue Ideen gewinnen": a card is drawn and a card back flies from the draw stack into the hand. */
    private fun onDrawStackClicked() {
        if (!canDrawIdea()) return
        val handSize = activeFaculty()?.handCards?.size ?: return
        val from = Point(drawStackPane.posX + DRAW_CARD_OFFSET, drawStackPane.posY + DRAW_CARD_OFFSET)
        val to = Point(
            handPane.posX + handStartX(handSize + 1) + handSize * HAND_STEP,
            handPane.posY + HAND_CARD_Y
        )
        flyCard(cardImages.backImage, from, to) {
            runAction { rootService.playerActionService.drawIdea() }
        }
    }

    private fun onStrategyClicked() {
        if (turnTransitionActive) return
        // No hand card, fewer than 2 cards in the draw stack or no AP: the action cannot start.
        if (mode == Mode.NORMAL && !canStartStrategy()) return
        mode = when (mode) {
            Mode.NORMAL -> Mode.STRATEGY_DISCARD
            Mode.STRATEGY_DISCARD -> Mode.NORMAL
            Mode.STRATEGY_KEEP -> Mode.STRATEGY_KEEP
        }
        redraw()
    }

    /** "Strategie anpassen" stays visible, but looks greyed out and cannot be clicked if it is not possible. */
    private fun updateStrategyButton() {
        val possible = mode != Mode.NORMAL || canStartStrategy()
        strategyButton.isDisabled = !possible
        strategyButton.visual = when {
            mode != Mode.NORMAL -> activeCardVisual
            possible -> cardVisual
            else -> strategyDisabledVisual
        }
        strategyName.font = font(17, if (possible) white else grey, true)
    }

    /**
     * Runs a service action and shows rule violations in the log instead of crashing.
     *
     * @return true if the action was executed without a rule violation
     */
    private fun runAction(allowDuringTransition: Boolean = false, action: () -> Unit): Boolean {
        if (turnTransitionActive && !allowDuringTransition) return false
        return try {
            action()
            true
        } catch (e: IllegalStateException) {
            addLog("⚠ ${e.message}")
            false
        } catch (e: IllegalArgumentException) {
            addLog("⚠ ${e.message}")
            false
        }
    }

    // ===================== Animationen =====================

    /**
     * Calls [action] (the service) and, if it succeeded, lets a copy of the card fly from [from] to [to].
     * The copy is removed when it has arrived.
     */
    private fun flyCard(visual: Visual, from: Point, to: Point, action: () -> Boolean) {
        if (turnTransitionActive) return
        effectLabels.toList().forEach { removeEffect(it) }

        pendingEffectMillis = FLY_DURATION
        val success = action()
        pendingEffectMillis = 0
        if (!success) return

        val flying = Label(
            posX = from.x, posY = from.y, width = HAND_CARD_WIDTH, height = HAND_CARD_HEIGHT, visual = visual
        )
        effectLabels += flying
        addComponents(flying)
        animate(FLY_DURATION, onStep = { t ->
            val eased = t * t * (3 - 2 * t)
            flying.posX = from.x + (to.x - from.x) * eased
            flying.posY = from.y + (to.y - from.y) * eased
        }, onDone = { removeEffect(flying) })
    }

    /** Shows [text] at [at]; it floats upwards and fades out (e.g. "+5" after publishing). */
    private fun floatText(text: String, at: Point, color: Color) {
        val label = Label(
            posX = at.x, posY = at.y, width = FLOAT_WIDTH, height = FLOAT_HEIGHT, text = text,
            font = font(FLOAT_FONT_SIZE, color, true), alignment = Alignment.CENTER
        )
        effectLabels += label
        addComponents(label)
        pendingEffectMillis = FLOAT_DURATION
        animate(FLOAT_DURATION, onStep = { t ->
            label.posY = at.y - FLOAT_DISTANCE * t
            label.opacity = 1.0 - t
        }, onDone = { removeEffect(label) })
    }

    /**
     * Runs a small animation without BGW animations: a background timer calls [onStep] with the
     * progress 0..1 on the GUI thread, [onDone] is called once at the end.
     */
    private fun animate(durationMillis: Int, onStep: (Double) -> Unit, onDone: () -> Unit = {}) {
        val stepMillis = (durationMillis / ANIMATION_STEPS).toLong().coerceAtLeast(1)
        var step = 0
        effectTimer.scheduleAtFixedRate(object : TimerTask() {
            override fun run() {
                step++
                val t = (step.toDouble() / ANIMATION_STEPS).coerceAtMost(1.0)
                val done = step >= ANIMATION_STEPS
                if (done) cancel()
                onGui {
                    onStep(t)
                    if (done) onDone()
                }
            }
        }, stepMillis, stepMillis)
    }

    /** UI changes from a background thread or an animation callback must run on the GUI thread. */
    private fun onGui(task: () -> Unit) {
        BoardGameApplication.runOnGUIThread { task() }
    }

    /** Removes an animation label from the scene (only once). */
    private fun removeEffect(label: Label) {
        if (effectLabels.remove(label)) removeComponents(label)
    }

    /** Absolute position of [child], which lies inside [pane]. */
    private fun absolute(pane: Pane<ComponentView>, child: ComponentView) =
        Point(pane.posX + child.posX, pane.posY + child.posY)

    /** Where a flying card lands in the research column of [suit]. */
    private fun researchTarget(suit: CardSuit): Point {
        val column = researchColumns[SUITS.indexOf(suit)]
        return Point(column.posX + (column.width - HAND_CARD_WIDTH) / 2, column.posY + RESEARCH_TARGET_Y)
    }

    // ===================== Hover-Effekte =====================

    /** Lifts and enlarges [component] while the mouse is over it, but only if [canHover] is true. */
    private fun liftOnHover(component: ComponentView, canHover: () -> Boolean) {
        val baseY = component.posY
        component.onMouseEntered = {
            if (canHover()) {
                component.posY = baseY - HOVER_LIFT
                component.scale = HOVER_SCALE
            }
        }
        component.onMouseExited = {
            component.posY = baseY
            component.scale = 1.0
        }
    }

    /** Changes the visual of [component] while the mouse is over it, but only if [canHover] is true. */
    private fun highlightOnHover(
        component: ComponentView, hoverVisual: Visual, normalVisual: () -> Visual, canHover: () -> Boolean
    ) {
        component.onMouseEntered = { if (canHover()) component.visual = hoverVisual }
        component.onMouseExited = { component.visual = normalVisual() }
    }

    // ===================== Ist die Aktion gerade möglich? =====================

    private fun activeFaculty(): Faculty? = rootService.currentGame?.let { it.faculties[it.facultyAtTurn] }

    private fun hasActionPoints(points: Int): Boolean =
        mode == Mode.NORMAL && (activeFaculty()?.actionPoints ?: 0) >= points

    private fun canDrawIdea(): Boolean {
        val game = rootService.currentGame ?: return false
        val hand = activeFaculty()?.handCards ?: return false
        return hasActionPoints(1) && game.drawStack.isNotEmpty() && hand.size <= MAX_HAND_BEFORE_DRAW
    }

    private fun canPublish(suit: CardSuit): Boolean =
        hasActionPoints(2) && (activeFaculty()?.researchActivity?.get(suit)?.size ?: 0) >= 2

    private fun canStartStrategy(): Boolean {
        val game = rootService.currentGame ?: return false
        val faculty = activeFaculty() ?: return false
        return mode != Mode.STRATEGY_KEEP && faculty.actionPoints >= 1 &&
                faculty.handCards.isNotEmpty() && game.drawStack.size >= 2
    }

    // ===================== Hilfsmethoden =====================

    /** Creates a card image; it lifts on hover if [canHover] is true. */
    private fun cardLabel(
        card: ResearchCard, x: Int, y: Int, width: Int, height: Int,
        canHover: () -> Boolean, onClick: () -> Unit
    ): Label {
        val label = Label(
            posX = x, posY = y, width = width, height = height,
            visual = cardImages.frontImageFor(card.suit, card.value)
        ).apply { onMouseClicked = { onClick() } }
        liftOnHover(label, canHover)
        return label
    }

    private fun suitColor(suit: CardSuit) = when (suit) {
        CardSuit.SPADES -> Color(66, 133, 244)
        CardSuit.HEARTS -> Color(229, 57, 53)
        CardSuit.DIAMONDS -> Color(230, 170, 20)
        CardSuit.CLUBS -> Color(67, 160, 71)
    }

    private fun suitVisual(suit: CardSuit) = when (suit) {
        CardSuit.SPADES -> ColorVisual(66, 133, 244)
        CardSuit.HEARTS -> ColorVisual(229, 57, 53)
        CardSuit.DIAMONDS -> ColorVisual(251, 192, 45)
        CardSuit.CLUBS -> ColorVisual(67, 160, 71)
    }

    /** Light version of the suit color, used when hovering over a research stack. */
    private fun suitLightVisual(suit: CardSuit) = when (suit) {
        CardSuit.SPADES -> ColorVisual(219, 232, 255)
        CardSuit.HEARTS -> ColorVisual(255, 221, 221)
        CardSuit.DIAMONDS -> ColorVisual(255, 241, 200)
        CardSuit.CLUBS -> ColorVisual(220, 243, 222)
    }

    private fun initials(faculty: Faculty) = faculty.name.take(2).uppercase()

    private fun addLog(message: String) {
        logMessages.add(message)
        if (logMessages.size > 100) logMessages.removeFirst()
        logLabel.text = logMessages.joinToString("\n")
    }

    fun getLogs(): List<String> {
        return logMessages.toList()
    }

    private companion object {
        val SUITS = listOf(CardSuit.SPADES, CardSuit.HEARTS, CardSuit.DIAMONDS, CardSuit.CLUBS)
        val SHORT_NAMES = listOf("KI", "SEC", "SWE", "THEO")
        val LONG_NAMES = listOf(
            "Künstliche Intelligenz", "IT-Sicherheit", "Software-Engineering", "Theoretische Informatik"
        )
        const val TURN_FADE_MILLIS = 300
        const val TURN_COUNTDOWN_SECONDS = 3
        const val COUNTDOWN_STEP_MILLIS = 1000
        const val MAX_HAND_BEFORE_DRAW = 5
        const val HOVER_LIFT = 14.0
        const val HOVER_SCALE = 1.06
        const val BUTTON_HOVER_SCALE = 1.03

        // Hand
        const val HAND_PANE_WIDTH = 1220
        const val HAND_CARD_Y = 15
        const val HAND_CARD_WIDTH = 105
        const val HAND_CARD_HEIGHT = 160
        const val HAND_STEP = 118

        // Strategie anpassen
        const val STRATEGY_LIFT = 12
        const val FAN_ANGLE = 5.0
        const val FAN_DROP = 6
        const val GLOW_OUTER = 10
        const val GLOW_INNER = 4
        const val STRATEGY_FADE_MILLIS = 250

        // Animationen (Dauer in Millisekunden)
        const val ANIMATION_STEPS = 20
        const val FLY_DURATION = 350
        const val FLOAT_DURATION = 900
        const val FLOAT_DISTANCE = 60
        const val FLOAT_WIDTH = 220
        const val FLOAT_HEIGHT = 40
        const val FLOAT_FONT_SIZE = 26
        const val DRAW_CARD_OFFSET = 6
        const val RESEARCH_TARGET_Y = 30
    }

    //fun getLogs(): List<String> {
    //    return logMessages.toList()
    //}
}