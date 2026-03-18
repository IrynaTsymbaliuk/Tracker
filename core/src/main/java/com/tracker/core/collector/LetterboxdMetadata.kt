package com.tracker.core.collector

/**
 * Type-safe metadata for Letterboxd movie evidence.
 *
 * @property title Movie title
 * @property publishedDate When the review was published (milliseconds)
 * @property watchedDate When the movie was watched (milliseconds)
 */
data class LetterboxdMetadata(
    val title: String,
    val publishedDate: Long,
    val watchedDate: Long
) {
    /** Converts to map for Evidence compatibility. */
    fun toMap(): Map<String, Any> = mapOf(
        KEY_TITLE to title,
        KEY_PUBLISHED_DATE to publishedDate,
        KEY_WATCHED_DATE to watchedDate
    )

    companion object {
        const val KEY_TITLE = "title"
        const val KEY_PUBLISHED_DATE = "publishedDate"
        const val KEY_WATCHED_DATE = "watchedDate"

        /**
         * Extracts LetterboxdMetadata from a metadata map.
         *
         * @return LetterboxdMetadata if all fields are valid, null otherwise
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
