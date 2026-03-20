package com.tracker.core.collector

import android.util.Log
import com.tracker.core.model.CounterEvidence
import com.tracker.core.result.MovieInfo
import com.tracker.core.types.DataSource
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext

/**
 * Collects movie watching evidence from Letterboxd RSS feeds.
 *
 * Safe to call from any thread. Uses retry logic with exponential backoff for network failures.
 *
 * **Permissions:**
 * - `android.permission.INTERNET` (required)
 * - `android.permission.ACCESS_NETWORK_STATE` (optional, only if using AndroidNetworkConnectivityChecker)
 *
 * **Example:**
 * ```
 * val collector = LetterboxdCollector(rssFetcher = HttpRssFetcher(networkChecker = checker))
 * val evidence = collector.collect(fromMillis, toMillis, "username")
 * val metadata = LetterboxdMetadata.fromMap(evidence.first().metadata)
 * println("Movie: ${metadata?.title}")
 * ```
 */
class LetterboxdCollector(
    private val dispatcher: CoroutineDispatcher = Dispatchers.IO,
    private val rssFetcher: RssFetcher
) {

    companion object {
        private const val TAG = "LetterboxdCollector"
        private const val BASE_URL = "https://letterboxd.com"
        private const val LETTERBOXD_CONFIDENCE = 0.95f
    }

    /**
     * Collects movie watching evidence from Letterboxd RSS feed.
     *
     * @param fromMillis Start of time range (inclusive, milliseconds)
     * @param toMillis End of time range (inclusive, milliseconds)
     * @param letterboxdUsername Letterboxd username
     * @return List of movie evidence in the time range
     * @throws InvalidLetterboxdIdException if username is blank
     * @throws NetworkException if network request fails
     * @throws RssParseException if RSS parsing fails
     */
    suspend fun collect(
        fromMillis: Long,
        toMillis: Long,
        letterboxdUsername: String?
    ): List<CounterEvidence> = withContext(dispatcher) {
        Log.d(
            TAG,
            "Starting collection for user: $letterboxdUsername, range: $fromMillis-$toMillis"
        )

        checkId(letterboxdUsername)

        val rssUrl = "$BASE_URL/$letterboxdUsername/rss/"
        Log.d(TAG, "Fetching RSS feed from: $rssUrl")

        // Check for cancellation before expensive network call
        coroutineContext.ensureActive()

        val rssContent = rssFetcher.fetch(rssUrl)
        Log.d(TAG, "RSS feed fetched successfully, size: ${rssContent.length} bytes")

        // Check for cancellation before parsing
        coroutineContext.ensureActive()

        val allMovies = parseRssFeed(rssContent)
        Log.d(TAG, "Parsed ${allMovies.size} movies from RSS feed")

        val filtered = allMovies.filter { movie ->
            movie.watchedDate in fromMillis..toMillis ||
                    movie.publishedDate in fromMillis..toMillis
        }

        Log.d(TAG, "Filtered to ${filtered.size} movies in time range")

        filtered.map { movie ->
            val metadata = LetterboxdMetadata(
                title = movie.title,
                publishedDate = movie.publishedDate,
                watchedDate = movie.watchedDate
            )

            CounterEvidence(
                source = DataSource.LETTERBOXD_RSS,
                confidence = LETTERBOXD_CONFIDENCE,
                metadata = metadata.toMap(),
                counter = 1
            )
        }
    }

    private fun checkId(id: String?) {
        if (id.isNullOrBlank()) {
            Log.e(TAG, "Invalid Letterboxd username: empty or blank")
            throw InvalidLetterboxdIdException("Letterboxd username cannot be empty")
        }
    }

    internal fun parseRssFeed(rssContent: String): List<MovieInfo> {
        return try {
            RssParser().parse(rssContent)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to parse RSS feed: ${e.message}", e)
            throw RssParseException("Failed to parse RSS feed: ${e.message}", e)
        }
    }

}

/** Thrown when Letterboxd username is invalid. */
class InvalidLetterboxdIdException(message: String) : Exception(message)

/** Thrown when network request fails. */
class NetworkException(message: String, cause: Throwable? = null) : Exception(message, cause)

/** Thrown when RSS feed parsing fails. */
class RssParseException(message: String, cause: Throwable? = null) : Exception(message, cause)
