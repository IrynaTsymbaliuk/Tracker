package com.tracker

import android.app.AppOpsManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Process
import android.provider.Settings
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
 * 2. How to check and request PACKAGE_USAGE_STATS permission
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
        checkPermissionAndQuery()
    }

    /**
     * Step 1: Build the Tracker instance
     */
    private fun setupTracker() {
        tracker = Tracker.Builder(this)
            .requestMetrics(Metric.LANGUAGE_LEARNING)
            .setLookbackDays(30)  // Query last 30 days
            .setMinConfidence(0.50f)  // 50% confidence threshold
            .build()
    }

    /**
     * Step 2: Setup UI click listeners
     */
    private fun setupUI() {
        binding.btnQuery.setOnClickListener {
            checkPermissionAndQuery()
        }

        binding.btnRequestPermission.setOnClickListener {
            requestUsageStatsPermission()
        }
    }

    /**
     * Step 3: Check permission and query if granted
     */
    private fun checkPermissionAndQuery() {
        if (hasUsageStatsPermission()) {
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
            tvLanguageLearningDays.text = "Language Learning Days: ${result.summary.languageLearningDays ?: 0}"
            tvAverageMinutes.text = "Average Minutes/Day: ${result.summary.averageLanguageLearningMinutes ?: 0}"

            // Data quality
            tvDataQuality.text = buildDataQualityText(result.dataQuality)

            // Day-by-day results
            tvDayByDayResults.text = buildDayByDayText(result.days)

            // Show results section
            resultSection.visibility = View.VISIBLE
        }
    }

    private fun buildDataQualityText(dataQuality: com.tracker.core.result.DataQuality): String {
        return buildString {
            appendLine("Reliability: ${dataQuality.overallReliability}")
            appendLine("Available Sources: ${dataQuality.availableSources.size}")
            appendLine("Missing Sources: ${dataQuality.missingSources.size}")

            if (dataQuality.recommendations.isNotEmpty()) {
                appendLine("\nRecommendations:")
                dataQuality.recommendations.forEach {
                    appendLine("  • $it")
                }
            }
        }
    }

    private fun buildDayByDayText(days: List<com.tracker.core.result.DayResult>): String {
        if (days.isEmpty()) {
            return "No language learning activity found in the last 30 days."
        }

        return buildString {
            appendLine("Found ${days.size} days with language learning activity:\n")
            days.take(10).forEach { day ->  // Show first 10 days
                val date = java.text.SimpleDateFormat("MMM dd, yyyy", java.util.Locale.getDefault())
                    .format(java.util.Date(day.timestampMillis))

                val ll = day.languageLearning
                if (ll != null && ll.occurred) {
                    appendLine("$date:")
                    appendLine("  Duration: ${ll.durationMinutes} minutes")
                    appendLine("  Confidence: ${String.format("%.0f%%", ll.confidence * 100)} (${ll.confidenceLevel})")
                    if (ll.apps.isNotEmpty()) {
                        appendLine("  Apps: ${ll.apps.joinToString(", ") { it.appName }}")
                    }
                    appendLine()
                }
            }

            if (days.size > 10) {
                appendLine("... and ${days.size - 10} more days")
            }
        }
    }

    /**
     * Check if PACKAGE_USAGE_STATS permission is granted
     */
    private fun hasUsageStatsPermission(): Boolean {
        val appOps = getSystemService(Context.APP_OPS_SERVICE) as? AppOpsManager ?: return false
        val mode = appOps.checkOpNoThrow(
            AppOpsManager.OPSTR_GET_USAGE_STATS,
            Process.myUid(),
            packageName
        )
        return mode == AppOpsManager.MODE_ALLOWED
    }

    /**
     * Request PACKAGE_USAGE_STATS permission by opening Settings
     */
    private fun requestUsageStatsPermission() {
        startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS))
    }

    override fun onResume() {
        super.onResume()
        // Re-check permission when returning from Settings
        checkPermissionAndQuery()
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
