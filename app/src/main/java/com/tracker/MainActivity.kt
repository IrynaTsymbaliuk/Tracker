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
 * 3. How to query metrics using coroutines
 * 4. How to display results (summary, day-by-day data, data quality)
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
            .setLookbackDays(30)  // Query last 30 days
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
                // Query using the default lookback period (30 days)
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
            // Summary statistics
            tvTotalDays.text = "Total Days: ${result.summary.totalDays}"

            // Language Learning summary
            tvLanguageLearningDays.text = "Language Learning Days: ${result.summary.languageLearningDays ?: 0}"
            tvLanguageLearningAverage.text = "Avg Minutes/Day: ${formatAverage(result.summary.averageLanguageLearningMinutes)}"

            // Reading summary
            tvReadingDays.text = "Reading Days: ${result.summary.readingDays ?: 0}"
            tvReadingAverage.text = "Avg Minutes/Day: ${formatAverage(result.summary.averageReadingMinutes)}"

            // Day-by-day results
            tvDayByDayResults.text = buildDayByDayText(result.days)

            // Show results section
            resultSection.visibility = View.VISIBLE
        }
    }

    private fun formatAverage(average: Float?): String {
        return if (average != null) {
            String.format("%.1f", average)
        } else {
            "0.0"
        }
    }

    private fun buildDayByDayText(days: List<com.tracker.core.result.DayResult>): String {
        if (days.isEmpty()) {
            return "No activity found in the last 30 days."
        }

        return buildString {
            appendLine("Found ${days.size} days with activity:\n")
            days.take(10).forEach { day ->  // Show first 10 days
                val date = java.text.SimpleDateFormat("MMM dd, yyyy", java.util.Locale.getDefault())
                    .format(java.util.Date(day.timestampMillis))

                val ll = day.languageLearning
                val reading = day.reading
                val hasActivity = (ll != null && ll.occurred) || (reading != null && reading.occurred)

                if (hasActivity) {
                    appendLine("$date:")

                    // Show language learning if present (same order as summary)
                    if (ll != null && ll.occurred) {
                        appendLine("  📚 Language Learning:")
                        appendLine("    Duration: ${ll.durationMinutes} min")
                        appendLine("    Confidence: ${String.format("%.0f%%", ll.confidence * 100)} (${ll.confidenceLevel})")
                        if (ll.apps.isNotEmpty()) {
                            appendLine("    Apps: ${ll.apps.joinToString(", ") { it.appName }}")
                        }
                    }

                    // Show reading if present (same order as summary)
                    if (reading != null && reading.occurred) {
                        appendLine("  📖 Reading:")
                        appendLine("    Duration: ${reading.durationMinutes} min")
                        appendLine("    Confidence: ${String.format("%.0f%%", reading.confidence * 100)} (${reading.confidenceLevel})")
                        if (reading.apps.isNotEmpty()) {
                            appendLine("    Apps: ${reading.apps.joinToString(", ") { it.appName }}")
                        }
                    }

                    appendLine()
                }
            }

            if (days.size > 10) {
                appendLine("... and ${days.size - 10} more days")
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
        binding.tvDayByDayResults.text = "Error: $message"
        binding.resultSection.visibility = View.VISIBLE
    }
}
