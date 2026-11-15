package com.example.widget

import android.content.Context
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.glance.GlanceId
import androidx.glance.action.ActionParameters
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.state.updateAppWidgetState
import com.example.vpn.WidgetSyncWorker

object DayPulseWidgetUpdater {

    val KEY_REVISION = intPreferencesKey("widget_revision")

    suspend fun pushUpdate(context: Context) {
        val widget = DayPulseWidget()
        val manager = GlanceAppWidgetManager(context)
        val glanceIds = manager.getGlanceIds(DayPulseWidget::class.java)
        for (glanceId in glanceIds) {
            updateAppWidgetState(context, glanceId) { prefs ->
                prefs[KEY_REVISION] = (prefs[KEY_REVISION] ?: 0) + 1
            }
            widget.update(context, glanceId)
        }
    }
}

class RefreshWidgetAction : ActionCallback {
    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters,
    ) {
        WidgetSyncWorker.enqueueImmediate(context)
    }
}
