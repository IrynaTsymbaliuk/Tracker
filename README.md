# Tracker

**Detect user habits from Android system data and third-party services — no user input required.**

Tracker is an Android library that automatically identifies user behaviors (like language learning, reading, and movie watching) by analyzing device usage patterns and third-party RSS feeds. Your app gets reliable habit data without asking users to manually track anything.

## Do You Need This?

- ✅ You're building a habit tracking, wellness, or productivity app
- ✅ You want to detect behaviors without manual logging
- ✅ You need confidence scores for detected activities
- ✅ You want graceful degradation when permissions are missing

- ❌ You just need basic step counting (use Health Connect directly)
- ❌ You want to track custom app-specific actions (use your own analytics)

## What It Does

```kotlin
// 1. Build tracker
val tracker = Tracker.Builder(context)
    .setMinConfidence(0.50f)  // 50% confidence threshold
    .setLetterboxdUsername("your_username")  // Optional: for movie watching tracking
    .build()

// 2. Query individual metrics for the last 24 hours
val llResult = tracker.queryLanguageLearning()
val readingResult = tracker.queryReading()
val movieResult = tracker.queryMovieWatching()

// Language Learning result:
llResult?.occurred              // Whether language learning was detected
llResult?.durationMinutes       // Total time spent
llResult?.confidence            // Confidence score (0.0-1.0)
llResult?.confidenceLevel       // LOW/MEDIUM/HIGH
llResult?.apps                  // Apps used (List<AppInfo>)

// Reading result:
readingResult?.occurred         // Whether reading was detected
readingResult?.durationMinutes  // Total time spent
readingResult?.confidence       // Confidence score (0.0-1.0)
readingResult?.apps             // Apps used (List<AppInfo>)

// Movie Watching result:
movieResult?.occurred           // Whether movies were watched
movieResult?.count              // Number of movies watched
movieResult?.confidence         // Confidence score (0.0-1.0)
movieResult?.movies             // Movies watched (List<MovieInfo>)
```

**Output example (last 24 hours):**
- **Language Learning**: "45 minutes detected with 85% confidence (HIGH)"
  - Apps: Duolingo, Anki
- **Reading**: "30 minutes detected with 75% confidence (MEDIUM)"
  - Apps: Kindle
- **Movie Watching**: "3 movies watched with 95% confidence (HIGH)"
  - Movies: The Matrix, Inception, Interstellar

## Supported Metrics

| Metric | Data Source | Detected Apps/Data | Permission Required |
|--------|-------------|-------------------|---------------------|
| **LANGUAGE_LEARNING** | App usage stats | Duolingo, Anki, LingoDeer, Drops, Kanji Study, and 8 more | PACKAGE_USAGE_STATS |
| **READING** | App usage stats | Kindle, Google Play Books | PACKAGE_USAGE_STATS |
| **MOVIE_WATCHING** | Letterboxd RSS | Movie titles and watch dates from public RSS feed | INTERNET (no user permission required) |

*More metrics coming: Exercise (Health Connect), Sleep, Social Activity*

## Installation

Add to your `build.gradle`:

```gradle
dependencies {
    implementation 'com.tracker:core:2.0.0'
}
```

Add permissions to `AndroidManifest.xml`:

```xml
<!-- Required for language learning and reading tracking -->
<uses-permission android:name="android.permission.PACKAGE_USAGE_STATS"
    tools:ignore="ProtectedPermissions" />

<!-- Required for movie watching tracking (Letterboxd RSS) -->
<uses-permission android:name="android.permission.INTERNET" />
```

## Key Features

### 🎯 **Confidence Scores**
Every detection includes a confidence level (LOW/MEDIUM/HIGH) based on data quality and evidence strength.

### 🔍 **Transparency**
See exactly what data sources are available, missing, and how they affect reliability:

```kotlin
val accessInfo = tracker.getAccessRequirements(Metric.LANGUAGE_LEARNING)
// Shows: which permissions are granted, what's missing, recommendations
```

### 📊 **Data Quality Tracking**
Results include quality metrics:
- Which data sources were used
- What's missing and why
- Recommendations to improve accuracy

### 🏗️ **Extensible Architecture**
Built with a self-describing collector pattern. Adding new metrics or data sources requires minimal code changes.

### ⚡ **Coroutines First**
Async by default with Kotlin coroutines, plus callback API for Java/legacy code.

## Advanced Usage

### Adjust Confidence Threshold

```kotlin
val tracker = Tracker.Builder(context)
    .setMinConfidence(0.70f)  // Only return HIGH confidence results
    .build()
```

### Check Detected Apps and Movies

```kotlin
val llResult = tracker.queryLanguageLearning()
val readingResult = tracker.queryReading()
val movieResult = tracker.queryMovieWatching()

// Language learning apps
llResult?.apps?.forEach { app ->
    println("${app.appName}: ${app.durationMinutes} minutes")
}

// Reading apps
readingResult?.apps?.forEach { app ->
    println("${app.appName}: ${app.durationMinutes} minutes")
}

// Movies watched
movieResult?.movies?.forEach { movie ->
    println("${movie.title} watched on ${movie.watchedDate}")
}
```

### Using Callbacks Instead of Coroutines

```kotlin
// Query with callback (Java-friendly)
tracker.queryLanguageLearning { result ->
    println("Language learning: ${result?.occurred}")
}

tracker.queryMovieWatching { result ->
    println("Movies watched: ${result?.count}")
}
```

## How It Works

1. **Data Collection**: Queries Android system APIs (UsageStatsManager) and third-party RSS feeds (Letterboxd)
2. **Evidence Gathering**: Collects timestamped evidence from the last 24 hours
3. **Smart Aggregation**: Filters invalid sessions (duration ≤ 0), calculates confidence scores
4. **Result Building**: Returns structured data with confidence metrics

**Privacy**:
- System data processing happens entirely on-device
- Letterboxd data is fetched from public RSS feeds (requires INTERNET permission)
- No data is sent to any servers beyond the third-party services you configure

## Architecture

```
Tracker (public API)
  └─> HabitEngine (orchestration)
       └─> MetricProviders (one per metric)
            ├─> LanguageLearningProvider (UsageStats collection + aggregation)
            ├─> ReadingProvider (UsageStats collection + aggregation)
            └─> MovieWatchingProvider (Letterboxd RSS parsing)
```

- **MetricProviders** encapsulate all logic for their metric (collection, aggregation, confidence scoring)
- Each provider filters invalid data (e.g., sessions with duration ≤ 0)
- Providers return null when no valid data is available
- Network operations run on IO dispatcher for non-blocking execution

## Requirements

- **Minimum SDK**: 26 (Android 8.0)
- **Target SDK**: 34+
- **Language**: Kotlin 1.9+

## Sample App

Run the included sample app to see the library in action:

```bash
./gradlew :app:installDebug
```

The sample demonstrates:
- Permission request flow
- Querying multiple metrics (Language Learning, Reading, Movie Watching)
- Displaying activity results for the last 24 hours
- Showing confidence scores and detected apps/movies
- Handling null values and missing data gracefully

**Note:** To enable movie watching in the sample app, set your Letterboxd username in `MainActivity.kt`

## Contributing

See [CONTRIBUTING.md](CONTRIBUTING.md) for guidelines.

## License

Apache 2.0 — see [LICENSE](LICENSE) for details.

## Roadmap

- [ ] Health Connect integration (exercise, sleep)
- [ ] OAuth support for third-party APIs (Goodreads, Strava, etc.)
- [ ] Additional metrics (Social Activity, Screen Time)
- [ ] Support for more movie tracking services (Trakt, IMDb)
