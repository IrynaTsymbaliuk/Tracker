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
 */
@RunWith(RobolectricTestRunner::class)
class HabitEngineTimeRefactorTest {

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
    fun `queryMetric accepts Long timestamps fromMillis and toMillis`() = runTest {
        val engine = createEngine()

        val fromMillis = 1700000000000L - 86_400_000L
        val toMillis = 1700000000000L

        coEvery { mockLanguageLearningCollector.collect(any(), any()) } returns Result.success(emptyList())
        every { mockLanguageLearningAggregator.aggregate(any(), any(), any()) } returns null

        // Should accept millisecond timestamps
        val result = engine.queryMetric(Metric.LANGUAGE_LEARNING, fromMillis, toMillis)

        assertNotNull(result)
        coVerify { mockLanguageLearningCollector.collect(fromMillis, toMillis) }
    }

    @Test
    fun `collectors receive correct fromMillis and toMillis`() = runTest {
        val engine = createEngine()

        val fromMillis = 1646092800000L // March 1, 2022 00:00:00 UTC
        val toMillis = 1646179200000L   // March 2, 2022 00:00:00 UTC

        var llFromMillis: Long? = null
        var llToMillis: Long? = null

        coEvery { mockLanguageLearningCollector.collect(any(), any()) } answers {
            llFromMillis = firstArg()
            llToMillis = secondArg()
            Result.success(emptyList())
        }

        every { mockLanguageLearningAggregator.aggregate(any(), any(), any()) } returns null

        engine.queryMetric(Metric.LANGUAGE_LEARNING, fromMillis, toMillis)

        assertEquals(fromMillis, llFromMillis)
        assertEquals(toMillis, llToMillis)
    }

    @Test
    fun `fromMillis and toMillis passed through unchanged to collectors`() = runTest {
        val engine = createEngine()

        val fromMillis = 1700000000000L - 86_400_000L
        val toMillis = 1700000000000L

        var capturedFrom: Long? = null
        var capturedTo: Long? = null

        coEvery { mockReadingCollector.collect(any(), any()) } answers {
            capturedFrom = firstArg()
            capturedTo = secondArg()
            Result.success(emptyList())
        }

        every { mockReadingAggregator.aggregate(any(), any(), any()) } returns null

        engine.queryMetric(Metric.READING, fromMillis, toMillis)

        assertEquals(fromMillis, capturedFrom)
        assertEquals(toMillis, capturedTo)
    }

    @Test
    fun `result contains single metric result not a list of days`() = runTest {
        val engine = createEngine()

        val fromMillis = 1700000000000L - 86_400_000L
        val toMillis = 1700000000000L

        val evidence = listOf(
            Evidence(
                source = DataSource.USAGE_STATS,
                timestampMillis = 1700000000000L - 3600000L, // 1 hour before end
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

        val result = engine.queryMetric(Metric.LANGUAGE_LEARNING, fromMillis, toMillis) as LanguageLearningMetricResult

        // NEW: Result has direct field, not days list
        assertNotNull(result.result)
        assertEquals(true, result.result?.occurred)
        assertEquals(45, result.result?.durationMinutes)
    }

    @Test
    fun `aggregator receives all evidence within time range`() = runTest {
        val engine = createEngine()

        val fromMillis = 1700000000000L - 86_400_000L
        val toMillis = 1700000000000L

        val evidence = listOf(
            Evidence(
                source = DataSource.USAGE_STATS,
                timestampMillis = fromMillis + 1000L,
                confidence = 0.75f,
                durationMinutes = 30
            ),
            Evidence(
                source = DataSource.USAGE_STATS,
                timestampMillis = toMillis - 1000L,
                confidence = 0.80f,
                durationMinutes = 40
            )
        )

        var capturedEvidence: List<Evidence>? = null

        coEvery { mockReadingCollector.collect(any(), any()) } returns Result.success(evidence)
        every { mockReadingAggregator.aggregate(any(), any(), any()) } answers {
            capturedEvidence = secondArg()
            ReadingResult(
                occurred = true,
                confidence = 0.91f,
                confidenceLevel = ConfidenceLevel.HIGH,
                durationMinutes = 70,
                sessionCount = 2,
                source = DataSource.USAGE_STATS,
                apps = emptyList()
            )
        }

        engine.queryMetric(Metric.READING, fromMillis, toMillis)

        assertEquals(2, capturedEvidence?.size)
        assertEquals(evidence, capturedEvidence)
    }

    @Test
    fun `DataQuality built correctly when no permission`() = runTest {
        val engine = createEngine()

        // Simulate permission denied
        coEvery { mockLanguageLearningCollector.collect(any(), any()) } returns
            Result.failure(SecurityException("PACKAGE_USAGE_STATS permission required"))

        val result = engine.queryMetric(Metric.LANGUAGE_LEARNING, 1700000000000L - 86_400_000L, 1700000000000L) as LanguageLearningMetricResult

        assertNull(result.result) // No data when permission missing
        assertNotNull(result.dataQuality)
        // DataQuality should reflect missing source
    }

    @Test
    fun `DataQuality built correctly when permission granted`() = runTest {
        val engine = createEngine()

        val readingResult = ReadingResult(
            occurred = true,
            confidence = 0.85f,
            confidenceLevel = ConfidenceLevel.HIGH,
            durationMinutes = 60,
            sessionCount = 2,
            source = DataSource.USAGE_STATS,
            apps = emptyList()
        )

        coEvery { mockReadingCollector.collect(any(), any()) } returns Result.success(
            listOf(
                Evidence(
                    source = DataSource.USAGE_STATS,
                    timestampMillis = 1700000000000L,
                    confidence = 0.85f,
                    durationMinutes = 60
                )
            )
        )
        every { mockReadingAggregator.aggregate(any(), any(), any()) } returns readingResult

        val result = engine.queryMetric(Metric.READING, 1700000000000L - 86_400_000L, 1700000000000L) as ReadingMetricResult

        assertNotNull(result.result)
        assertNotNull(result.dataQuality)
        // DataQuality should show available sources
    }

    @Test
    fun `querying one metric does not affect another`() = runTest {
        val engine = createEngine()

        val fromMillis = 1700000000000L - 86_400_000L
        val toMillis = 1700000000000L

        coEvery { mockLanguageLearningCollector.collect(any(), any()) } returns Result.success(emptyList())
        coEvery { mockReadingCollector.collect(any(), any()) } returns Result.success(emptyList())
        every { mockLanguageLearningAggregator.aggregate(any(), any(), any()) } returns null
        every { mockReadingAggregator.aggregate(any(), any(), any()) } returns null

        // Query language learning
        engine.queryMetric(Metric.LANGUAGE_LEARNING, fromMillis, toMillis)

        // Only language learning collector should be called
        coVerify(exactly = 1) { mockLanguageLearningCollector.collect(any(), any()) }
        coVerify(exactly = 0) { mockReadingCollector.collect(any(), any()) }

        // Now query reading
        engine.queryMetric(Metric.READING, fromMillis, toMillis)

        // Now reading collector should be called too
        coVerify(exactly = 1) { mockReadingCollector.collect(any(), any()) }
    }

    @Test
    fun `null result when aggregator returns null`() = runTest {
        val engine = createEngine()

        coEvery { mockLanguageLearningCollector.collect(any(), any()) } returns Result.success(emptyList())
        every { mockLanguageLearningAggregator.aggregate(any(), any(), any()) } returns null

        val result = engine.queryMetric(Metric.LANGUAGE_LEARNING, 1700000000000L - 86_400_000L, 1700000000000L) as LanguageLearningMetricResult

        assertNull(result.result)
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
}
