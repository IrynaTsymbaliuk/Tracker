package com.tracker.core.engine

import com.tracker.core.aggregator.Aggregator
import com.tracker.core.collector.Collector
import com.tracker.core.model.Evidence
import com.tracker.core.permission.PermissionManager
import com.tracker.core.permission.PermissionStatus
import com.tracker.core.result.AccessStatus
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
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test
import java.util.Calendar

/**
 * Test suite for HabitEngine.
 *
 * Tests coordination of collectors, aggregators, and result building.
 */
class HabitEngineTest {

    @After
    fun tearDown() {
        clearAllMocks()
    }

    // ============================================================
    // Query Execution Tests
    // ============================================================

    @Test
    fun `query with LANGUAGE_LEARNING metric calls language collector`() = runTest {
        // Arrange
        val mockCollector = mockk<Collector>()
        coEvery { mockCollector.collect(any(), any()) } returns Result.success(emptyList())

        val engine = createEngine(
            requestedMetrics = setOf(Metric.LANGUAGE_LEARNING),
            collectors = mapOf(Metric.LANGUAGE_LEARNING to mockCollector)
        )

        // Act
        engine.query(fromMillis = 1000L, toMillis = 2000L)

        // Assert
        coVerify { mockCollector.collect(1000L, 2000L) }
    }

    @Test
    fun `query without LANGUAGE_LEARNING metric doesn't call language collector`() = runTest {
        // Arrange
        val mockCollector = mockk<Collector>()

        val engine = createEngine(
            requestedMetrics = emptySet(),
            collectors = mapOf(Metric.LANGUAGE_LEARNING to mockCollector)
        )

        // Act
        engine.query(fromMillis = 1000L, toMillis = 2000L)

        // Assert
        coVerify(exactly = 0) { mockCollector.collect(any(), any()) }
    }

    @Test
    fun `query aggregates all evidence in single call not per day`() = runTest {
        // Arrange
        val now = 1700000000000L
        val fromMillis = now - 86_400_000L
        val toMillis = now

        // Evidence from different times within 24h window
        val evidence = listOf(
            createEvidence(timestampMillis = fromMillis + 1000L),
            createEvidence(timestampMillis = fromMillis + 3600000L), // 1 hour later
            createEvidence(timestampMillis = toMillis - 1000L) // Near end
        )

        val mockCollector = mockk<Collector>()
        coEvery { mockCollector.collect(any(), any()) } returns Result.success(evidence)

        val mockAggregator = mockk<Aggregator<LanguageLearningResult>>()
        every {
            mockAggregator.aggregate(
                any(),
                any(),
                any()
            )
        } returns createLanguageLearningResult()

        val engine = createEngine(
            requestedMetrics = setOf(Metric.LANGUAGE_LEARNING),
            collectors = mapOf(Metric.LANGUAGE_LEARNING to mockCollector),
            aggregators = mapOf(Metric.LANGUAGE_LEARNING to mockAggregator)
        )

        // Act
        engine.query(fromMillis = fromMillis, toMillis = toMillis)

        // Assert - Aggregator called ONCE with all 3 evidence items
        verify(exactly = 1) { mockAggregator.aggregate(any(), match { it.size == 3 }, any()) }
    }

    @Test
    fun `query returns direct result fields not days list`() = runTest {
        // Arrange
        val evidence = listOf(createEvidence(timestampMillis = 1700000000000L))

        val mockCollector = mockk<Collector>()
        coEvery { mockCollector.collect(any(), any()) } returns Result.success(evidence)

        val expectedResult = createLanguageLearningResult(occurred = true, durationMinutes = 45)
        val mockAggregator = mockk<Aggregator<LanguageLearningResult>>()
        every {
            mockAggregator.aggregate(
                any(),
                any(),
                any()
            )
        } returns expectedResult

        val engine = createEngine(
            requestedMetrics = setOf(Metric.LANGUAGE_LEARNING),
            collectors = mapOf(Metric.LANGUAGE_LEARNING to mockCollector),
            aggregators = mapOf(Metric.LANGUAGE_LEARNING to mockAggregator)
        )

        // Act
        val result = engine.query(fromMillis = 1000L, toMillis = 2000L)

        // Assert - Direct field access, no days
        assertNotNull(result.languageLearning)
        assertEquals(expectedResult, result.languageLearning)
        // result.days would be a compile error
    }

