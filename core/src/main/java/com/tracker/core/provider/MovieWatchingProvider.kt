package com.tracker.core.provider

import com.tracker.core.collector.LetterboxdCollector
import com.tracker.core.result.MovieWatchingResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class MovieWatchingProvider internal constructor(
    private val letterboxdCollector: LetterboxdCollector,
    private val letterboxdUsername: String?
) : MetricProvider<MovieWatchingResult> {

    override suspend fun query(
        fromMillis: Long,
        toMillis: Long,
        minConfidence: Float
    ): MovieWatchingResult? {
        // If no username is provided, return null (no data available)
        if (letterboxdUsername.isNullOrBlank()) {
            return null
        }

        // LetterboxdCollector makes blocking network calls, so run on IO dispatcher
        return withContext(Dispatchers.IO) {
            letterboxdCollector.collect(fromMillis, toMillis, letterboxdUsername)
        }
    }
}
