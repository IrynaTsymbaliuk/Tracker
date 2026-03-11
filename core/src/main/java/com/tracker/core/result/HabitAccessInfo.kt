package com.tracker.core.result

import com.tracker.core.types.Metric

/**
 * Access information for a specific habit metric.
 *
 * Shows what data sources are available, what's missing, and the overall
 * reliability for tracking this habit.
 *
 * @property metric The habit metric this information applies to
 * @property currentReliability Current reliability with granted sources only
 * @property potentialReliability Potential reliability if all sources were available
 * @property sources All registered sources for this metric with their status
 */
data class HabitAccessInfo(
    val metric: Metric,
    val currentReliability: ReliabilityLevel,
    val potentialReliability: ReliabilityLevel,
    val sources: List<SourceAccessInfo>
)
