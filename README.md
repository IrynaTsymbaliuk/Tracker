# Tracker

**Detect user habits from Android system data and third-party services — no user input required.**

Tracker is an Android library that automatically identifies behaviors like language learning, reading, movie watching, social media usage, step counting, meditation, and exercise by analyzing device usage, Health Connect data, and third-party feeds. Your app gets structured habit data with confidence scores, without asking users to log anything manually.

## Is This for You?

- ✅ Building a habit tracking, wellness, or productivity app
- ✅ Want to detect behaviors without manual logging
- ✅ Need confidence scores for detected activities
- ✅ Want graceful degradation when permissions are missing

- ❌ You just need step counting — use Health Connect directly
- ❌ You want to track custom in-app actions — use your own analytics

## Quick Start

```kotlin
val tracker = Tracker.Builder(context)
    .setLetterboxdUsername("your_username")  // optional: required only for movie watching
    .build()

lifecycleScope.launch {
    try {
        val reading = tracker.queryReading()           // today by default
        val learning = tracker.queryLanguageLearning()
        val social = tracker.querySocialMedia()

        // Reading — null means no activity in the time range
        reading?.durationMinutes   // total minutes across all sessions
        reading?.confidence        // 0.0–1.0
        reading?.confidence?.toConfidenceLevel()  // optional LOW / MEDIUM / HIGH banding (extension)
        reading?.sessions          // List<UsageSession> sorted by startTime

        // Language learning
        learning?.durationMinutes
        learning?.sessions

        // Social media
        social?.durationMinutes
        social?.sessions           // List<UsageSession> — one entry per app-open

        // Derive apps and session count from sessions
        reading?.sessions?.map { it.appName }?.distinct()   // ["Kindle"]
        social?.sessions?.size                               // 23
        social?.sessions?.groupBy { it.packageName }        // per-app session breakdown
    } catch (e: PermissionDeniedException) {
        // PACKAGE_USAGE_STATS not granted — direct user to Settings
    } catch (e: NoMonitorableAppsException) {
        // None of the known apps are installed on this device
    }

    // Movie watching requires a Letterboxd username
    // Throws IllegalStateException if username is not set
    try {
        val movies = tracker.queryMovieWatching()
        movies?.count              // number of films logged (= sessions.size) — null means no films in range
        movies?.sessions           // List<MovieSession> — title, year, watchedDate, publishedDate,
                                   //   tmdbId, rating, review, posterUrl, isRewatch, isLiked

        // Each session carries the film's The Movie Database (TMDB) id from the feed's
        // tmdb:movieId element — use it to fetch runtime, cast, etc. from TMDB.
        // tmdbId is null when the feed omits it (e.g. TV entries or unlinked films).
        // year, rating (0.5–5.0), review and posterUrl are null when the feed omits them;
        // isRewatch flags rewatches and isLiked flags films the user hearted.
        movies?.sessions?.forEach { session ->
            session.year         // e.g. 2024, or null
            session.tmdbId?.let { id ->
                // GET https://api.themoviedb.org/3/movie/$id
            }
            session.rating       // e.g. 4.5f, or null
            session.review       // plain-text review, or null
            session.posterUrl    // poster image URL from the feed, or null
            session.isRewatch    // true if logged as a rewatch
            session.isLiked      // true if the user liked (hearted) the film
        }
    } catch (e: IllegalStateException) {
        // Username not configured — call tracker.setLetterboxdUsername("username") first
    } catch (e: NetworkException) {
        // Network request failed
    }

    // Step counting via Health Connect — returns null if HC unavailable or permission not granted
    val steps = tracker.queryStepCounting(days = 2)
    steps?.totalSteps      // Long — deduplicated total across the queried window
    steps?.sessions        // List<StepSession> — one hourly bucket per non-empty hour
    steps?.confidence      // 0.99 when sourced from Health Connect

    // Distance via Health Connect — walking, running, cycling, etc. Returns null if HC
    // unavailable, API < 26, or READ_DISTANCE not granted.
    val distance = tracker.queryDistance(days = 2)
    distance?.totalMeters       // Double — deduplicated total across the window, in meters
    distance?.totalKilometers   // Double — convenience accessor (totalMeters / 1000)
    distance?.sessions          // List<DistanceSession> — one hourly bucket per non-empty hour

    // Meditation — fuses Health Connect MindfulnessSessionRecord with known-meditation-app
    // foreground sessions. Falls back to UsageStats-only if Health Connect is unavailable.
    val meditation = tracker.queryMeditation()
    meditation?.durationMinutes  // total meditation time across all (deduplicated) sessions
    meditation?.sessions         // List<MeditationSession> sorted by startTime
    meditation?.sources          // [HEALTH_CONNECT], [USAGE_STATS], or [HEALTH_CONNECT, USAGE_STATS]

    // A session that was seen by both HealthConnect and Calm is merged into one:
    meditation?.sessions?.forEach { s ->
        s.sources       // e.g. [HEALTH_CONNECT, USAGE_STATS] for a merged session
        s.packageName   // "com.calm.android" when UsageStats contributed; null for HC-only
        s.appName       // "Calm" when UsageStats contributed; null for HC-only
    }

    // Exercise via Health Connect ExerciseSessionRecord — returns null if HC unavailable,
    // READ_EXERCISE permission not granted, or no sessions in the window.
    val exercise = tracker.queryExercise()
    exercise?.durationMinutes  // total exercise time across all sessions
    exercise?.sessions         // List<ExerciseSession> sorted by startTime
    exercise?.confidence       // 0.99 — ExerciseSessionRecord is authoritative

    // Each session exposes both the raw HC type id and a snake_case string:
    exercise?.sessions?.forEach { s ->
        s.exerciseTypeId   // Int — e.g. 56 (EXERCISE_TYPE_RUNNING)
        s.exerciseType     // String — e.g. "running", "strength_training", "yoga"
        s.durationMinutes
    }

    // Derive per-type breakdowns directly:
    val durationByType: Map<String, Int> = exercise?.sessions
        ?.groupBy { it.exerciseType }
        ?.mapValues { (_, s) -> s.sumOf { it.durationMinutes } }
        ?: emptyMap()
}
```