    @Test
    fun `result has no summary field`() = runTest {
        // Arrange
        val mockCollector = mockk<Collector>()
        coEvery { mockCollector.collect(any(), any()) } returns Result.success(emptyList())

        val engine = createEngine(
            requestedMetrics = setOf(Metric.LANGUAGE_LEARNING),
            collectors = mapOf(Metric.LANGUAGE_LEARNING to mockCollector)
        )

        // Act
        val result = engine.query(fromMillis = 1000L, toMillis = 2000L)

        // Assert - No summary field exists
        assertNotNull(result.dataQuality)
        // result.summary would be a compile error
    }

    // ============================================================
    // Reading Integration Tests
    // ============================================================

    @Test
    fun `query with READING metric calls reading collector`() = runTest {
        // Arrange
        val mockCollector = mockk<Collector>()
        coEvery { mockCollector.collect(any(), any()) } returns Result.success(emptyList())

        val engine = createEngine(
            requestedMetrics = setOf(Metric.READING),
            collectors = mapOf(Metric.READING to mockCollector)
        )

        // Act
        engine.query(fromMillis = 1000L, toMillis = 2000L)

        // Assert
        coVerify { mockCollector.collect(1000L, 2000L) }
    }

    @Test
    fun `query without READING metric doesn't call reading collector`() = runTest {
        // Arrange
        val mockCollector = mockk<Collector>()

        val engine = createEngine(
            requestedMetrics = emptySet(),
            collectors = mapOf(Metric.READING to mockCollector)
        )

        // Act
        engine.query(fromMillis = 1000L, toMillis = 2000L)

        // Assert
        coVerify(exactly = 0) { mockCollector.collect(any(), any()) }
    }

    @Test
    fun `query with READING metric aggregates reading evidence correctly`() = runTest {
        // Arrange
        val evidence = listOf(createEvidence(timestampMillis = 1700000000000L))

        val mockCollector = mockk<Collector>()
        coEvery { mockCollector.collect(any(), any()) } returns Result.success(evidence)

        val mockAggregator = mockk<Aggregator<ReadingResult>>()
        every {
            mockAggregator.aggregate(
                any(),
                any(),
                any()
            )
        } returns createReadingResult()

        val engine = createEngine(
            requestedMetrics = setOf(Metric.READING),
            collectors = mapOf(Metric.READING to mockCollector),
            aggregators = mapOf(Metric.READING to mockAggregator)
        )

        // Act
        val result = engine.query(fromMillis = 1000L, toMillis = 2000L)

        // Assert
        assertNotNull(result.reading)
        verify(exactly = 1) { mockAggregator.aggregate(any(), match { it.size == 1 }, any()) }
    }

    @Test
    fun `query with both metrics calls both collectors and aggregators`() = runTest {
        // Arrange
        val langEvidence = listOf(createEvidence(timestampMillis = 1700000000000L))
        val readingEvidence = listOf(createEvidence(timestampMillis = 1700000000000L + 1000L))

        val mockLangCollector = mockk<Collector>()
        coEvery { mockLangCollector.collect(any(), any()) } returns Result.success(langEvidence)

        val mockReadingCollector = mockk<Collector>()
        coEvery { mockReadingCollector.collect(any(), any()) } returns Result.success(readingEvidence)

        val mockLangAggregator = mockk<Aggregator<LanguageLearningResult>>()
        every { mockLangAggregator.aggregate(any(), any(), any()) } returns createLanguageLearningResult(
            durationMinutes = 45
        )

        val mockReadingAggregator = mockk<Aggregator<ReadingResult>>()
        every { mockReadingAggregator.aggregate(any(), any(), any()) } returns createReadingResult(
            durationMinutes = 30
        )

        val engine = createEngine(
            requestedMetrics = setOf(Metric.LANGUAGE_LEARNING, Metric.READING),
            collectors = mapOf(
                Metric.LANGUAGE_LEARNING to mockLangCollector,
                Metric.READING to mockReadingCollector
            ),
            aggregators = mapOf(
                Metric.LANGUAGE_LEARNING to mockLangAggregator,
                Metric.READING to mockReadingAggregator
            )
        )

        // Act
        val result = engine.query(fromMillis = 1000L, toMillis = 2000L)

        // Assert
        coVerify { mockLangCollector.collect(1000L, 2000L) }
        coVerify { mockReadingCollector.collect(1000L, 2000L) }
        assertNotNull(result.languageLearning)
        assertNotNull(result.reading)
        assertEquals(45, result.languageLearning?.durationMinutes)
        assertEquals(30, result.reading?.durationMinutes)
    }

