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
 * Test suite for ReadingAggregator.
 *
 * Tests aggregation logic including:
 * - Basic aggregation with single/multiple evidence
 * - Duration summation and app package extraction
 * - Confidence combination using probability formula
 * - Weak-only penalty application
 * - Occurred determination (>= minConfidence threshold)
 * - Confidence level mapping (HIGH/MEDIUM/LOW)
 * - Edge cases (zero duration, null ranges, missing packageName)
 * - Session count (number of valid reading sessions)
 *
 * Valid session criteria:
 * - durationMinutes != null && durationMinutes > 0
 * - packageName != null
 *
 * Note: Currently only UsageStats source exists. When OAuth sources are added,
 * deduplication logic will be required to avoid counting the same session twice
 * when reported by multiple sources (similar to LanguageLearningAggregator).
 */
class ReadingAggregatorTest {

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
        val aggregator = ReadingAggregator()
        val emptyEvidence = emptyList<Evidence>()

        // Act
        val result = aggregator.aggregate(dayMillis = 1000L, evidence = emptyEvidence, minConfidence = 0.50f)

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
        val aggregator = ReadingAggregator()
        val evidence = listOf(
            Evidence(
                source = DataSource.USAGE_STATS,
                timestampMillis = 1000L,
                confidence = 0.82f,  // Kindle confidence from KnownApps
                durationMinutes = 45,
                metadata = mapOf(
                    "packageName" to "com.amazon.kindle",
                    "appName" to "Kindle"
                )
            )
        )

        // Act
        val result = aggregator.aggregate(dayMillis = 1000L, evidence = evidence, minConfidence = 0.50f)

