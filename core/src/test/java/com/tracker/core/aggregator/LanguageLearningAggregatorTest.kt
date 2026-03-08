package com.tracker.core.aggregator

import com.tracker.core.model.Evidence
import com.tracker.core.types.ConfidenceLevel
import com.tracker.core.types.DataSource
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Test suite for LanguageLearningAggregator.
 *
 * Tests aggregation logic including:
 * - Basic aggregation with single/multiple evidence
 * - Duration summation and app package extraction
 * - Deduplication of overlapping evidence (>80%)
 * - Confidence combination using probability formula
 * - Weak-only penalty application
 * - Occurred determination (>= 0.50 threshold)
 * - Confidence level mapping (HIGH/MEDIUM/LOW)
 * - Edge cases (zero duration, null ranges, overlap calculation)
 */
class LanguageLearningAggregatorTest {

    // ============================================================
    // Basic Aggregation Tests
    // ============================================================

    /**
     * Test: Empty evidence returns null.
     *
     * When no evidence is provided, aggregation should return null
     * since there's nothing to aggregate.
     */
    @Test
    fun `aggregate with empty evidence returns null`() {
        // Arrange
        val aggregator = LanguageLearningAggregator()
        val emptyEvidence = emptyList<Evidence>()

        // Act
        val result = aggregator.aggregate(dayMillis = 1000L, evidence = emptyEvidence)

        // Assert
        assertNull(result)
    }

    /**
     * Test: Single evidence returns correct result.
     *
     * With a single piece of evidence, the result should directly
     * reflect that evidence's properties.
     */
    @Test
    fun `aggregate with single evidence returns correct result`() {
        // Arrange
        val aggregator = LanguageLearningAggregator()
        val evidence = listOf(
            Evidence(
                source = DataSource.USAGE_STATS,
                timestampMillis = 1000L,
                confidence = 0.75f,
                durationMinutes = 30,
                metadata = mapOf(
                    "packageName" to "com.duolingo",
                    "appName" to "Duolingo"
                )
            )
        )

        // Act
        val result = aggregator.aggregate(dayMillis = 1000L, evidence = evidence)

        // Assert
        assertNotNull(result)
        assertEquals(true, result!!.occurred)
        assertEquals(0.75f, result.confidence, 0.001f)
        assertEquals(ConfidenceLevel.HIGH, result.confidenceLevel)
        assertEquals(30, result.durationMinutes)
        assertEquals(DataSource.USAGE_STATS, result.source)
        assertEquals(1, result.apps.size)
        assertEquals("com.duolingo", result.apps[0].packageName)
        assertEquals("Duolingo", result.apps[0].appName)
    }

    /**
     * Test: Multiple evidence items are combined correctly.
     *
     * Uses probability formula: 1 - ∏(1 - p_i)
     * Two 0.70 confidences: 1 - (0.3 * 0.3) = 0.91
     */
    @Test
    fun `aggregate combines multiple evidence items correctly`() {
        // Arrange
        val aggregator = LanguageLearningAggregator()
        val evidence = listOf(
            Evidence(
                source = DataSource.USAGE_STATS,
                timestampMillis = 1000L,
                confidence = 0.70f,
                durationMinutes = 20
            ),
            Evidence(
                source = DataSource.USAGE_STATS,
                timestampMillis = 2000L,
                confidence = 0.70f,
                durationMinutes = 25
            )
        )

        // Act
        val result = aggregator.aggregate(dayMillis = 1000L, evidence = evidence)

        // Assert
        assertNotNull(result)
        // Combined probability: 1 - (0.3 * 0.3) = 0.91
        assertEquals(0.91f, result!!.confidence, 0.001f)
        assertEquals(true, result.occurred)
        assertEquals(ConfidenceLevel.HIGH, result.confidenceLevel)
    }

