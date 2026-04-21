package com.tracker.core.result

import com.tracker.core.types.ConfidenceLevel
import com.tracker.core.types.DataSource

/**
 * Result for exercise habit detection.
 *
 * Sourced exclusively from Health Connect `ExerciseSessionRecord`. Confidence is
 * fixed at `0.99` — Health Connect exercise records are authoritative entries
 * written by fitness apps or logged manually by the user.
 *
 * Callers can derive per-type breakdowns directly from [sessions]:
 *
 * ```
 * val durationByType: Map<String, Int> = sessions
 *     .groupBy { it.exerciseType }
 *     .mapValues { (_, s) -> s.sumOf { it.durationMinutes } }
 * ```
 *
 * @property sources Data sources that contributed to this result (always contains
 * [DataSource.HEALTH_CONNECT] for this habit).
 * @property confidence Combined confidence score (0.99 when sourced from Health Connect).
 * @property confidenceLevel Categorical confidence level.
 * @property timeRange The queried time range.
 * @property durationMinutes Total exercise time across all sessions.
 * @property sessions Individual exercise sessions, sorted by [ExerciseSession.startTime]
 * ascending.
 *
 * `null` return value means no data is available: Health Connect is not installed,
 * the `READ_EXERCISE` permission has not been granted, or the API level is below 26.
 */
data class ExerciseResult(
    override val sources: List<DataSource>,
    override val confidence: Float,
    override val confidenceLevel: ConfidenceLevel,
    override val timeRange: TimeRange,
    val durationMinutes: Int,
    val sessions: List<ExerciseSession> = emptyList()
) : HabitResult()
