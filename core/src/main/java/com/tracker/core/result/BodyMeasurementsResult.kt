package com.tracker.core.result

import androidx.health.connect.client.records.BasalMetabolicRateRecord
import androidx.health.connect.client.records.BodyFatRecord
import androidx.health.connect.client.records.BodyWaterMassRecord
import androidx.health.connect.client.records.BoneMassRecord
import androidx.health.connect.client.records.HeightRecord
import androidx.health.connect.client.records.LeanBodyMassRecord
import androidx.health.connect.client.records.WeightRecord
import com.tracker.core.types.DataSource

/**
 * Body measurements sourced from Health Connect.
 *
 * Each property contains complete, timestamped Health Connect records sorted by [time] ascending.
 * Body-composition apps commonly write the related values seconds apart (and can omit any value),
 * so Tracker preserves the independent record streams rather than pairing them by timestamp.
 *
 * Health Connect has no muscle-percentage record. [leanBodyMassRecords] exposes its closest
 * available measurement, `LeanBodyMassRecord`, as a mass value.
 */
data class BodyMeasurementsResult(
    override val sources: List<DataSource>,
    override val timeRange: TimeRange,
    val weightRecords: List<WeightRecord> = emptyList(),
    val bodyFatRecords: List<BodyFatRecord> = emptyList(),
    val leanBodyMassRecords: List<LeanBodyMassRecord> = emptyList(),
    val boneMassRecords: List<BoneMassRecord> = emptyList(),
    val bodyWaterMassRecords: List<BodyWaterMassRecord> = emptyList(),
    val basalMetabolicRateRecords: List<BasalMetabolicRateRecord> = emptyList(),
    val heightRecords: List<HeightRecord> = emptyList()
) : HabitResult() {
    /** Total number of raw Health Connect records across every measurement type. */
    val recordCount: Int
        get() = weightRecords.size + bodyFatRecords.size + leanBodyMassRecords.size +
            boneMassRecords.size + bodyWaterMassRecords.size + basalMetabolicRateRecords.size +
            heightRecords.size
}
