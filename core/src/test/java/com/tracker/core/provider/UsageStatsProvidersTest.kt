package com.tracker.core.provider

import com.tracker.core.collector.UsageEventsCollector
import com.tracker.core.collector.UsageStatsMetadata
import com.tracker.core.config.KnownApps
import com.tracker.core.model.DurationEvidence
import com.tracker.core.types.DataSource
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** Edge-case coverage for the three UsageStats-backed providers. */
class UsageStatsProvidersTest {

    // --- SocialMedia ---

    @Test
    fun socialMedia_emptyEvidence_returnsNull() = runBlocking {
        val collector = mockk<UsageEventsCollector>()
        every { collector.collect(FROM, TO, KnownApps.socialMedia) } returns emptyList()

        assertNull(SocialMediaProvider(collector).query(FROM, TO))
    }

    @Test
    fun socialMedia_allZeroDurationSessions_returnsNull() = runBlocking {
        val collector = mockk<UsageEventsCollector>()
        every { collector.collect(FROM, TO, KnownApps.socialMedia) } returns listOf(
            usageEvidence("com.x", "X", 1_000L, 1_000L, 0)
        )

        assertNull(SocialMediaProvider(collector).query(FROM, TO))
    }

    @Test
    fun socialMedia_sumsDurationAndSortsByStartTime() = runBlocking {
        val collector = mockk<UsageEventsCollector>()
        every { collector.collect(FROM, TO, KnownApps.socialMedia) } returns listOf(
            usageEvidence("com.late", "Late", 9_000L, 12_000L, 3),
            usageEvidence("com.early", "Early", 1_000L, 6_000L, 5)
        )

        val result = SocialMediaProvider(collector).query(FROM, TO) ?: error("expected result")

        assertEquals(8, result.durationMinutes)
        assertEquals(listOf("com.early", "com.late"), result.sessions.map { it.packageName })
        assertEquals(listOf(DataSource.USAGE_STATS), result.sources)
    }

    // --- Reading ---

    @Test
    fun reading_emptyEvidence_returnsNull() = runBlocking {
        val collector = mockk<UsageEventsCollector>()
        every { collector.collect(FROM, TO, KnownApps.reading) } returns emptyList()

        assertNull(ReadingProvider(collector).query(FROM, TO))
    }

    @Test
    fun reading_onlyZeroDurationSessions_returnsNull() = runBlocking {
        val collector = mockk<UsageEventsCollector>()
        every { collector.collect(FROM, TO, KnownApps.reading) } returns listOf(
            usageEvidence("com.amazon.kindle", "Kindle", 1_000L, 1_000L, 0)
        )

        assertNull(ReadingProvider(collector).query(FROM, TO))
    }

    // --- LanguageLearning ---

    @Test
    fun languageLearning_emptyEvidence_returnsNull() = runBlocking {
        val collector = mockk<UsageEventsCollector>()
        every { collector.collect(FROM, TO, KnownApps.languageLearning) } returns emptyList()

        assertNull(LanguageLearningProvider(collector).query(FROM, TO))
    }

    private fun usageEvidence(
        pkg: String, appName: String, start: Long, end: Long, minutes: Int
    ): DurationEvidence = DurationEvidence(
        source = DataSource.USAGE_STATS,
        confidence = 0.9f,
        metadata = UsageStatsMetadata(pkg, appName).toMap(),
        durationMinutes = minutes,
        startTimeMillis = start,
        endTimeMillis = end
    )

    private companion object {
        const val FROM = 0L
        const val TO = 100_000L
    }
}
