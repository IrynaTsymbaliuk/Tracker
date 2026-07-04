package com.tracker.core.provider

import com.tracker.core.collector.UsageEventsCollector
import com.tracker.core.collector.UsageStatsMetadata
import com.tracker.core.config.KnownApps
import com.tracker.core.result.ReadingResult
import com.tracker.core.result.TimeRange
import com.tracker.core.result.UsageSession
import com.tracker.core.types.DataSource

class ReadingProvider internal constructor(
    private val usageEventsCollector: UsageEventsCollector
) : MetricProvider<ReadingResult> {

    override suspend fun query(
        fromMillis: Long,
        toMillis: Long
    ): ReadingResult? {

        val evidenceList = usageEventsCollector.collect(
            fromMillis,
            toMillis,
            KnownApps.reading
        ).ifEmpty { return null }

        val validEvidenceList = evidenceList.filter {
            it.durationMinutes > 0
        }.ifEmpty { return null }

        val totalDuration = validEvidenceList.sumOf { it.durationMinutes }

        val sessions = validEvidenceList.mapNotNull { ev ->
            val metadata = UsageStatsMetadata.fromMap(ev.metadata) ?: return@mapNotNull null
            UsageSession(
                startTime = ev.startTimeMillis,
                endTime = ev.endTimeMillis,
                durationMinutes = ev.durationMinutes,
                packageName = metadata.packageName,
                appName = metadata.appName
            )
        }.sortedBy { it.startTime }

        return ReadingResult(
            sources = listOf(DataSource.USAGE_STATS),
            timeRange = TimeRange(fromMillis, toMillis),
            durationMinutes = totalDuration,
            sessions = sessions
        )
    }
}
