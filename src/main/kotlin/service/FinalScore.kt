package service

import entity.Faculty

/**
* Represents the final scoreboard entry and point breakdown for a single [Faculty] at the end of the game.
*
* This data class encapsulates all evaluated scoring categories, detailing how the final total score
* was calculated during the end-of-game evaluation phase.
*
* @property faculty The specific [Faculty] whose end-of-game scores are captured in this entry.
* @property publicationPoints The base points accumulated from successfully published research cards.
* @property majorityBonus The bonus points awarded for achieving the highest number of publications
 * in specific research areas.
* @property breadthBonus The tactical bonus awarded for diversifying research across multiple different areas.
* @property focusBonus The specialized bonus awarded for achieving deep, high-value dedication in a single research
 * area
* @property unpublishedPoints The partial or penalty-adjusted points calculated from remaining un-submitted research
 * on the table or hand.
* @property total The final cumulative victory score (sum of all categories) used to determine the definitive
 * game ranking.
*/
data class FinalScore (
    val faculty: Faculty,
    val publicationPoints: Int,
    val majorityBonus: Int,
    val breadthBonus: Int,
    val focusBonus: Int,
    val unpublishedPoints: Int,
    val total: Int

)