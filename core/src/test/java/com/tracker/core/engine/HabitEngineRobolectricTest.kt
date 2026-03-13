package com.tracker.core.engine

import android.content.Context
import com.tracker.core.aggregator.Aggregator
import com.tracker.core.collector.Collector
import com.tracker.core.model.Evidence
import com.tracker.core.permission.PermissionManager
import com.tracker.core.result.HabitResult
import com.tracker.core.result.LanguageLearningResult
import com.tracker.core.result.ReadingResult
import com.tracker.core.types.ConfidenceLevel
import com.tracker.core.types.DataSource
import com.tracker.core.types.Metric
import io.mockk.clearAllMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
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
 * Tests for HabitEngine.
 */
@RunWith(RobolectricTestRunner::class)
class HabitEngineRobolectricTest {

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
    fun `query accepts Long timestamps fromMillis and toMillis`() = runTest {
        val engine = createEngine(Metric.LANGUAGE_LEARNING)

        val fromMillis = 1700000000000L - 86_400_000L
        val toMillis = 1700000000000L

        coEvery { mockLanguageLearningCollector.collect(any(), any()) } returns Result.success(emptyList())
        every { mockLanguageLearningAggregator.aggregate(any(), any(), any()) } returns null

        // Should accept millisecond timestamps
        val result = engine.query(fromMillis, toMillis)

        assertNotNull(result)
        coVerify { mockLanguageLearningCollector.collect(fromMillis, toMillis) }
    }

    @Test
    fun `collectors receive correct fromMillis and toMillis`() = runTest {
        val engine = createEngine(Metric.LANGUAGE_LEARNING, Metric.READING)

        val fromMillis = 1646092800000L // March 1, 2022 00:00:00 UTC
        val toMillis = 1646179200000L   // March 2, 2022 00:00:00 UTC

        var llFromMillis: Long? = null
        var llToMillis: Long? = null
        var readingFromMillis: Long? = null
        var readingToMillis: Long? = null

        coEvery { mockLanguageLearningCollector.collect(any(), any()) } answers {
            llFromMillis = firstArg()
            llToMillis = secondArg()
            Result.success(emptyList())
        }

        coEvery { mockReadingCollector.collect(any(), any()) } answers {
            readingFromMillis = firstArg()
            readingToMillis = secondArg()
            Result.success(emptyList())
        }

        every { mockLanguageLearningAggregator.aggregate(any(), any(), any()) } returns null
        every { mockReadingAggregator.aggregate(any(), any(), any()) } returns null

        engine.query(fromMillis, toMillis)

        assertEquals(fromMillis, llFromMillis)
        assertEquals(toMillis, llToMillis)
        assertEquals(fromMillis, readingFromMillis)
        assertEquals(toMillis, readingToMillis)
    }

    @Test
    fun `fromMillis and toMillis passed through unchanged to collectors`() = runTest {
        val engine = createEngine(Metric.READING)

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

        engine.query(fromMillis, toMillis)

        assertEquals(fromMillis, capturedFrom)
        assertEquals(toMillis, capturedTo)
    }

    @Test
    fun `result contains single result not a list of days`() = runTest {
        val engine = createEngine(Metric.LANGUAGE_LEARNING)

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

        val result = engine.query(fromMillis, toMillis)

        assertNotNull(result.languageLearning)
        assertEquals(true, result.languageLearning?.occurred)
        assertEquals(45, result.languageLearning?.durationMinutes)
    }

