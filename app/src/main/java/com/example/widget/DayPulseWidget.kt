package com.example.widget

import android.appwidget.AppWidgetManager
import android.content.Context
import android.os.Build
import android.widget.RemoteViews
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.datastore.preferences.core.Preferences
import androidx.glance.ColorFilter
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.LocalSize
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.currentState
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.state.GlanceStateDefinition
import androidx.glance.state.PreferencesGlanceStateDefinition
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.example.MainActivity
import com.example.R
import com.example.data.AppPreferences
import com.example.data.AppSettings
import com.example.data.CachedWidgetSnapshot
import com.example.model.WidgetTodayResponse
import com.example.util.Formatters
import com.example.vpn.VpnMonitor

class DayPulseWidget : GlanceAppWidget() {

    override val stateDefinition: GlanceStateDefinition<*> = PreferencesGlanceStateDefinition

    override val sizeMode: SizeMode = SizeMode.Responsive(RESPONSIVE_BUCKETS)

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val preferences = AppPreferences(context)
        val settings = preferences.getSettingsOnce()
        val snapshot = preferences.getCachedWidgetOnce()
        val vpnConnected = VpnMonitor(context).isVpnConnectedNow()

        provideContent {
            // Observe revision so pushUpdate() always triggers recomposition
            @Suppress("UNUSED_VARIABLE")
            val revision = currentState<Preferences>()[DayPulseWidgetUpdater.KEY_REVISION] ?: 0
            val size = LocalSize.current

            GlanceTheme {
                WidgetContent(
                    size = size,
                    settings = settings,
                    snapshot = snapshot,
                    vpnConnected = vpnConnected,
                )
            }
        }
    }

    override fun onCompositionError(
        context: Context,
        glanceId: GlanceId,
        appWidgetId: Int,
        throwable: Throwable,
    ) {
        val fallback = RemoteViews(context.packageName, android.R.layout.simple_list_item_1).apply {
            setTextViewText(android.R.id.text1, "DayPulse: Tap to open app")
        }
        AppWidgetManager.getInstance(context).updateAppWidget(appWidgetId, fallback)
    }

    companion object {
        private val RESPONSIVE_BUCKETS = setOf(
            DpSize(180.dp, 110.dp),
            DpSize(270.dp, 110.dp),
            DpSize(270.dp, 160.dp),
            DpSize(270.dp, 210.dp),
        )
    }
}

private val WidgetSurfaceColor = Color(0xFF0E1522)
private val WidgetCardColor = Color(0xFF162032)
private val WidgetTextPrimary = Color(0xFFF1F5F9)
private val WidgetTextSecondary = Color(0xFF94A3B8)
private val WidgetCyan = Color(0xFF2EE6D6)
private val WidgetAmber = Color(0xFFF59E0B)
private val WidgetEmerald = Color(0xFF10B981)
private val WidgetCoral = Color(0xFFF43F5E)
private val WidgetSlate = Color(0xFF64748B)

@Composable
private fun WidgetContent(
    size: DpSize,
    settings: AppSettings,
    snapshot: CachedWidgetSnapshot,
    vpnConnected: Boolean,
) {
    val isCompactWidth = size.width < 230.dp
    val isTall = size.height >= 180.dp
    val data = snapshot.widgetData
    val isConfigured = settings.baseUrl.isNotBlank()

    val rootModifier = GlanceModifier
        .fillMaxSize()
        .background(WidgetSurfaceColor)
        .let {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                it.cornerRadius(android.R.dimen.system_app_widget_background_radius)
            } else {
                it.cornerRadius(20.dp)
            }
        }
        .clickable(actionStartActivity<MainActivity>())
        .padding(start = 10.dp, end = 10.dp, top = 8.dp, bottom = 4.dp)

    Column(modifier = rootModifier) {
        WidgetHeader(
            data = data,
            isConfigured = isConfigured,
            requireVpn = settings.requireVpn,
            vpnConnected = vpnConnected,
            isCompact = isCompactWidth,
        )

        Spacer(modifier = GlanceModifier.height(4.dp))

        when {
            !isConfigured -> {
                WidgetEmptyNotice(
                    title = "Setup Server URL",
                    subtitle = "Tap to enter your desktop WireGuard IP & port 7879",
                    accentColor = WidgetCyan,
                )
            }
            settings.requireVpn && !vpnConnected && data == null -> {
                WidgetEmptyNotice(
                    title = "WireGuard VPN Off",
                    subtitle = "Connect VPN to sync today's activity breakdown",
                    accentColor = WidgetAmber,
                )
            }
            data == null || !data.hasData -> {
                WidgetEmptyNotice(
                    title = if (snapshot.lastError != null) "Waiting for Desktop" else "No Activity Today",
                    subtitle = snapshot.lastError ?: "Desktop daemon hasn't pushed data for today yet",
                    accentColor = WidgetSlate,
                )
            }
            else -> {
                WidgetBody(
                    size = size,
                    data = data,
                    isCompactWidth = isCompactWidth,
                    isTall = isTall,
                    vpnConnected = vpnConnected,
                    requireVpn = settings.requireVpn,
                    syncedAtMs = snapshot.syncedAtMs,
                )
            }
        }
    }
}

