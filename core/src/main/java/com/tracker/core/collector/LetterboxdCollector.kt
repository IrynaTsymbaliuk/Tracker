package com.tracker.core.collector

import android.content.Context
import android.util.Log
import com.tracker.core.model.CounterEvidence
import com.tracker.core.result.MovieInfo
import com.tracker.core.types.DataSource
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory
import java.io.StringReader
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

/**
 * Collector for gathering movie watching evidence from Letterboxd RSS feeds.
 *
 * All public methods are safe to call from any thread, including the main thread.
 * Network operations are automatically performed on the IO dispatcher.
 *
 * **Required Permissions:**
 * - `android.permission.INTERNET` - Required for network requests
 *
 * **Optional Permissions:**
 * - `android.permission.ACCESS_NETWORK_STATE` - Only needed if using AndroidNetworkConnectivityChecker
 *
 * **Network Connectivity Checking:**
 * By default, the collector does NOT check network connectivity before making requests.
 * Instead, it relies on retry logic with exponential backoff to handle network failures.
 * This works well for most use cases and doesn't require additional permissions.
 *
 * You can optionally enable pre-flight connectivity checking by injecting AndroidNetworkConnectivityChecker
 * (requires ACCESS_NETWORK_STATE permission in your app's manifest).
 *
 * **Example usage:**
 * ```
 * // Basic usage (recommended - no pre-check, uses retry logic)
 * val collector = LetterboxdCollector()
 * val evidence = collector.collect(fromMillis, toMillis, "username")
 *
 * // Access type-safe metadata
 * evidence.forEach { counterEvidence ->
 *     val metadata = LetterboxdMetadata.fromMap(counterEvidence.metadata)
 *     if (metadata != null) {
 *         println("Movie: ${metadata.title}")
 *         println("Watched: ${Date(metadata.watchedDate)}")
 *     }
 * }
 *
 * // With network connectivity pre-checking (requires ACCESS_NETWORK_STATE permission)
 * val fetcher = HttpRssFetcher(
 *     networkChecker = AndroidNetworkConnectivityChecker(context)
 * )
 * val collector = LetterboxdCollector(rssFetcher = fetcher)
 *
 * // With custom retry configuration
 * val fetcher = HttpRssFetcher(
 *     maxRetries = 5,
 *     retryDelayMs = 2000
 * )
 * val collector = LetterboxdCollector(rssFetcher = fetcher)
 * ```
 *
 * @property dispatcher The coroutine dispatcher to use for IO operations (injectable for testing)
 * @property rssFetcher The RSS fetcher implementation (injectable for testing/customization)
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

    private val watchedDatePattern = Regex("Watched on [A-Za-z]+, ([A-Za-z]+ \\d+, \\d{4})")

    /**
     * Collects movie watching evidence from Letterboxd RSS feed within the specified time range.
     *
     * This function is safe to call from any thread, including the main thread.
     * Network operations are automatically performed on IO dispatcher.
     *
     * The operation is cancellable - if the coroutine is cancelled, the operation will stop gracefully.
     *
     * @param fromMillis Start of time range in milliseconds (inclusive)
     * @param toMillis End of time range in milliseconds (inclusive)
     * @param id Letterboxd username (e.g., "johndoe")
     * @return List of evidence for movies watched/published in the time range
     * @throws InvalidLetterboxdIdException if username is empty or blank
     * @throws NetworkException if network request fails (e.g., timeout, no connection, HTTP error)
     * @throws RssParseException if RSS feed parsing fails (e.g., malformed XML, unexpected format)
     * @throws kotlinx.coroutines.CancellationException if the coroutine is cancelled
     */
    suspend fun collect(
        fromMillis: Long,
        toMillis: Long,
        letterboxdUsername: String
    ): List<CounterEvidence> = withContext(dispatcher) {
        Log.d(TAG, "Starting collection for user: $letterboxdUsername, range: $fromMillis-$toMillis")

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

    private fun checkId(id: String) {
        if (id.isBlank()) {
            Log.e(TAG, "Invalid Letterboxd username: empty or blank")
            throw InvalidLetterboxdIdException("Letterboxd username cannot be empty")
        }
    }

    internal fun parseRssFeed(rssContent: String): List<MovieInfo> {
        val movies = mutableListOf<MovieInfo>()

        val rssDateFormat =
            SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss Z", Locale.getDefault()).apply {
                timeZone = TimeZone.getTimeZone("UTC")
            }

        try {
            val factory = XmlPullParserFactory.newInstance()
            val parser = factory.newPullParser()
            parser.setInput(StringReader(rssContent))

            var eventType = parser.eventType
            var currentTitle: String? = null
            var currentPubDate: Long? = null
            var currentDescription: String? = null
            var inItem = false

            while (eventType != XmlPullParser.END_DOCUMENT) {
                when (eventType) {
                    XmlPullParser.START_TAG -> {
                        when (parser.name) {
                            "item" -> {
                                inItem = true
                                currentTitle = null
                                currentPubDate = null
                                currentDescription = null
                            }

                            "title" -> {
                                if (inItem) {
                                    currentTitle = parser.nextText()
                                }
                            }

                            "pubDate" -> {
                                if (inItem) {
                                    val dateText = parser.nextText()
                                    currentPubDate = try {
                                        rssDateFormat.parse(dateText)?.time
                                    } catch (e: Exception) {
                                        Log.w(TAG, "Failed to parse pubDate: $dateText", e)
                                        null
                                    }
                                }
                            }

                            "description" -> {
                                if (inItem) {
                                    currentDescription = parser.nextText()
                                }
                            }
                        }
                    }

                    XmlPullParser.END_TAG -> {
                        if (parser.name == "item" && inItem) {
                            if (currentTitle != null && currentPubDate != null) {
                                val watchedDate =
                                    extractWatchedDate(currentDescription) ?: currentPubDate
                                movies.add(
                                    MovieInfo(
                                        title = currentTitle,
                                        publishedDate = currentPubDate,
                                        watchedDate = watchedDate
                                    )
                                )
                            }
                            inItem = false
                        }
                    }
                }
                eventType = parser.next()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to parse RSS feed: ${e.message}", e)
            throw RssParseException("Failed to parse RSS feed: ${e.message}", e)
        }

        return movies
    }

    internal fun extractWatchedDate(description: String?): Long? {
        if (description == null) return null

        val match = watchedDatePattern.find(description) ?: return null
        val dateString = match.groupValues[1] // "Jan 15, 2024"

        val watchedDateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }

        return try {
            watchedDateFormat.parse(dateString)?.time
        } catch (e: Exception) {
            Log.w(TAG, "Failed to parse watched date: $dateString", e)
            null
        }
    }
}

/**
 * Exception thrown when a Letterboxd username is invalid.
 */
class InvalidLetterboxdIdException(message: String) : Exception(message)

/**
 * Exception thrown when network request fails.
 */
class NetworkException(message: String, cause: Throwable? = null) : Exception(message, cause)

/**
 * Exception thrown when RSS feed parsing fails.
 */
class RssParseException(message: String, cause: Throwable? = null) : Exception(message, cause)
