package com.tracker.core.collector

import android.app.usage.UsageStats
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.pm.PackageManager
import com.tracker.core.config.AppMetadata
import com.tracker.core.model.DurationEvidence
import com.tracker.core.permission.Permission
import com.tracker.core.permission.PermissionManager
import com.tracker.core.permission.PermissionStatus
import com.tracker.core.types.DataSource
import java.util.concurrent.TimeUnit

/**
 * Collects app foreground time from [android.app.usage.UsageStatsManager].
 *
 * Requires the user to grant `PACKAGE_USAGE_STATS` via Settings > Apps > Special app access > Usage access.
 */
class UsageStatsCollector(private val context: Context, private val permissionManager: PermissionManager) {

    /**
     * Returns [DurationEvidence] for known, installed apps with foreground time >= [minSessionMinutes]
     * within the given time range.
     *
     * @throws PermissionDeniedException if `PACKAGE_USAGE_STATS` is not granted.
     * @throws SystemServiceUnavailableException if [android.app.usage.UsageStatsManager] is unavailable.
     * @throws PackageManagerException if querying installed apps fails.
     * @throws NoMonitorableAppsException if none of the [knownApps] are installed.
     */
    fun collect(
        fromMillis: Long,
        toMillis: Long,
        knownApps: Map<String, AppMetadata>,
        minSessionMinutes: Int
    ): List<DurationEvidence> {

        checkPermissions()

        val installedApps = getInstalledApps(knownApps)

        val usageStatsList = getUsageStats(fromMillis, toMillis)

        return getEvidenceList(knownApps, minSessionMinutes, usageStatsList, installedApps)
    }

    private fun checkPermissions() {
        if (permissionManager.checkPermission(Permission.PACKAGE_USAGE_STATS) != PermissionStatus.GRANTED) {
            throw PermissionDeniedException("PACKAGE_USAGE_STATS")
        }
    }

    private fun getInstalledApps(knownApps: Map<String, AppMetadata>): Set<String> {
        val allInstalledPackages = try {
            context.packageManager.getInstalledApplications(PackageManager.GET_META_DATA)
                .map { it.packageName }.toSet()
        } catch (e: Exception) {
            throw PackageManagerException(e)
        }

        val installedKnownApps = knownApps.keys.filter { it in allInstalledPackages }.toSet()

        if (installedKnownApps.isEmpty()) {
            throw NoMonitorableAppsException()
        }

        return installedKnownApps
    }

    private fun getUsageStats(fromMillis: Long, toMillis: Long): List<UsageStats> {
        val usageStatsManager =
            context.getSystemService(Context.USAGE_STATS_SERVICE) as? UsageStatsManager
                ?: throw SystemServiceUnavailableException("UsageStatsManager")
        return usageStatsManager.queryUsageStats(
            UsageStatsManager.INTERVAL_DAILY,
            fromMillis,
            toMillis
        ) ?: emptyList()
    }

    private fun getEvidenceList(
        knownApps: Map<String, AppMetadata>,
        minSessionMinutes: Int,
        usageStatsList: List<UsageStats>,
        installedApps: Set<String>
    ): List<DurationEvidence> {
        return usageStatsList.mapNotNull { usageStats ->
            val packageName = usageStats.packageName ?: return@mapNotNull null
            val appMetadata = knownApps[packageName] ?: return@mapNotNull null

            if (packageName !in installedApps) return@mapNotNull null

            val totalTimeMinutes = TimeUnit.MILLISECONDS.toMinutes(usageStats.totalTimeInForeground)

            if (totalTimeMinutes < minSessionMinutes) return@mapNotNull null

            val metadata = UsageStatsMetadata(
                packageName = packageName,
                appName = getAppName(context.packageManager, packageName)
            )

            DurationEvidence(
                source = DataSource.USAGE_STATS,
                confidence = appMetadata.confidenceMultiplier,
                durationMinutes = totalTimeMinutes.toInt(),
                startTimeMillis = usageStats.firstTimeStamp,
                endTimeMillis = usageStats.lastTimeStamp,
                metadata = metadata.toMap()
            )
        }
    }

    private fun getAppName(packageManager: PackageManager, packageName: String): String {
        return try {
            val appInfo = packageManager.getApplicationInfo(packageName, 0)
            packageManager.getApplicationLabel(appInfo).toString()
        } catch (e: Exception) {
            packageName
        }
    }
}
