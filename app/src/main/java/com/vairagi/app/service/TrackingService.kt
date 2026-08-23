package com.vairagi.app.service

import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import androidx.core.app.NotificationCompat
import com.vairagi.app.MainActivity
import com.vairagi.app.R
import com.vairagi.app.VairagiApp
import com.vairagi.app.data.AppSettings
import com.vairagi.app.data.PreferencesManager
import com.vairagi.app.engine.CounterEngine
import com.vairagi.app.engine.TriggerReason
import com.vairagi.app.overlay.OverlayManager
import kotlinx.coroutines.*
import java.time.LocalDate

class TrackingService : Service() {

    private val serviceScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private lateinit var preferencesManager: PreferencesManager
    private lateinit var counterEngine: CounterEngine
    private lateinit var overlayManager: OverlayManager
    private var screenReceiver: ScreenStateReceiver? = null

    private var currentSettings = AppSettings()
    private var tickerJob: Job? = null

    companion object {
        const val ACTION_START_TRACKING = "com.vairagi.app.ACTION_START_TRACKING"
        const val ACTION_PAUSE_TRACKING = "com.vairagi.app.ACTION_PAUSE_TRACKING"
        const val ACTION_RESUME_TRACKING = "com.vairagi.app.ACTION_RESUME_TRACKING"
        const val ACTION_TEST_OVERLAY = "com.vairagi.app.ACTION_TEST_OVERLAY"
        const val NOTIFICATION_ID = 1001
    }

    override fun onCreate() {
        super.onCreate()
        preferencesManager = PreferencesManager(this)
        counterEngine = CounterEngine()
        overlayManager = OverlayManager(
            context = this,
            onDismissContinuous = {
                counterEngine.resetContinuousCounter()
                persistCurrentStats()
            }
        )

        registerScreenStateReceiver()
        startForegroundServiceNotification()
        observePreferencesAndState()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action

        val powerManager = getSystemService(Context.POWER_SERVICE) as? PowerManager
        val isScreenOn = powerManager?.isInteractive ?: true

        when (action) {
            Intent.ACTION_SCREEN_ON, Intent.ACTION_USER_PRESENT -> {
                val continuous = counterEngine.onScreenOn(System.currentTimeMillis())
                startTicker()
            }
            Intent.ACTION_SCREEN_OFF -> {
                counterEngine.onScreenOff(System.currentTimeMillis())
                stopTicker()
                persistCurrentStats()
            }
            ACTION_PAUSE_TRACKING -> {
                stopTicker()
            }
            ACTION_RESUME_TRACKING -> {
                if (isScreenOn) startTicker()
            }
            ACTION_TEST_OVERLAY -> {
                overlayManager.showOverlay(
                    reason = TriggerReason.Both(
                        continuousMinutes = currentSettings.continuousIntervalMinutes,
                        cumulativeMinutes = currentSettings.cumulativeIntervalMinutes
                    ),
                    soundEnabled = currentSettings.soundEnabled
                )
            }
            else -> {
                if (isScreenOn) {
                    counterEngine.onScreenOn(System.currentTimeMillis())
                    startTicker()
                }
            }
        }

        return START_STICKY
    }

    private fun registerScreenStateReceiver() {
        if (screenReceiver == null) {
            screenReceiver = ScreenStateReceiver()
            val filter = IntentFilter().apply {
                addAction(Intent.ACTION_SCREEN_ON)
                addAction(Intent.ACTION_SCREEN_OFF)
                addAction(Intent.ACTION_USER_PRESENT)
            }
            registerReceiver(screenReceiver, filter)
        }
    }

    private fun startForegroundServiceNotification() {
        val notificationIntent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this, 0, notificationIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val notification: Notification = NotificationCompat.Builder(this, VairagiApp.CHANNEL_ID_TRACKING)
            .setContentTitle(getString(R.string.notification_title))
            .setContentText(getString(R.string.notification_text))
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setContentIntent(pendingIntent)
            .build()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    private fun observePreferencesAndState() {
        serviceScope.launch {
            preferencesManager.settingsFlow.collect { settings ->
                currentSettings = settings
                counterEngine.continuousIntervalSeconds = settings.continuousIntervalMinutes * 60L
                counterEngine.cumulativeIntervalSeconds = settings.cumulativeIntervalMinutes * 60L
                counterEngine.breakThresholdSeconds = settings.breakThresholdSeconds.toLong()

                if (settings.trackingPaused) {
                    stopTicker()
                }
            }
        }

        serviceScope.launch {
            preferencesManager.usageStatsFlow.collect { stats ->
                if (counterEngine.getCumulativeSeconds() == 0L && stats.cumulativeSecondsToday > 0L) {
                    counterEngine.restoreState(
                        continuousSec = stats.continuousSecondsStreak,
                        cumulativeSec = stats.cumulativeSecondsToday,
                        date = LocalDate.now()
                    )
                }
            }
        }
    }

    private fun startTicker() {
        if (tickerJob?.isActive == true || currentSettings.trackingPaused) return

        tickerJob = serviceScope.launch(Dispatchers.Default) {
            while (isActive) {
                delay(1000L) // 1-second ticks
                val today = LocalDate.now()
                val triggerReason = counterEngine.tick(1L, today)

                if (triggerReason != null) {
                    withContext(Dispatchers.Main) {
                        overlayManager.showOverlay(
                            reason = triggerReason,
                            soundEnabled = currentSettings.soundEnabled
                        )
                    }
                }

                // Periodically persist state every 10 seconds
                if (counterEngine.getCumulativeSeconds() % 10 == 0L) {
                    persistCurrentStats()
                }
            }
        }
    }

    private fun stopTicker() {
        tickerJob?.cancel()
        tickerJob = null
    }

    private fun persistCurrentStats() {
        serviceScope.launch {
            preferencesManager.saveUsageState(
                cumulativeSeconds = counterEngine.getCumulativeSeconds(),
                continuousSeconds = counterEngine.getContinuousSeconds(),
                lastMidnightTs = System.currentTimeMillis(),
                todayDateStr = LocalDate.now().toString()
            )
            com.vairagi.app.widget.AppUsageWidgetProvider.updateAllWidgets(this@TrackingService)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        stopTicker()
        serviceScope.cancel()
        screenReceiver?.let { unregisterReceiver(it) }
        overlayManager.removeOverlay()
    }
}
