package com.tracker.core.provider

import com.tracker.core.collector.UsageStatsCollector
import com.tracker.core.collector.UsageStatsMetadata
import com.tracker.core.config.KnownApps
import com.tracker.core.result.AppInfo
import com.tracker.core.result.SocialMediaResult
import com.tracker.core.result.TimeRange
import com.tracker.core.result.UsageSession
import com.tracker.core.result.toConfidenceLevel
import com.tracker.core.result.toOccurred
import com.tracker.core.types.DataSource

/**
 * Detects social media usage from app foreground time.
 *
 * **Confidence scoring**:
 * - Traditional social apps (Facebook, Instagram, TikTok): 0.90-0.95
 * - Professional/borderline (LinkedIn, Reddit): 0.80-0.85
 * - Messaging apps (WhatsApp, Telegram): 0.75
 *
 * Combined confidence uses weighted average by duration.
 */
class SocialMediaProvider internal constructor(
    private val usageStatsCollector: UsageStatsCollector
) : MetricProvider<SocialMediaResult> {

    override suspend fun query(
        fromMillis: Long,
        toMillis: Long,
        minConfidence: Float
    ): SocialMediaResult? {

        val evidenceList = usageStatsCollector.collect(
            fromMillis,
            toMillis,
            KnownApps.socialMedia
        ).ifEmpty { return null }

        val validEvidenceList =
            evidenceList.filter { it.durationMinutes > 0 }.ifEmpty { return null }

        val combinedConfidence = weightedAverage(validEvidenceList)

        val totalDuration = validEvidenceList.sumOf { it.durationMinutes }

        val apps = validEvidenceList.mapNotNull { ev ->
            val metadata = UsageStatsMetadata.fromMap(ev.metadata) ?: return@mapNotNull null
            AppInfo(metadata.packageName, metadata.appName)
        }.distinctBy { it.packageName }

        val sessions = validEvidenceList
            .mapNotNull { ev ->
                val metadata = UsageStatsMetadata.fromMap(ev.metadata) ?: return@mapNotNull null
                UsageSession(
                    startTime = ev.startTimeMillis,
                    endTime = ev.endTimeMillis,
                    durationMinutes = ev.durationMinutes,
                    packageName = metadata.packageName,
                    appName = metadata.appName
                )
            }

        return SocialMediaResult(
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
