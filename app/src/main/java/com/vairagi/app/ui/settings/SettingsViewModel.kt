package com.vairagi.app.ui.settings

import android.app.Application
import android.content.Intent
import androidx.core.content.FileProvider
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.vairagi.app.data.AppSettings
import com.vairagi.app.data.DailyUsageStats
import com.vairagi.app.data.PreferencesManager
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.File

class SettingsViewModel(application: Application) : AndroidViewModel(application) {

    private val preferencesManager = PreferencesManager(application)

    val settingsState: StateFlow<AppSettings> = preferencesManager.settingsFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000L),
            initialValue = AppSettings()
        )

    val usageStatsState: StateFlow<DailyUsageStats> = preferencesManager.usageStatsFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000L),
            initialValue = DailyUsageStats()
        )

    fun updateContinuousInterval(minutes: Int) {
        viewModelScope.launch {
            preferencesManager.updateSettings { it.copy(continuousIntervalMinutes = minutes) }
        }
    }

    fun updateCumulativeInterval(minutes: Int) {
        viewModelScope.launch {
            preferencesManager.updateSettings { it.copy(cumulativeIntervalMinutes = minutes) }
        }
    }

    fun updateBreakThreshold(seconds: Int) {
        viewModelScope.launch {
            preferencesManager.updateSettings { it.copy(breakThresholdSeconds = seconds) }
        }
    }

    fun updateSoundEnabled(enabled: Boolean) {
        viewModelScope.launch {
            preferencesManager.updateSettings { it.copy(soundEnabled = enabled) }
        }
    }

    fun updateDynamicColor(enabled: Boolean) {
        viewModelScope.launch {
            preferencesManager.updateSettings { it.copy(useDynamicColor = enabled) }
        }
    }

    fun updateHideSensitiveData(enabled: Boolean) {
        viewModelScope.launch {
            preferencesManager.updateSettings { it.copy(hideSensitiveDataInRecents = enabled) }
        }
    }

    fun updateShowAppNamesOnWidget(enabled: Boolean) {
        viewModelScope.launch {
            preferencesManager.updateSettings { it.copy(showAppNamesOnWidget = enabled) }
        }
    }

    fun updateRequireBiometric(enabled: Boolean) {
        viewModelScope.launch {
            preferencesManager.updateSettings { it.copy(requireBiometricToPause = enabled) }
        }
    }

    fun resetAllData(onComplete: () -> Unit) {
        viewModelScope.launch {
            preferencesManager.resetAllData()
            onComplete()
        }
    }

    fun exportDataCsv() {
        viewModelScope.launch {
            val stats = usageStatsState.first()
            val csvBuilder = StringBuilder()
            csvBuilder.append("Date,CumulativeSeconds,CumulativeFormatted\n")
            stats.history7Days.forEach { (date, sec) ->
                val hrs = sec / 3600
                val mins = (sec % 3600) / 60
                csvBuilder.append("$date,$sec,\"${hrs}h ${mins}m\"\n")
            }

            try {
                val context = getApplication<Application>()
                val cacheDir = context.cacheDir

                // Clean stale CSV export files older than 24 hours
                cacheDir.listFiles()?.forEach { file ->
                    if (file.name.endsWith(".csv") && (System.currentTimeMillis() - file.lastModified() > 86_400_000L)) {
                        file.delete()
                    }
                }

                val csvFile = File(cacheDir, "vairagi_usage_history.csv")
                csvFile.writeText(csvBuilder.toString())

                val uri = FileProvider.getUriForFile(
                    context,
                    "${context.packageName}.fileprovider",
                    csvFile
                )

                val intent = Intent(Intent.ACTION_SEND).apply {
                    type = "text/csv"
                    putExtra(Intent.EXTRA_STREAM, uri)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(Intent.createChooser(intent, "Export Usage History").apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                })
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}
