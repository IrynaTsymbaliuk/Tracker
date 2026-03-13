package com.tracker.core.engine

import android.app.Activity
import android.content.Context
import com.tracker.core.aggregator.Aggregator
import com.tracker.core.aggregator.LanguageLearningAggregator
import com.tracker.core.aggregator.ReadingAggregator
import com.tracker.core.collector.Collector
import com.tracker.core.collector.UsageStatsLanguageLearningCollector
import com.tracker.core.collector.UsageStatsReadingCollector
import com.tracker.core.model.Evidence
import com.tracker.core.permission.Permission
import com.tracker.core.permission.PermissionManager
import com.tracker.core.permission.PermissionStatus
import com.tracker.core.result.AccessRequirement
import com.tracker.core.result.AccessStatus
import com.tracker.core.result.DataQuality
import com.tracker.core.result.HabitAccessInfo
import com.tracker.core.result.LanguageLearningMetricResult
import com.tracker.core.result.LanguageLearningResult
import com.tracker.core.result.MetricQueryResult
import com.tracker.core.result.MissingReason
import com.tracker.core.result.MissingSource
import com.tracker.core.result.ReadingMetricResult
import com.tracker.core.result.ReadingResult
import com.tracker.core.result.ReliabilityLevel
import com.tracker.core.result.SourceAccessInfo
import com.tracker.core.types.DataSource
import com.tracker.core.types.Metric
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Central coordinator for habit tracking.
 */
