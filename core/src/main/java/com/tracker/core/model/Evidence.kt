package com.tracker.core.model

import com.tracker.core.types.DataSource
import com.tracker.core.types.SleepStageType

/**
 * Evidence represents a single piece of data collected from a source.
 * Collectors return Evidence objects, which are then aggregated to produce final results.
 *
 * @property source Where the data came from
 * @property confidence Internal source weight for this evidence (0.0 to 1.0);
 * public result types expose [DataSource] values rather than result-level scores.
 * @property metadata Source-specific additional data (app name, package, language, etc.)
 */
sealed class Evidence {
    abstract val source: DataSource
    abstract val confidence: Float
    abstract val metadata: Map<String, Any>
}

data class DurationEvidence(
    override val source: DataSource,
    override val confidence: Float,
    override val metadata: Map<String, Any>,
    val durationMinutes: Int,
    val startTimeMillis: Long,
    val endTimeMillis: Long
) : Evidence()

data class CounterEvidence(
    override val source: DataSource,
    override val confidence: Float,
    override val metadata: Map<String, Any>,
    val counter: Int
) : Evidence()

data class StepEvidence(
    override val source: DataSource,
    override val confidence: Float,
    override val metadata: Map<String, Any>,
    val buckets: List<StepBucket>
) : Evidence()

data class StepBucket(
    val startTime: Long,
    val endTime: Long,
    val steps: Long
)

data class DistanceEvidence(
    override val source: DataSource,
    override val confidence: Float,
    override val metadata: Map<String, Any>,
    val buckets: List<DistanceBucket>
) : Evidence()

data class DistanceBucket(
    val startTime: Long,
    val endTime: Long,
    val meters: Double
)

data class SleepEvidence(
    override val source: DataSource,
    override val confidence: Float,
    override val metadata: Map<String, Any>,
    val sessions: List<SleepSessionData>
) : Evidence()

/**
 * Raw sleep-session data as read from a single Health Connect `SleepSessionRecord`.
 * [startTime] is when the user fell asleep (session start) and [endTime] is when they woke
 * (session end). [stages] is empty when the writing source recorded no stage breakdown.
 */
data class SleepSessionData(
    val startTime: Long,
    val endTime: Long,
    val stages: List<SleepStageData>
)

/** Raw sleep-stage interval as read from a `SleepSessionRecord.Stage`. */
data class SleepStageData(
    val startTime: Long,
    val endTime: Long,
    val type: SleepStageType
)
