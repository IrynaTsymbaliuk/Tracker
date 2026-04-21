package com.tracker.core.collector

import android.content.Context
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.ExerciseSessionRecord
import androidx.health.connect.client.request.ReadRecordsRequest
import androidx.health.connect.client.time.TimeRangeFilter
import com.tracker.core.common.TAG
import com.tracker.core.model.DurationEvidence
import com.tracker.core.types.DataSource
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.Instant
import java.util.concurrent.TimeUnit

/**
 * Collects exercise-session evidence from Health Connect via
 * [ExerciseSessionRecord].
 *
 * Requires API 26+ — [java.time.Instant] and [HealthConnectClient] both mandate it.
 * The call site ([com.tracker.core.provider.ExerciseProvider]) guards with a
 * [Build.VERSION.SDK_INT] check so this class is never instantiated on older devices.
 *
 * **Permissions:**
 * - `android.permission.health.READ_EXERCISE` (requested at runtime via
 *   [androidx.health.connect.client.PermissionController.createRequestPermissionResultContract]).
 *
 * @throws SystemServiceUnavailableException if Health Connect is not installed or
 * [HealthConnectClient.SDK_AVAILABLE] is not returned by [HealthConnectClient.getSdkStatus].
 * @throws PermissionDeniedException if [READ_EXERCISE_PERMISSION] has not been granted.
 */
