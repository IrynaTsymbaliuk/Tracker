package com.tracker.core

import android.content.Context
import android.os.Build
import com.tracker.core.collector.AndroidNetworkConnectivityChecker
import com.tracker.core.collector.HealthConnectDistanceCollector
import com.tracker.core.collector.HealthConnectExerciseCollector
import com.tracker.core.collector.HealthConnectMindfulnessCollector
import com.tracker.core.collector.HealthConnectSleepCollector
import com.tracker.core.collector.HealthConnectStepCollector
import com.tracker.core.collector.HealthConnectTrainingCollector
import com.tracker.core.collector.HttpRssFetcher
import com.tracker.core.collector.LetterboxdCollector
import com.tracker.core.collector.UsageEventsCollector
import com.tracker.core.config.AppMetadata
import com.tracker.core.config.KnownApps
import com.tracker.core.permission.PermissionManager
import com.tracker.core.provider.DistanceProvider
import com.tracker.core.provider.ExerciseProvider
import com.tracker.core.provider.LanguageLearningProvider
import com.tracker.core.provider.MeditationProvider
import com.tracker.core.provider.MovieWatchingProvider
import com.tracker.core.provider.ReadingProvider
import com.tracker.core.provider.SleepProvider
import com.tracker.core.provider.SocialMediaProvider
import com.tracker.core.provider.StepCountingProvider
import com.tracker.core.provider.TrainingProvider
import com.tracker.core.result.DistanceResult
import com.tracker.core.result.ExerciseResult
import com.tracker.core.result.LanguageLearningResult
import com.tracker.core.result.MeditationResult
import com.tracker.core.result.MovieWatchingResult
import com.tracker.core.result.ReadingResult
import com.tracker.core.result.SleepResult
import com.tracker.core.result.SocialMediaResult
import com.tracker.core.result.StepCountingResult
import com.tracker.core.result.TrainingResult

/**
 * Main entry point for the library. This is the only class the host app needs to interact with.
 *
 * All features are always available. [UsageEventsCollector] is created lazily on the first
 * usage-stats query and shared across all usage-stats features.
 *
 * Usage:
 * ```
 * val tracker = Tracker.Builder(context)
 *     .setLetterboxdUsername("username")  // Optional: can also be set later
 *     .build()
 *
 * // Or set/update username later
 * tracker.setLetterboxdUsername("username")
 *
 * // Query metrics for the current local day by default
 * val languageLearning = tracker.queryLanguageLearning()
 * val reading = tracker.queryReading()
 * val movieWatching = tracker.queryMovieWatching()  // throws if username not set
 * val socialMedia = tracker.querySocialMedia()
 * val meditation = tracker.queryMeditation()
 * val exercise = tracker.queryExercise()
 * val training = tracker.queryTraining()
 * ```
 */
