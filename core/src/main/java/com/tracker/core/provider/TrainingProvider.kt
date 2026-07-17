package com.tracker.core.provider

import android.os.Build
import com.tracker.core.collector.CollectorException
import com.tracker.core.collector.HealthConnectExerciseCollector
import com.tracker.core.collector.HealthConnectTrainingCollector
import com.tracker.core.result.TimeRange
import com.tracker.core.result.TrainingResult
import com.tracker.core.result.TrainingSession

/**
 * Reads planned training sessions from Health Connect.
 *
 * The Health Connect planned-exercise feature and the `READ_PLANNED_EXERCISE` permission are
 * both required. Unsupported Health Connect versions, unavailable services, and missing
 * permission degrade to `null`, consistent with Tracker's other Health Connect providers.
 */
class TrainingProvider internal constructor(
    private val healthConnectCollector: HealthConnectTrainingCollector
) : MetricProvider<TrainingResult> {

    override suspend fun query(fromMillis: Long, toMillis: Long): TrainingResult? {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return null

        val evidence = try {
            healthConnectCollector.collect(fromMillis, toMillis)
        } catch (e: CollectorException) {
            return null
        }

        if (evidence.sessions.isEmpty()) return null

        val sessions = evidence.sessions
            .map { record ->
                TrainingSession(
                    record = record,
                    exerciseType = HealthConnectExerciseCollector.exerciseTypeName(record.exerciseType)
                )
            }
            .sortedBy { it.startTime }

        return TrainingResult(
            sources = listOf(evidence.source),
            timeRange = TimeRange(fromMillis, toMillis),
            durationMinutes = sessions.sumOf { it.durationMinutes },
            sessions = sessions
        )
    }
}
