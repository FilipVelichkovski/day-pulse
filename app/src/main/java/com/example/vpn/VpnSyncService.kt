package com.example.vpn

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.example.DayPulseApp
import com.example.MainActivity
import com.example.R
import com.example.data.SyncOutcome
import com.example.util.Formatters
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class VpnSyncService : Service() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var pollingJob: Job? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        ensureNotificationChannel()
        val initialNotification = buildNotification("Monitoring WireGuard VPN…")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                NOTIFICATION_ID,
                initialNotification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC,
            )
        } else {
            startForeground(NOTIFICATION_ID, initialNotification)
        }

        val app = application as DayPulseApp
        serviceScope.launch {
            combine(
                app.preferences.settingsFlow,
                app.vpnMonitor.status,
            ) { settings, vpn -> settings to vpn }.collect { (settings, vpn) ->
                if (!settings.liveSyncEnabled) {
                    stopSelf()
                    return@collect
                }
                restartPollingLoop(
                    intervalSec = settings.syncIntervalSeconds,
                    requireVpn = settings.requireVpn,
                    vpnConnected = vpn.isVpnConnected,
                    baseUrl = settings.baseUrl,
                )
            }
        }
    }

    private fun restartPollingLoop(
        intervalSec: Int,
        requireVpn: Boolean,
        vpnConnected: Boolean,
        baseUrl: String,
    ) {
        pollingJob?.cancel()

        if (baseUrl.isBlank()) {
            updateNotification("Server URL not configured")
            return
        }

        if (requireVpn && !vpnConnected) {
            updateNotification("Waiting for WireGuard VPN connection…")
            return
        }

        val app = application as DayPulseApp
        pollingJob = serviceScope.launch {
            while (isActive) {
                when (val outcome = app.repository.refreshWidgetData()) {
                    is SyncOutcome.Success -> {
                        val total = Formatters.formatDurationCompact(outcome.data.totalActiveSec)
                        val state = outcome.data.state.uppercase()
                        val prefix = if (vpnConnected) "WireGuard active" else "Connected"
                        updateNotification("$prefix • Today: $total ($state)")
                    }
                    is SyncOutcome.BlockedByVpn -> {
                        updateNotification("Waiting for WireGuard VPN connection…")
                    }
                    is SyncOutcome.NotConfigured -> {
                        updateNotification("Server URL not configured")
                    }
                    is SyncOutcome.Error -> {
                        updateNotification("Sync paused: ${outcome.message}")
                    }
                }
                delay(intervalSec * 1000L)
            }
        }
    }

    private fun ensureNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                getString(R.string.sync_notification_channel_name),
                NotificationManager.IMPORTANCE_LOW,
            ).apply {
                description = getString(R.string.sync_notification_channel_desc)
                setShowBadge(false)
            }
            val nm = getSystemService(NotificationManager::class.java)
            nm.createNotificationChannel(channel)
        }
    }

    private fun updateNotification(contentText: String) {
        val nm = getSystemService(NotificationManager::class.java)
        nm.notify(NOTIFICATION_ID, buildNotification(contentText))
    }

    private fun buildNotification(contentText: String): Notification {
        val openIntent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_widget_vpn)
            .setContentTitle("DayPulse Widget Sync")
            .setContentText(contentText)
            .setOngoing(true)
            .setSilent(true)
            .setContentIntent(pendingIntent)
            .build()
    }

    override fun onDestroy() {
        pollingJob?.cancel()
        serviceScope.cancel()
        super.onDestroy()
    }

    companion object {
        private const val CHANNEL_ID = "daypulse_vpn_sync_channel"
        private const val NOTIFICATION_ID = 7879

        fun start(context: Context) {
            val intent = Intent(context, VpnSyncService::class.java)
            ContextCompat.startForegroundService(context, intent)
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, VpnSyncService::class.java))
        }
    }
}
