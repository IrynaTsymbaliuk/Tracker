package com.tracker.core.result

import com.tracker.core.types.ConfidenceLevel
import com.tracker.core.types.DataSource

/**
 * Result for distance travelled (walking, running, cycling, etc.) detection.
 *
 * Distance is read from Health Connect and returned as a list of hourly [DistanceSession]
 * buckets, so callers querying multiple days can separate distance by hour (and therefore
 * by day).
 *
 * Health Connect deduplicates across writing apps (Google Fit, Pixel, Fitbit, etc.) using the
 * user's data-source priority configuration before returning each bucket's distance.
 *
 * @property sources Data sources that contributed to this result. Always
 * `[DataSource.HEALTH_CONNECT]`.
 * @property confidence Confidence score (0.99 for Health Connect).
 * @property confidenceLevel Categorical confidence level.
 * @property timeRange The queried time range.
 * @property sessions Hourly distance buckets within [timeRange], sorted by
 * [DistanceSession.startTime] ascending. Hours with no recorded distance are omitted, so the
 * list may be non-contiguous. The final bucket may be shorter than an hour when [timeRange]
 * ends mid-hour.
 *
 * `null` return value means no data available: Health Connect is not installed, the API level
 * is below 26, or the `READ_DISTANCE` permission has not been granted.
 */
data class DistanceResult(
    override val sources: List<DataSource>,
    override val confidence: Float,
    override val confidenceLevel: ConfidenceLevel,
    override val timeRange: TimeRange,
    val sessions: List<DistanceSession>
) : HabitResult() {
    /** Total distance across all buckets, in meters. */
    val totalMeters: Double get() = sessions.sumOf { it.meters }

    /** Total distance across all buckets, in kilometers. */
    val totalKilometers: Double get() = totalMeters / 1000.0
}
