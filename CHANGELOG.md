# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [1.2.1] - 2026-06-13

### Added
- Expanded the known-app catalog backing `getTrackedLanguageLearningApps()`, `getTrackedReadingApps()`, and `getTrackedSocialMediaApps()`. No API changes — detection now covers more apps out of the box.
  - **Language learning:** Bunpo, Cake, EWA, HelloChinese, Innovative Language (Pod101), Kanji Study, Mango Languages, Speak, Tandem.
  - **Reading:** Audiobookshelf, Barnes & Noble NOOK, Feedly, Hoopla, Instapaper, Medium, Pocket, PressReader, Readwise, Substack, Voice Audiobook Player, Webnovel.
  - **Social media:** BeReal, Facebook Lite, Instagram Lite, KakaoTalk, Lemon8, LINE, Messenger, Nextdoor, Quora, rednote, Signal, Telegram X, TikTok Lite, Viber, WeChat, Weibo.

## [1.2.0] - 2026-06-13

### Added
- **Distance tracking via Health Connect.** New `Tracker.queryDistance(...)` API, backed by `HealthConnectDistanceCollector` and `DistanceProvider`. Returns a `DistanceResult` containing hourly `DistanceSession` buckets (`startTime`, `endTime`, `meters`), aggregated and deduplicated by Health Connect across writing apps per the user's data-source priority. Buckets are anchored to local midnight; hours with no recorded distance are omitted (the list is not guaranteed to be contiguous). Requires the Health Connect distance read permission.
- **Discoverable tracked-app lists.** New `getTrackedLanguageLearningApps()` and `getTrackedReadingApps()` on `Tracker`, each returning the curated `AppMetadata` (package name + base confidence multiplier) for the apps the library can detect. This is static configuration — independent of what is installed or whether permission has been granted — ordered alphabetically by app name.
- `MovieSession.tmdbId: Int?` — The Movie Database (TMDB) movie id from the Letterboxd feed's `tmdb:movieId` element, for looking up further movie details. `null` for TV entries or films not yet linked to TMDB.
- `MovieSession.rating: Float?` — the user's star rating (0.5–5.0 scale, half-star increments) from `letterboxd:memberRating`. `null` when the entry has no rating.
- `MovieSession.review: String?` — the user's written review as plain text (feed HTML is stripped, and the poster image and "Watched on …" boilerplate are removed). `null` when there is no review.
- `MovieSession.isRewatch: Boolean` — whether the entry is marked as a rewatch (`letterboxd:rewatch`); defaults to `false`.
- `MovieSession.isLiked: Boolean` — whether the user liked (hearted) the film (`letterboxd:memberLike`); defaults to `false`.

### Changed
- Updated the demo app and READMEs to cover distance tracking, the tracked-app lists, and the new `MovieSession` fields.

## [1.1.0] - 2026-05-22

### Changed
- **BREAKING**: `StepCountingResult.steps: Long` replaced by `sessions: List<StepSession>` plus a computed `totalSteps: Long` convenience. Each `StepSession` is a 1-hour bucket with `startTime`, `endTime`, and `steps`, letting callers separate today's steps from yesterday's when querying multi-day windows.
- `HealthConnectStepCollector` now uses `aggregateGroupByDuration` with a 1-hour slice instead of `aggregate`. Health Connect still deduplicates across writing apps per the user's data-source priority configuration. Hours with no recorded steps are omitted from the response.
- **BREAKING**: `MovieWatchingResult.movies: List<MovieInfo>` replaced by `sessions: List<MovieSession>`, and `count: Int` is now a computed property (`sessions.size`) instead of a constructor parameter. `MovieInfo` is renamed to `MovieSession` with the same fields (`title`, `publishedDate`, `watchedDate`). Sessions are sorted by `watchedDate` ascending.

### Migration

**Step counting result shape:**
```kotlin
// Before
val total = tracker.queryStepCounting()?.steps

// After — same total, plus hourly breakdown
val result = tracker.queryStepCounting(days = 2)
val total = result?.totalSteps
val todaysSteps = result?.sessions
    ?.filter { it.startTime >= startOfToday }
    ?.sumOf { it.steps }
```

**Movie watching result shape:**
```kotlin
// Before
val movies = tracker.queryMovieWatching()
movies?.movies?.forEach { (title, _, watchedDate) -> show(title, watchedDate) }

// After — MovieInfo -> MovieSession, movies -> sessions; count unchanged (now computed)
val movies = tracker.queryMovieWatching()
movies?.sessions?.forEach { show(it.title, it.watchedDate) }
```

[1.2.1]: https://github.com/IrynaTsymbaliuk/Tracker/releases/tag/v1.2.1
[1.2.0]: https://github.com/IrynaTsymbaliuk/Tracker/releases/tag/v1.2.0
