package com.tracker.core.collector

import android.content.Context
import com.tracker.core.config.AppMetadata
import com.tracker.core.config.HabitConfig
import com.tracker.core.config.KnownApps
import com.tracker.core.permission.PermissionManager

/**
 * Collects reading evidence from UsageStats API.
 *
 * This collector monitors known reading apps and creates
 * Evidence for sessions that exceed the minimum duration threshold.
 *
 * Required permissions: PACKAGE_USAGE_STATS
 * Reliability contribution: MEDIUM
 */
internal class UsageStatsReadingCollector(
    context: Context,
    permissionManager: PermissionManager
) : AbstractUsageStatsCollector(context, permissionManager) {

    override fun getKnownApps(): Map<String, AppMetadata> {
        return KnownApps.reading
    }

    override fun getMinSessionMinutes(): Int {
        return HabitConfig.READING_MIN_SESSION_MINUTES
    }
}
