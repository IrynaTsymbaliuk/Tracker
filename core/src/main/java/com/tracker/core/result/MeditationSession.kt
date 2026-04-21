package com.tracker.core.result

import com.tracker.core.types.DataSource

/**
 * Represents a single meditation session within the queried time range.
 *
 * A meditation session can originate from one of two sources:
 *
 * - **HealthConnect** ([DataSource.HEALTH_CONNECT]): a `MindfulnessSessionRecord` written by
 *   a meditation app (or manually logged by the user). [packageName] and [appName] are `null`
 *   because HealthConnect does not expose the originating app in the record.
 * - **UsageStats** ([DataSource.USAGE_STATS]): a foreground session of a known meditation
 *   app (see [com.tracker.core.config.KnownApps.meditation]). [packageName] and [appName]
 *   are populated.
 *
 * When the same real-world session is detected by both sources (e.g. Calm writes a
 * mindfulness record while the user is in the app), the two are deduplicated into a single
 * [MeditationSession] whose [sources] list contains both [DataSource.HEALTH_CONNECT] and
 * [DataSource.USAGE_STATS]. Timestamps in that case are taken from HealthConnect (which
 * reflects the explicit start/end of the meditation), while [packageName]/[appName] are
 * taken from UsageStats if available.
 *
 * Sessions are sorted by [startTime] ascending in [MeditationResult.sessions].
 *
 * ## Deduplication when storing locally
 *
 * Unlike [UsageSession], meditation sessions can come from multiple sources. Use the
 * tuple ([startTime], [endTime]) as the deduplication key — both are derived from real
 * events (HealthConnect record boundaries or `ACTIVITY_RESUMED`/`ACTIVITY_PAUSED`) and
 * are stable across overlapping query windows.
 *
 * ## 0-minute sessions
 *
 * Meditation sessions are not filtered by minimum duration — even a 0-minute foreground
 * session of a meditation app (a quick app open) is kept, matching the behavior of
 * [UsageSession] to keep session counts accurate.
 *
 * @property startTime Session start time (milliseconds since epoch).
 * @property endTime Session end time (milliseconds since epoch).
 * @property durationMinutes Session duration in whole minutes. 0 for sessions under 1 minute.
 * @property sources Data sources that reported this session. Contains two elements when a
 * HealthConnect record and a UsageStats session were merged.
 * @property packageName App package identifier (e.g. `com.calm.android`) when a UsageStats
 * session contributed to this [MeditationSession]; `null` for HealthConnect-only sessions.
 * @property appName Human-readable app name (e.g. `Calm`) when a UsageStats session
 * contributed; `null` for HealthConnect-only sessions.
 */
data class MeditationSession(
    val startTime: Long,
    val endTime: Long,
    val durationMinutes: Int,
    val sources: List<DataSource>,
    val packageName: String? = null,
    val appName: String? = null
)
