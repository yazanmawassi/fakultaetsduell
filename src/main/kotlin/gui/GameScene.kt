
package gui

import service.Refreshable
import tools.aqua.bgw.core.BoardGameScene

/**
 * Temporary placeholder for GameScene to ensure the GitLab pipeline stays green.
 * Will be overwritten by the teammate's actual implementation during merge.
 */
class GameScene(private val application: SopraApplication) : BoardGameScene(width = 1920, height = 1080), Refreshable
