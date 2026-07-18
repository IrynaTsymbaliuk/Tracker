# Tracker

**Detect user habits from Android system data and third-party services — no user input required.**

Tracker is an Android library that automatically identifies behaviors like language learning, reading, movie watching, social media usage, step counting, meditation, exercise, training plans, and sleep by analyzing device usage, Health Connect data, and third-party feeds. Your app gets structured habit data — each result tagged with the data source it came from — without asking users to log anything manually.

## Is This for You?

- ✅ Building a habit tracking, wellness, or productivity app
- ✅ Want to detect behaviors without manual logging
- ✅ Want to know each detection's data source (authoritative sensor/log vs. inferred app usage)
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
        reading?.sources           // [USAGE_STATS] — where the detection came from
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
    steps?.sources         // [HEALTH_CONNECT]

    // Distance via Health Connect — walking, running, cycling, etc. Returns null if HC
    // unavailable, API < 26, or READ_DISTANCE not granted.
    val distance = tracker.queryDistance(days = 2)
    distance?.totalMeters       // Double — deduplicated total across the window, in meters
    distance?.totalKilometers   // Double — convenience accessor (totalMeters / 1000)
    distance?.sessions          // List<DistanceSession> — one hourly bucket per non-empty hour

    // Body measurements via Health Connect. Each list preserves complete records sorted by time;
    // related scale values are intentionally not combined by timestamp.
    val body = tracker.queryBodyMeasurements(days = 30)
    body?.weightRecords?.lastOrNull()?.weight?.inKilograms              // e.g. 70.2
    body?.bodyFatRecords?.lastOrNull()?.percentage?.value               // e.g. 21.5 (percent)
    body?.leanBodyMassRecords?.lastOrNull()?.mass?.inKilograms          // closest HC muscle value
    body?.boneMassRecords?.lastOrNull()?.mass?.inKilograms
    body?.bodyWaterMassRecords?.lastOrNull()?.mass?.inKilograms
    body?.basalMetabolicRateRecords?.lastOrNull()?.basalMetabolicRate
        ?.inKilocaloriesPerDay
    body?.heightRecords?.lastOrNull()?.height?.inMeters

    // Sleep via Health Connect SleepSessionRecord — one entry per night/nap. Returns null if
    // HC unavailable, API < 26, or READ_SLEEP not granted.
    val sleep = tracker.querySleep(days = 2)
    sleep?.totalSleepMinutes   // Long — total time asleep across the window, in minutes
    sleep?.totalSleepHours     // Double — convenience accessor (totalSleepMinutes / 60)
    sleep?.sessions?.forEach { night ->
        night.startTime        // Long — when the user fell asleep (epoch millis)
        night.endTime          // Long — when the user woke
        night.asleepMinutes    // Long — minutes actually asleep (excludes awake stages)
        night.timeInBedMinutes // Long — startTime..endTime
        night.efficiency       // Double? — asleep / time in bed (null if no stage data)
        night.quality          // SleepQuality — EXCELLENT/GOOD/FAIR/POOR/UNKNOWN (from efficiency)
        night.deepMinutes      // Long — deep-sleep minutes (0 if source wrote no stages)
        night.remMinutes       // Long — REM minutes
        night.lightMinutes     // Long — light-sleep minutes
        night.awakeMinutes     // Long — minutes awake during the session
        night.stages           // List<SleepStage> — raw stage intervals, may be empty
    }

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
    exercise?.sources          // [HEALTH_CONNECT] — ExerciseSessionRecord is authoritative

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

    // Training plans via Health Connect PlannedExerciseSessionRecord. The explicit range overload
    // also supports upcoming plans; every platform field is available through session.record.
    val training = tracker.queryTraining(
        fromMillis = System.currentTimeMillis(),
        toMillis = System.currentTimeMillis() + 7 * 24 * 60 * 60 * 1000L
    )
    training?.durationMinutes  // Long — planned minutes across all sessions
    training?.sessions?.forEach { session ->
        session.exerciseType    // String — e.g. "running", "strength_training"
        session.exerciseTypeId  // Int — raw Health Connect type id
        session.record          // PlannedExerciseSessionRecord — title, notes, blocks, steps,
                                // goals, performance targets, metadata, zone offsets, etc.
    }
}
```

**Example output (today):**
- Reading: 30 min · 2 sessions · Kindle
- Language Learning: 45 min · 5 sessions · Duolingo, Anki
- Movie Watching: 3 films · The Matrix (tmdb:603), Inception (tmdb:27205), Interstellar (tmdb:157336)
- Social Media: 120 min · 23 sessions · Instagram, Reddit, WhatsApp
- Steps: 7,622 steps
- Distance: 5.42 km
- Body: 70.2 kg · 21.5% fat · 54.0 kg lean
- Meditation: 15 min · 1 session · Calm (HealthConnect + UsageStats merged)
- Exercise: 45 min · 2 sessions · Running, Strength Training
- Sleep: 7h 32m asleep · fell asleep 23:15, woke 07:02 · 89% efficiency · GOOD

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
| **BODY_MEASUREMENTS** | Health Connect | Complete, independent `WeightRecord`, `BodyFatRecord`, `LeanBodyMassRecord`, `BoneMassRecord`, `BodyWaterMassRecord`, `BasalMetabolicRateRecord`, and `HeightRecord` streams | related `health.READ_*` body permissions · API 26+ |
| **MEDITATION** | Health Connect + Foreground session events (fused) | `MindfulnessSessionRecord`s plus Calm, Headspace, Insight Timer, Balance, Waking Up, Smiling Mind, Ten Percent Happier, Medito, MEISOON, Mindvalley | `health.READ_MINDFULNESS` (optional, API 26+) · `PACKAGE_USAGE_STATS` |
| **EXERCISE** | Health Connect | `ExerciseSessionRecord`s written by any fitness app (Strava, Google Fit, Samsung Health, Peloton, etc.) or logged manually | `health.READ_EXERCISE` · API 26+ |
| **TRAINING** | Health Connect | `PlannedExerciseSessionRecord`s (training plans), including titles, notes, completion links, metadata, and complete block/step goals and targets | `health.READ_PLANNED_EXERCISE` · API 26+ · planned-exercise feature |
| **SLEEP** | Health Connect | `SleepSessionRecord`s (one per night/nap) with fall-asleep/wake times, per-stage breakdown, efficiency, and a derived quality band | `health.READ_SLEEP` · API 26+ |

**Note on Social Media**: Includes messaging apps (WhatsApp, Telegram) with a lower base `confidenceMultiplier` (0.75, exposed via `getTrackedSocialMediaApps()`) as they may be used for work or family communication.

**Note on session accuracy**: On Android 10+ (API 29), session tracking uses `ACTIVITY_RESUMED`/`ACTIVITY_PAUSED` events for precise per-session start and end times. Consecutive activity transitions within the same app are merged into a single session if the gap between them is under 30 seconds.

**Note on sessions deduplication**: When storing sessions locally across multiple queries, use `(packageName, startTime)` as the composite key. Exception: if `session.startTime == result.timeRange.from`, the session start was inferred (the app was already open at the query boundary) — use `(packageName, endTime)` for those. Sessions under 1 minute have `durationMinutes = 0` but are still present in the list. See `UsageSession` for full details.

**Note on movie watching**: `queryMovieWatching()` parses the public Letterboxd RSS feed for the configured username. Each `MovieSession` exposes the film `title` (from the feed's `letterboxd:filmTitle` element) and `year` (from `letterboxd:filmYear`, `null` when omitted), the `watchedDate` and `publishedDate` (milliseconds), and `tmdbId` — the The Movie Database movie id taken from the feed's `tmdb:movieId` element. The `watchedDate` is read from the feed's authoritative `letterboxd:watchedDate` element (parsed at UTC midnight), falling back to the diary entry's published date when absent. Use `tmdbId` to look up full details (runtime, cast, etc.) via the TMDB API, e.g. `https://api.themoviedb.org/3/movie/{tmdbId}`. `tmdbId` is `null` when the feed omits it (TV diary entries carry `tmdb:tvId` instead, and a few films are not yet linked to TMDB), so always null-check before using it. Each session also carries the user's own log data: `rating` (the `letterboxd:memberRating` star score on a 0.5–5.0 scale, `null` when the entry is unrated), `review` (the written review as plain text with the feed's HTML and "Watched on …" boilerplate stripped, `null` when there's no review), `posterUrl` (the poster image URL extracted from the entry description, `null` when omitted), `isRewatch` (from `letterboxd:rewatch`, `false` when omitted), and `isLiked` (from `letterboxd:memberLike`, `true` when the user hearted the film, `false` when omitted).

