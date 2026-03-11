package com.tracker.core.engine

import com.tracker.core.aggregator.Aggregator
import com.tracker.core.collector.Collector
import com.tracker.core.model.Evidence
import com.tracker.core.permission.PermissionManager
import com.tracker.core.permission.PermissionStatus
import com.tracker.core.result.AccessStatus
import com.tracker.core.result.LanguageLearningResult
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
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar

/**
 * Test suite for HabitEngine.
 *
 * Tests coordination of collectors, aggregators, day grouping, summary building,
 * and data quality assessment for habit tracking.
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
    fun `query groups evidence by day correctly`() = runTest {
        // Arrange
        val day1Start = createTimestamp(2024, 1, 1, 0, 0)
        val day1_8am = createTimestamp(2024, 1, 1, 8, 0)
        val day1_5pm = createTimestamp(2024, 1, 1, 17, 0)
        val day2_10am = createTimestamp(2024, 1, 2, 10, 0)

        val evidence = listOf(
            createEvidence(timestampMillis = day1_8am),
            createEvidence(timestampMillis = day1_5pm),
            createEvidence(timestampMillis = day2_10am)
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
        engine.query(fromMillis = day1Start, toMillis = day2_10am)

        // Assert - Day 1 should have 2 evidences grouped together
        verify { mockAggregator.aggregate(day1Start, match { it.size == 2 }, any()) }
    }

    @Test
    fun `query returns only days with evidence not empty days`() = runTest {
        // Arrange
        val jan1 = createTimestamp(2024, 1, 1)
        val jan3 = createTimestamp(2024, 1, 3)
        val jan5 = createTimestamp(2024, 1, 5)

        // Evidence only on Jan 1 and Jan 3 (Jan 2, 4, 5 have no evidence)
        val evidence = listOf(
            createEvidence(timestampMillis = jan1),
            createEvidence(timestampMillis = jan3)
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
        val result = engine.query(fromMillis = jan1, toMillis = jan5)

        // Assert - Sparse result: only 2 days, not 5
        assertEquals(2, result.days.size)
        assertTrue(result.days.any { it.timestampMillis == jan1 })
        assertTrue(result.days.any { it.timestampMillis == jan3 })
    }

    // ============================================================
    // Day Grouping Tests
    // ============================================================

    @Test
    fun `group evidence by day uses start of day timestamp 00_00_00`() = runTest {
        // Arrange
        val day1Start = createTimestamp(2024, 1, 1, 0, 0)
        val day1_8am = createTimestamp(2024, 1, 1, 8, 0)
        val day1_5pm = createTimestamp(2024, 1, 1, 17, 0)

        val evidence = listOf(
            createEvidence(timestampMillis = day1_8am),
            createEvidence(timestampMillis = day1_5pm)
        )

        val engine = createEngine(requestedMetrics = setOf(Metric.LANGUAGE_LEARNING))

        // Act
        val grouped = engine.groupEvidenceByDay(evidence)

        // Assert
        assertTrue(grouped.containsKey(day1Start))
        assertEquals(2, grouped[day1Start]?.size)
    }

    @Test
    fun `calculate days in range includes all days between from and to inclusive`() = runTest {
        // Arrange
        val jan1 = createTimestamp(2024, 1, 1)
        val jan5 = createTimestamp(2024, 1, 5)

        val engine = createEngine(requestedMetrics = setOf(Metric.LANGUAGE_LEARNING))

        // Act
        val days = engine.calculateDaysInRange(jan1, jan5)

        // Assert
        assertEquals(5, days)
    }

    @Test
    fun `calculate days in range with same day returns 1`() = runTest {
        // Arrange
        val jan1_8am = createTimestamp(2024, 1, 1, 8, 0)
        val jan1_5pm = createTimestamp(2024, 1, 1, 17, 0)

        val engine = createEngine(requestedMetrics = setOf(Metric.LANGUAGE_LEARNING))

        // Act
        val days = engine.calculateDaysInRange(jan1_8am, jan1_5pm)

        // Assert
        assertEquals(1, days)
    }

    @Test
    fun `evidence from different hours of same day grouped together`() = runTest {
        // Arrange
        val day1Start = createTimestamp(2024, 1, 1, 0, 0)
        val day1_2am = createTimestamp(2024, 1, 1, 2, 0)
        val day1_8am = createTimestamp(2024, 1, 1, 8, 0)
        val day1_5pm = createTimestamp(2024, 1, 1, 17, 0)
        val day1_11pm = createTimestamp(2024, 1, 1, 23, 0)

        val evidence = listOf(
            createEvidence(timestampMillis = day1_2am),
            createEvidence(timestampMillis = day1_8am),
            createEvidence(timestampMillis = day1_5pm),
            createEvidence(timestampMillis = day1_11pm)
        )

        val engine = createEngine(requestedMetrics = setOf(Metric.LANGUAGE_LEARNING))

        // Act
        val grouped = engine.groupEvidenceByDay(evidence)

        // Assert
        assertEquals(1, grouped.size)
        assertEquals(4, grouped[day1Start]?.size)
    }

    @Test
    fun `days without evidence are not returned in sparse map`() = runTest {
        // Arrange
        val jan1 = createTimestamp(2024, 1, 1)
        val jan2 = createTimestamp(2024, 1, 2)
        val jan3 = createTimestamp(2024, 1, 3)

        // Evidence only on day 1
        val evidence = listOf(createEvidence(timestampMillis = jan1))

        val engine = createEngine(requestedMetrics = setOf(Metric.LANGUAGE_LEARNING))

        // Act
        val grouped = engine.groupEvidenceByDay(evidence)

        // Assert
        assertEquals(1, grouped.size)
        assertEquals(1, grouped[jan1]?.size)
        assertFalse(grouped.containsKey(jan2))
        assertFalse(grouped.containsKey(jan3))
    }

    // ============================================================
    // Summary Building Tests
    // ============================================================

    @Test
    fun `summary counts language learning days correctly`() = runTest {
        // Arrange
        val jan1 = createTimestamp(2024, 1, 1)
        val jan2 = createTimestamp(2024, 1, 2)
        val jan3 = createTimestamp(2024, 1, 3)

        val evidence = listOf(
            createEvidence(timestampMillis = jan1),
            createEvidence(timestampMillis = jan2),
            createEvidence(timestampMillis = jan3)
        )

        val mockCollector = mockk<Collector>()
        coEvery { mockCollector.collect(any(), any()) } returns Result.success(evidence)

        val mockAggregator = mockk<Aggregator<LanguageLearningResult>>()
        every { mockAggregator.aggregate(jan1, any(), any()) } returns createLanguageLearningResult(
            occurred = true
        )
        every { mockAggregator.aggregate(jan2, any(), any()) } returns createLanguageLearningResult(
            occurred = true
        )
        every { mockAggregator.aggregate(jan3, any(), any()) } returns createLanguageLearningResult(
            occurred = false
        )

        val engine = createEngine(
            requestedMetrics = setOf(Metric.LANGUAGE_LEARNING),
            collectors = mapOf(Metric.LANGUAGE_LEARNING to mockCollector),
            aggregators = mapOf(Metric.LANGUAGE_LEARNING to mockAggregator)
        )

        // Act
        val result = engine.query(fromMillis = jan1, toMillis = jan3)

        // Assert - Only days with occurred=true counted
        assertEquals(2, result.summary.languageLearningDays)
    }

    @Test
    fun `summary calculates average as total minutes divided by total days in range`() = runTest {
        // Arrange
        val jan1 = createTimestamp(2024, 1, 1)
        val jan7 = createTimestamp(2024, 1, 7)

        // Only 2 days have activity: Jan 1 (30 min), Jan 3 (60 min)
        val jan3 = createTimestamp(2024, 1, 3)
        val evidence = listOf(
            createEvidence(timestampMillis = jan1),
            createEvidence(timestampMillis = jan3)
        )

        val mockCollector = mockk<Collector>()
        coEvery { mockCollector.collect(any(), any()) } returns Result.success(evidence)

        val mockAggregator = mockk<Aggregator<LanguageLearningResult>>()
        every { mockAggregator.aggregate(jan1, any(), any()) } returns createLanguageLearningResult(
            durationMinutes = 30
        )
        every { mockAggregator.aggregate(jan3, any(), any()) } returns createLanguageLearningResult(
            durationMinutes = 60
        )

        val engine = createEngine(
            requestedMetrics = setOf(Metric.LANGUAGE_LEARNING),
            collectors = mapOf(Metric.LANGUAGE_LEARNING to mockCollector),
            aggregators = mapOf(Metric.LANGUAGE_LEARNING to mockAggregator)
        )

        // Act
        val result = engine.query(fromMillis = jan1, toMillis = jan7)

        // Assert - Average = 90 total minutes / 7 days = 12 min/day
        assertEquals(7, result.summary.totalDays)
        assertEquals(12, result.summary.averageLanguageLearningMinutes)
    }

    @Test
    fun `summary calculates total minutes correctly`() = runTest {
        // Arrange
        val jan1 = createTimestamp(2024, 1, 1)
        val jan2 = createTimestamp(2024, 1, 2)
        val jan3 = createTimestamp(2024, 1, 3)

        val evidence = listOf(
            createEvidence(timestampMillis = jan1),
            createEvidence(timestampMillis = jan2),
            createEvidence(timestampMillis = jan3)
        )

        val mockCollector = mockk<Collector>()
        coEvery { mockCollector.collect(any(), any()) } returns Result.success(evidence)

        val mockAggregator = mockk<Aggregator<LanguageLearningResult>>()
        every { mockAggregator.aggregate(jan1, any(), any()) } returns createLanguageLearningResult(
            durationMinutes = 20
        )
        every { mockAggregator.aggregate(jan2, any(), any()) } returns createLanguageLearningResult(
            durationMinutes = 30
        )
        every { mockAggregator.aggregate(jan3, any(), any()) } returns createLanguageLearningResult(
            durationMinutes = 15
        )

        val engine = createEngine(
            requestedMetrics = setOf(Metric.LANGUAGE_LEARNING),
            collectors = mapOf(Metric.LANGUAGE_LEARNING to mockCollector),
            aggregators = mapOf(Metric.LANGUAGE_LEARNING to mockAggregator)
        )

        // Act
        val result = engine.query(fromMillis = jan1, toMillis = jan3)

        // Assert - Total = 20 + 30 + 15 = 65 minutes, Average = 65/3 = 21 min/day
        assertEquals(21, result.summary.averageLanguageLearningMinutes)
    }

    @Test
    fun `summary handles zero language learning days returns 0 for average`() = runTest {
        // Arrange
        val jan1 = createTimestamp(2024, 1, 1)
        val jan2 = createTimestamp(2024, 1, 2)

        val mockCollector = mockk<Collector>()
        coEvery { mockCollector.collect(any(), any()) } returns Result.success(emptyList())

        val engine = createEngine(
            requestedMetrics = setOf(Metric.LANGUAGE_LEARNING),
            collectors = mapOf(Metric.LANGUAGE_LEARNING to mockCollector)
        )

        // Act
        val result = engine.query(fromMillis = jan1, toMillis = jan2)

        // Assert
        assertEquals(0, result.summary.languageLearningDays)
        assertEquals(0, result.summary.averageLanguageLearningMinutes)
    }

    @Test
    fun `summary totalDays equals number of days in range not days with data`() = runTest {
        // Arrange
        val jan1 = createTimestamp(2024, 1, 1)
        val jan7 = createTimestamp(2024, 1, 7)

        // Only 2 days have evidence
        val evidence = listOf(
            createEvidence(timestampMillis = jan1),
            createEvidence(timestampMillis = jan7)
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
        val result = engine.query(fromMillis = jan1, toMillis = jan7)

        // Assert
        assertEquals(7, result.summary.totalDays)
        assertEquals(2, result.days.size)
    }

    @Test
    fun `summary fields are null when metric not requested`() = runTest {
        // Arrange
        val jan1 = createTimestamp(2024, 1, 1)
        val jan2 = createTimestamp(2024, 1, 2)

        val engine = createEngine(requestedMetrics = emptySet())

        // Act
        val result = engine.query(fromMillis = jan1, toMillis = jan2)

        // Assert
        assertNull(result.summary.languageLearningDays)
        assertNull(result.summary.averageLanguageLearningMinutes)
    }

    // ============================================================
    // Data Quality Tests
    // ============================================================

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
        timestampMillis: Long = 1000L,
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
