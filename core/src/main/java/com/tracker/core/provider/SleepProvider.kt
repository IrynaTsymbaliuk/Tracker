package com.tracker.core.provider

import android.os.Build
import com.tracker.core.collector.CollectorException
import com.tracker.core.collector.HealthConnectSleepCollector
import com.tracker.core.result.SleepResult
import com.tracker.core.result.SleepSession
import com.tracker.core.result.SleepStage
import com.tracker.core.result.TimeRange

/**
 * Detects sleep via Health Connect `SleepSessionRecord`.
 *
 * Returns null if Health Connect is unavailable, the API level is below 26,
 * or the `READ_SLEEP` permission has not been granted.
 *
 * To enable Health Connect, request [HealthConnectSleepCollector.READ_SLEEP_PERMISSION]
 * at runtime via [PermissionController.createRequestPermissionResultContract] before querying.
 */
class SleepProvider internal constructor(
    private val healthConnectCollector: HealthConnectSleepCollector
) : MetricProvider<SleepResult> {

    override suspend fun query(
        fromMillis: Long,
        toMillis: Long
    ): SleepResult? {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return null

        val evidence = try {
            healthConnectCollector.collect(fromMillis, toMillis)
        } catch (e: CollectorException) {
            return null
        }

        return SleepResult(
            sources = listOf(evidence.source),
            timeRange = TimeRange(fromMillis, toMillis),
            sessions = evidence.sessions.map { session ->
                SleepSession(
                    startTime = session.startTime,
                    endTime = session.endTime,
                    stages = session.stages.map { stage ->
                        SleepStage(
                            startTime = stage.startTime,
                            endTime = stage.endTime,
                            type = stage.type
                        )
                    }
                )
            }
        )
    }
}
