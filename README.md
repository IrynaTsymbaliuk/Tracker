# Tracker

**Detect user habits from Android system data and third-party services — no user input required.**

Tracker is an Android library that automatically identifies behaviors like language learning, reading, movie watching, and social media usage by analyzing device usage and third-party feeds. Your app gets structured habit data with confidence scores, without asking users to log anything manually.

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

        // Reading
        reading?.occurred          // true if reading was detected
        reading?.durationMinutes   // total minutes across all sessions
        reading?.confidence        // 0.0–1.0
        reading?.confidenceLevel   // LOW / MEDIUM / HIGH
        reading?.sessions          // List<UsageSession> sorted by startTime

        // Language learning
        learning?.occurred
        learning?.durationMinutes
        learning?.sessions

        // Social media
        social?.occurred
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
        movies?.occurred
        movies?.count              // number of films logged
        movies?.movies             // List<MovieInfo> — title, watchedDate, publishedDate
    } catch (e: IllegalStateException) {
        // Username not configured — call tracker.setLetterboxdUsername("username") first
    } catch (e: NetworkException) {
        // Network request failed
    }
}
```

**Example output (today):**
- Reading: 30 min · 2 sessions · Kindle · 75% confidence (MEDIUM)
- Language Learning: 45 min · 5 sessions · Duolingo, Anki · 85% confidence (HIGH)
- Movie Watching: 3 films · The Matrix, Inception, Interstellar · 95% confidence (HIGH)
- Social Media: 120 min · 23 sessions · Instagram, Reddit, WhatsApp · 88% confidence (HIGH)

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
| **LANGUAGE_LEARNING** | Foreground session events | Duolingo, Anki, LingoDeer, Drops, Kanji Study, Renshuu, and 8 more | `PACKAGE_USAGE_STATS` |
| **READING** | Foreground session events | Kindle, Google Play Books | `PACKAGE_USAGE_STATS` |
| **MOVIE_WATCHING** | Letterboxd RSS | Film titles and watch dates from public feed | `INTERNET` (no user prompt) |
| **SOCIAL_MEDIA_USAGE** | Foreground session events | Facebook, Instagram, Twitter/X, TikTok, Reddit, WhatsApp, and 9 more | `PACKAGE_USAGE_STATS` |

**Note on Social Media**: Includes messaging apps (WhatsApp, Telegram) with lower confidence scores as they may be used for work/family communication.

**Note on session accuracy**: On Android 10+ (API 29), session tracking uses `ACTIVITY_RESUMED`/`ACTIVITY_PAUSED` events for precise per-session start and end times. Consecutive activity transitions within the same app are merged into a single session if the gap between them is under 30 seconds.

**Note on sessions deduplication**: When storing sessions locally across multiple queries, use `(packageName, startTime)` as the composite key. Exception: if `session.startTime == result.timeRange.from`, the session start was inferred (the app was already open at the query boundary) — use `(packageName, endTime)` for those. Sessions under 1 minute have `durationMinutes = 0` but are still present in the list. See `UsageSession` for full details.

## Installation

```kotlin
dependencies {
    implementation("com.tracker:core:4.0.0")
}
```

Add to `AndroidManifest.xml`:

```xml
<!-- Required for language learning, reading, and social media -->
<uses-permission android:name="android.permission.PACKAGE_USAGE_STATS"
    tools:ignore="ProtectedPermissions" />

<!-- Required for movie watching (Letterboxd RSS) -->
<uses-permission android:name="android.permission.INTERNET" />
```

`PACKAGE_USAGE_STATS` is a protected permission — the user must grant it manually via **Settings → Apps → Special app access → Usage access**.

## Privacy

- All usage stats are processed entirely on-device
- Letterboxd data is fetched from public RSS feeds — no authentication, no private data
- No data is sent to any server beyond the third-party services you configure

## Requirements

- **Min SDK**: 21 (Android 5.0)
- **Target SDK**: 36
- **Kotlin**: 1.9+

## Sample App

```bash
./gradlew :app:installDebug
```

Demonstrates the full flow: permission request, selecting a time window (Today / 2 Days / 7 Days), querying all metrics (language learning, reading, social media, movie watching), and displaying results. To enable movie watching, set your Letterboxd username in `MainActivity.kt`.

## License

Apache 2.0 — see [LICENSE](LICENSE) for details.