        // Assert
        assertNotNull(result)
        assertEquals(true, result!!.occurred)
        assertEquals(0.82f, result.confidence, 0.001f)
        assertEquals(ConfidenceLevel.HIGH, result.confidenceLevel)
        assertEquals(45, result.durationMinutes)
        assertEquals(DataSource.USAGE_STATS, result.source)
        assertEquals(1, result.sessionCount)  // 1 valid session
        assertEquals(1, result.apps.size)
        assertEquals("com.amazon.kindle", result.apps[0].packageName)
        assertNull(result.title)
    }

    /**
     * Test: Multiple evidence items from different reading apps are combined correctly.
     *
     * Uses probability formula: 1 - ∏(1 - p_i)
     * Two sessions: 0.82 (Kindle) and 0.80 (Play Books)
     * Combined: 1 - (0.18 * 0.20) = 1 - 0.036 = 0.964
     *
     * These are sequential sessions from the same day (Android allows only one app
     * in foreground at a time, so no overlap possible with single UsageStats source).
     */
    @Test
    fun `aggregate combines multiple reading sessions correctly`() {
        // Arrange
        val aggregator = ReadingAggregator()
        val evidence = listOf(
            Evidence(
                source = DataSource.USAGE_STATS,
                timestampMillis = 1000L,
                confidence = 0.82f,  // Kindle
                durationMinutes = 30,
                metadata = mapOf(
                    "packageName" to "com.amazon.kindle",
                    "appName" to "Kindle"
                )
            ),
            Evidence(
                source = DataSource.USAGE_STATS,
                timestampMillis = 2000L,
                confidence = 0.80f,  // Play Books
                durationMinutes = 25,
                metadata = mapOf(
                    "packageName" to "com.google.android.apps.books",
                    "appName" to "Play Books"
                )
            )
        )

        // Act
        val result = aggregator.aggregate(dayMillis = 1000L, evidence = evidence, minConfidence = 0.50f)

        // Assert
        assertNotNull(result)
        // Combined probability: 1 - (0.18 * 0.20) = 0.964
        assertEquals(0.964f, result!!.confidence, 0.001f)
        assertEquals(true, result.occurred)
        assertEquals(ConfidenceLevel.HIGH, result.confidenceLevel)
        assertEquals(2, result.sessionCount)  // 2 valid sessions
        assertEquals(2, result.apps.size)  // 2 distinct apps
    }

    /**
     * Test: Durations are summed from all evidence with valid sessions.
     *
     * Null durations are ignored and not counted as sessions.
     */
    @Test
    fun `aggregate sums durations and counts only valid sessions`() {
        // Arrange
        val aggregator = ReadingAggregator()
        val evidence = listOf(
            Evidence(
                source = DataSource.USAGE_STATS,
                timestampMillis = 1000L,
                confidence = 0.82f,  // Kindle
                durationMinutes = 30,
                metadata = mapOf(
                    "packageName" to "com.amazon.kindle",
                    "appName" to "Kindle"
                )
            ),
            Evidence(
                source = DataSource.USAGE_STATS,
                timestampMillis = 2000L,
                confidence = 0.80f,  // Play Books
                durationMinutes = 20,
                metadata = mapOf(
                    "packageName" to "com.google.android.apps.books",
                    "appName" to "Play Books"
                )
            ),
            Evidence(
                source = DataSource.USAGE_STATS,
                timestampMillis = 3000L,
                confidence = 0.82f,  // Kindle again
                durationMinutes = null,  // null duration = NOT a valid session
                metadata = mapOf(
                    "packageName" to "com.amazon.kindle",
                    "appName" to "Kindle"
                )
            )
        )

        // Act
        val result = aggregator.aggregate(dayMillis = 1000L, evidence = evidence, minConfidence = 0.50f)

        // Assert
        assertNotNull(result)
        assertEquals(50, result!!.durationMinutes)  // 30 + 20
        assertEquals(2, result.sessionCount)  // Only 2 valid sessions
    }

    /**
     * Test: Zero duration is NOT counted as a session.
     *
     * sessionCount only includes evidence where durationMinutes > 0.
     */
    @Test
    fun `zero duration not counted as session`() {
        // Arrange
        val aggregator = ReadingAggregator()
        val evidence = listOf(
            Evidence(
                source = DataSource.USAGE_STATS,
                timestampMillis = 1000L,
                confidence = 0.82f,  // Kindle
                durationMinutes = 0,  // Zero duration = NOT a valid session
                metadata = mapOf(
                    "packageName" to "com.amazon.kindle",
                    "appName" to "Kindle"
                )
            ),
            Evidence(
                source = DataSource.USAGE_STATS,
                timestampMillis = 2000L,
                confidence = 0.80f,  // Play Books
                durationMinutes = 30,
                metadata = mapOf(
                    "packageName" to "com.google.android.apps.books",
                    "appName" to "Play Books"
                )
            )
        )

        // Act
        val result = aggregator.aggregate(dayMillis = 1000L, evidence = evidence, minConfidence = 0.50f)

        // Assert
        assertNotNull(result)
        assertEquals(30, result!!.durationMinutes)  // 0 + 30 = 30
        assertEquals(1, result.sessionCount)  // Only 1 valid session
    }

    /**
     * Test: Missing packageName means NOT a valid session.
     *
     * Evidence without packageName:
     * - Is still included in confidence aggregation
     * - Duration is still summed
     * - But NOT counted as a session
     * - NOT included in appPackages
     */
    @Test
    fun `missing packageName not counted as session`() {
        // Arrange
        val aggregator = ReadingAggregator()
        val evidence = listOf(
            Evidence(
                source = DataSource.USAGE_STATS,
                timestampMillis = 1000L,
                confidence = 0.80f,
                durationMinutes = 30,
                metadata = emptyMap()  // No packageName = NOT a valid session
            ),
            Evidence(
                source = DataSource.USAGE_STATS,
                timestampMillis = 2000L,
                confidence = 0.82f,  // Kindle
                durationMinutes = 20,
                metadata = mapOf(
                    "packageName" to "com.amazon.kindle",
                    "appName" to "Kindle"
                )
            )
        )

        // Act
        val result = aggregator.aggregate(dayMillis = 1000L, evidence = evidence, minConfidence = 0.50f)

        // Assert
        assertNotNull(result)
        // Should still aggregate confidence: 1 - (0.20 * 0.18) = 0.964
        assertEquals(0.964f, result!!.confidence, 0.001f)
        // Should sum durations
        assertEquals(50, result.durationMinutes)
        // Should include only evidence with packageName
        assertEquals(1, result.apps.map { it.packageName }.size)
        assertEquals("com.amazon.kindle", result.apps.map { it.packageName }[0])
        // Only 1 valid session (Kindle has packageName + duration > 0)
        assertEquals(1, result.sessionCount)
    }

    /**
     * Test: Distinct app packages are extracted.
     *
     * Multiple sessions from the same app should result in that package appearing
     * only once in the appPackages list.
     * Note: Same package = same confidence (from KnownApps metadata).
     */
    @Test
    fun `aggregate extracts distinct app packages`() {
        // Arrange
        val aggregator = ReadingAggregator()
        val evidence = listOf(
            Evidence(
                source = DataSource.USAGE_STATS,
                timestampMillis = 1000L,
                confidence = 0.82f,  // Kindle
                durationMinutes = 20,
                metadata = mapOf(
                    "packageName" to "com.amazon.kindle",
                    "appName" to "Kindle"
                )
            ),
            Evidence(
                source = DataSource.USAGE_STATS,
                timestampMillis = 2000L,
                confidence = 0.80f,  // Play Books
                durationMinutes = 15,
                metadata = mapOf(
                    "packageName" to "com.google.android.apps.books",
                    "appName" to "Play Books"
                )
            ),
            Evidence(
                source = DataSource.USAGE_STATS,
                timestampMillis = 3000L,
                confidence = 0.82f,  // Kindle again (same confidence)
                durationMinutes = 25,
                metadata = mapOf(
                    "packageName" to "com.amazon.kindle",
                    "appName" to "Kindle"
                )
            )
        )

        // Act
        val result = aggregator.aggregate(dayMillis = 1000L, evidence = evidence, minConfidence = 0.50f)

        // Assert
        assertNotNull(result)
        assertEquals(2, result!!.apps.size)  // 2 distinct apps
        assertTrue(result.apps.map { it.packageName }.contains("com.amazon.kindle"))
        assertTrue(result.apps.map { it.packageName }.contains("com.google.android.apps.books"))
        assertEquals(3, result.sessionCount)  // 3 valid sessions
        assertEquals(60, result.durationMinutes)  // 20 + 15 + 25
    }

    /**
     * Test: sessionCount equals number of valid sessions, not distinct apps.
     *
     * Example: User reads Play Books (session 1), then Kindle (session 2),
     * then Play Books again (session 3) → 3 sessions, 2 distinct apps.
     *
     * Valid session = durationMinutes > 0 && packageName != null
     */
    @Test
    fun `sessionCount equals total valid sessions not distinct apps`() {
        // Arrange
        val aggregator = ReadingAggregator()
        val evidence = listOf(
            // Session 1: Play Books 8am
            Evidence(
                source = DataSource.USAGE_STATS,
                timestampMillis = 1000L,
                confidence = 0.80f,  // Play Books
                durationMinutes = 20,
                metadata = mapOf(
                    "packageName" to "com.google.android.apps.books",
                    "appName" to "Play Books"
                )
            ),
            // Session 2: Kindle 10am
            Evidence(
                source = DataSource.USAGE_STATS,
                timestampMillis = 2000L,
                confidence = 0.82f,  // Kindle
                durationMinutes = 15,
                metadata = mapOf(
                    "packageName" to "com.amazon.kindle",
                    "appName" to "Kindle"
                )
            ),
            // Session 3: Play Books 2pm (same app as session 1, but NEW session)
            Evidence(
                source = DataSource.USAGE_STATS,
                timestampMillis = 3000L,
                confidence = 0.80f,  // Play Books (same confidence as session 1)
                durationMinutes = 25,
                metadata = mapOf(
                    "packageName" to "com.google.android.apps.books",
                    "appName" to "Play Books"
                )
            )
        )

        // Act
        val result = aggregator.aggregate(dayMillis = 1000L, evidence = evidence, minConfidence = 0.50f)

        // Assert
        assertNotNull(result)
        assertEquals(3, result!!.sessionCount)  // 3 valid reading sessions
        assertEquals(2, result.apps.size)  // 2 distinct apps
        assertEquals(60, result.durationMinutes)  // 20 + 15 + 25
    }

    // ============================================================
    // Weak-Only Penalty Tests
    // ============================================================

    /**
     * Test: Weak-only penalty applied when all evidence below minConfidence.
     *
     * When ALL evidence has confidence < minConfidence, a 0.15 penalty is applied.
     * This reduces confidence for cases where only weak signals exist.
     */
    @Test
    fun `weak-only penalty applied when all evidence below minConfidence`() {
        // Arrange
        val aggregator = ReadingAggregator()
        val evidence = listOf(
            Evidence(
                source = DataSource.USAGE_STATS,
                timestampMillis = 1000L,
                confidence = 0.40f,
                durationMinutes = 10,
                metadata = mapOf("packageName" to "com.example.readingapp1")
            ),
            Evidence(
                source = DataSource.USAGE_STATS,
                timestampMillis = 2000L,
                confidence = 0.35f,
                durationMinutes = 8,
                metadata = mapOf("packageName" to "com.example.readingapp2")
            )
        )

        // Act
        val result = aggregator.aggregate(dayMillis = 1000L, evidence = evidence, minConfidence = 0.50f)

        // Assert
        assertNotNull(result)
        // Combined: 1 - (0.60 * 0.65) = 1 - 0.39 = 0.61
        // After penalty: 0.61 - 0.15 = 0.46
        assertEquals(0.46f, result!!.confidence, 0.001f)
        assertEquals(false, result.occurred)
        assertEquals(ConfidenceLevel.LOW, result.confidenceLevel)
        assertEquals(2, result.sessionCount)
    }

    /**
     * Test: Weak-only penalty NOT applied when any evidence >= minConfidence.
     *
     * If at least one evidence has confidence >= minConfidence, no penalty is applied.
     */
    @Test
    fun `weak-only penalty not applied when any evidence above or equal minConfidence`() {
        // Arrange
        val aggregator = ReadingAggregator()
        val evidence = listOf(
            Evidence(
                source = DataSource.USAGE_STATS,
                timestampMillis = 1000L,
                confidence = 0.40f,
                durationMinutes = 10,
                metadata = mapOf("packageName" to "com.example.readingapp1")
            ),
            Evidence(
                source = DataSource.USAGE_STATS,
                timestampMillis = 2000L,
                confidence = 0.50f,  // >= minConfidence
                durationMinutes = 15,
                metadata = mapOf("packageName" to "com.example.readingapp2")
            )
        )

        // Act
        val result = aggregator.aggregate(dayMillis = 1000L, evidence = evidence, minConfidence = 0.50f)

        // Assert
        assertNotNull(result)
        // Combined: 1 - (0.60 * 0.50) = 0.70
        // No penalty applied
        assertEquals(0.70f, result!!.confidence, 0.001f)
        assertEquals(true, result.occurred)
        assertEquals(2, result.sessionCount)
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
        val aggregator = ReadingAggregator()
        // Create evidence that after weak-only penalty would go negative
        val evidence = listOf(
            Evidence(
                source = DataSource.USAGE_STATS,
                timestampMillis = 1000L,
                confidence = 0.10f,
                durationMinutes = 5,
                metadata = mapOf(
                    "packageName" to "com.amazon.kindle",
                    "appName" to "Kindle"
                )
            )
        )

        // Act
        val result = aggregator.aggregate(dayMillis = 1000L, evidence = evidence, minConfidence = 0.50f)

        // Assert
        assertNotNull(result)
        // Original: 0.10, After penalty: 0.10 - 0.15 = -0.05, Clamped: 0.0
        assertEquals(0.0f, result!!.confidence, 0.001f)
        assertEquals(false, result.occurred)
        assertEquals(ConfidenceLevel.LOW, result.confidenceLevel)
        assertEquals(1, result.sessionCount)
    }

    // ============================================================
    // Occurred Determination Tests
    // ============================================================

    /**
     * Test: Confidence >= minConfidence sets occurred to true.
     *
     * When confidence meets or exceeds the threshold, occurred should be true.
     */
    @Test
    fun `confidence above or equal minConfidence sets occurred to true`() {
        // Arrange
        val aggregator = ReadingAggregator()
        val evidence = listOf(
            Evidence(
                source = DataSource.USAGE_STATS,
                timestampMillis = 1000L,
                confidence = 0.51f,
                durationMinutes = 10,
                metadata = mapOf("packageName" to "com.example.readingapp")
            )
        )

        // Act
        val result = aggregator.aggregate(dayMillis = 1000L, evidence = evidence, minConfidence = 0.50f)

        // Assert
        assertNotNull(result)
        assertEquals(0.51f, result!!.confidence, 0.001f)
        assertEquals(true, result.occurred)
        assertEquals(ConfidenceLevel.MEDIUM, result.confidenceLevel)
    }

    /**
     * Test: Confidence < minConfidence sets occurred to false.
     *
     * When confidence is below the minConfidence threshold, occurred should be false.
     * Note: Single evidence with confidence < minConfidence triggers weak-only penalty.
     */
    @Test
    fun `confidence below minConfidence sets occurred to false`() {
        // Arrange
        val aggregator = ReadingAggregator()
        val evidence = listOf(
            Evidence(
                source = DataSource.USAGE_STATS,
                timestampMillis = 1000L,
                confidence = 0.49f,
                durationMinutes = 10,
                metadata = mapOf("packageName" to "com.example.readingapp")
            )
        )

        // Act
        val result = aggregator.aggregate(dayMillis = 1000L, evidence = evidence, minConfidence = 0.50f)

        // Assert
        assertNotNull(result)
        // 0.49 is < 0.50, so weak-only penalty applied: 0.49 - 0.15 = 0.34
        assertEquals(0.34f, result!!.confidence, 0.001f)
        assertEquals(false, result.occurred)
        assertEquals(ConfidenceLevel.LOW, result.confidenceLevel)
    }

    /**
     * Test: Confidence exactly 0.50 (minConfidence) sets occurred to true.
     *
     * Boundary test: exactly meeting the threshold should count as occurred.
     */
    @Test
    fun `confidence exactly 0_50 sets occurred to true`() {
        // Arrange
        val aggregator = ReadingAggregator()
        val evidence = listOf(
            Evidence(
                source = DataSource.USAGE_STATS,
                timestampMillis = 1000L,
                confidence = 0.50f,
                durationMinutes = 10,
                metadata = mapOf("packageName" to "com.example.readingapp")
            )
        )

        // Act
        val result = aggregator.aggregate(dayMillis = 1000L, evidence = evidence, minConfidence = 0.50f)

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
     * Thresholds: HIGH >= 0.75, MEDIUM >= 0.50, LOW < 0.50
     */
    @Test
    fun `confidence exactly 0_74 maps to MEDIUM level`() {
        // Arrange
        val aggregator = ReadingAggregator()
        val evidence = listOf(
            Evidence(
                source = DataSource.USAGE_STATS,
                timestampMillis = 1000L,
                confidence = 0.74f,
                durationMinutes = 10,
                metadata = mapOf("packageName" to "com.example.readingapp")
            )
        )

        // Act
        val result = aggregator.aggregate(dayMillis = 1000L, evidence = evidence, minConfidence = 0.50f)

        // Assert
        assertNotNull(result)
        assertEquals(0.74f, result!!.confidence, 0.001f)
        assertEquals(ConfidenceLevel.MEDIUM, result.confidenceLevel)
        assertEquals(true, result.occurred)  // >= 0.50
    }

    /**
     * Test: Confidence exactly 0.75 maps to HIGH level.
     *
     * This is a critical boundary test for the HIGH confidence threshold.
     */
    @Test
    fun `confidence exactly 0_75 maps to HIGH level`() {
        // Arrange
        val aggregator = ReadingAggregator()
        val evidence = listOf(
            Evidence(
                source = DataSource.USAGE_STATS,
                timestampMillis = 1000L,
                confidence = 0.75f,
                durationMinutes = 10,
                metadata = mapOf("packageName" to "com.example.readingapp")
            )
        )

        // Act
        val result = aggregator.aggregate(dayMillis = 1000L, evidence = evidence, minConfidence = 0.50f)

        // Assert
        assertNotNull(result)
        assertEquals(0.75f, result!!.confidence, 0.001f)
        assertEquals(ConfidenceLevel.HIGH, result.confidenceLevel)
        assertEquals(true, result.occurred)
    }

    /**
     * Test: Confidence below 0.50 maps to LOW level.
     */
    @Test
    fun `confidence below 0_50 maps to LOW level`() {
        // Arrange
        val aggregator = ReadingAggregator()
        val evidence = listOf(
            Evidence(
                source = DataSource.USAGE_STATS,
                timestampMillis = 1000L,
                confidence = 0.49f,
                durationMinutes = 10,
                metadata = mapOf("packageName" to "com.example.readingapp")
            )
        )

        // Act
        val result = aggregator.aggregate(dayMillis = 1000L, evidence = evidence, minConfidence = 0.50f)

        // Assert
        assertNotNull(result)
        // With penalty: 0.49 - 0.15 = 0.34
        assertEquals(0.34f, result!!.confidence, 0.001f)
        assertEquals(ConfidenceLevel.LOW, result.confidenceLevel)
    }

    // ============================================================
    // MinConfidence Parameter Tests
    // ============================================================

    /**
     * Test: minConfidence parameter affects occurred determination.
     *
     * Same evidence with different minConfidence values should produce
     * different occurred results.
     */
    @Test
    fun `aggregate uses minConfidence for occurred determination`() {
        // Arrange
        val aggregator = ReadingAggregator()
        val evidence = listOf(
            Evidence(
                source = DataSource.USAGE_STATS,
                timestampMillis = 1000L,
                confidence = 0.60f,
                durationMinutes = 10,
                metadata = mapOf("packageName" to "com.example.readingapp")
            )
        )

        // Act & Assert 1: With minConfidence = 0.50, should occur
        val result1 = aggregator.aggregate(dayMillis = 1000L, evidence = evidence, minConfidence = 0.50f)
        assertNotNull(result1)
        assertEquals(0.60f, result1!!.confidence, 0.001f)
        assertEquals(true, result1.occurred) // 0.60 >= 0.50

        // Act & Assert 2: With minConfidence = 0.70, should NOT occur
        // Note: 0.60 < 0.70, so weak-only penalty applies: 0.60 - 0.15 = 0.45
        val result2 = aggregator.aggregate(dayMillis = 1000L, evidence = evidence, minConfidence = 0.70f)
        assertNotNull(result2)
        assertEquals(0.45f, result2!!.confidence, 0.001f) // Penalty applied
        assertEquals(false, result2.occurred) // 0.45 < 0.70

        // Act & Assert 3: With minConfidence = 0.60, should occur (exactly at threshold)
        val result3 = aggregator.aggregate(dayMillis = 1000L, evidence = evidence, minConfidence = 0.60f)
        assertNotNull(result3)
        assertEquals(0.60f, result3!!.confidence, 0.001f)
        assertEquals(true, result3.occurred) // 0.60 >= 0.60
    }

    /**
     * Test: minConfidence parameter affects weak-only penalty determination.
     *
     * Evidence with confidence 0.45 should:
     * - Be considered "weak" when minConfidence = 0.50
     * - Be considered "strong" when minConfidence = 0.40
     */
    @Test
    fun `aggregate uses minConfidence for weak-only penalty determination`() {
        // Arrange
        val aggregator = ReadingAggregator()
        val evidence = listOf(
            Evidence(
                source = DataSource.USAGE_STATS,
                timestampMillis = 1000L,
                confidence = 0.45f,
                durationMinutes = 10,
                metadata = mapOf("packageName" to "com.example.readingapp")
            )
        )

        // Act & Assert 1: With minConfidence = 0.50, evidence is weak, penalty applied
        val result1 = aggregator.aggregate(dayMillis = 1000L, evidence = evidence, minConfidence = 0.50f)
        assertNotNull(result1)
        // Original confidence 0.45, penalty applied: 0.45 - 0.15 = 0.30
        assertEquals(0.30f, result1!!.confidence, 0.001f)

        // Act & Assert 2: With minConfidence = 0.40, evidence is NOT weak, no penalty
        val result2 = aggregator.aggregate(dayMillis = 1000L, evidence = evidence, minConfidence = 0.40f)
        assertNotNull(result2)
        // Original confidence 0.45, no penalty applied
        assertEquals(0.45f, result2!!.confidence, 0.001f)
    }

    // ============================================================
    // Edge Cases
    // ============================================================

    /**
     * Test: All durations null returns null durationMinutes and zero sessions.
     *
     * When all evidence has null duration, the result should have null duration
     * and sessionCount = 0.
     */
    @Test
    fun `all durations null returns null durationMinutes and zero sessions`() {
        // Arrange
        val aggregator = ReadingAggregator()
        val evidence = listOf(
            Evidence(
                source = DataSource.USAGE_STATS,
                timestampMillis = 1000L,
                confidence = 0.82f,  // Kindle
                durationMinutes = null,
                metadata = mapOf(
                    "packageName" to "com.amazon.kindle",
                    "appName" to "Kindle"
                )
            ),
            Evidence(
                source = DataSource.USAGE_STATS,
                timestampMillis = 2000L,
                confidence = 0.80f,  // Play Books
                durationMinutes = null,
                metadata = mapOf(
                    "packageName" to "com.google.android.apps.books",
                    "appName" to "Play Books"
                )
            )
        )

        // Act
        val result = aggregator.aggregate(dayMillis = 1000L, evidence = evidence, minConfidence = 0.50f)

        // Assert
        assertNotNull(result)
        assertNull(result!!.durationMinutes)
        assertNull(result.sessionCount)  // No valid sessions
    }

    /**
     * Test: All packageNames null returns empty appPackages and zero sessions.
     *
     * When all evidence has null packageName, the result should have empty appPackages
     * and sessionCount = 0.
     */
    @Test
    fun `all packageNames null returns empty appPackages and zero sessions`() {
        // Arrange
        val aggregator = ReadingAggregator()
        val evidence = listOf(
            Evidence(
                source = DataSource.USAGE_STATS,
                timestampMillis = 1000L,
                confidence = 0.82f,
                durationMinutes = 30,
                metadata = emptyMap()  // No packageName
            ),
            Evidence(
                source = DataSource.USAGE_STATS,
                timestampMillis = 2000L,
                confidence = 0.80f,
                durationMinutes = 20,
                metadata = emptyMap()  // No packageName
            )
        )

        // Act
        val result = aggregator.aggregate(dayMillis = 1000L, evidence = evidence, minConfidence = 0.50f)

        // Assert
        assertNotNull(result)
        // Confidence still aggregated: 1 - (0.18 * 0.20) = 0.964
        assertEquals(0.964f, result!!.confidence, 0.001f)
        // Duration still summed
        assertEquals(50, result.durationMinutes)
        // No valid sessions (missing packageName)
        assertNull(result.sessionCount)
        assertEquals(0, result.apps.map { it.packageName }.size)
    }

    /**
     * Test: title is always null.
     *
     * Reserved for future OAuth integration.
     */
    @Test
    fun `title is always null - reserved for future OAuth`() {
        // Arrange
        val aggregator = ReadingAggregator()
        val evidence = listOf(
            Evidence(
                source = DataSource.USAGE_STATS,
                timestampMillis = 1000L,
                confidence = 0.82f,  // Kindle
                durationMinutes = 45,
                metadata = mapOf(
                    "packageName" to "com.amazon.kindle",
                    "appName" to "Kindle"
                )
            )
        )

        // Act
        val result = aggregator.aggregate(dayMillis = 1000L, evidence = evidence, minConfidence = 0.50f)

        // Assert
        assertNotNull(result)
        assertNull(result!!.title)
    }

    /**
     * Test: Multiple sessions from same app on same day are counted separately.
     *
     * Since Android allows only one app in foreground at a time, multiple
     * Evidence items from the same app represent distinct sequential sessions.
     * They should be combined with probability formula, durations summed,
     * and ALL sessions counted.
     * Note: Same app = same confidence (0.82 for Kindle).
     */
    @Test
    fun `multiple sessions from same app counted separately`() {
        // Arrange
        val aggregator = ReadingAggregator()
        val evidence = listOf(
            // Session 1: Kindle 8am
            Evidence(
                source = DataSource.USAGE_STATS,
                timestampMillis = 1000L,
                confidence = 0.82f,  // Kindle
                durationMinutes = 30,
                metadata = mapOf(
                    "packageName" to "com.amazon.kindle",
                    "appName" to "Kindle"
                )
            ),
            // Session 2: Kindle 2pm
            Evidence(
                source = DataSource.USAGE_STATS,
                timestampMillis = 2000L,
                confidence = 0.82f,  // Kindle (same confidence)
                durationMinutes = 20,
                metadata = mapOf(
                    "packageName" to "com.amazon.kindle",
                    "appName" to "Kindle"
                )
            )
        )

        // Act
        val result = aggregator.aggregate(dayMillis = 1000L, evidence = evidence, minConfidence = 0.50f)

        // Assert
        assertNotNull(result)
        // Combined confidence: 1 - (0.18 * 0.18) = 0.9676
        assertEquals(0.9676f, result!!.confidence, 0.001f)
        // Durations summed
        assertEquals(50, result.durationMinutes)  // 30 + 20
        // Only one distinct app
        assertEquals(1, result.apps.map { it.packageName }.size)
        // Two distinct sessions
        assertEquals(2, result.sessionCount)
    }
}
