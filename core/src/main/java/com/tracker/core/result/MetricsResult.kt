package com.tracker.core.result

/**
 * Top-level result returned to the host app.
 *
 * @property languageLearning Language learning result for last 24h (null if not requested or no data)
 * @property reading Reading result for last 24h (null if not requested or no data)
 * @property dataQuality Information about data sources and reliability
 */
data class MetricsResult(
    val languageLearning: LanguageLearningResult? = null,
    val reading: ReadingResult? = null,
    val dataQuality: DataQuality
)
