package com.example.ui.components

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AddToHomeScreen
import androidx.compose.material.icons.rounded.DesktopWindows
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.PauseCircle
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material.icons.rounded.VpnKey
import androidx.compose.material.icons.rounded.WifiOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.model.CategorySummary
import com.example.model.LiveStatusResponse
import com.example.model.SummaryStatus
import com.example.model.WidgetTodayResponse
import com.example.ui.theme.MonoMetricLarge
import com.example.ui.theme.MonoMetricMedium
import com.example.ui.theme.StatusActive
import com.example.ui.theme.StatusLocked
import com.example.ui.theme.StatusOffline
import com.example.ui.theme.StatusPaused
import com.example.util.Formatters
import com.example.vpn.VpnStatus

@Composable
fun VpnStatusPill(
    vpnStatus: VpnStatus,
    requireVpn: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val (label, tint, containerTint) = when {
        vpnStatus.isVpnConnected -> {
            val iface = vpnStatus.interfaceName?.let { " ($it)" } ?: ""
            Triple("WireGuard VPN$iface", StatusActive, StatusActive.copy(alpha = 0.14f))
        }
        requireVpn -> {
            Triple("VPN Disconnected", StatusPaused, StatusPaused.copy(alpha = 0.14f))
        }
        else -> {
            Triple("LAN Direct Mode", MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.primary.copy(alpha = 0.14f))
        }
    }

    Surface(
        modifier = modifier
            .minimumInteractiveComponentSize()
            .clip(RoundedCornerShape(50))
            .clickable(onClick = onClick)
            .testTag("vpn_status_pill"),
        shape = RoundedCornerShape(50),
        color = containerTint,
        border = BorderStroke(1.dp, tint.copy(alpha = 0.35f)),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = Icons.Rounded.Shield,
                contentDescription = "VPN tunnel status",
                tint = tint,
                modifier = Modifier.size(15.dp),
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                color = tint,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
fun LiveActivityBanner(
    liveStatus: LiveStatusResponse?,
    fallbackStatus: SummaryStatus?,
    liveDurationSec: Double?,
    isStale: Boolean,
    hasData: Boolean,
    modifier: Modifier = Modifier,
) {
    val effectiveState = liveStatus?.state ?: when {
        !hasData || isStale -> "offline"
        fallbackStatus?.screenLocked == true -> "locked"
        fallbackStatus?.paused == true -> "paused"
        fallbackStatus?.tracking == true -> "active"
        else -> "offline"
    }

    val currentApp = liveStatus?.currentApp ?: fallbackStatus?.currentApp
    val currentTitle = liveStatus?.currentTitle ?: fallbackStatus?.currentTitle
    val currentCategory = liveStatus?.currentCategory ?: fallbackStatus?.currentCategory

    val (badgeText, badgeColor, statusIcon) = when (effectiveState) {
        "active" -> Triple("LIVE • ACTIVE", StatusActive, Icons.Rounded.DesktopWindows)
        "paused" -> Triple("PAUSED", StatusPaused, Icons.Rounded.PauseCircle)
        "locked" -> Triple("SCREEN LOCKED", StatusLocked, Icons.Rounded.Lock)
        else -> Triple("OFFLINE", StatusOffline, Icons.Rounded.WifiOff)
    }

    val infiniteTransition = rememberInfiniteTransition(label = "live_pulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 0.25f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 900),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "pulse_alpha",
    )

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("live_activity_banner"),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer,
        ),
        border = BorderStroke(1.dp, badgeColor.copy(alpha = 0.35f)),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(
                            badgeColor.copy(alpha = 0.12f),
                            Color.Transparent,
                        ),
                    ),
                )
                .padding(16.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(9.dp)
                            .alpha(if (effectiveState == "active") pulseAlpha else 1f)
                            .background(badgeColor, CircleShape),
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = badgeText,
                        style = MaterialTheme.typography.labelMedium,
                        color = badgeColor,
                        fontWeight = FontWeight.Bold,
                    )
                }

                if (effectiveState == "active" && liveDurationSec != null) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.surfaceContainerHigh,
                    ) {
                        Text(
                            text = Formatters.formatDuration(liveDurationSec),
                            style = MonoMetricMedium,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            if (effectiveState == "active" && !currentApp.isNullOrBlank()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = statusIcon,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp),
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = Formatters.formatFriendlyAppName(currentApp),
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false),
                    )
                    if (!currentCategory.isNullOrBlank()) {
                        Spacer(modifier = Modifier.width(8.dp))
                        val catColor = Formatters.colorForCategory(currentCategory)
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = catColor.copy(alpha = 0.18f),
                        ) {
                            Text(
                                text = currentCategory,
                                style = MaterialTheme.typography.labelSmall,
                                color = catColor,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                            )
                        }
                    }
                }
                if (!currentTitle.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = currentTitle,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            } else {
                val subtitle = when (effectiveState) {
                    "paused" -> "Activity tracking is currently paused on your desktop."
                    "locked" -> "Desktop screen is locked. Tracking resumes on unlock."
                    else -> "Desktop daemon is offline or data is stale (>60s)."
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = statusIcon,
                        contentDescription = null,
                        tint = badgeColor,
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

@Composable
fun CategoryDonutChart(
    categories: List<CategorySummary>,
    totalActiveSec: Double,
    modifier: Modifier = Modifier,
) {
    val trackColor = MaterialTheme.colorScheme.surfaceContainerHigh
    Box(
        modifier = modifier.size(164.dp),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val strokeWidth = 18.dp.toPx()
            val inset = strokeWidth / 2f
            val arcSize = Size(size.width - strokeWidth, size.height - strokeWidth)
            val topLeft = Offset(inset, inset)

            // Background ring
            drawArc(
                color = trackColor,
                startAngle = 0f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round),
            )

            if (categories.isNotEmpty()) {
                val totalPct = categories.sumOf { it.sharePct }.coerceAtLeast(1.0)
                val gapDegrees = if (categories.size > 1) 4f else 0f
                val totalGap = gapDegrees * categories.size
                val availableSweep = (360f - totalGap).coerceAtLeast(60f)

                var startAngle = -90f
                categories.forEach { cat ->
                    val sweep = ((cat.sharePct / totalPct) * availableSweep).toFloat().coerceAtLeast(4f)
                    drawArc(
                        color = Formatters.colorForCategory(cat.category),
                        startAngle = startAngle,
                        sweepAngle = sweep,
                        useCenter = false,
                        topLeft = topLeft,
                        size = arcSize,
                        style = Stroke(width = strokeWidth, cap = StrokeCap.Butt),
                    )
                    startAngle += sweep + gapDegrees
                }
            }
        }

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = Formatters.formatDurationCompact(totalActiveSec),
                style = MonoMetricLarge.copy(fontSize = 24.sp),
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = "ACTIVE TODAY",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
fun StackedCategoryBar(
    categories: List<CategorySummary>,
    modifier: Modifier = Modifier,
) {
    if (categories.isEmpty()) return
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(10.dp)
            .clip(RoundedCornerShape(6.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerHigh),
        horizontalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        categories.forEach { cat ->
            val weight = (cat.sharePct.toFloat() / 100f).coerceAtLeast(0.03f)
            Box(
                modifier = Modifier
                    .weight(weight)
                    .height(10.dp)
                    .background(Formatters.colorForCategory(cat.category)),
            )
        }
    }
}

@Composable
fun WidgetPreviewCard(
    widgetData: WidgetTodayResponse?,
    pinnedCount: Int,
    canRequestPin: Boolean,
    vpnConnected: Boolean,
    requireVpn: Boolean,
    onPinWidget: () -> Unit,
    onRefreshWidget: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("widget_preview_card"),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer,
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Home Screen Widget",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                        text = if (pinnedCount > 0) {
                            "$pinnedCount widget instance${if (pinnedCount > 1) "s" else ""} active on launcher"
                        } else {
                            "Live preview • Add to your Android home screen"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                OutlinedButton(
                    onClick = onRefreshWidget,
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .testTag("sync_widget_button"),
                    shape = RoundedCornerShape(12.dp),
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Refresh,
                        contentDescription = "Sync widget",
                        modifier = Modifier.size(16.dp),
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Sync")
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Simulated Glance widget surface matching DayPulseWidget.kt
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                color = Color(0xFF0E1522),
                border = BorderStroke(1.dp, Color(0xFF23324D)),
            ) {
                Column(
                    modifier = Modifier.padding(
                        start = 12.dp,
                        end = 12.dp,
                        top = 10.dp,
                        bottom = 6.dp,
                    ),
                ) {
                    val stateLabel = when {
                        requireVpn && !vpnConnected -> "VPN OFF" to StatusPaused
                        widgetData == null || !widgetData.hasData || widgetData.isStale -> "OFFLINE" to StatusOffline
                        widgetData.state == "active" -> "ACTIVE" to StatusActive
                        widgetData.state == "paused" -> "PAUSED" to StatusPaused
                        else -> "LOCKED" to StatusLocked
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = Color(0xFF162032),
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(7.dp)
                                        .background(stateLabel.second, CircleShape),
                                )
                                Spacer(modifier = Modifier.width(5.dp))
                                Text(
                                    text = stateLabel.first,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = stateLabel.second,
                                    fontWeight = FontWeight.Bold,
                                )
                            }
                        }

                        if (widgetData != null && widgetData.date.isNotBlank()) {
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = widgetData.date,
                                style = MaterialTheme.typography.labelMedium,
                                color = Color(0xFF94A3B8),
                            )
                        }

                        Spacer(modifier = Modifier.weight(1f))

                        Text(
                            text = Formatters.formatDurationCompact(widgetData?.totalActiveSec),
                            style = MonoMetricMedium.copy(fontSize = 20.sp),
                            color = Color(0xFFF1F5F9),
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    val categories = widgetData?.topCategories.orEmpty()
                    if (categories.isNotEmpty()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(Color(0xFF162032)),
                            horizontalArrangement = Arrangement.spacedBy(2.dp),
                        ) {
                            categories.forEach { cat ->
                                Box(
                                    modifier = Modifier
                                        .weight((cat.sharePct.toFloat() / 100f).coerceAtLeast(0.04f))
                                        .height(8.dp)
                                        .background(Formatters.colorForCategory(cat.category)),
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        categories.take(3).forEach { cat ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 2.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(10.dp)
                                        .background(Formatters.colorForCategory(cat.category), CircleShape),
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = cat.category,
                                    style = MaterialTheme.typography.titleSmall.copy(fontSize = 15.sp),
                                    color = Color(0xFFF1F5F9),
                                    modifier = Modifier.weight(1f),
                                )
                                Text(
                                    text = "${Formatters.formatDurationCompact(cat.totalSec)}  ${Formatters.formatSharePct(cat.sharePct)}",
                                    style = MaterialTheme.typography.labelLarge.copy(fontSize = 14.sp),
                                    color = Color(0xFF94A3B8),
                                )
                            }
                        }
                    } else {
                        Text(
                            text = "Widget will display your top categories and live window once synced.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color(0xFF94A3B8),
                        )
                    }
                }
            }

            if (canRequestPin) {
                Spacer(modifier = Modifier.height(12.dp))
                Button(
                    onClick = onPinWidget,
                    modifier = Modifier
                        .fillMaxWidth()
                        .minimumInteractiveComponentSize()
                        .testTag("pin_widget_button"),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary,
                    ),
                ) {
                    Icon(
                        imageVector = Icons.Rounded.AddToHomeScreen,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (pinnedCount > 0) "Add Another Widget to Home Screen" else "Pin Widget to Home Screen",
                        style = MaterialTheme.typography.labelLarge,
                    )
                }
            }
        }
    }
}

