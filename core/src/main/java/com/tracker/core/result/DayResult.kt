package com.tracker.core.result

/**
 * Results for a single day.
 * Initially supports only language learning; other habits will be added later.
 *
 * @property timestampMillis Timestamp representing this day (start of day in milliseconds)
 * @property languageLearning Language learning result (null if no data available)
 */
data class DayResult(
    val timestampMillis: Long,
    val languageLearning: LanguageLearningResult? = null
)
