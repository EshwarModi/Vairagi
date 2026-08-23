package com.vairagi.app

import com.vairagi.app.engine.AchievementsEngine
import com.vairagi.app.engine.GrowthStage
import org.junit.Assert.assertEquals
import org.junit.Test

class AchievementsEngineTest {

    @Test
    fun testGrowthStageCalculation() {
        val engine = AchievementsEngine()

        // 0-day history -> SPROUT
        val status0 = engine.calculateStatus(emptyMap(), 30)
        assertEquals(GrowthStage.SPROUT, status0.stage)
        assertEquals(0, status0.currentStreakDays)

        // 3-day successful history -> SAPLING
        val history3 = mapOf(
            "2026-08-22" to 1200L,
            "2026-08-21" to 1400L,
            "2026-08-20" to 1500L
        )
        val status3 = engine.calculateStatus(history3, 30)
        assertEquals(GrowthStage.SAPLING, status3.stage)
        assertEquals(3, status3.currentStreakDays)
    }
}
