# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

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

[1.1.0]: https://github.com/IrynaTsymbaliuk/Tracker/releases/tag/v1.1.0
