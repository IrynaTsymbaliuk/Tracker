package com.tracker.core.provider

import com.tracker.core.collector.UsageEventsCollector
import com.tracker.core.collector.UsageStatsMetadata
import com.tracker.core.config.KnownApps
import com.tracker.core.result.LanguageLearningResult
import com.tracker.core.result.TimeRange
import com.tracker.core.result.UsageSession
import com.tracker.core.types.DataSource

class LanguageLearningProvider internal constructor(
    private val usageEventsCollector: UsageEventsCollector
) : MetricProvider<LanguageLearningResult> {

    override suspend fun query(
        fromMillis: Long,
        toMillis: Long
    ): LanguageLearningResult? {

        val evidenceList = usageEventsCollector.collect(
            fromMillis,
            toMillis,
            KnownApps.languageLearning
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

        return LanguageLearningResult(
            sources = listOf(DataSource.USAGE_STATS),
            timeRange = TimeRange(fromMillis, toMillis),
            durationMinutes = totalDuration,
            sessions = sessions
        )
    }
}