**Note on step counting**: `queryStepCounting(days)` uses Health Connect's `aggregateGroupByDuration` API with a 1-hour slice, which deduplicates across all contributing apps (e.g. Google Fit, phone step counter) before returning each bucket's count. The result exposes hourly `StepSession` buckets in `sessions` (so callers can separate today's steps from yesterday's) and a convenience `totalSteps` sum. Hours with no recorded steps are omitted. Returns `null` if Health Connect is unavailable or the `READ_STEPS` permission has not been granted.

**Note on distance**: `queryDistance(days)` reads Health Connect `DistanceRecord` via the same `aggregateGroupByDuration` 1-hour slicing as step counting, so it deduplicates across all writing apps (Google Fit, Pixel, Fitbit, etc.) by the user's data-source priority before returning each bucket. The result exposes hourly `DistanceSession` buckets in `sessions` plus convenience `totalMeters`/`totalKilometers` sums. Distance is in **meters** (a `Double`, since records are fractional). Hours with no recorded distance are omitted. Returns `null` if Health Connect is unavailable, the API level is below 26, or the `READ_DISTANCE` permission has not been granted.

**Note on body measurements**: `queryBodyMeasurements(days)` reads the raw Health Connect body-measurement record types: weight, body-fat percentage, lean body mass, bone mass, body-water mass, basal metabolic rate, and height. `BodyMeasurementsResult` exposes one complete, time-sorted list per type, including each record's metadata and zone offset. Tracker deliberately does **not** pair records into artificial scale readings: a device can write weight and body fat seconds apart, or omit one entirely. Health Connect does not provide a muscle-percentage record; `leanBodyMassRecords` is the closest available muscle-related value and is a mass measurement. The query accepts any granted subset of the seven body permissions and returns accessible records; it returns `null` if no records exist, none of those permissions is granted, Health Connect is unavailable, or the API level is below 26.

