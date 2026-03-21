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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        tracker = Tracker.Builder(this.applicationContext)
            .enableReading()
            .enableLanguageLearning()
            .enableSocialMedia()
            .enableMovieWatching()
            .setLetterboxdUsername(letterboxdUsername)
            .setMinConfidence(0.50f)
            .build()

        binding.btnRequestPermission.setOnClickListener {
            startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS))
        }
        binding.btnQuery.setOnClickListener { queryMetrics() }
    }

    override fun onResume() {
        super.onResume()
        if (hasUsageStatsPermission()) {
            binding.permissionSection.visibility = View.GONE
            binding.btnQuery.visibility = View.VISIBLE
            queryMetrics()
        } else {
            binding.permissionSection.visibility = View.VISIBLE
            binding.btnQuery.visibility = View.GONE
            binding.resultSection.visibility = View.GONE
        }
    }


    private fun queryMetrics() {
        binding.progressBar.visibility = View.VISIBLE
        binding.btnQuery.isEnabled = false

        lifecycleScope.launch {
            try {
                val learning = tracker.queryLanguageLearning()
                val reading = tracker.queryReading()
                val social = tracker.querySocialMedia()
                val movies = tracker.queryMovieWatching()
                displayResults(learning, reading, social, movies)
            } catch (e: Exception) {
                Toast.makeText(this@MainActivity, "Query failed: ${e.message}", Toast.LENGTH_LONG)
                    .show()
            } finally {
                binding.progressBar.visibility = View.GONE
                binding.btnQuery.isEnabled = true
            }
        }
    }

    private fun displayResults(
        learning: LanguageLearningResult?,
        reading: ReadingResult?,
        social: SocialMediaResult?,
        movies: MovieWatchingResult?
    ) {
        with(binding) {
            tvTimeRange.text = "Last 24 hours"

            // Language Learning
            when {
                learning == null -> {
                    tvLanguageLearningStatus.text = "No data"
                    tvLanguageLearningDuration.text = ""
                    tvLanguageLearningConfidence.text = ""
                    tvLanguageLearningApps.text = ""
                }

                !learning.occurred -> {
                    tvLanguageLearningStatus.text = "No activity detected"
                    tvLanguageLearningDuration.text = "Duration: 0 min"
                    tvLanguageLearningConfidence.text =
                        "Confidence: ${formatConfidence(learning.confidence)} (${learning.confidenceLevel})"
                    tvLanguageLearningApps.text = ""
                }

                else -> {
                    tvLanguageLearningStatus.text = "✓ Detected"
                    tvLanguageLearningDuration.text = "Duration: ${learning.durationMinutes} min (${learning.sessionCount} sessions)"
                    tvLanguageLearningConfidence.text =
                        "Confidence: ${formatConfidence(learning.confidence)} (${learning.confidenceLevel})"
                    tvLanguageLearningApps.text =
                        "Apps: ${learning.apps.joinToString(", ") { it.appName }}"
                }
            }

            // Reading
            when {
                reading == null -> {
                    tvReadingStatus.text = "No data"
                    tvReadingDuration.text = ""
                    tvReadingConfidence.text = ""
                    tvReadingApps.text = ""
                }

                !reading.occurred -> {
                    tvReadingStatus.text = "No activity detected"
                    tvReadingDuration.text = "Duration: 0 min"
                    tvReadingConfidence.text =
                        "Confidence: ${formatConfidence(reading.confidence)} (${reading.confidenceLevel})"
                    tvReadingApps.text = ""
                }

                else -> {
                    tvReadingStatus.text = "✓ Detected"
                    tvReadingDuration.text = "Duration: ${reading.durationMinutes} min (${reading.sessionCount} sessions)"
                    tvReadingConfidence.text =
                        "Confidence: ${formatConfidence(reading.confidence)} (${reading.confidenceLevel})"
                    tvReadingApps.text = "Apps: ${reading.apps.joinToString(", ") { it.appName }}"
                }
            }

            // Social Media
            when {
                social == null -> {
                    tvSocialMediaStatus.text = "No data"
                    tvSocialMediaDuration.text = ""
                    tvSocialMediaConfidence.text = ""
                    tvSocialMediaApps.text = ""
                }

                !social.occurred -> {
                    tvSocialMediaStatus.text = "No activity detected"
                    tvSocialMediaDuration.text = "Duration: 0 min"
                    tvSocialMediaConfidence.text =
                        "Confidence: ${formatConfidence(social.confidence)} (${social.confidenceLevel})"
                    tvSocialMediaApps.text = ""
                }

                else -> {
                    tvSocialMediaStatus.text = "✓ Detected"
                    tvSocialMediaDuration.text = "Duration: ${social.durationMinutes} min (${social.sessionCount} sessions)"
                    tvSocialMediaConfidence.text =
                        "Confidence: ${formatConfidence(social.confidence)} (${social.confidenceLevel})"
                    tvSocialMediaApps.text = "Apps: ${social.apps.joinToString(", ") { it.appName }}"
                }
            }

            // Movie Watching
            when {
                movies == null && letterboxdUsername == null -> {
                    tvMovieWatchingStatus.text =
                        "Not configured — set letterboxdUsername in MainActivity"
                    tvMovieWatchingCount.text = ""
                    tvMovieWatchingConfidence.text = ""
                    tvMovieWatchingMovies.text = ""
                }

                movies == null -> {
                    tvMovieWatchingStatus.text = "No data — feed unavailable"
                    tvMovieWatchingCount.text = ""
                    tvMovieWatchingConfidence.text = ""
                    tvMovieWatchingMovies.text = ""
                }

                !movies.occurred -> {
                    tvMovieWatchingStatus.text = "No films logged today"
                    tvMovieWatchingCount.text = "Count: 0"
                    tvMovieWatchingConfidence.text =
                        "Confidence: ${formatConfidence(movies.confidence)} (${movies.confidenceLevel})"
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
