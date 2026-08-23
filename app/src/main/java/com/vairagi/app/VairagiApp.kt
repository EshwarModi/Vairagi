package com.vairagi.app

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build

class VairagiApp : Application() {

    override fun onCreate() {
        super.onCreate()
        createNotificationChannels()
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID_TRACKING,
                getString(R.string.notification_channel_name),
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = getString(R.string.notification_channel_desc)
                setShowBadge(false)
            }

            val alertChannel = NotificationChannel(
                CHANNEL_ID_ALERT,
                "Vairagi Interruption Alert",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "High priority notification channel for screen interruptions"
                enableVibration(true)
            }

            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager?.createNotificationChannel(channel)
            notificationManager?.createNotificationChannel(alertChannel)
        }
    }

    companion object {
        const val CHANNEL_ID_TRACKING = "vairagi_tracking_channel"
        const val CHANNEL_ID_ALERT = "vairagi_alert_channel"
    }
}