    /**
     * Test: Durations are summed from all evidence.
     *
     * Null durations should be ignored, only non-null values summed.
     */
    @Test
    fun `aggregate sums durations from all evidence`() {
        // Arrange
        val aggregator = LanguageLearningAggregator()
        val evidence = listOf(
            Evidence(
                source = DataSource.USAGE_STATS,
                timestampMillis = 1000L,
                confidence = 0.80f,
                durationMinutes = 20
            ),
            Evidence(
                source = DataSource.USAGE_STATS,
                timestampMillis = 2000L,
                confidence = 0.60f,
                durationMinutes = 30
            ),
            Evidence(
                source = DataSource.USAGE_STATS,
                timestampMillis = 3000L,
                confidence = 0.70f,
                durationMinutes = null  // null should be ignored
            )
        )

        // Act
        val result = aggregator.aggregate(dayMillis = 1000L, evidence = evidence)

        // Assert
        assertNotNull(result)
        assertEquals(50, result!!.durationMinutes)  // 20 + 30
    }

    /**
     * Test: Distinct app packages are extracted.
     *
     * Duplicate package names should be removed, only distinct packages retained.
     */
    @Test
    fun `aggregate extracts distinct app packages`() {
        // Arrange
        val aggregator = LanguageLearningAggregator()
        val evidence = listOf(
            Evidence(
                source = DataSource.USAGE_STATS,
                timestampMillis = 1000L,
                confidence = 0.80f,
                metadata = mapOf(
                    "packageName" to "com.duolingo",
                    "appName" to "Duolingo"
                )
            ),
            Evidence(
                source = DataSource.USAGE_STATS,
                timestampMillis = 2000L,
                confidence = 0.70f,
                metadata = mapOf(
                    "packageName" to "com.babbel",
                    "appName" to "Babbel"
                )
            ),
            Evidence(
                source = DataSource.USAGE_STATS,
                timestampMillis = 3000L,
                confidence = 0.60f,
                metadata = mapOf(
                    "packageName" to "com.duolingo",
                    "appName" to "Duolingo"
                )  // Duplicate
            )
        )

        // Act
        val result = aggregator.aggregate(dayMillis = 1000L, evidence = evidence)

        // Assert
        assertNotNull(result)
        assertEquals(2, result!!.apps.size)  // Distinct only
        assertTrue(result.apps.any { it.packageName == "com.duolingo" && it.appName == "Duolingo" })
        assertTrue(result.apps.any { it.packageName == "com.babbel" && it.appName == "Babbel" })
    }

    // ============================================================
    // Deduplication Tests
    // ============================================================

    /**
     * Test: Overlapping evidence (>80%) keeps higher confidence item.
     *
     * When two evidence items overlap by more than 80%, only the one
     * with higher confidence should be retained.
     */
    @Test
    fun `overlapping evidence keeps higher confidence item`() {
        // Arrange
        val aggregator = LanguageLearningAggregator()
        val evidence = listOf(
            Evidence(
                source = DataSource.USAGE_STATS,
                timestampMillis = 1000L,
                confidence = 0.60f,  // Lower confidence
                durationMinutes = 30,
                startTimeMillis = 1000L,
                endTimeMillis = 1000L + 30 * 60 * 1000,  // 30 minutes
                metadata = mapOf(
                    "packageName" to "com.duolingo",
                    "appName" to "Duolingo"
                )
            ),
            Evidence(
                source = DataSource.USAGE_STATS,
                timestampMillis = 1000L,
                confidence = 0.80f,  // Higher confidence
                durationMinutes = 25,
                startTimeMillis = 1100L,  // Small offset, creates >80% overlap
                endTimeMillis = 1100L + 25 * 60 * 1000,  // 25 minutes
                metadata = mapOf(
                    "packageName" to "com.duolingo",  // Same app
                    "appName" to "Duolingo"
                )
            )
        )

        // Act
        val result = aggregator.aggregate(dayMillis = 1000L, evidence = evidence)

        // Assert
        assertNotNull(result)
        // Should use only the 0.80 confidence item (lower one filtered out)
        assertEquals(0.80f, result!!.confidence, 0.001f)
        // Duration should be from the kept item only
        assertEquals(25, result.durationMinutes)
    }

