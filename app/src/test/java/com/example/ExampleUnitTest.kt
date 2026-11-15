package com.example

import com.example.model.SummaryTodayResponse
import com.example.model.WidgetTodayResponse
import com.example.network.DynamicApiClientFactory
import com.example.util.Formatters
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleUnitTest {

    @Test
    fun formatters_formatDurationsAndAppNames() {
        assertEquals("4h 02m", Formatters.formatDurationCompact(14520.5))
        assertEquals("5m 42s", Formatters.formatDuration(342.1))
        assertEquals("Zed", Formatters.formatFriendlyAppName("dev.zed.Zed"))
        assertEquals("Firefox", Formatters.formatFriendlyAppName("org.mozilla.firefox"))
        assertEquals("http://10.0.0.2:7879/", Formatters.normalizeBaseUrl("10.0.0.2:7879"))
        assertEquals("main.rs", Formatters.extractFileName("/home/user/project/src/main.rs"))
    }

    @Test
    fun moshi_parsesWidgetAndSummaryResponses() {
        val widgetJson = """
            {
              "date": "2026-11-15",
              "has_data": true,
              "is_stale": false,
              "updated_at": 1794762087.42,
              "total_active_sec": 14520.5,
              "state": "active",
              "current_app": "dev.zed.Zed",
              "current_category": "Development",
              "current_duration_sec": 342.1,
              "top_categories": [
                {"category": "Development", "total_sec": 9800.0, "share_pct": 67.5}
              ],
              "top_apps": [
                {"app_name": "dev.zed.Zed", "total_sec": 7200.0}
              ]
            }
        """.trimIndent()

        val widget = DynamicApiClientFactory.moshi
            .adapter(WidgetTodayResponse::class.java)
            .fromJson(widgetJson)

        assertNotNull(widget)
        assertTrue(widget!!.hasData)
        assertEquals("active", widget.state)
        assertEquals(1, widget.topCategories.size)
        assertEquals("Development", widget.topCategories.first().category)

        val summaryJson = """
            {
              "has_data": false,
              "is_stale": true,
              "date": "2026-11-15",
              "source_host": null,
              "updated_at": 0.0,
              "total_active_sec": 0.0,
              "status": {"tracking": false, "paused": false, "screen_locked": false},
              "categories": [],
              "apps": [],
              "top_sites": [],
              "recent_files": []
            }
        """.trimIndent()

        val summary = DynamicApiClientFactory.moshi
            .adapter(SummaryTodayResponse::class.java)
            .fromJson(summaryJson)

        assertNotNull(summary)
        assertEquals(false, summary!!.hasData)
        assertTrue(summary.categories.isEmpty())
    }
}
