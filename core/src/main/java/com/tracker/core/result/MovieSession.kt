package com.tracker.core.result

/**
 * A single movie-watching session logged via Letterboxd.
 *
 * Letterboxd diary entries record *when* a movie was watched but not how long it ran, so a
 * session is represented by two timestamps rather than a start/end pair: [watchedDate] (the
 * diary day) and [publishedDate] (when the entry was logged).
 *
 * Sessions are exposed via [MovieWatchingResult.sessions].
 *
 * @property title Movie title as it appears in the Letterboxd entry.
 * @property publishedDate When the Letterboxd diary entry was published, in milliseconds since epoch.
 * @property watchedDate When the movie was watched according to the diary entry, in milliseconds
 * since epoch (parsed at UTC midnight; falls back to [publishedDate] when the feed omits it).
 */
data class MovieSession(
    val title: String,
    val publishedDate: Long,
    val watchedDate: Long
)
