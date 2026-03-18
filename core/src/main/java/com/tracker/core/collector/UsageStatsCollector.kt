package com.tracker.core.collector

import android.app.usage.UsageStats
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.pm.PackageManager
import com.tracker.core.config.AppMetadata
import com.tracker.core.config.KnownApps
import com.tracker.core.model.DurationEvidence
import com.tracker.core.permission.Permission
import com.tracker.core.permission.PermissionManager
import com.tracker.core.permission.PermissionStatus
import com.tracker.core.types.DataSource
import java.util.concurrent.TimeUnit

class UsageStatsCollector(private val context: Context, private val permissionManager: PermissionManager) {

    fun collect(
        fromMillis: Long,
        toMillis: Long,
        knownApps: Map<String, AppMetadata>,
        minSessionMinutes: Int
    ): List<DurationEvidence>? {

        checkPermissions()

        val installedApps = getInstalledApps()

        val usageStatsList = getUsageStats(fromMillis, toMillis)

        val evidenceList =
            getEvidenceList(
                knownApps,
                minSessionMinutes,
                usageStatsList,
                installedApps
            )?.ifEmpty { return null } ?: return null

        return evidenceList

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
        knownApps: Map<String, AppMetadata>,
        minSessionMinutes: Int,
        usageStatsList: List<UsageStats?>?,
        installedApps: Set<String>
    ): List<DurationEvidence>? {
        return usageStatsList?.mapNotNull { usageStats ->
            usageStats?.let {
                val packageName = usageStats.packageName ?: return@mapNotNull null
                val appMetadata = knownApps[packageName] ?: return@mapNotNull null

                if (packageName !in installedApps) null

                val totalTimeMillis = usageStats.totalTimeInForeground
                val totalTimeMinutes = TimeUnit.MILLISECONDS.toMinutes(totalTimeMillis).toInt()

                if (totalTimeMinutes < 0 || totalTimeMinutes < minSessionMinutes) null

                DurationEvidence(
                    source = DataSource.USAGE_STATS,
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

}

/**
 * Base exception for collector failures.
 */
sealed class CollectorException(message: String) : Exception(message)

/**
 * Thrown when required permission is not granted.
 */
class PermissionDeniedException(
    val permission: String
) : CollectorException("Permission denied: $permission")

/**
 * Thrown when a required system service is unavailable.
 */
class SystemServiceUnavailableException(
    val serviceName: String
) : CollectorException("System service unavailable: $serviceName")

/**
 * Thrown when no monitorable apps are installed.
 */
class NoMonitorableAppsException : CollectorException(
    "No known language learning apps are installed"
)

/**
 * Thrown when PackageManager operations fail.
 */
class PackageManagerException(
    cause: Throwable
) : CollectorException("Failed to query installed applications: ${cause.message}")