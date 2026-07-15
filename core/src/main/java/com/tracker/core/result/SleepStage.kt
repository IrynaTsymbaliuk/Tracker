package com.tracker.core.result

import com.tracker.core.types.SleepStageType
import java.util.concurrent.TimeUnit

/**
 * A single stage interval within a [SleepSession] (e.g. a stretch of deep sleep or an awakening),
 * as reported by Health Connect's `SleepSessionRecord.Stage`.
 *
 * @property startTime Stage start (milliseconds since epoch).
 * @property endTime Stage end (milliseconds since epoch).
 * @property type The kind of stage (deep, REM, awake, etc.). See [SleepStageType].
 */
data class SleepStage(
    val startTime: Long,
    val endTime: Long,
    val type: SleepStageType
) {
    /** Length of this stage in whole minutes. */
    val durationMinutes: Long
        get() = TimeUnit.MILLISECONDS.toMinutes(endTime - startTime)
}
