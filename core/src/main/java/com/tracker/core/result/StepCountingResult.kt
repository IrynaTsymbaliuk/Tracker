package com.tracker.core.result

import com.tracker.core.types.ConfidenceLevel
import com.tracker.core.types.DataSource

/**
 * Result for step counting detection.
 *
 * @property source Primary data source: [DataSource.HEALTH_CONNECT] when available,
 * [DataSource.SENSOR] when falling back to the hardware step-counter.
 * @property confidence Confidence score from the winning data source (0.0 to 1.0).
 * Health Connect yields 0.99; the hardware sensor yields 0.85.
 * @property confidenceLevel Categorical confidence level
 * @property timeRange The queried time range
 * @property steps Total steps counted in the time range.
 *
 * `null` return value means no data available: Health Connect is not installed,
 * the device has no step-counter sensor, or both sources threw a [CollectorException].
 *
 * Note: when [source] is [DataSource.SENSOR], [steps] reflects the delta since midnight
 * of the first queried day only. Historical range queries return 0 for past days because
 * [android.hardware.Sensor.TYPE_STEP_COUNTER] accumulates from the last device reboot
 * and prior-day baselines are not retained.
 */
data class StepCountingResult(
    override val source: DataSource,
    override val confidence: Float,
    override val confidenceLevel: ConfidenceLevel,
    override val timeRange: TimeRange,
    val steps: Long
) : HabitResult()
