package com.tracker.core.result

import com.tracker.core.types.ConfidenceLevel
import com.tracker.core.types.DataSource

/**
 * Result for movie watching habit detection via Letterboxd RSS feed.
 *
 * @property occurred Whether movie watching was detected for this day (true if count > 0)
 * @property source Data source (LETTERBOXD_RSS)
 * @property confidence Confidence score from the data source (0.0 to 1.0, typically 0.95 for Letterboxd RSS)
 * @property confidenceLevel Categorical confidence level
 * @property timeRange The queried time range
 * @property count Number of movies watched in the time range
 * @property movies List of movies watched in the time range with title and dates
 *
 * **Important null distinction:**
 * - `MovieWatchingResult = null` → no data available (username not provided, feed unavailable, or network error)
 * - `MovieWatchingResult(count = 0, occurred = false)` → feed was read successfully but no films were logged in this range
 */
data class MovieWatchingResult(
    override val occurred: Boolean,
    override val source: DataSource,
    override val confidence: Float,
    override val confidenceLevel: ConfidenceLevel,
    override val timeRange: TimeRange,
    val count: Int,
    val movies: List<MovieInfo> = emptyList()
) : HabitResult()
