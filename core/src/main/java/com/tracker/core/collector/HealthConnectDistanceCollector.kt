package com.tracker.core.collector

import android.content.Context
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.DistanceRecord
import androidx.health.connect.client.request.AggregateGroupByDurationRequest
import androidx.health.connect.client.time.TimeRangeFilter
import com.tracker.core.common.TAG
import com.tracker.core.model.DistanceBucket
import com.tracker.core.model.DistanceEvidence
import com.tracker.core.types.DataSource
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.Duration
import java.time.Instant

/**
 * Collects distance evidence from Health Connect (primary source).
 *
 * Requires API 26+ — [java.time.Instant] and [HealthConnectClient] both mandate it.
 * The call site ([com.tracker.core.provider.DistanceProvider]) guards with a
 * [Build.VERSION.SDK_INT] check so this class is never instantiated on older devices.
 *
 * **Permissions:**
 * - `android.permission.health.READ_DISTANCE` (requested at runtime via
 *   [androidx.health.connect.client.PermissionController.createRequestPermissionResultContract])
 *
 * @throws SystemServiceUnavailableException if Health Connect is not installed or
 * [HealthConnectClient.SDK_AVAILABLE] is not returned by [HealthConnectClient.getSdkStatus].
 * @throws PermissionDeniedException if [READ_DISTANCE_PERMISSION] has not been granted.
 */
@RequiresApi(Build.VERSION_CODES.O)
class HealthConnectDistanceCollector(
    private val context: Context,
    private val dispatcher: CoroutineDispatcher = Dispatchers.IO
) {

    companion object {
        val READ_DISTANCE_PERMISSION: String =
            HealthPermission.getReadPermission(DistanceRecord::class)
    }

    private val client: HealthConnectClient by lazy {
        HealthConnectClient.getOrCreate(context)
    }

    /**
     * Returns distance recorded by Health Connect in [[fromMillis], [toMillis]], sliced into
     * 1-hour buckets. Uses [AggregateGroupByDurationRequest] with [DistanceRecord.DISTANCE_TOTAL]
     * so Health Connect deduplicates across writing apps (Google Fit, Pixel, Fitbit, etc.)
     * according to the user's data-source priority configuration.
     *
     * Hours with no recorded distance are omitted by Health Connect and therefore absent from
     * [DistanceEvidence.buckets]. The final bucket may be shorter than an hour when [toMillis]
     * falls mid-hour.
     *
     * @param fromMillis Start of time range (inclusive, milliseconds since epoch)
     * @param toMillis End of time range (inclusive, milliseconds since epoch)
     * @return [DistanceEvidence] with [DataSource.HEALTH_CONNECT] and the hourly buckets
     */
    suspend fun collect(fromMillis: Long, toMillis: Long): DistanceEvidence =
        withContext(dispatcher) {
            Log.d(TAG, "Collecting distance for range: $fromMillis–$toMillis")

            val status = HealthConnectClient.getSdkStatus(context)
            if (status != HealthConnectClient.SDK_AVAILABLE) {
                Log.d(TAG, "Health Connect unavailable, SDK status: $status")
                throw SystemServiceUnavailableException("HealthConnect (SDK status: $status)")
            }

            val granted = client.permissionController.getGrantedPermissions()
            if (READ_DISTANCE_PERMISSION !in granted) {
                Log.d(TAG, "READ_DISTANCE permission not granted")
                throw PermissionDeniedException("android.permission.health.READ_DISTANCE")
            }

            val request = AggregateGroupByDurationRequest(
                metrics = setOf(DistanceRecord.DISTANCE_TOTAL),
                timeRangeFilter = TimeRangeFilter.between(
                    startTime = Instant.ofEpochMilli(fromMillis),
                    endTime = Instant.ofEpochMilli(toMillis)
                ),
                timeRangeSlicer = Duration.ofHours(1)
            )
            val buckets = client.aggregateGroupByDuration(request).map { group ->
                DistanceBucket(
                    startTime = group.startTime.toEpochMilli(),
                    endTime = group.endTime.toEpochMilli(),
                    meters = group.result[DistanceRecord.DISTANCE_TOTAL]?.inMeters ?: 0.0
                )
            }
            Log.d(TAG, "Collected ${buckets.size} hourly distance buckets from Health Connect")

            DistanceEvidence(
                source = DataSource.HEALTH_CONNECT,
                metadata = emptyMap(),
                buckets = buckets
            )
        }
}
