package com.tracker.core.provider

import com.tracker.core.collector.HealthConnectMindfulnessCollector
import com.tracker.core.collector.SystemServiceUnavailableException
import com.tracker.core.collector.UsageEventsCollector
import com.tracker.core.collector.UsageStatsMetadata
import com.tracker.core.config.KnownApps
import com.tracker.core.model.DurationEvidence
import com.tracker.core.types.DataSource
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class MeditationProviderTest {

    @Test
    fun query_bothSourcesEmpty_returnsNull() = runBlocking {
        val hc = mockk<HealthConnectMindfulnessCollector>()
        val usage = mockk<UsageEventsCollector>()
        coEvery { hc.collect(FROM, TO) } returns emptyList()
        coEvery { usage.collect(FROM, TO, KnownApps.meditation) } returns emptyList()

        assertNull(MeditationProvider(hc, usage).query(FROM, TO))
    }

    @Test
    fun query_healthConnectOnly_producesHealthConnectSessions() = runBlocking {
        val hc = mockk<HealthConnectMindfulnessCollector>()
        val usage = mockk<UsageEventsCollector>()
        coEvery { hc.collect(FROM, TO) } returns listOf(hcEvidence(min(0), min(20), 20))
        coEvery { usage.collect(FROM, TO, KnownApps.meditation) } returns emptyList()

        val result = MeditationProvider(hc, usage).query(FROM, TO) ?: error("expected result")

        assertEquals(listOf(DataSource.HEALTH_CONNECT), result.sources)
        assertEquals(listOf(listOf(DataSource.HEALTH_CONNECT)), result.sessions.map { it.sources })
        assertEquals(20, result.durationMinutes)
    }

    @Test
    fun query_fallsBackToUsageStatsOnlyWhenHealthConnectUnavailable() = runBlocking {
        val usage = mockk<UsageEventsCollector>()
        coEvery { usage.collect(FROM, TO, KnownApps.meditation) } returns listOf(
            usageEvidence("com.calm.android", "Calm", min(0), min(10), 10)
        )

        // Pass null HealthConnect collector — the older-device path.
        val result = MeditationProvider(null, usage).query(FROM, TO) ?: error("expected result")

        assertEquals(listOf(DataSource.USAGE_STATS), result.sources)
        assertEquals("com.calm.android", result.sessions.single().packageName)
    }

    @Test
    fun query_mergesOverlapExceedingHalfOfShorterSession() = runBlocking {
        val hc = mockk<HealthConnectMindfulnessCollector>()
        val usage = mockk<UsageEventsCollector>()
        coEvery { hc.collect(FROM, TO) } returns listOf(hcEvidence(min(0), min(20), 20))
        coEvery { usage.collect(FROM, TO, KnownApps.meditation) } returns listOf(
            usageEvidence("com.calm.android", "Calm", min(5), min(15), 10) // overlap 10m of a 10m session
        )

        val result = MeditationProvider(hc, usage).query(FROM, TO) ?: error("expected result")

        val session = result.sessions.single()
        assertEquals(listOf(DataSource.HEALTH_CONNECT, DataSource.USAGE_STATS), session.sources)
        assertEquals(min(0), session.startTime) // HealthConnect timestamps win
        assertEquals("com.calm.android", session.packageName)
    }

    @Test
    fun query_doesNotMergeWhenOverlapBelowHalfOfShorterSession() = runBlocking {
        val hc = mockk<HealthConnectMindfulnessCollector>()
        val usage = mockk<UsageEventsCollector>()
        coEvery { hc.collect(FROM, TO) } returns listOf(hcEvidence(min(0), min(20), 20))
        coEvery { usage.collect(FROM, TO, KnownApps.meditation) } returns listOf(
            // overlap is only [15,20] = 5m of a 20m HC session -> below 50% of shorter -> no merge
            usageEvidence("com.calm.android", "Calm", min(15), min(40), 25)
        )

        val result = MeditationProvider(hc, usage).query(FROM, TO) ?: error("expected result")

        assertEquals(2, result.sessions.size)
        assertEquals(
            listOf(listOf(DataSource.HEALTH_CONNECT), listOf(DataSource.USAGE_STATS)),
            result.sessions.map { it.sources }
        )
    }

    @Test
    fun query_oneHealthConnectSessionPicksLargerOverlap_leavesOtherUnmatched() = runBlocking {
        val hc = mockk<HealthConnectMindfulnessCollector>()
        val usage = mockk<UsageEventsCollector>()
        coEvery { hc.collect(FROM, TO) } returns listOf(hcEvidence(min(0), min(30), 30))
        coEvery { usage.collect(FROM, TO, KnownApps.meditation) } returns listOf(
            usageEvidence("com.a", "A", min(2), min(12), 10),  // overlap 10m
            usageEvidence("com.b", "B", min(20), min(28), 8)   // overlap 8m
        )

        val result = MeditationProvider(hc, usage).query(FROM, TO) ?: error("expected result")

        // Merged session (HC + A) plus the unmatched B usage-only session.
        assertEquals(2, result.sessions.size)
        val merged = result.sessions.first { it.sources.size == 2 }
        assertEquals("com.a", merged.packageName)
        val soloUsage = result.sessions.first { it.sources == listOf(DataSource.USAGE_STATS) }
        assertEquals("com.b", soloUsage.packageName)
        assertEquals(listOf(DataSource.HEALTH_CONNECT, DataSource.USAGE_STATS), result.sources)
    }

    @Test
    fun query_healthConnectThrows_treatedAsEmptyAndFallsBackToUsage() = runBlocking {
        val hc = mockk<HealthConnectMindfulnessCollector>()
        val usage = mockk<UsageEventsCollector>()
        coEvery { hc.collect(FROM, TO) } throws SystemServiceUnavailableException("HC")
        coEvery { usage.collect(FROM, TO, KnownApps.meditation) } returns listOf(
            usageEvidence("com.calm.android", "Calm", min(0), min(10), 10)
        )

        val result = MeditationProvider(hc, usage).query(FROM, TO) ?: error("expected result")

        assertEquals(listOf(DataSource.USAGE_STATS), result.sources)
    }

    @Test
    fun query_sessionsAreSortedByStartTime() = runBlocking {
        val hc = mockk<HealthConnectMindfulnessCollector>()
        val usage = mockk<UsageEventsCollector>()
        coEvery { hc.collect(FROM, TO) } returns listOf(hcEvidence(min(60), min(70), 10))
        coEvery { usage.collect(FROM, TO, KnownApps.meditation) } returns listOf(
            usageEvidence("com.calm.android", "Calm", min(5), min(15), 10)
        )

        val result = MeditationProvider(hc, usage).query(FROM, TO) ?: error("expected result")

        assertEquals(listOf(min(5), min(60)), result.sessions.map { it.startTime })
    }

    private fun hcEvidence(start: Long, end: Long, minutes: Int): DurationEvidence =
        DurationEvidence(
            source = DataSource.HEALTH_CONNECT,
            confidence = 0.99f,
            metadata = emptyMap(),
            durationMinutes = minutes,
            startTimeMillis = start,
            endTimeMillis = end
        )

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

    private fun min(m: Int): Long = m * 60_000L

    private companion object {
        const val FROM = 1_000L
        const val TO = 10_000_000L
    }
}
