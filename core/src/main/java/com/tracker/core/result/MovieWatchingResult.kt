package com.tracker.core.result

import com.tracker.core.types.DataSource

/**
 * Result for movie watching habit detection via Letterboxd RSS feed.
 *
 * @property sources Data sources that contributed to this result (always contains
 * [DataSource.LETTERBOXD_RSS] for this habit)
 * @property timeRange The queried time range
 * @property sessions Movie-watching sessions in the time range, each with a title and
 * timestamps (see [MovieSession]), sorted by [MovieSession.watchedDate] ascending.
 *
 * `null` return value means no data available (username not set, feed unavailable, or no films in range).
 */
data class MovieWatchingResult(
    override val sources: List<DataSource>,
    override val timeRange: TimeRange,
    val sessions: List<MovieSession> = emptyList()
) : HabitResult() {
    /** Number of movies watched in the time range. */
    val count: Int get() = sessions.size
}
