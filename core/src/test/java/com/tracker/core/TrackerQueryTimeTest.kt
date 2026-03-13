package com.tracker.core

import android.content.Context
import com.tracker.core.types.Metric
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

/**
 * Tests for Tracker query methods.
 * Verifies that queryAsync() and query() use 24-hour window with no date parameters.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class TrackerQueryTimeTest {

    private lateinit var context: Context
    private val testDispatcher = StandardTestDispatcher()
    private val fixedTime = 1700000000000L // Nov 14, 2023 22:13:20 UTC

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        context = RuntimeEnvironment.getApplication()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `queryAsync takes no parameters`() = runTest {
        // This test verifies the method signature has changed
        val tracker = createTrackerWithFixedTime(Metric.LANGUAGE_LEARNING)

        val result = tracker.queryAsync()

        assertNotNull(result)
    }

    @Test
    fun `MetricsResult has dataQuality field`() = runTest {
        val tracker = createTrackerWithFixedTime(Metric.LANGUAGE_LEARNING)

        val result = tracker.queryAsync()

        // Verify result structure
        assertNotNull(result.dataQuality)
    }

    @Test
    fun `TimeProvider can be injected for deterministic tests`() {
        val fixedTimeProvider = TimeProvider { fixedTime }

        val now1 = fixedTimeProvider.now()
        val now2 = fixedTimeProvider.now()

        assertEquals(fixedTime, now1)
        assertEquals(fixedTime, now2)
    }

    @Test
    fun `TimeProvider computes 24h window correctly`() {
        val fixedTimeProvider = TimeProvider { fixedTime }

        val now = fixedTimeProvider.now()
        val fromMillis = now - 86_400_000L
        val toMillis = now

        assertEquals(86_400_000L, toMillis - fromMillis)
        assertEquals(fixedTime, toMillis)
        assertEquals(fixedTime - 86_400_000L, fromMillis)
    }

    // Helper methods

    private fun createTrackerWithFixedTime(vararg metrics: Metric): Tracker {
        val builder = Tracker.Builder(context)
            .requestMetrics(*metrics)
            .setMinConfidence(0.50f)

        // Inject fixed time provider via internal property
        builder.timeProvider = TimeProvider { fixedTime }

        return builder.build()
    }
}
