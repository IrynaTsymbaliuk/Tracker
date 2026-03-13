package com.tracker.core

import android.app.Activity
import android.content.Context
import com.tracker.core.engine.HabitEngine
import com.tracker.core.result.AccessRequirement
import com.tracker.core.result.AccessStatus
import com.tracker.core.result.HabitAccessInfo
import com.tracker.core.result.LanguageLearningMetricResult
import com.tracker.core.result.ReadingMetricResult
import com.tracker.core.types.Metric
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Tracker - Main entry point for the library.
 * This is the only class the host app needs to interact with.
 *
 * Usage:
 * ```
 * val tracker = Tracker.Builder(context)
 *     .setMinConfidence(0.50)
 *     .build()
 *
 * // Query language learning with coroutines (last 24 hours)
 * val result = tracker.queryLanguageLearning()
 *
 * // Query language learning with callback (last 24 hours)
 * tracker.queryLanguageLearning { result ->
 *     // Handle result
 * }
 *
 * // Query reading with coroutines (last 24 hours)
 * val result = tracker.queryReading()
 *
 * // Query reading with callback (last 24 hours)
 * tracker.queryReading { result ->
 *     // Handle result
 * }
 * ```
 */
class Tracker private constructor(
    private val context: Context,
    private val minConfidence: Float,
    internal val timeProvider: TimeProvider = TimeProvider { System.currentTimeMillis() }
) {

    private val habitEngine = HabitEngine.create(
        context,
        minConfidence
    )

    /**
     * Get the configured minimum confidence threshold.
     *
     * @return Minimum confidence value (0.0 to 1.0)
     */
    fun getMinConfidence(): Float = minConfidence

    /**
     * Get access requirements for a specific metric.
     *
     * This method provides transparency about what data sources are available,
     * what's missing, and how to improve data quality for the requested metric.
     *
     * @param metric The metric to check access for
     * @return HabitAccessInfo for the metric
     */
    fun getAccessRequirements(metric: Metric): HabitAccessInfo {
        return habitEngine.getAccessRequirements(metric)
    }

    /**
     * Check if all required access is granted for a specific metric.
     *
     * This is a convenience method that checks if all data sources for the
     * requested metric have their access requirements met.
     *
     * @param metric The metric to check access for
     * @return true if all required permissions/credentials are granted, false otherwise
     */
    fun hasAllRequiredAccess(metric: Metric): Boolean {
        return getAccessRequirements(metric)
            .sources
            .all { it.status == AccessStatus.GRANTED }
    }

    /**
     * Request missing access for a specific metric.
     *
     * This method automatically requests all missing permissions/credentials
     * needed by the library for the specified metric. For system permissions
     * like PACKAGE_USAGE_STATS, this will open the appropriate Settings screen.
     *
     * Call this method when [hasAllRequiredAccess] returns false for a metric.
     *
     * @param activity The activity to use for launching permission requests
     * @param metric The metric to request access for
     */
    fun requestMissingAccess(activity: Activity, metric: Metric) {
        getAccessRequirements(metric)
            .sources
            .filter { it.status != AccessStatus.GRANTED }
            .distinctBy { it.requirement }
            .forEach { source ->
                when (val requirement = source.requirement) {
                    is AccessRequirement.SystemPermission -> {
                        habitEngine.requestPermission(activity, requirement)
                    }
                }
            }
    }

    /**
     * Query language learning metric asynchronously using coroutines.
     *
     * Returns data for the last 24 hours from current time.
     *
     * @return LanguageLearningMetricResult containing language learning data and data quality
     */
    suspend fun queryLanguageLearning(): LanguageLearningMetricResult {
        val now = timeProvider.now()
        val toMillis = now
        val fromMillis = now - 86_400_000L // 24 hours in milliseconds
        return habitEngine.queryMetric(Metric.LANGUAGE_LEARNING, fromMillis, toMillis) as LanguageLearningMetricResult
    }

    /**
     * Query language learning metric using callback.
     *
     * Returns data for the last 24 hours from current time.
     * Callback is invoked on the Main dispatcher.
     *
     * @param callback Called when results are ready
     */
    fun queryLanguageLearning(callback: (LanguageLearningMetricResult) -> Unit) {
        CoroutineScope(Dispatchers.Main).launch {
            val result = queryLanguageLearning()
            callback(result)
        }
    }

    /**
     * Query reading metric asynchronously using coroutines.
     *
     * Returns data for the last 24 hours from current time.
     *
     * @return ReadingMetricResult containing reading data and data quality
     */
    suspend fun queryReading(): ReadingMetricResult {
        val now = timeProvider.now()
        val toMillis = now
        val fromMillis = now - 86_400_000L // 24 hours in milliseconds
        return habitEngine.queryMetric(Metric.READING, fromMillis, toMillis) as ReadingMetricResult
    }

    /**
     * Query reading metric using callback.
     *
     * Returns data for the last 24 hours from current time.
     * Callback is invoked on the Main dispatcher.
     *
     * @param callback Called when results are ready
     */
    fun queryReading(callback: (ReadingMetricResult) -> Unit) {
        CoroutineScope(Dispatchers.Main).launch {
            val result = queryReading()
            callback(result)
        }
    }

    /**
     * Builder for creating Tracker instances.
     */
    class Builder(private val context: Context) {
        private var minConfidence: Float = 0.50f
        internal var timeProvider: TimeProvider = TimeProvider { System.currentTimeMillis() }

        /**
         * Set minimum confidence threshold for results.
         *
         * @param confidence Minimum confidence (0.0 to 1.0, default: 0.50)
         * @return This builder for chaining
         */
        fun setMinConfidence(confidence: Float): Builder {
            require(confidence in 0.0f..1.0f) { "Confidence must be between 0.0 and 1.0" }
            this.minConfidence = confidence
            return this
        }

        /**
         * Build the Tracker instance.
         *
         * @return Configured Tracker instance
         */
        fun build(): Tracker {
            return Tracker(
                context = context.applicationContext,
                minConfidence = minConfidence,
                timeProvider = timeProvider
            )
        }
    }
}