    @Test
    fun `unrequested metric returns null in result`() = runTest {
        // Arrange - Only request LANGUAGE_LEARNING
        val mockCollector = mockk<Collector>()
        coEvery { mockCollector.collect(any(), any()) } returns Result.success(
            listOf(createEvidence())
        )

        val mockAggregator = mockk<Aggregator<LanguageLearningResult>>()
        every { mockAggregator.aggregate(any(), any(), any()) } returns createLanguageLearningResult()

        val engine = createEngine(
            requestedMetrics = setOf(Metric.LANGUAGE_LEARNING),
            collectors = mapOf(Metric.LANGUAGE_LEARNING to mockCollector),
            aggregators = mapOf(Metric.LANGUAGE_LEARNING to mockAggregator)
        )

        // Act
        val result = engine.query(fromMillis = 1000L, toMillis = 2000L)

        // Assert
        assertNotNull(result.languageLearning)
        assertNull(result.reading) // Not requested
    }

    @Test
    fun `aggregator returns null when no evidence collected`() = runTest {
        // Arrange
        val mockCollector = mockk<Collector>()
        coEvery { mockCollector.collect(any(), any()) } returns Result.success(emptyList())

        val mockAggregator = mockk<Aggregator<LanguageLearningResult>>()
        every { mockAggregator.aggregate(any(), any(), any()) } returns null

        val engine = createEngine(
            requestedMetrics = setOf(Metric.LANGUAGE_LEARNING),
            collectors = mapOf(Metric.LANGUAGE_LEARNING to mockCollector),
            aggregators = mapOf(Metric.LANGUAGE_LEARNING to mockAggregator)
        )

        // Act
        val result = engine.query(fromMillis = 1000L, toMillis = 2000L)

        // Assert
        assertNull(result.languageLearning)
    }

    @Test
    fun `collector failure results in null metric result`() = runTest {
        // Arrange
        val mockCollector = mockk<Collector>()
        coEvery { mockCollector.collect(any(), any()) } returns
            Result.failure(SecurityException("Permission denied"))

        val engine = createEngine(
            requestedMetrics = setOf(Metric.LANGUAGE_LEARNING),
            collectors = mapOf(Metric.LANGUAGE_LEARNING to mockCollector)
        )

        // Act
        val result = engine.query(fromMillis = 1000L, toMillis = 2000L)

        // Assert
        assertNull(result.languageLearning)
        assertNotNull(result.dataQuality)
    }

    @Test
    fun `evidence from entire 24h window aggregated together`() = runTest {
        // Arrange
        val now = 1700000000000L
        val fromMillis = now - 86_400_000L

        // Evidence spanning the entire 24h window
        val evidence = listOf(
            createEvidence(timestampMillis = fromMillis), // Start of window
            createEvidence(timestampMillis = fromMillis + 12 * 3600 * 1000L), // Middle (12 hours)
            createEvidence(timestampMillis = now - 1000L) // Near end
        )

        val mockCollector = mockk<Collector>()
        coEvery { mockCollector.collect(any(), any()) } returns Result.success(evidence)

        val mockAggregator = mockk<Aggregator<LanguageLearningResult>>()
        every { mockAggregator.aggregate(any(), any(), any()) } returns createLanguageLearningResult()

        val engine = createEngine(
            requestedMetrics = setOf(Metric.LANGUAGE_LEARNING),
            collectors = mapOf(Metric.LANGUAGE_LEARNING to mockCollector),
            aggregators = mapOf(Metric.LANGUAGE_LEARNING to mockAggregator)
        )

        // Act
        engine.query(fromMillis = fromMillis, toMillis = now)

        // Assert - All 3 evidence items aggregated in single call
        verify(exactly = 1) { mockAggregator.aggregate(any(), match { it.size == 3 }, any()) }
    }

