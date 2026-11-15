package com.example.vpn

import android.content.Context
import androidx.work.Configuration
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.OutOfQuotaPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.example.DayPulseApp
import com.example.data.SyncOutcome
import java.util.concurrent.TimeUnit

class WidgetSyncWorker(
    appContext: Context,
    params: WorkerParameters,
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val app = applicationContext as? DayPulseApp ?: return Result.failure()
        return when (app.repository.refreshWidgetData()) {
            is SyncOutcome.Success,
            is SyncOutcome.NotConfigured,
            is SyncOutcome.BlockedByVpn -> Result.success()
            is SyncOutcome.Error -> if (runAttemptCount < 2) Result.retry() else Result.success()
        }
    }

    companion object {
        private const val PERIODIC_WORK_NAME = "daypulse_periodic_widget_sync"
        private const val IMMEDIATE_WORK_NAME = "daypulse_immediate_widget_sync"

        private fun getWorkManager(context: Context): WorkManager {
            if (!WorkManager.isInitialized()) {
                try {
                    WorkManager.initialize(
                        context.applicationContext,
                        Configuration.Builder().build(),
                    )
                } catch (_: IllegalStateException) {
                    // Already initialized concurrently
                }
            }
            return WorkManager.getInstance(context)
        }

        fun schedulePeriodic(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            val request = PeriodicWorkRequestBuilder<WidgetSyncWorker>(
                15, TimeUnit.MINUTES,
            )
                .setConstraints(constraints)
                .build()

            getWorkManager(context).enqueueUniquePeriodicWork(
                PERIODIC_WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                request,
            )
        }

        fun enqueueImmediate(context: Context) {
            val request = OneTimeWorkRequestBuilder<WidgetSyncWorker>()
                .setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
                .build()

            getWorkManager(context).enqueueUniqueWork(
                IMMEDIATE_WORK_NAME,
                ExistingWorkPolicy.REPLACE,
                request,
            )
        }
    }
}
