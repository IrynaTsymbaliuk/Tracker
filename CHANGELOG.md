# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

## [1.4.0] - 2026-07-18

### Added
- Body-measurement tracking via Health Connect: `Tracker.queryBodyMeasurements(days)` returns a
  `BodyMeasurementsResult` containing complete, independently timestamped records for weight,
  body fat, lean body mass, bone mass, body-water mass, basal metabolic rate, and height. It
  supports any granted subset of the related `health.READ_*` permissions and never invents a
  combined scale reading from records that merely occurred near one another. Health Connect has no
  muscle-percentage record; lean body mass is exposed as the closest available measurement.
- Training-plan tracking via Health Connect: `Tracker.queryTraining(days)` and
  `Tracker.queryTraining(fromMillis, toMillis)` read `PlannedExerciseSessionRecord`s. The latter
  supports upcoming plans. Results are returned as `TrainingResult` / `TrainingSession`; each
  session preserves its complete Health Connect record in `TrainingSession.record`, including
  title, notes, metadata, completion link, zone offsets, and the full block/step goal and target
  hierarchy. Requires `health.READ_PLANNED_EXERCISE`, API 26+, and Health Connect's
  planned-exercise feature. Unsupported or unavailable configurations return `null`.

### Changed
- Removed the unused `confidence` property from every internal `Evidence` type. Collectors no
  longer calculate or carry a score that no provider reads; results continue to identify their
  contributing `sources`. `AppMetadata.confidenceMultiplier` remains catalogue reference metadata
  and does not affect collection or aggregation.

## [1.3.0] - 2026-07-15

### Added
- Sleep tracking via Health Connect: `Tracker.querySleep(days)` reads raw `SleepSessionRecord`s and returns a `SleepResult` with one `SleepSession` per night/nap. Each session exposes when the user fell asleep (`startTime`) and woke (`endTime`), minutes actually asleep (`asleepMinutes`), the stage breakdown (`deepMinutes`/`remMinutes`/`lightMinutes`/`awakeMinutes`, `timeInBedMinutes`, and the raw `stages: List<SleepStage>`), `efficiency` (asleep ÷ time in bed, `null` when the source wrote no stages), and a derived `quality` band. New public types: `SleepStage`, `SleepStageType`, and `SleepQuality` (a non-clinical band derived from sleep efficiency). `SleepResult.totalSleepMinutes`/`totalSleepHours` sum time asleep across sessions. Requires the `health.READ_SLEEP` permission and API 26+; returns `null` when Health Connect is unavailable or the permission is not granted. Sessions are read via `readRecords` (like exercise) with no cross-source deduplication.

### Changed
- Clarified release/install documentation, Kotlin requirements, tracked-app catalog examples, and default query-window wording.
- Simplified CI to use the Gradle build as the single verification entry point.

### Removed
- **Breaking:** removed the `confidence` and `confidenceLevel` properties from `HabitResult` and all result types, along with the `ConfidenceLevel` enum and `Float.toConfidenceLevel()` extension. The scores were uncalibrated heuristics, computed inconsistently across providers, and largely redundant with `sources` (Health Connect / Letterboxd results were a constant tied 1:1 to the source). Use `sources: List<DataSource>` to distinguish authoritative sensor/log data from inferred app usage. Per-app base multipliers remain available as reference data via `AppMetadata.confidenceMultiplier` (`getTracked*Apps()`).

### Fixed
- Removed sample-app hardcoded strings and the personal Letterboxd default from the demo.
- Cleaned Android lint warnings around API-level usage and sample visibility checks.
- Improved `HttpRssFetcher` timeout, retry, and cancellation behavior; HTTP retry classification now uses typed status failures instead of parsing exception messages.

### Tests
- Added focused coverage for RSS retry/cancellation, query windows, known-app manifest parity, and provider aggregation edge cases.

## [1.2.2] - 2026-06-14

### Added
- `MovieSession.year: Int?` — the film's release year from the Letterboxd feed's `letterboxd:filmYear` element. `null` when the feed omits it.
- `MovieSession.posterUrl: String?` — the poster image URL extracted from the diary entry description's `<img>` tag. `null` when the feed omits it.

### Changed
- Movie titles are now read from the feed's dedicated `letterboxd:filmTitle` element instead of being scraped from the entry `<title>` (with its `, <year>` and rating suffix). The old `<title>` parsing remains as a fallback for entries that omit `letterboxd:filmTitle`.
- Updated the demo app and READMEs to surface the new `year` and `posterUrl` fields.

### Fixed
- `MovieSession.watchedDate` is now read from the feed's authoritative `letterboxd:watchedDate` element (parsed at UTC midnight). Previously it was scraped from the description's "Watched on …" sentence, whose regex required a comma after the weekday that current Letterboxd feeds omit — so it silently fell back to the entry's publish date, which could be off by days for films logged after the fact. The description scrape and publish date are retained as fallbacks.

## [1.2.1] - 2026-06-13

### Added
- Expanded the known-app catalog backing `getTrackedLanguageLearningApps()`, `getTrackedReadingApps()`, and `getTrackedSocialMediaApps()`. No API changes — detection now covers more apps out of the box.
  - **Language learning:** Bunpo, Cake, EWA, HelloChinese, Innovative Language (Pod101), Kanji Study, Mango Languages, Speak, Tandem.
  - **Reading:** Audiobookshelf, Barnes & Noble NOOK, Feedly, Hoopla, Instapaper, Medium, Pocket, PressReader, Readwise, Substack, Voice Audiobook Player, Webnovel.
  - **Social media:** BeReal, Facebook Lite, Instagram Lite, KakaoTalk, Lemon8, LINE, Messenger, Nextdoor, Quora, rednote, Signal, Telegram X, TikTok Lite, Viber, WeChat, Weibo.

## [1.2.0] - 2026-06-13

### Added
- **Distance tracking via Health Connect.** New `Tracker.queryDistance(...)` API, backed by `HealthConnectDistanceCollector` and `DistanceProvider`. Returns a `DistanceResult` containing hourly `DistanceSession` buckets (`startTime`, `endTime`, `meters`), aggregated and deduplicated by Health Connect across writing apps per the user's data-source priority. Buckets are anchored to local midnight; hours with no recorded distance are omitted (the list is not guaranteed to be contiguous). Requires the Health Connect distance read permission.
- **Discoverable tracked-app lists.** New `getTrackedLanguageLearningApps()` and `getTrackedReadingApps()` on `Tracker`, each returning the curated `AppMetadata` (package name + base confidence multiplier) for the apps the library can detect. This is static configuration, independent of what is installed or whether permission has been granted.
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

[Unreleased]: https://github.com/IrynaTsymbaliuk/Tracker/compare/v1.4.0...HEAD
[1.4.0]: https://github.com/IrynaTsymbaliuk/Tracker/compare/v1.3.0...v1.4.0
[1.3.0]: https://github.com/IrynaTsymbaliuk/Tracker/compare/v1.2.2...v1.3.0
[1.2.2]: https://github.com/IrynaTsymbaliuk/Tracker/releases/tag/v1.2.2
[1.2.1]: https://github.com/IrynaTsymbaliuk/Tracker/releases/tag/v1.2.1
[1.2.0]: https://github.com/IrynaTsymbaliuk/Tracker/releases/tag/v1.2.0
