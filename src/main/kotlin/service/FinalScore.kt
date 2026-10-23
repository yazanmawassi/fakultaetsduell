package service

import entity.Faculty

data class FinalScore (
    val faculty: Faculty,
    val publicationPoints: Int,
    val majorityBonus: Int,
    val breadthBonus: Int,
    val focusBonus: Int,
    val unpublishedPoints: Int,
    val total: Int

)