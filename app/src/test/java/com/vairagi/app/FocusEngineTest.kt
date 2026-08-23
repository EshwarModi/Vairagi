package com.vairagi.app

import com.vairagi.app.engine.FocusEngine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FocusEngineTest {

    @Test
    fun testFocusSessionLifecycle() {
        val engine = FocusEngine()
        assertFalse(engine.getState().isActive)

        val startedState = engine.startFocusSession(25)
        assertTrue(startedState.isActive)
        assertEquals(25, startedState.targetDurationMinutes)
        assertEquals(0L, startedState.elapsedSeconds)

        // Tick 60 seconds
        repeat(60) {
            engine.onTick()
        }
        assertEquals(60L, engine.getState().elapsedSeconds)
        assertTrue(engine.getState().isActive)

        val stoppedState = engine.stopFocusSession()
        assertFalse(stoppedState.isActive)
    }
}
