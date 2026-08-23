package com.vairagi.app.engine

enum class GrowthStage(val title: String, val minDays: Int, val description: String) {
    SPROUT("Fresh Sprout", 1, "Your mindful detachment journey begins."),
    SAPLING("Growing Sapling", 3, "Building consistent screen time awareness."),
    TREE("Full Oak", 7, "A full week of intentional focus and balance."),
    BODHI("Bodhi Tree", 14, "Mastery of digital detachment and peace.")
}

data class AchievementStatus(
    val currentStreakDays: Int,
    val stage: GrowthStage,
    val nextStageDays: Int,
    val progress: Float
)

class AchievementsEngine {

    fun calculateStatus(history7Days: Map<String, Long>, intentionMinutes: Int): AchievementStatus {
        val intentionSec = intentionMinutes * 60L
        var streak = 0

        val sortedEntries = history7Days.entries.sortedByDescending { it.key }
        for (entry in sortedEntries) {
            if (entry.value <= intentionSec) {
                streak++
            } else {
                break
            }
        }

        val stage = when {
            streak >= GrowthStage.BODHI.minDays -> GrowthStage.BODHI
            streak >= GrowthStage.TREE.minDays -> GrowthStage.TREE
            streak >= GrowthStage.SAPLING.minDays -> GrowthStage.SAPLING
            else -> GrowthStage.SPROUT
        }

        val nextDays = when (stage) {
            GrowthStage.SPROUT -> GrowthStage.SAPLING.minDays
            GrowthStage.SAPLING -> GrowthStage.TREE.minDays
            GrowthStage.TREE -> GrowthStage.BODHI.minDays
            GrowthStage.BODHI -> GrowthStage.BODHI.minDays
        }

        val progress = (streak.toFloat() / nextDays.toFloat()).coerceIn(0f, 1f)

        return AchievementStatus(
            currentStreakDays = streak,
            stage = stage,
            nextStageDays = nextDays,
            progress = progress
        )
    }
}
