package com.example

import android.app.Application
import com.example.data.ActivityRepository
import com.example.data.AppPreferences
import com.example.vpn.VpnMonitor
import com.example.vpn.VpnSyncService
import com.example.vpn.WidgetSyncWorker
import com.example.widget.DayPulseWidgetUpdater
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class DayPulseApp : Application() {

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    lateinit var preferences: AppPreferences
        private set

    lateinit var vpnMonitor: VpnMonitor
        private set

    lateinit var repository: ActivityRepository
        private set

    override fun onCreate() {
        super.onCreate()
        preferences = AppPreferences(this)
        vpnMonitor = VpnMonitor(this)
        repository = ActivityRepository(this, preferences, vpnMonitor)

        WidgetSyncWorker.schedulePeriodic(this)

        vpnMonitor.startMonitoring { vpnStatus ->
            appScope.launch {
                val settings = preferences.getSettingsOnce()
                if (vpnStatus.isVpnConnected) {
                    WidgetSyncWorker.enqueueImmediate(this@DayPulseApp)
                    if (settings.liveSyncEnabled) {
                        try {
                            VpnSyncService.start(this@DayPulseApp)
                        } catch (_: Exception) {
                            // Android 12+ may restrict FGS start from background if app is idle;
                            // WorkManager immediate sync above still updates the widget.
                        }
                    }
                } else {
                    DayPulseWidgetUpdater.pushUpdate(this@DayPulseApp)
                }
            }
        }
    }
}
