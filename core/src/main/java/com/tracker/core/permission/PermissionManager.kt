package com.tracker.core.permission

import android.app.AppOpsManager
import android.content.Context
import android.os.Process

/**
 * Manages permission checks for Tracker library.
 *
 * This class ONLY checks permissions - it does not request them.
 * The host app is responsible for requesting permissions when needed.
 *
 * Collectors return empty evidence if permissions are missing.
 * The host app can check DataQuality in results to see what's missing.
 */
class PermissionManager(private val context: Context) {

    /**
     * Check the status of a specific permission.
     *
     * @param permission The permission to check
     * @return Current status of the permission
     */
    fun checkPermission(permission: Permission): PermissionStatus {
        return when (permission) {
            Permission.PACKAGE_USAGE_STATS -> checkUsageStatsPermission()
        }
    }

    /**
     * Check if PACKAGE_USAGE_STATS permission is granted.
     *
     * @return GRANTED if permission is available, MISSING otherwise
     */
    private fun checkUsageStatsPermission(): PermissionStatus {
        val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as? AppOpsManager
            ?: return PermissionStatus.MISSING

        val mode = appOps.checkOpNoThrow(
            AppOpsManager.OPSTR_GET_USAGE_STATS,
            Process.myUid(),
            context.packageName
        )

        return if (mode == AppOpsManager.MODE_ALLOWED) {
            PermissionStatus.GRANTED
        } else {
            PermissionStatus.MISSING
        }
    }
}