    /**
     * Test: Non-overlapping evidence are all retained.
     *
     * When evidence items don't overlap significantly (<80%),
     * all should be kept and combined.
     */
    @Test
    fun `non-overlapping evidence all retained`() {
        // Arrange
        val aggregator = LanguageLearningAggregator()
        val evidence = listOf(
            Evidence(
                source = DataSource.USAGE_STATS,
                timestampMillis = 1000L,
                confidence = 0.70f,
                durationMinutes = 20,
                startTimeMillis = 1000L,
                endTimeMillis = 1000L + 20 * 60 * 1000  // 8:00-8:20
            ),
            Evidence(
                source = DataSource.USAGE_STATS,
                timestampMillis = 1000L,
                confidence = 0.60f,
                durationMinutes = 15,
                startTimeMillis = 1000L + 30 * 60 * 1000,  // 8:30-8:45 (no overlap)
                endTimeMillis = 1000L + 45 * 60 * 1000
            )
        )

        // Act
        val result = aggregator.aggregate(dayMillis = 1000L, evidence = evidence)

        // Assert
        assertNotNull(result)
        // Both should be combined: 1 - (0.3 * 0.4) = 0.88
        assertEquals(0.88f, result!!.confidence, 0.001f)
        // Both durations summed
        assertEquals(35, result.durationMinutes)  // 20 + 15
    }

    /**
     * Test: Evidence with no time range (null) is not deduplicated.
     *
     * When startTimeMillis or endTimeMillis is null, overlap calculation
     * returns 0f, so evidence should not be filtered out.
     */
    @Test
    fun `evidence with no time range not deduplicated`() {
        // Arrange
        val aggregator = LanguageLearningAggregator()
        val evidence = listOf(
            Evidence(
                source = DataSource.USAGE_STATS,
                timestampMillis = 1000L,
                confidence = 0.80f,
                durationMinutes = 30,
                startTimeMillis = null,  // Missing time range
                endTimeMillis = null
            ),
            Evidence(
                source = DataSource.USAGE_STATS,
                timestampMillis = 1000L,
                confidence = 0.70f,
                durationMinutes = 20,
                startTimeMillis = null,  // Missing time range
                endTimeMillis = null
            )
        )

        // Act
        val result = aggregator.aggregate(dayMillis = 1000L, evidence = evidence)

        // Assert
        assertNotNull(result)
        // Both should be kept and combined: 1 - (0.2 * 0.3) = 0.94
        assertEquals(0.94f, result!!.confidence, 0.001f)
        // Both durations summed
        assertEquals(50, result.durationMinutes)  // 30 + 20
    }

    /**
     * Test: Multiple overlaps handled correctly, keeps highest confidence.
     *
     * When multiple evidence items overlap with each other, only the
     * highest confidence item should be retained.
     */
    @Test
    fun `multiple overlaps handled correctly keeps highest confidence`() {
        // Arrange
        val aggregator = LanguageLearningAggregator()
        val evidence = listOf(
            Evidence(
                source = DataSource.USAGE_STATS,
                timestampMillis = 1000L,
                confidence = 0.90f,  // Highest - should be kept
                durationMinutes = 30,
                startTimeMillis = 1000L,
                endTimeMillis = 1000L + 30 * 60 * 1000,
                metadata = mapOf(
                    "packageName" to "com.duolingo",
                    "appName" to "Duolingo"
                )
            ),
            Evidence(
                source = DataSource.USAGE_STATS,
                timestampMillis = 1000L,
                confidence = 0.70f,  // Overlaps with first - filtered
                durationMinutes = 28,
                startTimeMillis = 1100L,
                endTimeMillis = 1100L + 28 * 60 * 1000,
                metadata = mapOf(
                    "packageName" to "com.duolingo",  // Same app
                    "appName" to "Duolingo"
                )
            ),
            Evidence(
                source = DataSource.USAGE_STATS,
                timestampMillis = 1000L,
                confidence = 0.60f,  // Also overlaps with first - filtered
                durationMinutes = 25,
                startTimeMillis = 1200L,
                endTimeMillis = 1200L + 25 * 60 * 1000,
                metadata = mapOf(
                    "packageName" to "com.duolingo",  // Same app
                    "appName" to "Duolingo"
                )
            )
        )

        // Act
        val result = aggregator.aggregate(dayMillis = 1000L, evidence = evidence)

        // Assert
        assertNotNull(result)
        // Should keep only the 0.90 item
        assertEquals(0.90f, result!!.confidence, 0.001f)
        assertEquals(30, result.durationMinutes)
    }

