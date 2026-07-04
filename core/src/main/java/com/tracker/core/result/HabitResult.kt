package com.tracker.core.result

import com.tracker.core.types.DataSource

/**
 * Base class for all habit detection results.
 *
 * @property sources The data sources that contributed to this result. A result fused from
 * multiple collectors (e.g. HealthConnect mindfulness sessions + UsageStats app sessions)
 * lists all contributing sources here; single-source results still use a one-element list.
 * @property timeRange The queried time range
 */
sealed class HabitResult {
    abstract val sources: List<DataSource>
    abstract val timeRange: TimeRange
}
