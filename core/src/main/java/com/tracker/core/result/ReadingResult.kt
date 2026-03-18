package com.tracker.core.result

import com.tracker.core.types.ConfidenceLevel
import com.tracker.core.types.DataSource

/**
 * Result for reading habit detection.
 *
 * @property occurred Whether reading was detected for this day
 * @property confidence Combined confidence score (0.0 to 1.0)
 * @property confidenceLevel Categorical confidence level
 * @property source Primary data source
 * @property durationMinutes Total time spent reading across all apps (nullable)
 * @property sessionCount Number of distinct reading sessions (where duration > 0 and packageName exists)
 * @property apps List of apps that contributed to this result
// * @property title The book title being read (nullable, reserved for future OAuth integration)
// *
// * Note: title is currently always null. It will be populated when OAuth integration
// * is added (e.g., Google Play Books API, Kindle API) to provide richer reading context.
 */
data class ReadingResult(
    override val occurred: Boolean,
    override val source: DataSource,
    override val confidence: Float,
    override val confidenceLevel: ConfidenceLevel,
    override val timeRange: TimeRange,
    val durationMinutes: Int?,
    val sessionCount: Int?,
    val apps: List<AppInfo> = emptyList()
) : HabitResult()
