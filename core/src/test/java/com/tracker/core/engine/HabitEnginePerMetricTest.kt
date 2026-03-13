package com.tracker.core.engine

import android.content.Context
import com.tracker.core.aggregator.Aggregator
import com.tracker.core.collector.Collector
import com.tracker.core.model.Evidence
import com.tracker.core.permission.PermissionManager
import com.tracker.core.result.LanguageLearningMetricResult
import com.tracker.core.result.LanguageLearningResult
import com.tracker.core.result.ReadingMetricResult
import com.tracker.core.result.ReadingResult
import com.tracker.core.types.ConfidenceLevel
import com.tracker.core.types.DataSource
import com.tracker.core.types.Metric
import io.mockk.clearAllMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

/**
 * Tests for HabitEngine after refactoring to per-metric queries.
 *
 * - HabitEngine.queryMetric(metric, fromMillis, toMillis) returns single metric result
 */
@RunWith(RobolectricTestRunner::class)
class HabitEnginePerMetricTest {

    private lateinit var context: Context
    private lateinit var mockPermissionManager: PermissionManager
    private lateinit var mockLanguageLearningCollector: Collector
    private lateinit var mockReadingCollector: Collector
    private lateinit var mockLanguageLearningAggregator: Aggregator<LanguageLearningResult>
    private lateinit var mockReadingAggregator: Aggregator<ReadingResult>

    @Before
    fun setup() {
        context = RuntimeEnvironment.getApplication()
        mockPermissionManager = mockk(relaxed = true)
        mockLanguageLearningCollector = mockk(relaxed = true)
        mockReadingCollector = mockk(relaxed = true)
        mockLanguageLearningAggregator = mockk(relaxed = true)
        mockReadingAggregator = mockk(relaxed = true)
    }

    @After
    fun tearDown() {
        clearAllMocks()
    }

    @Test
    fun `queryMetric accepts Metric parameter`() = runTest {
        val engine = createEngine()

        val fromMillis = 1700000000000L - 86_400_000L
        val toMillis = 1700000000000L

        coEvery { mockLanguageLearningCollector.collect(any(), any()) } returns Result.success(emptyList())
        every { mockLanguageLearningAggregator.aggregate(any(), any(), any()) } returns null

        // Should accept metric parameter
        val result = engine.queryMetric(Metric.LANGUAGE_LEARNING, fromMillis, toMillis)

        assertNotNull(result)
    }

    @Test
    fun `queryMetric only queries requested metric collector`() = runTest {
        val engine = createEngine()

        val fromMillis = 1700000000000L - 86_400_000L
        val toMillis = 1700000000000L

        coEvery { mockLanguageLearningCollector.collect(any(), any()) } returns Result.success(emptyList())
        coEvery { mockReadingCollector.collect(any(), any()) } returns Result.success(emptyList())
        every { mockLanguageLearningAggregator.aggregate(any(), any(), any()) } returns null

        // Query only language learning
        engine.queryMetric(Metric.LANGUAGE_LEARNING, fromMillis, toMillis)

        // Only language learning collector should be called
        coVerify(exactly = 1) { mockLanguageLearningCollector.collect(any(), any()) }
        coVerify(exactly = 0) { mockReadingCollector.collect(any(), any()) }
    }

    @Test
    fun `queryMetric returns LanguageLearningMetricResult for LANGUAGE_LEARNING`() = runTest {
        val engine = createEngine()

        val evidence = listOf(
            Evidence(
                source = DataSource.USAGE_STATS,
                timestampMillis = 1700000000000L,
                confidence = 0.85f,
                durationMinutes = 45
            )
        )

        val expectedResult = LanguageLearningResult(
            occurred = true,
            confidence = 0.85f,
            confidenceLevel = ConfidenceLevel.HIGH,
            durationMinutes = 45,
            sessionCount = 1,
            source = DataSource.USAGE_STATS,
            apps = emptyList()
        )

        coEvery { mockLanguageLearningCollector.collect(any(), any()) } returns Result.success(evidence)
        every { mockLanguageLearningAggregator.aggregate(any(), any(), any()) } returns expectedResult

        val result = engine.queryMetric(Metric.LANGUAGE_LEARNING, 1700000000000L - 86_400_000L, 1700000000000L) as LanguageLearningMetricResult

        assertNotNull(result.result)
        assertEquals(true, result.result?.occurred)
        assertEquals(45, result.result?.durationMinutes)
        assertNotNull(result.dataQuality)
    }

    @Test
    fun `queryMetric returns ReadingMetricResult for READING`() = runTest {
        val engine = createEngine()

        val evidence = listOf(
            Evidence(
                source = DataSource.USAGE_STATS,
                timestampMillis = 1700000000000L,
                confidence = 0.75f,
                durationMinutes = 30
            )
        )

        val expectedResult = ReadingResult(
            occurred = true,
            confidence = 0.75f,
            confidenceLevel = ConfidenceLevel.MEDIUM,
            durationMinutes = 30,
            sessionCount = 1,
            source = DataSource.USAGE_STATS,
            apps = emptyList()
        )

        coEvery { mockReadingCollector.collect(any(), any()) } returns Result.success(evidence)
        every { mockReadingAggregator.aggregate(any(), any(), any()) } returns expectedResult

        val result = engine.queryMetric(Metric.READING, 1700000000000L - 86_400_000L, 1700000000000L) as ReadingMetricResult

        assertNotNull(result.result)
        assertEquals(true, result.result?.occurred)
        assertEquals(30, result.result?.durationMinutes)
        assertNotNull(result.dataQuality)
    }

