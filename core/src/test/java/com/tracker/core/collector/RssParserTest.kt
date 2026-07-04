package com.tracker.core.collector

import com.tracker.core.result.MovieSession
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

@RunWith(RobolectricTestRunner::class)
class RssParserTest {

    private val parser = RssParser()

    @Test
    fun parse_returnsCompleteMovieSession_whenLetterboxdItemHasAllSupportedFields() {
        val movie = parseSingle(
            item(
                title = "Fallback Title, 2026 - ★★★★½",
                filmTitle = "The Real Film",
                filmYear = "2026",
                pubDate = "Sun, 14 Jun 2026 18:45:00 +0000",
                watchedDate = "2026-06-13",
                tmdbMovieId = "123456",
                memberRating = "4.5",
                rewatch = "Yes",
                memberLike = "true",
                description = """
                    <![CDATA[
                        <p><img src="https://a.ltrbxd.com/poster.jpg"/></p>
                        <p>Watched on Saturday June 13, 2026.</p>
                        <p>Sharp, funny, and humane.</p>
                    ]]>
                """.trimIndent()
            )
        )

        assertEquals("The Real Film", movie.title)
        assertEquals(2026, movie.year)
        assertEquals(utcDateTime("Sun, 14 Jun 2026 18:45:00 +0000"), movie.publishedDate)
        assertEquals(utcDate("2026-06-13"), movie.watchedDate)
        assertEquals(123456, movie.tmdbId)
        assertEquals(4.5f, movie.rating ?: 0f, 0.0f)
        assertEquals("Sharp, funny, and humane.", movie.review)
        assertEquals("https://a.ltrbxd.com/poster.jpg", movie.posterUrl)
        assertTrue(movie.isRewatch)
        assertTrue(movie.isLiked)
    }

    @Test
    fun parse_returnsMultipleMovieSessions_inFeedOrder() {
        val movies = parser.parse(
            rss(
                item(title = "First", pubDate = "Sun, 14 Jun 2026 18:45:00 +0000"),
                item(title = "Second", pubDate = "Mon, 15 Jun 2026 18:45:00 +0000"),
                item(title = "Third", pubDate = "Tue, 16 Jun 2026 18:45:00 +0000")
            )
        )

        assertEquals(listOf("First", "Second", "Third"), movies.map { it.title })
    }

    @Test
    fun parse_returnsEmptyList_whenFeedHasNoItems() {
        assertTrue(parser.parse(rss()).isEmpty())
    }

    @Test
    fun parse_skipsItem_whenTitleIsMissing() {
        val movies = parser.parse(
            rss(item(title = null, pubDate = "Sun, 14 Jun 2026 18:45:00 +0000"))
        )

        assertTrue(movies.isEmpty())
    }

    @Test
    fun parse_skipsItem_whenPubDateIsMissing() {
        val movies = parser.parse(rss(item(title = "Missing Date", pubDate = null)))

        assertTrue(movies.isEmpty())
    }

    @Test
    fun parse_prefersFilmTitle_overEntryTitle() {
        val movie = parseSingle(
            item(title = "Entry Title, 2026 - ★★★", filmTitle = "Feed Film Title")
        )

        assertEquals("Feed Film Title", movie.title)
    }

    @Test
    fun parse_fallsBackToEntryTitle_whenFilmTitleIsMissing() {
        val movie = parseSingle(item(title = "Entry Title"))

        assertEquals("Entry Title", movie.title)
    }

    @Test
    fun parse_stripsLetterboxdRatingSuffix_fromFallbackTitle() {
        val movie = parseSingle(item(title = "The Movie, 2026 - ★★★½"))

        assertEquals("The Movie, 2026", movie.title)
    }

    @Test
    fun parse_keepsFallbackTitle_whenNoRatingSuffixExists() {
        val movie = parseSingle(item(title = "The Movie, 2026"))

        assertEquals("The Movie, 2026", movie.title)
    }

    @Test
    fun parse_ignoresBlankFilmTitle_andFallsBackToEntryTitle() {
        val movie = parseSingle(item(title = "Fallback Title", filmTitle = "   "))

        assertEquals("Fallback Title", movie.title)
    }

    @Test
    fun parse_prefersLetterboxdWatchedDate_overDescriptionAndPubDate() {
        val movie = parseSingle(
            item(
                watchedDate = "2026-06-13",
                pubDate = "Sun, 14 Jun 2026 18:45:00 +0000",
                description = "<![CDATA[<p>Watched on Monday June 1, 2026.</p>]]>"
            )
        )

        assertEquals(utcDate("2026-06-13"), movie.watchedDate)
    }

    @Test
    fun parse_usesDescriptionWatchedDate_whenLetterboxdWatchedDateMissing() {
        val movie = parseSingle(
            item(
                pubDate = "Sun, 14 Jun 2026 18:45:00 +0000",
                description = "<![CDATA[<p>Watched on Sunday May 31, 2026.</p>]]>"
            )
        )

        assertEquals(utcDate("2026-05-31"), movie.watchedDate)
    }

