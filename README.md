# Tracker

**Detect user habits from Android system data and third-party services — no user input required.**

Tracker is an Android library that automatically identifies behaviors like language learning, reading, movie watching, social media usage, physical activity, and meditation by analyzing device usage, Health Connect data, and third-party feeds. Your app gets structured habit data with confidence scores, without asking users to log anything manually.

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
    .setMinConfidence(0.50f)
    .build()

lifecycleScope.launch {
    try {
        val reading = tracker.queryReading()           // today by default
        val learning = tracker.queryLanguageLearning()
        val social = tracker.querySocialMedia()

        // Reading — null means no activity in the time range
        reading?.durationMinutes   // total minutes across all sessions
        reading?.confidence        // 0.0–1.0
        reading?.confidenceLevel   // LOW / MEDIUM / HIGH
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
        movies?.count              // number of films logged — null means no films in range
        movies?.movies             // List<MovieInfo> — title, watchedDate, publishedDate
    } catch (e: IllegalStateException) {
        // Username not configured — call tracker.setLetterboxdUsername("username") first
    } catch (e: NetworkException) {
        // Network request failed
    }

    // Step counting via Health Connect — returns null if HC unavailable or permission not granted
    val steps = tracker.queryStepCounting()
    steps?.steps           // Long — deduplicated across all data sources
    steps?.confidence      // 0.99 when sourced from Health Connect

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
}
```

**Example output (today):**
- Reading: 30 min · 2 sessions · Kindle · 75% confidence (MEDIUM)
- Language Learning: 45 min · 5 sessions · Duolingo, Anki · 85% confidence (HIGH)
- Movie Watching: 3 films · The Matrix, Inception, Interstellar · 95% confidence (HIGH)
- Social Media: 120 min · 23 sessions · Instagram, Reddit, WhatsApp · 88% confidence (HIGH)
- Steps: 7,622 steps · 99% confidence (HIGH)
- Meditation: 15 min · 1 session · Calm (HealthConnect + UsageStats merged) · 97% confidence (HIGH)

Session count and app list are derived from `sessions`:
```kotlin
result?.sessions?.size                            // session count
result?.sessions?.map { it.appName }?.distinct() // app list
```

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
| **LANGUAGE_LEARNING** | Foreground session events | Duolingo, Anki, LingoDeer, Drops, Kanji Study, Renshuu, and 7 more | `PACKAGE_USAGE_STATS` |
| **READING** | Foreground session events | Kindle, Google Play Books | `PACKAGE_USAGE_STATS` |
| **MOVIE_WATCHING** | Letterboxd RSS | Film titles and watch dates from public feed | `INTERNET` (no user prompt) |
| **SOCIAL_MEDIA** | Foreground session events | Facebook, Instagram, Twitter/X, TikTok, Reddit, WhatsApp, and 9 more | `PACKAGE_USAGE_STATS` |
| **STEP_COUNTING** | Health Connect | Aggregated across all step sources, deduped by HC | `health.READ_STEPS` · API 26+ |
| **MEDITATION** | Health Connect + Foreground session events (fused) | `MindfulnessSessionRecord`s plus Calm, Headspace, Insight Timer, Balance, Waking Up, Smiling Mind, Ten Percent Happier, Medito, MEISOON, Mindvalley | `health.READ_MINDFULNESS` (optional, API 26+) · `PACKAGE_USAGE_STATS` |

**Note on Social Media**: Includes messaging apps (WhatsApp, Telegram) with lower confidence scores (0.75) as they may be used for work or family communication.

**Note on session accuracy**: On Android 10+ (API 29), session tracking uses `ACTIVITY_RESUMED`/`ACTIVITY_PAUSED` events for precise per-session start and end times. Consecutive activity transitions within the same app are merged into a single session if the gap between them is under 30 seconds.

**Note on sessions deduplication**: When storing sessions locally across multiple queries, use `(packageName, startTime)` as the composite key. Exception: if `session.startTime == result.timeRange.from`, the session start was inferred (the app was already open at the query boundary) — use `(packageName, endTime)` for those. Sessions under 1 minute have `durationMinutes = 0` but are still present in the list. See `UsageSession` for full details.

**Note on step counting**: `queryStepCounting()` uses Health Connect's aggregation API (`StepsRecord.COUNT_TOTAL`), which deduplicates across all contributing apps (e.g. Google Fit, phone step counter) before returning the total. Returns `null` if Health Connect is unavailable or the `READ_STEPS` permission has not been granted.

**Note on meditation**: `queryMeditation()` fuses two sources:
- **Health Connect** `MindfulnessSessionRecord` (authoritative, confidence `0.99`)
- **UsageStats** foreground sessions of known meditation apps (confidence `0.85`–`0.95` per app)

Sessions that overlap significantly (≥ 50% of the shorter session's duration) are deduplicated into a single `MeditationSession` whose `sources` list contains both `HEALTH_CONNECT` and `USAGE_STATS`. The result's top-level `sources` reports every source that contributed. If Health Connect is unavailable, the record type is unsupported on this device, or the `READ_MINDFULNESS` permission is denied, the query automatically falls back to UsageStats-only. Returns `null` only when **neither** source produced any sessions.

**Note on the `sources` field**: every `HabitResult` exposes `sources: List<DataSource>` (not `source`). Single-source results contain a one-element list; meditation may contain one or two elements depending on which sources contributed.

## Installation

```kotlin
dependencies {
    implementation("com.tracker:core:5.0.0")
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

<!-- Optional but recommended for meditation — enables the Health Connect mindfulness source.
     The meditation query falls back to UsageStats-only if this permission is not granted. -->
<uses-permission android:name="android.permission.health.READ_MINDFULNESS" />
```

`PACKAGE_USAGE_STATS` is a protected permission — the user must grant it manually via **Settings → Apps → Special app access → Usage access**.

Health Connect permissions (`health.READ_STEPS` and `health.READ_MINDFULNESS`) must be requested at runtime using `PermissionController.createRequestPermissionResultContract()`. You can request both in a single prompt:

```kotlin
val launcher = registerForActivityResult(
    PermissionController.createRequestPermissionResultContract()
) { /* refresh UI */ }

launcher.launch(setOf(
    HealthPermission.getReadPermission(StepsRecord::class),
    HealthPermission.getReadPermission(MindfulnessSessionRecord::class)
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
- **Kotlin**: 1.9+
- **Step counting**: requires API 26+ and Health Connect

## Sample App

```bash
./gradlew :app:installDebug
```

Demonstrates the full flow: permission setup for `PACKAGE_USAGE_STATS` and Health Connect (steps + mindfulness), querying all six metrics for today (language learning, reading, social media, movie watching, step counting, meditation), and displaying results. The meditation row shows which sources contributed (`HC`, `Usage`, or `HC+Usage`). To enable movie watching, set your Letterboxd username in `MainActivity.kt`.

## License

Apache 2.0 — see [LICENSE](LICENSE) for details.