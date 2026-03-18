package com.tracker.core.collector

/**
 * Type-safe metadata for Letterboxd movie evidence.
 * Provides compile-time safety and IDE autocomplete for accessing movie data.
 *
 * @property title The movie title
 * @property publishedDate When the review/diary entry was published (milliseconds since epoch)
 * @property watchedDate When the movie was actually watched (milliseconds since epoch)
 */
data class LetterboxdMetadata(
    val title: String,
    val publishedDate: Long,
    val watchedDate: Long
) {
    /**
     * Converts the typed metadata to a generic map for Evidence compatibility.
     * This is used internally when creating CounterEvidence objects.
     */
    fun toMap(): Map<String, Any> = mapOf(
        KEY_TITLE to title,
        KEY_PUBLISHED_DATE to publishedDate,
        KEY_WATCHED_DATE to watchedDate
    )

    companion object {
        // Public constants for map keys (for backwards compatibility)
        const val KEY_TITLE = "title"
        const val KEY_PUBLISHED_DATE = "publishedDate"
        const val KEY_WATCHED_DATE = "watchedDate"

        /**
         * Safely extracts LetterboxdMetadata from a generic metadata map.
         * Returns null if the map doesn't contain valid Letterboxd metadata.
         *
         * @param map The metadata map from CounterEvidence
         * @return LetterboxdMetadata if all required fields are present and valid, null otherwise
         */
        fun fromMap(map: Map<String, Any>): LetterboxdMetadata? {
            return try {
                LetterboxdMetadata(
                    title = map[KEY_TITLE] as? String ?: return null,
                    publishedDate = map[KEY_PUBLISHED_DATE] as? Long ?: return null,
                    watchedDate = map[KEY_WATCHED_DATE] as? Long ?: return null
                )
            } catch (e: Exception) {
                null
            }
        }
    }
}
