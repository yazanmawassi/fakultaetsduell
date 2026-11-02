package gui

import entity.CardSuit
import entity.Faculty
import entity.ResearchCard
import service.Refreshable
import service.RootService
import tools.aqua.bgw.components.ComponentView
import tools.aqua.bgw.components.layoutviews.Pane
import tools.aqua.bgw.components.uicomponents.Button
import tools.aqua.bgw.components.uicomponents.Label
import tools.aqua.bgw.core.Alignment
import tools.aqua.bgw.core.BoardGameScene
import tools.aqua.bgw.core.Color
import tools.aqua.bgw.util.Font
import tools.aqua.bgw.visual.ColorVisual
import tools.aqua.bgw.visual.Visual

/**
 * Main game screen: faculties and the action "Strategie anpassen" on the left,
 * opponents, central literature, own research and hand in the middle,
 * action points, log and "Zug beenden" on the right.
 *
 * Connected to the service: "Strategie anpassen" and "Zug beenden".
 * The other actions are marked with a comment `// US "..."` that shows which
 * service call belongs there.
 */
class GameScene(private val rootService: RootService) :
    BoardGameScene(1920, 1080, background = ColorVisual(13, 43, 43)), Refreshable {

    /** What a click on a hand card currently means. */
    private enum class Mode { NORMAL, STRATEGY_DISCARD, STRATEGY_KEEP }
    private val cardImages = CardImageLoader()
    private var mode = Mode.NORMAL
    private var turnsPlayed = 0
    private val logMessages = mutableListOf<String>()

    // ===================== Farben & Schriften =====================

    private val white = Color(255, 255, 255)
    private val dark = Color(19, 35, 43)
    private val grey = Color(110, 125, 130)
    private val muted = Color(160, 185, 185)
    private val lime = Color(200, 255, 60)

    private val cardVisual = ColorVisual(17, 52, 52)
    private val activeCardVisual = ColorVisual(28, 72, 56)
    private val boxVisual = ColorVisual(13, 43, 43)
    private val lightVisual = ColorVisual(250, 252, 251)
    private val paperVisual = ColorVisual(255, 255, 255)

    // Hover-Farben
    private val sabotageHoverVisual = ColorVisual(255, 214, 214)
    private val strategyHoverVisual = ColorVisual(40, 95, 70)
    private val endTurnVisual = ColorVisual(200, 255, 60)
    private val endTurnHoverVisual = ColorVisual(228, 255, 150)
    private val endTurnDisabledVisual = ColorVisual(70, 95, 95)

    private fun font(size: Int, color: Color = white, bold: Boolean = false) =
        Font(size = size, color = color, fontWeight = if (bold) Font.FontWeight.BOLD else Font.FontWeight.NORMAL)

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
    ).apply {
        onMouseClicked = {
            if (canDrawIdea()) {
                runAction {
                    rootService.playerActionService.drawIdea()
                }
            }
        }
    }

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

    private val handPane = Pane<ComponentView>(posX = 320, posY = 775, width = 1220, height = 175)

    private val infoLabel = Label(
        posX = 630, posY = 975, width = 600, height = 40, text = "", font = font(14, dark), visual = lightVisual
    )

    private val strategyPane = Pane<ComponentView>(
        posX = 690, posY = 300, width = 560, height = 230, visual = paperVisual
    ).apply { isVisible = false }

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

    private val logLabel = Label(
        posX = 1565, posY = 230, width = 330, height = 640, text = "",
        font = font(13, white), alignment = Alignment.TOP_LEFT, isWrapText = true, visual = cardVisual
    )

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
        addComponents(strategyPane)
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
        strategyName.onMouseExited = {
            strategyButton.visual = if (mode == Mode.NORMAL) cardVisual else activeCardVisual
        }

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

    // ===================== Refreshable =====================

    override fun refreshAfterStartGame() {
        mode = Mode.NORMAL
        turnsPlayed = 0
        strategyPane.isVisible = false
        logMessages.clear()
        logLabel.text = ""
        redraw()
    }

    override fun refreshAfterResearch(faculty: Faculty) = redraw()
    override fun refreshAfterDrawIdea(faculty: Faculty) = redraw()
    override fun refreshAfterPublish(faculty: Faculty, area: CardSuit, points: Int) = redraw()
    override fun refreshAfterSabotage(target: Faculty, area: CardSuit) = redraw()
    override fun refreshAfterUseLiterature(faculty: Faculty, card: ResearchCard) = redraw()

    override fun refreshAfterTurnChange(nextFaculty: Faculty) {
        turnsPlayed++
        mode = Mode.NORMAL
        strategyPane.isVisible = false
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
        strategyPane.isVisible = true
        redraw()
    }

    override fun refreshAfterStrategyEnd(faculty: Faculty) {
        mode = Mode.NORMAL
        strategyPane.isVisible = false
        redraw()
    }

    override fun refreshAfterLog(message: String) = addLog(message)

    // ===================== Zeichnen =====================

    /** Rebuilds the whole table from the current game state. */
    private fun redraw() {
        val game = rootService.currentGame ?: return
        val current = game.faculties[game.facultyAtTurn]

        roundLabel.text = "RUNDE  %02d".format(turnsPlayed / game.faculties.size + 1)
        drawStackTopLabel.text = "NACHZIEHSTAPEL  ${game.drawStack.size}"
        apValue.text = "${current.actionPoints} von 2 AP verfügbar"
        literatureCount.text = "${game.centralLiterature.size} / 6 OFFEN"
        strategyButton.visual = if (mode == Mode.NORMAL) cardVisual else activeCardVisual

        drawFaculties(game.faculties, current)
        drawOpponents(game.faculties.filter { it !== current })
        drawLiterature(game.centralLiterature)
        drawDrawStack(game.drawStack.size)
        drawResearch(current)
        drawHand(current)
        infoLabel.text = infoText(current)
        endTurnButton.isDisabled = mode == Mode.STRATEGY_KEEP || rootService.playerActionService.hasPossibleAction()
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
                    // US "Konkurrenz ausbremsen": hier
                    // runAction { rootService.playerActionService.sabotage(opponent, suit) } aufrufen
                    onMouseClicked = { notConnected("Konkurrenz ausbremsen") }
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
     * Clicking a card removes it from the central literature and moves it
     * to the active faculty's matching research stack.
     *
     * @param cards Currently available [ResearchCard]s in central literature.
     */
    private fun drawLiterature(cards: List<ResearchCard>) {
        literaturePane.clear()
        cards.forEachIndexed { i, card ->
            literaturePane.add(cardLabel(card, i * 113, 5, 91, 140, { hasActionPoints(2) }) {
                runAction { rootService.playerActionService.useLiterature(card) }
            })
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
        handTitle.text = "DEINE HAND   ${hand.size} / 6"
        val cardWidth = 105
        val step = 118
        val total = cardWidth + (hand.size - 1).coerceAtLeast(0) * step
        val startX = (1220 - total) / 2
        hand.forEachIndexed { i, card ->
            val canClick = { mode == Mode.STRATEGY_DISCARD || hasActionPoints(1) }
            handPane.add(cardLabel(card, startX + i * step, 15, cardWidth, 160, canClick) { onHandCardClicked(card) })
        }
    }

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
     * In normal mode, uses the card for research. The service validates the
     * action, moves the card to the matching research stack, updates the
     * central literature, and deducts one action point.
     *
     * During strategy adjustment, discards the selected card. If the player
     * must choose one of the two drawn cards, displays a reminder instead.
     *
     * @param card the hand card clicked by the player
     */
    private fun onHandCardClicked(card: ResearchCard) {
        when (mode) {
            Mode.NORMAL -> runAction {
                rootService.playerActionService.research(card)
            }
            Mode.STRATEGY_DISCARD -> runAction {
                rootService.playerActionService.startStrategy(card)
            }
            Mode.STRATEGY_KEEP -> addLog(
                "Bitte zuerst eine der zwei gezogenen Karten wählen."
            )
        }
    }

    private fun onStrategyClicked() {
        mode = when (mode) {
            Mode.NORMAL -> Mode.STRATEGY_DISCARD
            Mode.STRATEGY_DISCARD -> Mode.NORMAL
            Mode.STRATEGY_KEEP -> Mode.STRATEGY_KEEP
        }
        redraw()
    }

    /** Runs a service action and shows rule violations in the log instead of crashing. */
    private fun runAction(action: () -> Unit) {
        try {
            action()
        } catch (e: IllegalStateException) {
            addLog("⚠ ${e.message}")
        } catch (e: IllegalArgumentException) {
            addLog("⚠ ${e.message}")
        } catch (e: NotImplementedError) {
            addLog("⚠ Aktion noch nicht implementiert.")
        }
    }

    /** Placeholder for actions whose user story is not connected to the service yet. */
    private fun notConnected(action: String) {
        addLog("ℹ $action ist noch nicht mit dem Service verbunden.")
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
        logMessages.add(0, message)
        if (logMessages.size > MAX_LOG_LINES) logMessages.removeLast()
        logLabel.text = logMessages.joinToString("\n")
    }

    private companion object {
        val SUITS = listOf(CardSuit.SPADES, CardSuit.HEARTS, CardSuit.DIAMONDS, CardSuit.CLUBS)
        val SHORT_NAMES = listOf("KI", "SEC", "SWE", "THEO")
        val LONG_NAMES = listOf(
            "Künstliche Intelligenz", "IT-Sicherheit", "Software-Engineering", "Theoretische Informatik"
        )
        const val MAX_LOG_LINES = 20
        const val MAX_HAND_BEFORE_DRAW = 5
        const val HOVER_LIFT = 14.0
        const val HOVER_SCALE = 1.06
        const val BUTTON_HOVER_SCALE = 1.03
    }
}