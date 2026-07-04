package com.tracker.core.result

import com.tracker.core.types.DataSource

/**
 * Result for step counting detection.
 *
 * Steps are read from Health Connect and returned as a list of hourly [StepSession] buckets,
 * so callers querying multiple days can separate steps by hour (and therefore by day).
 *
 * Health Connect deduplicates across writing apps (Google Fit, Pixel step counter, Fitbit,
 * etc.) using the user's data-source priority configuration before returning each bucket's
 * count.
 *
 * @property sources Data sources that contributed to this result. Always
 * `[DataSource.HEALTH_CONNECT]`.
 * @property timeRange The queried time range.
 * @property sessions Hourly step buckets within [timeRange], sorted by [StepSession.startTime]
 * ascending. Hours with no recorded steps are omitted, so the list may be non-contiguous.
 * The final bucket may be shorter than an hour when [timeRange] ends mid-hour.
 *
 * `null` return value means no data available: Health Connect is not installed, the API
 * level is below 26, or the `READ_STEPS` permission has not been granted.
 */
data class StepCountingResult(
    override val sources: List<DataSource>,
    override val timeRange: TimeRange,
    val sessions: List<StepSession>
) : HabitResult() {
    val totalSteps: Long get() = sessions.sumOf { it.steps }
}
