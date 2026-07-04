package com.tracker.core.result

import com.tracker.core.types.DataSource

/**
 * Result for reading habit detection.
 *
 * @property sources Data sources that contributed to this result (always contains
 * [DataSource.USAGE_STATS] for this habit)
 * @property timeRange The queried time range
 * @property durationMinutes Total time spent reading across all sessions
 * @property sessions Individual foreground sessions, sorted by [UsageSession.startTime] ascending.
 * See [UsageSession] for deduplication key guidance when storing sessions locally.
 */
data class ReadingResult(
    override val sources: List<DataSource>,
    override val timeRange: TimeRange,
    val durationMinutes: Int,
    val sessions: List<UsageSession> = emptyList()
) : HabitResult()
