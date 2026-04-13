package com.tracker.core.result

import com.tracker.core.types.ConfidenceLevel
import com.tracker.core.types.DataSource

/**
 * Result for language learning habit detection.
 *
 * @property source Primary data source
 * @property confidence Combined confidence score
 * @property confidenceLevel Categorical confidence level
 * @property timeRange The queried time range
 * @property durationMinutes Total time spent in language learning apps across all sessions
 * @property sessions Individual foreground sessions, sorted by [UsageSession.startTime] ascending.
 * See [UsageSession] for deduplication key guidance when storing sessions locally.
 */
data class LanguageLearningResult(
    override val source: DataSource,
    override val confidence: Float,
    override val confidenceLevel: ConfidenceLevel,
    override val timeRange: TimeRange,
    val durationMinutes: Int,
    val sessions: List<UsageSession> = emptyList()
) : HabitResult()
