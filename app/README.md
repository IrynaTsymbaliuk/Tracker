# Tracker Library - Sample App

This sample application demonstrates how to integrate and use the **Tracker** library to monitor language learning and reading habits.

## Features Demonstrated

### 1. **Library Setup**
```kotlin
val tracker = Tracker.Builder(context)
    .setMinConfidence(0.50f)  // 50% confidence threshold
    .build()
```

### 2. **Permission Handling**
- Checks for `PACKAGE_USAGE_STATS` permission per-metric
- Guides user to grant permission via Settings
- Re-checks permission when returning from Settings

```kotlin
// Check access for specific metrics
if (!tracker.hasAllRequiredAccess(Metric.LANGUAGE_LEARNING)) {
    tracker.requestMissingAccess(this, Metric.LANGUAGE_LEARNING)
}
```

### 3. **Querying Metrics**
```kotlin
// Query each metric individually using coroutines
lifecycleScope.launch {
    val llResult = tracker.queryLanguageLearning()
    val readingResult = tracker.queryReading()
    displayResults(llResult, readingResult)
}
```

### 4. **Displaying Results**
The app shows results for the last 24 hours:

- **Language Learning Section**:
  - Activity status (detected or not)
  - Duration (in minutes)
  - Confidence score and level (HIGH/MEDIUM/LOW)
  - Apps used (by human-readable name, e.g., "Duolingo, Anki")

- **Reading Section**:
  - Activity status (detected or not)
  - Duration (in minutes)
  - Confidence score and level (HIGH/MEDIUM/LOW)
  - Apps used (e.g., "Kindle, Google Play Books")

- **Social Media Section**:
  - Activity status (detected or not)
  - Duration (in minutes)
  - Confidence score and level (HIGH/MEDIUM/LOW)
  - Apps used (e.g., "Instagram, Reddit, WhatsApp")

- **Movie Watching Section**:
  - Activity status (detected or not)
  - Number of films logged
  - Film titles

## How to Run

1. **Build the project**:
   ```bash
   ./gradlew :app:assembleDebug
   ```

2. **Install on device/emulator**:
   ```bash
   ./gradlew :app:installDebug
   ```

3. **Grant Permission**:
   - Open the app
   - Tap "Grant Permission"
   - Find "Tracker Demo" in the list
   - Enable "Permit usage access"

4. **View Results**:
   - Return to the app
   - Tap "Query Metrics"
   - View your language learning and reading activity from the last 24 hours

## Code Structure

```
MainActivity.kt
├── setupTracker()           # Build Tracker instance
├── checkPermissionAndQuery() # Permission check flow
├── queryMetrics()           # Query using coroutines
├── displayResults()         # Show results in UI
└── UI state management methods
```

## Key Learnings

1. **Builder Pattern**: Configure the Tracker with fluent API
2. **Coroutines**: Use suspend functions for modern async handling
3. **Permission Flow**: Proper permission request and handling
4. **Result Structure**: Access result data with confidence scores and session details
5. **Error Handling**: Handle permission denied and query errors

## Testing

The app works best when you have language learning or reading apps installed and have used them in the last 24 hours.

**Supported language learning apps:**
- Duolingo, Anki, LingoDeer, Drops
- Japanese learning apps: Kanji Study, Renshuu, J5a, Hey Japan
- And 5 more (see KnownApps.kt for the complete list)

**Supported reading apps:**
- Kindle, Google Play Books

The library will automatically detect and track usage of any installed supported apps.

## Notes

- The app requires Android API 26+ (Android 8.0+)
- `PACKAGE_USAGE_STATS` is a protected permission that requires user action
- Queries return data for the last 24 hours only
- Each metric can be queried independently with its own data quality information
