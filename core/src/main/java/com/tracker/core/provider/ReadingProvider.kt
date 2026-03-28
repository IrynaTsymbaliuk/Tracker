package com.tracker.core.provider

import com.tracker.core.collector.UsageEventsCollector
import com.tracker.core.collector.UsageStatsMetadata
import com.tracker.core.config.KnownApps
import com.tracker.core.result.ReadingResult
import com.tracker.core.result.TimeRange
import com.tracker.core.result.UsageSession
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

        // Deduplicate to one confidence per unique app before combining — sessions from the
        // same app are not independent events, so combining per-session would inflate the result.
        val uniqueAppConfidences = validEvidenceList
            .distinctBy { UsageStatsMetadata.fromMap(it.metadata)?.packageName }
            .map { it.confidence }
        val combinedConfidence = combineProbabilities(uniqueAppConfidences)
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
            occurred = combinedConfidence.toOccurred(minConfidence),
            source = DataSource.USAGE_STATS,
            confidence = combinedConfidence,
            confidenceLevel = combinedConfidence.toConfidenceLevel(),
            timeRange = TimeRange(fromMillis, toMillis),
            durationMinutes = totalDuration,
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