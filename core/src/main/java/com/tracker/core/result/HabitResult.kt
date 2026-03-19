package com.tracker.core.result

import com.tracker.core.types.ConfidenceLevel
import com.tracker.core.types.DataSource

/**
 * Base class for all habit detection results.
 *
 * @property occurred Whether the habit was detected for the queried time range
 * @property confidence Combined confidence score (0.0 to 1.0)
 * @property confidenceLevel Categorical representation of confidence
 * @property source The primary data source
 * @property timeRange The queried time range
 */
sealed class HabitResult {
    abstract val occurred: Boolean
    abstract val confidence: Float
    abstract val confidenceLevel: ConfidenceLevel
    abstract val source: DataSource
    abstract val timeRange: TimeRange
}
