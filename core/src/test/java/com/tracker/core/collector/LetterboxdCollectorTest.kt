package com.tracker.core.collector

import com.tracker.core.types.DataSource
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.time.Instant

@RunWith(RobolectricTestRunner::class)
class LetterboxdCollectorTest {

    @Test
    fun collect_blankUsername_throwsWithoutFetching() {
        var fetched = false
        val collector = LetterboxdCollector(rssFetcher = { fetched = true; "" })

        assertThrows(InvalidLetterboxdIdException::class.java) {
            runBlocking { collector.collect(0L, Long.MAX_VALUE, "  ") }
        }
        assertEquals(false, fetched)
    }

    @Test
    fun collect_keepsFilmsWhoseWatchedDateIsInRange_dropsOutOfRange() = runBlocking {
        val feed = rss(
            item(filmTitle = "InRange", watchedDate = "2020-06-15", pubDate = "Tue, 16 Jun 2020 10:00:00 +0000"),
            item(filmTitle = "OutOfRange", watchedDate = "2019-01-01", pubDate = "Wed, 02 Jan 2019 10:00:00 +0000")
        )
        val collector = LetterboxdCollector(rssFetcher = { feed })

        val evidence = collector.collect(
            fromMillis = utcMidnight("2020-06-01"),
            toMillis = utcMidnight("2020-06-30"),
            letterboxdUsername = "user"
        )

        val titles = evidence.map { LetterboxdMetadata.fromMap(it.metadata)?.title }
        assertEquals(listOf("InRange"), titles)
        assertEquals(DataSource.LETTERBOXD_RSS, evidence.first().source)
        assertEquals(0.95f, evidence.first().confidence)
    }

    @Test
    fun collect_keepsFilmWhenPublishedDateIsInRangeEvenIfWatchedDateIsNot() = runBlocking {
        // watchedDate out of window, but the diary entry was published inside it.
        val feed = rss(
            item(filmTitle = "PublishedInRange", watchedDate = "2019-12-30", pubDate = "Mon, 15 Jun 2020 10:00:00 +0000")
        )
        val collector = LetterboxdCollector(rssFetcher = { feed })

        val evidence = collector.collect(
            fromMillis = utcMidnight("2020-06-01"),
            toMillis = utcMidnight("2020-06-30"),
            letterboxdUsername = "user"
        )

        assertEquals(1, evidence.size)
    }

    @Test
    fun collect_emptyFeed_returnsEmptyList() = runBlocking {
        val collector = LetterboxdCollector(rssFetcher = { rss(/* no items */) })

        val evidence = collector.collect(0L, Long.MAX_VALUE, "user")

        assertTrue(evidence.isEmpty())
    }

    @Test
    fun collect_malformedFeed_throwsRssParseException() {
        val collector = LetterboxdCollector(rssFetcher = { "<rss><item></rss>" })

        assertThrows(RssParseException::class.java) {
            runBlocking { collector.collect(0L, Long.MAX_VALUE, "user") }
        }
    }

    @Test
    fun collect_networkFailureFromFetcher_propagates() {
        val collector = LetterboxdCollector(rssFetcher = { throw NetworkException("boom") })

        assertThrows(NetworkException::class.java) {
            runBlocking { collector.collect(0L, Long.MAX_VALUE, "user") }
        }
    }

    @Test
    fun collect_percentEncodesUsernameInFeedUrl() = runBlocking {
        var requestedUrl = ""
        val collector = LetterboxdCollector(rssFetcher = { url -> requestedUrl = url; rss() })

        collector.collect(0L, Long.MAX_VALUE, "user name")

        assertTrue("was: $requestedUrl", requestedUrl.contains("user+name") || requestedUrl.contains("user%20name"))
    }

    // --- RSS fixtures ---

    private fun rss(vararg items: String): String =
        """<?xml version="1.0" encoding="UTF-8"?>
           <rss><channel>${items.joinToString("")}</channel></rss>"""

    private fun item(filmTitle: String, watchedDate: String, pubDate: String): String =
        """<item>
             <title>$filmTitle</title>
             <letterboxd:filmTitle>$filmTitle</letterboxd:filmTitle>
             <pubDate>$pubDate</pubDate>
             <letterboxd:watchedDate>$watchedDate</letterboxd:watchedDate>
           </item>"""

    private fun utcMidnight(isoDate: String): Long =
        Instant.parse("${isoDate}T00:00:00Z").toEpochMilli()
}