    @Test
    fun `aggregator receives all evidence within time range`() = runTest {
        val engine = createEngine(Metric.READING)

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
        every { mockReadingAggregator.aggregate(any(), capture(slot<List<Evidence>>()), any()) } answers {
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

        engine.query(fromMillis, toMillis)

        assertEquals(2, capturedEvidence?.size)
        assertEquals(evidence, capturedEvidence)
    }

    @Test
    fun `aggregator called once with all evidence not once per day`() = runTest {
        val engine = createEngine(Metric.LANGUAGE_LEARNING)

        val evidence = listOf(
            Evidence(
                source = DataSource.USAGE_STATS,
                timestampMillis = 1700000000000L - 86_400_000L + 3600000L, // Early in window
                confidence = 0.75f,
                durationMinutes = 30
            ),
            Evidence(
                source = DataSource.USAGE_STATS,
                timestampMillis = 1700000000000L - 3600000L, // Late in window
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

        engine.query(1700000000000L - 86_400_000L, 1700000000000L)

        verify(exactly = 1) { mockLanguageLearningAggregator.aggregate(any(), any(), any()) }
    }

    @Test
    fun `DataQuality built correctly when no permission`() = runTest {
        val engine = createEngine(Metric.LANGUAGE_LEARNING)

        // Simulate permission denied
        coEvery { mockLanguageLearningCollector.collect(any(), any()) } returns
            Result.failure(SecurityException("PACKAGE_USAGE_STATS permission required"))

        val result = engine.query(1700000000000L - 86_400_000L, 1700000000000L)

        assertNull(result.languageLearning) // No data when permission missing
        assertNotNull(result.dataQuality)
        // DataQuality should reflect missing source
    }

    @Test
    fun `DataQuality built correctly when permission granted`() = runTest {
        val engine = createEngine(Metric.READING)

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

        val result = engine.query(1700000000000L - 86_400_000L, 1700000000000L)

        assertNotNull(result.reading)
        assertNotNull(result.dataQuality)
        // DataQuality should show available sources
    }

    @Test
    fun `multiple metrics aggregate independently`() = runTest {
        val engine = createEngine(Metric.LANGUAGE_LEARNING, Metric.READING)

        val llEvidence = listOf(
            Evidence(
                source = DataSource.USAGE_STATS,
                timestampMillis = 1700000000000L,
                confidence = 0.85f,
                durationMinutes = 45
            )
        )

        val readingEvidence = listOf(
            Evidence(
                source = DataSource.USAGE_STATS,
                timestampMillis = 1700000000000L,
                confidence = 0.75f,
                durationMinutes = 30
            )
        )

        val llResult = LanguageLearningResult(
            occurred = true,
            confidence = 0.85f,
            confidenceLevel = ConfidenceLevel.HIGH,
            durationMinutes = 45,
            sessionCount = 1,
            source = DataSource.USAGE_STATS,
            apps = emptyList()
        )

        val readingResult = ReadingResult(
            occurred = true,
            confidence = 0.75f,
            confidenceLevel = ConfidenceLevel.HIGH,
            durationMinutes = 30,
            sessionCount = 1,
            source = DataSource.USAGE_STATS,
            apps = emptyList()
        )

        coEvery { mockLanguageLearningCollector.collect(any(), any()) } returns Result.success(llEvidence)
        coEvery { mockReadingCollector.collect(any(), any()) } returns Result.success(readingEvidence)
        every { mockLanguageLearningAggregator.aggregate(any(), any(), any()) } returns llResult
        every { mockReadingAggregator.aggregate(any(), any(), any()) } returns readingResult

        val result = engine.query(1700000000000L - 86_400_000L, 1700000000000L)

        assertNotNull(result.languageLearning)
        assertNotNull(result.reading)
        assertEquals(45, result.languageLearning?.durationMinutes)
        assertEquals(30, result.reading?.durationMinutes)
    }

    @Test
    fun `null result when aggregator returns null`() = runTest {
        val engine = createEngine(Metric.LANGUAGE_LEARNING)

        coEvery { mockLanguageLearningCollector.collect(any(), any()) } returns Result.success(emptyList())
        every { mockLanguageLearningAggregator.aggregate(any(), any(), any()) } returns null

        val result = engine.query(1700000000000L - 86_400_000L, 1700000000000L)

        assertNull(result.languageLearning)
    }

    @Test
    fun `unrequested metric returns null`() = runTest {
        // Only request LANGUAGE_LEARNING
        val engine = createEngine(Metric.LANGUAGE_LEARNING)

        val llResult = LanguageLearningResult(
            occurred = true,
            confidence = 0.85f,
            confidenceLevel = ConfidenceLevel.HIGH,
            durationMinutes = 45,
            sessionCount = 1,
            source = DataSource.USAGE_STATS,
            apps = emptyList()
        )

        coEvery { mockLanguageLearningCollector.collect(any(), any()) } returns Result.success(
            listOf(
                Evidence(
                    source = DataSource.USAGE_STATS,
                    timestampMillis = 1700000000000L,
                    confidence = 0.85f,
                    durationMinutes = 45
                )
            )
        )
        every { mockLanguageLearningAggregator.aggregate(any(), any(), any()) } returns llResult

        val result = engine.query(1700000000000L - 86_400_000L, 1700000000000L)

        assertNotNull(result.languageLearning)
        assertNull(result.reading) // Not requested
    }

    // Helper methods

    private fun createEngine(vararg metrics: Metric): HabitEngine {
        val collectors = mutableMapOf<Metric, Collector>()
        val aggregators = mutableMapOf<Metric, Aggregator<out HabitResult>>()

        if (Metric.LANGUAGE_LEARNING in metrics) {
            collectors[Metric.LANGUAGE_LEARNING] = mockLanguageLearningCollector
            aggregators[Metric.LANGUAGE_LEARNING] = mockLanguageLearningAggregator
        }

        if (Metric.READING in metrics) {
            collectors[Metric.READING] = mockReadingCollector
            aggregators[Metric.READING] = mockReadingAggregator
        }

        return HabitEngine(
            requestedMetrics = metrics.toSet(),
            minConfidence = 0.50f,
            permissionManager = mockPermissionManager,
            collectors = collectors,
            aggregators = aggregators
        )
    }
}
