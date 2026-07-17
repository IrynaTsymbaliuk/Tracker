package com.tracker.core.config

/**
 * Metadata about a known app.
 *
 * @property packageName Android package name
 * @property confidenceMultiplier Catalogue reference value for this app (0.0 to 1.0). It is not
 * used by the evidence pipeline or result aggregation.
 */
data class AppMetadata(
    val packageName: String,
    val confidenceMultiplier: Float
)
