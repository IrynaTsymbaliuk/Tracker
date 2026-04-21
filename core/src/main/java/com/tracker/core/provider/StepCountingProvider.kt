package com.tracker.core.provider

import android.os.Build
import com.tracker.core.collector.CollectorException
import com.tracker.core.collector.HealthConnectStepCollector
import com.tracker.core.result.StepCountingResult
import com.tracker.core.result.TimeRange
import com.tracker.core.result.toConfidenceLevel

/**
 * Detects physical activity by counting steps via Health Connect.
 *
 * Returns null if Health Connect is unavailable, the API level is below 26,
 * or the `READ_STEPS` permission has not been granted.
 *
 * To enable Health Connect, request [HealthConnectStepCollector.READ_STEPS_PERMISSION]
 * at runtime via [PermissionController.createRequestPermissionResultContract] before querying.
 */
class StepCountingProvider internal constructor(
    private val healthConnectCollector: HealthConnectStepCollector
) : MetricProvider<StepCountingResult> {

    override suspend fun query(
        fromMillis: Long,
        toMillis: Long,
        minConfidence: Float
    ): StepCountingResult? {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return null

        val evidence = try {
            healthConnectCollector.collect(fromMillis, toMillis)
        } catch (e: CollectorException) {
            return null
        }

        return StepCountingResult(
            sources = listOf(evidence.source),
            confidence = evidence.confidence,
            confidenceLevel = evidence.confidence.toConfidenceLevel(),
            timeRange = TimeRange(fromMillis, toMillis),
            steps = evidence.steps
        )
    }
}