    // ============================================================
    // Data Quality Tests
    // ============================================================

    @Test
    fun `data quality reflects available and missing sources`() = runTest {
        // Arrange
        val mockCollector = mockk<Collector>()
        coEvery { mockCollector.collect(any(), any()) } returns Result.success(
            listOf(createEvidence())
        )

        val mockAggregator = mockk<Aggregator<LanguageLearningResult>>()
        every { mockAggregator.aggregate(any(), any(), any()) } returns createLanguageLearningResult()

        val engine = createEngine(
            requestedMetrics = setOf(Metric.LANGUAGE_LEARNING),
            collectors = mapOf(Metric.LANGUAGE_LEARNING to mockCollector),
            aggregators = mapOf(Metric.LANGUAGE_LEARNING to mockAggregator)
        )

        // Act
        val result = engine.query(fromMillis = 1000L, toMillis = 2000L)

        // Assert
        assertNotNull(result.dataQuality)
    }

    // ============================================================
    // Helper Methods
    // ============================================================

    private fun createTimestamp(
        year: Int,
        month: Int,
        dayOfMonth: Int,
        hour: Int = 0,
        minute: Int = 0
    ): Long {
        return Calendar.getInstance().apply {
            set(year, month - 1, dayOfMonth, hour, minute, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
    }

    private fun createEvidence(
        timestampMillis: Long = 1700000000000L,
        source: DataSource = DataSource.USAGE_STATS,
        confidence: Float = 0.80f,
        durationMinutes: Int = 30
    ): Evidence {
        return Evidence(
            source = source,
            timestampMillis = timestampMillis,
            confidence = confidence,
            durationMinutes = durationMinutes
        )
    }

    private fun createLanguageLearningResult(
        occurred: Boolean = true,
        confidence: Float = 0.80f,
        durationMinutes: Int? = 30
    ): LanguageLearningResult {
        return LanguageLearningResult(
            occurred = occurred,
            confidence = confidence,
            confidenceLevel = ConfidenceLevel.HIGH,
            durationMinutes = durationMinutes,
            source = DataSource.USAGE_STATS
        )
    }

    private fun createReadingResult(
        occurred: Boolean = true,
        confidence: Float = 0.82f,
        durationMinutes: Int? = 30
    ): ReadingResult {
        return ReadingResult(
            occurred = occurred,
            confidence = confidence,
            confidenceLevel = ConfidenceLevel.HIGH,
            durationMinutes = durationMinutes,
            sessionCount = 1,
            source = DataSource.USAGE_STATS,
            apps = listOf(com.tracker.core.result.AppInfo("com.amazon.kindle", "Kindle"))
        )
    }

    private fun createEngine(
        requestedMetrics: Set<Metric>,
        minConfidence: Float = 0.50f,
        permissionManager: PermissionManager? = null,
        collectors: Map<Metric, Collector> = emptyMap(),
        aggregators: Map<Metric, Aggregator<out com.tracker.core.result.HabitResult>> = emptyMap()
    ): HabitEngine {
        val mockPermissionManager =
            permissionManager ?: mockk<PermissionManager>(relaxed = true).also {
                every { it.checkPermission(any()) } returns PermissionStatus.GRANTED
                every { it.check(any()) } returns AccessStatus.GRANTED
            }

        return HabitEngine(
            requestedMetrics = requestedMetrics,
            minConfidence = minConfidence,
            permissionManager = mockPermissionManager,
            collectors = collectors,
            aggregators = aggregators
        )
    }
}
