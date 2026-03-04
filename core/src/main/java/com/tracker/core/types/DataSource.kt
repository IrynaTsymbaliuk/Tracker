package com.tracker.core.types

/**
 * Represents the source from which behavioral data was collected.
 * Initially supports only USAGE_STATS for language learning tracking.
 */
enum class DataSource {
    USAGE_STATS,
    UNKNOWN
}
