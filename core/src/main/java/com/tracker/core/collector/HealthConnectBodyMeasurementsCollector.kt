package com.tracker.core.collector

import android.content.Context
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.BasalMetabolicRateRecord
import androidx.health.connect.client.records.BodyFatRecord
import androidx.health.connect.client.records.BodyWaterMassRecord
import androidx.health.connect.client.records.BoneMassRecord
import androidx.health.connect.client.records.HeightRecord
import androidx.health.connect.client.records.LeanBodyMassRecord
import androidx.health.connect.client.records.Record
import androidx.health.connect.client.records.WeightRecord
import androidx.health.connect.client.request.ReadRecordsRequest
import androidx.health.connect.client.time.TimeRangeFilter
import com.tracker.core.common.TAG
import com.tracker.core.model.BodyMeasurementsEvidence
import com.tracker.core.types.DataSource
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.Instant
import kotlin.reflect.KClass

/**
 * Collects raw body measurements from Health Connect.
 *
 * The seven record types are intentionally read independently: Health Connect does not guarantee
 * they share a timestamp, and combining near-by records would invent a scale measurement that was
 * never actually written. A granted subset is supported; callers receive every accessible type.
 *
 * @throws SystemServiceUnavailableException if Health Connect is unavailable.
 * @throws PermissionDeniedException if none of [READ_BODY_MEASUREMENTS_PERMISSIONS] is granted.
 */
@RequiresApi(Build.VERSION_CODES.O)
class HealthConnectBodyMeasurementsCollector(
    private val context: Context,
    private val dispatcher: CoroutineDispatcher = Dispatchers.IO
) {

    companion object {
        val READ_WEIGHT_PERMISSION: String = HealthPermission.getReadPermission(WeightRecord::class)
        val READ_BODY_FAT_PERMISSION: String = HealthPermission.getReadPermission(BodyFatRecord::class)
        val READ_LEAN_BODY_MASS_PERMISSION: String =
            HealthPermission.getReadPermission(LeanBodyMassRecord::class)
        val READ_BONE_MASS_PERMISSION: String = HealthPermission.getReadPermission(BoneMassRecord::class)
        val READ_BODY_WATER_MASS_PERMISSION: String =
            HealthPermission.getReadPermission(BodyWaterMassRecord::class)
        val READ_BASAL_METABOLIC_RATE_PERMISSION: String =
            HealthPermission.getReadPermission(BasalMetabolicRateRecord::class)
        val READ_HEIGHT_PERMISSION: String = HealthPermission.getReadPermission(HeightRecord::class)

        /** Every Health Connect permission used by [collect]. */
        val READ_BODY_MEASUREMENTS_PERMISSIONS: Set<String> = linkedSetOf(
            READ_WEIGHT_PERMISSION,
            READ_BODY_FAT_PERMISSION,
            READ_LEAN_BODY_MASS_PERMISSION,
            READ_BONE_MASS_PERMISSION,
            READ_BODY_WATER_MASS_PERMISSION,
            READ_BASAL_METABOLIC_RATE_PERMISSION,
            READ_HEIGHT_PERMISSION
        )
    }

    private val client: HealthConnectClient by lazy { HealthConnectClient.getOrCreate(context) }

    /** Returns all accessible records that overlap `[fromMillis, toMillis]`, sorted by time. */
    suspend fun collect(fromMillis: Long, toMillis: Long): BodyMeasurementsEvidence =
        withContext(dispatcher) {
            Log.d(TAG, "Collecting body measurements for range: $fromMillis–$toMillis")

            val status = HealthConnectClient.getSdkStatus(context)
            if (status != HealthConnectClient.SDK_AVAILABLE) {
                Log.d(TAG, "Health Connect unavailable, SDK status: $status")
                throw SystemServiceUnavailableException("HealthConnect (SDK status: $status)")
            }

            val granted = client.permissionController.getGrantedPermissions()
            if (granted.none { it in READ_BODY_MEASUREMENTS_PERMISSIONS }) {
                Log.d(TAG, "No body-measurement permission granted")
                throw PermissionDeniedException("android.permission.health.READ_WEIGHT and related body permissions")
            }

            val timeRange = TimeRangeFilter.between(
                startTime = Instant.ofEpochMilli(fromMillis),
                endTime = Instant.ofEpochMilli(toMillis)
            )

            val weightRecords = if (READ_WEIGHT_PERMISSION in granted) {
                readAllRecords(WeightRecord::class, timeRange).sortedBy { it.time }
            } else emptyList()
            val bodyFatRecords = if (READ_BODY_FAT_PERMISSION in granted) {
                readAllRecords(BodyFatRecord::class, timeRange).sortedBy { it.time }
            } else emptyList()
            val leanBodyMassRecords = if (READ_LEAN_BODY_MASS_PERMISSION in granted) {
                readAllRecords(LeanBodyMassRecord::class, timeRange).sortedBy { it.time }
            } else emptyList()
            val boneMassRecords = if (READ_BONE_MASS_PERMISSION in granted) {
                readAllRecords(BoneMassRecord::class, timeRange).sortedBy { it.time }
            } else emptyList()
            val bodyWaterMassRecords = if (READ_BODY_WATER_MASS_PERMISSION in granted) {
                readAllRecords(BodyWaterMassRecord::class, timeRange).sortedBy { it.time }
            } else emptyList()
            val basalMetabolicRateRecords = if (READ_BASAL_METABOLIC_RATE_PERMISSION in granted) {
                readAllRecords(BasalMetabolicRateRecord::class, timeRange).sortedBy { it.time }
            } else emptyList()
            val heightRecords = if (READ_HEIGHT_PERMISSION in granted) {
                readAllRecords(HeightRecord::class, timeRange).sortedBy { it.time }
            } else emptyList()

            val recordCount = weightRecords.size + bodyFatRecords.size + leanBodyMassRecords.size +
                boneMassRecords.size + bodyWaterMassRecords.size + basalMetabolicRateRecords.size +
                heightRecords.size
            Log.d(TAG, "Collected $recordCount body measurements from Health Connect")

            BodyMeasurementsEvidence(
                source = DataSource.HEALTH_CONNECT,
                metadata = emptyMap(),
                weightRecords = weightRecords,
                bodyFatRecords = bodyFatRecords,
                leanBodyMassRecords = leanBodyMassRecords,
                boneMassRecords = boneMassRecords,
                bodyWaterMassRecords = bodyWaterMassRecords,
                basalMetabolicRateRecords = basalMetabolicRateRecords,
                heightRecords = heightRecords
            )
        }

    private suspend fun <T : Record> readAllRecords(
        recordType: KClass<T>,
        timeRange: TimeRangeFilter
    ): List<T> {
        val records = mutableListOf<T>()
        var pageToken: String? = null
        do {
            val response = client.readRecords(
                ReadRecordsRequest(
                    recordType = recordType,
                    timeRangeFilter = timeRange,
                    pageToken = pageToken
                )
            )
            records += response.records
            pageToken = response.pageToken
        } while (pageToken != null)
        return records
    }
}
