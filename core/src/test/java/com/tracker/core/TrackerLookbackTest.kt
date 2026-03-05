package com.tracker.core

import android.content.Context
import com.tracker.core.types.Metric
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

@RunWith(RobolectricTestRunner::class)
class TrackerLookbackTest {

    private lateinit var context: Context

    @Before
    fun setup() {
        context = RuntimeEnvironment.getApplication()
    }

    /**
     * Test: Default lookback period is 30 days when not specified.
     *
     * Given: A Tracker instance created without calling setLookbackDays()
     * When: We query the configured lookback period
     * Then: It should return 30 days
     */
    @Test
    fun `default lookback days is 30`() {
        // Arrange & Act
        val tracker = Tracker.Builder(context)
            .requestMetrics(Metric.LANGUAGE_LEARNING)
            .build()

        // Assert
        assertEquals(30, tracker.getLookbackDays())
    }

    /**
     * Test: Custom lookback period is respected.
     *
     * Verifies that setLookbackDays() overrides the default.
     */
    @Test
    fun `custom lookback days is respected`() {
        // Arrange
        val customDays = 45

        // Act
        val tracker = Tracker.Builder(context)
            .requestMetrics(Metric.LANGUAGE_LEARNING)
            .setLookbackDays(customDays)
            .build()

        // Assert
        assertEquals(customDays, tracker.getLookbackDays())
    }

    /**
     * Test: Negative lookback days are rejected.
     */
    @Test(expected = IllegalArgumentException::class)
    fun `setLookbackDays rejects negative values`() {
        Tracker.Builder(context)
            .requestMetrics(Metric.LANGUAGE_LEARNING)
            .setLookbackDays(-1)
            .build()
    }

    /**
     * Test: Zero lookback days are rejected.
     */
    @Test(expected = IllegalArgumentException::class)
    fun `setLookbackDays rejects zero`() {
        Tracker.Builder(context)
            .requestMetrics(Metric.LANGUAGE_LEARNING)
            .setLookbackDays(0)
            .build()
    }

    /**
     * Test: Minimum valid lookback of 1 day is accepted.
     */
    @Test
    fun `setLookbackDays accepts min value of 1`() {
        // Act
        val tracker = Tracker.Builder(context)
            .requestMetrics(Metric.LANGUAGE_LEARNING)
            .setLookbackDays(1)
            .build()

        // Assert
        assertEquals(1, tracker.getLookbackDays())
    }

    /**
     * Test: Lookback days exceeding 365 are rejected.
     */
    @Test(expected = IllegalArgumentException::class)
    fun `setLookbackDays rejects values greater than 365`() {
        Tracker.Builder(context)
            .requestMetrics(Metric.LANGUAGE_LEARNING)
            .setLookbackDays(366)
            .build()
    }

    /**
     * Test: Maximum valid lookback of 365 days is accepted.
     */
    @Test
    fun `setLookbackDays accepts max value of 365`() {
        // Act
        val tracker = Tracker.Builder(context)
            .requestMetrics(Metric.LANGUAGE_LEARNING)
            .setLookbackDays(365)
            .build()

        // Assert
        assertEquals(365, tracker.getLookbackDays())
    }

}
