package com.tracker.core.provider

import com.tracker.core.model.DurationEvidence
import com.tracker.core.result.HabitResult

interface MetricProvider<T : HabitResult> {

    /**
     * @param fromMillis Start time in milliseconds since epoch (inclusive)
     * @param toMillis End time in milliseconds since epoch (inclusive)
     * @param minConfidence Minimum confidence threshold (0.0–1.0); results below this are considered not occurred
     * @return result for the given range, or null if no data is available
     */
    suspend fun query(
        fromMillis: Long,
        toMillis: Long,
        minConfidence: Float
    ): T?
}

internal fun weightedAverage(usages: List<DurationEvidence>): Float {
    if (usages.isEmpty()) return 0f
    val totalDuration: Double = usages.sumOf { it.durationMinutes }.toDouble()
    if (totalDuration == 0.0) return 0f
    return usages.sumOf { it.durationMinutes.toDouble() / totalDuration * it.confidence }.toFloat()
}
