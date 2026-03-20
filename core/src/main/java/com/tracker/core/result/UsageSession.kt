package com.tracker.core.result

/**
 * Represents a single usage session for reading or language learning.
 *
 * @property startTime Session start time (milliseconds since epoch)
 * @property endTime Session end time (milliseconds since epoch)
 * @property durationMinutes Session duration in minutes
 * @property packageName App package identifier
 * @property appName Human-readable app name
 */
data class UsageSession(
    val startTime: Long,
    val endTime: Long,
    val durationMinutes: Int,
    val packageName: String,
    val appName: String
)
