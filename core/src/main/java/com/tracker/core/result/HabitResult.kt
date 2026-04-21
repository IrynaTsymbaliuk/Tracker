package com.tracker.core.result

import com.tracker.core.types.ConfidenceLevel
import com.tracker.core.types.DataSource

/**
 * Base class for all habit detection results.
 *
 * @property confidence Combined confidence score (0.0 to 1.0)
 * @property confidenceLevel Categorical representation of confidence
 * @property sources The data sources that contributed to this result. A result fused from
 * multiple collectors (e.g. HealthConnect mindfulness sessions + UsageStats app sessions)
 * lists all contributing sources here; single-source results still use a one-element list.
 * @property timeRange The queried time range
 */
sealed class HabitResult {
    abstract val confidence: Float
    abstract val confidenceLevel: ConfidenceLevel
    abstract val sources: List<DataSource>
    abstract val timeRange: TimeRange
}
