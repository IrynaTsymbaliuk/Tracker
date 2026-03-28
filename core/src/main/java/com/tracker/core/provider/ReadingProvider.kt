package com.tracker.core.provider

import com.tracker.core.collector.UsageEventsCollector
import com.tracker.core.collector.UsageStatsMetadata
import com.tracker.core.config.KnownApps
import com.tracker.core.result.AppInfo
import com.tracker.core.result.ReadingResult
import com.tracker.core.result.TimeRange
import com.tracker.core.result.toConfidenceLevel
import com.tracker.core.result.toOccurred
import com.tracker.core.types.DataSource

class ReadingProvider internal constructor(
    private val usageEventsCollector: UsageEventsCollector
) : MetricProvider<ReadingResult> {

    override suspend fun query(
        fromMillis: Long,
        toMillis: Long,
        minConfidence: Float
    ): ReadingResult? {

        val evidenceList = usageEventsCollector.collect(
            fromMillis,
            toMillis,
            KnownApps.reading
        ).ifEmpty { return null }

        val validEvidenceList = evidenceList.filter {
            it.durationMinutes > 0
        }.ifEmpty { return null }

        val uniqueAppConfidences = validEvidenceList
            .distinctBy { UsageStatsMetadata.fromMap(it.metadata)?.packageName }
            .map { it.confidence }
        val combinedConfidence = combineProbabilities(uniqueAppConfidences)
        val totalDuration = validEvidenceList.sumOf { it.durationMinutes }

        val occurred = combinedConfidence.toOccurred(minConfidence)
        val confidenceLevel = combinedConfidence.toConfidenceLevel()

        val apps = validEvidenceList.mapNotNull { ev ->
            val metadata = UsageStatsMetadata.fromMap(ev.metadata) ?: return@mapNotNull null
            AppInfo(metadata.packageName, metadata.appName)
        }.distinctBy { it.packageName }

        return ReadingResult(
            occurred = occurred,
            source = DataSource.USAGE_STATS,
            confidence = combinedConfidence,
            confidenceLevel = confidenceLevel,
            timeRange = TimeRange(fromMillis, toMillis),
            durationMinutes = totalDuration,
            sessionCount = validEvidenceList.size,
            apps = apps
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
