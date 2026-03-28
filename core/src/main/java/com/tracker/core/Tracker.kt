package com.tracker.core

import android.content.Context
import com.tracker.core.collector.AndroidNetworkConnectivityChecker
import com.tracker.core.collector.HttpRssFetcher
import com.tracker.core.collector.LetterboxdCollector
import com.tracker.core.collector.UsageEventsCollector
import com.tracker.core.permission.PermissionManager
import com.tracker.core.provider.LanguageLearningProvider
import com.tracker.core.provider.MovieWatchingProvider
import com.tracker.core.provider.ReadingProvider
import com.tracker.core.provider.SocialMediaProvider
import com.tracker.core.result.LanguageLearningResult
import com.tracker.core.result.MovieWatchingResult
import com.tracker.core.result.ReadingResult
import com.tracker.core.result.SocialMediaResult
import java.util.Calendar

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
 * // Query metrics (last 24 hours)
 * val languageLearning = tracker.queryLanguageLearning()
 * val reading = tracker.queryReading()
 * val movieWatching = tracker.queryMovieWatching()
 * val socialMedia = tracker.querySocialMedia()
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
     * @param days Number of days to include: 1 = today from midnight through now,
     * 2 = yesterday midnight through now, etc. Must be >= 1.
     * @return Language learning data for the requested window, or null if not available
     */
    suspend fun queryLanguageLearning(days: Int = 1): LanguageLearningResult? {
        val (from, to) = queryWindow(days)
        return languageLearningProvider?.query(from, to, minConfidence)
    }

    /**
     * @param days Number of days to include: 1 = today from midnight through now,
     * 2 = yesterday midnight through now, etc. Must be >= 1.
     * @return Reading data for the requested window, or null if not available
     */
    suspend fun queryReading(days: Int = 1): ReadingResult? {
        val (from, to) = queryWindow(days)
        return readingProvider?.query(from, to, minConfidence)
    }

    /**
     * Requires Letterboxd username to be set via [setLetterboxdUsername].
     *
     * @param days Number of days to include: 1 = today from midnight through now,
     * 2 = yesterday midnight through now, etc. Must be >= 1.
     * @return Movie watching data for the requested window, or null if username not set or feed unavailable
     */
    suspend fun queryMovieWatching(days: Int = 1): MovieWatchingResult? {
        val (from, to) = queryWindow(days)
        return movieWatchingProvider?.query(from, to, minConfidence)
    }

    /**
     * @param days Number of days to include: 1 = today from midnight through now,
     * 2 = yesterday midnight through now, etc. Must be >= 1.
     * @return Social media usage data for the requested window, or null if not available
     */
    suspend fun querySocialMedia(days: Int = 1): SocialMediaResult? {
        val (from, to) = queryWindow(days)
        return socialMediaProvider?.query(from, to, minConfidence)
    }

    /**
     * Returns a (fromMillis, toMillis) pair for the given number of days.
     *
     * from = midnight of (today - (days - 1)) in the device's local timezone.
     * to = the current time.
     *
     * Anchors midnight calculation to [timeProvider] so tests can control the result.
     */
    private fun queryWindow(days: Int): Pair<Long, Long> {
        require(days >= 1) { "days must be >= 1, was $days" }
        val now = timeProvider.now()
        val calendar = Calendar.getInstance().apply {
            timeInMillis = now
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            add(Calendar.DAY_OF_YEAR, -(days - 1))
        }
        return calendar.timeInMillis to now
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
            val usageEventsCollector = UsageEventsCollector(context, permissionManager)

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
                readingProvider = if (enableReading) ReadingProvider(usageEventsCollector) else null,
                languageLearningProvider = if (enableLanguageLearning) LanguageLearningProvider(
                    usageEventsCollector
                ) else null,
                movieWatchingProvider = movieProvider,
                socialMediaProvider = if (enableSocialMedia) SocialMediaProvider(usageEventsCollector) else null,
                timeProvider = timeProvider
            )
        }
    }
}
