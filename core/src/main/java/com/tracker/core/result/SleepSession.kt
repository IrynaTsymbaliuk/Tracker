package com.tracker.core.result

import com.tracker.core.types.SleepQuality
import com.tracker.core.types.SleepStageType
import java.util.concurrent.TimeUnit

/**
 * A single night (or nap) of sleep, read from one Health Connect `SleepSessionRecord`.
 *
 * The two headline timestamps come straight from the record: [startTime] is when the user fell
 * asleep and [endTime] is when they woke. Everything else — how long they actually slept, how the
 * time split across stages, sleep efficiency, and the derived [quality] band — is computed from
 * [stages].
 *
 * **Sources without stage data:** many trackers write only a session with start/end and no
 * stages. For those, [stages] is empty, [asleepMinutes] falls back to the full time in bed,
 * [efficiency] is `null` (there is no awake data to measure against), and [quality] is
 * [SleepQuality.UNKNOWN].
 *
 * @property startTime When the user fell asleep — session start (milliseconds since epoch).
 * @property endTime When the user woke — session end (milliseconds since epoch).
 * @property stages The stage breakdown, in the order Health Connect returned it. May be empty.
 */
data class SleepSession(
    val startTime: Long,
    val endTime: Long,
    val stages: List<SleepStage>
) {
    /** Total time between falling asleep and waking, in whole minutes (time in bed). */
    val timeInBedMinutes: Long
        get() = TimeUnit.MILLISECONDS.toMinutes(endTime - startTime)

    /**
     * Minutes actually spent asleep: the sum of stages where [SleepStageType.isAsleep] is true.
     * When no stages were recorded, falls back to [timeInBedMinutes] (the whole session is
     * assumed to be sleep, matching Health Connect's `SLEEP_DURATION_TOTAL` behaviour).
     */
    val asleepMinutes: Long
        get() = if (stages.isEmpty()) timeInBedMinutes else stageMinutes { it.isAsleep }

    /** Minutes spent awake during the session (awake / awake-in-bed / out-of-bed stages). */
    val awakeMinutes: Long
        get() = stageMinutes { !it.isAsleep }

    /** Minutes in light sleep. */
    val lightMinutes: Long get() = stageMinutes { it == SleepStageType.LIGHT }

    /** Minutes in deep sleep. */
    val deepMinutes: Long get() = stageMinutes { it == SleepStageType.DEEP }

    /** Minutes in REM sleep. */
    val remMinutes: Long get() = stageMinutes { it == SleepStageType.REM }

    /**
     * Sleep efficiency: the fraction of time in bed spent asleep (`0.0`–`1.0`), or `null` when
     * the session has no stage data (no awake time to measure, so efficiency is unknowable).
     * This is the standard proxy for sleep quality and drives [quality].
     */
    val efficiency: Double?
        get() {
            if (stages.isEmpty()) return null
            val inBed = endTime - startTime
            if (inBed <= 0L) return null
            val asleep = stages.filter { it.type.isAsleep }.sumOf { it.endTime - it.startTime }
            return (asleep.toDouble() / inBed.toDouble()).coerceIn(0.0, 1.0)
        }

    /**
     * A coarse, non-clinical quality band derived from [efficiency]. [SleepQuality.UNKNOWN] when
     * there is no stage data. See [SleepQuality] for the banding and caveats.
     */
    val quality: SleepQuality
        get() = SleepQuality.fromEfficiency(efficiency)

    /** Sums, in whole minutes, the stages whose type matches [predicate]. Computed from millis. */
    private inline fun stageMinutes(predicate: (SleepStageType) -> Boolean): Long {
        val millis = stages.filter { predicate(it.type) }.sumOf { it.endTime - it.startTime }
        return TimeUnit.MILLISECONDS.toMinutes(millis)
    }
}
