package com.tracker.core.result

/**
 * Represents a single exercise session within the queried time range.
 *
 * Sessions are sourced from Health Connect `ExerciseSessionRecord` and sorted by
 * [startTime] ascending in [ExerciseResult.sessions].
 *
 * ## Deduplication when storing locally
 *
 * Use the tuple ([startTime], [endTime]) as the deduplication key — both are derived
 * from the record boundaries and are stable across overlapping query windows.
 *
 * ## 0-minute sessions
 *
 * No minimum-duration filter is applied; very short sessions (under 1 minute) have
 * [durationMinutes] = 0 but are still present in the list, so session counts stay
 * accurate.
 *
 * ## Exercise type
 *
 * Both [exerciseTypeId] and [exerciseType] are exposed:
 * - Prefer [exerciseTypeId] (the raw `ExerciseSessionRecord.EXERCISE_TYPE_*` int)
 *   for filtering, persistence, and any machine-readable logic — it is stable.
 * - Use [exerciseType] for display. It is the lowercase name from
 *   `ExerciseSessionRecord.EXERCISE_TYPE_INT_TO_STRING_MAP` (e.g. `"running"`,
 *   `"strength_training"`, `"biking"`).
 *
 * @property startTime Session start time (milliseconds since epoch).
 * @property endTime Session end time (milliseconds since epoch).
 * @property durationMinutes Session duration in whole minutes. 0 for sessions under 1 minute.
 * @property exerciseTypeId Raw Health Connect exercise type constant.
 * @property exerciseType Human-readable lowercase name of the exercise type.
 */
data class ExerciseSession(
    val startTime: Long,
    val endTime: Long,
    val durationMinutes: Int,
    val exerciseTypeId: Int,
    val exerciseType: String
)
