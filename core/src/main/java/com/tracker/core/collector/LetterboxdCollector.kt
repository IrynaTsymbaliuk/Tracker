package com.tracker.core.collector

import android.util.Log
import com.tracker.core.model.CounterEvidence
import com.tracker.core.result.MovieInfo
import com.tracker.core.types.DataSource
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory
import java.io.StringReader
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

class LetterboxdCollector(
    private val dispatcher: CoroutineDispatcher = Dispatchers.IO,
    private val rssFetcher: RssFetcher = HttpRssFetcher()
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
     * @param fromMillis Start of time range in milliseconds (inclusive)
     * @param toMillis End of time range in milliseconds (inclusive)
     * @param id Letterboxd username (e.g., "johndoe")
     * @return List of evidence for movies watched/published in the time range
     * @throws InvalidLetterboxdIdException if username is empty or blank
     * @throws NetworkException if network request fails (e.g., timeout, no connection, HTTP error)
     * @throws RssParseException if RSS feed parsing fails (e.g., malformed XML, unexpected format)
     */
    suspend fun collect(
        fromMillis: Long,
        toMillis: Long,
        id: String
    ): List<CounterEvidence> = withContext(dispatcher) {
        Log.d(TAG, "Starting collection for user: $id, range: $fromMillis-$toMillis")

        checkId(id)

        val rssUrl = "$BASE_URL/$id/rss/"
        Log.d(TAG, "Fetching RSS feed from: $rssUrl")

        val rssContent = rssFetcher.fetch(rssUrl)
        Log.d(TAG, "RSS feed fetched successfully, size: ${rssContent.length} bytes")

        val allMovies = parseRssFeed(rssContent)
        Log.d(TAG, "Parsed ${allMovies.size} movies from RSS feed")

        val filtered = allMovies.filter { movie ->
            movie.watchedDate in fromMillis..toMillis ||
                    movie.publishedDate in fromMillis..toMillis
        }

        Log.d(TAG, "Filtered to ${filtered.size} movies in time range")

        filtered.map { movie ->
            CounterEvidence(
                source = DataSource.LETTERBOXD_RSS,
                confidence = LETTERBOXD_CONFIDENCE,
                metadata = mapOf(
                    "title" to movie.title,
                    "publishedDate" to movie.publishedDate,
                    "watchedDate" to movie.watchedDate
                ),
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

        val rssDateFormat = SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss Z", Locale.getDefault()).apply {
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
