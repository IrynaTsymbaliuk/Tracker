package com.tracker.core.model

import androidx.health.connect.client.records.PlannedExerciseSessionRecord
import com.tracker.core.types.DataSource

/**
 * Planned training sessions read from Health Connect without dropping record data.
 *
 * The records remain intact until they are exposed by [com.tracker.core.result.TrainingSession],
 * including their metadata and nested blocks/steps.
 */
data class TrainingEvidence(
    override val source: DataSource,
    override val metadata: Map<String, Any>,
    val sessions: List<PlannedExerciseSessionRecord>
) : Evidence()
