package com.tracker.core.result

import com.tracker.core.types.ConfidenceLevel
import com.tracker.core.types.DataSource

/**
 * Result for reading habit detection.
 *
 * @property occurred Whether reading was detected for the queried time range
 * @property source Primary data source
 * @property confidence Combined confidence score (0.0 to 1.0)
 * @property confidenceLevel Categorical confidence level
 * @property timeRange The queried time range
 * @property durationMinutes Total time spent reading across all sessions
 * @property sessions Individual foreground sessions, sorted by [UsageSession.startTime] ascending.
 * See [UsageSession] for deduplication key guidance when storing sessions locally.
 */
data class ReadingResult(
    override val occurred: Boolean,
    override val source: DataSource,
    override val confidence: Float,
    override val confidenceLevel: ConfidenceLevel,
    override val timeRange: TimeRange,
    val durationMinutes: Int,
    val sessions: List<UsageSession> = emptyList()
) : HabitResult()