**Example output (today):**
- Reading: 30 min · 2 sessions · Kindle · 75% confidence (MEDIUM)
- Language Learning: 45 min · 5 sessions · Duolingo, Anki · 85% confidence (HIGH)
- Movie Watching: 3 films · The Matrix (tmdb:603), Inception (tmdb:27205), Interstellar (tmdb:157336) · 95% confidence (HIGH)
- Social Media: 120 min · 23 sessions · Instagram, Reddit, WhatsApp · 88% confidence (HIGH)
- Steps: 7,622 steps · 99% confidence (HIGH)
- Distance: 5.42 km · 99% confidence (HIGH)
- Meditation: 15 min · 1 session · Calm (HealthConnect + UsageStats merged) · 97% confidence (HIGH)
- Exercise: 45 min · 2 sessions · Running, Strength Training · 99% confidence (HIGH)

Session count and app list are derived from `sessions`:
```kotlin
result?.sessions?.size                            // session count
result?.sessions?.map { it.appName }?.distinct() // app list
```

## Listing tracked apps

To discover which apps the library can detect for a habit — independent of what's installed or which permissions are granted — call the matching `getTracked*Apps()` method:

```kotlin
val readingApps = tracker.getTrackedReadingApps()
readingApps.forEach { app ->
    app.packageName            // e.g. "com.amazon.kindle"
    app.confidenceMultiplier   // base confidence when this app is detected, e.g. 0.82
}

// Just the package names for the language-learning catalogue:
val packages = tracker.getTrackedLanguageLearningApps().map { it.packageName }

tracker.getTrackedSocialMediaApps()
tracker.getTrackedMeditationApps()
```

These exist only for the habits backed by a known-app list: language learning, reading, social media, and meditation. Movie watching (Letterboxd RSS), step counting, and exercise (Health Connect) are sourced from feeds and sensors rather than a fixed app list, so they have no equivalent. The lists are static configuration — they reflect what the library *can* detect, not what is installed on the current device. (Meditation can additionally be detected from Health Connect `MindfulnessSessionRecord`s; `getTrackedMeditationApps()` covers only the known-app source.)

