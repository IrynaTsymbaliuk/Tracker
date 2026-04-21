@file:OptIn(ExperimentalMindfulnessSessionApi::class)

package com.tracker.core.collector

import android.content.Context
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.feature.ExperimentalMindfulnessSessionApi
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.MindfulnessSessionRecord
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
 * Collects meditation-session evidence from Health Connect via
 * [MindfulnessSessionRecord].
 *
 * Requires API 26+ — [java.time.Instant] and [HealthConnectClient] both mandate it.
 * [MindfulnessSessionRecord] itself was added in a recent `androidx.health.connect:connect-client`
 * release; on devices or Health Connect installations that do not support the record type,
 * this collector either throws [SystemServiceUnavailableException] (caught by the provider)
 * or returns an empty list so the caller can fall back to [UsageEventsCollector].
 *
 * **Permissions:**
 * - `android.permission.health.READ_MINDFULNESS` (requested at runtime via
 *   [androidx.health.connect.client.PermissionController.createRequestPermissionResultContract]).
 *
 * @throws SystemServiceUnavailableException if Health Connect is not installed or
 * [HealthConnectClient.SDK_AVAILABLE] is not returned by [HealthConnectClient.getSdkStatus],
 * or if the MindfulnessSessionRecord API is missing on this client version.
 * @throws PermissionDeniedException if [READ_MINDFULNESS_PERMISSION] has not been granted.
 */
@RequiresApi(Build.VERSION_CODES.O)
class HealthConnectMindfulnessCollector(
    private val context: Context,
    private val dispatcher: CoroutineDispatcher = Dispatchers.IO
) {

    companion object {
        internal const val HEALTH_CONNECT_CONFIDENCE = 0.99f

        val READ_MINDFULNESS_PERMISSION: String =
            HealthPermission.getReadPermission(MindfulnessSessionRecord::class)
    }

    private val client: HealthConnectClient by lazy {
        HealthConnectClient.getOrCreate(context)
    }

    /**
     * Returns one [DurationEvidence] per [MindfulnessSessionRecord] that overlaps
     * `[fromMillis, toMillis]`, with [DataSource.HEALTH_CONNECT] and confidence
     * [HEALTH_CONNECT_CONFIDENCE].
     *
     * @param fromMillis Start of time range (inclusive, milliseconds since epoch)
     * @param toMillis End of time range (inclusive, milliseconds since epoch)
     * @return list of [DurationEvidence] — empty if no sessions are recorded in the range
     */
    suspend fun collect(fromMillis: Long, toMillis: Long): List<DurationEvidence> =
        withContext(dispatcher) {
            Log.d(TAG, "Collecting mindfulness sessions for range: $fromMillis–$toMillis")

            val status = HealthConnectClient.getSdkStatus(context)
            if (status != HealthConnectClient.SDK_AVAILABLE) {
                Log.d(TAG, "Health Connect unavailable, SDK status: $status")
                throw SystemServiceUnavailableException("HealthConnect (SDK status: $status)")
            }

            val granted = try {
                client.permissionController.getGrantedPermissions()
            } catch (e: NoSuchMethodError) {
                // Older client / Health Connect installations predate the mindfulness API
                throw SystemServiceUnavailableException("HealthConnect.MindfulnessSessionRecord")
            } catch (e: ClassNotFoundException) {
                throw SystemServiceUnavailableException("HealthConnect.MindfulnessSessionRecord")
            }

            if (READ_MINDFULNESS_PERMISSION !in granted) {
                Log.d(TAG, "READ_MINDFULNESS permission not granted")
                throw PermissionDeniedException("android.permission.health.READ_MINDFULNESS")
            }

            val request = ReadRecordsRequest(
                recordType = MindfulnessSessionRecord::class,
                timeRangeFilter = TimeRangeFilter.between(
                    startTime = Instant.ofEpochMilli(fromMillis),
                    endTime = Instant.ofEpochMilli(toMillis)
                )
            )

            val records = try {
                client.readRecords(request).records
            } catch (e: NoSuchMethodError) {
                throw SystemServiceUnavailableException("HealthConnect.MindfulnessSessionRecord")
            } catch (e: ClassNotFoundException) {
                throw SystemServiceUnavailableException("HealthConnect.MindfulnessSessionRecord")
            }

            Log.d(TAG, "Collected ${records.size} mindfulness sessions from Health Connect")

            records.map { record ->
                val startMillis = record.startTime.toEpochMilli()
                val endMillis = record.endTime.toEpochMilli()
                val durationMinutes =
                    TimeUnit.MILLISECONDS.toMinutes(endMillis - startMillis).toInt()

                DurationEvidence(
                    source = DataSource.HEALTH_CONNECT,
                    confidence = HEALTH_CONNECT_CONFIDENCE,
                    metadata = emptyMap(),
                    durationMinutes = durationMinutes,
                    startTimeMillis = startMillis,
                    endTimeMillis = endMillis
                )
            }
        }
}
