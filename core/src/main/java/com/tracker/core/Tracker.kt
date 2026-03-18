package com.tracker.core

import android.content.Context
import com.tracker.core.collector.LetterboxdCollector
import com.tracker.core.collector.UsageStatsCollector
import com.tracker.core.permission.PermissionManager
import com.tracker.core.provider.LanguageLearningProvider
import com.tracker.core.provider.MovieWatchingProvider
import com.tracker.core.provider.ReadingProvider
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
 *     .setLetterboxdUsername("username")
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
 *
 * // Query movie watching with coroutines (last 24 hours)
 * val result = tracker.queryMovieWatching()
 *
 * // Query movie watching with callback (last 24 hours)
 * tracker.queryMovieWatching { result ->
 *     // Handle result
 * }
 * ```
 */
class Tracker private constructor(
    private val minConfidence: Float,
    private val readingProvider: ReadingProvider?,
    private val languageLearningProvider: LanguageLearningProvider?,
    private val movieWatchingProvider: MovieWatchingProvider?,
    internal val timeProvider: TimeProvider = TimeProvider { System.currentTimeMillis() }
) {

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
        return languageLearningProvider?.query(fromMillis, toMillis, minConfidence)
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
        return readingProvider?.query(fromMillis, toMillis, minConfidence)
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
     * Query movie watching metric asynchronously using coroutines.
     * Returns data for the last 24 hours from current time.
     * Requires Letterboxd username to be set via Builder.setLetterboxdUsername().
     * @return HabitResult
     */
    suspend fun queryMovieWatching(): HabitResult? {
        val now = timeProvider.now()
        val toMillis = now
        val fromMillis = now - 86_400_000L // 24 hours in milliseconds
        return movieWatchingProvider?.query(fromMillis, toMillis, minConfidence)
    }

    /**
     * Query movie watching metric using callback.
     * Returns data for the last 24 hours from current time.
     * Requires Letterboxd username to be set via Builder.setLetterboxdUsername().
     * Callback is invoked on the Main dispatcher.
     * @param callback Called when results are ready
     */
    fun queryMovieWatching(callback: (HabitResult?) -> Unit) {
        CoroutineScope(Dispatchers.Main).launch {
            val result = queryMovieWatching()
            callback(result)
        }
    }

    /**
     * Builder for creating Tracker instances.
     */
    class Builder(private val context: Context) {
        private var minConfidence: Float = 0.50f
        internal var timeProvider: TimeProvider = TimeProvider { System.currentTimeMillis() }

        private var enableReading = false
        private var enableLanguageLearning = false
        private var letterboxdUsername: String? = null

        fun enableReading() {
            enableReading = true
        }

        fun enableLanguageLearning() {
            enableLanguageLearning = true
        }

        fun enableMovieWatching(username: String) {
            letterboxdUsername = username
        }

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
         * Set Letterboxd username for movie watching tracking.
         * Required for queryMovieWatching() to work.
         * @param username Letterboxd username
         * @return This builder for chaining
         */
        fun setLetterboxdUsername(username: String?): Builder {
            this.letterboxdUsername = username
            return this
        }

        /**
         * Build the Tracker instance.
         * @return Configured Tracker instance
         */
        fun build(): Tracker {
            val permissionManager = PermissionManager(context)
            return Tracker(
                minConfidence = minConfidence,
                readingProvider = if (enableReading) ReadingProvider(
                    UsageStatsCollector(
                        context,
                        permissionManager
                    )
                ) else null,
                languageLearningProvider = if (enableLanguageLearning) LanguageLearningProvider(
                    UsageStatsCollector(context, permissionManager)
                ) else null,
                movieWatchingProvider = letterboxdUsername?.let {
                    MovieWatchingProvider(
                        LetterboxdCollector(),
                        it
                    )
                },
                timeProvider = timeProvider
            )
        }
    }
}
