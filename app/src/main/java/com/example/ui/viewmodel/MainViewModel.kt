package com.example.ui.viewmodel

import android.app.Application
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.os.Build
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.DayPulseApp
import com.example.data.AppSettings
import com.example.data.CachedWidgetSnapshot
import com.example.data.HealthTestResult
import com.example.data.SyncOutcome
import com.example.model.LiveStatusResponse
import com.example.model.SummaryTodayResponse
import com.example.model.WidgetCategoryItem
import com.example.model.WidgetTodayResponse
import com.example.model.WidgetAppItem
import com.example.vpn.VpnSyncService
import com.example.vpn.VpnStatus
import com.example.vpn.WidgetSyncWorker
import com.example.widget.DayPulseWidgetReceiver
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

enum class ActivityLogTab {
    APPS,
    SITES,
    FILES,
}

data class MainUiState(
    val settings: AppSettings = AppSettings(),
    val vpnStatus: VpnStatus = VpnStatus(),
    val summary: SummaryTodayResponse? = null,
    val liveStatus: LiveStatusResponse? = null,
    val cachedWidget: CachedWidgetSnapshot = CachedWidgetSnapshot(),
    val liveTickerSec: Double? = null,
    val isRefreshing: Boolean = false,
    val errorMessage: String? = null,
    val blockedByVpn: Boolean = false,
    val lastSyncedAtMs: Long = 0L,
    val activityTab: ActivityLogTab = ActivityLogTab.APPS,
    val activitySearchQuery: String = "",
    val isTestingConnection: Boolean = false,
    val healthTestResult: HealthTestResult? = null,
    val pinnedWidgetCount: Int = 0,
    val canRequestPinWidget: Boolean = false,
) {
    val isConfigured: Boolean get() = settings.baseUrl.isNotBlank()

    val effectiveWidgetPreview: WidgetTodayResponse?
        get() = cachedWidget.widgetData ?: summary?.let { sum ->
            WidgetTodayResponse(
                date = sum.date,
                hasData = sum.hasData,
                isStale = sum.isStale,
                updatedAt = sum.updatedAt,
                totalActiveSec = sum.totalActiveSec,
                state = liveStatus?.state ?: when {
                    !sum.hasData || sum.isStale -> "offline"
                    sum.status.screenLocked -> "locked"
                    sum.status.paused -> "paused"
                    sum.status.tracking -> "active"
                    else -> "offline"
                },
                currentApp = liveStatus?.currentApp ?: sum.status.currentApp,
                currentCategory = liveStatus?.currentCategory ?: sum.status.currentCategory,
                currentDurationSec = liveTickerSec ?: liveStatus?.currentDurationSec ?: sum.status.currentDurationSec,
                topCategories = sum.categories.take(settings.widgetTopCount).map {
                    WidgetCategoryItem(it.category, it.totalSec, it.sharePct)
                },
                topApps = sum.apps.take(settings.widgetTopCount).map {
                    WidgetAppItem(it.appName, it.totalSec)
                },
            )
        }
}

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val app = application as DayPulseApp
    private val repository = app.repository
    private val preferences = app.preferences
    private val vpnMonitor = app.vpnMonitor

    private val _uiState = MutableStateFlow(MainUiState())
    val uiState: StateFlow<MainUiState> = _uiState.asStateFlow()

    private var pollingJob: Job? = null
    private var tickerJob: Job? = null

    init {
        refreshPinCapability()

        viewModelScope.launch {
            combine(
                preferences.settingsFlow,
                vpnMonitor.status,
                preferences.cachedSummaryFlow,
                preferences.cachedWidgetFlow,
            ) { settings, vpn, cachedSum, cachedWidget ->
                Quad(settings, vpn, cachedSum, cachedWidget)
            }.collect { (settings, vpn, cachedSum, cachedWidget) ->
                _uiState.update { current ->
                    current.copy(
                        settings = settings,
                        vpnStatus = vpn,
                        summary = current.summary ?: cachedSum.summaryData,
                        cachedWidget = cachedWidget,
                        lastSyncedAtMs = maxOf(current.lastSyncedAtMs, cachedSum.syncedAtMs, cachedWidget.syncedAtMs),
                        blockedByVpn = settings.requireVpn && !vpn.isVpnConnected,
                    )
                }
            }
        }

        startLiveTicker()
    }

    fun onAppForegrounded() {
        vpnMonitor.refreshNow()
        refreshPinCapability()
        refreshAll()
        startForegroundPolling()
    }

    fun onAppBackgrounded() {
        pollingJob?.cancel()
        pollingJob = null
    }

    fun refreshAll() {
        viewModelScope.launch {
            _uiState.update { it.copy(isRefreshing = true, errorMessage = null) }
            val summaryOutcome = repository.refreshSummary()
            val widgetOutcome = repository.refreshWidgetData()
            val statusOutcome = repository.pollLiveStatus()

            _uiState.update { state ->
                val newSummary = (summaryOutcome as? SyncOutcome.Success)?.data ?: state.summary
                val newLive = (statusOutcome as? SyncOutcome.Success)?.data ?: state.liveStatus
                val errorMsg = when {
                    summaryOutcome is SyncOutcome.Error -> summaryOutcome.message
                    widgetOutcome is SyncOutcome.Error -> widgetOutcome.message
                    else -> null
                }
                val blocked = summaryOutcome is SyncOutcome.BlockedByVpn ||
                    widgetOutcome is SyncOutcome.BlockedByVpn

                state.copy(
                    isRefreshing = false,
                    summary = newSummary,
                    liveStatus = newLive,
                    liveTickerSec = newLive?.currentDurationSec ?: newSummary?.status?.currentDurationSec,
                    errorMessage = errorMsg,
                    blockedByVpn = blocked,
                    lastSyncedAtMs = if (summaryOutcome is SyncOutcome.Success) {
                        System.currentTimeMillis()
                    } else {
                        state.lastSyncedAtMs
                    },
                )
            }
        }
    }

    private fun startForegroundPolling() {
        pollingJob?.cancel()
        pollingJob = viewModelScope.launch {
            while (isActive) {
                delay(5_000L)
                val current = _uiState.value
                if (!current.isConfigured) continue
                if (current.settings.requireVpn && !vpnMonitor.isVpnConnectedNow()) continue

                when (val outcome = repository.pollLiveStatus()) {
                    is SyncOutcome.Success -> {
                        _uiState.update { state ->
                            state.copy(
                                liveStatus = outcome.data,
                                liveTickerSec = outcome.data.currentDurationSec,
                                errorMessage = null,
                                blockedByVpn = false,
                            )
                        }
                    }
                    is SyncOutcome.BlockedByVpn -> {
                        _uiState.update { it.copy(blockedByVpn = true) }
                    }
                    is SyncOutcome.Error -> {
                        _uiState.update { it.copy(errorMessage = outcome.message) }
                    }
                    is SyncOutcome.NotConfigured -> Unit
                }
            }
        }
    }

    private fun startLiveTicker() {
        tickerJob?.cancel()
        tickerJob = viewModelScope.launch {
            while (isActive) {
                delay(1_000L)
                _uiState.update { state ->
                    val status = state.liveStatus
                    val isActiveNow = status != null && status.hasData && !status.isStale && status.state == "active"
                    if (isActiveNow && state.liveTickerSec != null) {
                        state.copy(liveTickerSec = state.liveTickerSec + 1.0)
                    } else {
                        state
                    }
                }
            }
        }
    }

    fun selectActivityTab(tab: ActivityLogTab) {
        _uiState.update { it.copy(activityTab = tab) }
    }

    fun updateSearchQuery(query: String) {
        _uiState.update { it.copy(activitySearchQuery = query) }
    }

    fun saveConnectionSettings(baseUrl: String, bearerToken: String) {
        viewModelScope.launch {
            preferences.updateConnectionSettings(baseUrl, bearerToken)
            refreshAll()
        }
    }

    fun testConnection(baseUrl: String, bearerToken: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isTestingConnection = true, healthTestResult = null) }
            val result = repository.testConnection(baseUrl, bearerToken)
            _uiState.update { it.copy(isTestingConnection = false, healthTestResult = result) }
            if (result.ok) {
                preferences.updateConnectionSettings(baseUrl, bearerToken)
                refreshAll()
            }
        }
    }

    fun setRequireVpn(requireVpn: Boolean) {
        viewModelScope.launch {
            preferences.setRequireVpn(requireVpn)
            refreshAll()
        }
    }

    fun setLiveSyncEnabled(enabled: Boolean) {
        viewModelScope.launch {
            preferences.setLiveSyncEnabled(enabled)
            if (enabled) {
                VpnSyncService.start(app)
                WidgetSyncWorker.schedulePeriodic(app)
            } else {
                VpnSyncService.stop(app)
            }
        }
    }

    fun setSyncIntervalSeconds(seconds: Int) {
        viewModelScope.launch {
            preferences.setSyncIntervalSeconds(seconds)
        }
    }

    fun setWidgetTopCount(count: Int) {
        viewModelScope.launch {
            preferences.setWidgetTopCount(count)
            repository.refreshWidgetData()
        }
    }

    fun requestPinWidget(): Boolean {
        val manager = AppWidgetManager.getInstance(app)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && manager.isRequestPinAppWidgetSupported) {
            val provider = ComponentName(app, DayPulseWidgetReceiver::class.java)
            return manager.requestPinAppWidget(provider, null, null)
        }
        return false
    }

    fun refreshPinCapability() {
        val manager = AppWidgetManager.getInstance(app)
        val provider = ComponentName(app, DayPulseWidgetReceiver::class.java)
        val count = try {
            manager.getAppWidgetIds(provider).size
        } catch (_: Exception) {
            0
        }
        val canPin = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && manager.isRequestPinAppWidgetSupported
        _uiState.update { it.copy(pinnedWidgetCount = count, canRequestPinWidget = canPin) }
    }

    private data class Quad<A, B, C, D>(val a: A, val b: B, val c: C, val d: D)
}