class Tracker private constructor(
    private val appContext: Context,
    letterboxdUsername: String?,
    internal val timeProvider: TimeProvider // internal for testing
) {

    private var letterboxdUsername: String? = letterboxdUsername

    private val permissionManager by lazy { PermissionManager(appContext) }

    private val usageEventsCollector by lazy {
        UsageEventsCollector(appContext, permissionManager)
    }

    private val readingProvider by lazy { ReadingProvider(usageEventsCollector) }
    private val languageLearningProvider by lazy { LanguageLearningProvider(usageEventsCollector) }
    private val socialMediaProvider by lazy { SocialMediaProvider(usageEventsCollector) }

    private val movieWatchingProvider by lazy {
        MovieWatchingProvider(
            LetterboxdCollector(
                rssFetcher = HttpRssFetcher(
                    networkChecker = AndroidNetworkConnectivityChecker(appContext)
                )
            )
        )
    }

    private val stepCountingProvider: StepCountingProvider? by lazy {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            StepCountingProvider(HealthConnectStepCollector(appContext))
        } else null
    }

    private val distanceProvider: DistanceProvider? by lazy {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            DistanceProvider(HealthConnectDistanceCollector(appContext))
        } else null
    }

    private val sleepProvider: SleepProvider? by lazy {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            SleepProvider(HealthConnectSleepCollector(appContext))
        } else null
    }

    private val meditationProvider: MeditationProvider by lazy {
        val mindfulnessCollector = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            HealthConnectMindfulnessCollector(appContext)
        } else null
        MeditationProvider(
            healthConnectCollector = mindfulnessCollector,
            usageEventsCollector = usageEventsCollector
        )
    }

    private val exerciseProvider: ExerciseProvider? by lazy {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            ExerciseProvider(HealthConnectExerciseCollector(appContext))
        } else null
    }

    private val trainingProvider: TrainingProvider? by lazy {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            TrainingProvider(HealthConnectTrainingCollector(appContext))
        } else null
    }

    /**
     * Sets the Letterboxd username for movie watching queries.
     * Can be called at any time to update the username.
     *
     * @param username Letterboxd username
     */
    fun setLetterboxdUsername(username: String?) {
        letterboxdUsername = username
    }

    /**
     * Returns the apps the library tracks for language learning — the curated set of known apps
     * whose foreground usage is matched when detecting this habit.
     *
     * This is static configuration: it reflects what the library *can* detect, independent of
     * which apps are actually installed on the device or whether any permission has been granted.
     * Each [AppMetadata] exposes the app's package name and its base confidence multiplier.
     *
     * @return The tracked language learning apps.
     */
    fun getTrackedLanguageLearningApps(): List<AppMetadata> =
        KnownApps.languageLearning.values.toList()

    /**
     * Returns the apps the library tracks for reading. See [getTrackedLanguageLearningApps] for
     * notes on what this list represents.
     *
     * @return The tracked reading apps.
     */
    fun getTrackedReadingApps(): List<AppMetadata> =
        KnownApps.reading.values.toList()

    /**
     * Returns the apps the library tracks for social media. See [getTrackedLanguageLearningApps]
     * for notes on what this list represents.
     *
     * @return The tracked social media apps.
     */
    fun getTrackedSocialMediaApps(): List<AppMetadata> =
        KnownApps.socialMedia.values.toList()

    /**
     * Returns the apps the library tracks for meditation. See [getTrackedLanguageLearningApps] for
     * notes on what this list represents.
     *
     * Note that meditation can additionally be detected from Health Connect
     * `MindfulnessSessionRecord`s; this list covers only the known-app (UsageStats) source.
     *
     * @return The tracked meditation apps.
     */
    fun getTrackedMeditationApps(): List<AppMetadata> =
        KnownApps.meditation.values.toList()

    /**
     * @param days Number of days to include: 1 = today from midnight through now,
     * 2 = yesterday midnight through now, etc. Must be >= 1.
     * @return Language learning data for the requested window, or null if no data is available
     * @throws PermissionDeniedException if PACKAGE_USAGE_STATS is not granted
     * @throws NoMonitorableAppsException if none of the known language learning apps are installed
     */
    suspend fun queryLanguageLearning(days: Int = 1): LanguageLearningResult? {
        val (from, to) = queryWindow(days)
        return languageLearningProvider.query(from, to)
    }

    /**
     * @param days Number of days to include: 1 = today from midnight through now,
     * 2 = yesterday midnight through now, etc. Must be >= 1.
     * @return Reading data for the requested window, or null if no data is available
     * @throws PermissionDeniedException if PACKAGE_USAGE_STATS is not granted
     * @throws NoMonitorableAppsException if none of the known reading apps are installed
     */
    suspend fun queryReading(days: Int = 1): ReadingResult? {
        val (from, to) = queryWindow(days)
        return readingProvider.query(from, to)
    }

    /**
     * Requires a Letterboxd username to be set via [setLetterboxdUsername] or
     * [Builder.setLetterboxdUsername] before calling.
     *
     * @param days Number of days to include: 1 = today from midnight through now,
     * 2 = yesterday midnight through now, etc. Must be >= 1.
     * @return Movie watching data for the requested window, or null if the feed is unavailable
     * @throws IllegalStateException if Letterboxd username has not been set
     * @throws NetworkException if the network request fails
     * @throws RssParseException if the RSS feed cannot be parsed
     */
    suspend fun queryMovieWatching(days: Int = 1): MovieWatchingResult? {
        val username = letterboxdUsername
        check(!username.isNullOrBlank()) {
            "Letterboxd username is not set. Call setLetterboxdUsername() or Builder.setLetterboxdUsername() before querying movie watching."
        }
        movieWatchingProvider.setUsername(username)
        val (from, to) = queryWindow(days)
        return movieWatchingProvider.query(from, to)
    }

    /**
     * @param days Number of days to include: 1 = today from midnight through now,
     * 2 = yesterday midnight through now, etc. Must be >= 1.
     * @return Social media usage data for the requested window, or null if no data is available
     * @throws PermissionDeniedException if PACKAGE_USAGE_STATS is not granted
     * @throws NoMonitorableAppsException if none of the known social media apps are installed
     */
    suspend fun querySocialMedia(days: Int = 1): SocialMediaResult? {
        val (from, to) = queryWindow(days)
        return socialMediaProvider.query(from, to)
    }

    /**
     * Requires Health Connect to be installed and [HealthConnectStepCollector.READ_STEPS_PERMISSION]
     * to be granted at runtime via [PermissionController.createRequestPermissionResultContract].
     *
     * @param days Number of days to include: 1 = today from midnight through now,
     * 2 = yesterday midnight through now, etc. Must be >= 1.
     * @return Step count data for the requested window, or null if Health Connect is unavailable
     * or the API level is below 26
     */
    suspend fun queryStepCounting(days: Int = 1): StepCountingResult? {
        val (from, to) = queryWindow(days)
        return stepCountingProvider?.query(from, to)
    }

    /**
     * Queries distance travelled (walking, running, cycling, etc.) from Health Connect
     * `DistanceRecord`, returned as hourly buckets so multi-day queries can be split by hour.
     *
     * Requires Health Connect to be installed, API 26+, and
     * [HealthConnectDistanceCollector.READ_DISTANCE_PERMISSION] to be granted at runtime via
     * [androidx.health.connect.client.PermissionController.createRequestPermissionResultContract].
     *
     * @param days Number of days to include: 1 = today from midnight through now,
     * 2 = yesterday midnight through now, etc. Must be >= 1.
     * @return Distance data for the requested window, or null if Health Connect is unavailable,
     * the API level is below 26, or the permission has not been granted.
     */
    suspend fun queryDistance(days: Int = 1): DistanceResult? {
        val (from, to) = queryWindow(days)
        return distanceProvider?.query(from, to)
    }

    /**
     * Queries sleep from Health Connect `SleepSessionRecord`, returned as one
     * [com.tracker.core.result.SleepSession] per night/nap. Each session exposes when the user
     * fell asleep (`startTime`) and woke (`endTime`), the per-stage breakdown, minutes actually
     * asleep, sleep efficiency, and a derived [com.tracker.core.types.SleepQuality] band.
     *
     * Requires Health Connect to be installed, API 26+, and
     * [HealthConnectSleepCollector.READ_SLEEP_PERMISSION] to be granted at runtime via
     * [androidx.health.connect.client.PermissionController.createRequestPermissionResultContract].
     *
     * @param days Number of days to include: 1 = today from midnight through now,
     * 2 = yesterday midnight through now, etc. Must be >= 1.
     * @return Sleep data for the requested window, or null if Health Connect is unavailable,
     * the API level is below 26, or the permission has not been granted.
     */
    suspend fun querySleep(days: Int = 1): SleepResult? {
        val (from, to) = queryWindow(days)
        return sleepProvider?.query(from, to)
    }

    /**
     * Queries meditation activity from two fused sources:
     * - Health Connect `MindfulnessSessionRecord` (requires
     *   [HealthConnectMindfulnessCollector.READ_MINDFULNESS_PERMISSION] granted at runtime
     *   and API 26+; automatically falls back to UsageStats-only if unavailable).
     * - UsageStats foreground sessions of known meditation apps
     *   (see [com.tracker.core.config.KnownApps.meditation]).
     *
     * Overlapping sessions from both sources are deduplicated, and the resulting
     * [MeditationResult.sources] lists all contributing sources.
     *
     * @param days Number of days to include: 1 = today from midnight through now,
     * 2 = yesterday midnight through now, etc. Must be >= 1.
     * @return Meditation data for the requested window, or null if neither source has data
     * @throws PermissionDeniedException if neither source grants access
     * (Health Connect throws are caught internally; UsageStats permission denial is caught too,
     * so this method typically returns null rather than throwing. If PACKAGE_USAGE_STATS is
     * denied AND no HealthConnect data is available, the result is null).
     */
    suspend fun queryMeditation(days: Int = 1): MeditationResult? {
        val (from, to) = queryWindow(days)
        return meditationProvider.query(from, to)
    }

    /**
     * Queries exercise activity from Health Connect `ExerciseSessionRecord`.
     *
     * Requires Health Connect to be installed, API 26+, and
     * [HealthConnectExerciseCollector.READ_EXERCISE_PERMISSION] to be granted at
     * runtime via
     * [androidx.health.connect.client.PermissionController.createRequestPermissionResultContract].
     *
     * @param days Number of days to include: 1 = today from midnight through now,
     * 2 = yesterday midnight through now, etc. Must be >= 1.
     * @return Exercise data for the requested window, or null if Health Connect is
     * unavailable, the API level is below 26, the permission has not been granted,
     * or no sessions exist in the window.
     */
    suspend fun queryExercise(days: Int = 1): ExerciseResult? {
        val (from, to) = queryWindow(days)
        return exerciseProvider?.query(from, to)
    }

    /**
     * Queries Health Connect planned exercise sessions (training plans).
     *
     * Training plans can be scheduled in the future. This overload follows Tracker's usual
     * day-window convention; use [queryTraining] with explicit timestamps to read a future range.
     * Requires Health Connect, API 26+, the planned-exercise feature, and
     * [HealthConnectTrainingCollector.READ_PLANNED_EXERCISE_PERMISSION] granted at runtime.
     *
     * @param days Number of days to include ending now; must be >= 1.
     * @return Training plans in the window, or null when none exist or Health Connect is
     * unavailable, unsupported, or not permitted.
     */
    suspend fun queryTraining(days: Int = 1): TrainingResult? {
        val (from, to) = queryWindow(days)
        return trainingProvider?.query(from, to)
    }

    /**
     * Queries Health Connect planned exercise sessions in an explicit time range. This is useful
     * for retrieving upcoming training plans.
     *
     * @param fromMillis Inclusive range start in milliseconds since epoch.
     * @param toMillis Inclusive range end in milliseconds since epoch; must be after [fromMillis].
     */
    suspend fun queryTraining(fromMillis: Long, toMillis: Long): TrainingResult? {
        require(fromMillis < toMillis) { "fromMillis must be before toMillis" }
        return trainingProvider?.query(fromMillis, toMillis)
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
        return QueryWindowCalculator.calculate(days, timeProvider.now())
    }

    /**
     * Builder for creating [Tracker] instances.
     */
    class Builder(private val context: Context) {
        internal var timeProvider: TimeProvider =
            TimeProvider { System.currentTimeMillis() } // internal for testing
        private var letterboxdUsername: String? = null

        /**
         * Sets Letterboxd username for movie watching queries.
         * Can also be set or updated later via [Tracker.setLetterboxdUsername].
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
            return Tracker(
                appContext = context.applicationContext,
                letterboxdUsername = letterboxdUsername,
                timeProvider = timeProvider
            )
        }
    }
}
