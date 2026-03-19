package com.tracker.core.provider

import com.tracker.core.collector.LetterboxdCollector
import com.tracker.core.result.MovieInfo
import com.tracker.core.result.MovieWatchingResult
import com.tracker.core.result.TimeRange
import com.tracker.core.result.toConfidenceLevel
import com.tracker.core.types.DataSource

class MovieWatchingProvider internal constructor(
    private val letterboxdCollector: LetterboxdCollector,
    private val letterboxdUsername: String?
) : MetricProvider<MovieWatchingResult> {

    override suspend fun query(
        fromMillis: Long,
        toMillis: Long,
        minConfidence: Float
    ): MovieWatchingResult? {
        if (letterboxdUsername.isNullOrBlank()) return null

        val evidenceList = letterboxdCollector.collect(fromMillis, toMillis, letterboxdUsername)?.ifEmpty { return null } ?: return null

        val totalCounter = evidenceList.sumOf { it.counter }

        val movies = evidenceList.mapNotNull { ev ->
            val title = ev.metadata["title"] as? String
            val publishedDate = ev.metadata["publishedDate"] as? Long
            val watchedDate = ev.metadata["watchedDate"] as? Long
            if (title != null && publishedDate != null && watchedDate != null) {
                MovieInfo(title, publishedDate, watchedDate)
            } else {
                null
            }
        }

        val confidence = evidenceList.first().confidence

        return MovieWatchingResult(
            occurred = totalCounter > 0,
            source = DataSource.LETTERBOXD_RSS,
            confidence = confidence,
            confidenceLevel = confidence.toConfidenceLevel(),
            timeRange = TimeRange(fromMillis, toMillis),
            count = totalCounter,
            movies = movies
        )

    }
}
