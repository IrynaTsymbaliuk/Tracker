package com.tracker.core.result

/**
 * Represents every possible type of access a collector can require.
 *
 * This sealed class hierarchy allows collectors to declare what they need:
 * - System permissions (PACKAGE_USAGE_STATS, ACTIVITY_RECOGNITION, Health Connect, etc.)
 * - OAuth tokens (Google, Trakt, Goodreads, Duolingo)
 * - API credentials (custom API keys)
 * - No requirements (local data sources)
 */
sealed class AccessRequirement {

    /**
     * Requires a system permission.
     *
     * @property permission The Android permission string (e.g., "android.permission.PACKAGE_USAGE_STATS")
     */
    data class SystemPermission(val permission: String) : AccessRequirement()

    companion object {
        // Known system permissions
        const val PERMISSION_USAGE_STATS = "android.permission.PACKAGE_USAGE_STATS"
    }
}
