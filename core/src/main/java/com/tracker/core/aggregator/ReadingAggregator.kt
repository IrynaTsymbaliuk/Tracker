package com.tracker.core.aggregator

import com.tracker.core.result.AppInfo
import com.tracker.core.result.ReadingResult
import com.tracker.core.types.ConfidenceLevel
import com.tracker.core.types.DataSource

/**
 * Aggregates reading evidence into a single result.
 *
 * Uses common aggregation logic from AbstractHabitAggregator and constructs
 * a ReadingResult with the aggregated data.
 *
 * Note: Deduplication logic will be added when OAuth sources are introduced,
 * where the same reading session could be reported by both UsageStats and OAuth APIs.
 */
internal class ReadingAggregator : AbstractHabitAggregator<ReadingResult>() {

    override fun createResult(
        occurred: Boolean,
        confidence: Float,
        confidenceLevel: ConfidenceLevel,
        durationMinutes: Int?,
        sessionCount: Int?,
        source: DataSource,
        apps: List<AppInfo>
    ): ReadingResult {
        return ReadingResult(
            occurred = occurred,
            confidence = confidence,
            confidenceLevel = confidenceLevel,
            durationMinutes = durationMinutes,
            sessionCount = sessionCount,
            source = source,
            apps = apps,
            title = null  // Always null without OAuth integration
        )
    }
}
