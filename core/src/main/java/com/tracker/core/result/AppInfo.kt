package com.tracker.core.result

/**
 * Information about an app that contributed to a habit detection result.
 *
 * @property packageName The app's package identifier
 * @property appName The human-readable app name
 */
data class AppInfo(
    val packageName: String,
    val appName: String
)