    /**
     * Test: Overlap calculation uses shorter duration as base.
     *
     * Overlap percentage is calculated as: overlap / min(duration1, duration2)
     * This test verifies a short event completely inside a long event
     * results in 100% overlap (based on the short event's duration).
     */
    @Test
    fun `overlap calculation uses shorter duration as base`() {
        // Arrange
        val aggregator = LanguageLearningAggregator()
        // Create evidence where a short event is completely within a long event
        // Long: 60 minutes (1000 to 1000 + 60*60*1000)
        // Short: 20 minutes (1000 to 1000 + 20*60*1000), completely inside long
        // Overlap: 20 minutes / 20 minutes (shorter) = 100% overlap
        val evidence = listOf(
            Evidence(
                source = DataSource.USAGE_STATS,
                timestampMillis = 1000L,
                confidence = 0.80f,  // Higher confidence
                durationMinutes = 60,
                startTimeMillis = 1000L,
                endTimeMillis = 1000L + 60 * 60 * 1000,  // 60 minutes
                metadata = mapOf(
                    "packageName" to "com.duolingo",
                    "appName" to "Duolingo"
                )
            ),
            Evidence(
                source = DataSource.USAGE_STATS,
                timestampMillis = 1000L,
                confidence = 0.70f,  // Lower confidence
                durationMinutes = 20,
                startTimeMillis = 1000L,
                endTimeMillis = 1000L + 20 * 60 * 1000,  // 20 minutes, fully inside
                metadata = mapOf(
                    "packageName" to "com.duolingo",  // Same app
                    "appName" to "Duolingo"
                )
            )
        )

        // Act
        val result = aggregator.aggregate(dayMillis = 1000L, evidence = evidence)

        // Assert
        assertNotNull(result)
        // Short event should be filtered (100% overlap > 80%)
        // Should keep only the 0.80 confidence item
        assertEquals(0.80f, result!!.confidence, 0.001f)
        assertEquals(60, result.durationMinutes)
    }

    // ============================================================
    // Confidence Combination Tests
    // ============================================================

    /**
     * Test: Two confidence values combined using probability formula.
     *
     * Formula: 1 - (1 - p1) * (1 - p2)
     * Example: 1 - (1 - 0.60) * (1 - 0.50) = 1 - (0.4 * 0.5) = 0.80
     */
    @Test
    fun `two confidence values combined using probability formula`() {
        // Arrange
        val aggregator = LanguageLearningAggregator()
        val evidence = listOf(
            Evidence(
                source = DataSource.USAGE_STATS,
                timestampMillis = 1000L,
                confidence = 0.60f
            ),
            Evidence(
                source = DataSource.USAGE_STATS,
                timestampMillis = 2000L,
                confidence = 0.50f
            )
        )

        // Act
        val result = aggregator.aggregate(dayMillis = 1000L, evidence = evidence)

        // Assert
        assertNotNull(result)
        // Formula: 1 - (1 - 0.60) * (1 - 0.50) = 1 - (0.4 * 0.5) = 1 - 0.2 = 0.80
        assertEquals(0.80f, result!!.confidence, 0.001f)
    }

    /**
     * Test: Multiple (3+) confidence values combined correctly.
     *
     * Formula: 1 - ∏(1 - p_i)
     * Example: Three 0.50 values: 1 - (0.5)^3 = 1 - 0.125 = 0.875
     */
    @Test
    fun `multiple confidence values combined correctly`() {
        // Arrange
        val aggregator = LanguageLearningAggregator()
        val evidence = listOf(
            Evidence(
                source = DataSource.USAGE_STATS,
                timestampMillis = 1000L,
                confidence = 0.50f
            ),
            Evidence(
                source = DataSource.USAGE_STATS,
                timestampMillis = 2000L,
                confidence = 0.50f
            ),
            Evidence(
                source = DataSource.USAGE_STATS,
                timestampMillis = 3000L,
                confidence = 0.50f
            )
        )

        // Act
        val result = aggregator.aggregate(dayMillis = 1000L, evidence = evidence)

        // Assert
        assertNotNull(result)
        // Formula: 1 - (1 - 0.50)^3 = 1 - 0.5^3 = 1 - 0.125 = 0.875
        assertEquals(0.875f, result!!.confidence, 0.001f)
    }

