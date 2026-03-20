package com.tracker.core.provider

import com.tracker.core.collector.UsageStatsCollector
import com.tracker.core.config.KnownApps
import com.tracker.core.model.DurationEvidence
import com.tracker.core.result.AppInfo
import com.tracker.core.result.ReadingResult
import com.tracker.core.result.TimeRange
import com.tracker.core.result.UsageSession
import com.tracker.core.result.toConfidenceLevel
import com.tracker.core.result.toOccurred
import com.tracker.core.types.DataSource
import kotlin.math.max

class ReadingProvider internal constructor(
    private val usageStatsCollector: UsageStatsCollector
) : MetricProvider<ReadingResult> {

    private companion object {
        /**
         * Minimum session duration for reading to count as evidence.
         * Sessions shorter than this are filtered out.
         */
        const val READING_MIN_SESSION_MINUTES = 5

        /**
         * Penalty applied to combined confidence when all evidence is below minConfidence threshold.
         * This reduces confidence for cases where only weak signals exist.
         */
        const val WEAK_ONLY_PENALTY = 0.15f
    }

    override suspend fun query(
        fromMillis: Long,
        toMillis: Long,
        minConfidence: Float
    ): ReadingResult? {

        val evidenceList = usageStatsCollector.collect(
            fromMillis,
            toMillis,
            KnownApps.reading,
            READING_MIN_SESSION_MINUTES
        ).ifEmpty { return null }

        // Filter out invalid sessions with duration <= 0
        val validEvidenceList = evidenceList.filter {
            it.durationMinutes > 0
        }.ifEmpty { return null }

        var combinedConfidence = combineProbabilities(validEvidenceList.map { it.confidence })
        val totalDuration = validEvidenceList.sumOf { it.durationMinutes }

        if (validEvidenceList.all { it.confidence < minConfidence }) {
            combinedConfidence = max(0f, combinedConfidence - WEAK_ONLY_PENALTY)
        }

        val occurred = combinedConfidence.toOccurred(minConfidence)
        val confidenceLevel = combinedConfidence.toConfidenceLevel()

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

        return ReadingResult(
            occurred = occurred,
            source = DataSource.USAGE_STATS,
            confidence = combinedConfidence,
            confidenceLevel = confidenceLevel,
            timeRange = TimeRange(fromMillis, toMillis),
            durationMinutes = totalDuration,
            sessionCount = validEvidenceList.size,
            apps = apps,
            sessions = sessions
        )
    }

    /**
     * Combine independent probabilities using the formula:
     * combined = 1 - ∏(1 - p_i)
     *
     * This represents the probability that at least one of the independent
     * events is true (i.e., at least one session was genuine).
     */
    private fun combineProbabilities(confidences: List<Float>): Float {
        if (confidences.isEmpty()) return 0f
        if (confidences.size == 1) return confidences.first()

        var product = 1.0f
        for (confidence in confidences) {
            product *= (1.0f - confidence)
        }

        return 1.0f - product
    }
}
