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
import com.tracker.core.result.HabitResult
import com.tracker.core.result.LanguageLearningResult
import com.tracker.core.result.MetricsResult
import com.tracker.core.result.MissingReason
import com.tracker.core.result.MissingSource
import com.tracker.core.result.ReadingResult
import com.tracker.core.result.ReliabilityLevel
import com.tracker.core.result.SourceAccessInfo
import com.tracker.core.types.DataSource
import com.tracker.core.types.Metric
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Central coordinator for habit tracking.
 *
 * 1. Register collectors and aggregators per metric
 * 2. Run collectors (in parallel in future)
 * 3. Aggregate all evidence at once
 * 4. Build final MetricsResult with direct fields and data quality
 */
class HabitEngine internal constructor(
    private val requestedMetrics: Set<Metric>,
    private val minConfidence: Float,
    private val permissionManager: PermissionManager,
    private val collectors: Map<Metric, Collector>,
    private val aggregators: Map<Metric, Aggregator<out HabitResult>>
) {

    companion object {
        /**
         * Create a HabitEngine with default dependencies.
         */
        fun create(
            context: Context,
            requestedMetrics: Set<Metric>,
            minConfidence: Float
        ): HabitEngine {
            val permissionManager = PermissionManager(context)

            // Only instantiate collectors for requested metrics
            val collectors = buildMap<Metric, Collector> {
                if (Metric.LANGUAGE_LEARNING in requestedMetrics) {
                    put(Metric.LANGUAGE_LEARNING, UsageStatsLanguageLearningCollector(context, permissionManager))
                }
                if (Metric.READING in requestedMetrics) {
                    put(Metric.READING, UsageStatsReadingCollector(context, permissionManager))
                }
            }

            // Only instantiate aggregators for requested metrics
            val aggregators = buildMap<Metric, Aggregator<out HabitResult>> {
                if (Metric.LANGUAGE_LEARNING in requestedMetrics) {
                    put(Metric.LANGUAGE_LEARNING, LanguageLearningAggregator())
                }
                if (Metric.READING in requestedMetrics) {
                    put(Metric.READING, ReadingAggregator())
                }
            }

            return HabitEngine(requestedMetrics, minConfidence, permissionManager, collectors, aggregators)
        }
    }

    /**
     * Get access requirements for all requested metrics.
     *
     * Dynamically builds access info from registered collectors,
     * following the self-describing collector pattern.
     *
     * @return List of HabitAccessInfo, one per requested metric
     */
    fun getAccessRequirements(): List<HabitAccessInfo> {
        return requestedMetrics.map { metric ->
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

            HabitAccessInfo(
                metric = metric,
                currentReliability = currentReliability,
                potentialReliability = potentialReliability,
                sources = sources
            )
        }
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
     * Query metrics for the specified time range.
     *
     * @param fromMillis Start time in milliseconds since epoch (inclusive)
     * @param toMillis End time in milliseconds since epoch (inclusive)
     * @return MetricsResult with direct metric fields
     */
    suspend fun query(fromMillis: Long, toMillis: Long): MetricsResult = withContext(Dispatchers.IO) {
        // Collect evidence for requested metrics, keeping them separated by metric
        val evidenceByMetric = mutableMapOf<Metric, List<Evidence>>()

        for ((metric, collector) in collectors) {
            if (metric in requestedMetrics) {
                val result = collector.collect(fromMillis, toMillis)
                // On success, store evidence; on failure, store empty list
                evidenceByMetric[metric] = result.getOrNull() ?: emptyList()
            }
        }

        // Aggregate all evidence at once for each metric (no day grouping)
        val languageLearningResult = if (Metric.LANGUAGE_LEARNING in requestedMetrics) {
            val evidence = evidenceByMetric[Metric.LANGUAGE_LEARNING] ?: emptyList()
            @Suppress("UNCHECKED_CAST")
            val aggregator = aggregators[Metric.LANGUAGE_LEARNING] as? Aggregator<LanguageLearningResult>
            // Pass toMillis as the timestamp parameter (aggregator signature requires it)
            aggregator?.aggregate(toMillis, evidence, minConfidence)
        } else {
            null
        }

        val readingResult = if (Metric.READING in requestedMetrics) {
            val evidence = evidenceByMetric[Metric.READING] ?: emptyList()
            @Suppress("UNCHECKED_CAST")
            val aggregator = aggregators[Metric.READING] as? Aggregator<ReadingResult>
            // Pass toMillis as the timestamp parameter (aggregator signature requires it)
            aggregator?.aggregate(toMillis, evidence, minConfidence)
        } else {
            null
        }

        // Build data quality
        val dataQuality = buildDataQuality()

        MetricsResult(
            languageLearning = languageLearningResult,
            reading = readingResult,
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
