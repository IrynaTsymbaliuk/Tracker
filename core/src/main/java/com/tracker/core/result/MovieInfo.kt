package com.tracker.core.result

/**
 * A movie entry logged via Letterboxd.
 *
 * @property title Movie title as it appears in the Letterboxd entry
 * @property publishedDate When the Letterboxd diary entry was published, in milliseconds since epoch
 * @property watchedDate When the movie was watched according to the diary entry, in milliseconds since epoch
 */
data class MovieInfo(
    val title: String,
    val publishedDate: Long,
    val watchedDate: Long
)
