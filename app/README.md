# Tracker Library - Sample App

This sample application demonstrates how to integrate and use the **Tracker** library to monitor language learning, reading, social media usage, and movie watching habits.

## Features Demonstrated

### 1. **Library Setup**
```kotlin
val tracker = Tracker.Builder(context)
    .enableReading()
    .enableLanguageLearning()
    .enableSocialMedia()
    .enableMovieWatching()
    .setLetterboxdUsername("your_username")
    .setMinConfidence(0.50f)
    .build()
```

### 2. **Permission Handling**
- Checks for `PACKAGE_USAGE_STATS` permission on resume
- Guides user to grant it via Settings
- Re-checks permission when returning from Settings

```kotlin
// Direct the user to the system usage access settings screen
startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS))
```

### 3. **Querying Metrics**
```kotlin
lifecycleScope.launch {
    val learning = tracker.queryLanguageLearning()
    val reading = tracker.queryReading()
    val social = tracker.querySocialMedia()
    val movies = tracker.queryMovieWatching()
    displayResults(learning, reading, social, movies)
}
```

### 4. **Displaying Results**
The app shows results for the last 24 hours:

- **Language Learning Section**:
  - Activity status (detected or not)
  - Duration and session count (e.g. "45 min (5 sessions)")
  - Confidence score and level (HIGH/MEDIUM/LOW)
  - Apps used (by human-readable name, e.g., "Duolingo, Anki")

- **Reading Section**:
  - Activity status (detected or not)
  - Duration and session count (e.g. "30 min (2 sessions)")
  - Confidence score and level (HIGH/MEDIUM/LOW)
  - Apps used (e.g., "Kindle, Google Play Books")

- **Social Media Section**:
  - Activity status (detected or not)
  - Duration and session count (e.g. "120 min (23 sessions)")
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
   - Return to the app — metrics are queried automatically
   - Or tap "Query Metrics" to refresh
   - View activity from the last 24 hours

## Code Structure

```
MainActivity.kt
├── onCreate()               # Build Tracker instance, wire button listeners
├── onResume()               # Check permission, auto-query if granted
├── queryMetrics()           # Query all metrics using coroutines
├── displayResults()         # Show results in UI with session counts
└── hasUsageStatsPermission() # AppOpsManager permission check
```

## Key Learnings

1. **Builder Pattern**: Configure the Tracker with a fluent API
2. **Coroutines**: Use suspend functions for async metric queries
3. **Permission Flow**: Check on resume, redirect to system settings if missing
4. **Result Structure**: Access duration, session count, confidence score, and contributing apps
5. **Error Handling**: Catch exceptions for permission denied and query failures

## Testing

The app works best when you have supported apps installed and have used them in the last 24 hours.

**Supported language learning apps:**
- Duolingo, Anki, LingoDeer, Drops
- Japanese learning apps: Kanji Study, Renshuu, J5a, Hey Japan
- And 5 more (see KnownApps.kt for the complete list)

**Supported reading apps:**
- Kindle, Google Play Books

**Supported social media apps:**
- Facebook, Instagram, Twitter/X, TikTok, Snapchat
- Reddit, Pinterest, LinkedIn, Discord, Threads
- WhatsApp, Telegram, Mastodon, Bluesky, Tumblr

**Movie watching:**
- Set `letterboxdUsername` in `MainActivity.kt` to your Letterboxd username

## Notes

- The app requires Android API 21+ (Android 5.0)
- `PACKAGE_USAGE_STATS` is a protected permission that requires user action
- Queries return data for the last 24 hours only
- Each metric can be queried independently
- On Android 10+, session times use precise `ACTIVITY_RESUMED`/`ACTIVITY_PAUSED` events