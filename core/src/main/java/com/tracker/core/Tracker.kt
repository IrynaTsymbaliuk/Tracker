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
 *     .setLookbackDays(30)
 *     .build()
 *
 * // Query with coroutines
 * val result = tracker.queryAsync(fromMillis, toMillis)
 *
 * // Query with callback
 * tracker.query(fromMillis, toMillis) { result ->
 *     // Handle result
 * }
 * ```
 */
class Tracker private constructor(
    private val context: Context,
    private val requestedMetrics: Set<Metric>,
    private val minConfidence: Float,
    private val lookbackDays: Int
) {

    private val habitEngine = HabitEngine.create(
        context,
        requestedMetrics,
        minConfidence
    )

    /**
     * Get the configured lookback period in days.
     *
     * @return Number of days used for default queries
     */
    fun getLookbackDays(): Int = lookbackDays

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
     * Query metrics asynchronously using coroutines with default lookback period.
     *
     * @return MetricsResult containing data from the last lookbackDays
     */
    suspend fun queryAsync(): MetricsResult {
        val toMillis = System.currentTimeMillis()
        val fromMillis = toMillis - (lookbackDays * 24 * 60 * 60 * 1000L)
        return habitEngine.query(fromMillis, toMillis)
    }

    /**
     * Query metrics asynchronously using coroutines.
     *
     * @param fromMillis Start time in milliseconds since epoch (inclusive)
     * @param toMillis End time in milliseconds since epoch (inclusive)
     * @return MetricsResult containing all requested data
     */
    suspend fun queryAsync(fromMillis: Long, toMillis: Long): MetricsResult {
        return habitEngine.query(fromMillis, toMillis)
    }

    /**
     * Query metrics using callback with default lookback period.
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
     * Query metrics using callback.
     *
     * @param fromMillis Start time in milliseconds since epoch (inclusive)
     * @param toMillis End time in milliseconds since epoch (inclusive)
     * @param callback Called when results are ready
     */
    fun query(fromMillis: Long, toMillis: Long, callback: (MetricsResult) -> Unit) {
        CoroutineScope(Dispatchers.Main).launch {
            val result = habitEngine.query(fromMillis, toMillis)
            callback(result)
        }
    }

    /**
     * Builder for creating Tracker instances.
     */
    class Builder(private val context: Context) {
        private val metrics = mutableSetOf<Metric>()
        private var lookbackDays: Int = 30
        private var minConfidence: Float = 0.50f

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
         * Set the default lookback period in days.
         * This is used when querying without explicit time range.
         *
         * @param days Number of days to look back (1-365, default: 30)
         * @return This builder for chaining
         * @throws IllegalArgumentException if days is not in valid range
         */
        fun setLookbackDays(days: Int): Builder {
            require(days in 1..365) { "Lookback days must be between 1 and 365" }
            this.lookbackDays = days
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
                lookbackDays = lookbackDays
            )
        }
    }
}
