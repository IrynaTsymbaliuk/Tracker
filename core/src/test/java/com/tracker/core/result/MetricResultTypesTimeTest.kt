package com.tracker.core.result

import com.tracker.core.types.ConfidenceLevel
import com.tracker.core.types.DataSource
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * Tests for individual result types.
 * These are the base result types used within metric-specific results.
 */
class MetricResultTypesTimeTest {

    @Test
    fun `LanguageLearningResult has all required fields`() {
        val result = LanguageLearningResult(
            occurred = true,
            confidence = 0.85f,
            confidenceLevel = ConfidenceLevel.HIGH,
            durationMinutes = 45,
            sessionCount = 2,
            source = DataSource.USAGE_STATS,
            language = "Spanish",
            apps = listOf(
                AppInfo(packageName = "com.duolingo", appName = "Duolingo")
            )
        )

        assertEquals(true, result.occurred)
        assertEquals(0.85f, result.confidence)
        assertEquals(ConfidenceLevel.HIGH, result.confidenceLevel)
        assertEquals(45, result.durationMinutes)
        assertEquals(2, result.sessionCount)
        assertEquals(DataSource.USAGE_STATS, result.source)
        assertEquals("Spanish", result.language)
        assertEquals(1, result.apps.size)
    }

    @Test
    fun `ReadingResult has all required fields`() {
        val result = ReadingResult(
            occurred = true,
            confidence = 0.75f,
            confidenceLevel = ConfidenceLevel.MEDIUM,
            durationMinutes = 30,
            sessionCount = 1,
            source = DataSource.USAGE_STATS,
            title = null,
            apps = listOf(
                AppInfo(packageName = "com.kindle", appName = "Kindle")
            )
        )

        assertEquals(true, result.occurred)
        assertEquals(0.75f, result.confidence)
        assertEquals(ConfidenceLevel.MEDIUM, result.confidenceLevel)
        assertEquals(30, result.durationMinutes)
        assertEquals(1, result.sessionCount)
        assertEquals(DataSource.USAGE_STATS, result.source)
        assertNull(result.title)
        assertEquals(1, result.apps.size)
    }

    @Test
    fun `LanguageLearningResult supports occurred false`() {
        val result = LanguageLearningResult(
            occurred = false,
            confidence = 0.0f,
            confidenceLevel = ConfidenceLevel.LOW,
            durationMinutes = 0,
            sessionCount = 0,
            source = DataSource.USAGE_STATS,
            apps = emptyList()
        )

        assertEquals(false, result.occurred)
        assertEquals(0, result.durationMinutes)
    }

    @Test
    fun `ReadingResult supports occurred false`() {
        val result = ReadingResult(
            occurred = false,
            confidence = 0.0f,
            confidenceLevel = ConfidenceLevel.LOW,
            durationMinutes = 0,
            sessionCount = 0,
            source = DataSource.USAGE_STATS,
            apps = emptyList()
        )

        assertEquals(false, result.occurred)
        assertEquals(0, result.durationMinutes)
    }
}
