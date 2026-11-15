package com.example.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class WidgetTodayResponse(
    @Json(name = "date") val date: String = "",
    @Json(name = "has_data") val hasData: Boolean = false,
    @Json(name = "is_stale") val isStale: Boolean = false,
    @Json(name = "updated_at") val updatedAt: Double = 0.0,
    @Json(name = "total_active_sec") val totalActiveSec: Double = 0.0,
    @Json(name = "state") val state: String = "offline",
    @Json(name = "current_app") val currentApp: String? = null,
    @Json(name = "current_category") val currentCategory: String? = null,
    @Json(name = "current_duration_sec") val currentDurationSec: Double? = null,
    @Json(name = "top_categories") val topCategories: List<WidgetCategoryItem> = emptyList(),
    @Json(name = "top_apps") val topApps: List<WidgetAppItem> = emptyList(),
)

@JsonClass(generateAdapter = true)
data class WidgetCategoryItem(
    @Json(name = "category") val category: String,
    @Json(name = "total_sec") val totalSec: Double,
    @Json(name = "share_pct") val sharePct: Double,
)

@JsonClass(generateAdapter = true)
data class WidgetAppItem(
    @Json(name = "app_name") val appName: String,
    @Json(name = "total_sec") val totalSec: Double,
)

@JsonClass(generateAdapter = true)
data class SummaryTodayResponse(
    @Json(name = "has_data") val hasData: Boolean = false,
    @Json(name = "is_stale") val isStale: Boolean = false,
    @Json(name = "date") val date: String = "",
    @Json(name = "source_host") val sourceHost: String? = null,
    @Json(name = "updated_at") val updatedAt: Double = 0.0,
    @Json(name = "total_active_sec") val totalActiveSec: Double = 0.0,
    @Json(name = "status") val status: SummaryStatus = SummaryStatus(),
    @Json(name = "categories") val categories: List<CategorySummary> = emptyList(),
    @Json(name = "apps") val apps: List<AppSummary> = emptyList(),
    @Json(name = "top_sites") val topSites: List<TopSiteSummary> = emptyList(),
    @Json(name = "recent_files") val recentFiles: List<RecentFileSummary> = emptyList(),
)

@JsonClass(generateAdapter = true)
data class SummaryStatus(
    @Json(name = "tracking") val tracking: Boolean = false,
    @Json(name = "paused") val paused: Boolean = false,
    @Json(name = "screen_locked") val screenLocked: Boolean = false,
    @Json(name = "current_app") val currentApp: String? = null,
    @Json(name = "current_title") val currentTitle: String? = null,
    @Json(name = "current_category") val currentCategory: String? = null,
    @Json(name = "current_duration_sec") val currentDurationSec: Double? = null,
)

@JsonClass(generateAdapter = true)
data class CategorySummary(
    @Json(name = "category") val category: String,
    @Json(name = "total_sec") val totalSec: Double,
    @Json(name = "share_pct") val sharePct: Double,
    @Json(name = "session_count") val sessionCount: Int = 0,
    @Json(name = "app_count") val appCount: Int = 0,
)

@JsonClass(generateAdapter = true)
data class AppSummary(
    @Json(name = "app_name") val appName: String,
    @Json(name = "category") val category: String,
    @Json(name = "total_sec") val totalSec: Double,
    @Json(name = "session_count") val sessionCount: Int = 0,
    @Json(name = "file_count") val fileCount: Int = 0,
)

@JsonClass(generateAdapter = true)
data class TopSiteSummary(
    @Json(name = "domain") val domain: String,
    @Json(name = "page_title") val pageTitle: String? = null,
    @Json(name = "app_name") val appName: String? = null,
    @Json(name = "last_seen_at") val lastSeenAt: Double = 0.0,
)

@JsonClass(generateAdapter = true)
data class RecentFileSummary(
    @Json(name = "app_name") val appName: String,
    @Json(name = "file_path") val filePath: String,
    @Json(name = "source") val source: String? = null,
    @Json(name = "last_seen_at") val lastSeenAt: Double = 0.0,
)

@JsonClass(generateAdapter = true)
data class LiveStatusResponse(
    @Json(name = "has_data") val hasData: Boolean = false,
    @Json(name = "is_stale") val isStale: Boolean = false,
    @Json(name = "updated_at") val updatedAt: Double = 0.0,
    @Json(name = "state") val state: String = "offline",
    @Json(name = "tracking") val tracking: Boolean = false,
    @Json(name = "paused") val paused: Boolean = false,
    @Json(name = "screen_locked") val screenLocked: Boolean = false,
    @Json(name = "current_app") val currentApp: String? = null,
    @Json(name = "current_title") val currentTitle: String? = null,
    @Json(name = "current_category") val currentCategory: String? = null,
    @Json(name = "current_duration_sec") val currentDurationSec: Double? = null,
)

@JsonClass(generateAdapter = true)
data class HealthResponse(
    @Json(name = "status") val status: String = "",
    @Json(name = "has_data") val hasData: Boolean = false,
    @Json(name = "date") val date: String? = null,
    @Json(name = "updated_at") val updatedAt: Double? = null,
)

@JsonClass(generateAdapter = true)
data class ApiErrorResponse(
    @Json(name = "error") val error: String = "Unknown error",
)