    /**
     * Test: Weak-only penalty applied when all evidence < 0.50.
     *
     * When ALL evidence has confidence < 0.50, a 0.15 penalty is applied.
     * This reduces confidence for cases where only weak signals exist.
     */
    @Test
    fun `weak-only penalty applied when all evidence below 0_50`() {
        // Arrange
        val aggregator = LanguageLearningAggregator()
        val evidence = listOf(
            Evidence(
                source = DataSource.USAGE_STATS,
                timestampMillis = 1000L,
                confidence = 0.40f
            ),
            Evidence(
                source = DataSource.USAGE_STATS,
                timestampMillis = 2000L,
                confidence = 0.30f
            )
        )

        // Act
        val result = aggregator.aggregate(dayMillis = 1000L, evidence = evidence)

        // Assert
        assertNotNull(result)
        // Combined: 1 - (0.6 * 0.7) = 1 - 0.42 = 0.58
        // After penalty: 0.58 - 0.15 = 0.43
        assertEquals(0.43f, result!!.confidence, 0.001f)
        assertEquals(false, result.occurred)
        assertEquals(ConfidenceLevel.LOW, result.confidenceLevel)
    }

    /**
     * Test: Weak-only penalty NOT applied when any evidence >= 0.50.
     *
     * If at least one evidence has confidence >= 0.50, no penalty is applied.
     */
    @Test
    fun `weak-only penalty not applied when any evidence above or equal 0_50`() {
        // Arrange
        val aggregator = LanguageLearningAggregator()
        val evidence = listOf(
            Evidence(
                source = DataSource.USAGE_STATS,
                timestampMillis = 1000L,
                confidence = 0.50f  // At threshold
            ),
            Evidence(
                source = DataSource.USAGE_STATS,
                timestampMillis = 2000L,
                confidence = 0.30f
            )
        )

        // Act
        val result = aggregator.aggregate(dayMillis = 1000L, evidence = evidence)

        // Assert
        assertNotNull(result)
        // Combined: 1 - (0.5 * 0.7) = 1 - 0.35 = 0.65
        // No penalty applied (one evidence >= 0.50)
        assertEquals(0.65f, result!!.confidence, 0.001f)
        assertEquals(true, result.occurred)
    }

    /**
     * Test: Combined confidence clamped to minimum 0.0.
     *
     * After applying weak-only penalty, confidence should not go below 0.0.
     * Uses max(0f, combinedConfidence - WEAK_ONLY_PENALTY).
     */
    @Test
    fun `combined confidence clamped to minimum 0_0`() {
        // Arrange
        val aggregator = LanguageLearningAggregator()
        // Create evidence that after weak-only penalty would go negative
        val evidence = listOf(
            Evidence(
                source = DataSource.USAGE_STATS,
                timestampMillis = 1000L,
                confidence = 0.10f
            ),
            Evidence(
                source = DataSource.USAGE_STATS,
                timestampMillis = 2000L,
                confidence = 0.05f
            )
        )

        // Act
        val result = aggregator.aggregate(dayMillis = 1000L, evidence = evidence)

        // Assert
        assertNotNull(result)
        // Combined: 1 - (0.9 * 0.95) = 1 - 0.855 = 0.145
        // After penalty: 0.145 - 0.15 = -0.005, clamped to 0.0
        assertEquals(0.0f, result!!.confidence, 0.001f)
        assertEquals(false, result.occurred)
    }

    // ============================================================
    // Occurred Determination Tests
    // ============================================================

