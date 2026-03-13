package com.tracker.core

import android.app.Activity
import android.content.Context
import com.tracker.core.engine.HabitEngine
import com.tracker.core.result.AccessRequirement
import com.tracker.core.result.AccessStatus
import com.tracker.core.result.HabitAccessInfo
import com.tracker.core.result.MetricsResult
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
 *     .requestMetrics(Metric.LANGUAGE_LEARNING)
 *     .setMinConfidence(0.50)
 *     .build()
 *
 * // Query with coroutines (last 24 hours)
 * val result = tracker.queryAsync()
 *
 * // Query with callback (last 24 hours)
 * tracker.query { result ->
 *     // Handle result
 * }
 * ```
 */
class Tracker private constructor(
    private val context: Context,
    private val requestedMetrics: Set<Metric>,
    private val minConfidence: Float,
    internal val timeProvider: TimeProvider = TimeProvider { System.currentTimeMillis() }
) {

    private val habitEngine = HabitEngine.create(
        context,
        requestedMetrics,
        minConfidence
    )

    /**
     * Get the configured minimum confidence threshold.
     *
     * @return Minimum confidence value (0.0 to 1.0)
     */
    fun getMinConfidence(): Float = minConfidence

    /**
     * Get access requirements for all requested metrics.
     *
     * This method provides transparency about what data sources are available,
     * what's missing, and how to improve data quality for each requested metric.
     *
     * @return List of HabitAccessInfo, one per requested metric
     */
    fun getAccessRequirements(): List<HabitAccessInfo> {
        return habitEngine.getAccessRequirements()
    }

    /**
     * Check if all required access is granted.
     *
     * This is a convenience method that checks if all data sources for all
     * requested metrics have their access requirements met.
     *
     * @return true if all required permissions/credentials are granted, false otherwise
     */
    fun hasAllRequiredAccess(): Boolean {
        return getAccessRequirements()
            .flatMap { it.sources }
            .all { it.status == AccessStatus.GRANTED }
    }

    /**
     * Request missing access for all requested metrics.
     *
     * This method automatically requests all missing permissions/credentials
     * needed by the library. For system permissions like PACKAGE_USAGE_STATS,
     * this will open the appropriate Settings screen.
     *
     * Call this method when [hasAllRequiredAccess] returns false.
     *
     * @param activity The activity to use for launching permission requests
     */
    fun requestMissingAccess(activity: Activity) {
        getAccessRequirements()
            .flatMap { it.sources }
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
     * Query metrics asynchronously using coroutines.
     *
     * Returns data for the last 24 hours from current time.
     *
     * @return MetricsResult containing data from the last 24 hours
     */
    suspend fun queryAsync(): MetricsResult {
        val now = timeProvider.now()
        val toMillis = now
        val fromMillis = now - 86_400_000L // 24 hours in milliseconds
        return habitEngine.query(fromMillis, toMillis)
    }

    /**
     * Query metrics using callback.
     *
     * Returns data for the last 24 hours from current time.
     * Callback is invoked on the Main dispatcher.
     *
     * @param callback Called when results are ready
     */
    fun query(callback: (MetricsResult) -> Unit) {
        CoroutineScope(Dispatchers.Main).launch {
            val result = queryAsync()
            callback(result)
        }
    }

    /**
     * Builder for creating Tracker instances.
     */
    class Builder(private val context: Context) {
        private val metrics = mutableSetOf<Metric>()
        private var minConfidence: Float = 0.50f
        internal var timeProvider: TimeProvider = TimeProvider { System.currentTimeMillis() }

        /**
         * Request specific metrics to track.
         *
         * @param metrics Metrics to track (e.g., LANGUAGE_LEARNING)
         * @return This builder for chaining
         */
        fun requestMetrics(vararg metrics: Metric): Builder {
            this.metrics.addAll(metrics)
            return this
        }

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
            require(metrics.isNotEmpty()) { "At least one metric must be requested" }
            return Tracker(
                context = context.applicationContext,
                requestedMetrics = metrics.toSet(),
                minConfidence = minConfidence,
                timeProvider = timeProvider
            )
        }
    }
}
