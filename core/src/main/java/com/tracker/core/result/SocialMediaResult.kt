package com.tracker.core.result

import com.tracker.core.types.ConfidenceLevel
import com.tracker.core.types.DataSource

/**
 * Result for social media usage detection.
 *
 * Tracks time spent in social networking apps including traditional platforms
 * (Facebook, Instagram, Twitter), messaging apps (WhatsApp, Telegram), and
 * community platforms (Reddit, Discord).
 *
 * @property sources Data sources that contributed to this result (always contains
 * [DataSource.USAGE_STATS] for this habit)
 * @property confidence Combined confidence score
 * @property confidenceLevel Categorical confidence level
 * @property timeRange The queried time range
 * @property durationMinutes Total time spent across all sessions
 * @property sessions Individual foreground sessions, sorted by [UsageSession.startTime] ascending.
 * See [UsageSession] for deduplication key guidance when storing sessions locally.
 */
data class SocialMediaResult(
    override val sources: List<DataSource>,
    override val confidence: Float,
    override val confidenceLevel: ConfidenceLevel,
    override val timeRange: TimeRange,
    val durationMinutes: Int,
    val sessions: List<UsageSession> = emptyList()
) : HabitResult()
