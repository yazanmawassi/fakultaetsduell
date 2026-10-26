package service

import entity.FacultyGame
/**
 * The root service class is responsible for managing services and the entity layer reference.
 * This class acts as a central hub for every other service within the application.
 *
 */
class RootService {

    /** The currently running game, or `null` if no game was started yet. */
    var currentGame: FacultyGame? = null

    /** Service for starting games, changing turns and ending the game. */
    val gameService = GameService(this)

    /** Service for all actions a faculty can do in its turn. */
    val playerActionService = PlayerActionService(this)

    /**
     * Registers [newRefreshable] at all services,,
     * so it is informed about every change in the game.
     */
    fun addRefreshable(newRefreshable: Refreshable) {
        gameService.addRefreshable(newRefreshable)
        playerActionService.addRefreshable(newRefreshable)
    }

    /** Registers all [newRefreshables] at all services. */
    fun addRefreshables(vararg newRefreshables: Refreshable) {
        newRefreshables.forEach { addRefreshable(it) }
    }
}