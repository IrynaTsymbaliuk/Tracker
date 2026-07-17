package com.tracker.core.collector

import android.content.Context
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.StepsRecord
import androidx.health.connect.client.request.AggregateGroupByDurationRequest
import androidx.health.connect.client.time.TimeRangeFilter
import com.tracker.core.common.TAG
import com.tracker.core.model.StepBucket
import com.tracker.core.model.StepEvidence
import com.tracker.core.types.DataSource
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.Duration
import java.time.Instant

/**
 * Collects step count evidence from Health Connect (primary source).
 *
 * Requires API 26+ — [java.time.Instant] and [HealthConnectClient] both mandate it.
 * The call site ([StepCountingProvider]) guards with a [Build.VERSION.SDK_INT] check
 * so this class is never instantiated on older devices.
 *
 * **Permissions:**
 * - `android.permission.health.READ_STEPS` (requested at runtime via
 *   [PermissionController.createRequestPermissionResultContract])
 *
 * @throws SystemServiceUnavailableException if Health Connect is not installed or
 * [HealthConnectClient.SDK_AVAILABLE] is not returned by [HealthConnectClient.getSdkStatus].
 * @throws PermissionDeniedException if [READ_STEPS_PERMISSION] has not been granted.
 */
@RequiresApi(Build.VERSION_CODES.O)
class HealthConnectStepCollector(
    private val context: Context,
    private val dispatcher: CoroutineDispatcher = Dispatchers.IO
) {

    companion object {
        val READ_STEPS_PERMISSION: String =
            HealthPermission.getReadPermission(StepsRecord::class)
    }

    private val client: HealthConnectClient by lazy {
        HealthConnectClient.getOrCreate(context)
    }

    /**
     * Returns steps recorded by Health Connect in [[fromMillis], [toMillis]], sliced into
     * 1-hour buckets. Uses [AggregateGroupByDurationRequest] with [StepsRecord.COUNT_TOTAL]
     * so Health Connect deduplicates across writing apps (Google Fit, Pixel step counter,
     * Fitbit, etc.) according to the user's data-source priority configuration.
     *
     * Hours with no recorded steps are omitted by Health Connect and therefore absent from
     * [StepEvidence.buckets]. The final bucket may be shorter than an hour when [toMillis]
     * falls mid-hour.
     *
     * @param fromMillis Start of time range (inclusive, milliseconds since epoch)
     * @param toMillis End of time range (inclusive, milliseconds since epoch)
     * @return [StepEvidence] with [DataSource.HEALTH_CONNECT] and the hourly buckets
     */
    suspend fun collect(fromMillis: Long, toMillis: Long): StepEvidence = withContext(dispatcher) {
        Log.d(TAG, "Collecting steps for range: $fromMillis–$toMillis")

        val status = HealthConnectClient.getSdkStatus(context)
        if (status != HealthConnectClient.SDK_AVAILABLE) {
            Log.d(TAG, "Health Connect unavailable, SDK status: $status")
            throw SystemServiceUnavailableException("HealthConnect (SDK status: $status)")
        }

        val granted = client.permissionController.getGrantedPermissions()
        if (READ_STEPS_PERMISSION !in granted) {
            Log.d(TAG, "READ_STEPS permission not granted")
            throw PermissionDeniedException("android.permission.health.READ_STEPS")
        }

        val request = AggregateGroupByDurationRequest(
            metrics = setOf(StepsRecord.COUNT_TOTAL),
            timeRangeFilter = TimeRangeFilter.between(
                startTime = Instant.ofEpochMilli(fromMillis),
                endTime = Instant.ofEpochMilli(toMillis)
            ),
            timeRangeSlicer = Duration.ofHours(1)
        )
        val buckets = client.aggregateGroupByDuration(request).map { group ->
            StepBucket(
                startTime = group.startTime.toEpochMilli(),
                endTime = group.endTime.toEpochMilli(),
                steps = group.result[StepsRecord.COUNT_TOTAL] ?: 0L
            )
        }
        Log.d(TAG, "Collected ${buckets.size} hourly step buckets from Health Connect")

        StepEvidence(
            source = DataSource.HEALTH_CONNECT,
            metadata = emptyMap(),
            buckets = buckets
        )
    }
}