## Querying by time window

All query methods accept an optional `days` parameter:

```kotlin
tracker.queryReading(days = 1)   // today: midnight → now (default)
tracker.queryReading(days = 2)   // yesterday midnight → now
tracker.queryReading(days = 7)   // 6 days ago midnight → now
```

`days = 1` always starts at **midnight of the current day** in the device's local timezone, not 24 hours ago. This means results grow throughout the day as more activity is recorded.

```
days = 1  │ ████░░░░  today so far
days = 2  │ ████████ ████░░░░  yesterday (complete) + today so far
days = 7  │ ████████ ████████ ████████ ████████ ████████ ████████ ████░░░░
```

**Constraints:**
- Must be `>= 1` — throws `IllegalArgumentException` otherwise
- Android retains usage events for approximately 14 days. Queries beyond that return empty results without error.

## Metrics

| Metric | Source | Apps / Data | Permission |
|---|---|---|---|
| **LANGUAGE_LEARNING** | Foreground session events | Expanded known-app catalog including Duolingo, Anki, Drops, Busuu, Babbel, LingQ, Quizlet, Renshuu, and other study apps | `PACKAGE_USAGE_STATS` |
| **READING** | Foreground session events | Expanded known-app catalog including Kindle, Google Play Books, Audible, Libby, Kobo, ReadEra, Moon+ Reader, Pocket, Substack, and other reading/listening apps | `PACKAGE_USAGE_STATS` |
| **MOVIE_WATCHING** | Letterboxd RSS | Film titles, release years, watch dates, TMDB ids, poster URLs, plus the user's rating, review, rewatch flag, and like flag from public feed | `INTERNET` (no user prompt) |
| **SOCIAL_MEDIA** | Foreground session events | Expanded known-app catalog including Facebook, Instagram, X/Twitter, TikTok, Reddit, WhatsApp, Telegram, LINE, Discord, Bluesky, Mastodon, and other social/messaging apps | `PACKAGE_USAGE_STATS` |
| **STEP_COUNTING** | Health Connect | Aggregated across all step sources, deduped by HC | `health.READ_STEPS` · API 26+ |
| **DISTANCE** | Health Connect | `DistanceRecord` aggregated across all sources (walking, running, cycling, etc.), deduped by HC | `health.READ_DISTANCE` · API 26+ |
| **MEDITATION** | Health Connect + Foreground session events (fused) | `MindfulnessSessionRecord`s plus Calm, Headspace, Insight Timer, Balance, Waking Up, Smiling Mind, Ten Percent Happier, Medito, MEISOON, Mindvalley | `health.READ_MINDFULNESS` (optional, API 26+) · `PACKAGE_USAGE_STATS` |
| **EXERCISE** | Health Connect | `ExerciseSessionRecord`s written by any fitness app (Strava, Google Fit, Samsung Health, Peloton, etc.) or logged manually | `health.READ_EXERCISE` · API 26+ |

**Note on Social Media**: Includes messaging apps (WhatsApp, Telegram) with lower confidence scores (0.75) as they may be used for work or family communication.

**Note on session accuracy**: On Android 10+ (API 29), session tracking uses `ACTIVITY_RESUMED`/`ACTIVITY_PAUSED` events for precise per-session start and end times. Consecutive activity transitions within the same app are merged into a single session if the gap between them is under 30 seconds.

**Note on sessions deduplication**: When storing sessions locally across multiple queries, use `(packageName, startTime)` as the composite key. Exception: if `session.startTime == result.timeRange.from`, the session start was inferred (the app was already open at the query boundary) — use `(packageName, endTime)` for those. Sessions under 1 minute have `durationMinutes = 0` but are still present in the list. See `UsageSession` for full details.

