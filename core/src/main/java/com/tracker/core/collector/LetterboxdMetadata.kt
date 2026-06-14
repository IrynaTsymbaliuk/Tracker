package com.tracker.core.collector

/**
 * Type-safe metadata for Letterboxd movie evidence.
 *
 * @property title Movie title
 * @property year Release year, or `null` when the feed omits it
 * @property publishedDate When the review was published (milliseconds)
 * @property watchedDate When the movie was watched (milliseconds)
 * @property tmdbId The Movie Database (TMDB) movie id, or `null` when the feed omits it
 * @property rating The user's star rating (0.5–5.0), or `null` when the entry is unrated
 * @property review The user's plain-text review, or `null` when the entry has no review
 * @property posterUrl The poster image URL, or `null` when the feed omits it
 * @property isRewatch Whether the entry is marked as a rewatch
 * @property isLiked Whether the user liked (hearted) the film
 */
data class LetterboxdMetadata(
    val title: String,
    val year: Int? = null,
    val publishedDate: Long,
    val watchedDate: Long,
    val tmdbId: Int? = null,
    val rating: Float? = null,
    val review: String? = null,
    val posterUrl: String? = null,
    val isRewatch: Boolean = false,
    val isLiked: Boolean = false
) {
    /** Converts to map for Evidence compatibility. */
    fun toMap(): Map<String, Any> = buildMap {
        put(KEY_TITLE, title)
        year?.let { put(KEY_YEAR, it) }
        put(KEY_PUBLISHED_DATE, publishedDate)
        put(KEY_WATCHED_DATE, watchedDate)
        tmdbId?.let { put(KEY_TMDB_ID, it) }
        rating?.let { put(KEY_RATING, it) }
        review?.let { put(KEY_REVIEW, it) }
        posterUrl?.let { put(KEY_POSTER_URL, it) }
        put(KEY_IS_REWATCH, isRewatch)
        put(KEY_IS_LIKED, isLiked)
    }

    companion object {
        const val KEY_TITLE = "title"
        const val KEY_YEAR = "year"
        const val KEY_PUBLISHED_DATE = "publishedDate"
        const val KEY_WATCHED_DATE = "watchedDate"
        const val KEY_TMDB_ID = "tmdbId"
        const val KEY_RATING = "rating"
        const val KEY_REVIEW = "review"
        const val KEY_POSTER_URL = "posterUrl"
        const val KEY_IS_REWATCH = "isRewatch"
        const val KEY_IS_LIKED = "isLiked"

        /**
         * Extracts LetterboxdMetadata from a metadata map.
         *
         * @return LetterboxdMetadata if all fields are valid, null otherwise
         */
        fun fromMap(map: Map<String, Any>): LetterboxdMetadata? {
            return try {
                LetterboxdMetadata(
                    title = map[KEY_TITLE] as? String ?: return null,
                    year = map[KEY_YEAR] as? Int,
                    publishedDate = map[KEY_PUBLISHED_DATE] as? Long ?: return null,
                    watchedDate = map[KEY_WATCHED_DATE] as? Long ?: return null,
                    tmdbId = map[KEY_TMDB_ID] as? Int,
                    rating = map[KEY_RATING] as? Float,
                    review = map[KEY_REVIEW] as? String,
                    posterUrl = map[KEY_POSTER_URL] as? String,
                    isRewatch = map[KEY_IS_REWATCH] as? Boolean ?: false,
                    isLiked = map[KEY_IS_LIKED] as? Boolean ?: false
                )
            } catch (e: Exception) {
                null
            }
        }
    }
}
