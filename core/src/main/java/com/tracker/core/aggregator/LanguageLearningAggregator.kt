package com.tracker.core.aggregator

import com.tracker.core.model.Evidence
import com.tracker.core.result.LanguageLearningResult
import com.tracker.core.result.toConfidenceLevel
import com.tracker.core.result.toOccurred
import com.tracker.core.types.DataSource
import kotlin.math.max
import kotlin.math.min

/**
 * Aggregates language learning evidence into a single result.
 *
 * Aggregation process:
 * 1. Deduplicate overlapping time ranges
 * 2. Combine confidence scores using probability formula
 * 3. Sum durations from all evidence
 * 4. Apply weak-only penalty if all evidence is below 0.50 confidence
 * 5. Determine occurred status based on thresholds
 */
class LanguageLearningAggregator : Aggregator<LanguageLearningResult> {

    companion object {
        private const val WEAK_ONLY_PENALTY = 0.15f
        private const val OVERLAP_THRESHOLD = 0.80f // 80% overlap
    }

    override fun aggregate(dayMillis: Long, evidence: List<Evidence>, minConfidence: Float): LanguageLearningResult? {
        if (evidence.isEmpty()) {
            return null
        }

        // Step 1: Deduplicate overlapping evidence
        val deduplicated = deduplicateOverlapping(evidence)

        // Step 2: Combine confidence scores
        var combinedConfidence = combineProbabilities(deduplicated.map { it.confidence })

        // Step 3: Sum durations
        val totalDuration = deduplicated.mapNotNull { it.durationMinutes }.sum()

        // Step 4: Apply weak-only penalty
        val allWeak = deduplicated.all { it.confidence < minConfidence }
        if (allWeak) {
            combinedConfidence = max(0f, combinedConfidence - WEAK_ONLY_PENALTY)
        }

        // Step 5: Determine occurred status
        val occurred = combinedConfidence.toOccurred(minConfidence)
        val confidenceLevel = combinedConfidence.toConfidenceLevel()

        // Step 6: Extract app information (package name + app name)
        val apps = deduplicated.mapNotNull { evidence ->
            val packageName = evidence.metadata["packageName"] as? String
            val appName = evidence.metadata["appName"] as? String
            if (packageName != null && appName != null) {
                com.tracker.core.result.AppInfo(packageName, appName)
            } else {
                null
            }
        }.distinctBy { it.packageName }

        // Step 7: Determine primary source (all from USAGE_STATS for now)
        val primarySource = deduplicated.firstOrNull()?.source ?: DataSource.USAGE_STATS

        return LanguageLearningResult(
            occurred = occurred,
            confidence = combinedConfidence,
            confidenceLevel = confidenceLevel,
            durationMinutes = if (totalDuration > 0) totalDuration else null,
            source = primarySource,
            apps = apps
        )
    }

    /**
     * Deduplicate evidence with overlapping time ranges.
     * Only deduplicates evidence from the SAME app package.
     * If two evidence from the same app overlap by more than 80% → keep the one with higher confidence.
     * Different apps are never considered duplicates, even with overlapping times.
     */
    private fun deduplicateOverlapping(evidence: List<Evidence>): List<Evidence> {
        if (evidence.size <= 1) return evidence

        val sorted = evidence.sortedByDescending { it.confidence }
        val result = mutableListOf<Evidence>()

        for (current in sorted) {
            var maxOverlap = 0f

            val currentPackage = current.metadata["packageName"] as? String

            for (existing in result) {
                val existingPackage = existing.metadata["packageName"] as? String

                // Only check overlap if both evidence are from the same app
                if (currentPackage != null && currentPackage == existingPackage) {
                    val overlapPct = calculateOverlapPercentage(current, existing)
                    if (overlapPct > maxOverlap) {
                        maxOverlap = overlapPct
                    }
                }
            }

            val overlaps = maxOverlap > OVERLAP_THRESHOLD

            if (!overlaps) {
                result.add(current)
            }
        }

        return result
    }

    /**
     * Calculate overlap percentage between two evidence items.
     * Returns value between 0.0 and 1.0.
     */
    private fun calculateOverlapPercentage(e1: Evidence, e2: Evidence): Float {
        val start1 = e1.startTimeMillis ?: return 0f
        val end1 = e1.endTimeMillis ?: return 0f
        val start2 = e2.startTimeMillis ?: return 0f
        val end2 = e2.endTimeMillis ?: return 0f

        val overlapStart = max(start1, start2)
        val overlapEnd = min(end1, end2)

        if (overlapStart >= overlapEnd) {
            return 0f // No overlap
        }

        val overlapDuration = overlapEnd - overlapStart
        val duration1 = end1 - start1
        val duration2 = end2 - start2
        val shorterDuration = min(duration1, duration2)

        return if (shorterDuration > 0) {
            overlapDuration.toFloat() / shorterDuration.toFloat()
        } else {
            0f
        }
    }

    /**
     * Combine independent probabilities using the formula:
     * combined = 1 - ∏(1 - p_i)
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
