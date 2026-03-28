package com.tracker.core.result

/**
 * Represents a single foreground session for an app within the queried time range.
 *
 * Sessions are sorted by [startTime] ascending in all results that expose them.
 *
 * ## Deduplication when storing locally
 *
 * Use [packageName] + [startTime] as the composite key to deduplicate sessions across
 * overlapping queries:
 * ```
 * INSERT OR IGNORE INTO sessions (packageName, startTime, endTime, durationMinutes, appName)
 * ```
 *
 * **Exception — inferred session starts**: if the app was already open at the start of the
 * query window, no `ACTIVITY_RESUMED` event exists for that session, so [startTime] is set
 * to `result.timeRange.from`. Two queries covering different windows can produce different
 * inferred [startTime] values for the same real session, making [packageName] + [startTime]
 * unreliable. For these sessions ([startTime] == `result.timeRange.from`), use
 * [packageName] + [endTime] as the deduplication key instead — [endTime] always comes from
 * a real `ACTIVITY_PAUSED` event.
 *
 * ## 0-minute sessions
 *
 * Sessions shorter than 1 minute have [durationMinutes] = 0 because duration is stored in
 * whole minutes. They represent real app opens (e.g. a quick notification check) and are
 * included in [sessions] so session counts are accurate. Filter them out for display if
 * needed, but keep them for storage to avoid losing the open event.
 *
 * @property startTime Session start time (milliseconds since epoch). May be inferred as
 * `result.timeRange.from` if the app was already open when the query window began.
 * @property endTime Session end time (milliseconds since epoch). Always derived from a real
 * `ACTIVITY_PAUSED` event, or `result.timeRange.to` if the app was still open at query end.
 * @property durationMinutes Session duration in whole minutes. 0 for sessions under 1 minute.
 * @property packageName App package identifier (e.g. `com.instagram.android`)
 * @property appName Human-readable app name (e.g. `Instagram`)
 */
data class UsageSession(
    val startTime: Long,
    val endTime: Long,
    val durationMinutes: Int,
    val packageName: String,
    val appName: String
)