# Tracker

An Android library for automatically detecting and tracking user habits through device usage analysis.

## Overview

**Tracker** is a privacy-conscious Android library that passively monitors device usage to detect habit patterns. Currently focused on **language learning habit tracking**, it analyzes when users engage with known language learning apps and provides confidence-scored insights about their learning behavior.

Rather than requiring manual logging, Tracker leverages Android's UsageStats API to automatically detect habit occurrences, calculate confidence levels, and provide rich analytics about user behavior patterns.

## Features

- **Automatic Habit Detection** - Passive tracking without manual user input
- **Confidence Scoring** - Probabilistic confidence levels (HIGH/MEDIUM/LOW) for each detected habit
- **Smart Aggregation**
  - Deduplicates overlapping app usage sessions
  - Combines multiple signals using probability mathematics
  - Applies penalties for weak-only evidence
- **Data Quality Reporting** - Transparent information about missing permissions and data sources
- **Privacy-Conscious** - Only checks permissions, never requests them automatically
- **Flexible Time Ranges** - Query any date range or use default lookback periods
- **Rich Metrics**
  - Daily occurrence detection
  - Duration tracking (total time spent)
  - Summary statistics (total days engaged, average duration)
  - App-level details (which specific apps were used)
- **Coroutine Support** - Full async/await support with Kotlin coroutines
- **Extensible Architecture** - Plugin-like collectors and aggregators designed for future metrics

## Supported Metrics

### Language Learning
Tracks usage of 13 language learning apps (with focus on Japanese language learning):
- **Duolingo** - Popular multi-language learning platform
- **Anki** - Spaced repetition flashcard app
- **LingoDeer** - Grammar-focused language learning
- **Drops** - Visual vocabulary learning (2 variants)
- **Kanji Study** - Japanese kanji learning
- **Renshuu** - Japanese learning platform
- **J5a** - Japanese learning app
- **J5KjAnd** - Japanese kanji and vocabulary
- **MyTest** - Language testing app
- **Hey Japan** - Japanese language learning
- **Ten Words** - Vocabulary learning
- **JP News** - Japanese news reading

Each app has configurable confidence multipliers (0.75-0.85) and minimum session duration thresholds (5-10 minutes) to ensure accurate habit detection.

## Installation

