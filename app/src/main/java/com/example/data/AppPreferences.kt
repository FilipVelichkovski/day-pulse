package com.example.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.model.SummaryTodayResponse
import com.example.model.WidgetTodayResponse
import com.example.network.DynamicApiClientFactory
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.appDataStore: DataStore<Preferences> by preferencesDataStore(name = "daypulse_prefs")

data class AppSettings(
    val baseUrl: String = "",
    val bearerToken: String = "",
    val requireVpn: Boolean = true,
    val liveSyncEnabled: Boolean = false,
    val syncIntervalSeconds: Int = 60,
    val widgetTopCount: Int = 4,
)

data class CachedWidgetSnapshot(
    val widgetData: WidgetTodayResponse? = null,
    val syncedAtMs: Long = 0L,
    val lastError: String? = null,
)

data class CachedSummarySnapshot(
    val summaryData: SummaryTodayResponse? = null,
    val syncedAtMs: Long = 0L,
)

class AppPreferences(private val context: Context) {

    private val widgetAdapter by lazy {
        DynamicApiClientFactory.moshi.adapter(WidgetTodayResponse::class.java)
    }
    private val summaryAdapter by lazy {
        DynamicApiClientFactory.moshi.adapter(SummaryTodayResponse::class.java)
    }

    val settingsFlow: Flow<AppSettings> = context.appDataStore.data.map { prefs ->
        AppSettings(
            baseUrl = prefs[KEY_BASE_URL] ?: "",
            bearerToken = prefs[KEY_BEARER_TOKEN] ?: "",
            requireVpn = prefs[KEY_REQUIRE_VPN] ?: true,
            liveSyncEnabled = prefs[KEY_LIVE_SYNC_ENABLED] ?: false,
            syncIntervalSeconds = (prefs[KEY_SYNC_INTERVAL_SEC] ?: 60).coerceIn(15, 900),
            widgetTopCount = (prefs[KEY_WIDGET_TOP_COUNT] ?: 4).coerceIn(1, 10),
        )
    }

    val cachedWidgetFlow: Flow<CachedWidgetSnapshot> = context.appDataStore.data.map { prefs ->
        val json = prefs[KEY_CACHED_WIDGET_JSON]
        val parsed = json?.let {
            try {
                widgetAdapter.fromJson(it)
            } catch (_: Exception) {
                null
            }
        }
        CachedWidgetSnapshot(
            widgetData = parsed,
            syncedAtMs = prefs[KEY_CACHED_WIDGET_SYNCED_AT] ?: 0L,
            lastError = prefs[KEY_CACHED_WIDGET_ERROR],
        )
    }

    val cachedSummaryFlow: Flow<CachedSummarySnapshot> = context.appDataStore.data.map { prefs ->
        val json = prefs[KEY_CACHED_SUMMARY_JSON]
        val parsed = json?.let {
            try {
                summaryAdapter.fromJson(it)
            } catch (_: Exception) {
                null
            }
        }
        CachedSummarySnapshot(
            summaryData = parsed,
            syncedAtMs = prefs[KEY_CACHED_SUMMARY_SYNCED_AT] ?: 0L,
        )
    }

    suspend fun getSettingsOnce(): AppSettings = settingsFlow.first()

    suspend fun getCachedWidgetOnce(): CachedWidgetSnapshot = cachedWidgetFlow.first()

    suspend fun updateConnectionSettings(baseUrl: String, bearerToken: String) {
        context.appDataStore.edit { prefs ->
            prefs[KEY_BASE_URL] = baseUrl.trim()
            prefs[KEY_BEARER_TOKEN] = bearerToken.trim()
        }
    }

    suspend fun setRequireVpn(requireVpn: Boolean) {
        context.appDataStore.edit { prefs ->
            prefs[KEY_REQUIRE_VPN] = requireVpn
        }
    }

    suspend fun setLiveSyncEnabled(enabled: Boolean) {
        context.appDataStore.edit { prefs ->
            prefs[KEY_LIVE_SYNC_ENABLED] = enabled
        }
    }

    suspend fun setSyncIntervalSeconds(seconds: Int) {
        context.appDataStore.edit { prefs ->
            prefs[KEY_SYNC_INTERVAL_SEC] = seconds.coerceIn(15, 900)
        }
    }

    suspend fun setWidgetTopCount(count: Int) {
        context.appDataStore.edit { prefs ->
            prefs[KEY_WIDGET_TOP_COUNT] = count.coerceIn(1, 10)
        }
    }

    suspend fun saveWidgetSnapshot(response: WidgetTodayResponse, syncedAtMs: Long) {
        val json = widgetAdapter.toJson(response)
        context.appDataStore.edit { prefs ->
            prefs[KEY_CACHED_WIDGET_JSON] = json
            prefs[KEY_CACHED_WIDGET_SYNCED_AT] = syncedAtMs
            prefs.remove(KEY_CACHED_WIDGET_ERROR)
        }
    }

    suspend fun saveWidgetError(error: String?) {
        context.appDataStore.edit { prefs ->
            if (error.isNullOrBlank()) {
                prefs.remove(KEY_CACHED_WIDGET_ERROR)
            } else {
                prefs[KEY_CACHED_WIDGET_ERROR] = error
            }
        }
    }

    suspend fun saveSummarySnapshot(response: SummaryTodayResponse, syncedAtMs: Long) {
        val json = summaryAdapter.toJson(response)
        context.appDataStore.edit { prefs ->
            prefs[KEY_CACHED_SUMMARY_JSON] = json
            prefs[KEY_CACHED_SUMMARY_SYNCED_AT] = syncedAtMs
        }
    }

    companion object {
        private val KEY_BASE_URL = stringPreferencesKey("base_url")
        private val KEY_BEARER_TOKEN = stringPreferencesKey("bearer_token")
        private val KEY_REQUIRE_VPN = booleanPreferencesKey("require_vpn")
        private val KEY_LIVE_SYNC_ENABLED = booleanPreferencesKey("live_sync_enabled")
        private val KEY_SYNC_INTERVAL_SEC = intPreferencesKey("sync_interval_sec")
        private val KEY_WIDGET_TOP_COUNT = intPreferencesKey("widget_top_count")
        private val KEY_CACHED_WIDGET_JSON = stringPreferencesKey("cached_widget_json")
        private val KEY_CACHED_WIDGET_SYNCED_AT = longPreferencesKey("cached_widget_synced_at")
        private val KEY_CACHED_WIDGET_ERROR = stringPreferencesKey("cached_widget_error")
        private val KEY_CACHED_SUMMARY_JSON = stringPreferencesKey("cached_summary_json")
        private val KEY_CACHED_SUMMARY_SYNCED_AT = longPreferencesKey("cached_summary_synced_at")
    }
}
