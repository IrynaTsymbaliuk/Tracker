package com.tracker.core.result

/**
 * A single hourly bucket of step counts within the queried time range.
 *
 * Buckets are produced by Health Connect's `aggregateGroupByDuration` with a 1-hour slice,
 * anchored to the query window's `from` instant (which [com.tracker.core.Tracker.queryStepCounting]
 * sets to local midnight). The final bucket may be shorter than an hour when the query
 * window's `to` falls mid-hour — [endTime] reflects the actual data range.
 *
 * Hours with no recorded steps are omitted from [StepCountingResult.sessions]; the list is
 * not guaranteed to be contiguous.
 *
 * @property startTime Bucket start (milliseconds since epoch).
 * @property endTime Bucket end (milliseconds since epoch).
 * @property steps Steps recorded in this hour, already deduplicated by Health Connect
 * across writing apps according to the user's data-source priority configuration.
 */
data class StepSession(
    val startTime: Long,
    val endTime: Long,
    val steps: Long
)
