package com.tracker.core.model

import androidx.health.connect.client.records.BasalMetabolicRateRecord
import androidx.health.connect.client.records.BodyFatRecord
import androidx.health.connect.client.records.BodyWaterMassRecord
import androidx.health.connect.client.records.BoneMassRecord
import androidx.health.connect.client.records.HeightRecord
import androidx.health.connect.client.records.LeanBodyMassRecord
import androidx.health.connect.client.records.WeightRecord
import com.tracker.core.types.DataSource

/**
 * Raw body-measurement records read from Health Connect.
 *
 * Health Connect stores each measurement type independently. The lists intentionally remain
 * separate so records with nearby, but different, timestamps are never presented as one synthetic
 * scale reading.
 */
data class BodyMeasurementsEvidence(
    override val source: DataSource,
    override val metadata: Map<String, Any>,
    val weightRecords: List<WeightRecord>,
    val bodyFatRecords: List<BodyFatRecord>,
    val leanBodyMassRecords: List<LeanBodyMassRecord>,
    val boneMassRecords: List<BoneMassRecord>,
    val bodyWaterMassRecords: List<BodyWaterMassRecord>,
    val basalMetabolicRateRecords: List<BasalMetabolicRateRecord>,
    val heightRecords: List<HeightRecord>
) : Evidence()
