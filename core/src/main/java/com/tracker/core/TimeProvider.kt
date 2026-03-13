package com.tracker.core

/**
 * Internal time provider interface for testability.
 *
 * Allows injecting fixed time in tests while using system time in production.
 * This interface is internal and not part of the public API.
 */
internal fun interface TimeProvider {
    /**
     * Returns the current time in milliseconds since epoch.
     */
    fun now(): Long
}
