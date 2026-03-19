package com.tracker.core.result

import com.tracker.core.types.ConfidenceLevel
import com.tracker.core.types.DataSource

/**
 * Result for language learning habit detection.
 *
 * @property occurred Whether language learning was detected for this day
 * @property source Primary data source
 * @property confidence Combined confidence score
 * @property confidenceLevel Categorical confidence level
 * @property timeRange
 * @property durationMinutes Total time spent in language learning apps
 * @property sessionCount Number of distinct learning sessions
 * @property apps List of apps that contributed to this result
 */

data class LanguageLearningResult(
    override val occurred: Boolean,
    override val source: DataSource,
    override val confidence: Float,
    override val confidenceLevel: ConfidenceLevel,
    override val timeRange: TimeRange,
    val durationMinutes: Int,
    val sessionCount: Int,
    val apps: List<AppInfo> = emptyList()
) : HabitResult()
