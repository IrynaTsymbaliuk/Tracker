package com.tracker.core.provider

import com.tracker.core.result.HabitResult

interface MetricProvider<T : HabitResult> {

    /**
     * Query the metric for the specified time range.
     *
     * This method:
     * 1. Collects data from all available sources
     * 2. Aggregates and deduplicates evidence
     * 3. Calculates confidence scores
     * 4. Filters by minConfidence threshold
     * 5. Builds the result
     *
     * @param fromMillis Start time in milliseconds since epoch (inclusive)
     * @param toMillis End time in milliseconds since epoch (inclusive)
     * @param minConfidence Minimum confidence threshold (0.0-1.0)
     * @return HabitResult
     */
    suspend fun query(
        fromMillis: Long,
        toMillis: Long,
        minConfidence: Float
    ): T?
}
