# Tracker

**Detect user habits from Android system data — no user input required.**

Tracker is an Android library that automatically identifies user behaviors (like language learning and reading) by analyzing device usage patterns. Your app gets reliable habit data without asking users to manually track anything.

## Do You Need This?

✅ You're building a habit tracking, wellness, or productivity app
✅ You want to detect behaviors without manual logging
✅ You need confidence scores for detected activities
✅ You want graceful degradation when permissions are missing

❌ You just need basic step counting (use Health Connect directly)
❌ You want to track custom app-specific actions (use your own analytics)

## What It Does

```kotlin
// 1. Configure what to track
val tracker = Tracker.Builder(context)
    .requestMetrics(Metric.LANGUAGE_LEARNING, Metric.READING)
    .setLookbackDays(30)
    .build()

// 2. Request permissions (opens Settings)
if (!tracker.hasAllRequiredAccess()) {
    tracker.requestMissingAccess(this)
}

// 3. Get results
val result = tracker.queryAsync()

// Result contains:
result.summary.languageLearningDays            // How many days user studied
result.summary.averageLanguageLearningMinutes  // Average language learning time per day
result.summary.readingDays                     // How many days user read
result.summary.averageReadingMinutes           // Average reading time per day
result.days                                    // Day-by-day breakdown with confidence scores
result.dataQuality                             // What data sources are available/missing
```

**Output example:**
- **Language Learning**: "15 out of 30 days detected, average 25.5 minutes/day (HIGH confidence)"
  - Apps: Duolingo, Anki, LingoDeer
- **Reading**: "12 out of 30 days detected, average 18.3 minutes/day (HIGH confidence)"
  - Apps: Kindle, Google Play Books

## Supported Metrics

| Metric | Data Source | Detected Apps | Permission Required |
|--------|-------------|---------------|---------------------|
| **LANGUAGE_LEARNING** | App usage stats | Duolingo, Anki, LingoDeer, Drops, Kanji Study, and 8 more | PACKAGE_USAGE_STATS |
| **READING** | App usage stats | Kindle, Google Play Books | PACKAGE_USAGE_STATS |

*More metrics coming: Exercise (Health Connect), Sleep, Social Activity*

## Installation

Add to your `build.gradle`:

```gradle
dependencies {
    implementation 'com.tracker:core:1.0.0'
}
```

Add permission to `AndroidManifest.xml`:

```xml
<uses-permission android:name="android.permission.PACKAGE_USAGE_STATS"
    tools:ignore="ProtectedPermissions" />
```

## Key Features

### 🎯 **Confidence Scores**
Every detection includes a confidence level (LOW/MEDIUM/HIGH) based on data quality and evidence strength.

### 🔍 **Transparency**
See exactly what data sources are available, missing, and how they affect reliability:

```kotlin
val accessInfo = tracker.getAccessRequirements()
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

### Custom Time Ranges

```kotlin
val oneWeekAgo = System.currentTimeMillis() - (7 * 24 * 60 * 60 * 1000L)
val now = System.currentTimeMillis()
val result = tracker.queryAsync(oneWeekAgo, now)
```

### Adjust Confidence Threshold

```kotlin
val tracker = Tracker.Builder(context)
    .requestMetrics(Metric.LANGUAGE_LEARNING, Metric.READING)
    .setMinConfidence(0.70f)  // Only return HIGH confidence results
    .build()
```

### Day-by-Day Analysis

```kotlin
result.days.forEach { day ->
    val date = SimpleDateFormat("MMM dd, yyyy").format(Date(day.timestampMillis))

    // Language learning activity
    day.languageLearning?.let { ll ->
        if (ll.occurred) {
            println("$date - Language Learning: ${ll.durationMinutes} min (${ll.confidenceLevel})")
            ll.apps.forEach { app ->
                println("  - ${app.appName}")
            }
        }
    }

    // Reading activity
    day.reading?.let { reading ->
        if (reading.occurred) {
            println("$date - Reading: ${reading.durationMinutes} min (${reading.confidenceLevel})")
            reading.apps.forEach { app ->
                println("  - ${app.appName}")
            }
        }
    }
}
```

## How It Works

1. **Data Collection**: Queries Android system APIs (UsageStatsManager, Health Connect, etc.)
2. **Evidence Gathering**: Collects timestamped evidence from multiple sources
3. **Smart Aggregation**: Groups evidence by day, removes duplicates, calculates confidence
4. **Result Building**: Returns structured data with quality metrics

**Privacy**: All processing happens on-device. No data leaves the user's phone.

## Architecture

```
Tracker (public API)
  └─> HabitEngine (orchestration)
       ├─> Collectors (gather evidence from system APIs)
       ├─> Aggregators (process evidence into habits)
       └─> PermissionManager (check/request access)
```

- **Collectors** are self-describing (declare their own requirements)
- **Aggregators** handle metric-specific logic (confidence scoring, deduplication)
- **PermissionManager** provides unified access checking across all sources

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
- Querying multiple metrics (Language Learning + Reading)
- Displaying summary statistics with decimal averages
- Day-by-day breakdown showing both habits
- Confidence scores and app detection

## Contributing

See [CONTRIBUTING.md](CONTRIBUTING.md) for guidelines.

## License

Apache 2.0 — see [LICENSE](LICENSE) for details.

## Roadmap

- [ ] Health Connect integration (exercise, sleep)
- [ ] OAuth support for third-party APIs (Goodreads, Kindle API, Trakt, etc.)
- [ ] Additional metrics (Social Activity, Screen Time, Focus Time)
- [ ] Custom habit definitions via DSL
- [ ] ML-based confidence scoring

---

**Questions?** Open an issue or check the [documentation](docs/).