@Composable
fun HeroSetupOrVpnCard(
    isConfigured: Boolean,
    blockedByVpn: Boolean,
    errorMessage: String?,
    onOpenSettings: () -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("hero_status_card"),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer,
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)),
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(150.dp),
            ) {
                Image(
                    painter = painterResource(id = R.drawable.img_vpn_telemetry_banner_1794765395423),
                    contentDescription = "WireGuard VPN telemetry banner",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                )
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color.Transparent,
                                    MaterialTheme.colorScheme.surfaceContainer,
                                ),
                            ),
                        ),
                )
            }

            Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 14.dp)) {
                val (icon, title, description) = when {
                    !isConfigured -> Triple(
                        Icons.Rounded.Settings,
                        "Connect Your Activity Server",
                        "Enter your desktop's WireGuard IP and port 7879 in Settings to sync today's time breakdown and power your home screen widget.",
                    )
                    blockedByVpn -> Triple(
                        Icons.Rounded.VpnKey,
                        "Waiting for WireGuard VPN",
                        "VPN-only sync is enabled. Connect your WireGuard tunnel to fetch today's activity, or toggle LAN Direct Mode in Settings.",
                    )
                    else -> Triple(
                        Icons.Rounded.WifiOff,
                        "Unable to Reach Desktop Server",
                        errorMessage ?: "Make sure the activity tracker server is listening on port 7879 and reachable over WireGuard.",
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(22.dp),
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                Spacer(modifier = Modifier.height(16.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Button(
                        onClick = onOpenSettings,
                        modifier = Modifier
                            .weight(1f)
                            .minimumInteractiveComponentSize()
                            .testTag("hero_open_settings_button"),
                        shape = RoundedCornerShape(14.dp),
                    ) {
                        Text("Open Settings")
                    }
                    if (isConfigured) {
                        OutlinedButton(
                            onClick = onRetry,
                            modifier = Modifier
                                .weight(1f)
                                .minimumInteractiveComponentSize()
                                .testTag("hero_retry_button"),
                            shape = RoundedCornerShape(14.dp),
                        ) {
                            Text("Retry Sync")
                        }
                    }
                }
            }
        }
    }
}
