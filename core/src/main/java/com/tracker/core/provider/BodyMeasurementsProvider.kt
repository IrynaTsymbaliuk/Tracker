package com.tracker.core.provider

import android.os.Build
import com.tracker.core.collector.CollectorException
import com.tracker.core.collector.HealthConnectBodyMeasurementsCollector
import com.tracker.core.result.BodyMeasurementsResult
import com.tracker.core.result.TimeRange

/** Reads all supported body-measurement record types from Health Connect. */
class BodyMeasurementsProvider internal constructor(
    private val healthConnectCollector: HealthConnectBodyMeasurementsCollector
) : MetricProvider<BodyMeasurementsResult> {

    override suspend fun query(fromMillis: Long, toMillis: Long): BodyMeasurementsResult? {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return null

        val evidence = try {
            healthConnectCollector.collect(fromMillis, toMillis)
        } catch (e: CollectorException) {
            return null
        }

        val result = BodyMeasurementsResult(
            sources = listOf(evidence.source),
            timeRange = TimeRange(fromMillis, toMillis),
            weightRecords = evidence.weightRecords,
            bodyFatRecords = evidence.bodyFatRecords,
            leanBodyMassRecords = evidence.leanBodyMassRecords,
            boneMassRecords = evidence.boneMassRecords,
            bodyWaterMassRecords = evidence.bodyWaterMassRecords,
            basalMetabolicRateRecords = evidence.basalMetabolicRateRecords,
            heightRecords = evidence.heightRecords
        )
        return result.takeUnless { it.recordCount == 0 }
    }
}
