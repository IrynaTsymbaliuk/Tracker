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
    .enableReading()
    .enableLanguageLearning()
    .enableSocialMedia()
    .enableMovieWatching()
    .setLetterboxdUsername("your_username")  // Optional: can also be set later
    .setMinConfidence(0.50f)
    .build()

lifecycleScope.launch {
    val reading = tracker.queryReading()
    val learning = tracker.queryLanguageLearning()
    val movies = tracker.queryMovieWatching()
    val social = tracker.querySocialMedia()

    // Reading
    reading?.occurred          // true if reading was detected
    reading?.durationMinutes   // total minutes across all sessions
    reading?.sessionCount      // number of distinct reading sessions
    reading?.confidence        // 0.0–1.0
    reading?.confidenceLevel   // LOW / MEDIUM / HIGH
    reading?.apps              // List<AppInfo> — apps that contributed

    // Language learning
    learning?.occurred
    learning?.durationMinutes
    learning?.sessionCount
    learning?.apps

    // Movie watching
    movies?.occurred
    movies?.count              // number of films logged
    movies?.movies             // List<MovieInfo> — title, watchedDate, publishedDate

    // Social media
    social?.occurred
    social?.durationMinutes
    social?.sessionCount       // number of distinct app-open sessions
    social?.apps               // List<AppInfo> — apps used
}
```

**Example output (last 24 hours):**
- Reading: 30 min · 2 sessions · Kindle · 75% confidence (MEDIUM)
- Language Learning: 45 min · 5 sessions · Duolingo, Anki · 85% confidence (HIGH)
- Movie Watching: 3 films · The Matrix, Inception, Interstellar · 95% confidence (HIGH)
- Social Media: 120 min · 23 sessions · Instagram, Reddit, WhatsApp · 88% confidence (HIGH)

## Metrics

| Metric | Source | Apps / Data | Permission |
|---|---|---|---|
| **LANGUAGE_LEARNING** | Foreground session events | Duolingo, Anki, LingoDeer, Drops, Kanji Study, Renshuu, and 8 more | `PACKAGE_USAGE_STATS` |
| **READING** | Foreground session events | Kindle, Google Play Books | `PACKAGE_USAGE_STATS` |
| **MOVIE_WATCHING** | Letterboxd RSS | Film titles and watch dates from public feed | `INTERNET` (no user prompt) |
| **SOCIAL_MEDIA_USAGE** | Foreground session events | Facebook, Instagram, Twitter/X, TikTok, Reddit, WhatsApp, and 9 more | `PACKAGE_USAGE_STATS` |

**Note on Social Media**: Includes messaging apps (WhatsApp, Telegram) with lower confidence scores as they may be used for work/family communication.

**Note on session accuracy**: On Android 10+ (API 29), session tracking uses `ACTIVITY_RESUMED`/`ACTIVITY_PAUSED` events for precise per-session start and end times. Consecutive activity transitions within the same app are merged into a single session if the gap between them is under 30 seconds.

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

Demonstrates the full flow: permission request, querying all metrics (language learning, reading, social media, movie watching), and displaying results. To enable movie watching, set your Letterboxd username in `MainActivity.kt`.

## License

Apache 2.0 — see [LICENSE](LICENSE) for details.