[![](https://jitpack.io/v/IrynaTsymbaliuk/Tracker.svg)](https://jitpack.io/#IrynaTsymbaliuk/Tracker)

### Step 1: Add JitPack Repository

Add the JitPack repository to your project's `settings.gradle.kts`:

```kotlin
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        maven { url = uri("https://jitpack.io") }  // Add this line
    }
}
```

Or if using the older project-level `build.gradle.kts`:

```kotlin
allprojects {
    repositories {
        google()
        mavenCentral()
        maven { url = uri("https://jitpack.io") }  // Add this line
    }
}
```

### Step 2: Add Dependency

Add the library to your app module's `build.gradle.kts`:

```kotlin
dependencies {
    implementation("com.github.IrynaTsymbaliuk:Tracker:1.0.0")
}
```

Or with Groovy DSL:

```groovy
dependencies {
    implementation 'com.github.IrynaTsymbaliuk:Tracker:1.0.0'
}
```

## Requirements

- **Min SDK**: Android 5.0 (API 21)
- **Target SDK**: Android 14+ (API 36)
- **Permission**: `android.permission.PACKAGE_USAGE_STATS`

## Permissions

### Required Permission

Add to your `AndroidManifest.xml`:

```xml
<uses-permission android:name="android.permission.PACKAGE_USAGE_STATS"
    tools:ignore="ProtectedPermissions" />
```

### Requesting Permission

The library **does not** automatically request permissions. Your app must guide users to enable the permission:

```kotlin
if (!tracker.hasRequiredPermissions()) {
    // Guide user to settings
    val intent = Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)
    startActivity(intent)
}
```

**Note**: This is a special permission that requires users to manually enable it in Android Settings > Apps > Special app access > Usage access.

## Usage

### Basic Setup

```kotlin
import com.tracker.core.Tracker
import com.tracker.core.types.Metric

// Build the tracker
val tracker = Tracker.Builder(context)
    .requestMetrics(Metric.LANGUAGE_LEARNING)
    .setLookbackDays(30)              // Optional: default is 30 days
    .setMinConfidence(0.50f)          // Optional: default is 0.50
    .build()
```

### Async Query (Coroutines)

```kotlin
// In a coroutine scope
launch {
    // Query with default lookback period
    val result = tracker.queryAsync()

    // Or query a custom time range
    val startTime = System.currentTimeMillis() - (7 * 24 * 60 * 60 * 1000L) // 7 days ago
    val endTime = System.currentTimeMillis()
    val customResult = tracker.queryAsync(startTime, endTime)

    // Process results
    result.days.forEach { day ->
        day.languageLearning?.let { learning ->
            println("Date: ${day.date}")
            println("Occurred: ${learning.occurred}")
            println("Confidence: ${learning.confidence} (${learning.confidenceLevel})")
            println("Duration: ${learning.durationMinutes} minutes")
            println("Apps used: ${learning.apps.joinToString(", ") { it.appName }}")
        }
    }
}
```

### Callback-based Query

```kotlin
val startTime = System.currentTimeMillis() - (30 * 24 * 60 * 60 * 1000L)
val endTime = System.currentTimeMillis()

tracker.query(startTime, endTime) { result ->
    // Summary statistics
    println("Total days with language learning: ${result.summary.languageLearning?.totalDays}")
    println("Average duration: ${result.summary.languageLearning?.averageDurationMinutes} minutes")

    // Daily breakdown
    result.days.forEach { day ->
        day.languageLearning?.let {
            println("${day.date}: ${it.occurred} (${it.confidence})")
        }
    }

    // Data quality
    println("Data reliability: ${result.dataQuality.reliability}")
    if (!result.dataQuality.hasAllSources) {
        println("Missing sources: ${result.dataQuality.missingSources}")
    }
}
```

### Checking Permissions

```kotlin
// Check if required permissions are granted
if (tracker.hasRequiredPermissions()) {
    // Proceed with queries
} else {
    // Guide user to enable permission
    println("Missing permissions: ${tracker.getMissingPermissions()}")
}
```

## Sample App

A complete sample application is included in the `app/` module to demonstrate how to integrate and use the Tracker library.

### Features

The sample app demonstrates:
- **Library setup** using the Builder pattern
- **Permission handling** for PACKAGE_USAGE_STATS
- **Querying metrics** with Kotlin coroutines
- **Displaying results** with Material Design UI
- **Data quality reporting** and recommendations

### Screenshots
<img width=300 src="https://github.com/user-attachments/assets/39275b57-5110-440d-aa87-b204b4b52b68">

### Running the Sample App

1. **Build and install**:
   ```bash
   ./gradlew :app:assembleDebug
   ./gradlew :app:installDebug
   ```

2. **Grant permission**:
   - Open the app
   - Tap "Grant Permission"
   - Find "Tracker Demo" in the list
   - Enable "Permit usage access"

3. **View results**:
   - Return to the app
   - Tap "Query Last 30 Days"
   - View language learning statistics

The app displays three main sections:
- **Summary Statistics**: Total days tracked, language learning days, average minutes per day
- **Data Quality**: Reliability level, available/missing data sources, and recommendations
- **Day-by-Day Results**: Detailed breakdown showing date, duration, confidence level, and apps used

### What You'll See

The sample app shows:
- Which language learning apps you've used (by name, e.g., "Duolingo", "Anki")
- How long you spent on each day
- Confidence scores and levels (HIGH/MEDIUM/LOW)
- Data quality and reliability information
- Helpful recommendations if permissions are missing

### Code Example

See the complete implementation in [`MainActivity.kt`](app/src/main/java/com/tracker/MainActivity.kt) for a full example of:
- Building the Tracker instance
- Checking and requesting permissions
- Querying metrics asynchronously
- Displaying results in a user-friendly format

For more details, see the [Sample App README](app/README.md).

## API Reference

### Tracker.Builder

Fluent builder for configuring the Tracker instance.

#### Methods

| Method | Description | Default |
|--------|-------------|---------|
| `requestMetrics(Metric)` | Specify which metric to track (required) | - |
| `setLookbackDays(Int)` | Set default lookback period (1-365 days) | 30 |
| `setMinConfidence(Float)` | Set minimum confidence threshold (0.0-1.0) | 0.50 |
| `build()` | Create the Tracker instance | - |

### Tracker

Main API for querying habit data.

#### Methods

| Method | Return Type | Description |
|--------|-------------|-------------|
| `queryAsync()` | `MetricsResult` | Query with default lookback period (suspend) |
| `queryAsync(fromMillis, toMillis)` | `MetricsResult` | Query custom time range (suspend) |
| `query(fromMillis, toMillis, callback)` | `Unit` | Callback-based query |
| `hasRequiredPermissions()` | `Boolean` | Check if all permissions granted |
| `getMissingPermissions()` | `List<Permission>` | Get list of missing permissions |

### MetricsResult

Top-level result containing all habit data.

```kotlin
data class MetricsResult(
    val days: List<DayResult>,           // Daily results
    val summary: SummaryStatistics,       // Aggregate statistics
    val dataQuality: DataQuality          // Data availability info
)
```

### DayResult

Results for a single day.

```kotlin
data class DayResult(
    val date: LocalDate,                  // The date
    val languageLearning: LanguageLearningResult?  // Language learning data (null if not occurred)
)
```

### LanguageLearningResult

Detailed language learning habit data.

```kotlin
data class LanguageLearningResult(
    val occurred: Boolean,                // Did the habit occur?
    val confidence: Float,                // Confidence score (0.0-1.0)
    val confidenceLevel: ConfidenceLevel, // HIGH/MEDIUM/LOW
    val durationMinutes: Int?,            // Total duration in minutes (null if none)
    val apps: List<AppInfo>               // Apps used (package name + display name)
)

data class AppInfo(
    val packageName: String,              // App package identifier (e.g., "com.duolingo")
    val appName: String                   // Human-readable name (e.g., "Duolingo")
)
```

### ConfidenceLevel

Enum representing confidence tiers.

| Level | Range |
|-------|-------|
| `HIGH` | ≥ 0.75 |
| `MEDIUM` | 0.50 - 0.74 |
| `LOW` | < 0.50 |

### DataQuality

Information about data availability and reliability.

```kotlin
data class DataQuality(
    val hasAllSources: Boolean,           // Are all data sources available?
    val missingSources: Set<DataSource>,  // Which sources are missing?
    val reliability: String               // "HIGH", "MEDIUM", or "LOW"
)
```

## How It Works

### 1. Data Collection
The `UsageStatsCollector` queries Android's UsageStatsManager to retrieve app usage events for language learning apps. Each usage session becomes an `Evidence` object with:
- Timestamp range
- Duration
- Base confidence score (from app metadata)
- App package name

### 2. Smart Aggregation
The `LanguageLearningAggregator` processes evidence for each day:

**Deduplication**: Overlapping sessions from the *same app* (>80% overlap) are deduplicated to avoid double-counting. Different apps are never considered duplicates, even with overlapping times.

**Confidence Combination**: Multiple pieces of evidence are combined using probability mathematics:
```
combined_confidence = 1 - ∏(1 - confidence_i)
```

**Weak Evidence Penalty**: If all evidence has confidence < 0.50, a 0.15 penalty is applied to prevent false positives.

**Duration Summation**: All session durations are summed for total daily time.

**App Tracking**: Both package names and human-readable app names are preserved for display.

### 3. Threshold Application
Results below the minimum confidence threshold (default 0.50) are filtered out and marked as non-occurrences.

### 4. Summary Statistics
Summary stats are calculated across all days:
- Total days with habit occurrence
- Average duration per occurrence
- Total time spent across all days

## Configuration

### App Metadata

Each tracked app has configurable metadata in `KnownApps`:

```kotlin
AppMetadata(
    packageName = "com.duolingo",
    displayName = "Duolingo",
    confidenceMultiplier = 0.85f,        // Higher = more confident signal
    minSessionDurationMinutes = 5        // Minimum session length to count
)
```

This allows fine-tuning of detection sensitivity for different apps.

## Error Handling

The library uses graceful degradation:

- **Missing Permissions**: Returns results with `dataQuality.reliability = "LOW"` and marks missing sources
- **No Apps Installed**: Returns zero occurrences (not an error)
- **Invalid Time Ranges**: Throws `IllegalArgumentException` during query
- **Invalid Configuration**: Throws `IllegalArgumentException` during build

## Best Practices

1. **Check Permissions First**: Always verify permissions before querying
2. **Use Appropriate Time Ranges**: Longer ranges consume more memory; consider pagination for large datasets
3. **Respect Privacy**: Inform users about what data you're tracking and why
4. **Handle Missing Data**: Check `dataQuality` to understand result reliability
5. **Use Coroutines**: Prefer `queryAsync()` over callbacks for cleaner async code
6. **Set Appropriate Thresholds**: Adjust `minConfidence` based on your use case (stricter = fewer false positives)

## Example: Full Integration

```kotlin
class HabitTrackingViewModel(context: Context) : ViewModel() {
    private val tracker = Tracker.Builder(context)
        .requestMetrics(Metric.LANGUAGE_LEARNING)
        .setLookbackDays(30)
        .setMinConfidence(0.60f)  // Slightly stricter threshold
        .build()

    private val _habitData = MutableLiveData<MetricsResult>()
    val habitData: LiveData<MetricsResult> = _habitData

    fun loadHabitData() {
        viewModelScope.launch {
            if (!tracker.hasRequiredPermissions()) {
                // Handle missing permissions
                _error.postValue("Usage access permission required")
                return@launch
            }

            try {
                val result = tracker.queryAsync()
                _habitData.postValue(result)

                // Check data quality
                if (result.dataQuality.reliability == "LOW") {
                    _warning.postValue("Limited data available")
                }
            } catch (e: Exception) {
                _error.postValue("Failed to load habit data: ${e.message}")
            }
        }
    }
}
```

## Future Metrics

The library is designed to support additional metrics in the future:
- Workout/Exercise habits
- Sleep patterns
- Reading habits
- Meditation/Mindfulness
- And more...

The extensible architecture allows easy addition of new `Collector` and `Aggregator` implementations.

## Testing

The library includes comprehensive test coverage:
- Unit tests for all core components
- Robolectric tests for Android-specific functionality
- MockK for mocking dependencies
- Coroutine testing utilities

Run tests:
```bash
./gradlew test
```

## Requirements & Compatibility

| Component | Version |
|-----------|---------|
| Min SDK | 21 (Android 5.0) |
| Target SDK | 36 (Android 14+) |
| Kotlin | 2.0.21+ |
| Coroutines | 1.7.3+ |

### Library Dependencies

The core library has only one dependency:
- `kotlinx-coroutines-android:1.7.3`

✅ **No AndroidX dependencies!**
✅ **No UI libraries!**
✅ **Ultra-lightweight and focused on functionality**

## Changelog

See [CHANGELOG.md](CHANGELOG.md) for a detailed history of changes and releases.

**Current Version:** 1.0.0 (Released: March 9, 2026)

## License

This project is licensed under the Apache License 2.0 - see the [LICENSE](LICENSE) file for details.

## Support

For issues, questions, or feature requests, please [open an issue](link-to-issues) on the project repository.

---

**Privacy Notice**: This library accesses app usage statistics. Always inform users about data collection and respect their privacy preferences.

**Copyright Notice**: Copyright 2026 Iryna Tsymbaliuk. Licensed under the Apache License, Version 2.0.
