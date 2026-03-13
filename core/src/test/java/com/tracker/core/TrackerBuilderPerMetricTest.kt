package com.tracker.core

import android.content.Context
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

/**
 * Tests for Tracker.Builder after removing requestMetrics().
 */
@RunWith(RobolectricTestRunner::class)
class TrackerBuilderPerMetricTest {

    private lateinit var context: Context

    @Before
    fun setup() {
        context = RuntimeEnvironment.getApplication()
    }

    @Test
    fun `builder works without requestMetrics`() {
        val tracker = Tracker.Builder(context)
            .setMinConfidence(0.50f)
            .build()

        assertNotNull(tracker)
    }

    @Test
    fun `builder defaults minConfidence to 0_50`() {
        val tracker = Tracker.Builder(context)
            .build()

        assertEquals(0.50f, tracker.getMinConfidence(), 0.001f)
    }

    @Test
    fun `builder accepts custom minConfidence`() {
        val tracker = Tracker.Builder(context)
            .setMinConfidence(0.75f)
            .build()

        assertEquals(0.75f, tracker.getMinConfidence(), 0.001f)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `builder rejects minConfidence below 0`() {
        Tracker.Builder(context)
            .setMinConfidence(-0.1f)
            .build()
    }

    @Test(expected = IllegalArgumentException::class)
    fun `builder rejects minConfidence above 1`() {
        Tracker.Builder(context)
            .setMinConfidence(1.1f)
            .build()
    }

    @Test
    fun `builder can be built with minimal config`() {
        // Should work with just context
        val tracker = Tracker.Builder(context).build()

        assertNotNull(tracker)
    }

    @Test
    fun `builder method chaining works`() {
        val tracker = Tracker.Builder(context)
            .setMinConfidence(0.60f)
            .build()

        assertEquals(0.60f, tracker.getMinConfidence(), 0.001f)
    }
}
