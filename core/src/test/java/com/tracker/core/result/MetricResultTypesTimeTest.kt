package com.tracker.core.result

import com.tracker.core.types.ConfidenceLevel
import com.tracker.core.types.DataSource
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * Tests for result types after time refactoring.
 * Verifies that:
 * - MetricsResult has direct metric fields (languageLearning, reading)
 */
class MetricResultTypesTimeTest {

    @Test
    fun `MetricsResult has languageLearning field`() {
        val result = MetricsResult(
            languageLearning = LanguageLearningResult(
                occurred = true,
                confidence = 0.85f,
                confidenceLevel = ConfidenceLevel.HIGH,
                durationMinutes = 45,
                sessionCount = 2,
                source = DataSource.USAGE_STATS,
                apps = emptyList()
            ),
            reading = null,
            dataQuality = createDataQuality()
        )

        assertNotNull(result.languageLearning)
        assertEquals(true, result.languageLearning?.occurred)
    }

    @Test
    fun `MetricsResult has reading field`() {
        val result = MetricsResult(
            languageLearning = null,
            reading = ReadingResult(
                occurred = true,
                confidence = 0.75f,
                confidenceLevel = ConfidenceLevel.MEDIUM,
                durationMinutes = 30,
                sessionCount = 1,
                source = DataSource.USAGE_STATS,
                apps = emptyList()
            ),
            dataQuality = createDataQuality()
        )

        assertNotNull(result.reading)
        assertEquals(30, result.reading?.durationMinutes)
    }

    @Test
    fun `MetricsResult has dataQuality field`() {
        val dataQuality = createDataQuality()
        val result = MetricsResult(
            languageLearning = null,
            reading = null,
            dataQuality = dataQuality
        )

        assertNotNull(result.dataQuality)
        assertEquals(dataQuality, result.dataQuality)
    }

    @Test
    fun `MetricsResult supports null languageLearning`() {
        val result = MetricsResult(
            languageLearning = null,
            reading = ReadingResult(
                occurred = true,
                confidence = 0.75f,
                confidenceLevel = ConfidenceLevel.MEDIUM,
                durationMinutes = 30,
                sessionCount = 1,
                source = DataSource.USAGE_STATS,
                apps = emptyList()
            ),
            dataQuality = createDataQuality()
        )

        assertNull(result.languageLearning)
        assertNotNull(result.reading)
    }

    @Test
    fun `MetricsResult supports null reading`() {
        val result = MetricsResult(
            languageLearning = LanguageLearningResult(
                occurred = true,
                confidence = 0.85f,
                confidenceLevel = ConfidenceLevel.HIGH,
                durationMinutes = 45,
                sessionCount = 2,
                source = DataSource.USAGE_STATS,
                apps = emptyList()
            ),
            reading = null,
            dataQuality = createDataQuality()
        )

        assertNotNull(result.languageLearning)
        assertNull(result.reading)
    }

    @Test
    fun `MetricsResult supports both metrics`() {
        val result = MetricsResult(
            languageLearning = LanguageLearningResult(
                occurred = true,
                confidence = 0.85f,
                confidenceLevel = ConfidenceLevel.HIGH,
                durationMinutes = 45,
                sessionCount = 2,
                source = DataSource.USAGE_STATS,
                apps = emptyList()
            ),
            reading = ReadingResult(
                occurred = true,
                confidence = 0.75f,
                confidenceLevel = ConfidenceLevel.MEDIUM,
                durationMinutes = 30,
                sessionCount = 1,
                source = DataSource.USAGE_STATS,
                apps = emptyList()
            ),
            dataQuality = createDataQuality()
        )

        assertNotNull(result.languageLearning)
        assertNotNull(result.reading)
    }

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

    // Helper methods

    private fun createDataQuality() = DataQuality(
        availableSources = listOf(DataSource.USAGE_STATS),
        missingSources = emptyList(),
        overallReliability = ReliabilityLevel.HIGH,
        recommendations = emptyList()
    )
}
