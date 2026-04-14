# Tracker — Sample App

This sample application demonstrates how to integrate and use the **Tracker** library to monitor language learning, reading, social media usage, movie watching, and step counting.

## Features Demonstrated

### 1. Library Setup

```kotlin
val tracker = Tracker.Builder(context)
    .setLetterboxdUsername("your_username")  // optional: required only for movie watching
    .build()
```

### 2. Permission Handling

The app handles two separate permissions:

**Usage Stats** (`PACKAGE_USAGE_STATS`) — required for language learning, reading, and social media:
```kotlin
// Check via AppOpsManager
val mode = appOps.checkOpNoThrow(
    AppOpsManager.OPSTR_GET_USAGE_STATS, Process.myUid(), packageName
)
// Direct the user to the system usage access settings screen
startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS))
```

**Health Connect** (`health.READ_STEPS`) — required for step counting:
```kotlin
// Request at runtime using the Health Connect permission contract
val launcher = registerForActivityResult(
    PermissionController.createRequestPermissionResultContract()
) { updateHcPermissionUi() }
launcher.launch(setOf(HealthPermission.getReadPermission(StepsRecord::class)))
```

Both permissions show inline status rows with a "Grant" button that appears only when the permission is missing.

### 3. Querying Metrics

All metrics are queried for today (midnight → now). Each query is wrapped independently so a failure in one does not affect the others:

```kotlin
lifecycleScope.launch {
    val language = runCatching { tracker.queryLanguageLearning() }.getOrNull()
    val reading  = runCatching { tracker.queryReading() }.getOrNull()
    val social   = runCatching { tracker.querySocialMedia() }.getOrNull()
    val movies   = runCatching { tracker.queryMovieWatching() }.getOrNull()
    val steps    = runCatching { tracker.queryStepCounting() }.getOrNull()
    displayResults(language, reading, social, movies, steps)
}
```

A `null` result means either no activity in the time range or a missing/denied permission.

### 4. Displaying Results

Each metric is shown as a single line:

- **Language**: `📚 Language    45 min · HIGH · 85%`
- **Reading**: `📖 Reading    30 min · MEDIUM · 75%`
- **Social**: `📱 Social    120 min · HIGH · 88%`
- **Movies**: `🎬 Movies    3 films · HIGH · 95%`
- **Steps**: `👣 Steps    7,622 steps · HIGH · 99%`

`—` is shown when the result is null (no data or permission not granted).

## How to Run

1. **Install on device**:
   ```bash
   ./gradlew :app:installDebug
   ```

2. **Grant Usage Access**:
   - Tap "Grant" next to "Usage access  ✗"
   - Find "Tracker Demo" in the list and enable "Permit usage access"
   - Return to the app — metrics update automatically

3. **Grant Health Connect** (optional, for step counting):
   - Tap "Grant" next to "Health Connect  ✗"
   - Approve the `Steps` permission in the Health Connect dialog
   - Return to the app — step count appears automatically

4. **Enable Movie Watching** (optional):
   - Set `letterboxdUsername` in `MainActivity.kt` to your Letterboxd username

5. **Refresh**:
   - Tap "Query Today" to manually refresh all metrics

## Code Structure

```
MainActivity.kt
├── onCreate()                  # Build Tracker, register HC permission launcher, wire buttons
├── onResume()                  # Update permission UI, auto-query metrics
├── updateUsagePermissionUi()   # Show/hide Grant button based on AppOps check
├── hasUsageStatsPermission()   # AppOpsManager permission check
├── updateHcPermissionUi()      # Show/hide Grant button based on HC permission state
├── queryMetrics()              # Query all metrics concurrently using coroutines
└── displayResults()            # Render one line per metric
```

## Key Learnings

1. **Builder Pattern**: Configure the Tracker with a fluent API — no `enable*` flags needed, all features are always available
2. **Coroutines**: All query methods are `suspend` functions; wrap each independently with `runCatching` so one failure doesn't block the rest
3. **Two permission flows**: `PACKAGE_USAGE_STATS` requires a manual system settings redirect; `health.READ_STEPS` uses the Health Connect runtime permission contract
4. **Health Connect manifest setup**: Both `ACTION_SHOW_PERMISSIONS_RATIONALE` (Android 9–13) and the `VIEW_PERMISSION_USAGE` activity-alias (Android 14+) must be declared for the permission dialog to work
5. **Step counting deduplication**: Use `queryStepCounting()` rather than summing `StepsRecord` entries manually — Health Connect's aggregation API handles deduplication across Google Fit, the phone step counter, and other sources

## Testing

The app works best when you have supported apps installed and have used them today.

**Supported language learning apps:**
- Duolingo, Anki, LingoDeer, Drops
- Japanese learning apps: Kanji Study, Renshuu, J5a, Hey Japan, JP News, Mytest, TenWords
- And 2 more (see `KnownApps.kt` for the complete list)

**Supported reading apps:**
- Kindle, Google Play Books

**Supported social media apps:**
- Facebook, Instagram, Twitter/X, TikTok, Snapchat
- Reddit, Pinterest, LinkedIn, Discord, Threads
- WhatsApp, Telegram, Mastodon, Bluesky, Tumblr

**Movie watching:**
- Set `letterboxdUsername` in `MainActivity.kt` to your Letterboxd username

**Step counting:**
- Requires Health Connect to be installed (built-in on Android 14+; available via Google Play on Android 9–13)
- Grant the `Steps` permission when prompted

## Notes

- The sample app requires Android API 26+ (Android 8.0)
- The library itself supports Android API 21+; the higher sample app minimum is due to Health Connect and `java.time` usage
- `PACKAGE_USAGE_STATS` is a protected permission that requires user action in system settings
- Queries always cover today from midnight in the device's local timezone
- On Android 10+, session times use precise `ACTIVITY_RESUMED`/`ACTIVITY_PAUSED` events