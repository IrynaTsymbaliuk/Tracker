package com.tracker.core.result

/**
 * Results for a single day.
 *
 * @property timestampMillis Timestamp representing this day (start of day in milliseconds)
 * @property languageLearning Language learning result (null if no data available)
 * @property reading Reading result (null if no data available)
 */
data class DayResult(
    val timestampMillis: Long,
    val languageLearning: LanguageLearningResult? = null,
    val reading: ReadingResult? = null
)
