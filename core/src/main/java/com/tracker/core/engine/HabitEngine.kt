package com.tracker.core.engine

import android.content.Context
import com.tracker.core.aggregator.LanguageLearningAggregator
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
class HabitEngine(
    private val context: Context,
    private val requestedMetrics: Set<Metric>
) {

    private val permissionManager = PermissionManager(context)
    private val languageLearningCollector = UsageStatsCollector(context, permissionManager)
    private val languageLearningAggregator = LanguageLearningAggregator()

    /**
     * Query metrics for the specified time range.
     */
    suspend fun query(fromMillis: Long, toMillis: Long): MetricsResult = withContext(Dispatchers.IO) {
        // Collect evidence for requested metrics
        val allEvidence = mutableListOf<Evidence>()

        if (Metric.LANGUAGE_LEARNING in requestedMetrics) {
            val result = languageLearningCollector.collect(fromMillis, toMillis)
            // On success, add evidence; on failure, continue with empty evidence
            result.getOrNull()?.let { evidence ->
                allEvidence.addAll(evidence)
            }
        }

        // Group evidence by day
        val evidenceByDay = groupEvidenceByDay(allEvidence, fromMillis, toMillis)

        // Aggregate evidence for each day
        val dayResults = evidenceByDay.map { (dayMillis, evidence) ->
            val languageLearningResult = if (Metric.LANGUAGE_LEARNING in requestedMetrics) {
                val dayEvidence = evidence.filter { it.source == DataSource.USAGE_STATS }
                languageLearningAggregator.aggregate(dayMillis, dayEvidence)
            } else {
                null
            }

            DayResult(
                timestampMillis = dayMillis,
                languageLearning = languageLearningResult
            )
        }

        // Build summary
        val summary = buildSummary(dayResults)

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
     */
    private fun groupEvidenceByDay(
        evidence: List<Evidence>,
        fromMillis: Long,
        toMillis: Long
    ): Map<Long, List<Evidence>> {
        // Generate all days in the range
        val allDays = generateDayRange(fromMillis, toMillis)

        // Group evidence by day
        val grouped = evidence.groupBy { e ->
            getStartOfDay(e.timestampMillis)
        }

        // Ensure all days are present (even with empty evidence)
        return allDays.associateWith { day ->
            grouped[day] ?: emptyList()
        }
    }

    /**
     * Generate list of day timestamps (start of each day) in the range.
     */
    private fun generateDayRange(fromMillis: Long, toMillis: Long): List<Long> {
        val days = mutableListOf<Long>()
        var currentDay = getStartOfDay(fromMillis)
        val endDay = getStartOfDay(toMillis)

        while (currentDay <= endDay) {
            days.add(currentDay)
            currentDay += TimeUnit.DAYS.toMillis(1)
        }

        return days
    }

    /**
     * Get start of day timestamp (00:00:00) for given timestamp.
     */
    private fun getStartOfDay(timestampMillis: Long): Long {
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
     */
    private fun buildSummary(dayResults: List<DayResult>): Summary {
        val languageLearningDays = dayResults.count { it.languageLearning?.occurred == true }

        val totalMinutes = dayResults.mapNotNull { it.languageLearning?.durationMinutes }.sum()
        val averageMinutes = if (languageLearningDays > 0) {
            totalMinutes / languageLearningDays
        } else {
            null
        }

        return Summary(
            totalDays = dayResults.size,
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
