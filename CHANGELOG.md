# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [5.0.0] - 2026-04-14

### Added
- **Step counting** via Health Connect (`queryStepCounting()`). Returns total steps for the queried window, deduplicated across all contributing data sources (Google Fit, phone step counter, etc.) using Health Connect's aggregation API. Requires `android.permission.health.READ_STEPS` and API 26+.
- **`sessions: List<UsageSession>`** on `LanguageLearningResult`, `ReadingResult`, and `SocialMediaResult`. Each entry represents one foreground session with `startTime`, `endTime`, `durationMinutes`, `packageName`, and `appName`. Sessions are sorted by `startTime` ascending.
- **`days` parameter** on all query methods. `days = 1` (default) covers today from midnight through now. `days = 2` adds yesterday, and so on. Previously all queries were hardcoded to the last 24 hours regardless of midnight boundaries.
- **`StepCountingResult`** — new result type with `steps: Long`, `confidence`, `confidenceLevel`, and `timeRange`.
- **`DataSource.HEALTH_CONNECT`** and **`DataSource.SENSOR`** — new data source enum values.
- **`StepEvidence`** — new internal evidence type for step counting data.

### Changed
- **BREAKING**: Removed `occurred: Boolean` from `HabitResult` and all result subclasses (`LanguageLearningResult`, `ReadingResult`, `SocialMediaResult`, `MovieWatchingResult`). Activity presence is now indicated entirely by a non-null return value — if the method returns a result, the habit occurred.
- **BREAKING**: Replaced `sessionCount: Int` and `apps: List<AppInfo>` with `sessions: List<UsageSession>` on `LanguageLearningResult`, `ReadingResult`, and `SocialMediaResult`. Session count and app list are derived from `sessions`: `sessions.size`, `sessions.map { it.appName }.distinct()`.
- **BREAKING**: Deleted `AppInfo` data class. Use `UsageSession.packageName` and `UsageSession.appName` instead.
- **BREAKING**: Removed `enableReading()`, `enableLanguageLearning()`, `enableSocialMedia()`, and `enableMovieWatching()` from `Tracker.Builder`. All features are now always available — no opt-in flags required. Providers are created lazily and only on first use.
- **BREAKING**: `queryMovieWatching()` now throws `IllegalStateException` instead of returning `null` when a Letterboxd username has not been set.
- **BREAKING**: Removed `Float.toOccurred(threshold: Float)` extension from `ConfidenceExtensions`.
- Time window for all queries changed from "last 24 hours" to **midnight-anchored days**. `days = 1` starts at midnight of the current day in the device's local timezone, not 24 hours ago.
- `Tracker` now always uses `applicationContext` internally, preventing Activity memory leaks when an Activity context is passed to the builder.
- `UsageEventsCollector` is created lazily on the first usage-stats query and shared across reading, language learning, and social media providers.

### Migration

**Removed `occurred` field:**
```kotlin
// Before
if (result.occurred) { show(result.durationMinutes) }

// After — null means not occurred, non-null means occurred
result?.let { show(it.durationMinutes) }
```

**Replaced `sessionCount` / `apps` with `sessions`:**
```kotlin
// Before
result.sessionCount
result.apps.map { it.appName }

// After
result.sessions.size
result.sessions.map { it.appName }.distinct()
```

**Removed Builder `enable*()` methods:**
```kotlin
// Before
val tracker = Tracker.Builder(context)
    .enableReading()
    .enableLanguageLearning()
    .enableSocialMedia()
    .enableMovieWatching()
    .setLetterboxdUsername("username")
    .build()

// After
val tracker = Tracker.Builder(context)
    .setLetterboxdUsername("username")  // still optional
    .build()
```

**`queryMovieWatching()` throws instead of returning null when username is missing:**
```kotlin
// Before — null meant "username not set"
val movies = tracker.queryMovieWatching()

// After — throws if username not configured
try {
    val movies = tracker.queryMovieWatching()
} catch (e: IllegalStateException) {
    // username not set — call tracker.setLetterboxdUsername("username") first
}
```

**Time window — midnight-anchored:**
```kotlin
// Before — last 24 hours from now
tracker.queryReading()  // e.g. 14:00 yesterday → 14:00 today

// After — today from midnight (default days = 1)
tracker.queryReading()          // midnight today → now
tracker.queryReading(days = 2)  // midnight yesterday → now
```

---

## [4.0.0] - 2026-03-28

### Changed
- **BREAKING**: Replaced `UsageStatsManager`-based collection with `UsageEvents`-based session tracking. On Android 10+ (API 29), sessions use `ACTIVITY_RESUMED`/`ACTIVITY_PAUSED` events for precise per-activity start and end times. On earlier versions, `MOVE_TO_FOREGROUND`/`MOVE_TO_BACKGROUND` events are used.
- Consecutive activity transitions within the same app are merged into a single session when the gap between them is ≤ 30 seconds.
- Edge-case handling for apps already open at query start (inferred `startTime`) and apps still open at query end (inferred `endTime`).

## [3.0.0] - 2026-03-19

### Added
- **Movie watching tracking** via Letterboxd RSS. Automatically detects films logged on Letterboxd within the queried time range. Returns titles, watch dates, and confidence score. Requires a public Letterboxd profile.

## [2.0.0] - 2026-03-12

### Added
- **Reading habit tracking** with support for Kindle and Google Play Books.

### Changed
- **BREAKING**: Confidence fields now return `Float` instead of `Int` for precise scores (e.g. `0.75` instead of `75`).

## [1.0.0] - 2026-03-09

### Added
- Initial release of the Tracker library.
- Language learning habit tracking with automatic app usage detection.
- Support for 13 language learning apps (Duolingo, Anki, Kanji Study, Renshuu, and more).
- `Tracker` class with fluent Builder API.
- Kotlin coroutines support.
- Confidence scoring system (0.0–1.0) with HIGH / MEDIUM / LOW levels.
- `PACKAGE_USAGE_STATS` permission checking — never auto-requests, never blocks.
- Android 11+ package visibility declarations.
- ProGuard/R8 consumer rules.
- Apache 2.0 license.

[5.0.0]: https://github.com/IrynaTsymbaliuk/Tracker/releases/tag/v5.0.0
[4.0.0]: https://github.com/IrynaTsymbaliuk/Tracker/releases/tag/v4.0.0
[3.0.0]: https://github.com/IrynaTsymbaliuk/Tracker/releases/tag/v3.0.0
[2.0.0]: https://github.com/IrynaTsymbaliuk/Tracker/releases/tag/v2.0.0
[1.0.0]: https://github.com/IrynaTsymbaliuk/Tracker/releases/tag/v1.0.0