    @Test
    fun parse_usesPubDate_whenWatchedDateIsMissingEverywhere() {
        val movie = parseSingle(item(pubDate = "Sun, 14 Jun 2026 18:45:00 +0000"))

        assertEquals(utcDateTime("Sun, 14 Jun 2026 18:45:00 +0000"), movie.watchedDate)
    }

    @Test
    fun parse_usesDescriptionWatchedDate_whenLetterboxdWatchedDateIsMalformed() {
        val movie = parseSingle(
            item(
                watchedDate = "June 13, 2026",
                pubDate = "Sun, 14 Jun 2026 18:45:00 +0000",
                description = "<![CDATA[<p>Watched on Saturday June 13, 2026.</p>]]>"
            )
        )

        assertEquals(utcDate("2026-06-13"), movie.watchedDate)
    }

    @Test
    fun parse_usesPubDate_whenDescriptionWatchedDateIsMalformed() {
        val movie = parseSingle(
            item(
                pubDate = "Sun, 14 Jun 2026 18:45:00 +0000",
                description = "<![CDATA[<p>Watched on someday recently.</p>]]>"
            )
        )

        assertEquals(utcDateTime("Sun, 14 Jun 2026 18:45:00 +0000"), movie.watchedDate)
    }

    @Test
    fun parse_parsesWatchedDateAtUtcMidnight() {
        val movie = parseSingle(item(watchedDate = "2026-06-13"))

        assertEquals(utcDate("2026-06-13"), movie.watchedDate)
    }

    @Test
    fun parse_parsesPubDateWithTimezoneOffsetCorrectly() {
        val movie = parseSingle(item(pubDate = "Sun, 14 Jun 2026 20:45:00 +0200"))

        assertEquals(utcDateTime("Sun, 14 Jun 2026 18:45:00 +0000"), movie.publishedDate)
    }

    @Test
    fun parse_setsOptionalFieldsToNull_whenYearTmdbRatingPosterReviewAreMissing() {
        val movie = parseSingle(item())

        assertNull(movie.year)
        assertNull(movie.tmdbId)
        assertNull(movie.rating)
        assertNull(movie.review)
        assertNull(movie.posterUrl)
    }

    @Test
    fun parse_setsYearToNull_whenFilmYearIsMalformed() {
        val movie = parseSingle(item(filmYear = "unknown"))

        assertNull(movie.year)
    }

    @Test
    fun parse_setsTmdbIdToNull_whenMovieIdIsMalformed() {
        val movie = parseSingle(item(tmdbMovieId = "not-a-number"))

        assertNull(movie.tmdbId)
    }

    @Test
    fun parse_setsRatingToNull_whenMemberRatingIsMalformed() {
        val movie = parseSingle(item(memberRating = "five"))

        assertNull(movie.rating)
    }

    @Test
    fun parse_allowsHalfStarRatingValues() {
        val movie = parseSingle(item(memberRating = "0.5"))

        assertEquals(0.5f, movie.rating ?: 0f, 0.0f)
    }

    @Test
    fun parse_doesNotUseTmdbTvIdAsMovieId() {
        val movie = parseSingle(
            item(
                tmdbMovieId = null,
                extraTags = "<tmdb:tvId>98765</tmdb:tvId>"
            )
        )

        assertNull(movie.tmdbId)
    }

    @Test
    fun parse_extractsPosterUrl_fromDescriptionImage() {
        val movie = parseSingle(
            item(description = "<![CDATA[<p><img src=\"https://example.com/poster.png\"/></p>]]>")
        )

        assertEquals("https://example.com/poster.png", movie.posterUrl)
    }

    @Test
    fun parse_setsPosterUrlToNull_whenDescriptionHasNoImage() {
        val movie = parseSingle(item(description = "<![CDATA[<p>No poster.</p>]]>"))

        assertNull(movie.posterUrl)
    }

    @Test
    fun parse_extractsPlainTextReview_fromHtmlDescription() {
        val movie = parseSingle(
            item(description = "<![CDATA[<p>A <strong>great</strong> watch.</p>]]>")
        )

        assertEquals("A great watch.", movie.review)
    }

    @Test
    fun parse_removesWatchedOnBoilerplate_fromReview() {
        val movie = parseSingle(
            item(
                description = """
                    <![CDATA[
                        <p>Watched on Sunday May 31, 2026.</p>
                        <p>The review remains.</p>
                    ]]>
                """.trimIndent()
            )
        )

        assertEquals("The review remains.", movie.review)
    }

    @Test
    fun parse_returnsNullReview_whenDescriptionOnlyContainsPosterAndWatchedOnText() {
        val movie = parseSingle(
            item(
                description = """
                    <![CDATA[
                        <p><img src="https://example.com/poster.png"/></p>
                        <p>Watched on Sunday May 31, 2026.</p>
                    ]]>
                """.trimIndent()
            )
        )

        assertNull(movie.review)
    }

