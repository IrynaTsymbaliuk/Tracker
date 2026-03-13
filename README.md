# Tracker

**Detect user habits from Android system data — no user input required.**

Tracker is an Android library that automatically identifies user behaviors (like language learning and reading) by analyzing device usage patterns. Your app gets reliable habit data without asking users to manually track anything.

## Do You Need This?

- ✅ You're building a habit tracking, wellness, or productivity app
- ✅ You want to detect behaviors without manual logging
- ✅ You need confidence scores for detected activities
- ✅ You want graceful degradation when permissions are missing

- ❌ You just need basic step counting (use Health Connect directly)
- ❌ You want to track custom app-specific actions (use your own analytics)

## What It Does

```kotlin
// 1. Configure what to track
val tracker = Tracker.Builder(context)
    .requestMetrics(Metric.LANGUAGE_LEARNING, Metric.READING)
    .setMinConfidence(0.50f)  // 50% confidence threshold
    .build()

// 2. Request permissions (opens Settings)
if (!tracker.hasAllRequiredAccess()) {
    tracker.requestMissingAccess(this)
}

// 3. Get results for the last 24 hours
val result = tracker.queryAsync()

// Result contains:
result.languageLearning?.occurred              // Whether language learning was detected
result.languageLearning?.durationMinutes       // Total time spent
result.languageLearning?.confidence            // Confidence score (0.0-1.0)
result.languageLearning?.confidenceLevel       // LOW/MEDIUM/HIGH
result.languageLearning?.apps                  // Apps used

result.reading?.occurred                       // Whether reading was detected
result.reading?.durationMinutes                // Total time spent
result.reading?.confidence                     // Confidence score (0.0-1.0)
result.reading?.apps                           // Apps used

result.dataQuality                             // What data sources are available/missing
```

**Output example (last 24 hours):**
- **Language Learning**: "45 minutes detected with 85% confidence (HIGH)"
  - Apps: Duolingo, Anki
- **Reading**: "30 minutes detected with 75% confidence (MEDIUM)"
  - Apps: Kindle

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
    implementation 'com.tracker:core:2.0.0'
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

### Adjust Confidence Threshold

```kotlin
val tracker = Tracker.Builder(context)
    .requestMetrics(Metric.LANGUAGE_LEARNING, Metric.READING)
    .setMinConfidence(0.70f)  // Only return HIGH confidence results
    .build()
```

### Check Detected Apps

```kotlin
val result = tracker.queryAsync()

// Language learning apps
result.languageLearning?.apps?.forEach { app ->
    println("${app.appName}: ${app.durationMinutes} minutes")
}

// Reading apps
result.reading?.apps?.forEach { app ->
    println("${app.appName}: ${app.durationMinutes} minutes")
}
```

## How It Works

1. **Data Collection**: Queries Android system APIs (UsageStatsManager, Health Connect, etc.)
2. **Evidence Gathering**: Collects timestamped evidence from the last 24 hours
3. **Smart Aggregation**: Removes duplicates, calculates confidence scores
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
- Displaying activity results for the last 24 hours
- Showing confidence scores and detected apps
- Data quality information

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