@RequiresApi(Build.VERSION_CODES.O)
class HealthConnectExerciseCollector(
    private val context: Context,
    private val dispatcher: CoroutineDispatcher = Dispatchers.IO
) {

    companion object {
        internal const val HEALTH_CONNECT_CONFIDENCE = 0.99f

        val READ_EXERCISE_PERMISSION: String =
            HealthPermission.getReadPermission(ExerciseSessionRecord::class)

        /**
         * Maps Health Connect [ExerciseSessionRecord]`.EXERCISE_TYPE_*` int constants
         * to their snake_case string names. We roll our own map because
         * `ExerciseSessionRecord.EXERCISE_TYPE_INT_TO_STRING_MAP` is annotated with
         * `@RestrictTo(LIBRARY_GROUP)` and cannot be referenced outside the
         * `androidx.health.connect:connect-client` library.
         *
         * Unknown or future types fall back to `"other"` at the call site.
         */
        private val EXERCISE_TYPE_NAMES: Map<Int, String> = mapOf(
            ExerciseSessionRecord.EXERCISE_TYPE_BADMINTON to "badminton",
            ExerciseSessionRecord.EXERCISE_TYPE_BASEBALL to "baseball",
            ExerciseSessionRecord.EXERCISE_TYPE_BASKETBALL to "basketball",
            ExerciseSessionRecord.EXERCISE_TYPE_BIKING to "biking",
            ExerciseSessionRecord.EXERCISE_TYPE_BIKING_STATIONARY to "biking_stationary",
            ExerciseSessionRecord.EXERCISE_TYPE_BOOT_CAMP to "boot_camp",
            ExerciseSessionRecord.EXERCISE_TYPE_BOXING to "boxing",
            ExerciseSessionRecord.EXERCISE_TYPE_CALISTHENICS to "calisthenics",
            ExerciseSessionRecord.EXERCISE_TYPE_CRICKET to "cricket",
            ExerciseSessionRecord.EXERCISE_TYPE_DANCING to "dancing",
            ExerciseSessionRecord.EXERCISE_TYPE_ELLIPTICAL to "elliptical",
            ExerciseSessionRecord.EXERCISE_TYPE_EXERCISE_CLASS to "exercise_class",
            ExerciseSessionRecord.EXERCISE_TYPE_FENCING to "fencing",
            ExerciseSessionRecord.EXERCISE_TYPE_FOOTBALL_AMERICAN to "football_american",
            ExerciseSessionRecord.EXERCISE_TYPE_FOOTBALL_AUSTRALIAN to "football_australian",
            ExerciseSessionRecord.EXERCISE_TYPE_FRISBEE_DISC to "frisbee_disc",
            ExerciseSessionRecord.EXERCISE_TYPE_GOLF to "golf",
            ExerciseSessionRecord.EXERCISE_TYPE_GUIDED_BREATHING to "guided_breathing",
            ExerciseSessionRecord.EXERCISE_TYPE_GYMNASTICS to "gymnastics",
            ExerciseSessionRecord.EXERCISE_TYPE_HANDBALL to "handball",
            ExerciseSessionRecord.EXERCISE_TYPE_HIGH_INTENSITY_INTERVAL_TRAINING
                to "high_intensity_interval_training",
            ExerciseSessionRecord.EXERCISE_TYPE_HIKING to "hiking",
            ExerciseSessionRecord.EXERCISE_TYPE_ICE_HOCKEY to "ice_hockey",
            ExerciseSessionRecord.EXERCISE_TYPE_ICE_SKATING to "ice_skating",
            ExerciseSessionRecord.EXERCISE_TYPE_MARTIAL_ARTS to "martial_arts",
            ExerciseSessionRecord.EXERCISE_TYPE_OTHER_WORKOUT to "other_workout",
            ExerciseSessionRecord.EXERCISE_TYPE_PADDLING to "paddling",
            ExerciseSessionRecord.EXERCISE_TYPE_PARAGLIDING to "paragliding",
            ExerciseSessionRecord.EXERCISE_TYPE_PILATES to "pilates",
            ExerciseSessionRecord.EXERCISE_TYPE_RACQUETBALL to "racquetball",
            ExerciseSessionRecord.EXERCISE_TYPE_ROCK_CLIMBING to "rock_climbing",
            ExerciseSessionRecord.EXERCISE_TYPE_ROLLER_HOCKEY to "roller_hockey",
            ExerciseSessionRecord.EXERCISE_TYPE_ROWING to "rowing",
            ExerciseSessionRecord.EXERCISE_TYPE_ROWING_MACHINE to "rowing_machine",
            ExerciseSessionRecord.EXERCISE_TYPE_RUGBY to "rugby",
            ExerciseSessionRecord.EXERCISE_TYPE_RUNNING to "running",
            ExerciseSessionRecord.EXERCISE_TYPE_RUNNING_TREADMILL to "running_treadmill",
            ExerciseSessionRecord.EXERCISE_TYPE_SAILING to "sailing",
            ExerciseSessionRecord.EXERCISE_TYPE_SCUBA_DIVING to "scuba_diving",
            ExerciseSessionRecord.EXERCISE_TYPE_SKATING to "skating",
            ExerciseSessionRecord.EXERCISE_TYPE_SKIING to "skiing",
            ExerciseSessionRecord.EXERCISE_TYPE_SNOWBOARDING to "snowboarding",
            ExerciseSessionRecord.EXERCISE_TYPE_SNOWSHOEING to "snowshoeing",
            ExerciseSessionRecord.EXERCISE_TYPE_SOCCER to "soccer",
            ExerciseSessionRecord.EXERCISE_TYPE_SOFTBALL to "softball",
            ExerciseSessionRecord.EXERCISE_TYPE_SQUASH to "squash",
            ExerciseSessionRecord.EXERCISE_TYPE_STAIR_CLIMBING to "stair_climbing",
            ExerciseSessionRecord.EXERCISE_TYPE_STAIR_CLIMBING_MACHINE
                to "stair_climbing_machine",
            ExerciseSessionRecord.EXERCISE_TYPE_STRENGTH_TRAINING to "strength_training",
            ExerciseSessionRecord.EXERCISE_TYPE_STRETCHING to "stretching",
            ExerciseSessionRecord.EXERCISE_TYPE_SURFING to "surfing",
            ExerciseSessionRecord.EXERCISE_TYPE_SWIMMING_OPEN_WATER to "swimming_open_water",
            ExerciseSessionRecord.EXERCISE_TYPE_SWIMMING_POOL to "swimming_pool",
            ExerciseSessionRecord.EXERCISE_TYPE_TABLE_TENNIS to "table_tennis",
            ExerciseSessionRecord.EXERCISE_TYPE_TENNIS to "tennis",
            ExerciseSessionRecord.EXERCISE_TYPE_VOLLEYBALL to "volleyball",
            ExerciseSessionRecord.EXERCISE_TYPE_WALKING to "walking",
            ExerciseSessionRecord.EXERCISE_TYPE_WATER_POLO to "water_polo",
            ExerciseSessionRecord.EXERCISE_TYPE_WEIGHTLIFTING to "weightlifting",
            ExerciseSessionRecord.EXERCISE_TYPE_WHEELCHAIR to "wheelchair",
            ExerciseSessionRecord.EXERCISE_TYPE_YOGA to "yoga"
        )
    }

    private val client: HealthConnectClient by lazy {
        HealthConnectClient.getOrCreate(context)
    }

    /**
     * Returns one [DurationEvidence] per [ExerciseSessionRecord] that overlaps
     * `[fromMillis, toMillis]`, with [DataSource.HEALTH_CONNECT] and confidence
     * [HEALTH_CONNECT_CONFIDENCE]. Exercise type is carried in the evidence
     * metadata bag via [ExerciseMetadata].
     *
     * @param fromMillis Start of time range (inclusive, milliseconds since epoch)
     * @param toMillis End of time range (inclusive, milliseconds since epoch)
     * @return list of [DurationEvidence] — empty if no sessions are recorded in the range
     */
    suspend fun collect(fromMillis: Long, toMillis: Long): List<DurationEvidence> =
        withContext(dispatcher) {
            Log.d(TAG, "Collecting exercise sessions for range: $fromMillis–$toMillis")

            val status = HealthConnectClient.getSdkStatus(context)
            if (status != HealthConnectClient.SDK_AVAILABLE) {
                Log.d(TAG, "Health Connect unavailable, SDK status: $status")
                throw SystemServiceUnavailableException("HealthConnect (SDK status: $status)")
            }

            val granted = client.permissionController.getGrantedPermissions()
            if (READ_EXERCISE_PERMISSION !in granted) {
                Log.d(TAG, "READ_EXERCISE permission not granted")
                throw PermissionDeniedException("android.permission.health.READ_EXERCISE")
            }

            val request = ReadRecordsRequest(
                recordType = ExerciseSessionRecord::class,
                timeRangeFilter = TimeRangeFilter.between(
                    startTime = Instant.ofEpochMilli(fromMillis),
                    endTime = Instant.ofEpochMilli(toMillis)
                )
            )

            val records = client.readRecords(request).records
            Log.d(TAG, "Collected ${records.size} exercise sessions from Health Connect")

            records.map { record ->
                val startMillis = record.startTime.toEpochMilli()
                val endMillis = record.endTime.toEpochMilli()
                val durationMinutes =
                    TimeUnit.MILLISECONDS.toMinutes(endMillis - startMillis).toInt()

                val typeId = record.exerciseType
                val typeName = EXERCISE_TYPE_NAMES[typeId] ?: "other"

                val metadata = ExerciseMetadata(
                    exerciseTypeId = typeId,
                    exerciseType = typeName
                )

                DurationEvidence(
                    source = DataSource.HEALTH_CONNECT,
                    confidence = HEALTH_CONNECT_CONFIDENCE,
                    metadata = metadata.toMap(),
                    durationMinutes = durationMinutes,
                    startTimeMillis = startMillis,
                    endTimeMillis = endMillis
                )
            }
        }
}
