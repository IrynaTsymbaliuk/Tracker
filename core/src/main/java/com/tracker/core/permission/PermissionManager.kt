package com.tracker.core.permission

import android.app.Activity
import android.app.AppOpsManager
import android.content.Context
import android.content.Intent
import android.os.Process
import android.provider.Settings
import androidx.core.app.ActivityCompat
import com.tracker.core.result.AccessRequirement
import com.tracker.core.result.AccessStatus

/**
 * Manages permission checks for Tracker library.
 *
 * This class ONLY checks permissions - it does not request them.
 * The host app is responsible for requesting permissions when needed.
 *
 * Architecture is designed to support OAuth and API credentials in the future,
 * but currently only handles system permissions.
 *
 * @param context Android context
 */
class PermissionManager(
    private val context: Context
) {

    /**
     * Check the status of an access requirement.
     *
     * Currently only supports system permissions. OAuth and API credentials
     * are part of the extensible architecture but not yet implemented.
     *
     * @param requirement The requirement to check
     * @return Current status of the requirement
     */
    fun check(requirement: AccessRequirement): AccessStatus {
        return when (requirement) {
            is AccessRequirement.SystemPermission -> checkSystemPermission(requirement.permission)
        }
    }

    /**
     * Build an Intent to request a system permission.
     *
     * @param requirement The system permission requirement
     * @return Intent to launch permission request, or null if runtime permission
     */
    fun buildRequestIntent(requirement: AccessRequirement.SystemPermission): Intent? {
        return when (requirement.permission) {
            AccessRequirement.PERMISSION_USAGE_STATS ->
                Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)

            else -> null
        }
    }

    /**
     * Request a system permission (for runtime permissions).
     *
     * @param activity The activity to request from
     * @param requirement The system permission requirement
     */
    fun requestPermission(activity: Activity, requirement: AccessRequirement.SystemPermission) {
        when (requirement.permission) {
            AccessRequirement.PERMISSION_USAGE_STATS -> {
                // Open settings
                buildRequestIntent(requirement)?.let { activity.startActivity(it) }
            }

            else -> {
                // Generic runtime permission request
                ActivityCompat.requestPermissions(
                    activity,
                    arrayOf(requirement.permission),
                    1001
                )
            }
        }
    }

    /**
     * Check the status of a specific permission (legacy method for compatibility).
     *
     * @param permission The permission to check
     * @return Current status of the permission
     */
    fun checkPermission(permission: Permission): PermissionStatus {
        return when (permission) {
            Permission.PACKAGE_USAGE_STATS -> checkUsageStatsPermission()
        }
    }

    // ============================================================
    // Private Helper Methods
    // ============================================================

    private fun checkSystemPermission(permission: String): AccessStatus {
        return when (permission) {
            AccessRequirement.PERMISSION_USAGE_STATS -> {
                val status = checkUsageStatsPermission()
                when (status) {
                    PermissionStatus.GRANTED -> AccessStatus.GRANTED
                    PermissionStatus.MISSING -> AccessStatus.MISSING
                }
            }

            else -> AccessStatus.MISSING
        }
    }

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
