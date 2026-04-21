package com.tracker.core.result

import com.tracker.core.types.ConfidenceLevel
import com.tracker.core.types.DataSource

/**
 * Result for meditation habit detection.
 *
 * Meditation is detected from two sources that are merged into one result:
 *
 * - **HealthConnect** `MindfulnessSessionRecord` — authoritative entries written by meditation
 *   apps (Calm, Headspace, etc.) or manually logged by the user. Confidence 0.99.
 * - **UsageStats** foreground sessions — time spent in known meditation apps
 *   (see [com.tracker.core.config.KnownApps.meditation]). Confidence per app as configured
 *   in [com.tracker.core.config.AppMetadata.confidenceMultiplier] (0.85–0.95).
 *
 * When both sources report the same session, they are deduplicated into a single
 * [MeditationSession] whose [MeditationSession.sources] contains both sources.
 *
 * Falls back to UsageStats-only on devices where HealthConnect is unavailable, the
 * `MindfulnessSessionRecord` API is not supported, or the READ_MINDFULNESS permission
 * has not been granted.
 *
 * @property sources Data sources that contributed to this result. Possible values:
 * `[HEALTH_CONNECT]`, `[USAGE_STATS]`, or `[HEALTH_CONNECT, USAGE_STATS]`.
 * @property confidence Combined confidence score (0.0 to 1.0), weighted by session duration.
 * @property confidenceLevel Categorical confidence level.
 * @property timeRange The queried time range.
 * @property durationMinutes Total meditation time across all (deduplicated) sessions.
 * @property sessions Individual meditation sessions, sorted by [MeditationSession.startTime]
 * ascending.
 *
 * `null` return value means no data is available from either source.
 */
data class MeditationResult(
    override val sources: List<DataSource>,
    override val confidence: Float,
    override val confidenceLevel: ConfidenceLevel,
    override val timeRange: TimeRange,
    val durationMinutes: Int,
    val sessions: List<MeditationSession> = emptyList()
) : HabitResult()
