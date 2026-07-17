package com.tracker.core.result

import androidx.health.connect.client.records.PlannedExerciseSessionRecord
import java.util.concurrent.TimeUnit

/**
 * One planned training session from Health Connect.
 *
 * [record] is the complete [PlannedExerciseSessionRecord] returned by Health Connect. It is
 * deliberately preserved rather than flattened, so consumers receive every available field:
 * exact timestamps and zone offsets, title/notes, completion link, metadata, and the full nested
 * block → step → completion-goal/performance-target structure.
 *
 * The convenience properties below match the rest of Tracker's session APIs. Use [record] when
 * a Health Connect field is needed directly.
 */
data class TrainingSession(
    val record: PlannedExerciseSessionRecord,
    val exerciseType: String
) {
    /** Planned start time in milliseconds since epoch. */
    val startTime: Long get() = record.startTime.toEpochMilli()

    /** Planned end time in milliseconds since epoch. */
    val endTime: Long get() = record.endTime.toEpochMilli()

    /** Planned duration in whole minutes. */
    val durationMinutes: Long
        get() = TimeUnit.MILLISECONDS.toMinutes(endTime - startTime)

    /** Raw Health Connect `ExerciseSessionRecord.EXERCISE_TYPE_*` value. */
    val exerciseTypeId: Int get() = record.exerciseType
}
