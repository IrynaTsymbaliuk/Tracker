package com.tracker.core.provider

import com.tracker.core.result.HabitResult

interface MetricProvider<T : HabitResult> {

    /**
     * @param fromMillis Start time in milliseconds since epoch (inclusive)
     * @param toMillis End time in milliseconds since epoch (inclusive)
     * @return result for the given range, or null if no data is available
     */
    suspend fun query(
        fromMillis: Long,
        toMillis: Long
    ): T?
}
