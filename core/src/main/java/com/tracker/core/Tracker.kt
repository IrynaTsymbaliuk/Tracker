package com.tracker.core

import android.content.Context
import com.tracker.core.collector.AndroidNetworkConnectivityChecker
import com.tracker.core.collector.HttpRssFetcher
import com.tracker.core.collector.LetterboxdCollector
import com.tracker.core.collector.UsageStatsCollector
import com.tracker.core.permission.PermissionManager
import com.tracker.core.provider.LanguageLearningProvider
import com.tracker.core.provider.MovieWatchingProvider
import com.tracker.core.provider.ReadingProvider
import com.tracker.core.provider.SocialMediaProvider
import com.tracker.core.result.LanguageLearningResult
import com.tracker.core.result.MovieWatchingResult
import com.tracker.core.result.ReadingResult
import com.tracker.core.result.SocialMediaResult
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

/**
 * Main entry point for the library. This is the only class the host app needs to interact with.
 *
 * Usage:
 * ```
 * val tracker = Tracker.Builder(context)
 *     .enableReading()
 *     .enableLanguageLearning()
 *     .enableMovieWatching()
 *     .setLetterboxdUsername("username")  // Optional: can also be set later
 *     .setMinConfidence(0.50f)
 *     .build()
 *
 * // Or set/update username later
 * // tracker.setLetterboxdUsername("username")
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
 *
 * // Query social media with coroutines (last 24 hours)
 * val result = tracker.querySocialMedia()
 *
 * // Query social media with callback (last 24 hours)
 * tracker.querySocialMedia { result ->
 *     // Handle result
 * }
 * ```
 */
class Tracker private constructor(
    val minConfidence: Float,
    private val readingProvider: ReadingProvider?,
    private val languageLearningProvider: LanguageLearningProvider?,
    private val movieWatchingProvider: MovieWatchingProvider?,
    private val socialMediaProvider: SocialMediaProvider?,
    internal val timeProvider: TimeProvider // internal for testing
) {

    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    companion object {
        private const val LAST_24_HOURS_MS = 86_400_000L
    }

    /** Cancels all pending callback-based queries. */
    fun cancel() = scope.cancel()

    /**
     * Sets the Letterboxd username for movie watching queries.
     * Can be called at any time to enable or update movie tracking.
     *
     * @param username Letterboxd username
     */
    fun setLetterboxdUsername(username: String?) {
        movieWatchingProvider?.setUsername(username)
    }

    /**
     * @return Language learning data for the last 24 hours, or null if not available
     */
    suspend fun queryLanguageLearning(): LanguageLearningResult? {
        val now = timeProvider.now()
        return languageLearningProvider?.query(now - LAST_24_HOURS_MS, now, minConfidence)
    }

    /**
     * Callback-based variant of [queryLanguageLearning]. Callback is invoked on the Main dispatcher.
     */
    fun queryLanguageLearning(callback: (LanguageLearningResult?) -> Unit) {
        scope.launch { callback(queryLanguageLearning()) }
    }

    /**
     * @return Reading data for the last 24 hours, or null if not available
     */
    suspend fun queryReading(): ReadingResult? {
        val now = timeProvider.now()
        return readingProvider?.query(now - LAST_24_HOURS_MS, now, minConfidence)
    }

    /**
     * Callback-based variant of [queryReading]. Callback is invoked on the Main dispatcher.
     */
    fun queryReading(callback: (ReadingResult?) -> Unit) {
        scope.launch { callback(queryReading()) }
    }

    /**
     * Requires Letterboxd username to be set via [setLetterboxdUsername].
     * @return Movie watching data for the last 24 hours, or null if username not set or feed unavailable
     */
    suspend fun queryMovieWatching(): MovieWatchingResult? {
        val now = timeProvider.now()
        return movieWatchingProvider?.query(now - LAST_24_HOURS_MS, now, minConfidence)
    }

    /**
     * Callback-based variant of [queryMovieWatching]. Callback is invoked on the Main dispatcher.
     */
    fun queryMovieWatching(callback: (MovieWatchingResult?) -> Unit) {
        scope.launch { callback(queryMovieWatching()) }
    }

    /**
     * @return Social media usage data for the last 24 hours, or null if not available
     */
    suspend fun querySocialMedia(): SocialMediaResult? {
        val now = timeProvider.now()
        return socialMediaProvider?.query(now - LAST_24_HOURS_MS, now, minConfidence)
    }

    /**
     * Callback-based variant of [querySocialMedia]. Callback is invoked on the Main dispatcher.
     */
    fun querySocialMedia(callback: (SocialMediaResult?) -> Unit) {
        scope.launch { callback(querySocialMedia()) }
    }

    /**
     * Builder for creating [Tracker] instances.
     */
    class Builder(private val context: Context) {
        private var minConfidence: Float = 0.50f
        internal var timeProvider: TimeProvider =
            TimeProvider { System.currentTimeMillis() } // internal for testing

        private var enableReading = false
        private var enableLanguageLearning = false
        private var enableSocialMedia = false
        private var enableMovieWatching = false
        private var letterboxdUsername: String? = null

        fun enableReading(): Builder {
            enableReading = true
            return this
        }

        fun enableLanguageLearning(): Builder {
            enableLanguageLearning = true
            return this
        }

        fun enableSocialMedia(): Builder {
            enableSocialMedia = true
            return this
        }

        fun enableMovieWatching(): Builder {
            enableMovieWatching = true
            return this
        }

        /**
         * @param confidence Minimum confidence threshold (0.0 to 1.0, default: 0.50)
         * @return This builder for chaining
         */
        fun setMinConfidence(confidence: Float): Builder {
            require(confidence in 0.0f..1.0f) { "Confidence must be between 0.0 and 1.0" }
            this.minConfidence = confidence
            return this
        }

        /**
         * Sets Letterboxd username for movie watching queries.
         * Can also be set later via [Tracker.setLetterboxdUsername].
         *
         * @param username Letterboxd username
         * @return This builder for chaining
         */
        fun setLetterboxdUsername(username: String?): Builder {
            this.letterboxdUsername = username
            return this
        }

        /**
         * @return Configured [Tracker] instance
         */
        fun build(): Tracker {
            val permissionManager = PermissionManager(context)
            val usageStatsCollector = UsageStatsCollector(context, permissionManager)

            val movieProvider = if (enableMovieWatching) {
                MovieWatchingProvider(
                    LetterboxdCollector(
                        rssFetcher = HttpRssFetcher(
                            networkChecker = AndroidNetworkConnectivityChecker(context)
                        )
                    )
                ).apply {
                    letterboxdUsername?.let { setUsername(it) }
                }
            } else null

            return Tracker(
                minConfidence = minConfidence,
                readingProvider = if (enableReading) ReadingProvider(usageStatsCollector) else null,
                languageLearningProvider = if (enableLanguageLearning) LanguageLearningProvider(
                    usageStatsCollector
                ) else null,
                movieWatchingProvider = movieProvider,
                socialMediaProvider = if (enableSocialMedia) SocialMediaProvider(usageStatsCollector) else null,
                timeProvider = timeProvider
            )
        }
    }
}
