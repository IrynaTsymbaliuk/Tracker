package com.tracker.core.model

import com.tracker.core.types.DataSource

/**
 * Evidence represents a single piece of data collected from a source.
 * Collectors return Evidence objects, which are then aggregated to produce final results.
 *
 * @property source Where the data came from
 * @property timestampMillis Timestamp in milliseconds since epoch (for date grouping)
 * @property confidence How confident we are in this evidence (0.0 to 1.0)
 * @property durationMinutes Duration of the activity in minutes (nullable)
 * @property startTimeMillis When the activity started in milliseconds since epoch (nullable)
 * @property endTimeMillis When the activity ended in milliseconds since epoch (nullable)
 * @property metadata Source-specific additional data (app name, package, language, etc.)
 */
data class Evidence(
    val source: DataSource,
    val timestampMillis: Long,
    val confidence: Float,
    val durationMinutes: Int? = null,
    val startTimeMillis: Long? = null,
    val endTimeMillis: Long? = null,
    val metadata: Map<String, Any> = emptyMap()
) {
    init {
        require(confidence in 0.0f..1.0f) { "Confidence must be between 0.0 and 1.0" }
    }
}