    /**
     * Test: Confidence >= 0.50 sets occurred to true.
     *
     * When confidence is above the 0.50 threshold, occurred should be true.
     */
    @Test
    fun `confidence above 0_50 sets occurred to true`() {
        // Arrange
        val aggregator = LanguageLearningAggregator()
        val evidence = listOf(
            Evidence(
                source = DataSource.USAGE_STATS,
                timestampMillis = 1000L,
                confidence = 0.51f  // Just above threshold
            )
        )

        // Act
        val result = aggregator.aggregate(dayMillis = 1000L, evidence = evidence)

        // Assert
        assertNotNull(result)
        assertEquals(0.51f, result!!.confidence, 0.001f)
        assertEquals(true, result.occurred)
        assertEquals(ConfidenceLevel.MEDIUM, result.confidenceLevel)
    }

    /**
     * Test: Confidence < 0.50 sets occurred to false.
     *
     * When confidence is below the 0.50 threshold, occurred should be false.
     * Note: Single evidence with confidence < 0.50 triggers weak-only penalty.
     */
    @Test
    fun `confidence below 0_50 sets occurred to false`() {
        // Arrange
        val aggregator = LanguageLearningAggregator()
        val evidence = listOf(
            Evidence(
                source = DataSource.USAGE_STATS,
                timestampMillis = 1000L,
                confidence = 0.49f  // Just below threshold
            )
        )

        // Act
        val result = aggregator.aggregate(dayMillis = 1000L, evidence = evidence)

        // Assert
        assertNotNull(result)
        // 0.49 is < 0.50, so weak-only penalty applied: 0.49 - 0.15 = 0.34
        assertEquals(0.34f, result!!.confidence, 0.001f)
        assertEquals(false, result.occurred)
        assertEquals(ConfidenceLevel.LOW, result.confidenceLevel)
    }

    /**
     * Test: Confidence exactly 0.50 sets occurred to true.
     *
     * The threshold is >= 0.50, so exactly 0.50 should result in occurred = true.
     */
    @Test
    fun `confidence exactly 0_50 sets occurred to true`() {
        // Arrange
        val aggregator = LanguageLearningAggregator()
        val evidence = listOf(
            Evidence(
                source = DataSource.USAGE_STATS,
                timestampMillis = 1000L,
                confidence = 0.50f  // Exactly at threshold
            )
        )

        // Act
        val result = aggregator.aggregate(dayMillis = 1000L, evidence = evidence)

        // Assert
        assertNotNull(result)
        assertEquals(0.50f, result!!.confidence, 0.001f)
        assertEquals(true, result.occurred)  // >= 0.50 means true
        assertEquals(ConfidenceLevel.MEDIUM, result.confidenceLevel)
    }

    // ============================================================
    // Confidence Level Mapping Tests
    // ============================================================

    /**
     * Test: Confidence exactly 0.74 maps to MEDIUM level.
     *
     * Tests the upper boundary of MEDIUM level (< 0.75).
     * Thresholds: HIGH >= 0.75, MEDIUM >= 0.50, LOW < 0.50
     */
    @Test
    fun `confidence exactly 0_74 maps to MEDIUM level`() {
        // Arrange
        val aggregator = LanguageLearningAggregator()
        val evidence = listOf(
            Evidence(
                source = DataSource.USAGE_STATS,
                timestampMillis = 1000L,
                confidence = 0.74f  // Just below HIGH threshold (0.75)
            )
        )

        // Act
        val result = aggregator.aggregate(dayMillis = 1000L, evidence = evidence)

        // Assert
        assertNotNull(result)
        assertEquals(0.74f, result!!.confidence, 0.001f)
        assertEquals(ConfidenceLevel.MEDIUM, result.confidenceLevel)
        assertEquals(true, result.occurred)  // >= 0.50
    }