@Composable
private fun WidgetHeader(
    data: WidgetTodayResponse?,
    isConfigured: Boolean,
    requireVpn: Boolean,
    vpnConnected: Boolean,
    isCompact: Boolean,
) {
    val effectiveState = when {
        !isConfigured -> "setup"
        requireVpn && !vpnConnected -> "vpn_off"
        data == null -> "offline"
        !data.hasData || data.isStale -> "offline"
        else -> data.state.lowercase()
    }

    val (badgeLabel, badgeColor) = when (effectiveState) {
        "active" -> "ACTIVE" to WidgetEmerald
        "paused" -> "PAUSED" to WidgetAmber
        "locked" -> "LOCKED" to WidgetSlate
        "vpn_off" -> "VPN OFF" to WidgetAmber
        "setup" -> "SETUP" to WidgetCyan
        else -> "OFFLINE" to WidgetCoral
    }

    Row(
        modifier = GlanceModifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Status pill
        Row(
            modifier = GlanceModifier
                .background(WidgetCardColor)
                .cornerRadius(8.dp)
                .padding(horizontal = 8.dp, vertical = 3.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = GlanceModifier
                    .size(7.dp)
                    .background(badgeColor)
                    .cornerRadius(4.dp),
            ) {}
            Spacer(modifier = GlanceModifier.width(5.dp))
            Text(
                text = badgeLabel,
                style = TextStyle(
                    color = ColorProvider(badgeColor),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                ),
                maxLines = 1,
            )
        }

        if (!isCompact && data != null && data.date.isNotBlank()) {
            Spacer(modifier = GlanceModifier.width(8.dp))
            Text(
                text = data.date,
                style = TextStyle(
                    color = ColorProvider(WidgetTextSecondary),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                ),
                maxLines = 1,
            )
        }

        Spacer(modifier = GlanceModifier.defaultWeight())

        if (data != null && data.hasData) {
            Text(
                text = Formatters.formatDurationCompact(data.totalActiveSec),
                style = TextStyle(
                    color = ColorProvider(WidgetTextPrimary),
                    fontSize = if (isCompact) 19.sp else 22.sp,
                    fontWeight = FontWeight.Bold,
                ),
                maxLines = 1,
            )
            Spacer(modifier = GlanceModifier.width(6.dp))
        }

        Box(
            modifier = GlanceModifier
                .size(28.dp)
                .background(WidgetCardColor)
                .cornerRadius(8.dp)
                .clickable(actionRunCallback<RefreshWidgetAction>()),
            contentAlignment = Alignment.Center,
        ) {
            Image(
                provider = ImageProvider(R.drawable.ic_widget_refresh),
                contentDescription = "Refresh widget",
                colorFilter = ColorFilter.tint(ColorProvider(WidgetCyan)),
                modifier = GlanceModifier.size(16.dp),
            )
        }
    }
}

