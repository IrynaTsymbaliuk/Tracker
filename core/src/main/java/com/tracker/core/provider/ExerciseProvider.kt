package com.tracker.core.provider

import android.os.Build
import com.tracker.core.collector.CollectorException
import com.tracker.core.collector.ExerciseMetadata
import com.tracker.core.collector.HealthConnectExerciseCollector
import com.tracker.core.result.ExerciseResult
import com.tracker.core.result.ExerciseSession
import com.tracker.core.result.TimeRange
import com.tracker.core.result.toConfidenceLevel
import com.tracker.core.types.DataSource

/**
 * Detects exercise activity from Health Connect `ExerciseSessionRecord`.
 *
 * Returns `null` if Health Connect is unavailable, the API level is below 26,
 * the `READ_EXERCISE` permission has not been granted, or no sessions exist in
 * the queried window. No minimum-duration filter is applied — short sessions
 * (0 minutes rounded) are kept so session counts stay accurate.
 *
 * To enable Health Connect, request
 * [HealthConnectExerciseCollector.READ_EXERCISE_PERMISSION] at runtime via
 * [androidx.health.connect.client.PermissionController.createRequestPermissionResultContract]
 * before querying.
 */
class ExerciseProvider internal constructor(
    private val healthConnectCollector: HealthConnectExerciseCollector
) : MetricProvider<ExerciseResult> {

    override suspend fun query(
        fromMillis: Long,
        toMillis: Long,
        minConfidence: Float
    ): ExerciseResult? {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return null

        val evidenceList = try {
            healthConnectCollector.collect(fromMillis, toMillis)
        } catch (e: CollectorException) {
            return null
        }

        if (evidenceList.isEmpty()) return null

        val sessions = evidenceList.mapNotNull { ev ->
            val metadata = ExerciseMetadata.fromMap(ev.metadata) ?: return@mapNotNull null
            ExerciseSession(
                startTime = ev.startTimeMillis,
                endTime = ev.endTimeMillis,
                durationMinutes = ev.durationMinutes,
                exerciseTypeId = metadata.exerciseTypeId,
                exerciseType = metadata.exerciseType
            )
        }.sortedBy { it.startTime }

        if (sessions.isEmpty()) return null

        val totalDuration = sessions.sumOf { it.durationMinutes }
        // All evidence shares the same HealthConnect confidence, so the weighted
        // average collapses to that value. We still call weightedAverage() to stay
        // consistent with the other providers and to correctly handle any future
        // per-session confidence adjustments.
        val combinedConfidence = weightedAverage(evidenceList)

        return ExerciseResult(
            sources = listOf(DataSource.HEALTH_CONNECT),
            confidence = combinedConfidence,
            confidenceLevel = combinedConfidence.toConfidenceLevel(),
            timeRange = TimeRange(fromMillis, toMillis),
            durationMinutes = totalDuration,
            sessions = sessions
        )
    }
}
