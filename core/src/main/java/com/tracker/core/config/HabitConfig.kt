package com.tracker.core.config

/**
 * Configuration constants for habit detection.
 *
 * Each habit has specific characteristics that define what counts as a valid session.
 */
object HabitConfig {
    /**
     * Minimum session duration for language learning to count as evidence.
     * Sessions shorter than this are filtered out.
     */
    const val LANGUAGE_LEARNING_MIN_SESSION_MINUTES = 5

    /**
     * Minimum session duration for reading to count as evidence.
     * Sessions shorter than this are filtered out.
     */
    const val READING_MIN_SESSION_MINUTES = 5
}