    @Test
    fun `queryMetric returns null result when aggregator returns null`() = runTest {
        val engine = createEngine()

        coEvery { mockLanguageLearningCollector.collect(any(), any()) } returns Result.success(emptyList())
        every { mockLanguageLearningAggregator.aggregate(any(), any(), any()) } returns null

        val result = engine.queryMetric(Metric.LANGUAGE_LEARNING, 1700000000000L - 86_400_000L, 1700000000000L) as LanguageLearningMetricResult

        assertNull(result.result)
        assertNotNull(result.dataQuality)
    }

    @Test
    fun `queryMetric passes correct time range to collector`() = runTest {
        val engine = createEngine()

        val fromMillis = 1646092800000L // March 1, 2022
        val toMillis = 1646179200000L   // March 2, 2022

        var capturedFrom: Long? = null
        var capturedTo: Long? = null

        coEvery { mockLanguageLearningCollector.collect(any(), any()) } answers {
            capturedFrom = firstArg()
            capturedTo = secondArg()
            Result.success(emptyList())
        }
        every { mockLanguageLearningAggregator.aggregate(any(), any(), any()) } returns null

        engine.queryMetric(Metric.LANGUAGE_LEARNING, fromMillis, toMillis)

        assertEquals(fromMillis, capturedFrom)
        assertEquals(toMillis, capturedTo)
    }

    @Test
    fun `queryMetric passes minConfidence to aggregator`() = runTest {
        val minConfidence = 0.70f
        val engine = createEngineWithMinConfidence(minConfidence)

        var capturedConfidence: Float? = null

        coEvery { mockLanguageLearningCollector.collect(any(), any()) } returns Result.success(emptyList())
        every { mockLanguageLearningAggregator.aggregate(any(), any(), any()) } answers {
            capturedConfidence = thirdArg()
            null
        }

        engine.queryMetric(Metric.LANGUAGE_LEARNING, 1700000000000L - 86_400_000L, 1700000000000L)

        assertEquals(minConfidence, capturedConfidence)
    }

    @Test
    fun `queryMetric handles collector failure gracefully`() = runTest {
        val engine = createEngine()

        // Simulate permission denied
        coEvery { mockLanguageLearningCollector.collect(any(), any()) } returns
            Result.failure(SecurityException("PACKAGE_USAGE_STATS permission required"))

        val result = engine.queryMetric(Metric.LANGUAGE_LEARNING, 1700000000000L - 86_400_000L, 1700000000000L) as LanguageLearningMetricResult

        assertNull(result.result) // No data when permission missing
        assertNotNull(result.dataQuality)
    }

    @Test
    fun `queryMetric aggregator called once with all evidence`() = runTest {
        val engine = createEngine()

        val evidence = listOf(
            Evidence(
                source = DataSource.USAGE_STATS,
                timestampMillis = 1700000000000L - 86_400_000L + 3600000L,
                confidence = 0.75f,
                durationMinutes = 30
            ),
            Evidence(
                source = DataSource.USAGE_STATS,
                timestampMillis = 1700000000000L - 3600000L,
                confidence = 0.80f,
                durationMinutes = 40
            )
        )

        coEvery { mockLanguageLearningCollector.collect(any(), any()) } returns Result.success(evidence)
        every { mockLanguageLearningAggregator.aggregate(any(), any(), any()) } returns LanguageLearningResult(
            occurred = true,
            confidence = 0.91f,
            confidenceLevel = ConfidenceLevel.HIGH,
            durationMinutes = 70,
            sessionCount = 2,
            source = DataSource.USAGE_STATS,
            apps = emptyList()
        )

        engine.queryMetric(Metric.LANGUAGE_LEARNING, 1700000000000L - 86_400_000L, 1700000000000L)

        // Aggregator should be called ONCE with all evidence
        verify(exactly = 1) { mockLanguageLearningAggregator.aggregate(any(), any(), any()) }
    }

    // Helper methods

    private fun createEngine(): HabitEngine {
        val collectors = mapOf(
            Metric.LANGUAGE_LEARNING to mockLanguageLearningCollector,
            Metric.READING to mockReadingCollector
        )

        val aggregators = mapOf<Metric, Aggregator<*>>(
            Metric.LANGUAGE_LEARNING to mockLanguageLearningAggregator,
            Metric.READING to mockReadingAggregator
        )

        return HabitEngine(
            minConfidence = 0.50f,
            permissionManager = mockPermissionManager,
            collectors = collectors,
            aggregators = aggregators
        )
    }

    private fun createEngineWithMinConfidence(minConfidence: Float): HabitEngine {
        val collectors = mapOf(
            Metric.LANGUAGE_LEARNING to mockLanguageLearningCollector,
            Metric.READING to mockReadingCollector
        )

        val aggregators = mapOf<Metric, Aggregator<*>>(
            Metric.LANGUAGE_LEARNING to mockLanguageLearningAggregator,
            Metric.READING to mockReadingAggregator
        )

        return HabitEngine(
            minConfidence = minConfidence,
            permissionManager = mockPermissionManager,
            collectors = collectors,
            aggregators = aggregators
        )
    }
}
