package com.tracker.core.collector

import com.tracker.core.model.Evidence

/**
 * Interface for data collectors.
 * Each collector is responsible for gathering evidence from a specific data source.
 *
 * Collectors should:
 * - Never request permissions themselves (return failure result if permission missing)
 * - Handle errors gracefully and return Result.failure on errors
 * - Return Result.success with empty list when no evidence found
 * - Return one Evidence per detected activity/session
 */
interface Collector {
    /**
     * Collect evidence for the specified time range.
     *
     * @param fromMillis Start of time range (inclusive) in milliseconds since epoch
     * @param toMillis End of time range (inclusive) in milliseconds since epoch
     * @return Result containing List of Evidence objects found in this time range,
     *         or failure with exception describing the error
     */
    suspend fun collect(fromMillis: Long, toMillis: Long): Result<List<Evidence>>
}