**Note on movie watching**: `queryMovieWatching()` parses the public Letterboxd RSS feed for the configured username. Each `MovieSession` exposes the film `title` (from the feed's `letterboxd:filmTitle` element) and `year` (from `letterboxd:filmYear`, `null` when omitted), the `watchedDate` and `publishedDate` (milliseconds), and `tmdbId` — the The Movie Database movie id taken from the feed's `tmdb:movieId` element. The `watchedDate` is read from the feed's authoritative `letterboxd:watchedDate` element (parsed at UTC midnight), falling back to the diary entry's published date when absent. Use `tmdbId` to look up full details (runtime, cast, etc.) via the TMDB API, e.g. `https://api.themoviedb.org/3/movie/{tmdbId}`. `tmdbId` is `null` when the feed omits it (TV diary entries carry `tmdb:tvId` instead, and a few films are not yet linked to TMDB), so always null-check before using it. Each session also carries the user's own log data: `rating` (the `letterboxd:memberRating` star score on a 0.5–5.0 scale, `null` when the entry is unrated), `review` (the written review as plain text with the feed's HTML and "Watched on …" boilerplate stripped, `null` when there's no review), `posterUrl` (the poster image URL extracted from the entry description, `null` when omitted), `isRewatch` (from `letterboxd:rewatch`, `false` when omitted), and `isLiked` (from `letterboxd:memberLike`, `true` when the user hearted the film, `false` when omitted).

**Note on step counting**: `queryStepCounting(days)` uses Health Connect's `aggregateGroupByDuration` API with a 1-hour slice, which deduplicates across all contributing apps (e.g. Google Fit, phone step counter) before returning each bucket's count. The result exposes hourly `StepSession` buckets in `sessions` (so callers can separate today's steps from yesterday's) and a convenience `totalSteps` sum. Hours with no recorded steps are omitted. Returns `null` if Health Connect is unavailable or the `READ_STEPS` permission has not been granted.

**Note on distance**: `queryDistance(days)` reads Health Connect `DistanceRecord` via the same `aggregateGroupByDuration` 1-hour slicing as step counting, so it deduplicates across all writing apps (Google Fit, Pixel, Fitbit, etc.) by the user's data-source priority before returning each bucket. The result exposes hourly `DistanceSession` buckets in `sessions` plus convenience `totalMeters`/`totalKilometers` sums. Distance is in **meters** (a `Double`, since records are fractional). Hours with no recorded distance are omitted. Returns `null` if Health Connect is unavailable, the API level is below 26, or the `READ_DISTANCE` permission has not been granted.

**Note on meditation**: `queryMeditation()` fuses two sources:
- **Health Connect** `MindfulnessSessionRecord` (authoritative, confidence `0.99`)
- **UsageStats** foreground sessions of known meditation apps (confidence `0.85`–`0.95` per app)

Sessions that overlap significantly (≥ 50% of the shorter session's duration) are deduplicated into a single `MeditationSession` whose `sources` list contains both `HEALTH_CONNECT` and `USAGE_STATS`. The result's top-level `sources` reports every source that contributed. If Health Connect is unavailable, the record type is unsupported on this device, or the `READ_MINDFULNESS` permission is denied, the query automatically falls back to UsageStats-only. Returns `null` only when **neither** source produced any sessions.

**Note on exercise**: `queryExercise()` reads `ExerciseSessionRecord`s from Health Connect — these are authoritative entries written by fitness apps (Strava, Google Fit, Samsung Health, Peloton, Nike Run Club, and many others) or logged manually by the user. Confidence is fixed at `0.99`. No minimum-duration filter is applied: short sessions appear in `sessions` with `durationMinutes = 0` (rounded from seconds) so the session count stays accurate. Each `ExerciseSession` exposes both `exerciseTypeId` (the raw Health Connect integer, useful for programmatic mapping) and `exerciseType` (a snake_case string, e.g. `"running"`, `"strength_training"`, `"yoga"`). Returns `null` if Health Connect is unavailable, the API level is below 26, the `READ_EXERCISE` permission has not been granted, or no sessions exist in the window.

**Note on the `sources` field**: every `HabitResult` exposes `sources: List<DataSource>` (not `source`). Single-source results contain a one-element list; meditation may contain one or two elements depending on which sources contributed.

## Installation

Tracker is published through JitPack.

Add JitPack to your project repositories:

```kotlin
dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
        maven("https://jitpack.io")
    }
}
```

Then add the library dependency:

```kotlin
dependencies {
    implementation("com.github.IrynaTsymbaliuk:tracker:1.2.2")
}
```

Add to `AndroidManifest.xml`:

```xml
<!-- Required for language learning, reading, and social media -->
<uses-permission android:name="android.permission.PACKAGE_USAGE_STATS"
    tools:ignore="ProtectedPermissions" />

<!-- Required for movie watching (Letterboxd RSS) -->
<uses-permission android:name="android.permission.INTERNET" />

<!-- Required for step counting via Health Connect -->
<uses-permission android:name="android.permission.health.READ_STEPS" />

<!-- Required for distance via Health Connect -->
<uses-permission android:name="android.permission.health.READ_DISTANCE" />

<!-- Optional but recommended for meditation — enables the Health Connect mindfulness source.
     The meditation query falls back to UsageStats-only if this permission is not granted. -->
<uses-permission android:name="android.permission.health.READ_MINDFULNESS" />

<!-- Required for exercise via Health Connect -->
<uses-permission android:name="android.permission.health.READ_EXERCISE" />
```

`PACKAGE_USAGE_STATS` is a protected permission — the user must grant it manually via **Settings → Apps → Special app access → Usage access**.

Health Connect permissions (`health.READ_STEPS`, `health.READ_MINDFULNESS`, `health.READ_EXERCISE`, `health.READ_DISTANCE`) must be requested at runtime using `PermissionController.createRequestPermissionResultContract()`. You can request all of them in a single prompt:

```kotlin
val launcher = registerForActivityResult(
    PermissionController.createRequestPermissionResultContract()
) { /* refresh UI */ }

launcher.launch(setOf(
    HealthPermission.getReadPermission(StepsRecord::class),
    HealthPermission.getReadPermission(MindfulnessSessionRecord::class),
    HealthPermission.getReadPermission(ExerciseSessionRecord::class),
    HealthPermission.getReadPermission(DistanceRecord::class)
))
```

Add the following to the activity that handles the permission result:

```xml
<!-- Required for Health Connect (Android 9–13) -->
<intent-filter>
    <action android:name="androidx.health.ACTION_SHOW_PERMISSIONS_RATIONALE" />
    <category android:name="android.intent.category.DEFAULT" />
</intent-filter>
```

```xml
<!-- Required for Health Connect (Android 14+) -->
<activity-alias
    android:name=".ViewPermissionUsageActivity"
    android:exported="true"
    android:permission="android.permission.START_VIEW_PERMISSION_USAGE"
    android:targetActivity=".YourActivity">
    <intent-filter>
        <action android:name="android.intent.action.VIEW_PERMISSION_USAGE" />
        <category android:name="android.intent.category.HEALTH_PERMISSIONS" />
    </intent-filter>
</activity-alias>
```

## Privacy

- All usage stats are processed entirely on-device
- Health Connect data never leaves the device
- Letterboxd data is fetched from public RSS feeds — no authentication, no private data
- No data is sent to any server beyond the third-party services you configure

## Requirements

- **Min SDK**: 21 (Android 5.0)
- **Target SDK**: 36
- **Kotlin**: 2.0.21
- **Step counting, distance, meditation (HealthConnect branch), exercise**: require API 26+ and Health Connect

## Sample App

```bash
./gradlew :app:installDebug
```

Demonstrates the full flow: permission setup for `PACKAGE_USAGE_STATS` and Health Connect (steps + mindfulness + exercise + distance, all requested in one prompt), querying all eight metrics for today (language learning, reading, social media, movie watching, step counting, distance, meditation, exercise), and displaying results. For the usage-based metrics (language learning, reading, social media), step counting, and distance, the sample expands each result's `sessions` list into a per-session breakdown — one indented line per session showing **time from – time to** and the **app name** (or step count / distance for hourly buckets). The movie watching row expands into one line per film showing the **watched date**, **title**, and **TMDB id** (`tmdb:<id>`) when present. The meditation row shows which sources contributed (`HC`, `Usage`, or `HC+Usage`); the exercise row lists the distinct exercise types detected (e.g. `Running, Strength Training`). Movie watching is disabled by default in the sample; to enable it, add `.setLetterboxdUsername("your_username")` when building `Tracker` in `MainActivity.kt`.

## License

Apache 2.0 — see [LICENSE](LICENSE) for details.
