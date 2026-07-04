package com.tracker.core.provider

import com.tracker.core.collector.InvalidLetterboxdIdException
import com.tracker.core.collector.LetterboxdCollector
import com.tracker.core.collector.LetterboxdMetadata
import com.tracker.core.model.CounterEvidence
import com.tracker.core.types.DataSource
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Test

class MovieWatchingProviderTest {

    private val collector = mockk<LetterboxdCollector>()

    @Test
    fun query_blankUsername_throwsInvalidLetterboxdIdException() {
        val provider = MovieWatchingProvider(collector).apply { setUsername("   ") }

        assertThrows(InvalidLetterboxdIdException::class.java) {
            runBlocking { provider.query(FROM, TO) }
        }
    }

    @Test
    fun query_emptyEvidence_returnsNull() = runBlocking {
        coEvery { collector.collect(FROM, TO, "user") } returns emptyList()
        val provider = MovieWatchingProvider(collector).apply { setUsername("user") }

        assertNull(provider.query(FROM, TO))
    }

    @Test
    fun query_mapsSessionsAndSortsByWatchedDate() = runBlocking {
        coEvery { collector.collect(FROM, TO, "user") } returns listOf(
            movieEvidence(title = "Later", watchedDate = 5_000L),
            movieEvidence(title = "Earlier", watchedDate = 1_000L)
        )
        val provider = MovieWatchingProvider(collector).apply { setUsername("user") }

        val result = provider.query(FROM, TO) ?: error("expected result")

        assertEquals(listOf(DataSource.LETTERBOXD_RSS), result.sources)
        assertEquals(listOf("Earlier", "Later"), result.sessions.map { it.title })
        assertEquals(2, result.count)
    }

    @Test
    fun query_skipsEvidenceWithUnparseableMetadata() = runBlocking {
        coEvery { collector.collect(FROM, TO, "user") } returns listOf(
            movieEvidence(title = "Good", watchedDate = 1_000L),
            CounterEvidence(
                source = DataSource.LETTERBOXD_RSS,
                confidence = 0.95f,
                metadata = mapOf("garbage" to "value"), // fails LetterboxdMetadata.fromMap
                counter = 1
            )
        )
        val provider = MovieWatchingProvider(collector).apply { setUsername("user") }

        val result = provider.query(FROM, TO) ?: error("expected result")

        assertEquals(listOf("Good"), result.sessions.map { it.title })
    }

    private fun movieEvidence(title: String, watchedDate: Long): CounterEvidence =
        CounterEvidence(
            source = DataSource.LETTERBOXD_RSS,
            confidence = 0.95f,
            metadata = LetterboxdMetadata(
                title = title,
                publishedDate = watchedDate,
                watchedDate = watchedDate
            ).toMap(),
            counter = 1
        )

    private companion object {
        const val FROM = 0L
        const val TO = 10_000L
    }
}
