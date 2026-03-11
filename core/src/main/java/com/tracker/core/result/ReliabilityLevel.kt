package com.tracker.core.result

/**
 * Overall reliability assessment for data quality.
 * Ordered from worst (NONE=0) to best (HIGH=3) for ordinal comparison.
 */
enum class ReliabilityLevel {
    /**
     * No sources available or no metrics requested.
     */
    NONE,

    /**
     * Few sources available, low confidence.
     */
    LOW,

    /**
     * Some sources available, moderate confidence.
     */
    MEDIUM,

    /**
     * All required sources available with high confidence.
     */
    HIGH
}
