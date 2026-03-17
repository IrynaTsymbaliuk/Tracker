package com.tracker.core.result

import com.tracker.core.types.ConfidenceLevel
import com.tracker.core.types.DataSource

/**
 * Result for language learning habit detection.
 *
 * @property occurred Whether language learning was detected for this day
 * @property confidence Combined confidence score
 * @property confidenceLevel Categorical confidence level
 * @property durationMinutes Total time spent in language learning apps (nullable)
 * @property sessionCount Number of distinct learning sessions (nullable)
 * @property source Primary data source
 * @property language The language being learned (nullable, future enhancement)
 * @property apps List of apps that contributed to this result
 */
data class LanguageLearningResult(
    override val occurred: Boolean,
    override val confidence: Float,
    override val confidenceLevel: ConfidenceLevel,
    override val durationMinutes: Int?,
    val sessionCount: Int? = null,
    override val source: DataSource,
    val language: String? = null,
    val apps: List<AppInfo> = emptyList()
) : HabitResult(
    occurred = occurred,
    confidence = confidence,
    confidenceLevel = confidenceLevel,
    durationMinutes = durationMinutes,
    source = source,
    count = sessionCount
)
