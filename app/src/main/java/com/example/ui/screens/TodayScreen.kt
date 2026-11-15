package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Computer
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.model.CategorySummary
import com.example.ui.components.CategoryDonutChart
import com.example.ui.components.HeroSetupOrVpnCard
import com.example.ui.components.LiveActivityBanner
import com.example.ui.components.StackedCategoryBar
import com.example.ui.components.VpnStatusPill
import com.example.ui.components.WidgetPreviewCard
import com.example.ui.theme.MonoMetricMedium
import com.example.ui.viewmodel.MainUiState
import com.example.util.Formatters

@Composable
fun TodayScreen(
    uiState: MainUiState,
    onRefresh: () -> Unit,
    onOpenSettings: () -> Unit,
    onPinWidget: () -> Unit,
    onRefreshWidget: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val summary = uiState.summary

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter,
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 720.dp)
                .testTag("today_screen_list"),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "DayPulse",
                            style = MaterialTheme.typography.headlineMedium,
                            color = MaterialTheme.colorScheme.onBackground,
                        )
                        val syncSubtitle = when {
                            uiState.lastSyncedAtMs > 0L -> "Synced ${Formatters.formatRelativeMillis(uiState.lastSyncedAtMs)}"
                            uiState.isConfigured -> "Ready to sync"
                            else -> "Setup required"
                        }
                        Text(
                            text = syncSubtitle,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        VpnStatusPill(
                            vpnStatus = uiState.vpnStatus,
                            requireVpn = uiState.settings.requireVpn,
                            onClick = onOpenSettings,
                        )

                        IconButton(
                            onClick = onRefresh,
                            enabled = !uiState.isRefreshing,
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .testTag("refresh_dashboard_button"),
                        ) {
                            if (uiState.isRefreshing) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    strokeWidth = 2.dp,
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Rounded.Refresh,
                                    contentDescription = "Refresh dashboard",
                                    tint = MaterialTheme.colorScheme.primary,
                                )
                            }
                        }
                    }
                }
            }

            if (!uiState.isConfigured || (summary == null && (uiState.blockedByVpn || uiState.errorMessage != null))) {
                item {
                    HeroSetupOrVpnCard(
                        isConfigured = uiState.isConfigured,
                        blockedByVpn = uiState.blockedByVpn,
                        errorMessage = uiState.errorMessage,
                        onOpenSettings = onOpenSettings,
                        onRetry = onRefresh,
                    )
                }
            }

            if (summary != null) {
                item {
                    LiveActivityBanner(
                        liveStatus = uiState.liveStatus,
                        fallbackStatus = summary.status,
                        liveDurationSec = uiState.liveTickerSec ?: summary.status.currentDurationSec,
                        isStale = uiState.liveStatus?.isStale ?: summary.isStale,
                        hasData = uiState.liveStatus?.hasData ?: summary.hasData,
                    )
                }

                item {
                    DailyOverviewCard(
                        date = summary.date,
                        sourceHost = summary.sourceHost,
                        totalActiveSec = summary.totalActiveSec,
                        updatedAt = summary.updatedAt,
                        categories = summary.categories,
                        hasData = summary.hasData,
                    )
                }

                if (summary.categories.isNotEmpty()) {
                    item {
                        Text(
                            text = "TIME BY CATEGORY",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 4.dp),
                        )
                    }

                    items(
                        items = summary.categories,
                        key = { it.category },
                    ) { category ->
                        CategoryBreakdownRowCard(category = category)
                    }
                }
            }

            item {
                WidgetPreviewCard(
                    widgetData = uiState.effectiveWidgetPreview,
                    pinnedCount = uiState.pinnedWidgetCount,
                    canRequestPin = uiState.canRequestPinWidget,
                    vpnConnected = uiState.vpnStatus.isVpnConnected,
                    requireVpn = uiState.settings.requireVpn,
                    onPinWidget = onPinWidget,
                    onRefreshWidget = onRefreshWidget,
                )
            }

            item {
                Spacer(modifier = Modifier.height(12.dp))
            }
        }
    }
}

@Composable
private fun DailyOverviewCard(
    date: String,
    sourceHost: String?,
    totalActiveSec: Double,
    updatedAt: Double,
    categories: List<CategorySummary>,
    hasData: Boolean,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("daily_overview_card"),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer,
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.45f)),
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Rounded.Schedule,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (date.isNotBlank()) "Summary • $date" else "Today's Summary",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }

                if (!sourceHost.isNullOrBlank()) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.surfaceContainerHigh,
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Computer,
                                contentDescription = "Source desktop host",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(13.dp),
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = sourceHost,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            if (!hasData) {
                Text(
                    text = "No activity recorded yet today.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(18.dp),
                ) {
                    CategoryDonutChart(
                        categories = categories,
                        totalActiveSec = totalActiveSec,
                    )

                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        categories.take(4).forEach { cat ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(9.dp)
                                        .background(Formatters.colorForCategory(cat.category), CircleShape),
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = cat.category,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f),
                                )
                                Text(
                                    text = Formatters.formatSharePct(cat.sharePct),
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }

                        if (updatedAt > 0.0) {
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Host pushed ${Formatters.formatRelativeUnixSeconds(updatedAt)}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))
                StackedCategoryBar(categories = categories)
            }
        }
    }
}

@Composable
private fun CategoryBreakdownRowCard(category: CategorySummary) {
    val accent = Formatters.colorForCategory(category.category)
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("category_card_${category.category.lowercase()}"),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer,
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier
                        .size(11.dp)
                        .background(accent, CircleShape),
                )
                Spacer(modifier = Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = category.category,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                        text = "${category.sessionCount} sessions • ${category.appCount} app${if (category.appCount == 1) "" else "s"}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = Formatters.formatDurationCompact(category.totalSec),
                        style = MonoMetricMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        text = Formatters.formatSharePct(category.sharePct),
                        style = MaterialTheme.typography.labelMedium,
                        color = accent,
                    )
                }
            }
            Spacer(modifier = Modifier.height(10.dp))
            LinearProgressIndicator(
                progress = { (category.sharePct / 100.0).toFloat().coerceIn(0f, 1f) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(4.dp)),
                color = accent,
                trackColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                strokeCap = StrokeCap.Round,
            )
        }
    }
}
