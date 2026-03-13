package com.tracker.core.result

/**
 * Result returned by queryLanguageLearning().
 *
 * @property result Language learning activity result (null if no permission or no data)
 * @property dataQuality Information about data sources and reliability
 */
data class LanguageLearningMetricResult(
    val result: LanguageLearningResult?,
    override val dataQuality: DataQuality
) : MetricQueryResult
