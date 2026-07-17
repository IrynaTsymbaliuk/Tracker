package com.tracker.core.result

import com.tracker.core.types.DataSource

/**
 * Planned training sessions sourced from Health Connect `PlannedExerciseSessionRecord`.
 *
 * A training plan can be scheduled in the future. [timeRange] identifies the exact window used
 * for the query; [sessions] are sorted by planned start time.
 */
data class TrainingResult(
    override val sources: List<DataSource>,
    override val timeRange: TimeRange,
    val durationMinutes: Long,
    val sessions: List<TrainingSession> = emptyList()
) : HabitResult()
