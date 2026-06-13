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
 * @property tmdbId The Movie Database (TMDB) movie id from the Letterboxd feed's `tmdb:movieId`
 * element, used to look up further movie details in TMDB. `null` when the feed omits it (e.g.
 * TV entries or films not yet linked to TMDB).
 * @property rating The user's star rating from the feed's `letterboxd:memberRating` element, on a
 * 0.5–5.0 scale (half-star increments). `null` when the entry was logged without a rating.
 * @property review The user's written review as plain text (the feed's HTML is stripped, and the
 * poster image and "Watched on …" boilerplate are removed). `null` when the entry has no review.
 * @property isRewatch Whether the entry is marked as a rewatch, from the feed's
 * `letterboxd:rewatch` element. Defaults to `false` when the feed omits it.
 * @property isLiked Whether the user liked (hearted) the film, from the feed's
 * `letterboxd:memberLike` element. Defaults to `false` when the feed omits it.
 */
data class MovieSession(
    val title: String,
    val publishedDate: Long,
    val watchedDate: Long,
    val tmdbId: Int? = null,
    val rating: Float? = null,
    val review: String? = null,
    val isRewatch: Boolean = false,
    val isLiked: Boolean = false
)
