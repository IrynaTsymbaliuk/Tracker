package com.tracker.core.config

/**
 * Metadata about a known app.
 *
 * @property packageName Android package name
 * @property confidenceMultiplier Base confidence score for this app (0.0 to 1.0)
 *
 * Note: Minimum session duration is defined per-habit in HabitConfig, not per-app.
 */
data class AppMetadata(
    val packageName: String,
    val confidenceMultiplier: Float
)
