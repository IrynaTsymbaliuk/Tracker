package com.tracker.core.result

import com.tracker.core.types.ConfidenceLevel
import com.tracker.core.types.DataSource

/**
 * Result for reading habit detection.
 *
 * @property occurred Whether reading was detected for this day
 * @property source Primary data source
 * @property confidence Combined confidence score (0.0 to 1.0)
 * @property confidenceLevel Categorical confidence level
 * @property timeRange The queried time range
 * @property durationMinutes Total time spent reading across all apps
 * @property sessionCount Number of distinct reading sessions
 * @property apps List of apps that contributed to this result
 */
data class ReadingResult(
    override val occurred: Boolean,
    override val source: DataSource,
    override val confidence: Float,
    override val confidenceLevel: ConfidenceLevel,
    override val timeRange: TimeRange,
    val durationMinutes: Int,
    val sessionCount: Int,
    val apps: List<AppInfo> = emptyList()
) : HabitResult()