**Note on sleep**: `querySleep(days)` reads raw Health Connect `SleepSessionRecord`s (via `readRecords`, like exercise — **not** the hourly aggregation used for steps/distance), returning one `SleepSession` per night or nap in the window, sorted by start time. Each session exposes:

- `startTime` / `endTime` — when the user **fell asleep** and **woke** (epoch millis).
- `asleepMinutes` — minutes actually asleep (sum of the light/deep/REM/sleeping stages). When the source recorded no stages, this falls back to the full `timeInBedMinutes`.
- `timeInBedMinutes`, `awakeMinutes`, `lightMinutes`, `deepMinutes`, `remMinutes` — the stage split. Stage minutes are `0` when the source wrote no stage detail.
- `efficiency` — `asleepMinutes / timeInBedMinutes` as a `Double` (0.0–1.0), or `null` when there is no stage data (no awake time to measure).
- `quality` — a `SleepQuality` band (`EXCELLENT ≥ 0.90`, `GOOD ≥ 0.85`, `FAIR ≥ 0.75`, else `POOR`; `UNKNOWN` when `efficiency` is `null`). This is a **non-clinical convenience** derived from sleep efficiency (the standard quality proxy); apps wanting their own scoring can use the raw stage minutes instead.
- `stages` — the raw `List<SleepStage>` (each with `startTime`, `endTime`, and a `SleepStageType`).

`SleepResult.totalSleepMinutes` / `totalSleepHours` sum `asleepMinutes` across all sessions. Sessions are returned as written, with **no** cross-source deduplication — if two apps logged the same night you will see both (matching exercise). Returns `null` if Health Connect is unavailable, the API level is below 26, or the `READ_SLEEP` permission has not been granted.

**Note on meditation**: `queryMeditation()` fuses two sources:
- **Health Connect** `MindfulnessSessionRecord` (authoritative)
- **UsageStats** foreground sessions of known meditation apps (base `confidenceMultiplier` `0.85`–`0.95` per app, exposed via `getTrackedMeditationApps()`)

