package com.tracker.core.collector

import com.tracker.core.model.Evidence

/**
 * Interface for data collectors.
 * Each collector is responsible for gathering evidence from a specific data source.
 *
 * Collectors should:
 * - Never request permissions themselves (return empty list if permission missing)
 * - Handle errors gracefully and return empty list on failure
 * - Return one Evidence per detected activity/session
 */
interface Collector {
    /**
     * Collect evidence for the specified time range.
     *
     * @param fromMillis Start of time range (inclusive) in milliseconds since epoch
     * @param toMillis End of time range (inclusive) in milliseconds since epoch
     * @return List of Evidence objects found in this time range
     */
    suspend fun collect(fromMillis: Long, toMillis: Long): List<Evidence>
}
