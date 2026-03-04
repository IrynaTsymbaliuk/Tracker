package com.tracker.core.permission

/**
 * Status of a permission check.
 */
enum class PermissionStatus {
    /**
     * Permission is granted and available for use.
     */
    GRANTED,

    /**
     * Permission is not granted.
     * Host app should request it via Settings.
     */
    MISSING
}
