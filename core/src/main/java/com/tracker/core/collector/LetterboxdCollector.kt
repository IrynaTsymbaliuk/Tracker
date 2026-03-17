package com.tracker.core.collector

import android.content.Context
import com.tracker.core.result.MovieInfo
import com.tracker.core.result.MovieWatchingResult
import com.tracker.core.result.toConfidenceLevel
import com.tracker.core.types.DataSource
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory
import java.io.StringReader
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

/**
 * Collects movie watching data from Letterboxd RSS feeds.
 *
 * Parses a user's public Letterboxd RSS feed to extract watched/logged movies
 * within a specified time range.
 *
 * RSS Feed Format: https://letterboxd.com/{username}/rss/
 */
class LetterboxdCollector(private val context: Context) {

    companion object {
        private const val BASE_URL = "https://letterboxd.com"
        private const val LETTERBOXD_CONFIDENCE = 0.95f // High confidence - direct user logging
        private const val CONNECTION_TIMEOUT_MS = 10000
        private const val READ_TIMEOUT_MS = 10000

        // Letterboxd RSS date format: "Mon, 15 Jan 2024 12:34:56 +0000"
        private val RSS_DATE_FORMAT = SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss Z", Locale.US).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }

        // Letterboxd watched date format in description: "Watched on Monday, Jan 15, 2024"
        private val WATCHED_DATE_PATTERN = Regex("Watched on [A-Za-z]+, ([A-Za-z]+ \\d+, \\d{4})")
        private val WATCHED_DATE_FORMAT = SimpleDateFormat("MMM dd, yyyy", Locale.US).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }
    }

    /**
     * Collects movie watching data from Letterboxd RSS feed.
     *
     * @param fromMillis Start time in milliseconds since epoch (inclusive)
     * @param toMillis End time in milliseconds since epoch (inclusive)
     * @param id Letterboxd username
     * @return MovieWatchingResult with movies watched in the time range, or null if error
     * @throws InvalidLetterboxdIdException if the username is invalid
     * @throws NetworkException if network request fails
     */
    fun collect(
        fromMillis: Long,
        toMillis: Long,
        id: String
    ): MovieWatchingResult? {
        checkId(id)

        return try {
            val rssUrl = "$BASE_URL/$id/rss/"
            val rssContent = fetchRssFeed(rssUrl)
            val allMovies = parseRssFeed(rssContent)

            // Filter movies by date range
            val moviesInRange = allMovies.filter { movie ->
                movie.watchedDate in fromMillis..toMillis
            }

            if (moviesInRange.isEmpty()) {
                // No movies in this time range
                MovieWatchingResult(
                    occurred = false,
                    confidence = LETTERBOXD_CONFIDENCE,
                    confidenceLevel = LETTERBOXD_CONFIDENCE.toConfidenceLevel(),
                    source = DataSource.LETTERBOXD_RSS,
                    count = 0,
                    movies = emptyList()
                )
            } else {
                MovieWatchingResult(
                    occurred = true,
                    confidence = LETTERBOXD_CONFIDENCE,
                    confidenceLevel = LETTERBOXD_CONFIDENCE.toConfidenceLevel(),
                    source = DataSource.LETTERBOXD_RSS,
                    count = moviesInRange.size,
                    movies = moviesInRange
                )
            }
        } catch (e: Exception) {
            // Return null on error (network issues, parsing errors, etc.)
            null
        }
    }

    /**
     * Validates a Letterboxd username.
     *
     * Valid usernames:
     * - Must not be empty or blank
     *
     * @param id The Letterboxd username to validate
     * @throws InvalidLetterboxdIdException if the username is invalid
     */
    private fun checkId(id: String) {
        if (id.isEmpty() || id.isBlank()) {
            throw InvalidLetterboxdIdException("Letterboxd username cannot be empty")
        }
    }

    /**
     * Fetches RSS feed content from the given URL.
     *
     * @param urlString The RSS feed URL
     * @return RSS feed content as string
     * @throws NetworkException if the request fails
     */
    private fun fetchRssFeed(urlString: String): String {
        var connection: HttpURLConnection? = null
        try {
            val url = URL(urlString)
            connection = url.openConnection() as HttpURLConnection
            connection.connectTimeout = CONNECTION_TIMEOUT_MS
            connection.readTimeout = READ_TIMEOUT_MS
            connection.requestMethod = "GET"
            connection.setRequestProperty("User-Agent", "TrackerApp/1.0")

            val responseCode = connection.responseCode
            if (responseCode != HttpURLConnection.HTTP_OK) {
                throw NetworkException("HTTP error code: $responseCode")
            }

            return connection.inputStream.bufferedReader().use { it.readText() }
        } catch (e: Exception) {
            throw NetworkException("Failed to fetch RSS feed: ${e.message}", e)
        } finally {
            connection?.disconnect()
        }
    }

    /**
     * Parses Letterboxd RSS feed XML and extracts movie information.
     *
     * RSS feed structure:
     * <rss>
     *   <channel>
     *     <item>
     *       <title>Film Title</title>
     *       <pubDate>Mon, 15 Jan 2024 12:34:56 +0000</pubDate>
     *       <description>Watched on Monday, Jan 15, 2024...</description>
     *     </item>
     *   </channel>
     * </rss>
     *
     * @param rssContent RSS feed XML content
     * @return List of MovieInfo objects
     */
    private fun parseRssFeed(rssContent: String): List<MovieInfo> {
        val movies = mutableListOf<MovieInfo>()

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
                                        RSS_DATE_FORMAT.parse(dateText)?.time
                                    } catch (e: Exception) {
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
                            // End of item - create MovieInfo
                            if (currentTitle != null && currentPubDate != null) {
                                val watchedDate = extractWatchedDate(currentDescription) ?: currentPubDate
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
            throw RssParseException("Failed to parse RSS feed: ${e.message}", e)
        }

        return movies
    }

    /**
     * Extracts the watched date from the Letterboxd description field.
     *
     * Description format: "Watched on Monday, Jan 15, 2024..."
     *
     * @param description The item description text
     * @return Watched date in milliseconds, or null if not found
     */
    private fun extractWatchedDate(description: String?): Long? {
        if (description == null) return null

        val match = WATCHED_DATE_PATTERN.find(description) ?: return null
        val dateString = match.groupValues[1] // "Jan 15, 2024"

        return try {
            WATCHED_DATE_FORMAT.parse(dateString)?.time
        } catch (e: Exception) {
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
