package com.tracker.core.result

/**
 * Result returned by queryReading().
 *
 * @property result Reading activity result (null if no permission or no data)
 * @property dataQuality Information about data sources and reliability
 */
data class ReadingMetricResult(
    val result: ReadingResult?,
    override val dataQuality: DataQuality
) : MetricQueryResult
