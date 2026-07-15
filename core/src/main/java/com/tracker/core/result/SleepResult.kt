package com.tracker.core.result

import com.tracker.core.types.DataSource

/**
 * Result for sleep detection.
 *
 * Sleep is read from Health Connect `SleepSessionRecord`s as a list of [SleepSession]s — one per
 * night or nap in the queried window — each exposing when the user fell asleep, when they woke,
 * the per-stage breakdown, sleep efficiency, and a derived quality band.
 *
 * Sessions are returned as written by their source(s) with no cross-source deduplication, so if
 * two apps both logged the same night you will see two overlapping sessions (matching how
 * [ExerciseResult] treats exercise sessions).
 *
 * @property sources Data sources that contributed to this result. Always
 * `[DataSource.HEALTH_CONNECT]`.
 * @property timeRange The queried time range.
 * @property sessions Sleep sessions overlapping [timeRange], sorted by [SleepSession.startTime]
 * ascending. Empty when Health Connect returned no sessions in the window.
 *
 * `null` return value means no data available: Health Connect is not installed, the API level is
 * below 26, or the `READ_SLEEP` permission has not been granted.
 */
data class SleepResult(
    override val sources: List<DataSource>,
    override val timeRange: TimeRange,
    val sessions: List<SleepSession>
) : HabitResult() {
    /** Total time actually asleep across all sessions, in minutes. */
    val totalSleepMinutes: Long get() = sessions.sumOf { it.asleepMinutes }

    /** Total time actually asleep across all sessions, in hours. */
    val totalSleepHours: Double get() = totalSleepMinutes / 60.0
}
