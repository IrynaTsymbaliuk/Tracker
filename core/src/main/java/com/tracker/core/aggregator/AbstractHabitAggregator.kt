package com.tracker.core.aggregator

import com.tracker.core.model.Evidence
import com.tracker.core.result.AppInfo
import com.tracker.core.result.HabitResult
import com.tracker.core.result.toConfidenceLevel
import com.tracker.core.result.toOccurred
import com.tracker.core.types.DataSource
import kotlin.math.max

/**
 * Abstract base class for habit aggregators.
 *
 * Provides common aggregation logic:
 * 1. Combine confidence scores using probability formula: 1 - ∏(1 - p_i)
 * 2. Sum durations from all evidence
 * 3. Count valid sessions (durationMinutes > 0 && packageName != null)
 * 4. Apply weak-only penalty if all evidence is below minConfidence
 * 5. Determine occurred status based on thresholds
 * 6. Extract app information (packageName + appName)
 *
 * Subclasses only need to implement `createResult()` to construct their
 * specific HabitResult type with the aggregated data.
 */
internal abstract class AbstractHabitAggregator<T : HabitResult> : Aggregator<T> {

    override fun aggregate(dayMillis: Long, evidence: List<Evidence>, minConfidence: Float): T? {
        if (evidence.isEmpty()) {
            return null
        }

        // Step 1: Combine confidence scores using probability formula
        var combinedConfidence = combineProbabilities(evidence.map { it.confidence })

        // Step 2: Sum durations
        val totalDuration = evidence.mapNotNull { it.durationMinutes }.sum()

        // Step 3: Count valid sessions
        // Valid session = durationMinutes > 0 && packageName != null
        val sessionCount = evidence.count { ev ->
            val duration = ev.durationMinutes
            val packageName = ev.metadata["packageName"] as? String
            duration != null && duration > 0 && packageName != null
        }

        // Step 4: Apply weak-only penalty
        val allWeak = evidence.all { it.confidence < minConfidence }
        if (allWeak) {
            combinedConfidence = max(0f, combinedConfidence - Aggregator.WEAK_ONLY_PENALTY)
        }

        // Step 5: Determine occurred status
        val occurred = combinedConfidence.toOccurred(minConfidence)
        val confidenceLevel = combinedConfidence.toConfidenceLevel()

        // Step 6: Extract app information (package name + app name)
        val apps = evidence.mapNotNull { ev ->
            val packageName = ev.metadata["packageName"] as? String
            val appName = ev.metadata["appName"] as? String
            if (packageName != null && appName != null) {
                AppInfo(packageName, appName)
            } else {
                null
            }
        }.distinctBy { it.packageName }

        // Step 7: Determine primary source (all from USAGE_STATS for now)
        val primarySource = evidence.firstOrNull()?.source ?: DataSource.USAGE_STATS

        // Step 8: Delegate to subclass to create specific result type
        return createResult(
            occurred = occurred,
            confidence = combinedConfidence,
            confidenceLevel = confidenceLevel,
            durationMinutes = if (totalDuration > 0) totalDuration else null,
            sessionCount = if (sessionCount > 0) sessionCount else null,
            source = primarySource,
            apps = apps
        )
    }

    /**
     * Create the specific HabitResult type with aggregated data.
     *
     * Subclasses implement this to construct their specific result type
     * (e.g., LanguageLearningResult, ReadingResult) using the provided aggregated values.
     */
    protected abstract fun createResult(
        occurred: Boolean,
        confidence: Float,
        confidenceLevel: com.tracker.core.types.ConfidenceLevel,
        durationMinutes: Int?,
        sessionCount: Int?,
        source: DataSource,
        apps: List<AppInfo>
    ): T

    /**
     * Combine independent probabilities using the formula:
     * combined = 1 - ∏(1 - p_i)
     *
     * This represents the probability that at least one of the independent
     * events is true (i.e., at least one session was genuine).
     */
    private fun combineProbabilities(confidences: List<Float>): Float {
        if (confidences.isEmpty()) return 0f
        if (confidences.size == 1) return confidences.first()

        var product = 1.0f
        for (confidence in confidences) {
            product *= (1.0f - confidence)
        }

        return 1.0f - product
    }
}
