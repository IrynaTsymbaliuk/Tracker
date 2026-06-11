package com.tracker.core.collector

/**
 * Type-safe metadata for Letterboxd movie evidence.
 *
 * @property title Movie title
 * @property publishedDate When the review was published (milliseconds)
 * @property watchedDate When the movie was watched (milliseconds)
 * @property tmdbId The Movie Database (TMDB) movie id, or `null` when the feed omits it
 */
data class LetterboxdMetadata(
    val title: String,
    val publishedDate: Long,
    val watchedDate: Long,
    val tmdbId: Int? = null
) {
    /** Converts to map for Evidence compatibility. */
    fun toMap(): Map<String, Any> = buildMap {
        put(KEY_TITLE, title)
        put(KEY_PUBLISHED_DATE, publishedDate)
        put(KEY_WATCHED_DATE, watchedDate)
        tmdbId?.let { put(KEY_TMDB_ID, it) }
    }

    companion object {
        const val KEY_TITLE = "title"
        const val KEY_PUBLISHED_DATE = "publishedDate"
        const val KEY_WATCHED_DATE = "watchedDate"
        const val KEY_TMDB_ID = "tmdbId"

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
                    watchedDate = map[KEY_WATCHED_DATE] as? Long ?: return null,
                    tmdbId = map[KEY_TMDB_ID] as? Int
                )
            } catch (e: Exception) {
                null
            }
        }
    }
}
