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
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.StepsRecord
import androidx.lifecycle.lifecycleScope
import com.tracker.core.Tracker
import com.tracker.core.result.LanguageLearningResult
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
        binding.btnGrantHc.setOnClickListener {
            hcPermissionLauncher.launch(setOf(HC_STEPS_PERMISSION))
        }
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
            val granted = HealthConnectClient.getOrCreate(this)
                .permissionController.getGrantedPermissions()
            val hasPermission = HC_STEPS_PERMISSION in granted
            binding.tvHcStatus.text =
                if (hasPermission) "Health Connect  ✓" else "Health Connect  ✗"
            binding.btnGrantHc.visibility = if (hasPermission) View.GONE else View.VISIBLE
            binding.layoutHcPermission.visibility = View.VISIBLE
        } else {
            binding.layoutHcPermission.visibility = View.GONE
        }
    }

    private fun queryMetrics() {
        binding.progressBar.visibility = View.VISIBLE
        binding.btnQuery.isEnabled = false

        lifecycleScope.launch {
            val language = runCatching { tracker.queryLanguageLearning() }.getOrNull()
            val reading  = runCatching { tracker.queryReading() }.getOrNull()
            val social   = runCatching { tracker.querySocialMedia() }.getOrNull()
            val movies   = runCatching { tracker.queryMovieWatching() }.getOrNull()
            val steps    = runCatching { tracker.queryStepCounting() }.getOrNull()

            displayResults(language, reading, social, movies, steps)

            binding.progressBar.visibility = View.GONE
            binding.btnQuery.isEnabled = true
        }
    }

    private fun displayResults(
        language: LanguageLearningResult?,
        reading: ReadingResult?,
        social: SocialMediaResult?,
        movies: MovieWatchingResult?,
        steps: StepCountingResult?
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
    }

    private fun pct(confidence: Float) = "%.0f%%".format(confidence * 100)

    companion object {
        private val HC_STEPS_PERMISSION = HealthPermission.getReadPermission(StepsRecord::class)
    }
}
