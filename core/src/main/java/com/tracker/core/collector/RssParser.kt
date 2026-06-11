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

    private data class RssItem(
        val title: String? = null,
        val pubDate: Long? = null,
        val description: String? = null,
        val tmdbId: Int? = null
    ) {
        fun toMovieSession(watchedDateFormat: SimpleDateFormat): MovieSession? {
            val titleValue = title ?: return null
            val pubDateValue = pubDate ?: return null
            val watchedDate = extractWatchedDate(description, watchedDateFormat) ?: pubDateValue

            return MovieSession(
                title = titleValue,
                publishedDate = pubDateValue,
                watchedDate = watchedDate,
                tmdbId = tmdbId
            )
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
