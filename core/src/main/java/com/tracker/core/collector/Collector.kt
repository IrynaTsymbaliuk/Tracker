package com.tracker.core.collector

import com.tracker.core.model.Evidence
import com.tracker.core.result.AccessRequirement
import com.tracker.core.result.ReliabilityLevel

/**
 * Interface for data collectors.
 * Each collector is responsible for gathering evidence from a specific data source.
 *
 * Collectors should:
 * - Declare what permissions they require via sourceRequirements
 * - Never request permissions themselves (return failure result if permission missing)
 * - Handle errors gracefully and return Result.failure on errors
 * - Return Result.success with empty list when no evidence found
 * - Return one Evidence per detected activity/session
 */
interface Collector {
    /**
     * The set of access requirements this collector needs to operate.
     * Can include system permissions, OAuth tokens, API credentials, or be empty (NoRequirement).
     */
    val sourceRequirements: Set<AccessRequirement>

    /**
     * Human-readable name of this data source (e.g., "Usage Stats", "Health Connect", "Goodreads API").
     */
    val sourceName: String

    /**
     * How much this collector contributes to overall reliability when available.
     */
    val reliabilityContribution: ReliabilityLevel

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
