package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Apps
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.Language
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
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
import com.example.model.AppSummary
import com.example.model.RecentFileSummary
import com.example.model.TopSiteSummary
import com.example.ui.theme.MonoMetricMedium
import com.example.ui.viewmodel.ActivityLogTab
import com.example.ui.viewmodel.MainUiState
import com.example.util.Formatters

@Composable
fun ActivityScreen(
    uiState: MainUiState,
    onSelectTab: (ActivityLogTab) -> Unit,
    onSearchChanged: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val summary = uiState.summary
    val query = uiState.activitySearchQuery.trim()
    val totalSec = (summary?.totalActiveSec ?: 0.0).coerceAtLeast(1.0)

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter,
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 720.dp)
                .padding(horizontal = 16.dp),
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Activity Log",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onBackground,
            )
            Text(
                text = "Apps, visited domains, and active files tracked today",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Spacer(modifier = Modifier.height(14.dp))

            SingleChoiceSegmentedButtonRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("activity_tab_selector"),
            ) {
                ActivityLogTab.entries.forEachIndexed { index, tab ->
                    val label = when (tab) {
                        ActivityLogTab.APPS -> "Apps (${summary?.apps?.size ?: 0})"
                        ActivityLogTab.SITES -> "Sites (${summary?.topSites?.size ?: 0})"
                        ActivityLogTab.FILES -> "Files (${summary?.recentFiles?.size ?: 0})"
                    }
                    SegmentedButton(
                        selected = uiState.activityTab == tab,
                        onClick = { onSelectTab(tab) },
                        shape = SegmentedButtonDefaults.itemShape(
                            index = index,
                            count = ActivityLogTab.entries.size,
                        ),
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("activity_tab_${tab.name.lowercase()}"),
                    ) {
                        Text(
                            text = label,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = uiState.activitySearchQuery,
                onValueChange = onSearchChanged,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("activity_search_input"),
                placeholder = { Text("Filter by app, domain, or file path…") },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Rounded.Search,
                        contentDescription = "Search activity",
                    )
                },
                trailingIcon = {
                    if (uiState.activitySearchQuery.isNotEmpty()) {
                        IconButton(
                            onClick = { onSearchChanged("") },
                            modifier = Modifier.minimumInteractiveComponentSize(),
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Close,
                                contentDescription = "Clear search",
                            )
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
            )

            Spacer(modifier = Modifier.height(12.dp))

            when (uiState.activityTab) {
                ActivityLogTab.APPS -> {
                    val apps = summary?.apps.orEmpty().filter {
                        query.isEmpty() ||
                            it.appName.contains(query, ignoreCase = true) ||
                            Formatters.formatFriendlyAppName(it.appName).contains(query, ignoreCase = true) ||
                            it.category.contains(query, ignoreCase = true)
                    }
                    if (apps.isEmpty()) {
                        EmptyActivityState(
                            icon = Icons.Rounded.Apps,
                            message = if (summary == null) "Sync with your desktop server to view app usage." else "No matching applications.",
                        )
                    } else {
                        LazyColumn(
                            contentPadding = PaddingValues(bottom = 24.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            items(apps, key = { "${it.appName}_${it.category}" }) { app ->
                                AppUsageCard(app = app, totalDaySec = totalSec)
                            }
                        }
                    }
                }

                ActivityLogTab.SITES -> {
                    val sites = summary?.topSites.orEmpty().filter {
                        query.isEmpty() ||
                            it.domain.contains(query, ignoreCase = true) ||
                            (it.pageTitle?.contains(query, ignoreCase = true) == true)
                    }
                    if (sites.isEmpty()) {
                        EmptyActivityState(
                            icon = Icons.Rounded.Language,
                            message = if (summary == null) "Sync with your desktop server to view visited sites." else "No matching visited domains.",
                        )
                    } else {
                        LazyColumn(
                            contentPadding = PaddingValues(bottom = 24.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            items(sites, key = { "${it.domain}_${it.lastSeenAt}" }) { site ->
                                TopSiteCard(site = site)
                            }
                        }
                    }
                }

                ActivityLogTab.FILES -> {
                    val files = summary?.recentFiles.orEmpty().filter {
                        query.isEmpty() ||
                            it.filePath.contains(query, ignoreCase = true) ||
                            it.appName.contains(query, ignoreCase = true)
                    }
                    if (files.isEmpty()) {
                        EmptyActivityState(
                            icon = Icons.Rounded.Description,
                            message = if (summary == null) "Sync with your desktop server to view recent files." else "No matching recent files.",
                        )
                    } else {
                        LazyColumn(
                            contentPadding = PaddingValues(bottom = 24.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            items(files, key = { "${it.filePath}_${it.lastSeenAt}" }) { file ->
                                RecentFileCard(file = file)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AppUsageCard(
    app: AppSummary,
    totalDaySec: Double,
) {
    val accent = Formatters.colorForCategory(app.category)
    val share = (app.totalSec / totalDaySec).toFloat().coerceIn(0f, 1f)

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)),
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = Formatters.formatFriendlyAppName(app.appName),
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = accent.copy(alpha = 0.16f),
                        ) {
                            Text(
                                text = app.category,
                                style = MaterialTheme.typography.labelSmall,
                                color = accent,
                                modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp),
                            )
                        }
                    }
                    Text(
                        text = "${app.appName} • ${app.sessionCount} sessions" +
                            if (app.fileCount > 0) " • ${app.fileCount} files" else "",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = Formatters.formatDurationCompact(app.totalSec),
                    style = MonoMetricMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold,
                )
            }
            Spacer(modifier = Modifier.height(10.dp))
            LinearProgressIndicator(
                progress = { share },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(5.dp)
                    .clip(RoundedCornerShape(3.dp)),
                color = accent,
                trackColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                strokeCap = StrokeCap.Round,
            )
        }
    }
}

@Composable
private fun TopSiteCard(site: TopSiteSummary) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.14f),
                modifier = Modifier.size(40.dp),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Rounded.Language,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp),
                    )
                }
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = site.domain,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (!site.pageTitle.isNullOrBlank()) {
                    Text(
                        text = site.pageTitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                val meta = buildString {
                    if (!site.appName.isNullOrBlank()) {
                        append(Formatters.formatFriendlyAppName(site.appName))
                        append(" • ")
                    }
                    append(Formatters.formatRelativeUnixSeconds(site.lastSeenAt))
                }
                Text(
                    text = meta,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun RecentFileCard(file: RecentFileSummary) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.14f),
                modifier = Modifier.size(40.dp),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Rounded.Description,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.size(20.dp),
                    )
                }
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = Formatters.extractFileName(file.filePath),
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                val parentDir = Formatters.extractParentDirectory(file.filePath)
                if (parentDir.isNotBlank()) {
                    Text(
                        text = parentDir,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                val meta = buildString {
                    append(Formatters.formatFriendlyAppName(file.appName))
                    if (!file.source.isNullOrBlank()) {
                        append(" (${file.source})")
                    }
                    append(" • ")
                    append(Formatters.formatRelativeUnixSeconds(file.lastSeenAt))
                }
                Text(
                    text = meta,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        }
    }
}

@Composable
private fun EmptyActivityState(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    message: String,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 48.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(42.dp),
            )
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