    /**
     * Test: Confidence exactly 0.75 maps to HIGH level.
     *
     * Tests the lower boundary of HIGH level (>= 0.75).
     * This is a critical boundary test for the HIGH confidence threshold.
     */
    @Test
    fun `confidence exactly 0_75 maps to HIGH level`() {
        // Arrange
        val aggregator = LanguageLearningAggregator()
        val evidence = listOf(
            Evidence(
                source = DataSource.USAGE_STATS,
                timestampMillis = 1000L,
                confidence = 0.75f  // Exactly at HIGH threshold
            )
        )

        // Act
        val result = aggregator.aggregate(dayMillis = 1000L, evidence = evidence)

        // Assert
        assertNotNull(result)
        assertEquals(0.75f, result!!.confidence, 0.001f)
        assertEquals(ConfidenceLevel.HIGH, result.confidenceLevel)
        assertEquals(true, result.occurred)
    }

    // ============================================================
    // Edge Cases Tests
    // ============================================================

    /**
     * Test: Overlap calculation handles zero duration gracefully.
     *
     * When evidence has zero duration (startTime == endTime),
     * overlap calculation should return 0f and not throw division by zero.
     */
    @Test
    fun `overlap calculation handles zero duration gracefully`() {
        // Arrange
        val aggregator = LanguageLearningAggregator()
        // Create evidence with same start and end time (zero duration)
        val evidence = listOf(
            Evidence(
                source = DataSource.USAGE_STATS,
                timestampMillis = 1000L,
                confidence = 0.80f,
                durationMinutes = 0,
                startTimeMillis = 1000L,
                endTimeMillis = 1000L  // Zero duration
            ),
            Evidence(
                source = DataSource.USAGE_STATS,
                timestampMillis = 1000L,
                confidence = 0.70f,
                durationMinutes = 0,
                startTimeMillis = 1000L,
                endTimeMillis = 1000L  // Zero duration
            )
        )

        // Act
        val result = aggregator.aggregate(dayMillis = 1000L, evidence = evidence)

        // Assert
        assertNotNull(result)
        // Both should be kept (overlap calculation returns 0f for zero duration)
        // Combined: 1 - (0.2 * 0.3) = 0.94
        assertEquals(0.94f, result!!.confidence, 0.001f)
    }

    /**
     * Test: Overlap percentage returns 0 when no overlap.
     *
     * When evidence items are sequential with no time overlap,
     * both should be retained.
     */
    @Test
    fun `overlap percentage returns 0 when no overlap`() {
        // Arrange
        val aggregator = LanguageLearningAggregator()
        // Create sequential events with no overlap
        val evidence = listOf(
            Evidence(
                source = DataSource.USAGE_STATS,
                timestampMillis = 1000L,
                confidence = 0.70f,
                durationMinutes = 10,
                startTimeMillis = 1000L,
                endTimeMillis = 1000L + 10 * 60 * 1000  // Ends at 1000 + 600000
            ),
            Evidence(
                source = DataSource.USAGE_STATS,
                timestampMillis = 2000L,
                confidence = 0.60f,
                durationMinutes = 10,
                startTimeMillis = 1000L + 10 * 60 * 1000 + 1,  // Starts 1ms after first ends
                endTimeMillis = 1000L + 20 * 60 * 1000
            )
        )

        // Act
        val result = aggregator.aggregate(dayMillis = 1000L, evidence = evidence)

        // Assert
        assertNotNull(result)
        // Both should be kept (no overlap)
        // Combined: 1 - (0.3 * 0.4) = 0.88
        assertEquals(0.88f, result!!.confidence, 0.001f)
    }

    /**
     * Test: Primary source is from first evidence after deduplication.
     *
     * The primary source should come from the first evidence item
     * (highest confidence after sorting).
     */
    @Test
    fun `primary source is from first evidence after deduplication`() {
        // Arrange
        val aggregator = LanguageLearningAggregator()
        val evidence = listOf(
            Evidence(
                source = DataSource.USAGE_STATS,
                timestampMillis = 1000L,
                confidence = 0.80f  // Highest confidence, will be first after sort
            ),
            Evidence(
                source = DataSource.UNKNOWN,  // Different source
                timestampMillis = 2000L,
                confidence = 0.60f
            )
        )

        // Act
        val result = aggregator.aggregate(dayMillis = 1000L, evidence = evidence)

        // Assert
        assertNotNull(result)
        // Primary source should be from the highest confidence evidence
        assertEquals(DataSource.USAGE_STATS, result!!.source)
    }
}
