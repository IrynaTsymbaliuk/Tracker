package com.tracker.core.provider

import android.os.Build
import com.tracker.core.collector.CollectorException
import com.tracker.core.collector.HealthConnectDistanceCollector
import com.tracker.core.result.DistanceResult
import com.tracker.core.result.DistanceSession
import com.tracker.core.result.TimeRange
import com.tracker.core.result.toConfidenceLevel

/**
 * Detects distance travelled (walking, running, cycling, etc.) via Health Connect.
 *
 * Returns null if Health Connect is unavailable, the API level is below 26,
 * or the `READ_DISTANCE` permission has not been granted.
 *
 * To enable Health Connect, request [HealthConnectDistanceCollector.READ_DISTANCE_PERMISSION]
 * at runtime via [PermissionController.createRequestPermissionResultContract] before querying.
 */
class DistanceProvider internal constructor(
    private val healthConnectCollector: HealthConnectDistanceCollector
) : MetricProvider<DistanceResult> {

    override suspend fun query(
        fromMillis: Long,
        toMillis: Long,
        minConfidence: Float
    ): DistanceResult? {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return null

        val evidence = try {
            healthConnectCollector.collect(fromMillis, toMillis)
        } catch (e: CollectorException) {
            return null
        }

        return DistanceResult(
            sources = listOf(evidence.source),
            confidence = evidence.confidence,
            confidenceLevel = evidence.confidence.toConfidenceLevel(),
            timeRange = TimeRange(fromMillis, toMillis),
            sessions = evidence.buckets.map { bucket ->
                DistanceSession(
                    startTime = bucket.startTime,
                    endTime = bucket.endTime,
                    meters = bucket.meters
                )
            }
        )
    }
}
