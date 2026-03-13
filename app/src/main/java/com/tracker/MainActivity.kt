package com.tracker

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.tracker.core.Tracker
import com.tracker.core.types.Metric
import com.tracker.databinding.ActivityMainBinding
import kotlinx.coroutines.launch

/**
 * Sample app demonstrating Tracker library usage.
 *
 * This app shows:
 * 1. How to build a Tracker instance
 * 2. How to check and request access using library methods
 * 3. How to query metrics for the last 24 hours using coroutines
 * 4. How to display results (activity data and data quality)
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
            .requestMetrics(Metric.LANGUAGE_LEARNING, Metric.READING)
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
            // Use library method to request missing access
            tracker.requestMissingAccess(this)
        }
    }

    /**
     * Step 3: Check access and query if granted
     */
    private fun checkAccessAndQuery() {
        // Use library method to check if all required access is granted
        if (tracker.hasAllRequiredAccess()) {
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
                // Query for the last 24 hours
                val result = tracker.queryAsync()

                // Display results
                displayResults(result)
            } catch (e: Exception) {
                showError(e.message ?: "Unknown error")
            }
        }
    }

    /**
     * Step 5: Display results in the UI
     */
    private fun displayResults(result: com.tracker.core.result.MetricsResult) {
        hideLoading()

        with(binding) {
            // Time period
            tvTimeRange.text = "Last 24 Hours"

            // Language Learning results
            val llResult = result.languageLearning
            if (llResult != null && llResult.occurred) {
                tvLanguageLearningStatus.text = "✓ Activity Detected"
                tvLanguageLearningDuration.text = "Duration: ${llResult.durationMinutes} minutes"
                tvLanguageLearningConfidence.text = "Confidence: ${formatConfidence(llResult.confidence)} (${llResult.confidenceLevel})"
                tvLanguageLearningApps.text = if (llResult.apps.isNotEmpty()) {
                    "Apps: ${llResult.apps.joinToString(", ") { it.appName }}"
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
            val readingResult = result.reading
            if (readingResult != null && readingResult.occurred) {
                tvReadingStatus.text = "✓ Activity Detected"
                tvReadingDuration.text = "Duration: ${readingResult.durationMinutes} minutes"
                tvReadingConfidence.text = "Confidence: ${formatConfidence(readingResult.confidence)} (${readingResult.confidenceLevel})"
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

            // Data Quality info
            tvDataQuality.text = buildDataQualityText(result.dataQuality)

            // Show results section
            resultSection.visibility = View.VISIBLE
        }
    }

    private fun formatConfidence(confidence: Float): String {
        return String.format("%.0f%%", confidence * 100)
    }

    private fun buildDataQualityText(dataQuality: com.tracker.core.result.DataQuality): String {
        return buildString {
            appendLine("Overall Reliability: ${dataQuality.overallReliability}")
            if (dataQuality.availableSources.isNotEmpty()) {
                appendLine("Available Sources: ${dataQuality.availableSources.joinToString(", ")}")
            }
            if (dataQuality.missingSources.isNotEmpty()) {
                appendLine("Missing Sources: ${dataQuality.missingSources.size}")
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // Re-check access when returning from Settings
        checkAccessAndQuery()
    }

    // UI State Management

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
        binding.tvDataQuality.text = "Error: $message"
        binding.resultSection.visibility = View.VISIBLE
    }
}
