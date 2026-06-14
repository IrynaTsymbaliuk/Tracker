package com.tracker.core.provider

import com.tracker.core.collector.InvalidLetterboxdIdException
import com.tracker.core.collector.LetterboxdCollector
import com.tracker.core.collector.LetterboxdMetadata
import com.tracker.core.result.MovieSession
import com.tracker.core.result.MovieWatchingResult
import com.tracker.core.result.TimeRange
import com.tracker.core.result.toConfidenceLevel
import com.tracker.core.types.DataSource

class MovieWatchingProvider internal constructor(
    private val letterboxdCollector: LetterboxdCollector
) : MetricProvider<MovieWatchingResult> {

    private var letterboxdUsername: String? = null

    fun setUsername(username: String?) {
        letterboxdUsername = username
    }

    override suspend fun query(
        fromMillis: Long,
        toMillis: Long,
        minConfidence: Float
    ): MovieWatchingResult? {
        if (letterboxdUsername.isNullOrBlank()) throw InvalidLetterboxdIdException("Letterboxd username is not set")

        val evidenceList = letterboxdCollector.collect(fromMillis, toMillis, letterboxdUsername).ifEmpty { return null }

        val sessions = evidenceList.mapNotNull { ev ->
            val metadata = LetterboxdMetadata.fromMap(ev.metadata) ?: return@mapNotNull null
            MovieSession(
                title = metadata.title,
                year = metadata.year,
                publishedDate = metadata.publishedDate,
                watchedDate = metadata.watchedDate,
                tmdbId = metadata.tmdbId,
                rating = metadata.rating,
                review = metadata.review,
                posterUrl = metadata.posterUrl,
                isRewatch = metadata.isRewatch,
                isLiked = metadata.isLiked
            )
        }.sortedBy { it.watchedDate }

        val confidence = evidenceList.first().confidence

        return MovieWatchingResult(
            sources = listOf(DataSource.LETTERBOXD_RSS),
            confidence = confidence,
            confidenceLevel = confidence.toConfidenceLevel(),
            timeRange = TimeRange(fromMillis, toMillis),
            sessions = sessions
        )

    }
}
