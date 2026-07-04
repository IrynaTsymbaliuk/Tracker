package com.tracker.core.provider

import com.tracker.core.collector.HealthConnectMindfulnessCollector
import com.tracker.core.collector.UsageEventsCollector
import com.tracker.core.collector.UsageStatsMetadata
import com.tracker.core.config.KnownApps
import com.tracker.core.model.DurationEvidence
import com.tracker.core.types.DataSource
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class ProviderAggregationTest {

    @Test
    fun languageLearning_keepsZeroMinuteSessions_sumsDuration_andSortsSessions() = runBlocking {
        val collector = mockk<UsageEventsCollector>()
        coEvery { collector.collect(FROM, TO, KnownApps.languageLearning) } returns listOf(
            usageEvidence(
                packageName = "com.example.late",
                appName = "Late",
                start = 3_000,
                end = 9_000,
                durationMinutes = 6,
                confidence = 0.90f
            ),
            usageEvidence(
                packageName = "com.example.zero",
                appName = "Zero",
                start = 2_000,
                end = 2_000,
                durationMinutes = 0,
                confidence = 1.00f
            ),
            usageEvidence(
                packageName = "com.example.early",
                appName = "Early",
                start = 1_000,
                end = 5_000,
                durationMinutes = 4,
                confidence = 0.50f
            )
        )

        val result = LanguageLearningProvider(collector).query(FROM, TO)
            ?: error("Expected language learning result")

        assertEquals(10, result.durationMinutes)
        assertEquals(
            listOf("com.example.early", "com.example.zero", "com.example.late"),
            result.sessions.map { it.packageName }
        )
    }

    @Test
    fun reading_keepsAllSessionsInDurationAndSortsByStartTime() = runBlocking {
        val collector = mockk<UsageEventsCollector>()
        coEvery { collector.collect(FROM, TO, KnownApps.reading) } returns listOf(
            usageEvidence(
                packageName = "com.example.reader",
                appName = "Reader",
                start = 2_000,
                end = 12_000,
                durationMinutes = 10,
                confidence = 0.80f
            ),
            usageEvidence(
                packageName = "com.example.reader",
                appName = "Reader",
                start = 1_000,
                end = 6_000,
                durationMinutes = 5,
                confidence = 0.80f
            ),
            usageEvidence(
                packageName = "com.example.audio",
                appName = "Audio",
                start = 20_000,
                end = 35_000,
                durationMinutes = 15,
                confidence = 0.50f
            )
        )

        val result = ReadingProvider(collector).query(FROM, TO)
            ?: error("Expected reading result")

        assertEquals(30, result.durationMinutes)
        assertEquals(
            listOf("com.example.reader", "com.example.reader", "com.example.audio"),
            result.sessions.map { it.packageName }
        )
    }

    @Test
    fun meditation_mergesOverlappingHealthConnectAndUsageSessions_andKeepsUnmatchedSessions() = runBlocking {
        val healthConnectCollector = mockk<HealthConnectMindfulnessCollector>()
        val usageEventsCollector = mockk<UsageEventsCollector>()
        coEvery { healthConnectCollector.collect(FROM, TO) } returns listOf(
            healthConnectEvidence(start = minutes(0), end = minutes(20), durationMinutes = 20),
            healthConnectEvidence(start = minutes(60), end = minutes(70), durationMinutes = 10)
        )
        coEvery { usageEventsCollector.collect(FROM, TO, KnownApps.meditation) } returns listOf(
            usageEvidence(
                packageName = "com.calm.android",
                appName = "Calm",
                start = minutes(5),
                end = minutes(15),
                durationMinutes = 10,
                confidence = 0.95f
            ),
            usageEvidence(
                packageName = "com.example.timer",
                appName = "Timer",
                start = minutes(30),
                end = minutes(40),
                durationMinutes = 10,
                confidence = 0.90f
            )
        )

        val result = (
            MeditationProvider(
                healthConnectCollector = healthConnectCollector,
                usageEventsCollector = usageEventsCollector
            ).query(FROM, TO)
        ) ?: error("Expected meditation result")

        assertEquals(listOf(DataSource.HEALTH_CONNECT, DataSource.USAGE_STATS), result.sources)
        assertEquals(40, result.durationMinutes)
        assertEquals(listOf(minutes(0), minutes(30), minutes(60)), result.sessions.map { it.startTime })
        assertEquals(listOf(DataSource.HEALTH_CONNECT, DataSource.USAGE_STATS), result.sessions[0].sources)
        assertEquals("com.calm.android", result.sessions[0].packageName)
        assertEquals(listOf(DataSource.USAGE_STATS), result.sessions[1].sources)
        assertEquals(listOf(DataSource.HEALTH_CONNECT), result.sessions[2].sources)
    }

    private fun usageEvidence(
        packageName: String,
        appName: String,
        start: Long,
        end: Long,
        durationMinutes: Int,
        confidence: Float
    ): DurationEvidence = DurationEvidence(
        source = DataSource.USAGE_STATS,
        confidence = confidence,
        metadata = UsageStatsMetadata(packageName, appName).toMap(),
        durationMinutes = durationMinutes,
        startTimeMillis = start,
        endTimeMillis = end
    )

    private fun healthConnectEvidence(
        start: Long,
        end: Long,
        durationMinutes: Int
    ): DurationEvidence = DurationEvidence(
        source = DataSource.HEALTH_CONNECT,
        confidence = 0.99f,
        metadata = emptyMap(),
        durationMinutes = durationMinutes,
        startTimeMillis = start,
        endTimeMillis = end
    )

    private fun minutes(value: Int): Long = value * 60_000L

    private companion object {
        const val FROM = 1_000L
        const val TO = 100_000L
    }
}
