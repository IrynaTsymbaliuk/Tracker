package com.tracker.core.result

import com.tracker.core.types.ConfidenceLevel
import com.tracker.core.types.DataSource
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Tests for ReadingResult data class.
 */
class ReadingResultTest {

    @Test
    fun `creates valid ReadingResult with all fields`() {
        // Arrange & Act
        val result = ReadingResult(
            occurred = true,
            confidence = 0.82f,
            confidenceLevel = ConfidenceLevel.HIGH,
            durationMinutes = 45,
            sessionCount = 2,
            source = DataSource.USAGE_STATS,
            apps = listOf(
                AppInfo("com.amazon.kindle", "Kindle"),
                AppInfo("com.google.android.apps.books", "Play Books")
            ),
            title = null
        )

        // Assert
        assertTrue(result.occurred)
        assertEquals(0.82f, result.confidence)
        assertEquals(ConfidenceLevel.HIGH, result.confidenceLevel)
        assertEquals(45, result.durationMinutes)
        assertEquals(2, result.sessionCount)
        assertEquals(DataSource.USAGE_STATS, result.source)
        assertEquals(2, result.apps.size)
        assertEquals("com.amazon.kindle", result.apps[0].packageName)
        assertEquals("com.google.android.apps.books", result.apps[1].packageName)
        assertNull(result.title)
    }

    @Test
    fun `creates ReadingResult with minimal fields`() {
        // Arrange & Act
        val result = ReadingResult(
            occurred = false,
            confidence = 0.30f,
            confidenceLevel = ConfidenceLevel.LOW,
            durationMinutes = null,
            sessionCount = null,
            source = DataSource.USAGE_STATS,
            apps = emptyList(),
            title = null
        )

        // Assert
        assertFalse(result.occurred)
        assertEquals(0.30f, result.confidence)
        assertEquals(ConfidenceLevel.LOW, result.confidenceLevel)
        assertNull(result.durationMinutes)
        assertNull(result.sessionCount)
        assertTrue(result.apps.isEmpty())
        assertNull(result.title)
    }

    @Test
    fun `title is always null - reserved for future OAuth`() {
        // Arrange & Act
        val result = ReadingResult(
            occurred = true,
            confidence = 0.80f,
            confidenceLevel = ConfidenceLevel.HIGH,
            durationMinutes = 30,
            sessionCount = 1,
            source = DataSource.USAGE_STATS,
            apps = listOf(AppInfo("com.google.android.apps.books", "Play Books")),
            title = null // Always null in current implementation
        )

        // Assert
        assertNull("title should always be null without OAuth integration", result.title)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `confidence validation - rejects value less than 0`() {
        // Arrange & Act & Assert
        ReadingResult(
            occurred = false,
            confidence = -0.1f,
            confidenceLevel = ConfidenceLevel.LOW,
            durationMinutes = null,
            sessionCount = null,
            source = DataSource.USAGE_STATS,
            apps = emptyList(),
            title = null
        )
    }

    @Test(expected = IllegalArgumentException::class)
    fun `confidence validation - rejects value greater than 1`() {
        // Arrange & Act & Assert
        ReadingResult(
            occurred = true,
            confidence = 1.1f,
            confidenceLevel = ConfidenceLevel.HIGH,
            durationMinutes = 60,
            sessionCount = 1,
            source = DataSource.USAGE_STATS,
            apps = listOf(AppInfo("com.amazon.kindle", "Kindle")),
            title = null
        )
    }

    @Test
    fun `confidence validation - accepts 0`() {
        // Arrange & Act
        val result = ReadingResult(
            occurred = false,
            confidence = 0.0f,
            confidenceLevel = ConfidenceLevel.LOW,
            durationMinutes = null,
            sessionCount = null,
            source = DataSource.USAGE_STATS,
            apps = emptyList(),
            title = null
        )

        // Assert
        assertEquals(0.0f, result.confidence)
    }

    @Test
    fun `confidence validation - accepts 1`() {
        // Arrange & Act
        val result = ReadingResult(
            occurred = true,
            confidence = 1.0f,
            confidenceLevel = ConfidenceLevel.HIGH,
            durationMinutes = 90,
            sessionCount = 1,
            source = DataSource.USAGE_STATS,
            apps = listOf(AppInfo("com.amazon.kindle", "Kindle")),
            title = null
        )

        // Assert
        assertEquals(1.0f, result.confidence)
    }

    @Test
    fun `sessionCount represents number of distinct reading apps`() {
        // Arrange & Act
        val result = ReadingResult(
            occurred = true,
            confidence = 0.85f,
            confidenceLevel = ConfidenceLevel.HIGH,
            durationMinutes = 120,
            sessionCount = 3,
            source = DataSource.USAGE_STATS,
            apps = listOf(
                AppInfo("com.amazon.kindle", "Kindle"),
                AppInfo("com.google.android.apps.books", "Play Books"),
                AppInfo("com.faultexception.reader", "Reader")
            ),
            title = null
        )

        // Assert
        assertEquals(3, result.sessionCount)
        assertEquals(3, result.apps.size)
    }

    @Test
    fun `apps can be empty when no apps detected`() {
        // Arrange & Act
        val result = ReadingResult(
            occurred = false,
            confidence = 0.0f,
            confidenceLevel = ConfidenceLevel.LOW,
            durationMinutes = null,
            sessionCount = 0,
            source = DataSource.USAGE_STATS,
            apps = emptyList(),
            title = null
        )

        // Assert
        assertTrue(result.apps.isEmpty())
        assertEquals(0, result.sessionCount)
    }
}
