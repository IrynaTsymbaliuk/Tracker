package com.tracker.core.types

/**
 * Represents the reliability level of a habit detection result.
 */
enum class ConfidenceLevel {
    HIGH,    // ≥ 0.75
    MEDIUM,  // 0.50 - 0.74
    LOW      // < 0.50
}
