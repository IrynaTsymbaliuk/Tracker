@file:OptIn(ExperimentalMindfulnessSessionApi::class)

package com.tracker

import android.app.AppOpsManager
import android.content.Intent
import android.os.Bundle
import android.os.Process
import android.provider.Settings
import androidx.activity.result.ActivityResultLauncher
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.PermissionController
import androidx.health.connect.client.feature.ExperimentalMindfulnessSessionApi
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.DistanceRecord
import androidx.health.connect.client.records.ExerciseSessionRecord
import androidx.health.connect.client.records.MindfulnessSessionRecord
import androidx.health.connect.client.records.SleepSessionRecord
import androidx.health.connect.client.records.StepsRecord
import androidx.lifecycle.lifecycleScope
import com.tracker.core.Tracker
import com.tracker.core.config.AppMetadata
import com.tracker.core.result.DistanceResult
import com.tracker.core.result.DistanceSession
import com.tracker.core.result.ExerciseResult
import com.tracker.core.result.LanguageLearningResult
import com.tracker.core.result.MeditationResult
import com.tracker.core.result.MovieSession
import com.tracker.core.result.MovieWatchingResult
import com.tracker.core.result.ReadingResult
import com.tracker.core.result.SleepResult
import com.tracker.core.result.SleepSession
import com.tracker.core.result.SocialMediaResult
import com.tracker.core.result.StepCountingResult
import com.tracker.core.result.StepSession
import com.tracker.core.result.UsageSession
import com.tracker.databinding.ActivityMainBinding
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var tracker: Tracker

    private val hcPermissionContract = PermissionController.createRequestPermissionResultContract()
    private lateinit var hcPermissionLauncher: ActivityResultLauncher<Set<String>>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        hcPermissionLauncher = registerForActivityResult(hcPermissionContract) {
            lifecycleScope.launch { updateHcPermissionUi() }
        }

        tracker = Tracker.Builder(this).build()

        binding.btnGrantUsage.setOnClickListener {
            startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS))
        }
        binding.btnGrantHc.setOnClickListener { requestMissingHcPermissions() }
        binding.btnQuery.setOnClickListener { queryMetrics() }
        binding.btnShowApps.setOnClickListener { toggleTrackedApps() }
    }

    override fun onResume() {
        super.onResume()
        updateUsagePermissionUi()
        lifecycleScope.launch { runCatching { updateHcPermissionUi() } }
        queryMetrics()
    }

    private fun updateUsagePermissionUi() {
        val granted = hasUsageStatsPermission()
        binding.tvUsageStatus.text = if (granted) "Usage access  ✓" else "Usage access  ✗"
        binding.btnGrantUsage.isVisible = !granted
    }

    private fun hasUsageStatsPermission(): Boolean {
        val appOps = getSystemService(APP_OPS_SERVICE) as? AppOpsManager ?: return false
        val mode = appOps.checkOpNoThrow(
            AppOpsManager.OPSTR_GET_USAGE_STATS,
            Process.myUid(),
            packageName
        )
        return mode == AppOpsManager.MODE_ALLOWED
    }

    private suspend fun updateHcPermissionUi() {
        val sdkAvailable =
            HealthConnectClient.getSdkStatus(this) == HealthConnectClient.SDK_AVAILABLE

        if (sdkAvailable) {
            val granted = grantedHcPermissions()
            val missing = REQUIRED_HC_PERMISSIONS - granted
            val allGranted = missing.isEmpty()

            binding.tvHcStatus.text = when {
                allGranted -> "Health Connect  ✓ (Steps + Mindfulness + Exercise + Distance + Sleep)"
                missing.size == REQUIRED_HC_PERMISSIONS.size -> "Health Connect  ✗"
                else -> {
                    val missingLabels = missing.mapNotNull { HC_PERMISSION_LABELS[it] }
                    "Health Connect  ◐ (missing: ${missingLabels.joinToString(", ")})"
                }
            }
            binding.btnGrantHc.isVisible = !allGranted
            binding.layoutHcPermission.isVisible = true
        } else {
            binding.layoutHcPermission.isVisible = false
        }
    }

    /**
     * Launches a single Health Connect permission prompt covering every item in
     * [REQUIRED_HC_PERMISSIONS] that has not already been granted. A single user
     * tap on "Grant" therefore covers both Steps and Mindfulness in one dialog.
     *
     * Safe to call even if Health Connect is unavailable or nothing is missing:
     * in those cases we silently do nothing and let the normal UI state apply.
     */
    private fun requestMissingHcPermissions() {
        lifecycleScope.launch {
            if (HealthConnectClient.getSdkStatus(this@MainActivity)
                != HealthConnectClient.SDK_AVAILABLE
            ) return@launch

            val missing = REQUIRED_HC_PERMISSIONS - grantedHcPermissions()
            if (missing.isEmpty()) {
                updateHcPermissionUi()
                return@launch
            }
            hcPermissionLauncher.launch(missing)
        }
    }

    private suspend fun grantedHcPermissions(): Set<String> =
        HealthConnectClient.getOrCreate(this)
            .permissionController.getGrantedPermissions()

    private fun queryMetrics() {
        binding.progressBar.isVisible = true
        binding.btnQuery.isEnabled = false

        lifecycleScope.launch {
            val language   = runCatching { tracker.queryLanguageLearning() }.getOrNull()
            val reading    = runCatching { tracker.queryReading() }.getOrNull()
            val social     = runCatching { tracker.querySocialMedia() }.getOrNull()
            val movies     = runCatching { tracker.queryMovieWatching() }.getOrNull()
            val steps      = runCatching { tracker.queryStepCounting() }.getOrNull()
            val distance   = runCatching { tracker.queryDistance() }.getOrNull()
            val meditation = runCatching { tracker.queryMeditation() }.getOrNull()
            val exercise   = runCatching { tracker.queryExercise() }.getOrNull()
            val sleep      = runCatching { tracker.querySleep() }.getOrNull()

            displayResults(language, reading, social, movies, steps, distance, meditation, exercise, sleep)

            binding.progressBar.isVisible = false
            binding.btnQuery.isEnabled = true
        }
    }

    /**
     * Toggles the static tracked-apps catalogue, built from the library's `getTracked*Apps()`
     * accessors. Unlike the query results above, this is pure configuration — it lists every app
     * the library *can* detect for each known-app habit, independent of what is installed on the
     * device or which permissions have been granted. No coroutine needed: the lookups are synchronous.
     */
    private fun toggleTrackedApps() {
        val tv = binding.tvTrackedApps
        if (tv.isVisible) {
            tv.isVisible = false
            binding.btnShowApps.setText(R.string.button_show_tracked_apps)
            return
        }

        tv.text = listOf(
            "📚 Language" to tracker.getTrackedLanguageLearningApps(),
            "📖 Reading" to tracker.getTrackedReadingApps(),
            "📱 Social" to tracker.getTrackedSocialMediaApps(),
            "🧘 Meditation" to tracker.getTrackedMeditationApps()
        ).joinToString(separator = "\n\n") { (label, apps) -> trackedAppsBlock(label, apps) }

        tv.isVisible = true
        binding.btnShowApps.setText(R.string.button_hide_tracked_apps)
    }

    /**
     * Formats one habit's tracked-apps catalogue: a header with the count followed by one indented
     * line per app showing its package name and base confidence multiplier.
     *
     * ```
     * 📖 Reading · 27 apps
     *     • com.amazon.kindle (82%)
     *     • com.audible.application (90%)
     * ```
     */
    private fun trackedAppsBlock(label: String, apps: List<AppMetadata>): String {
        val header = "$label · ${apps.size} app${if (apps.size != 1) "s" else ""}"
        val lines = apps.joinToString(separator = "\n") { app ->
            "    • ${app.packageName} (${pct(app.confidenceMultiplier)})"
        }
        return "$header\n$lines"
    }

    private fun displayResults(
        language: LanguageLearningResult?,
        reading: ReadingResult?,
        social: SocialMediaResult?,
        movies: MovieWatchingResult?,
        steps: StepCountingResult?,
        distance: DistanceResult?,
        meditation: MeditationResult?,
        exercise: ExerciseResult?,
        sleep: SleepResult?
    ) {
        binding.tvLanguage.text = language
            ?.let { "📚 Language    ${it.durationMinutes} min${sessionLines(it.sessions)}" }
            ?: "📚 Language    —"

        binding.tvReading.text = reading
            ?.let { "📖 Reading    ${it.durationMinutes} min${sessionLines(it.sessions)}" }
            ?: "📖 Reading    —"

        binding.tvSocial.text = social
            ?.let { "📱 Social    ${it.durationMinutes} min${sessionLines(it.sessions)}" }
            ?: "📱 Social    —"

        binding.tvMovies.text = movies
            ?.let { "🎬 Movies    ${it.count} film${if (it.count != 1) "s" else ""}${movieSessionLines(it.sessions)}" }
            ?: "🎬 Movies    —"

        binding.tvSteps.text = steps
            ?.let { "👣 Steps    ${"%,d".format(it.totalSteps)} steps${stepSessionLines(it.sessions)}" }
            ?: "👣 Steps    —"

        binding.tvDistance.text = distance
            ?.let { "📏 Distance    ${"%.2f".format(it.totalKilometers)} km${distanceSessionLines(it.sessions)}" }
            ?: "📏 Distance    —"

        binding.tvMeditation.text = meditation
            ?.let {
                val sessionCount = it.sessions.size
                val sessionLabel = if (sessionCount == 1) "session" else "sessions"
                "🧘 Meditation    ${it.durationMinutes} min · $sessionCount $sessionLabel · ${sourcesLabel(it)}"
            }
            ?: "🧘 Meditation    —"

        binding.tvExercise.text = exercise
            ?.let {
                val sessionCount = it.sessions.size
                val sessionLabel = if (sessionCount == 1) "session" else "sessions"
                val types = it.sessions
                    .map { s -> titleCase(s.exerciseType) }
                    .distinct()
                    .joinToString(", ")
                val typesSuffix = if (types.isNotEmpty()) " · $types" else ""
                "🏃 Exercise    ${it.durationMinutes} min · $sessionCount $sessionLabel$typesSuffix"
            }
            ?: "🏃 Exercise    —"

        binding.tvSleep.text = sleep
            ?.let {
                val sessionCount = it.sessions.size
                val sessionLabel = if (sessionCount == 1) "session" else "sessions"
                "😴 Sleep    ${hoursMinutes(it.totalSleepMinutes)} · $sessionCount $sessionLabel${sleepSessionLines(it.sessions)}"
            }
            ?: "😴 Sleep    —"
    }

    /** Formats a minute count as `Xh Ym` (e.g. `452` → `"7h 32m"`, `45` → `"45m"`). */
    private fun hoursMinutes(minutes: Long): String {
        val h = minutes / 60
        val m = minutes % 60
        return if (h > 0) "${h}h ${m}m" else "${m}m"
    }

    /**
     * Converts a Health Connect exercise-type snake_case string into a user-friendly
     * Title Case label (e.g. `"strength_training"` → `"Strength Training"`, `"running"`
     * → `"Running"`). Blank input returns `"Other"`.
     */
    private fun titleCase(raw: String): String {
        if (raw.isBlank()) return "Other"
        return raw.split('_')
            .joinToString(" ") { word ->
                word.replaceFirstChar { ch -> ch.uppercaseChar() }
            }
    }

    /** Formats a 0.0–1.0 multiplier as a whole-number percentage, e.g. `0.82` → `"82%"`. */
    private fun pct(value: Float) = "%.0f%%".format(value * 100)

    /** Short human-readable label for the list of contributing data sources. */
    private fun sourcesLabel(result: MeditationResult): String = result.sources
        .joinToString("+") { src ->
            when (src.name) {
                "HEALTH_CONNECT" -> "HC"
                "USAGE_STATS" -> "Usage"
                else -> src.name
            }
        }

    /**
     * Renders the per-session breakdown shown under a usage-based metric (Reading,
     * Language Learning, Social Media). Each session becomes its own indented line:
     *
     * ```
     *     • 09:15–09:42 · Duolingo (27 min)
     * ```
     *
     * Returns an empty string when there are no sessions, so callers can append the
     * result directly to the summary line. 0-minute sessions are kept so quick app
     * opens are still visible.
     */
    private fun sessionLines(sessions: List<UsageSession>): String {
        if (sessions.isEmpty()) return ""
        return sessions.joinToString(separator = "\n", prefix = "\n") { session ->
            val from = SESSION_TIME_FORMAT.format(Instant.ofEpochMilli(session.startTime))
            val to = SESSION_TIME_FORMAT.format(Instant.ofEpochMilli(session.endTime))
            "    • $from–$to · ${session.appName} (${session.durationMinutes} min)"
        }
    }

    /**
     * Renders the per-bucket step breakdown shown under the Steps metric. Each [StepSession]
     * is an hourly Health Connect bucket (not a discrete walking bout — raw step data has no
     * walk start/stop), so each line covers one hour:
     *
     * ```
     *     • 08:00–09:00 · 1,234 steps
     * ```
     *
     * Returns an empty string when there are no buckets. Empty hours are already omitted by
     * the library, so every line shown has a non-zero count.
     */
    private fun stepSessionLines(sessions: List<StepSession>): String {
        if (sessions.isEmpty()) return ""
        return sessions.joinToString(separator = "\n", prefix = "\n") { session ->
            val from = SESSION_TIME_FORMAT.format(Instant.ofEpochMilli(session.startTime))
            val to = SESSION_TIME_FORMAT.format(Instant.ofEpochMilli(session.endTime))
            "    • $from–$to · ${"%,d".format(session.steps)} steps"
        }
    }

    /**
     * Renders the per-bucket distance breakdown shown under the Distance metric. Like steps,
     * each [DistanceSession] is an hourly Health Connect bucket, so each line covers one hour:
     *
     * ```
     *     • 08:00–09:00 · 1.20 km
     * ```
     *
     * Distances under a kilometre are shown in meters (e.g. `820 m`). Returns an empty string
     * when there are no buckets. Empty hours are already omitted by the library, so every line
     * shown has a non-zero distance.
     */
    private fun distanceSessionLines(sessions: List<DistanceSession>): String {
        if (sessions.isEmpty()) return ""
        return sessions.joinToString(separator = "\n", prefix = "\n") { session ->
            val from = SESSION_TIME_FORMAT.format(Instant.ofEpochMilli(session.startTime))
            val to = SESSION_TIME_FORMAT.format(Instant.ofEpochMilli(session.endTime))
            val dist = if (session.meters >= 1000.0) {
                "%.2f km".format(session.meters / 1000.0)
            } else {
                "%.0f m".format(session.meters)
            }
            "    • $from–$to · $dist"
        }
    }

    /**
     * Renders the per-night sleep breakdown shown under the Sleep metric. Each [SleepSession] is
     * one Health Connect `SleepSessionRecord`, so each entry shows when the user **fell asleep**
     * and **woke**, how long they were **asleep**, the sleep **efficiency** and **quality** band,
     * and — when the source recorded stages — a stage line (deep / REM / light / awake):
     *
     * ```
     *     • asleep 23:15 → woke 07:02 · 7h 32m · 89% · GOOD
     *         deep 1h05m · REM 1h40m · light 4h47m · awake 0h32m
     * ```
     *
     * Efficiency and quality are omitted for sessions whose source wrote no stages (there is no
     * awake data to measure). Returns an empty string when there are no sessions.
     */
    private fun sleepSessionLines(sessions: List<SleepSession>): String {
        if (sessions.isEmpty()) return ""
        return sessions.joinToString(separator = "\n", prefix = "\n") { session ->
            val asleep = SESSION_TIME_FORMAT.format(Instant.ofEpochMilli(session.startTime))
            val woke = SESSION_TIME_FORMAT.format(Instant.ofEpochMilli(session.endTime))
            val efficiency = session.efficiency
                ?.let { " · ${"%.0f%%".format(it * 100)} · ${session.quality}" }
                ?: ""
            val header = "    • asleep $asleep → woke $woke · ${hoursMinutes(session.asleepMinutes)}$efficiency"

            val stageLine = if (session.stages.isEmpty()) "" else {
                val parts = buildList {
                    if (session.deepMinutes > 0) add("deep ${hoursMinutes(session.deepMinutes)}")
                    if (session.remMinutes > 0) add("REM ${hoursMinutes(session.remMinutes)}")
                    if (session.lightMinutes > 0) add("light ${hoursMinutes(session.lightMinutes)}")
                    if (session.awakeMinutes > 0) add("awake ${hoursMinutes(session.awakeMinutes)}")
                }
                if (parts.isEmpty()) "" else "\n        ${parts.joinToString(" · ")}"
            }
            "$header$stageLine"
        }
    }

    /**
     * Renders the per-film breakdown shown under the Movies metric. Each [MovieSession] becomes
     * its own indented line showing the **watched date**, the **title** with its **release year**
     * (when present), the **TMDB id** (when the feed provided one), the user's **star rating**, a
     * **♥** like marker and **↻** rewatch marker (when present), followed by indented lines for the
     * **poster URL** and the **review** text when the entry has them:
     *
     * ```
     *     • Jan 15 · Dune: Part Two (2024) (tmdb:693134) · ★★★★½ ♥ ↻
     *         🖼 https://a.ltrbxd.com/resized/film-poster/…-crop.jpg
     *         "Villeneuve outdid himself."
     * ```
     *
     * The `tmdb:<id>` tag is the The Movie Database movie id — use it to fetch full details
     * (runtime, cast, …) from the TMDB API. Films whose feed entry has no year, id, rating, poster,
     * or review simply omit those parts. Returns an empty string when there are no sessions.
     */
    private fun movieSessionLines(sessions: List<MovieSession>): String {
        if (sessions.isEmpty()) return ""
        return sessions.joinToString(separator = "\n", prefix = "\n") { session ->
            val watched = MOVIE_DATE_FORMAT.format(Instant.ofEpochMilli(session.watchedDate))
            val year = session.year?.let { " ($it)" } ?: ""
            val tmdb = session.tmdbId?.let { " (tmdb:$it)" } ?: ""
            val rating = session.rating?.let { " · ${starRating(it)}" } ?: ""
            val liked = if (session.isLiked) " ♥" else ""
            val rewatch = if (session.isRewatch) " ↻" else ""
            val poster = session.posterUrl?.let { "\n        🖼 $it" } ?: ""
            val review = session.review?.let { "\n        \"$it\"" } ?: ""
            "    • $watched · ${session.title}$year$tmdb$rating$liked$rewatch$poster$review"
        }
    }

    /**
     * Renders a 0.5–5.0 star rating as filled/half/empty star glyphs, e.g. `4.5` → `★★★★½`.
     */
    private fun starRating(rating: Float): String {
        val full = rating.toInt()
        val half = (rating - full) >= 0.5f
        return "★".repeat(full) + (if (half) "½" else "")
    }

    companion object {
        /** Formats session start/end timestamps as local `HH:mm` for the session breakdown. */
        private val SESSION_TIME_FORMAT: DateTimeFormatter =
            DateTimeFormatter.ofPattern("HH:mm").withZone(ZoneId.systemDefault())

        /** Formats a movie's watched date as e.g. `Jan 15` for the Movies breakdown. */
        private val MOVIE_DATE_FORMAT: DateTimeFormatter =
            DateTimeFormatter.ofPattern("MMM d").withZone(ZoneId.systemDefault())

        private val HC_STEPS_PERMISSION =
            HealthPermission.getReadPermission(StepsRecord::class)
        private val HC_MINDFULNESS_PERMISSION =
            HealthPermission.getReadPermission(MindfulnessSessionRecord::class)
        private val HC_EXERCISE_PERMISSION =
            HealthPermission.getReadPermission(ExerciseSessionRecord::class)
        private val HC_DISTANCE_PERMISSION =
            HealthPermission.getReadPermission(DistanceRecord::class)
        private val HC_SLEEP_PERMISSION =
            HealthPermission.getReadPermission(SleepSessionRecord::class)

        /**
         * Every Health Connect permission this sample app wants. Clicking the single
         * "Grant" row in the UI launches one prompt for every item here that is not
         * already granted.
         */
        private val REQUIRED_HC_PERMISSIONS: Set<String> = setOf(
            HC_STEPS_PERMISSION,
            HC_MINDFULNESS_PERMISSION,
            HC_EXERCISE_PERMISSION,
            HC_DISTANCE_PERMISSION,
            HC_SLEEP_PERMISSION
        )

        /**
         * User-facing label for each required Health Connect permission. Used to render
         * the partial-grant status message ("missing: Exercise, …").
         */
        private val HC_PERMISSION_LABELS: Map<String, String> = mapOf(
            HC_STEPS_PERMISSION to "Steps",
            HC_MINDFULNESS_PERMISSION to "Mindfulness",
            HC_EXERCISE_PERMISSION to "Exercise",
            HC_DISTANCE_PERMISSION to "Distance",
            HC_SLEEP_PERMISSION to "Sleep"
        )
    }
}
