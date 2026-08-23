package com.vairagi.app

import com.vairagi.app.engine.CounterEngine
import com.vairagi.app.engine.TriggerReason
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.time.LocalDate

class CounterEngineTest {

    private lateinit var engine: CounterEngine

    @Before
    fun setUp() {
        // Continuous limit: 15 min (900s), Cumulative limit: 30 min (1800s), Break threshold: 60s
        engine = CounterEngine(
            continuousIntervalSeconds = 900L,
            cumulativeIntervalSeconds = 1800L,
            breakThresholdSeconds = 60L
        )
    }

    @Test
    fun testContinuousCounterTriggersAt15Minutes() {
        val today = LocalDate.of(2026, 8, 21)
        engine.restoreState(0L, 0L, today, screenOn = true)

        var lastReason: TriggerReason?
        // Tick 899 seconds
        for (i in 1..899) {
            val reason = engine.tick(1L, today)
            assertNull("Should not trigger before 900 seconds", reason)
        }

        // Tick 900th second (15 minutes)
        lastReason = engine.tick(1L, today)
        assertNotNull("Should trigger at 900 seconds", lastReason)
        assertTrue("Trigger should be Continuous", lastReason is TriggerReason.Continuous)
        assertEquals(15, (lastReason as TriggerReason.Continuous).minutes)
    }

    @Test
    fun testScreenOffBreakResetsContinuousCounterWhenThresholdExceeded() {
        val today = LocalDate.of(2026, 8, 21)
        engine.restoreState(500L, 500L, today, screenOn = true)

        val screenOffTime = 1000000L
        engine.onScreenOff(screenOffTime)
        assertFalse(engine.isScreenOnState())

        // Turn screen back ON after 65 seconds (exceeding 60s break threshold)
        val screenOnTime = screenOffTime + (65 * 1000L)
        engine.onScreenOn(screenOnTime)

        assertTrue(engine.isScreenOnState())
        assertEquals("Continuous counter should reset to 0 after break >= 60s", 0L, engine.getContinuousSeconds())
        assertEquals("Cumulative counter should remain unchanged", 500L, engine.getCumulativeSeconds())
    }

    @Test
    fun testScreenOffBreakDoesNotResetContinuousCounterWhenBelowThreshold() {
        val today = LocalDate.of(2026, 8, 21)
        engine.restoreState(500L, 500L, today, screenOn = true)

        val screenOffTime = 1000000L
        engine.onScreenOff(screenOffTime)

        // Turn screen back ON after 30 seconds (below 60s break threshold)
        val screenOnTime = screenOffTime + (30 * 1000L)
        engine.onScreenOn(screenOnTime)

        assertEquals("Continuous counter should NOT reset after short break < 60s", 500L, engine.getContinuousSeconds())
    }

    @Test
    fun testResetContinuousCounterRestartsFromZero() {
        val today = LocalDate.of(2026, 8, 21)
        engine.restoreState(900L, 900L, today, screenOn = true)

        engine.resetContinuousCounter()
        assertEquals(0L, engine.getContinuousSeconds())
        assertEquals(900L, engine.getCumulativeSeconds())

        // Ticking 10 seconds starts continuous from 0
        engine.tick(10L, today)
        assertEquals(10L, engine.getContinuousSeconds())
        assertEquals(910L, engine.getCumulativeSeconds())
    }

    @Test
    fun testCumulativeCounterResetsAtMidnight() {
        val day1 = LocalDate.of(2026, 8, 21)
        val day2 = LocalDate.of(2026, 8, 22)

        engine.restoreState(1000L, 5000L, day1, screenOn = true)

        // Tick on next day
        engine.tick(1L, day2)

        assertEquals("Cumulative counter should reset to 0 at midnight date change", 1L, engine.getCumulativeSeconds())
        assertEquals("Current date should update to day2", day2, engine.getCurrentDate())
    }

    @Test
    fun testCoincidentTriggersMergeIntoBoth() {
        val today = LocalDate.of(2026, 8, 21)
        // Set continuous at 899s and cumulative at 1799s
        // At next tick (1 second), continuous reaches 900s (15m) AND cumulative reaches 1800s (30m)
        engine.restoreState(899L, 1799L, today, screenOn = true)

        val reason = engine.tick(1L, today)
        assertNotNull("Should trigger combined reason", reason)
        assertTrue("Should be TriggerReason.Both", reason is TriggerReason.Both)

        val both = reason as TriggerReason.Both
        assertEquals(15, both.continuousMinutes)
        assertEquals(30, both.cumulativeMinutes)
    }
}
