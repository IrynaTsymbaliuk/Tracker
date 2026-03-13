package com.tracker.core.engine

import android.content.Context
import com.tracker.core.permission.PermissionManager
import com.tracker.core.provider.LanguageLearningProvider
import com.tracker.core.provider.MetricProvider
import com.tracker.core.provider.ReadingProvider
import com.tracker.core.result.LanguageLearningResult
import com.tracker.core.types.ConfidenceLevel
import com.tracker.core.types.DataSource
import com.tracker.core.types.Metric
import io.mockk.clearAllMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

/**
 * Tests for HabitEngine after refactoring to per-metric queries with MetricProvider pattern.
 *
 * - HabitEngine.queryMetric(metric, fromMillis, toMillis) returns single metric result
 * - Uses MetricProvider pattern (single component per metric instead of Collector + Aggregator)
 */
@RunWith(RobolectricTestRunner::class)
class HabitEnginePerMetricTest {

    private lateinit var context: Context
    private lateinit var mockPermissionManager: PermissionManager
    private lateinit var mockLanguageLearningProvider: LanguageLearningProvider
    private lateinit var mockReadingProvider: ReadingProvider

    @Before
    fun setup() {
        context = RuntimeEnvironment.getApplication()
        mockPermissionManager = mockk(relaxed = true)
        mockLanguageLearningProvider = mockk(relaxed = true)
        mockReadingProvider = mockk(relaxed = true)
    }

    @After
    fun tearDown() {
        clearAllMocks()
    }

    @Test
    fun `queryMetric only queries requested metric provider`() = runTest {
        val engine = createEngine()

        val fromMillis = 1700000000000L - 86_400_000L
        val toMillis = 1700000000000L

        coEvery { mockLanguageLearningProvider.query(any(), any(), any()) } returns null

        // Query only language learning
        engine.queryMetric(Metric.LANGUAGE_LEARNING, fromMillis, toMillis)

        // Only language learning provider should be called
        coVerify(exactly = 1) { mockLanguageLearningProvider.query(any(), any(), any()) }
        coVerify(exactly = 0) { mockReadingProvider.query(any(), any(), any()) }
    }

    @Test
    fun `queryMetric passes correct time range to provider`() = runTest {
        val engine = createEngine()

        val fromMillis = 1646092800000L // March 1, 2022
        val toMillis = 1646179200000L   // March 2, 2022

        var capturedFrom: Long? = null
        var capturedTo: Long? = null

        coEvery { mockLanguageLearningProvider.query(any(), any(), any()) } answers {
            capturedFrom = firstArg()
            capturedTo = secondArg()
            null
        }

        engine.queryMetric(Metric.LANGUAGE_LEARNING, fromMillis, toMillis)

        assertEquals(fromMillis, capturedFrom)
        assertEquals(toMillis, capturedTo)
    }

    @Test
    fun `queryMetric passes minConfidence to provider`() = runTest {
        val minConfidence = 0.70f
        val engine = createEngineWithMinConfidence(minConfidence)

        var capturedConfidence: Float? = null

        coEvery { mockLanguageLearningProvider.query(any(), any(), any()) } answers {
            capturedConfidence = thirdArg()
            null
        }

        engine.queryMetric(Metric.LANGUAGE_LEARNING, 1700000000000L - 86_400_000L, 1700000000000L)

        assertEquals(minConfidence, capturedConfidence)
    }

    @Test
    fun `queryMetric provider called once per query`() = runTest {
        val engine = createEngine()

        coEvery {
            mockLanguageLearningProvider.query(
                any(),
                any(),
                any()
            )
        } returns LanguageLearningResult(
            occurred = true,
            confidence = 0.91f,
            confidenceLevel = ConfidenceLevel.HIGH,
            durationMinutes = 70,
            sessionCount = 2,
            source = DataSource.USAGE_STATS,
            apps = emptyList()
        )

        engine.queryMetric(Metric.LANGUAGE_LEARNING, 1700000000000L - 86_400_000L, 1700000000000L)

        // Provider should be called ONCE
        coVerify(exactly = 1) { mockLanguageLearningProvider.query(any(), any(), any()) }
    }

    // Helper methods

    private fun createEngine(): HabitEngine {
        val providers = mapOf(
            Metric.LANGUAGE_LEARNING to mockLanguageLearningProvider,
            Metric.READING to mockReadingProvider
        )

        return HabitEngine(
            minConfidence = 0.50f,
            providers = providers
        )
    }

    private fun createEngineWithMinConfidence(minConfidence: Float): HabitEngine {
        val providers = mapOf(
            Metric.LANGUAGE_LEARNING to mockLanguageLearningProvider,
            Metric.READING to mockReadingProvider
        )

        return HabitEngine(
            minConfidence = minConfidence,
            providers = providers
        )
    }
}
