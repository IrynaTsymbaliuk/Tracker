package com.tracker.core.result

import com.tracker.core.types.ConfidenceLevel
import com.tracker.core.types.DataSource
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * Tests for LanguageLearningMetricResult structure.
 * This result type is returned by queryLanguageLearning() methods.
 */
class LanguageLearningMetricResultTest {

    @Test
    fun `LanguageLearningMetricResult has result and dataQuality fields`() {
        val result = LanguageLearningResult(
            occurred = true,
            confidence = 0.85f,
            confidenceLevel = ConfidenceLevel.HIGH,
            durationMinutes = 45,
            sessionCount = 1,
            source = DataSource.USAGE_STATS,
            apps = emptyList()
        )

        val dataQuality = DataQuality(
            availableSources = listOf(DataSource.USAGE_STATS),
            missingSources = emptyList(),
            overallReliability = ReliabilityLevel.HIGH,
            recommendations = emptyList()
        )

        val metricResult = LanguageLearningMetricResult(
            result = result,
            dataQuality = dataQuality
        )

        assertNotNull(metricResult.result)
        assertEquals(true, metricResult.result?.occurred)
        assertEquals(45, metricResult.result?.durationMinutes)
        assertNotNull(metricResult.dataQuality)
        assertEquals(ReliabilityLevel.HIGH, metricResult.dataQuality.overallReliability)
    }

    @Test
    fun `LanguageLearningMetricResult can have null result`() {
        val dataQuality = DataQuality(
            availableSources = emptyList(),
            missingSources = emptyList(),
            overallReliability = ReliabilityLevel.LOW,
            recommendations = emptyList()
        )

        val metricResult = LanguageLearningMetricResult(
            result = null,
            dataQuality = dataQuality
        )

        assertNull(metricResult.result)
        assertNotNull(metricResult.dataQuality)
    }

    @Test
    fun `LanguageLearningMetricResult with no data but available source`() {
        val result = LanguageLearningResult(
            occurred = false,
            confidence = 0.0f,
            confidenceLevel = ConfidenceLevel.LOW,
            durationMinutes = 0,
            sessionCount = 0,
            source = DataSource.USAGE_STATS,
            apps = emptyList()
        )

        val dataQuality = DataQuality(
            availableSources = listOf(DataSource.USAGE_STATS),
            missingSources = emptyList(),
            overallReliability = ReliabilityLevel.HIGH,
            recommendations = emptyList()
        )

        val metricResult = LanguageLearningMetricResult(
            result = result,
            dataQuality = dataQuality
        )

        assertNotNull(metricResult.result)
        assertEquals(false, metricResult.result?.occurred)
        assertEquals(0, metricResult.result?.durationMinutes)
    }
}
