package com.tracker.core

import android.content.Context
import com.tracker.core.types.Metric
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

/**
 * Test suite for verifying the default minimum confidence threshold of 0.50.
 *
 * Tests that when a Tracker instance is created without explicitly
 * calling setMinConfidence(), it defaults to 0.50 (50%).
 */
@RunWith(RobolectricTestRunner::class)
class TrackerMinConfidenceTest {

    private lateinit var context: Context

    @Before
    fun setup() {
        context = RuntimeEnvironment.getApplication()
    }

    /**
     * Test: Default min confidence is 0.50 when not specified.
     *
     * Given: A Tracker instance created without calling setMinConfidence()
     * When: We query the configured minimum confidence
     * Then: It should return 0.50
     */
    @Test
    fun `default min confidence is 0_50`() {
        // Arrange & Act
        val tracker = Tracker.Builder(context)
            .requestMetrics(Metric.LANGUAGE_LEARNING)
            .build()

        // Assert (using delta for float comparison)
        assertEquals(0.50f, tracker.getMinConfidence(), 0.001f)
    }

    /**
     * Test: Custom min confidence is respected.
     *
     * Verifies that setMinConfidence() overrides the default.
     */
    @Test
    fun `custom min confidence is respected`() {
        // Arrange
        val customConfidence = 0.75f

        // Act
        val tracker = Tracker.Builder(context)
            .requestMetrics(Metric.LANGUAGE_LEARNING)
            .setMinConfidence(customConfidence)
            .build()

        // Assert
        assertEquals(customConfidence, tracker.getMinConfidence(), 0.001f)
    }

    /**
     * Test: Minimum valid confidence of 0.0 is accepted.
     */
    @Test
    fun `setMinConfidence accepts min value of 0_0`() {
        // Act
        val tracker = Tracker.Builder(context)
            .requestMetrics(Metric.LANGUAGE_LEARNING)
            .setMinConfidence(0.0f)
            .build()

        // Assert
        assertEquals(0.0f, tracker.getMinConfidence(), 0.001f)
    }

    /**
     * Test: Maximum valid confidence of 1.0 is accepted.
     */
    @Test
    fun `setMinConfidence accepts max value of 1_0`() {
        // Act
        val tracker = Tracker.Builder(context)
            .requestMetrics(Metric.LANGUAGE_LEARNING)
            .setMinConfidence(1.0f)
            .build()

        // Assert
        assertEquals(1.0f, tracker.getMinConfidence(), 0.001f)
    }

    /**
     * Test: Negative confidence values are rejected.
     */
    @Test(expected = IllegalArgumentException::class)
    fun `setMinConfidence rejects negative values`() {
        Tracker.Builder(context)
            .requestMetrics(Metric.LANGUAGE_LEARNING)
            .setMinConfidence(-0.1f)
            .build()
    }

    /**
     * Test: Confidence values greater than 1.0 are rejected.
     */
    @Test(expected = IllegalArgumentException::class)
    fun `setMinConfidence rejects values greater than 1_0`() {
        Tracker.Builder(context)
            .requestMetrics(Metric.LANGUAGE_LEARNING)
            .setMinConfidence(1.1f)
            .build()
    }
}
