package com.tracker.core.result

/**
 * Information about a single data source's access status.
 *
 * @property sourceName Human-readable name of the data source (e.g., "Usage Stats", "Health Connect")
 * @property requirement What this source requires to operate
 * @property status Current status of the requirement (GRANTED, MISSING, NOT_APPLICABLE)
 * @property reliabilityContribution How much this source contributes to overall reliability
 * @property description Human-readable description of what this source provides
 */
data class SourceAccessInfo(
    val sourceName: String,
    val requirement: AccessRequirement,
    val status: AccessStatus,
    val reliabilityContribution: ReliabilityLevel,
    val description: String
)
