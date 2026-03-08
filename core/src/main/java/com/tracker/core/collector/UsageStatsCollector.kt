package com.tracker.core.collector

import android.app.usage.UsageStats
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.pm.PackageManager
import com.tracker.core.config.KnownApps
import com.tracker.core.model.Evidence
import com.tracker.core.permission.Permission
import com.tracker.core.permission.PermissionManager
import com.tracker.core.permission.PermissionStatus
import com.tracker.core.types.DataSource
import java.util.concurrent.TimeUnit

/**
 * Collects language learning evidence from UsageStats API.
 *
 * This collector:
 * 1. Checks PACKAGE_USAGE_STATS permission (returns empty if missing)
 * 2. Gets list of installed packages
 * 3. Filters for known language learning apps
 * 4. Queries UsageStats for time range
 * 5. Creates Evidence for sessions that exceed minimum duration
 */
class UsageStatsCollector(
    private val context: Context,
    private val permissionManager: PermissionManager
) : Collector {

    private val usageStatsManager by lazy {
        context.getSystemService(Context.USAGE_STATS_SERVICE) as? UsageStatsManager
    }

    private val packageManager by lazy {
        context.packageManager
    }

    override suspend fun collect(fromMillis: Long, toMillis: Long): Result<List<Evidence>> {
        // Check permission first
        if (permissionManager.checkPermission(Permission.PACKAGE_USAGE_STATS) != PermissionStatus.GRANTED) {
            return Result.failure(PermissionDeniedException("PACKAGE_USAGE_STATS"))
        }

        // Check if UsageStatsManager is available
        val usageStatsManager = this.usageStatsManager
            ?: return Result.failure(SystemServiceUnavailableException("UsageStatsManager"))

        // Get installed language learning apps
        val installedLanguageApps = try {
            getInstalledLanguageLearningApps()
        } catch (e: Exception) {
            return Result.failure(PackageManagerException(e))
        }

        if (installedLanguageApps.isEmpty()) {
            return Result.failure(NoMonitorableAppsException())
        }

        // Query usage stats for the time range
        val usageStatsList = usageStatsManager.queryUsageStats(
            UsageStatsManager.INTERVAL_DAILY,
            fromMillis,
            toMillis
        ) ?: return Result.success(emptyList())

        // Build evidence from usage stats
        val evidenceList = mutableListOf<Evidence>()

        for (usageStats in usageStatsList) {
            val packageName = usageStats.packageName
            val appMetadata = KnownApps.getLanguageLearningApp(packageName) ?: continue

            // Skip if app is not installed (shouldn't happen, but safety check)
            if (packageName !in installedLanguageApps) continue

            // Get total foreground time for this app
            val totalTimeMillis = usageStats.totalTimeInForeground
            val totalTimeMinutes = TimeUnit.MILLISECONDS.toMinutes(totalTimeMillis).toInt()

            // Skip if doesn't meet minimum session duration
            if (totalTimeMinutes < appMetadata.minSessionMinutes) continue

            // Create evidence for this session
            val evidence = Evidence(
                source = DataSource.USAGE_STATS,
                timestampMillis = usageStats.firstTimeStamp,
                confidence = appMetadata.confidenceMultiplier,
                durationMinutes = totalTimeMinutes,
                startTimeMillis = usageStats.firstTimeStamp,
                endTimeMillis = usageStats.lastTimeStamp,
                metadata = mapOf(
                    "packageName" to packageName,
                    "appName" to getAppName(packageName)
                )
            )

            evidenceList.add(evidence)
        }

        return Result.success(evidenceList)
    }

    /**
     * Get list of installed language learning apps (package names).
     */
    private fun getInstalledLanguageLearningApps(): Set<String> {
        val installedApps = packageManager.getInstalledApplications(PackageManager.GET_META_DATA)
        val installedPackages = installedApps.map { it.packageName }.toSet()

        return KnownApps.languageLearning.keys.filter { it in installedPackages }.toSet()
    }

    /**
     * Get human-readable app name from package name.
     */
    private fun getAppName(packageName: String): String {
        return try {
            val appInfo = packageManager.getApplicationInfo(packageName, 0)
            packageManager.getApplicationLabel(appInfo).toString()
        } catch (e: Exception) {
            packageName
        }
    }
}
