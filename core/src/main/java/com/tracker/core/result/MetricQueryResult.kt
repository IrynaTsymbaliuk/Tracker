package com.tracker.core.result

/**
 * Common interface for metric-specific query results.
 * Internal use only - allows HabitEngine to return different metric types.
 */
internal sealed interface MetricQueryResult {
    val dataQuality: DataQuality
}
