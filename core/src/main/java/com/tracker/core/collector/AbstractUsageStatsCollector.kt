package com.tracker.core.collector

import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.pm.PackageManager
import com.tracker.core.config.AppMetadata
import com.tracker.core.model.Evidence
import com.tracker.core.permission.Permission
import com.tracker.core.permission.PermissionManager
import com.tracker.core.permission.PermissionStatus
import com.tracker.core.types.DataSource
import java.util.concurrent.TimeUnit

/**
 * Abstract base class for collectors that use UsageStats API.
 *
 * This class encapsulates the common logic for:
 * 1. Permission checking (PACKAGE_USAGE_STATS)
 * 2. Querying UsageStatsManager
 * 3. Filtering installed apps
 * 4. Creating Evidence from usage data
 *
 * Subclasses must provide:
 * - Which known apps to monitor
 * - Minimum session duration threshold
 */
internal abstract class AbstractUsageStatsCollector(
    private val context: Context,
    private val permissionManager: PermissionManager
) : Collector {

    override val sourceRequirements: Set<com.tracker.core.result.AccessRequirement> = setOf(
        com.tracker.core.result.AccessRequirement.SystemPermission(
            com.tracker.core.result.AccessRequirement.PERMISSION_USAGE_STATS
        )
    )

    override val sourceName: String = "Usage Stats"

    override val reliabilityContribution: com.tracker.core.result.ReliabilityLevel =
        com.tracker.core.result.ReliabilityLevel.MEDIUM

    private val usageStatsManager by lazy {
        context.getSystemService(Context.USAGE_STATS_SERVICE) as? UsageStatsManager
    }

    private val packageManager by lazy {
        context.packageManager
    }

    /**
     * Get the map of known apps to monitor for this habit type.
     */
    protected abstract fun getKnownApps(): Map<String, AppMetadata>

    /**
     * Get the minimum session duration in minutes for this habit type.
     */
    protected abstract fun getMinSessionMinutes(): Int

    override suspend fun collect(fromMillis: Long, toMillis: Long): Result<List<Evidence>> {
        // Check permission first
        if (permissionManager.checkPermission(Permission.PACKAGE_USAGE_STATS) != PermissionStatus.GRANTED) {
            return Result.failure(PermissionDeniedException("PACKAGE_USAGE_STATS"))
        }

        // Check if UsageStatsManager is available
        val usageStatsManager = this.usageStatsManager
            ?: return Result.failure(SystemServiceUnavailableException("UsageStatsManager"))

        // Get installed apps for this habit type
        val installedApps = try {
            getInstalledMonitoredApps()
        } catch (e: Exception) {
            return Result.failure(PackageManagerException(e))
        }

        if (installedApps.isEmpty()) {
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
        val knownApps = getKnownApps()
        val minSessionMinutes = getMinSessionMinutes()

        for (usageStats in usageStatsList) {
            val packageName = usageStats.packageName
            val appMetadata = knownApps[packageName] ?: continue

            // Skip if app is not installed (shouldn't happen, but safety check)
            if (packageName !in installedApps) continue

            // Get total foreground time for this app
            val totalTimeMillis = usageStats.totalTimeInForeground
            val totalTimeMinutes = TimeUnit.MILLISECONDS.toMinutes(totalTimeMillis).toInt()

            // Skip if doesn't meet minimum session duration
            if (totalTimeMinutes < minSessionMinutes) continue

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
     * Get list of installed apps that are being monitored (package names).
     */
    private fun getInstalledMonitoredApps(): Set<String> {
        val installedApps = packageManager.getInstalledApplications(PackageManager.GET_META_DATA)
        val installedPackages = installedApps.map { it.packageName }.toSet()

        return getKnownApps().keys.filter { it in installedPackages }.toSet()
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
