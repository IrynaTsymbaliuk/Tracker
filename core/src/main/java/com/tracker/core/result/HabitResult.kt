package com.tracker.core.result

import com.tracker.core.types.ConfidenceLevel
import com.tracker.core.types.DataSource

/**
 * Base class for all habit detection results.
 *
 * @property occurred Whether the habit was detected for this day
 * @property confidence Combined confidence score (0.0 to 1.0)
 * @property confidenceLevel Categorical representation of confidence
 * @property source The primary/winning data source
 */

sealed class HabitResult {
    abstract val occurred: Boolean
    abstract val confidence: Float
    abstract val confidenceLevel: ConfidenceLevel
    abstract val source: DataSource
    abstract val timeRange: TimeRange
}

/**
 * Converts a confidence score to a ConfidenceLevel.
 */
fun Float.toConfidenceLevel(): ConfidenceLevel = when {
    this >= 0.75f -> ConfidenceLevel.HIGH
    this >= 0.50f -> ConfidenceLevel.MEDIUM
    else -> ConfidenceLevel.LOW
}

/**
 * Determines if a habit occurred based on confidence threshold.
 *
 * @param threshold Minimum confidence threshold (default: 0.50)
 */
fun Float.toOccurred(threshold: Float = 0.50f): Boolean = this >= threshold
