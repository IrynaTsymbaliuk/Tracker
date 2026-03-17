package com.tracker.core.engine

import android.content.Context
import com.tracker.core.collector.LetterboxdCollector
import com.tracker.core.permission.PermissionManager
import com.tracker.core.provider.LanguageLearningProvider
import com.tracker.core.provider.MetricProvider
import com.tracker.core.provider.MovieWatchingProvider
import com.tracker.core.provider.ReadingProvider
import com.tracker.core.collector.UsageStateCollector
import com.tracker.core.result.HabitResult
import com.tracker.core.types.Metric

/**
 * Central coordinator for habit tracking.
 *
 * Simplified architecture using MetricProvider pattern:
 * - Each metric has ONE provider that handles collection + aggregation
 * - Providers encapsulate all logic for their metric
 * - Cleaner than separate Collector + Aggregator layers
 */
class HabitEngine internal constructor(
    private val minConfidence: Float,
    private val providers: Map<Metric, MetricProvider<*>>
) {

    companion object {

        fun create(
            context: Context,
            minConfidence: Float,
            letterboxdUsername: String? = null
        ): HabitEngine {
            val permissionManager = PermissionManager(context)

            val providers = mutableMapOf(
                Metric.LANGUAGE_LEARNING to LanguageLearningProvider(UsageStateCollector(context, permissionManager)),
                Metric.READING to ReadingProvider(UsageStateCollector(context, permissionManager)),
                Metric.MOVIE_WATCHING to MovieWatchingProvider(LetterboxdCollector(context), letterboxdUsername)
            )

            return HabitEngine(minConfidence, providers)
        }
    }

    /**
     * Query a specific metric for the specified time range.
     *
     * @param metric The metric to query
     * @param fromMillis Start time in milliseconds since epoch (inclusive)
     * @param toMillis End time in milliseconds since epoch (inclusive)
     * @return HabitResult containing the metric-specific result
     */
    internal suspend fun queryMetric(
        metric: Metric,
        fromMillis: Long,
        toMillis: Long
    ): HabitResult? {
        val provider = providers[metric]
            ?: throw IllegalStateException("No provider for $metric")

        return provider.query(fromMillis, toMillis, minConfidence)
    }
}
