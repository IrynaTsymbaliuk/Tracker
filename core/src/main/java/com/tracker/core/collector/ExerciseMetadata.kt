package com.tracker.core.collector

/**
 * Type-safe metadata for exercise evidence coming out of
 * [HealthConnectExerciseCollector].
 *
 * The Evidence pipeline uses a generic `Map<String, Any>` bag for source-specific
 * data; this helper converts to/from that bag so call sites stay type-safe.
 *
 * @property exerciseTypeId The raw Health Connect exercise type constant
 * (e.g. `ExerciseSessionRecord.EXERCISE_TYPE_RUNNING`). Stable across SDK versions.
 * @property exerciseType Human-readable lowercase name as returned by
 * `ExerciseSessionRecord.EXERCISE_TYPE_INT_TO_STRING_MAP` (e.g. `"running"`,
 * `"strength_training"`). Prefer the id for filtering and logic; use the name for display.
 */
data class ExerciseMetadata(
    val exerciseTypeId: Int,
    val exerciseType: String
) {
    /** Converts to map for Evidence compatibility. */
    fun toMap(): Map<String, Any> = mapOf(
        KEY_EXERCISE_TYPE_ID to exerciseTypeId,
        KEY_EXERCISE_TYPE to exerciseType
    )

    companion object {
        const val KEY_EXERCISE_TYPE_ID = "exerciseTypeId"
        const val KEY_EXERCISE_TYPE = "exerciseType"

        /**
         * Extracts [ExerciseMetadata] from a metadata map.
         *
         * @return [ExerciseMetadata] if all fields are valid, `null` otherwise.
         */
        fun fromMap(map: Map<String, Any>): ExerciseMetadata? {
            return try {
                ExerciseMetadata(
                    exerciseTypeId = map[KEY_EXERCISE_TYPE_ID] as? Int ?: return null,
                    exerciseType = map[KEY_EXERCISE_TYPE] as? String ?: return null
                )
            } catch (e: Exception) {
                null
            }
        }
    }
}
