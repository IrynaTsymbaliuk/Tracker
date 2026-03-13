package com.tracker

import android.app.AppOpsManager
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.tracker.core.Tracker
import com.tracker.core.result.HabitResult
import com.tracker.core.result.LanguageLearningResult
import com.tracker.core.result.ReadingResult
import com.tracker.databinding.ActivityMainBinding
import kotlinx.coroutines.launch

/**
 * Sample app demonstrating Tracker library usage.
 *
 * This app shows:
 * 1. How to build a Tracker instance
 * 2. How to check and request access per-metric
 * 3. How to query individual metrics for the last 24 hours using coroutines
 * 4. How to display results (activity data and data quality per metric)
 */
class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var tracker: Tracker

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupTracker()
        setupUI()
        checkAccessAndQuery()
    }

    /**
     * Step 1: Build the Tracker instance
     */
    private fun setupTracker() {
        tracker = Tracker.Builder(this)
            .setMinConfidence(0.50f)  // 50% confidence threshold
            .build()
    }

    /**
     * Step 2: Setup UI click listeners
     */
    private fun setupUI() {
        binding.btnQuery.setOnClickListener {
            checkAccessAndQuery()
        }

        binding.btnRequestPermission.setOnClickListener {
            startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS))
        }
    }

    /**
     * Step 3: Check access and query if granted
     */
    private fun checkAccessAndQuery() {
        val appOpsManager = getSystemService(APP_OPS_SERVICE) as AppOpsManager
        val mode = appOpsManager.checkOpNoThrow(
            AppOpsManager.OPSTR_GET_USAGE_STATS,
            android.os.Process.myUid(),
            packageName
        )
        val hasAccess = mode == AppOpsManager.MODE_ALLOWED

        if (hasAccess) {
            showPermissionGranted()
            queryMetrics()
        } else {
            showPermissionDenied()
        }
    }

    /**
     * Step 4: Query metrics using coroutines
     */
    private fun queryMetrics() {
        showLoading()

        lifecycleScope.launch {
            try {
                // Query each metric for the last 24 hours
                val languageLearningResult = tracker.queryLanguageLearning()
                val readingResult = tracker.queryReading()

                // Display results
                displayResults(languageLearningResult, readingResult)
            } catch (e: Exception) {
                showError(e.message ?: "Unknown error")
            }
        }
    }

    /**
     * Step 5: Display results in the UI
     */
    private fun displayResults(
        languageLearningResult: HabitResult?,
        readingResult: HabitResult?
    ) {
        hideLoading()

        with(binding) {
            // Time period
            tvTimeRange.text = "Last 24 Hours"

            // Language Learning results
            if (languageLearningResult != null && languageLearningResult.occurred && languageLearningResult is LanguageLearningResult) {
                tvLanguageLearningStatus.text = "✓ Activity Detected"
                tvLanguageLearningDuration.text =
                    "Duration: ${languageLearningResult.durationMinutes} minutes"
                tvLanguageLearningConfidence.text =
                    "Confidence: ${formatConfidence(languageLearningResult.confidence)} (${languageLearningResult.confidenceLevel})"
                tvLanguageLearningApps.text = if (languageLearningResult.apps.isNotEmpty()) {
                    "Apps: ${languageLearningResult.apps.joinToString(", ") { it.appName }}"
                } else {
                    "Apps: None detected"
                }
            } else {
                tvLanguageLearningStatus.text = "✗ No Activity"
                tvLanguageLearningDuration.text = "Duration: 0 minutes"
                tvLanguageLearningConfidence.text = "Confidence: N/A"
                tvLanguageLearningApps.text = "Apps: None"
            }

            // Reading results
            if (readingResult != null && readingResult.occurred && readingResult is ReadingResult) {
                tvReadingStatus.text = "✓ Activity Detected"
                tvReadingDuration.text = "Duration: ${readingResult.durationMinutes} minutes"
                tvReadingConfidence.text =
                    "Confidence: ${formatConfidence(readingResult.confidence)} (${readingResult.confidenceLevel})"
                tvReadingApps.text = if (readingResult.apps.isNotEmpty()) {
                    "Apps: ${readingResult.apps.joinToString(", ") { it.appName }}"
                } else {
                    "Apps: None detected"
                }
            } else {
                tvReadingStatus.text = "✗ No Activity"
                tvReadingDuration.text = "Duration: 0 minutes"
                tvReadingConfidence.text = "Confidence: N/A"
                tvReadingApps.text = "Apps: None"
            }

            // Show results section
            resultSection.visibility = View.VISIBLE
        }
    }

    private fun formatConfidence(confidence: Float): String {
        return String.format("%.0f%%", confidence * 100)
    }

    override fun onResume() {
        super.onResume()
        checkAccessAndQuery()
    }

    private fun showPermissionGranted() {
        binding.permissionSection.visibility = View.GONE
        binding.btnQuery.visibility = View.VISIBLE
    }

    private fun showPermissionDenied() {
        binding.permissionSection.visibility = View.VISIBLE
        binding.btnQuery.visibility = View.GONE
        binding.resultSection.visibility = View.GONE
    }

    private fun showLoading() {
        binding.progressBar.visibility = View.VISIBLE
        binding.btnQuery.isEnabled = false
    }

    private fun hideLoading() {
        binding.progressBar.visibility = View.GONE
        binding.btnQuery.isEnabled = true
    }

    private fun showError(message: String) {
        hideLoading()
        binding.resultSection.visibility = View.VISIBLE
    }
}
