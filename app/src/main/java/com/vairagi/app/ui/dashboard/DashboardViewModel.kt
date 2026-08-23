package com.vairagi.app.ui.dashboard

import android.app.Application
import android.content.Intent
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.vairagi.app.data.AppSettings
import com.vairagi.app.data.DailyUsageStats
import com.vairagi.app.data.PreferencesManager
import com.vairagi.app.engine.AppUsageItem
import com.vairagi.app.engine.UsageStatsHelper
import com.vairagi.app.service.TrackingService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate

class DashboardViewModel(application: Application) : AndroidViewModel(application) {

    private val preferencesManager = PreferencesManager(application)
    private val usageStatsHelper = UsageStatsHelper(application)

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

    private val _topAppsState = MutableStateFlow<List<AppUsageItem>>(emptyList())
    val topAppsState: StateFlow<List<AppUsageItem>> = _topAppsState.asStateFlow()

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    private val _expandedAppPackage = MutableStateFlow<String?>(null)
    val expandedAppPackage: StateFlow<String?> = _expandedAppPackage.asStateFlow()

    // Contextual Insight ("You're 20% calmer than last Tuesday")
    val contextualInsightState: StateFlow<String> = usageStatsState.combine(settingsState) { stats, settings ->
        calculateContextualInsight(stats, settings)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000L),
        initialValue = "Mindfully present today"
    )

    init {
        loadTopApps()
    }

    fun loadTopApps() {
        viewModelScope.launch(Dispatchers.IO) {
            _isRefreshing.value = true
            val list = usageStatsHelper.getTodayAppUsageList()
            _topAppsState.value = list
            _isRefreshing.value = false
        }
    }

    fun refreshDashboard() {
        viewModelScope.launch(Dispatchers.IO) {
            _isRefreshing.value = true
            delay(400L) // Smooth pull-to-refresh UX
            val list = usageStatsHelper.getTodayAppUsageList()
            _topAppsState.value = list
            _isRefreshing.value = false
        }
    }

    fun toggleAppExpanded(packageName: String) {
        _expandedAppPackage.value = if (_expandedAppPackage.value == packageName) null else packageName
    }

    fun togglePauseTracking() {
        val currentPaused = settingsState.value.trackingPaused
        viewModelScope.launch {
            preferencesManager.updateSettings { it.copy(trackingPaused = !currentPaused) }
            val intent = Intent(getApplication(), TrackingService::class.java).apply {
                action = if (!currentPaused) TrackingService.ACTION_PAUSE_TRACKING else TrackingService.ACTION_RESUME_TRACKING
            }
            getApplication<Application>().startService(intent)
        }
    }

    fun triggerTestOverlay() {
        val intent = Intent(getApplication(), TrackingService::class.java).apply {
            action = TrackingService.ACTION_TEST_OVERLAY
        }
        getApplication<Application>().startService(intent)
    }

    private fun calculateContextualInsight(stats: DailyUsageStats, settings: AppSettings): String {
        val todaySec = stats.cumulativeSecondsToday
        val history = stats.history7Days
        val today = LocalDate.now()
        val sameDayLastWeekStr = today.minusDays(7).toString()
        val lastWeekSec = history[sameDayLastWeekStr]

        if (lastWeekSec != null && lastWeekSec > 0) {
            val diffPct = ((lastWeekSec - todaySec).toDouble() / lastWeekSec.toDouble() * 100).toInt()
            if (diffPct > 0) {
                return "You're $diffPct% calmer than last ${today.dayOfWeek.name.lowercase().replaceFirstChar { it.uppercase() }}"
            } else if (diffPct < 0) {
                return "Screen time is slightly higher than last ${today.dayOfWeek.name.lowercase().replaceFirstChar { it.uppercase() }}"
            }
        }

        val intentionSec = settings.cumulativeIntervalMinutes * 60L
        val percent = if (intentionSec > 0) (todaySec * 100 / intentionSec).toInt() else 0
        return when {
            percent == 0 -> "A peaceful start to your day"
            percent < 50 -> "Well within your daily intention"
            percent < 90 -> "Approaching your daily intention"
            else -> "Mindful pause recommended soon"
        }
    }
}
