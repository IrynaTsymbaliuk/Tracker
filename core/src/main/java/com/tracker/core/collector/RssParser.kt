package com.tracker.core.collector

import android.util.Log
import com.tracker.core.common.TAG
import com.tracker.core.result.MovieSession
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory
import java.io.StringReader
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

internal class RssParser {

    private companion object {
        val watchedDatePattern = Regex("Watched on [A-Za-z]+, ([A-Za-z]+ \\d+, \\d{4})")

        /**
         * Matches the "Watched on …" boilerplate Letterboxd puts in the description of an entry
         * with no written review, e.g. `"Watched on Sunday May 31, 2026."`. Flexible about the
         * words/commas between "Watched on" and the year so it copes with the feed's date wording.
         */
        val watchedOnSentence = Regex("Watched on [\\w\\s,]*\\d{4}\\.?")

        /**
         * Matches the rating suffix Letterboxd appends to a rated diary entry's title, e.g. the
         * `" - ★★★★½"` in `"The Devil Wears Prada 2, 2026 - ★★★★½"`. Stripped to leave the pure title.
         */
        val ratingSuffix = Regex("\\s*-\\s*[★½]+\\s*$")

        /** Matches any HTML tag, used to reduce the description HTML to plain text. */
        val htmlTag = Regex("<[^>]+>")
    }

    private val dateFormat =
        SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss Z", Locale.ENGLISH).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }

    private val watchedDateFormat =
        SimpleDateFormat("MMM dd, yyyy", Locale.ENGLISH).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }

    fun parse(rssContent: String): List<MovieSession> {
        val parser = createParser(rssContent)
        val movies = mutableListOf<MovieSession>()
        var currentItem: RssItem? = null

        while (parser.eventType != XmlPullParser.END_DOCUMENT) {
            when (parser.eventType) {
                XmlPullParser.START_TAG -> {
                    currentItem = handleStartTag(parser, currentItem)
                }

                XmlPullParser.END_TAG -> {
                    if (parser.name == "item") {
                        currentItem?.toMovieSession(watchedDateFormat)?.let { movies.add(it) }
                        currentItem = null
                    }
                }
            }
            parser.next()
        }

        return movies
    }

    private fun createParser(content: String): XmlPullParser {
        val factory = XmlPullParserFactory.newInstance()
        return factory.newPullParser().apply {
            setInput(StringReader(content))
        }
    }

    private fun handleStartTag(parser: XmlPullParser, currentItem: RssItem?): RssItem? {
        return when (parser.name) {
            "item" -> RssItem()
            "title" -> currentItem?.copy(title = parser.nextText())
            "pubDate" -> currentItem?.copy(pubDate = parseDate(parser.nextText()))
            "description" -> currentItem?.copy(description = parser.nextText())
            "tmdb:movieId" -> currentItem?.copy(tmdbId = parseTmdbId(parser.nextText()))
            "letterboxd:memberRating" -> currentItem?.copy(rating = parseRating(parser.nextText()))
            "letterboxd:rewatch" -> currentItem?.copy(rewatch = parseBoolean(parser.nextText()))
            "letterboxd:memberLike" -> currentItem?.copy(liked = parseBoolean(parser.nextText()))
            else -> currentItem
        }
    }

    private fun parseDate(dateText: String): Long? {
        return try {
            dateFormat.parse(dateText)?.time
        } catch (e: Exception) {
            Log.w(TAG, "Failed to parse pubDate: $dateText", e)
            null
        }
    }

    private fun parseTmdbId(idText: String): Int? {
        return idText.trim().toIntOrNull().also {
            if (it == null) Log.w(TAG, "Failed to parse tmdb:movieId: $idText")
        }
    }

    private fun parseRating(ratingText: String): Float? {
        return ratingText.trim().toFloatOrNull().also {
            if (it == null) Log.w(TAG, "Failed to parse letterboxd:memberRating: $ratingText")
        }
    }

    /** Parses Letterboxd's boolean-ish flags, which appear as either `Yes`/`No` or `true`/`false`. */
    private fun parseBoolean(text: String): Boolean {
        val value = text.trim()
        return value.equals("Yes", ignoreCase = true) || value.equals("true", ignoreCase = true)
    }

    private data class RssItem(
        val title: String? = null,
        val pubDate: Long? = null,
        val description: String? = null,
        val tmdbId: Int? = null,
        val rating: Float? = null,
        val rewatch: Boolean = false,
        val liked: Boolean = false
    ) {
        fun toMovieSession(watchedDateFormat: SimpleDateFormat): MovieSession? {
            val titleValue = ratingSuffix.replace(title ?: return null, "").trim()
            val pubDateValue = pubDate ?: return null
            val watchedDate = extractWatchedDate(description, watchedDateFormat) ?: pubDateValue

            return MovieSession(
                title = titleValue,
                publishedDate = pubDateValue,
                watchedDate = watchedDate,
                tmdbId = tmdbId,
                rating = rating,
                review = extractReview(description),
                isRewatch = rewatch,
                isLiked = liked
            )
        }

        /**
         * Reduces the description HTML to the user's plain-text review. Letterboxd's diary
         * description holds the poster `<img>` and an optional "Watched on …" line followed by the
         * review body. We strip all HTML tags and that boilerplate; what remains is the review, or
         * `null` when the entry has no written review.
         */
        private fun extractReview(description: String?): String? {
            if (description == null) return null

            val plain = htmlTag.replace(description, " ")
                .let { watchedOnSentence.replace(it, " ") }
                .replace("&nbsp;", " ")
                .replace(Regex("\\s+"), " ")
                .trim()

            return plain.ifBlank { null }
        }

        private fun extractWatchedDate(
            description: String?,
            watchedDateFormat: SimpleDateFormat
        ): Long? {
            if (description == null) return null

            val match = watchedDatePattern.find(description) ?: return null
            val dateString = match.groupValues[1] // "Jan 15, 2024"

            return try {
                watchedDateFormat.parse(dateString)?.time
            } catch (e: Exception) {
                Log.w(TAG, "Failed to parse watched date: $dateString", e)
                null
            }
        }
    }

}
