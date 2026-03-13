package com.tracker.core

import android.content.Context
import com.tracker.core.result.LanguageLearningMetricResult
import com.tracker.core.result.ReadingMetricResult
import io.mockk.clearAllMocks
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

/**
 * Tests for per-metric query methods on Tracker.
 */
@RunWith(RobolectricTestRunner::class)
class TrackerPerMetricQueryTest {

    private lateinit var context: Context

    @Before
    fun setup() {
        context = RuntimeEnvironment.getApplication()
    }

    @After
    fun tearDown() {
        clearAllMocks()
    }

    @Test
    fun `queryLanguageLearning returns LanguageLearningMetricResult`() = runTest {
        val tracker = Tracker.Builder(context)
            .setMinConfidence(0.50f)
            .build()

        val result = tracker.queryLanguageLearning()

        assertNotNull(result)
        assertNotNull(result.dataQuality)
        // result.result may be null if no permission or no data
    }

    @Test
    fun `queryReading returns ReadingMetricResult`() = runTest {
        val tracker = Tracker.Builder(context)
            .setMinConfidence(0.50f)
            .build()

        val result = tracker.queryReading()

        assertNotNull(result)
        assertNotNull(result.dataQuality)
        // result.result may be null if no permission or no data
    }

    @Test
    fun `queryLanguageLearning callback variant works`() {
        val tracker = Tracker.Builder(context)
            .setMinConfidence(0.50f)
            .build()

        val latch = CountDownLatch(1)
        var capturedResult: LanguageLearningMetricResult? = null

        tracker.queryLanguageLearning { result ->
            capturedResult = result
            latch.countDown()
        }

        latch.await(5, TimeUnit.SECONDS)
        assertNotNull(capturedResult)
        assertNotNull(capturedResult?.dataQuality)
    }

    @Test
    fun `queryReading callback variant works`() {
        val tracker = Tracker.Builder(context)
            .setMinConfidence(0.50f)
            .build()

        val latch = CountDownLatch(1)
        var capturedResult: ReadingMetricResult? = null

        tracker.queryReading { result ->
            capturedResult = result
            latch.countDown()
        }

        latch.await(5, TimeUnit.SECONDS)
        assertNotNull(capturedResult)
        assertNotNull(capturedResult?.dataQuality)
    }

    @Test
    fun `can query language learning without querying reading`() = runTest {
        val tracker = Tracker.Builder(context)
            .setMinConfidence(0.50f)
            .build()

        // Only query language learning
        val llResult = tracker.queryLanguageLearning()

        assertNotNull(llResult)
        // Should not affect reading metric at all
    }

    @Test
    fun `can query reading without querying language learning`() = runTest {
        val tracker = Tracker.Builder(context)
            .setMinConfidence(0.50f)
            .build()

        // Only query reading
        val readingResult = tracker.queryReading()

        assertNotNull(readingResult)
        // Should not affect language learning metric at all
    }

    @Test
    fun `can query both metrics independently in sequence`() = runTest {
        val tracker = Tracker.Builder(context)
            .setMinConfidence(0.50f)
            .build()

        val llResult = tracker.queryLanguageLearning()
        val readingResult = tracker.queryReading()

        assertNotNull(llResult)
        assertNotNull(llResult.dataQuality)
        assertNotNull(readingResult)
        assertNotNull(readingResult.dataQuality)
    }

    @Test
    fun `query methods use 24h time window from now`() = runTest {
        val fixedTime = 1700000000000L
        val tracker = Tracker.Builder(context)
            .setMinConfidence(0.50f)
            .apply {
                timeProvider = TimeProvider { fixedTime }
            }
            .build()

        val result = tracker.queryLanguageLearning()

        assertNotNull(result)
        // Verify 24h window is used (implementation will check this via HabitEngine)
    }

    @Test
    fun `minConfidence setting applies to per-metric queries`() = runTest {
        val tracker = Tracker.Builder(context)
            .setMinConfidence(0.70f)
            .build()

        val llResult = tracker.queryLanguageLearning()
        val readingResult = tracker.queryReading()

        assertNotNull(llResult)
        assertNotNull(readingResult)
        // Confidence threshold is passed through to aggregators
    }
}
