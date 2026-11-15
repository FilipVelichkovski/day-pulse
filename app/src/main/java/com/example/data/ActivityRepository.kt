package com.example.data

import android.content.Context
import com.example.model.HealthResponse
import com.example.model.LiveStatusResponse
import com.example.model.SummaryTodayResponse
import com.example.model.WidgetTodayResponse
import com.example.network.DynamicApiClientFactory
import com.example.vpn.VpnMonitor
import com.example.widget.DayPulseWidgetUpdater
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import retrofit2.HttpException
import java.io.IOException

sealed class SyncOutcome<out T> {
    data class Success<T>(val data: T) : SyncOutcome<T>()
    data object NotConfigured : SyncOutcome<Nothing>()
    data object BlockedByVpn : SyncOutcome<Nothing>()
    data class Error(val message: String, val isAuthError: Boolean = false) : SyncOutcome<Nothing>()
}

data class HealthTestResult(
    val ok: Boolean,
    val latencyMs: Long,
    val health: HealthResponse? = null,
    val authVerified: Boolean? = null,
    val message: String,
)

class ActivityRepository(
    private val context: Context,
    val preferences: AppPreferences,
    val vpnMonitor: VpnMonitor,
) {

    suspend fun refreshWidgetData(): SyncOutcome<WidgetTodayResponse> = withContext(Dispatchers.IO) {
        val settings = preferences.getSettingsOnce()
        val api = DynamicApiClientFactory.create(settings.baseUrl, settings.bearerToken)
            ?: return@withContext SyncOutcome.NotConfigured

        val vpnConnected = vpnMonitor.refreshNow().isVpnConnected
        if (settings.requireVpn && !vpnConnected) {
            preferences.saveWidgetError("Waiting for WireGuard VPN")
            DayPulseWidgetUpdater.pushUpdate(context)
            return@withContext SyncOutcome.BlockedByVpn
        }

        return@withContext try {
            val response = api.getWidgetToday(top = settings.widgetTopCount)
            preferences.saveWidgetSnapshot(response, System.currentTimeMillis())
            DayPulseWidgetUpdater.pushUpdate(context)
            SyncOutcome.Success(response)
        } catch (e: HttpException) {
            val msg = DynamicApiClientFactory.parseHttpError(e)
            preferences.saveWidgetError(msg)
            DayPulseWidgetUpdater.pushUpdate(context)
            SyncOutcome.Error(msg, isAuthError = e.code() == 401)
        } catch (e: IOException) {
            val msg = "Cannot reach server: ${e.localizedMessage ?: "Network timeout"}"
            preferences.saveWidgetError(msg)
            DayPulseWidgetUpdater.pushUpdate(context)
            SyncOutcome.Error(msg)
        } catch (e: Exception) {
            val msg = e.localizedMessage ?: "Unexpected sync error"
            preferences.saveWidgetError(msg)
            DayPulseWidgetUpdater.pushUpdate(context)
            SyncOutcome.Error(msg)
        }
    }

    suspend fun refreshSummary(): SyncOutcome<SummaryTodayResponse> = withContext(Dispatchers.IO) {
        val settings = preferences.getSettingsOnce()
        val api = DynamicApiClientFactory.create(settings.baseUrl, settings.bearerToken)
            ?: return@withContext SyncOutcome.NotConfigured

        val vpnConnected = vpnMonitor.refreshNow().isVpnConnected
        if (settings.requireVpn && !vpnConnected) {
            return@withContext SyncOutcome.BlockedByVpn
        }

        return@withContext try {
            val response = api.getSummaryToday()
            preferences.saveSummarySnapshot(response, System.currentTimeMillis())
            SyncOutcome.Success(response)
        } catch (e: HttpException) {
            SyncOutcome.Error(DynamicApiClientFactory.parseHttpError(e), isAuthError = e.code() == 401)
        } catch (e: IOException) {
            SyncOutcome.Error("Cannot reach server: ${e.localizedMessage ?: "Connection failed"}")
        } catch (e: Exception) {
            SyncOutcome.Error(e.localizedMessage ?: "Unexpected error")
        }
    }

    suspend fun pollLiveStatus(): SyncOutcome<LiveStatusResponse> = withContext(Dispatchers.IO) {
        val settings = preferences.getSettingsOnce()
        val api = DynamicApiClientFactory.create(settings.baseUrl, settings.bearerToken)
            ?: return@withContext SyncOutcome.NotConfigured

        val vpnConnected = vpnMonitor.refreshNow().isVpnConnected
        if (settings.requireVpn && !vpnConnected) {
            return@withContext SyncOutcome.BlockedByVpn
        }

        return@withContext try {
            SyncOutcome.Success(api.getLiveStatus())
        } catch (e: HttpException) {
            SyncOutcome.Error(DynamicApiClientFactory.parseHttpError(e), isAuthError = e.code() == 401)
        } catch (e: IOException) {
            SyncOutcome.Error("Cannot reach server: ${e.localizedMessage ?: "Connection failed"}")
        } catch (e: Exception) {
            SyncOutcome.Error(e.localizedMessage ?: "Unexpected error")
        }
    }

    suspend fun testConnection(baseUrl: String, bearerToken: String): HealthTestResult =
        withContext(Dispatchers.IO) {
            val api = DynamicApiClientFactory.create(baseUrl, bearerToken)
                ?: return@withContext HealthTestResult(
                    ok = false,
                    latencyMs = 0,
                    message = "Enter a valid server URL (e.g., http://10.0.0.2:7879)",
                )

            val startNs = System.nanoTime()
            return@withContext try {
                val health = api.getHealth()
                val latencyMs = ((System.nanoTime() - startNs) / 1_000_000L).coerceAtLeast(1L)

                // Also probe /api/v1/status to verify bearer token works if server uses --token
                val authVerified = try {
                    api.getLiveStatus()
                    true
                } catch (e: HttpException) {
                    if (e.code() == 401) false else true
                } catch (_: Exception) {
                    null
                }

                if (authVerified == false) {
                    HealthTestResult(
                        ok = false,
                        latencyMs = latencyMs,
                        health = health,
                        authVerified = false,
                        message = "Server reachable (${latencyMs}ms), but Bearer token rejected (401 Unauthorized).",
                    )
                } else {
                    val dataNote = if (health.hasData) "Data ready for ${health.date}" else "Server online (no data pushed today yet)"
                    HealthTestResult(
                        ok = true,
                        latencyMs = latencyMs,
                        health = health,
                        authVerified = authVerified,
                        message = "Connected in ${latencyMs}ms • $dataNote",
                    )
                }
            } catch (e: HttpException) {
                val latencyMs = ((System.nanoTime() - startNs) / 1_000_000L).coerceAtLeast(1L)
                HealthTestResult(
                    ok = false,
                    latencyMs = latencyMs,
                    message = DynamicApiClientFactory.parseHttpError(e),
                )
            } catch (e: IOException) {
                val latencyMs = ((System.nanoTime() - startNs) / 1_000_000L).coerceAtLeast(1L)
                HealthTestResult(
                    ok = false,
                    latencyMs = latencyMs,
                    message = "Connection failed: ${e.localizedMessage ?: "Host unreachable (check WireGuard VPN & port 7879)"}",
                )
            } catch (e: Exception) {
                HealthTestResult(
                    ok = false,
                    latencyMs = 0,
                    message = e.localizedMessage ?: "Unknown error",
                )
            }
        }
}