    @Test
    fun parse_replacesNbspAndCollapsesWhitespace_inReview() {
        val movie = parseSingle(
            item(description = "<![CDATA[<p>One&nbsp;&nbsp;two<br/> three</p>]]>")
        )

        assertEquals("One two three", movie.review)
    }

    @Test
    fun parse_handlesDescriptionCdataWithNestedHtml() {
        val movie = parseSingle(
            item(
                description = """
                    <![CDATA[
                        <section><p><em>Layered</em> <a href="https://example.com">review</a>.</p></section>
                    ]]>
                """.trimIndent()
            )
        )

        assertEquals("Layered review.", movie.review)
    }

    @Test
    fun parse_setsRewatchAndLikedTrue_forYesValues() {
        val movie = parseSingle(item(rewatch = "Yes", memberLike = "Yes"))

        assertTrue(movie.isRewatch)
        assertTrue(movie.isLiked)
    }

    @Test
    fun parse_setsRewatchAndLikedTrue_forTrueValues() {
        val movie = parseSingle(item(rewatch = "true", memberLike = "true"))

        assertTrue(movie.isRewatch)
        assertTrue(movie.isLiked)
    }

    @Test
    fun parse_setsRewatchAndLikedFalse_forNoFalseMissingOrUnknownValues() {
        val noMovie = parseSingle(item(rewatch = "No", memberLike = "No"))
        val falseMovie = parseSingle(item(rewatch = "false", memberLike = "false"))
        val missingMovie = parseSingle(item(rewatch = null, memberLike = null))
        val unknownMovie = parseSingle(item(rewatch = "maybe", memberLike = "1"))

        listOf(noMovie, falseMovie, missingMovie, unknownMovie).forEach { movie ->
            assertFalse(movie.isRewatch)
            assertFalse(movie.isLiked)
        }
    }

    @Test
    fun parse_trimsBooleanValues_andIgnoresCase() {
        val movie = parseSingle(item(rewatch = "  yEs  ", memberLike = "  TRUE  "))

        assertTrue(movie.isRewatch)
        assertTrue(movie.isLiked)
    }

    @Test
    fun parse_throws_whenXmlIsMalformed() {
        assertThrows(Exception::class.java) {
            parser.parse("<rss><channel><item><title>Broken</title></channel></rss>")
        }
    }

    @Test
    fun parseRssFeed_wrapsParserFailureInRssParseException() {
        val collector = LetterboxdCollector(rssFetcher = RssFetcher { "" })

        assertThrows(RssParseException::class.java) {
            collector.parseRssFeed("<rss><channel><item><title>Broken</title></channel></rss>")
        }
    }

    private fun parseSingle(item: String): MovieSession {
        val movies = parser.parse(rss(item))
        assertEquals(1, movies.size)
        return movies.single()
    }

    private fun rss(vararg items: String): String = """
        <rss
            version="2.0"
            xmlns:letterboxd="https://letterboxd.com"
            xmlns:tmdb="https://www.themoviedb.org">
            <channel>
                <title>Letterboxd Test Feed</title>
                ${items.joinToString(separator = "\n")}
            </channel>
        </rss>
    """.trimIndent()

    private fun item(
        title: String? = "The Movie",
        filmTitle: String? = null,
        filmYear: String? = null,
        watchedDate: String? = null,
        pubDate: String? = "Sun, 14 Jun 2026 18:45:00 +0000",
        description: String? = null,
        tmdbMovieId: String? = null,
        memberRating: String? = null,
        rewatch: String? = null,
        memberLike: String? = null,
        extraTags: String = ""
    ): String = """
        <item>
            ${tag("title", title)}
            ${tag("letterboxd:filmTitle", filmTitle)}
            ${tag("letterboxd:filmYear", filmYear)}
            ${tag("letterboxd:watchedDate", watchedDate)}
            ${tag("pubDate", pubDate)}
            ${tag("description", description)}
            ${tag("tmdb:movieId", tmdbMovieId)}
            ${tag("letterboxd:memberRating", memberRating)}
            ${tag("letterboxd:rewatch", rewatch)}
            ${tag("letterboxd:memberLike", memberLike)}
            $extraTags
        </item>
    """.trimIndent()

    private fun tag(name: String, value: String?): String {
        return value?.let { "<$name>$it</$name>" }.orEmpty()
    }

    private fun utcDate(date: String): Long {
        return dateFormat("yyyy-MM-dd").parse(date)?.time ?: error("Invalid test date: $date")
    }

    private fun utcDateTime(date: String): Long {
        return dateFormat("EEE, dd MMM yyyy HH:mm:ss Z").parse(date)?.time
            ?: error("Invalid test datetime: $date")
    }

    private fun dateFormat(pattern: String): SimpleDateFormat {
        return SimpleDateFormat(pattern, Locale.ENGLISH).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }
    }
}
