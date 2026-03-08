# Tracker Library - Sample App

This sample application demonstrates how to integrate and use the **Tracker** library to monitor language learning habits.

## Features Demonstrated

### 1. **Library Setup**
```kotlin
val tracker = Tracker.Builder(context)
    .requestMetrics(Metric.LANGUAGE_LEARNING)
    .setLookbackDays(30)  // Query last 30 days
    .setMinConfidence(0.50f)  // 50% confidence threshold
    .build()
```

### 2. **Permission Handling**
- Checks for `PACKAGE_USAGE_STATS` permission
- Guides user to grant permission via Settings
- Re-checks permission when returning from Settings

### 3. **Querying Metrics**
```kotlin
// Using coroutines (recommended)
lifecycleScope.launch {
    val result = tracker.queryAsync()
    displayResults(result)
}

// Using callbacks (alternative)
tracker.query { result ->
    displayResults(result)
}
```

### 4. **Displaying Results**
The app shows three main sections:

- **Summary Statistics**: Total days, language learning days, average minutes
- **Data Quality**: Reliability level, available/missing sources, recommendations
- **Day-by-Day Results**: Detailed breakdown of each day's activity
  - Date
  - Duration (in minutes)
  - Confidence score and level (HIGH/MEDIUM/LOW)
  - Apps used (by human-readable name, e.g., "Duolingo, Anki")

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
   - Tap "Query Last 30 Days"
   - View your language learning statistics

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
2. **Coroutines**: Use `queryAsync()` for modern async handling
3. **Permission Flow**: Proper permission request and handling
4. **Result Structure**: Access summary, days, and data quality
5. **Error Handling**: Handle permission denied and query errors

## Testing

The app works best when you have language learning apps installed and have used them in the past 30 days. Supported apps include:
- Duolingo, Anki, LingoDeer, Drops
- Japanese learning apps: Kanji Study, Renshuu, J5a, Hey Japan
- And 5 more (see KnownApps.kt for the complete list)

The library will automatically detect and track usage of any installed supported apps.

## Notes

- The app requires Android API 26+ (Android 8.0+)
- `PACKAGE_USAGE_STATS` is a protected permission that requires user action
- The library uses sparse data structure - only days with activity are returned
- Average calculation: total minutes / total days in range (not just active days)
