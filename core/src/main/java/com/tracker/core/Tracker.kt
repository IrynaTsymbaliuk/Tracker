package com.tracker.core

import android.content.Context
import com.tracker.core.engine.HabitEngine
import com.tracker.core.result.HabitResult
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
     * Query language learning metric asynchronously using coroutines.
     * Returns data for the last 24 hours from current time.
     * @return HabitResult
     */
    suspend fun queryLanguageLearning(): HabitResult? {
        val now = timeProvider.now()
        val toMillis = now
        val fromMillis = now - 86_400_000L // 24 hours in milliseconds
        return habitEngine.queryMetric(Metric.LANGUAGE_LEARNING, fromMillis, toMillis)
    }

    /**
     * Query language learning metric using callback.
     * Returns data for the last 24 hours from current time.
     * Callback is invoked on the Main dispatcher.
     * @param callback Called when results are ready
     */
    fun queryLanguageLearning(callback: (HabitResult?) -> Unit) {
        CoroutineScope(Dispatchers.Main).launch {
            val result = queryLanguageLearning()
            callback(result)
        }
    }

    /**
     * Query reading metric asynchronously using coroutines.
     * Returns data for the last 24 hours from current time.
     * @return HabitResult
     */
    suspend fun queryReading(): HabitResult? {
        val now = timeProvider.now()
        val toMillis = now
        val fromMillis = now - 86_400_000L // 24 hours in milliseconds
        return habitEngine.queryMetric(Metric.READING, fromMillis, toMillis)
    }

    /**
     * Query reading metric using callback.
     * Returns data for the last 24 hours from current time.
     * Callback is invoked on the Main dispatcher.
     * @param callback Called when results are ready
     */
    fun queryReading(callback: (HabitResult?) -> Unit) {
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