class HabitEngine internal constructor(
    private val minConfidence: Float,
    private val permissionManager: PermissionManager,
    private val collectors: Map<Metric, Collector>,
    private val aggregators: Map<Metric, Aggregator<*>>
) {

    companion object {
        /**
         * Create a HabitEngine with default dependencies.
         */
        fun create(
            context: Context,
            minConfidence: Float
        ): HabitEngine {
            val permissionManager = PermissionManager(context)

            // Instantiate ALL collectors (not filtered by requested metrics)
            val collectors = mapOf(
                Metric.LANGUAGE_LEARNING to UsageStatsLanguageLearningCollector(context, permissionManager),
                Metric.READING to UsageStatsReadingCollector(context, permissionManager)
            )

            // Instantiate ALL aggregators (not filtered by requested metrics)
            val aggregators = mapOf<Metric, Aggregator<*>>(
                Metric.LANGUAGE_LEARNING to LanguageLearningAggregator(),
                Metric.READING to ReadingAggregator()
            )

            return HabitEngine(minConfidence, permissionManager, collectors, aggregators)
        }
    }

    /**
     * Get access requirements for a specific metric.
     *
     * Dynamically builds access info from the metric's collector,
     * following the self-describing collector pattern.
     *
     * @param metric The metric to check access for
     * @return HabitAccessInfo for the metric
     */
    fun getAccessRequirements(metric: Metric): HabitAccessInfo {
        val collector = collectors[metric]
            ?: throw IllegalStateException("No collector registered for metric: $metric")

        // Build source access info from collector's declared requirements
        val sources = collector.sourceRequirements.map { requirement ->
            SourceAccessInfo(
                sourceName = collector.sourceName,
                requirement = requirement,
                status = permissionManager.check(requirement),
                reliabilityContribution = collector.reliabilityContribution,
                description = when (requirement) {
                    is AccessRequirement.SystemPermission ->
                        "System permission: ${requirement.permission}"
                    else -> "Unknown requirement type"
                }
            )
        }

        // Calculate current reliability based on granted sources
        val currentReliability = if (sources.any { it.status == AccessStatus.GRANTED }) {
            // Use the highest reliability among granted sources
            sources
                .filter { it.status == AccessStatus.GRANTED }
                .maxOfOrNull { it.reliabilityContribution }
                ?: ReliabilityLevel.NONE
        } else {
            ReliabilityLevel.NONE
        }

        // Potential reliability is the highest possible if all sources were available
        val potentialReliability = sources
            .maxOfOrNull { it.reliabilityContribution }
            ?: ReliabilityLevel.NONE

        return HabitAccessInfo(
            metric = metric,
            currentReliability = currentReliability,
            potentialReliability = potentialReliability,
            sources = sources
        )
    }

    /**
     * Request a system permission.
     *
     * Delegates to PermissionManager to handle the actual permission request.
     * For PACKAGE_USAGE_STATS, this opens the Settings screen.
     *
     * @param activity The activity to use for launching permission requests
     * @param requirement The system permission requirement to request
     */
    fun requestPermission(activity: Activity, requirement: AccessRequirement.SystemPermission) {
        permissionManager.requestPermission(activity, requirement)
    }

    /**
     * Query a specific metric for the specified time range.
     *
     * @param metric The metric to query
     * @param fromMillis Start time in milliseconds since epoch (inclusive)
     * @param toMillis End time in milliseconds since epoch (inclusive)
     * @return MetricQueryResult containing the metric-specific result and data quality
     */
    internal suspend fun queryMetric(
        metric: Metric,
        fromMillis: Long,
        toMillis: Long
    ): MetricQueryResult = withContext(Dispatchers.IO) {
        when (metric) {
            Metric.LANGUAGE_LEARNING -> queryLanguageLearning(fromMillis, toMillis)
            Metric.READING -> queryReading(fromMillis, toMillis)
        }
    }

    /**
     * Query language learning metric.
     */
    private suspend fun queryLanguageLearning(
        fromMillis: Long,
        toMillis: Long
    ): LanguageLearningMetricResult {
        val collector = collectors[Metric.LANGUAGE_LEARNING]
            ?: throw IllegalStateException("No collector for LANGUAGE_LEARNING")

        val aggregator = aggregators[Metric.LANGUAGE_LEARNING]
            ?: throw IllegalStateException("No aggregator for LANGUAGE_LEARNING")

        // Collect evidence
        val evidenceResult = collector.collect(fromMillis, toMillis)
        val evidence = evidenceResult.getOrNull() ?: emptyList()

        // Aggregate evidence
        @Suppress("UNCHECKED_CAST")
        val result = aggregator.aggregate(toMillis, evidence, minConfidence) as? LanguageLearningResult

        // Build data quality
        val dataQuality = buildDataQuality()

        return LanguageLearningMetricResult(
            result = result,
            dataQuality = dataQuality
        )
    }

    /**
     * Query reading metric.
     */
    private suspend fun queryReading(
        fromMillis: Long,
        toMillis: Long
    ): ReadingMetricResult {
        val collector = collectors[Metric.READING]
            ?: throw IllegalStateException("No collector for READING")

        val aggregator = aggregators[Metric.READING]
            ?: throw IllegalStateException("No aggregator for READING")

        // Collect evidence
        val evidenceResult = collector.collect(fromMillis, toMillis)
        val evidence = evidenceResult.getOrNull() ?: emptyList()

        // Aggregate evidence
        @Suppress("UNCHECKED_CAST")
        val result = aggregator.aggregate(toMillis, evidence, minConfidence) as? ReadingResult

        // Build data quality
        val dataQuality = buildDataQuality()

        return ReadingMetricResult(
            result = result,
            dataQuality = dataQuality
        )
    }

    /**
     * Build data quality information.
     */
    private fun buildDataQuality(): DataQuality {
        val availableSources = mutableListOf<DataSource>()
        val missingSources = mutableListOf<MissingSource>()

        // Check USAGE_STATS permission
        when (permissionManager.checkPermission(Permission.PACKAGE_USAGE_STATS)) {
            PermissionStatus.GRANTED -> {
                availableSources.add(DataSource.USAGE_STATS)
            }
            PermissionStatus.MISSING -> {
                missingSources.add(
                    MissingSource(
                        source = DataSource.USAGE_STATS,
                        reason = MissingReason.NO_PERMISSION,
                        message = "Usage access permission is required"
                    )
                )
            }
        }

        // Determine overall reliability
        val reliability = when {
            missingSources.isEmpty() -> ReliabilityLevel.HIGH
            availableSources.isNotEmpty() -> ReliabilityLevel.MEDIUM
            else -> ReliabilityLevel.LOW
        }

        // Generate recommendations
        val recommendations = mutableListOf<String>()
        if (missingSources.any { it.reason == MissingReason.NO_PERMISSION }) {
            recommendations.add("Grant usage access permission in Settings for better tracking")
        }

        return DataQuality(
            availableSources = availableSources,
            missingSources = missingSources,
            overallReliability = reliability,
            recommendations = recommendations
        )
    }
}
