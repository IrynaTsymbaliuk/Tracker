@file:OptIn(ExperimentalMindfulnessSessionApi::class)

package com.tracker

import android.app.AppOpsManager
import android.content.Intent
import android.os.Bundle
import android.os.Process
import android.provider.Settings
import android.view.View
import androidx.activity.result.ActivityResultLauncher
import androidx.appcompat.app.AppCompatActivity
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.PermissionController
import androidx.health.connect.client.feature.ExperimentalMindfulnessSessionApi
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.ExerciseSessionRecord
import androidx.health.connect.client.records.MindfulnessSessionRecord
import androidx.health.connect.client.records.StepsRecord
import androidx.lifecycle.lifecycleScope
import com.tracker.core.Tracker
import com.tracker.core.result.ExerciseResult
import com.tracker.core.result.LanguageLearningResult
import com.tracker.core.result.MeditationResult
import com.tracker.core.result.MovieWatchingResult
import com.tracker.core.result.ReadingResult
import com.tracker.core.result.SocialMediaResult
import com.tracker.core.result.StepCountingResult
import com.tracker.databinding.ActivityMainBinding
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private val letterboxdUsername = "Ts_Irena"

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

        tracker = Tracker.Builder(this)
            .setLetterboxdUsername(letterboxdUsername)
            .build()

        binding.btnGrantUsage.setOnClickListener {
            startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS))
        }
        binding.btnGrantHc.setOnClickListener { requestMissingHcPermissions() }
        binding.btnQuery.setOnClickListener { queryMetrics() }
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
        binding.btnGrantUsage.visibility = if (granted) View.GONE else View.VISIBLE
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
                allGranted -> "Health Connect  ✓ (Steps + Mindfulness + Exercise)"
                missing.size == REQUIRED_HC_PERMISSIONS.size -> "Health Connect  ✗"
                else -> {
                    val missingLabels = missing.mapNotNull { HC_PERMISSION_LABELS[it] }
                    "Health Connect  ◐ (missing: ${missingLabels.joinToString(", ")})"
                }
            }
            binding.btnGrantHc.visibility = if (allGranted) View.GONE else View.VISIBLE
            binding.layoutHcPermission.visibility = View.VISIBLE
        } else {
            binding.layoutHcPermission.visibility = View.GONE
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
        binding.progressBar.visibility = View.VISIBLE
        binding.btnQuery.isEnabled = false

        lifecycleScope.launch {
            val language   = runCatching { tracker.queryLanguageLearning() }.getOrNull()
            val reading    = runCatching { tracker.queryReading() }.getOrNull()
            val social     = runCatching { tracker.querySocialMedia() }.getOrNull()
            val movies     = runCatching { tracker.queryMovieWatching() }.getOrNull()
            val steps      = runCatching { tracker.queryStepCounting() }.getOrNull()
            val meditation = runCatching { tracker.queryMeditation() }.getOrNull()
            val exercise   = runCatching { tracker.queryExercise() }.getOrNull()

            displayResults(language, reading, social, movies, steps, meditation, exercise)

            binding.progressBar.visibility = View.GONE
            binding.btnQuery.isEnabled = true
        }
    }

    private fun displayResults(
        language: LanguageLearningResult?,
        reading: ReadingResult?,
        social: SocialMediaResult?,
        movies: MovieWatchingResult?,
        steps: StepCountingResult?,
        meditation: MeditationResult?,
        exercise: ExerciseResult?
    ) {
        binding.tvLanguage.text = language
            ?.let { "📚 Language    ${it.durationMinutes} min · ${it.confidenceLevel} · ${pct(it.confidence)}" }
            ?: "📚 Language    —"

        binding.tvReading.text = reading
            ?.let { "📖 Reading    ${it.durationMinutes} min · ${it.confidenceLevel} · ${pct(it.confidence)}" }
            ?: "📖 Reading    —"

        binding.tvSocial.text = social
            ?.let { "📱 Social    ${it.durationMinutes} min · ${it.confidenceLevel} · ${pct(it.confidence)}" }
            ?: "📱 Social    —"

        binding.tvMovies.text = movies
            ?.let { "🎬 Movies    ${it.count} film${if (it.count != 1) "s" else ""} · ${it.confidenceLevel} · ${pct(it.confidence)}" }
            ?: "🎬 Movies    —"

        binding.tvSteps.text = steps
            ?.let { "👣 Steps    ${"%,d".format(it.steps)} steps · ${it.confidenceLevel} · ${pct(it.confidence)}" }
            ?: "👣 Steps    —"

        binding.tvMeditation.text = meditation
            ?.let {
                val sessionCount = it.sessions.size
                val sessionLabel = if (sessionCount == 1) "session" else "sessions"
                "🧘 Meditation    ${it.durationMinutes} min · $sessionCount $sessionLabel · ${sourcesLabel(it)} · ${pct(it.confidence)}"
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
                "🏃 Exercise    ${it.durationMinutes} min · $sessionCount $sessionLabel$typesSuffix · ${pct(it.confidence)}"
            }
            ?: "🏃 Exercise    —"
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

    /** Short human-readable label for the list of contributing data sources. */
    private fun sourcesLabel(result: MeditationResult): String = result.sources
        .joinToString("+") { src ->
            when (src.name) {
                "HEALTH_CONNECT" -> "HC"
                "USAGE_STATS" -> "Usage"
                else -> src.name
            }
        }

    private fun pct(confidence: Float) = "%.0f%%".format(confidence * 100)

    companion object {
        private val HC_STEPS_PERMISSION =
            HealthPermission.getReadPermission(StepsRecord::class)
        private val HC_MINDFULNESS_PERMISSION =
            HealthPermission.getReadPermission(MindfulnessSessionRecord::class)
        private val HC_EXERCISE_PERMISSION =
            HealthPermission.getReadPermission(ExerciseSessionRecord::class)

        /**
         * Every Health Connect permission this sample app wants. Clicking the single
         * "Grant" row in the UI launches one prompt for every item here that is not
         * already granted.
         */
        private val REQUIRED_HC_PERMISSIONS: Set<String> = setOf(
            HC_STEPS_PERMISSION,
            HC_MINDFULNESS_PERMISSION,
            HC_EXERCISE_PERMISSION
        )

        /**
         * User-facing label for each required Health Connect permission. Used to render
         * the partial-grant status message ("missing: Exercise, …").
         */
        private val HC_PERMISSION_LABELS: Map<String, String> = mapOf(
            HC_STEPS_PERMISSION to "Steps",
            HC_MINDFULNESS_PERMISSION to "Mindfulness",
            HC_EXERCISE_PERMISSION to "Exercise"
        )
    }
}
