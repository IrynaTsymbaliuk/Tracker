# Tracker

**Detect user habits from Android system data and third-party services — no user input required.**

Tracker is an Android library that automatically identifies behaviors like language learning, reading, and movie watching by analyzing device usage and third-party feeds. Your app gets structured habit data with confidence scores, without asking users to log anything manually.

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
    .setLetterboxdUsername("your_username") // optional, for movie tracking
    .setMinConfidence(0.50f)
    .build()

lifecycleScope.launch {
    val reading = tracker.queryReading()
    val learning = tracker.queryLanguageLearning()
    val movies = tracker.queryMovieWatching()

    // Reading
    reading?.occurred          // true if reading was detected
    reading?.durationMinutes   // total minutes
    reading?.confidence        // 0.0–1.0
    reading?.confidenceLevel   // LOW / MEDIUM / HIGH
    reading?.apps              // List<AppInfo> — apps that contributed

    // Language learning
    learning?.occurred
    learning?.durationMinutes
    learning?.apps

    // Movie watching
    movies?.occurred
    movies?.count              // number of films logged
    movies?.movies             // List<MovieInfo> — title, watchedDate, publishedDate
}

// Cancel in-flight callback queries when the component is destroyed
override fun onDestroy() {
    super.onDestroy()
    tracker.cancel()
}
```

**Example output (last 24 hours):**
- Reading: 30 min · Kindle · 75% confidence (MEDIUM)
- Language Learning: 45 min · Duolingo, Anki · 85% confidence (HIGH)
- Movie Watching: 3 films · The Matrix, Inception, Interstellar · 95% confidence (HIGH)

## Metrics

| Metric | Source | Apps / Data | Permission |
|---|---|---|---|
| **LANGUAGE_LEARNING** | App usage stats | Duolingo, Anki, LingoDeer, Drops, Kanji Study, Renshuu, and 8 more | `PACKAGE_USAGE_STATS` |
| **READING** | App usage stats | Kindle, Google Play Books | `PACKAGE_USAGE_STATS` |
| **MOVIE_WATCHING** | Letterboxd RSS | Film titles and watch dates from public feed | `INTERNET` (no user prompt) |

## Installation

```gradle
dependencies {
    implementation 'com.tracker:core:2.0.0'
}
```

Add to `AndroidManifest.xml`:

```xml
<!-- Required for language learning and reading -->
<uses-permission android:name="android.permission.PACKAGE_USAGE_STATS"
    tools:ignore="ProtectedPermissions" />

<!-- Required for movie watching (Letterboxd RSS) -->
<uses-permission android:name="android.permission.INTERNET" />
```

`PACKAGE_USAGE_STATS` is a protected permission — the user must grant it manually via **Settings → Apps → Special app access → Usage access**.

## Callbacks

A callback API is available for Java interop or non-coroutine contexts:

```kotlin
tracker.queryReading { result ->
    println("Reading: ${result?.occurred}, ${result?.durationMinutes} min")
}

tracker.queryLanguageLearning { result ->
    println("Learning: ${result?.confidence}")
}

tracker.queryMovieWatching { result ->
    result?.movies?.forEach { println(it.title) }
}

// Always call cancel() to clean up when done
tracker.cancel()
```

Callbacks are invoked on the Main dispatcher.

## Privacy

- All usage stats are processed entirely on-device
- Letterboxd data is fetched from public RSS feeds — no authentication, no private data
- No data is sent to any server beyond the third-party services you configure

## Requirements

- **Min SDK**: 21 (Android 5.0)
- **Target SDK**: 34+
- **Kotlin**: 1.9+

## Sample App

```bash
./gradlew :app:installDebug
```

Demonstrates the full flow: permission request, querying all three metrics, and displaying results. To enable movie watching, set your Letterboxd username in `MainActivity.kt`.

## License

Apache 2.0 — see [LICENSE](LICENSE) for details.
