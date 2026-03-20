package com.tracker.core.provider

import com.tracker.core.collector.UsageStatsCollector
import com.tracker.core.collector.UsageStatsMetadata
import com.tracker.core.config.KnownApps
import com.tracker.core.result.AppInfo
import com.tracker.core.result.LanguageLearningResult
import com.tracker.core.result.TimeRange
import com.tracker.core.result.UsageSession
import com.tracker.core.result.toConfidenceLevel
import com.tracker.core.result.toOccurred
import com.tracker.core.types.DataSource

class LanguageLearningProvider internal constructor(
    private val usageStatsCollector: UsageStatsCollector
) : MetricProvider<LanguageLearningResult> {

    override suspend fun query(
        fromMillis: Long,
        toMillis: Long,
        minConfidence: Float
    ): LanguageLearningResult? {

        val evidenceList = usageStatsCollector.collect(
            fromMillis,
            toMillis,
            KnownApps.languageLearning
        ).ifEmpty { return null }

        val validEvidenceList =
            evidenceList.filter { it.durationMinutes > 0 }.ifEmpty { return null }

        val combinedConfidence = weightedAverage(validEvidenceList)

        val totalDuration = validEvidenceList.sumOf { it.durationMinutes }

        val apps = validEvidenceList.mapNotNull { ev ->
            val metadata = UsageStatsMetadata.fromMap(ev.metadata) ?: return@mapNotNull null
            AppInfo(metadata.packageName, metadata.appName)
        }.distinctBy { it.packageName }

        val sessions = validEvidenceList.mapNotNull { ev ->
            val metadata = UsageStatsMetadata.fromMap(ev.metadata) ?: return@mapNotNull null
            UsageSession(
                startTime = ev.startTimeMillis,
                endTime = ev.endTimeMillis,
                durationMinutes = ev.durationMinutes,
                packageName = metadata.packageName,
                appName = metadata.appName
            )
        }

        return LanguageLearningResult(
            occurred = combinedConfidence.toOccurred(minConfidence),
            source = DataSource.USAGE_STATS,
            confidence = combinedConfidence,
            confidenceLevel = combinedConfidence.toConfidenceLevel(),
            timeRange = TimeRange(fromMillis, toMillis),
            durationMinutes = totalDuration,
            sessionCount = validEvidenceList.size,
            apps = apps,
            sessions = sessions
        )
    }
}
