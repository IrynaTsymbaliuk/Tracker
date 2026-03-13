package com.tracker.core.provider

import android.app.usage.UsageStats
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.pm.PackageManager
import com.tracker.core.collector.NoMonitorableAppsException
import com.tracker.core.collector.PackageManagerException
import com.tracker.core.collector.PermissionDeniedException
import com.tracker.core.collector.SystemServiceUnavailableException
import com.tracker.core.config.KnownApps
import com.tracker.core.model.Evidence
import com.tracker.core.permission.Permission
import com.tracker.core.permission.PermissionManager
import com.tracker.core.permission.PermissionStatus
import com.tracker.core.result.AppInfo
import com.tracker.core.result.ReadingResult
import com.tracker.core.result.toConfidenceLevel
import com.tracker.core.result.toOccurred
import com.tracker.core.types.DataSource
import java.util.concurrent.TimeUnit
import kotlin.math.max

class ReadingProvider internal constructor(
    private val context: Context,
    private val permissionManager: PermissionManager
) : MetricProvider<ReadingResult> {

    private companion object {
        /**
         * Minimum session duration for reading to count as evidence.
         * Sessions shorter than this are filtered out.
         */
        const val READING_MIN_SESSION_MINUTES = 5

        /**
         * Penalty applied to combined confidence when all evidence is below minConfidence threshold.
         * This reduces confidence for cases where only weak signals exist.
         */
        const val WEAK_ONLY_PENALTY = 0.15f
    }

    override suspend fun query(
        fromMillis: Long,
        toMillis: Long,
        minConfidence: Float
    ): ReadingResult? {
        checkPermissions()

        val installedApps = getInstalledApps()

        val usageStatsList = getUsageStats(fromMillis, toMillis)

        val evidenceList =
            getEvidenceList(usageStatsList, installedApps)?.ifEmpty { return null } ?: return null

        var combinedConfidence = combineProbabilities(evidenceList.map { it.confidence })
        val totalDuration = evidenceList.mapNotNull { it.durationMinutes }.sum()

        if (evidenceList.all { it.confidence < minConfidence }) {
            combinedConfidence = max(0f, combinedConfidence - WEAK_ONLY_PENALTY)
        }

        val occurred = combinedConfidence.toOccurred(minConfidence)
        val confidenceLevel = combinedConfidence.toConfidenceLevel()

        val apps = evidenceList.mapNotNull { ev ->
            val packageName = ev.metadata["packageName"] as? String
            val appName = ev.metadata["appName"] as? String
            if (packageName != null && appName != null) {
                AppInfo(packageName, appName)
            } else {
                null
            }
        }.distinctBy { it.packageName }

        val primarySource = DataSource.USAGE_STATS

        return ReadingResult(
            occurred = occurred,
            confidence = combinedConfidence,
            confidenceLevel = confidenceLevel,
            durationMinutes = if (totalDuration > 0) totalDuration else null,
            sessionCount = evidenceList.size,
            source = primarySource,
            apps = apps
        )
    }

    private fun checkPermissions() {
        if (permissionManager.checkPermission(Permission.PACKAGE_USAGE_STATS) != PermissionStatus.GRANTED) {
            throw PermissionDeniedException("PACKAGE_USAGE_STATS")
        }
    }

    private fun getInstalledApps(): Set<String> {
        try {
            val allInstalledPackages =
                context.packageManager.getInstalledApplications(PackageManager.GET_META_DATA)
                    .map { it.packageName }.toSet()
            val installedReadingApps =
                KnownApps.reading.keys.filter { it in allInstalledPackages }.toSet()

            if (installedReadingApps.isEmpty()) {
                throw NoMonitorableAppsException()
            }
            return installedReadingApps
        } catch (e: Exception) {
            throw PackageManagerException(e)
        }
    }

    private fun getUsageStats(fromMillis: Long, toMillis: Long): List<UsageStats?>? {
        val usageStatsManager =
            context.getSystemService(Context.USAGE_STATS_SERVICE) as? UsageStatsManager
                ?: throw SystemServiceUnavailableException("UsageStatsManager")
        return usageStatsManager.queryUsageStats(
            UsageStatsManager.INTERVAL_DAILY,
            fromMillis,
            toMillis
        )
    }

    private fun getEvidenceList(
        usageStatsList: List<UsageStats?>?,
        installedApps: Set<String>
    ): List<Evidence>? {
        return usageStatsList?.mapNotNull { usageStats ->
            usageStats?.let {
                val packageName = usageStats.packageName ?: return@mapNotNull null
                val appMetadata = KnownApps.reading[packageName] ?: return@mapNotNull null

                if (packageName !in installedApps) null

                val totalTimeMillis = usageStats.totalTimeInForeground
                val totalTimeMinutes = TimeUnit.MILLISECONDS.toMinutes(totalTimeMillis).toInt()

                if (totalTimeMinutes < 0 || totalTimeMinutes < READING_MIN_SESSION_MINUTES) null

                Evidence(
                    source = DataSource.USAGE_STATS,
                    timestampMillis = usageStats.firstTimeStamp,
                    confidence = appMetadata.confidenceMultiplier,
                    durationMinutes = totalTimeMinutes,
                    startTimeMillis = usageStats.firstTimeStamp,
                    endTimeMillis = usageStats.lastTimeStamp,
                    metadata = mapOf(
                        "packageName" to packageName,
                        "appName" to getAppName(context.packageManager, packageName)
                    )
                )
            }
        }
    }

    /**
     * Get human-readable app name from package name.
     */
    private fun getAppName(packageManager: PackageManager, packageName: String): String {
        return try {
            val appInfo = packageManager.getApplicationInfo(packageName, 0)
            packageManager.getApplicationLabel(appInfo).toString()
        } catch (e: Exception) {
            packageName
        }
    }

    /**
     * Combine independent probabilities using the formula:
     * combined = 1 - ∏(1 - p_i)
     *
     * This represents the probability that at least one of the independent
     * events is true (i.e., at least one session was genuine).
     */
    private fun combineProbabilities(confidences: List<Float>): Float {
        if (confidences.isEmpty()) return 0f
        if (confidences.size == 1) return confidences.first()

        var product = 1.0f
        for (confidence in confidences) {
            product *= (1.0f - confidence)
        }

        return 1.0f - product
    }
}
