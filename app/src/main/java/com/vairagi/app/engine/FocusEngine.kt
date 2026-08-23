package com.vairagi.app.engine

data class FocusSessionState(
    val isActive: Boolean = false,
    val targetDurationMinutes: Int = 25,
    val elapsedSeconds: Long = 0L,
    val strictIntervalMinutes: Int = 5
)

class FocusEngine {
    private var state = FocusSessionState()

    fun startFocusSession(durationMinutes: Int = 25): FocusSessionState {
        state = FocusSessionState(
            isActive = true,
            targetDurationMinutes = durationMinutes,
            elapsedSeconds = 0L,
            strictIntervalMinutes = 5
        )
        return state
    }

    fun onTick(): FocusSessionState {
        if (!state.isActive) return state

        val newElapsed = state.elapsedSeconds + 1L
        val totalTargetSec = state.targetDurationMinutes * 60L

        state = if (newElapsed >= totalTargetSec) {
            state.copy(isActive = false, elapsedSeconds = totalTargetSec)
        } else {
            state.copy(elapsedSeconds = newElapsed)
        }
        return state
    }

    fun stopFocusSession(): FocusSessionState {
        state = FocusSessionState(isActive = false)
        return state
    }

    fun getState(): FocusSessionState = state
}
