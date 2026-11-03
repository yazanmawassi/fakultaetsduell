package service



import entity.CardSuit
import entity.Faculty
import entity.ResearchCard

/**
 * This interface provides a mechanism for the service layer classes to communicate
 * (usually to the GUI classes) that certain changes have been made to the entity
 * layer, so that the user interface can be updated accordingly.
 *
 * Default (empty) implementations are provided for all methods, so that implementing
 * GUI classes only need to react to events relevant to them.
 *
 * @see AbstractRefreshingService
 */
interface Refreshable {

    /**
     * Triggered immediately after a new game instance has been successfully initialized.
     * Tells the user interface to render the active gameplay layout and set up components.
     */
    fun refreshAfterStartGame() {}

    /**
     * Triggered when a player advances their active research on the table.
     * Notifies the UI to update the visual representation of unsecured research activity stacks.
     *
     * @param faculty The [Faculty] executing the research action.
     */
    fun refreshAfterResearch(faculty: Faculty) {}

    /**
     * Triggered when a player successfully draws a card into their secret hand collection.
     * Notifies the UI to update the card counter or private hand graphics for the specific player.
     *
     * @param faculty The [Faculty] that drew the new research card.
     */
    fun refreshAfterDrawIdea(faculty: Faculty) {}

    /**
     * Triggered at the beginning of a strategy phase computation or card selection.
     * Informs the UI to temporarily display the temporary selection pool of drawn options.
     *
     * @param drawnCards The list of [ResearchCard] instances drawn for the strategic pool.
     */
    fun refreshAfterStrategyStart(drawnCards: List<ResearchCard>) {}

    /**
     * Triggered when a player successfully concludes their strategic card selections.
     * Instructs the UI to close the strategic modal and update the player's updated state.
     *
     * @param faculty The [Faculty] completing their strategy phase.
     */
    fun refreshAfterStrategyEnd(faculty: Faculty) {}

    /**
     * Triggered when a player officially publishes a research stack on the board.
     * Notifies the UI to clear the unsecured research stack, add the cards to the
     * permanent publications list, and update the faculty's scoring panel.
     *
     * @param faculty The [Faculty] that performed the publication.
     * @param area The [CardSuit] category defining the research topic published.
     * @param points The exact number of research points awarded for this specific action.
     */
    fun refreshAfterPublish(faculty: Faculty, area: CardSuit, points: Int) {}

    /**
     * Triggered when a player executes a sabotage action against a competing faculty.
     * Instructs the UI to visually reflect the disruption or card removal from the target's stack.
     *
     * @param target The opponent [Faculty] being targeted by the sabotage.
     * @param area The [CardSuit] category defining which research stack was sabotaged.
     */
    fun refreshAfterSabotage(target: Faculty, area: CardSuit) {}

    /**
     * Triggered when a faculty utilizes a shared card from the global literature board.
     * Notifies the UI to update the literature layout and display the changed card placement.
     *
     * @param faculty The [Faculty] utilizing the literature card.
     * @param card The specific [ResearchCard] instance pulled from literature.
     */
    fun refreshAfterUseLiterature(faculty: Faculty, card: ResearchCard) {}

    /**
     * Triggered whenever the current active turn cycles to the next participant.
     * Informs the UI to shift visual highlights to the active faculty and reset turn timers or controls.
     *
     * @param nextFaculty The [Faculty] that is now authorized to make moves.
     */
    fun refreshAfterTurnChange(nextFaculty: Faculty) {}

    /**
     * Requests a GUI countdown before ending a turn with no action points left.
     * A pending strategy selection must be completed before this event is sent.
     *
     * @param faculty the faculty whose action points have been exhausted
     */
    fun refreshAfterActionPointsDepleted(faculty: Faculty) {}

    /**
     * Triggered when the final endgame conditions are met and computations are complete.
     * Notifies the UI to stop active gameplay interaction and render the final scoring board overlay.
     *
     * @param scores The ranked list of calculated [FinalScore] data entries for all teams.
     */
    fun refreshAfterGameEnd(scores: List<FinalScore>) {}

    /**
     * Triggered to broadcast runtime logs or audit messages to the system view.
     * Tells the UI to append a new message block onto the scrollable log or terminal view.
     *
     * @param message The formal text log message describing an action that took place.
     */
    fun refreshAfterLog(message: String) {}
}
