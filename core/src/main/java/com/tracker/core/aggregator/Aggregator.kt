package com.tracker.core.aggregator

import com.tracker.core.model.Evidence
import com.tracker.core.result.HabitResult

/**
 * Interface for evidence aggregators.
 * Aggregators combine multiple pieces of evidence into a single result for a specific day.
 *
 * Aggregators are responsible for:
 * - Deduplicating overlapping evidence
 * - Combining confidence scores using probability math
 * - Applying penalties for weak-only evidence
 * - Calculating total duration
 * - Determining occurred status based on thresholds
 */
interface Aggregator<T : HabitResult> {
    /**
     * Aggregate evidence for a single day into a result.
     *
     * @param dayMillis Timestamp representing the day (can be start of day)
     * @param evidence List of all evidence for this day
     * @return Aggregated result, or null if no evidence available
     */
    fun aggregate(dayMillis: Long, evidence: List<Evidence>): T?
}
