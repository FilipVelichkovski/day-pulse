package com.example.widget

import android.content.Context
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import com.example.vpn.WidgetSyncWorker

class DayPulseWidgetReceiver : GlanceAppWidgetReceiver() {

    override val glanceAppWidget: GlanceAppWidget = DayPulseWidget()

    override fun onEnabled(context: Context) {
        super.onEnabled(context)
        WidgetSyncWorker.schedulePeriodic(context)
    }
}
