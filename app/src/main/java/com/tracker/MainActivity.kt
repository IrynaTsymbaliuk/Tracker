package com.tracker

import android.app.AppOpsManager
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.tracker.core.Tracker
import com.tracker.core.result.LanguageLearningResult
import com.tracker.core.result.MovieWatchingResult
import com.tracker.core.result.ReadingResult
import com.tracker.core.result.SocialMediaResult
import com.tracker.databinding.ActivityMainBinding
import kotlinx.coroutines.launch

/**
 * Demonstrates Tracker library integration:
 * 1. Build a Tracker instance with the Builder
 * 2. Check and request PACKAGE_USAGE_STATS permission
 * 3. Query individual metrics for the last 24 hours
 * 4. Display results — occurred state, duration, confidence level, contributing apps, and sessions
 *
 * Movie watching: set LETTERBOXD_USERNAME to your Letterboxd username to enable.
 */
class MainActivity : AppCompatActivity() {

    // Set your Letterboxd username here to enable movie watching tracking
    private val letterboxdUsername: String? = "Ts_Irena"

    private lateinit var binding: ActivityMainBinding
    private lateinit var tracker: Tracker
    private var selectedDays = 1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        tracker = Tracker.Builder(this.applicationContext)
            .setLetterboxdUsername(letterboxdUsername)
            .setMinConfidence(0.50f)
            .build()

        binding.chipGroupDays.setOnCheckedStateChangeListener { _, checkedIds ->
            selectedDays = when (checkedIds.firstOrNull()) {
                R.id.chip2Days -> 2
                R.id.chip7Days -> 7
                else -> 1
            }
            queryMetrics()
        }

        binding.btnRequestPermission.setOnClickListener {
            startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS))
        }
        binding.btnQuery.setOnClickListener { queryMetrics() }
    }

    override fun onResume() {
        super.onResume()
        if (hasUsageStatsPermission()) {
            binding.permissionSection.visibility = View.GONE
            binding.chipGroupDays.visibility = View.VISIBLE
            binding.btnQuery.visibility = View.VISIBLE
            queryMetrics()
        } else {
            binding.permissionSection.visibility = View.VISIBLE
            binding.chipGroupDays.visibility = View.GONE
            binding.btnQuery.visibility = View.GONE
            binding.resultSection.visibility = View.GONE
        }
    }

    private fun queryMetrics() {
        binding.progressBar.visibility = View.VISIBLE
        binding.btnQuery.isEnabled = false

        lifecycleScope.launch {
            var learning: LanguageLearningResult? = null
            var reading: ReadingResult? = null
            var social: SocialMediaResult? = null
            var movies: MovieWatchingResult? = null
            var movieError: String? = null

            try {
                learning = tracker.queryLanguageLearning(selectedDays)
                reading = tracker.queryReading(selectedDays)
                social = tracker.querySocialMedia(selectedDays)
            } catch (e: Exception) {
                Toast.makeText(this@MainActivity, "Query failed: ${e.message}", Toast.LENGTH_LONG)
                    .show()
            }

            try {
                movies = tracker.queryMovieWatching(selectedDays)
            } catch (_: IllegalStateException) {
                movieError = "Not configured — set letterboxdUsername in MainActivity"
            } catch (e: Exception) {
                movieError = "Feed unavailable: ${e.message}"
            }

            displayResults(learning, reading, social, movies, movieError)
            binding.progressBar.visibility = View.GONE
            binding.btnQuery.isEnabled = true
        }
    }

    private fun displayResults(
        learning: LanguageLearningResult?,
        reading: ReadingResult?,
        social: SocialMediaResult?,
        movies: MovieWatchingResult?,
        movieError: String? = null
    ) {
        with(binding) {
            tvTimeRange.text = if (selectedDays == 1) "Today" else "Last $selectedDays days"

            // Language Learning
            if (learning == null) {
                tvLanguageLearningStatus.text = "No data"
                tvLanguageLearningDuration.text = ""
                tvLanguageLearningConfidence.text = ""
                tvLanguageLearningApps.text = ""
            } else {
                tvLanguageLearningStatus.text = "✓ Detected"
                tvLanguageLearningDuration.text = "Duration: ${learning.durationMinutes} min (${learning.sessions.size} sessions)"
                tvLanguageLearningConfidence.text =
                    "Confidence: ${formatConfidence(learning.confidence)} (${learning.confidenceLevel})"
                tvLanguageLearningApps.text =
                    "Apps: ${learning.sessions.map { it.appName }.distinct().joinToString(", ")}"
            }

            // Reading
            if (reading == null) {
                tvReadingStatus.text = "No data"
                tvReadingDuration.text = ""
                tvReadingConfidence.text = ""
                tvReadingApps.text = ""
            } else {
                tvReadingStatus.text = "✓ Detected"
                tvReadingDuration.text = "Duration: ${reading.durationMinutes} min (${reading.sessions.size} sessions)"
                tvReadingConfidence.text =
                    "Confidence: ${formatConfidence(reading.confidence)} (${reading.confidenceLevel})"
                tvReadingApps.text = "Apps: ${reading.sessions.map { it.appName }.distinct().joinToString(", ")}"
            }

            // Social Media
            if (social == null) {
                tvSocialMediaStatus.text = "No data"
                tvSocialMediaDuration.text = ""
                tvSocialMediaConfidence.text = ""
                tvSocialMediaApps.text = ""
            } else {
                tvSocialMediaStatus.text = "✓ Detected"
                tvSocialMediaDuration.text = "Duration: ${social.durationMinutes} min (${social.sessions.size} sessions)"
                tvSocialMediaConfidence.text =
                    "Confidence: ${formatConfidence(social.confidence)} (${social.confidenceLevel})"
                tvSocialMediaApps.text = "Apps: ${social.sessions.map { it.appName }.distinct().joinToString(", ")}"
            }

            // Movie Watching
            when {
                movieError != null -> {
                    tvMovieWatchingStatus.text = movieError
                    tvMovieWatchingCount.text = ""
                    tvMovieWatchingConfidence.text = ""
                    tvMovieWatchingMovies.text = ""
                }

                movies == null -> {
                    tvMovieWatchingStatus.text = "No films logged in this period"
                    tvMovieWatchingCount.text = ""
                    tvMovieWatchingConfidence.text = ""
                    tvMovieWatchingMovies.text = ""
                }

                else -> {
                    tvMovieWatchingStatus.text =
                        "✓ ${movies.count} film${if (movies.count != 1) "s" else ""} logged"
                    tvMovieWatchingCount.text = "Count: ${movies.count}"
                    tvMovieWatchingConfidence.text =
                        "Confidence: ${formatConfidence(movies.confidence)} (${movies.confidenceLevel})"
                    tvMovieWatchingMovies.text =
                        "Films: ${movies.movies.joinToString(", ") { it.title }}"
                }
            }

            resultSection.visibility = View.VISIBLE
        }
    }

    private fun formatConfidence(confidence: Float) = String.format("%.0f%%", confidence * 100)

    private fun hasUsageStatsPermission(): Boolean {
        val appOpsManager = getSystemService(APP_OPS_SERVICE) as AppOpsManager
        val mode = appOpsManager.checkOpNoThrow(
            AppOpsManager.OPSTR_GET_USAGE_STATS,
            android.os.Process.myUid(),
            packageName
        )
        return mode == AppOpsManager.MODE_ALLOWED
    }
}
