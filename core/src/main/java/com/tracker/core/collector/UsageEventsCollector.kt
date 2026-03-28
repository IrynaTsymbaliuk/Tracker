package com.tracker.core.collector

import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import com.tracker.core.config.AppMetadata
import com.tracker.core.model.DurationEvidence
import com.tracker.core.permission.Permission
import com.tracker.core.permission.PermissionManager
import com.tracker.core.permission.PermissionStatus
import com.tracker.core.types.DataSource
import java.util.concurrent.TimeUnit

/**
 * Collects app foreground sessions from [android.app.usage.UsageStatsManager.queryEvents].
 *
 * Each [DurationEvidence] returned represents one meaningful foreground session. On API 29+,
 * [UsageEvents.Event.ACTIVITY_RESUMED] / [UsageEvents.Event.ACTIVITY_PAUSED] are used because
 * [UsageEvents.Event.MOVE_TO_FOREGROUND] / [UsageEvents.Event.MOVE_TO_BACKGROUND] are not
 * reliably emitted at that API level. Activity transitions within the same app are merged into
 * a single session as long as the gap between them is within [SESSION_GAP_MILLIS].
 *
 * Edge cases:
 * - App was already open at [fromMillis] (no foreground event seen): [fromMillis] is used as the
 *   inferred session start.
 * - App is still open at [toMillis] (no background event seen): [toMillis] is used as the session end.
 *
 * Requires the user to grant `PACKAGE_USAGE_STATS` via Settings > Apps > Special app access > Usage access.
 */
class UsageEventsCollector(
    private val context: Context,
    private val permissionManager: PermissionManager
) {

    companion object {
        // Maximum gap between two activity transitions of the same app that is still considered
        // a single continuous session (e.g. navigating between screens within the app).
        val SESSION_GAP_MILLIS = TimeUnit.SECONDS.toMillis(30)

        val FOREGROUND_EVENT = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            UsageEvents.Event.ACTIVITY_RESUMED
        } else {
            UsageEvents.Event.MOVE_TO_FOREGROUND
        }

        val BACKGROUND_EVENT = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            UsageEvents.Event.ACTIVITY_PAUSED
        } else {
            UsageEvents.Event.MOVE_TO_BACKGROUND
        }
    }

    /**
     * Returns one [DurationEvidence] per foreground session for known, installed apps within the
     * given time range.
     *
     * @throws PermissionDeniedException if `PACKAGE_USAGE_STATS` is not granted.
     * @throws SystemServiceUnavailableException if [android.app.usage.UsageStatsManager] is unavailable.
     * @throws PackageManagerException if querying installed apps fails.
     * @throws NoMonitorableAppsException if none of the [knownApps] are installed.
     */
    fun collect(
        fromMillis: Long,
        toMillis: Long,
        knownApps: Map<String, AppMetadata>
    ): List<DurationEvidence> {

        checkPermissions()

        val installedApps = getInstalledApps(knownApps)

        return buildSessions(fromMillis, toMillis, knownApps, installedApps)
    }

    private fun checkPermissions() {
        if (permissionManager.checkPermission(Permission.PACKAGE_USAGE_STATS) != PermissionStatus.GRANTED) {
            throw PermissionDeniedException("PACKAGE_USAGE_STATS")
        }
    }

    private fun getInstalledApps(knownApps: Map<String, AppMetadata>): Set<String> {
        val installedKnownApps = try {
            knownApps.keys.filter { packageName ->
                try {
                    context.packageManager.getPackageInfo(packageName, 0)
                    true
                } catch (e: PackageManager.NameNotFoundException) {
                    false
                }
            }.toSet()
        } catch (e: Exception) {
            throw PackageManagerException(e)
        }

        if (installedKnownApps.isEmpty()) {
            throw NoMonitorableAppsException()
        }

        return installedKnownApps
    }

    private fun buildSessions(
        fromMillis: Long,
        toMillis: Long,
        knownApps: Map<String, AppMetadata>,
        installedApps: Set<String>
    ): List<DurationEvidence> {
        val usageStatsManager =
            context.getSystemService(Context.USAGE_STATS_SERVICE) as? UsageStatsManager
                ?: throw SystemServiceUnavailableException("UsageStatsManager")

        val usageEvents = usageStatsManager.queryEvents(fromMillis, toMillis)
            ?: return emptyList()

        val pendingSessions = mutableMapOf<String, PendingSession>()
        val sessions = mutableListOf<DurationEvidence>()
        val event = UsageEvents.Event()

        while (usageEvents.hasNextEvent()) {
            usageEvents.getNextEvent(event)

            val packageName = event.packageName ?: continue
            val appMetadata = knownApps[packageName] ?: continue
            if (packageName !in installedApps) continue

            when (event.eventType) {
                FOREGROUND_EVENT -> {
                    val pending = pendingSessions[packageName]
                    val lastPaused = pending?.lastPausedTime

                    if (pending != null && lastPaused != null) {
                        if (event.timeStamp - lastPaused <= SESSION_GAP_MILLIS) {
                            // Activity transition within the same session — resume it
                            pendingSessions[packageName] = pending.copy(lastPausedTime = null)
                        } else {
                            // Gap too large — close the previous session and start a new one
                            addSession(sessions, packageName, appMetadata, pending.startTime, lastPaused)
                            pendingSessions[packageName] = PendingSession(event.timeStamp)
                        }
                    } else {
                        pendingSessions[packageName] = PendingSession(event.timeStamp)
                    }
                }
                BACKGROUND_EVENT -> {
                    val pending = pendingSessions[packageName]
                    if (pending != null) {
                        pendingSessions[packageName] = pending.copy(lastPausedTime = event.timeStamp)
                    } else {
                        // App was open before fromMillis — infer session start
                        pendingSessions[packageName] = PendingSession(
                            startTime = fromMillis,
                            lastPausedTime = event.timeStamp
                        )
                    }
                }
            }
        }

        // Close any sessions still open at the end of the query window
        for ((packageName, pending) in pendingSessions) {
            val appMetadata = knownApps[packageName] ?: continue
            val endTime = pending.lastPausedTime ?: toMillis
            addSession(sessions, packageName, appMetadata, pending.startTime, endTime)
        }

        return sessions
    }

    private fun addSession(
        sessions: MutableList<DurationEvidence>,
        packageName: String,
        appMetadata: AppMetadata,
        startTimeMillis: Long,
        endTimeMillis: Long
    ) {
        val durationMinutes = TimeUnit.MILLISECONDS.toMinutes(endTimeMillis - startTimeMillis).toInt()

        val metadata = UsageStatsMetadata(
            packageName = packageName,
            appName = getAppName(context.packageManager, packageName)
        )

        sessions.add(
            DurationEvidence(
                source = DataSource.USAGE_STATS,
                confidence = appMetadata.confidenceMultiplier,
                durationMinutes = durationMinutes,
                startTimeMillis = startTimeMillis,
                endTimeMillis = endTimeMillis,
                metadata = metadata.toMap()
            )
        )
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

private data class PendingSession(
    val startTime: Long,
    val lastPausedTime: Long? = null
)