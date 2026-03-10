package com.tracker.core.engine

import android.content.Context
import com.tracker.core.aggregator.Aggregator
import com.tracker.core.aggregator.LanguageLearningAggregator
import com.tracker.core.collector.Collector
import com.tracker.core.collector.UsageStatsCollector
import com.tracker.core.model.Evidence
import com.tracker.core.permission.Permission
import com.tracker.core.permission.PermissionManager
import com.tracker.core.permission.PermissionStatus
import com.tracker.core.result.*
import com.tracker.core.types.DataSource
import com.tracker.core.types.Metric
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Calendar
import java.util.concurrent.TimeUnit

/**
 * Central coordinator for habit tracking.
 *
 * Responsibilities:
 * 1. Register collectors and aggregators per metric
 * 2. Run collectors (in parallel in future)
 * 3. Group evidence by day
 * 4. Pass evidence to aggregators
 * 5. Build final MetricsResult with summary and data quality
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
        fun create(context: Context, requestedMetrics: Set<Metric>, minConfidence: Float): HabitEngine {
            val permissionManager = PermissionManager(context)
            val collectors = mapOf(
                Metric.LANGUAGE_LEARNING to UsageStatsCollector(context, permissionManager)
            )
            val aggregators = mapOf<Metric, Aggregator<out HabitResult>>(
                Metric.LANGUAGE_LEARNING to LanguageLearningAggregator()
            )
            return HabitEngine(requestedMetrics, minConfidence, permissionManager, collectors, aggregators)
        }
    }

    /**
     * Query metrics for the specified time range.
     */
    suspend fun query(fromMillis: Long, toMillis: Long): MetricsResult = withContext(Dispatchers.IO) {
        // Collect evidence for requested metrics
        val allEvidence = mutableListOf<Evidence>()

        for ((metric, collector) in collectors) {
            if (metric in requestedMetrics) {
                val result = collector.collect(fromMillis, toMillis)
                // On success, add evidence; on failure, continue with empty evidence
                result.getOrNull()?.let { evidence ->
                    allEvidence.addAll(evidence)
                }
            }
        }

        // Group evidence by day (sparse - only days with data)
        val evidenceByDay = groupEvidenceByDay(allEvidence)

        // Aggregate evidence for each day
        val dayResults = evidenceByDay.map { (dayMillis, evidence) ->
            val languageLearningResult = if (Metric.LANGUAGE_LEARNING in requestedMetrics) {
                val dayEvidence = evidence.filter { it.source == DataSource.USAGE_STATS }
                @Suppress("UNCHECKED_CAST")
                val aggregator = aggregators[Metric.LANGUAGE_LEARNING] as? Aggregator<LanguageLearningResult>
                aggregator?.aggregate(dayMillis, dayEvidence, minConfidence)
            } else {
                null
            }

            DayResult(
                timestampMillis = dayMillis,
                languageLearning = languageLearningResult
            )
        }

        // Build summary (needs total days in range, not just days with data)
        val totalDaysInRange = calculateDaysInRange(fromMillis, toMillis)
        val summary = buildSummary(dayResults, totalDaysInRange)

        // Build data quality
        val dataQuality = buildDataQuality()

        MetricsResult(
            days = dayResults,
            summary = summary,
            dataQuality = dataQuality
        )
    }

    /**
     * Group evidence by day (start of day timestamp).
     * Returns sparse map - only days with evidence are included.
     */
    internal fun groupEvidenceByDay(evidence: List<Evidence>): Map<Long, List<Evidence>> {
        return evidence.groupBy { e ->
            getStartOfDay(e.timestampMillis)
        }
    }

    /**
     * Calculate the number of days in the given range (inclusive).
     */
    internal fun calculateDaysInRange(fromMillis: Long, toMillis: Long): Int {
        val startDay = getStartOfDay(fromMillis)
        val endDay = getStartOfDay(toMillis)
        val diffMillis = endDay - startDay
        return (TimeUnit.MILLISECONDS.toDays(diffMillis) + 1).toInt()
    }

    /**
     * Get start of day timestamp (00:00:00) for given timestamp.
     */
    internal fun getStartOfDay(timestampMillis: Long): Long {
        val calendar = Calendar.getInstance().apply {
            timeInMillis = timestampMillis
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        return calendar.timeInMillis
    }

    /**
     * Build summary statistics.
     *
     * @param dayResults Results for days with data (sparse)
     * @param totalDaysInRange Total days in the queried range
     */
    private fun buildSummary(dayResults: List<DayResult>, totalDaysInRange: Int): Summary {
        val languageLearningDays = dayResults.count { it.languageLearning?.occurred == true }

        val totalMinutes = dayResults.mapNotNull { it.languageLearning?.durationMinutes }.sum()
        val averageMinutes = if (totalDaysInRange > 0) {
            totalMinutes / totalDaysInRange
        } else {
            0
        }

        return Summary(
            totalDays = totalDaysInRange,
            languageLearningDays = if (Metric.LANGUAGE_LEARNING in requestedMetrics) languageLearningDays else null,
            averageLanguageLearningMinutes = if (Metric.LANGUAGE_LEARNING in requestedMetrics) averageMinutes else null
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
