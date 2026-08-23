package com.vairagi.app.engine

import java.time.LocalDate

sealed class TriggerReason {
    data class Continuous(val minutes: Int) : TriggerReason()
    data class Cumulative(val minutes: Int) : TriggerReason()
    data class Both(val continuousMinutes: Int, val cumulativeMinutes: Int) : TriggerReason()
}

data class CounterState(
    val continuousSeconds: Long = 0L,
    val cumulativeSeconds: Long = 0L,
    val currentDate: LocalDate = LocalDate.now(),
    val isScreenOn: Boolean = false,
    val screenOffTimestamp: Long = 0L // Epoch ms when screen turned off
)

class CounterEngine(
    var continuousIntervalSeconds: Long = 15 * 60L,
    var cumulativeIntervalSeconds: Long = 30 * 60L,
    var breakThresholdSeconds: Long = 60L
) {

    private var continuousSeconds: Long = 0L
    private var cumulativeSeconds: Long = 0L
    private var currentDate: LocalDate = LocalDate.now()
    private var screenOffTimestamp: Long = 0L
    private var isScreenOn: Boolean = true

    // Internal tracking for trigger thresholds to avoid duplicate alerts for the same cycle
    private var lastContinuousTriggeredThreshold: Long = 0L
    private var lastCumulativeTriggeredThreshold: Long = 0L

    fun getContinuousSeconds(): Long = continuousSeconds
    fun getCumulativeSeconds(): Long = cumulativeSeconds
    fun getCurrentDate(): LocalDate = currentDate
    fun isScreenOnState(): Boolean = isScreenOn

    fun restoreState(
        continuousSec: Long,
        cumulativeSec: Long,
        date: LocalDate,
        screenOn: Boolean = true,
        offTimestamp: Long = 0L
    ) {
        this.continuousSeconds = continuousSec
        this.cumulativeSeconds = cumulativeSec
        this.currentDate = date
        this.isScreenOn = screenOn
        this.screenOffTimestamp = offTimestamp
    }

    /**
     * Called when screen turns ON or device is unlocked.
     * Checks if the break duration was >= breakThresholdSeconds.
     * If break >= threshold, resets continuous counter to 0.
     * If break < threshold, continuous counter continues from previous value.
     */
    fun onScreenOn(currentTimeMs: Long): Long {
        isScreenOn = true
        if (screenOffTimestamp > 0) {
            val breakDurationSeconds = (currentTimeMs - screenOffTimestamp) / 1000L
            if (breakDurationSeconds >= breakThresholdSeconds) {
                continuousSeconds = 0L
                lastContinuousTriggeredThreshold = 0L
            }
            screenOffTimestamp = 0L
        }
        return continuousSeconds
    }

    /**
     * Called when screen turns OFF or device locks.
     */
    fun onScreenOff(currentTimeMs: Long) {
        isScreenOn = false
        screenOffTimestamp = currentTimeMs
    }

    /**
     * Ticks the counter when screen is ON.
     * @param deltaSeconds Seconds elapsed since last tick (typically 1s)
     * @param today Current date according to local device time
     * @return TriggerReason if a popup should be shown, or null if no threshold hit.
     */
    fun tick(deltaSeconds: Long, today: LocalDate): TriggerReason? {
        if (!isScreenOn) return null

        // Check for midnight date change
        if (today != currentDate) {
            currentDate = today
            cumulativeSeconds = 0L
            lastCumulativeTriggeredThreshold = 0L
        }

        continuousSeconds += deltaSeconds
        cumulativeSeconds += deltaSeconds

        var continuousTrigger: TriggerReason.Continuous? = null
        var cumulativeTrigger: TriggerReason.Cumulative? = null

        // Continuous trigger logic: fires every continuousIntervalSeconds
        if (continuousIntervalSeconds > 0 && continuousSeconds >= continuousIntervalSeconds) {
            val thresholdCycle = continuousSeconds / continuousIntervalSeconds
            if (thresholdCycle > lastContinuousTriggeredThreshold) {
                lastContinuousTriggeredThreshold = thresholdCycle
                val mins = (continuousIntervalSeconds / 60).toInt()
                continuousTrigger = TriggerReason.Continuous(mins)
            }
        }

        // Cumulative trigger logic: fires every cumulativeIntervalSeconds
        if (cumulativeIntervalSeconds > 0 && cumulativeSeconds >= cumulativeIntervalSeconds) {
            val thresholdCycle = cumulativeSeconds / cumulativeIntervalSeconds
            if (thresholdCycle > lastCumulativeTriggeredThreshold) {
                lastCumulativeTriggeredThreshold = thresholdCycle
                val mins = ((thresholdCycle * cumulativeIntervalSeconds) / 60).toInt()
                cumulativeTrigger = TriggerReason.Cumulative(mins)
            }
        }

        // Merge coinciding triggers
        return when {
            continuousTrigger != null && cumulativeTrigger != null -> {
                TriggerReason.Both(continuousTrigger.minutes, cumulativeTrigger.minutes)
            }
            continuousTrigger != null -> continuousTrigger
            cumulativeTrigger != null -> cumulativeTrigger
            else -> null
        }
    }

    /**
     * Resets continuous counter to 0 (e.g. after dismissing continuous alert popup or user action).
     */
    fun resetContinuousCounter() {
        continuousSeconds = 0L
        lastContinuousTriggeredThreshold = 0L
    }

    /**
     * Resets both continuous & cumulative counters (e.g. manual reset).
     */
    fun resetAllCounters() {
        continuousSeconds = 0L
        cumulativeSeconds = 0L
        lastContinuousTriggeredThreshold = 0L
        lastCumulativeTriggeredThreshold = 0L
    }
}
