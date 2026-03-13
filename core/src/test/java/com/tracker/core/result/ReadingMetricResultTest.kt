package com.tracker.core.result

import com.tracker.core.types.ConfidenceLevel
import com.tracker.core.types.DataSource
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * Tests for ReadingMetricResult structure.
 * This result type is returned by queryReading() methods.
 */
class ReadingMetricResultTest {

    @Test
    fun `ReadingMetricResult has result and dataQuality fields`() {
        val result = ReadingResult(
            occurred = true,
            confidence = 0.75f,
            confidenceLevel = ConfidenceLevel.MEDIUM,
            durationMinutes = 30,
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

        val metricResult = ReadingMetricResult(
            result = result,
            dataQuality = dataQuality
        )

        assertNotNull(metricResult.result)
        assertEquals(true, metricResult.result?.occurred)
        assertEquals(30, metricResult.result?.durationMinutes)
        assertNotNull(metricResult.dataQuality)
        assertEquals(ReliabilityLevel.HIGH, metricResult.dataQuality.overallReliability)
    }

    @Test
    fun `ReadingMetricResult can have null result`() {
        val dataQuality = DataQuality(
            availableSources = emptyList(),
            missingSources = emptyList(),
            overallReliability = ReliabilityLevel.LOW,
            recommendations = emptyList()
        )

        val metricResult = ReadingMetricResult(
            result = null,
            dataQuality = dataQuality
        )

        assertNull(metricResult.result)
        assertNotNull(metricResult.dataQuality)
    }

    @Test
    fun `ReadingMetricResult with no activity but available source`() {
        val result = ReadingResult(
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

        val metricResult = ReadingMetricResult(
            result = result,
            dataQuality = dataQuality
        )

        assertNotNull(metricResult.result)
        assertEquals(false, metricResult.result?.occurred)
        assertEquals(0, metricResult.result?.durationMinutes)
    }
}
