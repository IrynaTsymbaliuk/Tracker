package com.tracker.core.model

import com.tracker.core.types.DataSource

/**
 * Evidence represents a single piece of data collected from a source.
 * Collectors return Evidence objects, which are then aggregated to produce final results.
 *
 * @property source Where the data came from
 * @property confidence How confident we are in this evidence (0.0 to 1.0)
 * @property metadata Source-specific additional data (app name, package, language, etc.)
 */
sealed class Evidence {
    abstract val source: DataSource
    abstract val confidence: Float
    abstract val metadata: Map<String, Any>
}

data class DurationEvidence(
    override val source: DataSource,
    override val confidence: Float,
    override val metadata: Map<String, Any>,
    val durationMinutes: Int,
    val startTimeMillis: Long,
    val endTimeMillis: Long
) : Evidence()

data class CounterEvidence(
    override val source: DataSource,
    override val confidence: Float,
    override val metadata: Map<String, Any>,
    val counter: Int
) : Evidence()

data class StepEvidence(
    override val source: DataSource,
    override val confidence: Float,
    override val metadata: Map<String, Any>,
    val steps: Long
) : Evidence()
