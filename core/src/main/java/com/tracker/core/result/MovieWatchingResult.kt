package com.tracker.core.result

import com.tracker.core.types.ConfidenceLevel
import com.tracker.core.types.DataSource

/**
 * Result for movie watching habit detection via Letterboxd RSS feed.
 *
 * @property occurred Whether movie watching was detected for this day (true if count > 0)
 * @property confidence Confidence score from the data source (0.0 to 1.0, typically 0.95 for Letterboxd RSS)
 * @property confidenceLevel Categorical confidence level
 * @property source Data source (LETTERBOXD_RSS)
 * @property count Number of movies watched on this day (nullable)
 * @property movies List of movies watched on this day with title and dates
 *
 * **Important null distinction:**
 * - `MovieWatchingResult = null` → no data available (username not provided, feed unavailable, or network error)
 * - `MovieWatchingResult(count = 0, occurred = false)` → data collected successfully but no films watched that day
 *
 * Note: durationMinutes is always null for movie watching as the RSS feed only provides watch dates, not durations.
 * The count field represents the number of discrete movie watch events for the day.
 */
data class MovieWatchingResult(
    override val occurred: Boolean,
    override val source: DataSource,
    override val confidence: Float,
    override val confidenceLevel: ConfidenceLevel,
    override val timeRange: TimeRange,
    val count: Int?,
    val movies: List<MovieInfo> = emptyList()
) : HabitResult()
