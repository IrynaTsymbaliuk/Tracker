package com.tracker.core.collector

import android.content.Context
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.SleepSessionRecord
import androidx.health.connect.client.request.ReadRecordsRequest
import androidx.health.connect.client.time.TimeRangeFilter
import com.tracker.core.common.TAG
import com.tracker.core.model.SleepEvidence
import com.tracker.core.model.SleepSessionData
import com.tracker.core.model.SleepStageData
import com.tracker.core.types.DataSource
import com.tracker.core.types.SleepStageType
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.Instant

/**
 * Collects sleep-session evidence from Health Connect via [SleepSessionRecord].
 *
 * Unlike step counting (which aggregates into hourly buckets), sleep is read as raw sessions so
 * callers get the actual fall-asleep time ([SleepSessionRecord.startTime]), wake time
 * ([SleepSessionRecord.endTime]), and the per-stage breakdown ([SleepSessionRecord.stages]) that
 * quality/duration figures are derived from. This mirrors [HealthConnectExerciseCollector].
 *
 * Requires API 26+ — [java.time.Instant] and [HealthConnectClient] both mandate it.
 * The call site ([com.tracker.core.provider.SleepProvider]) guards with a
 * [Build.VERSION.SDK_INT] check so this class is never instantiated on older devices.
 *
 * **Permissions:**
 * - `android.permission.health.READ_SLEEP` (requested at runtime via
 *   [androidx.health.connect.client.PermissionController.createRequestPermissionResultContract])
 *
 * @throws SystemServiceUnavailableException if Health Connect is not installed or
 * [HealthConnectClient.SDK_AVAILABLE] is not returned by [HealthConnectClient.getSdkStatus].
 * @throws PermissionDeniedException if [READ_SLEEP_PERMISSION] has not been granted.
 */
@RequiresApi(Build.VERSION_CODES.O)
class HealthConnectSleepCollector(
    private val context: Context,
    private val dispatcher: CoroutineDispatcher = Dispatchers.IO
) {

    companion object {
        val READ_SLEEP_PERMISSION: String =
            HealthPermission.getReadPermission(SleepSessionRecord::class)

        /**
         * Maps Health Connect `SleepSessionRecord.STAGE_TYPE_*` int constants to [SleepStageType].
         * Unknown or future stage types fall back to [SleepStageType.UNKNOWN].
         */
        private val STAGE_TYPES: Map<Int, SleepStageType> = mapOf(
            SleepSessionRecord.STAGE_TYPE_UNKNOWN to SleepStageType.UNKNOWN,
            SleepSessionRecord.STAGE_TYPE_AWAKE to SleepStageType.AWAKE,
            SleepSessionRecord.STAGE_TYPE_AWAKE_IN_BED to SleepStageType.AWAKE_IN_BED,
            SleepSessionRecord.STAGE_TYPE_OUT_OF_BED to SleepStageType.OUT_OF_BED,
            SleepSessionRecord.STAGE_TYPE_SLEEPING to SleepStageType.SLEEPING,
            SleepSessionRecord.STAGE_TYPE_LIGHT to SleepStageType.LIGHT,
            SleepSessionRecord.STAGE_TYPE_DEEP to SleepStageType.DEEP,
            SleepSessionRecord.STAGE_TYPE_REM to SleepStageType.REM
        )
    }

    private val client: HealthConnectClient by lazy {
        HealthConnectClient.getOrCreate(context)
    }

    /**
     * Returns one [SleepSessionData] per [SleepSessionRecord] that overlaps
     * `[fromMillis, toMillis]`, sorted by start time. Each session carries its raw fall-asleep
     * and wake timestamps plus any recorded stages; higher-level figures (duration asleep,
     * efficiency, quality) are computed from these in [com.tracker.core.result.SleepSession].
     *
     * Sessions from multiple writing apps are returned as-is (no cross-source deduplication),
     * matching [HealthConnectExerciseCollector].
     *
     * @param fromMillis Start of time range (inclusive, milliseconds since epoch)
     * @param toMillis End of time range (inclusive, milliseconds since epoch)
     * @return [SleepEvidence] with [DataSource.HEALTH_CONNECT] and the sleep sessions
     */
    suspend fun collect(fromMillis: Long, toMillis: Long): SleepEvidence =
        withContext(dispatcher) {
            Log.d(TAG, "Collecting sleep sessions for range: $fromMillis–$toMillis")

            val status = HealthConnectClient.getSdkStatus(context)
            if (status != HealthConnectClient.SDK_AVAILABLE) {
                Log.d(TAG, "Health Connect unavailable, SDK status: $status")
                throw SystemServiceUnavailableException("HealthConnect (SDK status: $status)")
            }

            val granted = client.permissionController.getGrantedPermissions()
            if (READ_SLEEP_PERMISSION !in granted) {
                Log.d(TAG, "READ_SLEEP permission not granted")
                throw PermissionDeniedException("android.permission.health.READ_SLEEP")
            }

            val request = ReadRecordsRequest(
                recordType = SleepSessionRecord::class,
                timeRangeFilter = TimeRangeFilter.between(
                    startTime = Instant.ofEpochMilli(fromMillis),
                    endTime = Instant.ofEpochMilli(toMillis)
                )
            )

            val records = client.readRecords(request).records
            Log.d(TAG, "Collected ${records.size} sleep sessions from Health Connect")

            val sessions = records
                .map { record ->
                    SleepSessionData(
                        startTime = record.startTime.toEpochMilli(),
                        endTime = record.endTime.toEpochMilli(),
                        stages = record.stages.map { stage ->
                            SleepStageData(
                                startTime = stage.startTime.toEpochMilli(),
                                endTime = stage.endTime.toEpochMilli(),
                                type = STAGE_TYPES[stage.stage] ?: SleepStageType.UNKNOWN
                            )
                        }
                    )
                }
                .sortedBy { it.startTime }

            SleepEvidence(
                source = DataSource.HEALTH_CONNECT,
                metadata = emptyMap(),
                sessions = sessions
            )
        }
}
