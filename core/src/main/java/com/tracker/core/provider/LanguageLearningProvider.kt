package com.tracker.core.provider

import com.tracker.core.collector.UsageStatsCollector
import com.tracker.core.config.KnownApps
import com.tracker.core.model.DurationEvidence
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

    private companion object {
        /**
         * Minimum session duration for language learning to count as evidence.
         * Sessions shorter than this are filtered out.
         */
        const val LANGUAGE_LEARNING_MIN_SESSION_MINUTES = 5
    }

    override suspend fun query(
        fromMillis: Long,
        toMillis: Long,
        minConfidence: Float
    ): LanguageLearningResult? {

        val evidenceList = usageStatsCollector.collect(
            fromMillis,
            toMillis,
            KnownApps.languageLearning,
            LANGUAGE_LEARNING_MIN_SESSION_MINUTES
        ).ifEmpty { return null }

        val validEvidenceList = evidenceList.filter { it.durationMinutes > 0 }.ifEmpty { return null }

        val combinedConfidence = weightedAverage(validEvidenceList)

        val totalDuration = validEvidenceList.sumOf { it.durationMinutes }

        val apps = validEvidenceList.mapNotNull { ev ->
            val packageName = ev.metadata["packageName"] as? String
            val appName = ev.metadata["appName"] as? String
            if (packageName != null && appName != null) {
                AppInfo(packageName, appName)
            } else {
                null
            }
        }.distinctBy { it.packageName }

        val sessions = validEvidenceList.mapNotNull { ev ->
            if (ev !is DurationEvidence) return@mapNotNull null
            val packageName = ev.metadata["packageName"] as? String
            val appName = ev.metadata["appName"] as? String
            if (packageName != null && appName != null) {
                UsageSession(
                    startTime = ev.startTimeMillis,
                    endTime = ev.endTimeMillis,
                    durationMinutes = ev.durationMinutes,
                    packageName = packageName,
                    appName = appName
                )
            } else {
                null
            }
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
