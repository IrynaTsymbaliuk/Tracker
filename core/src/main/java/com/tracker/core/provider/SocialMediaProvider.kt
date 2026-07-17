package com.tracker.core.provider

import com.tracker.core.collector.UsageEventsCollector
import com.tracker.core.collector.UsageStatsMetadata
import com.tracker.core.config.KnownApps
import com.tracker.core.result.SocialMediaResult
import com.tracker.core.result.TimeRange
import com.tracker.core.result.UsageSession
import com.tracker.core.types.DataSource

/**
 * Detects social media usage from app foreground sessions.
 *
 * Public results expose sources and sessions; the known-app catalogue's optional
 * [com.tracker.core.config.AppMetadata.confidenceMultiplier] is reference metadata only and
 * does not affect collection or aggregation.
 */
class SocialMediaProvider internal constructor(
    private val usageEventsCollector: UsageEventsCollector
) : MetricProvider<SocialMediaResult> {

    override suspend fun query(
        fromMillis: Long,
        toMillis: Long
    ): SocialMediaResult? {

        val evidenceList = usageEventsCollector.collect(
            fromMillis,
            toMillis,
            KnownApps.socialMedia
        ).ifEmpty { return null }

        val totalDuration = evidenceList.sumOf { it.durationMinutes }

        val sessions = evidenceList.mapNotNull { ev ->
            val metadata = UsageStatsMetadata.fromMap(ev.metadata) ?: return@mapNotNull null
            UsageSession(
                startTime = ev.startTimeMillis,
                endTime = ev.endTimeMillis,
                durationMinutes = ev.durationMinutes,
                packageName = metadata.packageName,
                appName = metadata.appName
            )
        }.sortedBy { it.startTime }

        return SocialMediaResult(
            sources = listOf(DataSource.USAGE_STATS),
            timeRange = TimeRange(fromMillis, toMillis),
            durationMinutes = totalDuration,
            sessions = sessions
        )
    }
}