Sessions that overlap significantly (≥ 50% of the shorter session's duration) are deduplicated into a single `MeditationSession` whose `sources` list contains both `HEALTH_CONNECT` and `USAGE_STATS`. The result's top-level `sources` reports every source that contributed. If Health Connect is unavailable, the record type is unsupported on this device, or the `READ_MINDFULNESS` permission is denied, the query automatically falls back to UsageStats-only. Returns `null` only when **neither** source produced any sessions.

**Note on exercise**: `queryExercise()` reads `ExerciseSessionRecord`s from Health Connect — these are authoritative entries written by fitness apps (Strava, Google Fit, Samsung Health, Peloton, Nike Run Club, and many others) or logged manually by the user — authoritative entries, so no heuristic scoring is applied. No minimum-duration filter is applied: short sessions appear in `sessions` with `durationMinutes = 0` (rounded from seconds) so the session count stays accurate. Each `ExerciseSession` exposes both `exerciseTypeId` (the raw Health Connect integer, useful for programmatic mapping) and `exerciseType` (a snake_case string, e.g. `"running"`, `"strength_training"`, `"yoga"`). Returns `null` if Health Connect is unavailable, the API level is below 26, the `READ_EXERCISE` permission has not been granted, or no sessions exist in the window.

**Note on training**: `queryTraining(days)` reads Health Connect `PlannedExerciseSessionRecord`s in Tracker's normal historic day window. Training plans can be future-dated, so `queryTraining(fromMillis, toMillis)` is available for upcoming plans. `TrainingSession.record` is the complete platform record, intentionally left unflattened: it includes exact timestamps/zone offsets, `hasExplicitTime`, title, notes, linked completed exercise id, metadata, and the complete `blocks` → `steps` → goals/targets hierarchy. It returns `null` when Health Connect is unavailable, API level is below 26, the planned-exercise feature is unavailable, the `READ_PLANNED_EXERCISE` permission is not granted, or no plans exist in the window.

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
    implementation("com.github.IrynaTsymbaliuk:tracker:1.3.0")
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

<!-- Required for planned training sessions via Health Connect -->
<uses-permission android:name="android.permission.health.READ_PLANNED_EXERCISE" />

<!-- Required for sleep sessions via Health Connect -->
<uses-permission android:name="android.permission.health.READ_SLEEP" />

<!-- Required for the corresponding body-measurement streams via Health Connect -->
<uses-permission android:name="android.permission.health.READ_WEIGHT" />
<uses-permission android:name="android.permission.health.READ_BODY_FAT" />
<uses-permission android:name="android.permission.health.READ_LEAN_BODY_MASS" />
<uses-permission android:name="android.permission.health.READ_BONE_MASS" />
<uses-permission android:name="android.permission.health.READ_BODY_WATER_MASS" />
<uses-permission android:name="android.permission.health.READ_BASAL_METABOLIC_RATE" />
<uses-permission android:name="android.permission.health.READ_HEIGHT" />
```

`PACKAGE_USAGE_STATS` is a protected permission — the user must grant it manually via **Settings → Apps → Special app access → Usage access**.

Health Connect permissions (including the selected body-measurement permissions below) must be requested at runtime using `PermissionController.createRequestPermissionResultContract()`. You can request all of them in a single prompt:

```kotlin
val launcher = registerForActivityResult(
    PermissionController.createRequestPermissionResultContract()
) { /* refresh UI */ }

launcher.launch(setOf(
    HealthPermission.getReadPermission(StepsRecord::class),
    HealthPermission.getReadPermission(MindfulnessSessionRecord::class),
    HealthPermission.getReadPermission(ExerciseSessionRecord::class),
    HealthPermission.getReadPermission(PlannedExerciseSessionRecord::class),
    HealthPermission.getReadPermission(DistanceRecord::class),
    HealthPermission.getReadPermission(SleepSessionRecord::class),
    HealthPermission.getReadPermission(WeightRecord::class),
    HealthPermission.getReadPermission(BodyFatRecord::class),
    HealthPermission.getReadPermission(LeanBodyMassRecord::class),
    HealthPermission.getReadPermission(BoneMassRecord::class),
    HealthPermission.getReadPermission(BodyWaterMassRecord::class),
    HealthPermission.getReadPermission(BasalMetabolicRateRecord::class),
    HealthPermission.getReadPermission(HeightRecord::class)
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
- **Step counting, distance, body measurements, meditation (HealthConnect branch), exercise**: require API 26+ and Health Connect

## Sample App

```bash
./gradlew :app:installDebug
```

Demonstrates the full flow: permission setup for `PACKAGE_USAGE_STATS` and Health Connect (steps, mindfulness, exercise, distance, body measurements, sleep, and supported training plans in one prompt), querying ten current-day metrics (language learning, reading, social media, movie watching, step counting, distance, body measurements, meditation, exercise, sleep) plus training plans in the next seven days, and displaying results. The Body row renders the newest accessible value from each independent record stream without pretending they were written at the same time. For the usage-based metrics (language learning, reading, social media), step counting, and distance, the sample expands each result's `sessions` list into a per-session breakdown — one indented line per session showing **time from – time to** and the **app name** (or step count / distance for hourly buckets). The movie watching row expands into one line per film showing the **watched date**, **title**, and **TMDB id** (`tmdb:<id>`) when present. The meditation row shows which sources contributed (`HC`, `Usage`, or `HC+Usage`); the exercise row lists the distinct exercise types detected (e.g. `Running, Strength Training`); the training row shows each plan's start, title/type, and duration while preserving the full record. Movie watching is disabled by default in the sample; to enable it, add `.setLetterboxdUsername("your_username")` when building `Tracker` in `MainActivity.kt`.

## License

Apache 2.0 — see [LICENSE](LICENSE) for details.
