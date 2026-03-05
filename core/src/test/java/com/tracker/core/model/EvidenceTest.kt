package com.tracker.core.model

import com.tracker.core.types.DataSource
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Test suite for Evidence class validation and construction.
 *
 * Tests confidence field validation (0.0-1.0 range) and
 * proper construction with required and optional fields.
 */
class EvidenceTest {

    /**
     * Test: Evidence with confidence exactly 0.0 is valid.
     *
     * Verifies the minimum boundary (0.0) is accepted.
     */
    @Test
    fun `evidence with confidence 0_0 is valid`() {
        // Act
        val evidence = Evidence(
            source = DataSource.USAGE_STATS,
            timestampMillis = 1000L,
            confidence = 0.0f
        )

        // Assert
        assertNotNull(evidence)
        assertEquals(0.0f, evidence.confidence, 0.001f)
    }

    /**
     * Test: Evidence with confidence exactly 1.0 is valid.
     *
     * Verifies the maximum boundary (1.0) is accepted.
     */
    @Test
    fun `evidence with confidence 1_0 is valid`() {
        // Act
        val evidence = Evidence(
            source = DataSource.USAGE_STATS,
            timestampMillis = 1000L,
            confidence = 1.0f
        )

        // Assert
        assertNotNull(evidence)
        assertEquals(1.0f, evidence.confidence, 0.001f)
    }

    /**
     * Test: Evidence with confidence below 0.0 throws IllegalArgumentException.
     *
     * Verifies values below 0.0 are rejected.
     */
    @Test(expected = IllegalArgumentException::class)
    fun `evidence with confidence below 0_0 throws exception`() {
        Evidence(
            source = DataSource.USAGE_STATS,
            timestampMillis = 1000L,
            confidence = -0.1f
        )
    }

    /**
     * Test: Evidence with confidence above 1.0 throws IllegalArgumentException.
     *
     * Verifies values above 1.0 are rejected.
     */
    @Test(expected = IllegalArgumentException::class)
    fun `evidence with confidence above 1_0 throws exception`() {
        Evidence(
            source = DataSource.USAGE_STATS,
            timestampMillis = 1000L,
            confidence = 1.1f
        )
    }

    /**
     * Test: Evidence can be created with required fields only.
     *
     * Verifies normal construction with only required parameters,
     * and that optional fields default to null/empty.
     */
    @Test
    fun `evidence can be created with required fields only`() {
        // Act
        val evidence = Evidence(
            source = DataSource.USAGE_STATS,
            timestampMillis = 1234567890L,
            confidence = 0.75f
        )

        // Assert
        assertNotNull(evidence)
        assertEquals(DataSource.USAGE_STATS, evidence.source)
        assertEquals(1234567890L, evidence.timestampMillis)
        assertEquals(0.75f, evidence.confidence, 0.001f)
        assertNull(evidence.durationMinutes)
        assertNull(evidence.startTimeMillis)
        assertNull(evidence.endTimeMillis)
        assertTrue(evidence.metadata.isEmpty())
    }

    /**
     * Test: Evidence can be created with all fields populated.
     *
     * Verifies construction with all optional fields provided,
     * including metadata map.
     */
    @Test
    fun `evidence can be created with all fields`() {
        // Arrange
        val metadata = mapOf(
            "appPackage" to "com.duolingo",
            "language" to "Spanish"
        )

        // Act
        val evidence = Evidence(
            source = DataSource.USAGE_STATS,
            timestampMillis = 1234567890L,
            confidence = 0.85f,
            durationMinutes = 30,
            startTimeMillis = 1234567890L,
            endTimeMillis = 1234569690L,
            metadata = metadata
        )

        // Assert
        assertNotNull(evidence)
        assertEquals(DataSource.USAGE_STATS, evidence.source)
        assertEquals(1234567890L, evidence.timestampMillis)
        assertEquals(0.85f, evidence.confidence, 0.001f)
        assertEquals(30, evidence.durationMinutes)
        assertEquals(1234567890L, evidence.startTimeMillis)
        assertEquals(1234569690L, evidence.endTimeMillis)
        assertEquals(2, evidence.metadata.size)
        assertEquals("com.duolingo", evidence.metadata["appPackage"])
        assertEquals("Spanish", evidence.metadata["language"])
    }
}
