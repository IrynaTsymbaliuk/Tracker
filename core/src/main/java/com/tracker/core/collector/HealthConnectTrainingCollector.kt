package com.tracker.core.collector

import android.content.Context
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.HealthConnectFeatures
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.PlannedExerciseSessionRecord
import androidx.health.connect.client.request.ReadRecordsRequest
import androidx.health.connect.client.time.TimeRangeFilter
import com.tracker.core.common.TAG
import com.tracker.core.model.TrainingEvidence
import com.tracker.core.types.DataSource
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.Instant

/**
 * Collects planned training sessions from Health Connect via
 * [PlannedExerciseSessionRecord].
 *
 * Planned exercise is a separately versioned Health Connect feature. It is guarded before the
 * read because older Health Connect implementations do not support this record type. Records may
 * be scheduled in the future; callers choose the queried window through [fromMillis] and
 * [toMillis].
 *
 * @throws SystemServiceUnavailableException if Health Connect or planned exercise is unavailable.
 * @throws PermissionDeniedException if [READ_PLANNED_EXERCISE_PERMISSION] has not been granted.
 */
@RequiresApi(Build.VERSION_CODES.O)
class HealthConnectTrainingCollector(
    private val context: Context,
    private val dispatcher: CoroutineDispatcher = Dispatchers.IO
) {

    companion object {
        val READ_PLANNED_EXERCISE_PERMISSION: String =
            HealthPermission.getReadPermission(PlannedExerciseSessionRecord::class)
    }

    private val client: HealthConnectClient by lazy {
        HealthConnectClient.getOrCreate(context)
    }

    /**
     * Returns every planned-exercise record that overlaps `[fromMillis, toMillis]`.
     * Pagination is followed so a broad query does not silently omit training sessions.
     */
    suspend fun collect(fromMillis: Long, toMillis: Long): TrainingEvidence =
        withContext(dispatcher) {
            Log.d(TAG, "Collecting planned training sessions for range: $fromMillis–$toMillis")

            val status = HealthConnectClient.getSdkStatus(context)
            if (status != HealthConnectClient.SDK_AVAILABLE) {
                Log.d(TAG, "Health Connect unavailable, SDK status: $status")
                throw SystemServiceUnavailableException("HealthConnect (SDK status: $status)")
            }

            if (client.features.getFeatureStatus(HealthConnectFeatures.FEATURE_PLANNED_EXERCISE) !=
                HealthConnectFeatures.FEATURE_STATUS_AVAILABLE
            ) {
                Log.d(TAG, "Health Connect planned exercise feature is unavailable")
                throw SystemServiceUnavailableException("HealthConnect.PlannedExerciseSessionRecord")
            }

            val granted = client.permissionController.getGrantedPermissions()
            if (READ_PLANNED_EXERCISE_PERMISSION !in granted) {
                Log.d(TAG, "READ_PLANNED_EXERCISE permission not granted")
                throw PermissionDeniedException("android.permission.health.READ_PLANNED_EXERCISE")
            }

            val timeRange = TimeRangeFilter.between(
                startTime = Instant.ofEpochMilli(fromMillis),
                endTime = Instant.ofEpochMilli(toMillis)
            )
            val records = mutableListOf<PlannedExerciseSessionRecord>()
            var pageToken: String? = null
            do {
                val response = client.readRecords(
                    ReadRecordsRequest(
                        recordType = PlannedExerciseSessionRecord::class,
                        timeRangeFilter = timeRange,
                        pageToken = pageToken
                    )
                )
                records += response.records
                pageToken = response.pageToken
            } while (pageToken != null)

            Log.d(TAG, "Collected ${records.size} planned training sessions from Health Connect")
            TrainingEvidence(
                source = DataSource.HEALTH_CONNECT,
                metadata = emptyMap(),
                sessions = records.sortedBy { it.startTime }
            )
        }
}
