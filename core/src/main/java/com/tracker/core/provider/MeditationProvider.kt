package com.tracker.core.provider

import android.os.Build
import com.tracker.core.collector.CollectorException
import com.tracker.core.collector.HealthConnectMindfulnessCollector
import com.tracker.core.collector.UsageEventsCollector
import com.tracker.core.collector.UsageStatsMetadata
import com.tracker.core.config.KnownApps
import com.tracker.core.model.DurationEvidence
import com.tracker.core.result.MeditationResult
import com.tracker.core.result.MeditationSession
import com.tracker.core.result.TimeRange
import com.tracker.core.result.toConfidenceLevel
import com.tracker.core.types.DataSource

/**
 * Detects meditation activity by fusing two sources:
 *
 * 1. Health Connect [androidx.health.connect.client.records.MindfulnessSessionRecord] via
 *    [HealthConnectMindfulnessCollector] (confidence 0.99).
 * 2. Foreground sessions of known meditation apps (Calm, Headspace, Insight Timer, etc.)
 *    via [UsageEventsCollector] against [KnownApps.meditation] (confidence 0.85–0.95).
 *
 * Behavior:
 * - If both sources return data, sessions that overlap significantly (≥ 50% of the
 *   shorter session's duration) are deduplicated into a single [MeditationSession]
 *   whose `sources` lists both [DataSource.HEALTH_CONNECT] and [DataSource.USAGE_STATS].
 * - If only one source is available (e.g. HealthConnect unavailable, permission denied,
 *   or record type unsupported on this device/client), the result is built from the
 *   remaining source. This is how the fallback to UsageStats-only works on older devices.
 * - Returns `null` when neither source produced usable sessions.
 *
 * No minimum-duration filter is applied — 0-minute sessions (brief app opens) are kept
 * so session counts remain accurate.
 */
class MeditationProvider internal constructor(
    private val healthConnectCollector: HealthConnectMindfulnessCollector?,
    private val usageEventsCollector: UsageEventsCollector
) : MetricProvider<MeditationResult> {

    override suspend fun query(
        fromMillis: Long,
        toMillis: Long,
        minConfidence: Float
    ): MeditationResult? {

        val healthConnectEvidence = collectHealthConnect(fromMillis, toMillis)
        val usageStatsEvidence = collectUsageStats(fromMillis, toMillis)

        if (healthConnectEvidence.isEmpty() && usageStatsEvidence.isEmpty()) return null

        val mergedSessions = mergeSessions(healthConnectEvidence, usageStatsEvidence)
        if (mergedSessions.isEmpty()) return null

        val sources = buildList {
            if (healthConnectEvidence.isNotEmpty()) add(DataSource.HEALTH_CONNECT)
            if (usageStatsEvidence.isNotEmpty()) add(DataSource.USAGE_STATS)
        }

        val combinedConfidence = weightedAverage(
            healthConnectEvidence + usageStatsEvidence
        )

        val totalDuration = mergedSessions.sumOf { it.durationMinutes }

        return MeditationResult(
            sources = sources,
            confidence = combinedConfidence,
            confidenceLevel = combinedConfidence.toConfidenceLevel(),
            timeRange = TimeRange(fromMillis, toMillis),
            durationMinutes = totalDuration,
            sessions = mergedSessions
        )
    }

    private suspend fun collectHealthConnect(
        fromMillis: Long,
        toMillis: Long
    ): List<DurationEvidence> {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return emptyList()
        val collector = healthConnectCollector ?: return emptyList()
        return try {
            collector.collect(fromMillis, toMillis)
        } catch (e: CollectorException) {
            emptyList()
        }
    }

    private fun collectUsageStats(
        fromMillis: Long,
        toMillis: Long
    ): List<DurationEvidence> {
        return try {
            usageEventsCollector.collect(fromMillis, toMillis, KnownApps.meditation)
        } catch (e: CollectorException) {
            emptyList()
        }
    }

    /**
     * Merges HealthConnect records and UsageStats sessions into a sorted list of
     * [MeditationSession]. Overlapping pairs (overlap ≥ 50% of the shorter session)
     * become a single merged session whose `sources` lists both origins.
     *
     * Matching strategy: for each HealthConnect evidence, find the UsageStats
     * evidence with the largest qualifying overlap; consume both. Remaining entries
     * from either side are kept as single-source sessions.
     */
    private fun mergeSessions(
        healthConnectEvidence: List<DurationEvidence>,
        usageStatsEvidence: List<DurationEvidence>
    ): List<MeditationSession> {

        val claimedUsageIdx = BooleanArray(usageStatsEvidence.size)
        val sessions = mutableListOf<MeditationSession>()

        for (hcEv in healthConnectEvidence) {
            var bestIdx = -1
            var bestOverlap = 0L

            for (i in usageStatsEvidence.indices) {
                if (claimedUsageIdx[i]) continue
                val usEv = usageStatsEvidence[i]
                val overlap = overlapMillis(
                    hcEv.startTimeMillis, hcEv.endTimeMillis,
                    usEv.startTimeMillis, usEv.endTimeMillis
                )
                if (overlap <= 0) continue

                val hcDur = (hcEv.endTimeMillis - hcEv.startTimeMillis).coerceAtLeast(1)
                val usDur = (usEv.endTimeMillis - usEv.startTimeMillis).coerceAtLeast(1)
                val shorter = minOf(hcDur, usDur)
                if (overlap * 2 < shorter) continue // < 50% of shorter duration

                if (overlap > bestOverlap) {
                    bestOverlap = overlap
                    bestIdx = i
                }
            }

            if (bestIdx >= 0) {
                val usEv = usageStatsEvidence[bestIdx]
                claimedUsageIdx[bestIdx] = true
                val metadata = UsageStatsMetadata.fromMap(usEv.metadata)
                // Prefer HealthConnect timestamps (authoritative for meditation start/end),
                // enrich with app info from UsageStats.
                sessions.add(
                    MeditationSession(
                        startTime = hcEv.startTimeMillis,
                        endTime = hcEv.endTimeMillis,
                        durationMinutes = hcEv.durationMinutes,
                        sources = listOf(DataSource.HEALTH_CONNECT, DataSource.USAGE_STATS),
                        packageName = metadata?.packageName,
                        appName = metadata?.appName
                    )
                )
            } else {
                // HealthConnect-only session
                sessions.add(
                    MeditationSession(
                        startTime = hcEv.startTimeMillis,
                        endTime = hcEv.endTimeMillis,
                        durationMinutes = hcEv.durationMinutes,
                        sources = listOf(DataSource.HEALTH_CONNECT),
                        packageName = null,
                        appName = null
                    )
                )
            }
        }

        // Unclaimed UsageStats evidence becomes its own UsageStats-only session.
        for (i in usageStatsEvidence.indices) {
            if (claimedUsageIdx[i]) continue
            val usEv = usageStatsEvidence[i]
            val metadata = UsageStatsMetadata.fromMap(usEv.metadata)
            sessions.add(
                MeditationSession(
                    startTime = usEv.startTimeMillis,
                    endTime = usEv.endTimeMillis,
                    durationMinutes = usEv.durationMinutes,
                    sources = listOf(DataSource.USAGE_STATS),
                    packageName = metadata?.packageName,
                    appName = metadata?.appName
                )
            )
        }

        return sessions.sortedBy { it.startTime }
    }

    private fun overlapMillis(
        aStart: Long, aEnd: Long,
        bStart: Long, bEnd: Long
    ): Long {
        val start = maxOf(aStart, bStart)
        val end = minOf(aEnd, bEnd)
        return (end - start).coerceAtLeast(0)
    }
}