@Composable
private fun WidgetBody(
    size: DpSize,
    data: WidgetTodayResponse,
    isCompactWidth: Boolean,
    isTall: Boolean,
    vpnConnected: Boolean,
    requireVpn: Boolean,
    syncedAtMs: Long,
) {
    // Current live window strip
    val currentApp = data.currentApp
    val hasLiveBanner = data.state == "active" && !currentApp.isNullOrBlank()
    if (hasLiveBanner) {
        val appLabel = Formatters.formatFriendlyAppName(currentApp)
        val catLabel = data.currentCategory ?: "Active"
        val durLabel = Formatters.formatDurationCompact(data.currentDurationSec)
        Row(
            modifier = GlanceModifier
                .fillMaxWidth()
                .background(WidgetCardColor)
                .cornerRadius(8.dp)
                .padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "Now: $appLabel • $catLabel ($durLabel)",
                style = TextStyle(
                    color = ColorProvider(WidgetCyan),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                ),
                maxLines = 1,
            )
        }
        Spacer(modifier = GlanceModifier.height(5.dp))
    } else if (requireVpn && !vpnConnected) {
        Row(
            modifier = GlanceModifier
                .fillMaxWidth()
                .background(WidgetCardColor)
                .cornerRadius(8.dp)
                .padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Image(
                provider = ImageProvider(R.drawable.ic_widget_vpn),
                contentDescription = null,
                colorFilter = ColorFilter.tint(ColorProvider(WidgetAmber)),
                modifier = GlanceModifier.size(14.dp),
            )
            Spacer(modifier = GlanceModifier.width(6.dp))
            Text(
                text = "Cached (${Formatters.formatRelativeMillis(syncedAtMs)}) • VPN off",
                style = TextStyle(
                    color = ColorProvider(WidgetAmber),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                ),
                maxLines = 1,
            )
        }
        Spacer(modifier = GlanceModifier.height(5.dp))
    }

    // Proportional stacked category bar
    val categories = data.topCategories
    if (categories.isNotEmpty()) {
        val barTotalWidth: Dp = (size.width - 20.dp).coerceAtLeast(120.dp)
        Row(
            modifier = GlanceModifier
                .fillMaxWidth()
                .height(8.dp)
                .background(WidgetCardColor)
                .cornerRadius(4.dp),
        ) {
            categories.forEachIndexed { index, item ->
                val fraction = (item.sharePct / 100.0).toFloat().coerceIn(0.03f, 1f)
                val segWidth = (barTotalWidth * fraction).coerceAtLeast(6.dp)
                Box(
                    modifier = GlanceModifier
                        .width(segWidth)
                        .height(8.dp)
                        .background(Formatters.colorForCategory(item.category)),
                ) {}
                if (index < categories.lastIndex) {
                    Spacer(modifier = GlanceModifier.width(2.dp))
                }
            }
        }
        Spacer(modifier = GlanceModifier.height(6.dp))
    }

    // Category rows with compact vertical spacing
    val maxCategoriesToShow = when {
        isTall -> 4
        size.height >= 150.dp -> 4
        isCompactWidth -> 3
        else -> 3
    }
    val categoryTextSize = if (isCompactWidth) 14.sp else 16.sp
    val metricTextSize = if (isCompactWidth) 13.sp else 15.sp

    categories.take(maxCategoriesToShow).forEach { item ->
        Row(
            modifier = GlanceModifier
                .fillMaxWidth()
                .padding(vertical = 1.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = GlanceModifier
                    .size(9.dp)
                    .background(Formatters.colorForCategory(item.category))
                    .cornerRadius(5.dp),
            ) {}
            Spacer(modifier = GlanceModifier.width(7.dp))
            Text(
                text = item.category,
                style = TextStyle(
                    color = ColorProvider(WidgetTextPrimary),
                    fontSize = categoryTextSize,
                    fontWeight = FontWeight.Medium,
                ),
                maxLines = 1,
                modifier = GlanceModifier.defaultWeight(),
            )
            Spacer(modifier = GlanceModifier.width(6.dp))
            Text(
                text = "${Formatters.formatDurationCompact(item.totalSec)}  ${Formatters.formatSharePct(item.sharePct)}",
                style = TextStyle(
                    color = ColorProvider(WidgetTextSecondary),
                    fontSize = metricTextSize,
                    fontWeight = FontWeight.Medium,
                ),
                maxLines = 1,
            )
        }
    }

    // Top Apps section on taller widgets
    if (isTall && data.topApps.isNotEmpty()) {
        Spacer(modifier = GlanceModifier.height(6.dp))
        Text(
            text = "TOP APPS",
            style = TextStyle(
                color = ColorProvider(WidgetTextSecondary),
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
            ),
        )
        Spacer(modifier = GlanceModifier.height(2.dp))
        data.topApps.take(3).forEach { app ->
            Row(
                modifier = GlanceModifier
                    .fillMaxWidth()
                    .padding(vertical = 1.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = Formatters.formatFriendlyAppName(app.appName),
                    style = TextStyle(
                        color = ColorProvider(WidgetTextPrimary),
                        fontSize = 14.sp,
                    ),
                    maxLines = 1,
                    modifier = GlanceModifier.defaultWeight(),
                )
                Text(
                    text = Formatters.formatDurationCompact(app.totalSec),
                    style = TextStyle(
                        color = ColorProvider(WidgetCyan),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                    ),
                    maxLines = 1,
                )
            }
        }
    }
}

@Composable
private fun WidgetEmptyNotice(
    title: String,
    subtitle: String,
    accentColor: Color,
) {
    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(WidgetCardColor)
            .cornerRadius(12.dp)
            .padding(start = 10.dp, end = 10.dp, top = 8.dp, bottom = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = title,
            style = TextStyle(
                color = ColorProvider(accentColor),
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold,
            ),
            maxLines = 1,
        )
        Spacer(modifier = GlanceModifier.height(4.dp))
        Text(
            text = subtitle,
            style = TextStyle(
                color = ColorProvider(WidgetTextSecondary),
                fontSize = 14.sp,
            ),
            maxLines = 2,
        )
    }
}
