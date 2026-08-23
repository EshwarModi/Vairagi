package com.vairagi.app

import android.app.AppOpsManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.Process
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.navigation.compose.rememberNavController
import com.vairagi.app.service.TrackingService
import com.vairagi.app.ui.navigation.NavGraph
import com.vairagi.app.ui.navigation.Screen
import com.vairagi.app.ui.theme.VairagiTheme

import androidx.activity.enableEdgeToEdge
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen

import android.os.StrictMode

import android.view.WindowManager
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.vairagi.app.data.PreferencesManager

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        val preferencesManager = PreferencesManager(this)

        if (BuildConfig.DEBUG) {
            StrictMode.setThreadPolicy(
                StrictMode.ThreadPolicy.Builder()
                    .detectDiskReads()
                    .detectDiskWrites()
                    .detectNetwork()
                    .penaltyLog()
                    .build()
            )
        }

        val isUsageStatsGranted = checkUsageStatsPermission(this)
        val isOverlayGranted = Settings.canDrawOverlays(this)
        val hasRequiredPermissions = isUsageStatsGranted && isOverlayGranted

        if (hasRequiredPermissions) {
            startTrackingService()
        }

        val startDestination = if (hasRequiredPermissions) {
            Screen.Dashboard.route
        } else {
            Screen.Onboarding.route
        }

        setContent {
            val settings by preferencesManager.settingsFlow.collectAsState(initial = com.vairagi.app.data.AppSettings())

            androidx.compose.runtime.LaunchedEffect(settings.hideSensitiveDataInRecents) {
                if (settings.hideSensitiveDataInRecents) {
                    window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
                } else {
                    window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
                }
            }

            VairagiTheme {
                val navController = rememberNavController()
                NavGraph(
                    navController = navController,
                    startDestination = startDestination
                )
            }
        }
    }

    private fun startTrackingService() {
        val serviceIntent = Intent(this, TrackingService::class.java).apply {
            action = TrackingService.ACTION_START_TRACKING
        }
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(serviceIntent)
            } else {
                startService(serviceIntent)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun checkUsageStatsPermission(context: Context): Boolean {
        val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as? AppOpsManager ?: return false
        val mode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            appOps.unsafeCheckOpNoThrow(
                AppOpsManager.OPSTR_GET_USAGE_STATS,
                Process.myUid(),
                context.packageName
            )
        } else {
            @Suppress("DEPRECATION")
            appOps.checkOpNoThrow(
                AppOpsManager.OPSTR_GET_USAGE_STATS,
                Process.myUid(),
                context.packageName
            )
        }
        return mode == AppOpsManager.MODE_ALLOWED
    }
}
