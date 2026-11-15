package com.example.util

import androidx.compose.ui.graphics.Color
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToLong

object Formatters {

    fun formatDuration(totalSeconds: Double?): String {
        if (totalSeconds == null || totalSeconds < 0) return "0m"
        val sec = totalSeconds.roundToLong()
        val hours = sec / 3600
        val minutes = (sec % 3600) / 60
        val seconds = sec % 60
        return when {
            hours > 0 -> "${hours}h ${minutes.toString().padStart(2, '0')}m"
            minutes > 0 -> "${minutes}m ${seconds.toString().padStart(2, '0')}s"
            else -> "${seconds}s"
        }
    }

    fun formatDurationCompact(totalSeconds: Double?): String {
        if (totalSeconds == null || totalSeconds < 0) return "0m"
        val sec = totalSeconds.roundToLong()
        val hours = sec / 3600
        val minutes = (sec % 3600) / 60
        return when {
            hours > 0 -> "${hours}h ${minutes.toString().padStart(2, '0')}m"
            minutes > 0 -> "${minutes}m"
            else -> "${sec}s"
        }
    }

    fun formatSharePct(sharePct: Double): String {
        return String.format(Locale.US, "%.1f%%", sharePct)
    }

    fun formatFriendlyAppName(rawAppName: String?): String {
        if (rawAppName.isNullOrBlank()) return "Unknown"
        val trimmed = rawAppName.trim()
        val known = when (trimmed.lowercase(Locale.ROOT)) {
            "dev.zed.zed" -> "Zed"
            "org.mozilla.firefox" -> "Firefox"
            "com.google.chrome", "google-chrome" -> "Chrome"
            "com.brave.browser" -> "Brave"
            "com.visualstudio.code", "code" -> "VS Code"
            "org.jetbrains.kotlin", "jetbrains-idea", "intellij" -> "IntelliJ IDEA"
            "com.slack.slack", "slack" -> "Slack"
            "com.discordapp.discord", "discord" -> "Discord"
            "org.gnome.terminal", "alacritty", "kitty", "com.mitchellh.ghostty" -> {
                trimmed.substringAfterLast('.').replaceFirstChar { it.uppercase() }
            }
            else -> null
        }
        if (known != null) return known

        val lastSegment = trimmed.substringAfterLast('.')
        return if (lastSegment.isNotBlank()) {
            lastSegment.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.ROOT) else it.toString() }
        } else {
            trimmed
        }
    }

    fun formatRelativeUnixSeconds(unixSeconds: Double?, nowMillis: Long = System.currentTimeMillis()): String {
        if (unixSeconds == null || unixSeconds <= 0.0) return "Never"
        val diffSeconds = ((nowMillis / 1000.0) - unixSeconds).roundToLong()
        return when {
            diffSeconds < 10 -> "Just now"
            diffSeconds < 60 -> "${diffSeconds}s ago"
            diffSeconds < 3600 -> "${diffSeconds / 60}m ago"
            diffSeconds < 86400 -> "${diffSeconds / 3600}h ago"
            else -> "${diffSeconds / 86400}d ago"
        }
    }

    fun formatRelativeMillis(millis: Long, nowMillis: Long = System.currentTimeMillis()): String {
        if (millis <= 0L) return "Never synced"
        val diffSeconds = ((nowMillis - millis) / 1000L).coerceAtLeast(0L)
        return when {
            diffSeconds < 10 -> "Just now"
            diffSeconds < 60 -> "${diffSeconds}s ago"
            diffSeconds < 3600 -> "${diffSeconds / 60}m ago"
            else -> "${diffSeconds / 3600}h ago"
        }
    }

    fun extractFileName(filePath: String): String {
        return filePath.trimEnd('/').substringAfterLast('/', filePath)
    }

    fun extractParentDirectory(filePath: String): String {
        val lastSlash = filePath.trimEnd('/').lastIndexOf('/')
        return if (lastSlash > 0) filePath.substring(0, lastSlash) else ""
    }

    private val categoryPalette = listOf(
        Color(0xFF2EE6D6), // Electric Cyan
        Color(0xFF6366F1), // Indigo
        Color(0xFFF59E0B), // Amber
        Color(0xFF10B981), // Emerald
        Color(0xFFEC4899), // Rose
        Color(0xFF38BDF8), // Sky
        Color(0xFFA855F7), // Violet
    )

    fun colorForCategory(category: String): Color {
        return when (category.trim().lowercase(Locale.ROOT)) {
            "development", "coding", "engineering" -> Color(0xFF2EE6D6)
            "learning", "research", "reading", "docs" -> Color(0xFF6366F1)
            "communication", "chat", "email", "meetings" -> Color(0xFFF59E0B)
            "design", "creative", "ui/ux" -> Color(0xFFEC4899)
            "browsing", "web" -> Color(0xFF38BDF8)
            "productivity", "writing", "planning" -> Color(0xFF10B981)
            "media", "entertainment", "music", "video" -> Color(0xFFA855F7)
            else -> {
                val index = abs(category.lowercase(Locale.ROOT).hashCode()) % categoryPalette.size
                categoryPalette[index]
            }
        }
    }

    fun normalizeBaseUrl(raw: String): String? {
        val trimmed = raw.trim()
        if (trimmed.isEmpty()) return null
        val withScheme = if (trimmed.startsWith("http://", ignoreCase = true) ||
            trimmed.startsWith("https://", ignoreCase = true)
        ) {
            trimmed
        } else {
            "http://$trimmed"
        }
        return withScheme.trimEnd('/') + "/"
    }
}
