package com.tracker.core.result

/**
 * Top-level result returned to the host app.
 *
 * @property days List of daily results
 * @property summary Aggregated statistics across all days
 * @property dataQuality Information about data sources and reliability
 */
data class MetricsResult(
    val days: List<DayResult>,
    val summary: Summary,
    val dataQuality: DataQuality
)

/**
 * Summary statistics across the queried time range.
 *
 * @property totalDays Number of days in the query range
 * @property languageLearningDays Number of days with detected language learning (nullable)
 * @property averageLanguageLearningMinutes Average daily minutes spent on language learning (nullable)
 * @property readingDays Number of days with detected reading (nullable)
 * @property averageReadingMinutes Average daily minutes spent reading (nullable)
 */
data class Summary(
    val totalDays: Int,
    val languageLearningDays: Int? = null,
    val averageLanguageLearningMinutes: Float? = null,
    val readingDays: Int? = null,
    val averageReadingMinutes: Float? = null
)
