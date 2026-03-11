package com.tracker.core.result

/**
 * Status of an access requirement.
 */
enum class AccessStatus {
    /**
     * The requirement is satisfied (permission granted, token provided, etc.).
     */
    GRANTED,

    /**
     * The requirement is not satisfied but could be (permission denied, token not provided).
     */
    MISSING,

    /**
     * The requirement cannot be satisfied (e.g., Health Connect not installed, old Android version).
     */
    NOT_APPLICABLE
}
