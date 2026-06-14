# Tracker — Sample App

This sample application demonstrates how to integrate and use the **Tracker** library to monitor language learning, reading, social media usage, movie watching, step counting, distance, meditation, and exercise.

## Features Demonstrated

### 1. Library Setup

```kotlin
val tracker = Tracker.Builder(context)
    .setLetterboxdUsername("your_username")  // optional: required only for movie watching
    .build()
```

### 2. Permission Handling

The app handles two separate permissions:

**Usage Stats** (`PACKAGE_USAGE_STATS`) — required for language learning, reading, social media, and the meditation-app-foreground branch:
```kotlin
// Check via AppOpsManager
val mode = appOps.checkOpNoThrow(
    AppOpsManager.OPSTR_GET_USAGE_STATS, Process.myUid(), packageName
)
// Direct the user to the system usage access settings screen
startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS))
```

**Health Connect** — required for step counting (`health.READ_STEPS`), distance (`health.READ_DISTANCE`), the HealthConnect branch of meditation (`health.READ_MINDFULNESS`), and exercise (`health.READ_EXERCISE`). All four are requested in a single runtime prompt:
```kotlin
val launcher = registerForActivityResult(
    PermissionController.createRequestPermissionResultContract()
) { updateHcPermissionUi() }

launcher.launch(setOf(
    HealthPermission.getReadPermission(StepsRecord::class),
    HealthPermission.getReadPermission(MindfulnessSessionRecord::class),
    HealthPermission.getReadPermission(ExerciseSessionRecord::class),
    HealthPermission.getReadPermission(DistanceRecord::class)
))
```

Both permissions show inline status rows with a "Grant" button that appears only when a permission is missing. The Health Connect status row distinguishes between "none granted" (`✗`), "partial" (`◐` with the list of missing items), and "all granted" (`✓`).

### 3. Querying Metrics

All metrics are queried for today (midnight → now). Each query is wrapped independently so a failure in one does not affect the others:

```kotlin
lifecycleScope.launch {
    val language   = runCatching { tracker.queryLanguageLearning() }.getOrNull()
    val reading    = runCatching { tracker.queryReading() }.getOrNull()
    val social     = runCatching { tracker.querySocialMedia() }.getOrNull()
    val movies     = runCatching { tracker.queryMovieWatching() }.getOrNull()
    val steps      = runCatching { tracker.queryStepCounting() }.getOrNull()
    val distance   = runCatching { tracker.queryDistance() }.getOrNull()
    val meditation = runCatching { tracker.queryMeditation() }.getOrNull()
    val exercise   = runCatching { tracker.queryExercise() }.getOrNull()
    displayResults(language, reading, social, movies, steps, distance, meditation, exercise)
}
```

A `null` result means either no activity in the time range or a missing/denied permission.

### 4. Displaying Results

Each metric is shown as a summary line:

- **Language**: `📚 Language    45 min · HIGH · 85%`
- **Reading**: `📖 Reading    30 min · MEDIUM · 75%`
- **Social**: `📱 Social    120 min · HIGH · 88%`
- **Movies**: `🎬 Movies    3 films · HIGH · 95%` (expands into one line per film with its release year, TMDB id, star rating, like marker, rewatch marker, poster URL, and review)
- **Steps**: `👣 Steps    7,622 steps · HIGH · 99%`
- **Distance**: `📏 Distance    5.42 km · HIGH · 99%`
- **Meditation**: `🧘 Meditation    15 min · 1 session · HC+Usage · 97%`
- **Exercise**: `🏃 Exercise    45 min · 2 sessions · Running, Strength Training · 99%`

**Per-session breakdown.** For the usage-based metrics (Language, Reading, Social), Steps, and
Distance, the sample expands the summary line into one indented line per session, showing **time
from – time to** and the **app name** (or step count / distance). This comes straight from each
result's `sessions` list — the library always returns it; the app simply renders it:

```
📱 Social    120 min · HIGH · 88%
    • 08:02–08:19 · Instagram (17 min)
    • 12:40–13:05 · Reddit (25 min)
    • 21:10–22:28 · Instagram (78 min)

👣 Steps    7,622 steps · HIGH · 99%
    • 08:00–09:00 · 1,204 steps
    • 12:00–13:00 · 3,560 steps
    • 18:00–19:00 · 2,858 steps

📏 Distance    5.42 km · HIGH · 99%
    • 08:00–09:00 · 0.92 km
    • 12:00–13:00 · 2.74 km
    • 18:00–19:00 · 1.76 km
```

`sessionLines()` formats `UsageSession`s (Language, Reading, Social) as `from–to · appName (N min)`;
`stepSessionLines()` formats `StepSession`s as `from–to · N steps`; `distanceSessionLines()` formats
`DistanceSession`s as `from–to · N km` (or `N m` under a kilometre). Timestamps are rendered as local
`HH:mm`. Note that step and distance "sessions" are **hourly Health Connect buckets**, not discrete walking bouts —
empty hours are omitted by the library, so every line shown has a non-zero count.

The Movies row gets the same treatment via `movieSessionLines()`, expanding each `MovieSession` into
`watchedDate · title (year) (tmdb:<id>) · ★rating ♥ ↻`, with the poster URL and review on further
indented lines:

```
🎬 Movies    3 films · HIGH · 95%
    • Jan 15 · Dune: Part Two (2024) (tmdb:693134) · ★★★★½ ♥ ↻
        🖼 https://a.ltrbxd.com/resized/film-poster/…-crop.jpg
        "Villeneuve outdid himself."
    • Jan 18 · Poor Things (2023) (tmdb:792307) · ★★★★
    • Jan 22 · The Zone of Interest (2023)
```

The `(year)` is the film's release year from the feed's `letterboxd:filmYear` element. `tmdb:<id>` is
the film's The Movie Database id, parsed from the feed's `tmdb:movieId` element — use it to fetch
runtime, cast, etc. from the TMDB API. The star score comes from `MovieSession.rating` (the feed's
`letterboxd:memberRating`), `♥` marks `isLiked` (the feed's `letterboxd:memberLike`), `↻` marks
`isRewatch`, the `🖼` line is `posterUrl` (extracted from the entry description), and the quoted line
is `review` (plain text). Films whose feed entry has no year, id, rating, poster, or review simply
omit those parts — e.g. *The Zone of Interest* above was logged unrated, without a review.

For meditation, the sample renders the active data sources inline: `HC` for Health Connect, `Usage` for UsageStats, `HC+Usage` when both contributed and were merged. `—` is shown when the result is null (no data or no permission granted on either source).

For exercise, the sample lists the distinct exercise types that appeared in the window (title-cased, comma-separated), deduplicated across sessions. Confidence is a flat 99% since all data comes from Health Connect `ExerciseSessionRecord`.

### 5. Listing tracked apps

Below the results, a **Show tracked apps** button toggles the library's static app catalogue, built
from the per-habit accessors `getTrackedLanguageLearningApps()`, `getTrackedReadingApps()`,
`getTrackedSocialMediaApps()`, and `getTrackedMeditationApps()`. Unlike the query results, this is
pure configuration — it lists every app the library *can* detect for each known-app habit,
regardless of what is installed or which permissions are granted, so the lookups are synchronous (no
coroutine). Each habit shows its app count and one indented line per app with its package name and
base confidence multiplier:

```
📖 Reading · 27 apps
    • com.amazon.kindle (82%)
    • com.audible.application (90%)
    ...
```

`toggleTrackedApps()` shows/hides the block; `trackedAppsBlock()` formats one habit's list. Movie
watching, step counting, and exercise have no entry here because they are sourced from feeds and
sensors rather than a fixed app list.

## How to Run

1. **Install on device**:
   ```bash
   ./gradlew :app:installDebug
   ```

2. **Grant Usage Access**:
   - Tap "Grant" next to "Usage access  ✗"
   - Find "Tracker Demo" in the list and enable "Permit usage access"
   - Return to the app — metrics update automatically

3. **Grant Health Connect** (optional, for step counting, distance, the HealthConnect branch of meditation, and exercise):
   - Tap "Grant" next to the Health Connect row
   - Approve `Steps`, `Distance`, `Mindfulness sessions`, and `Exercise sessions` in the Health Connect dialog
   - Return to the app — step count, distance, meditation, and exercise results appear automatically

4. **Enable Movie Watching** (optional):
   - Set `letterboxdUsername` in `MainActivity.kt` to your Letterboxd username

5. **Refresh**:
   - Tap "Query Today" to manually refresh all metrics

6. **Browse tracked apps** (no permissions needed):
   - Tap "Show tracked apps" to list every app the library can detect for language learning, reading, social media, and meditation, with each app's package name and base confidence

## Code Structure

```
MainActivity.kt
├── onCreate()                  # Build Tracker, register HC permission launcher, wire buttons
├── onResume()                  # Update permission UI, auto-query metrics
├── updateUsagePermissionUi()   # Show/hide Grant button based on AppOps check
├── hasUsageStatsPermission()   # AppOpsManager permission check
├── updateHcPermissionUi()      # Show/hide Grant button based on HC permission state (steps + mindfulness + exercise)
├── queryMetrics()              # Query all eight metrics concurrently using coroutines
├── toggleTrackedApps()         # Show/hide the static tracked-apps catalogue (getTracked*Apps())
├── trackedAppsBlock()          # Format one habit's tracked apps as "package (confidence)" lines
├── displayResults()            # Render the summary line per metric
├── sessionLines()              # Expand UsageSession list into "from–to · appName (N min)" lines
├── stepSessionLines()          # Expand StepSession list into "from–to · N steps" lines (hourly buckets)
├── distanceSessionLines()      # Expand DistanceSession list into "from–to · N km" lines (hourly buckets)
├── movieSessionLines()         # Expand MovieSession list into "watchedDate · title (year) (tmdb:id) · ★rating ♥ ↻ + poster + review" lines
├── starRating()                # Render a 0.5–5.0 rating as ★ glyphs (e.g. 4.5 → ★★★★½)
├── sourcesLabel()              # Render MeditationResult.sources as "HC+Usage"
└── titleCase()                 # Convert "strength_training" → "Strength Training" for exercise type labels
```

## Key Learnings

1. **Builder Pattern**: Configure the Tracker with a fluent API — no `enable*` flags needed, all features are always available
2. **Coroutines**: All query methods are `suspend` functions; wrap each independently with `runCatching` so one failure doesn't block the rest
3. **Two permission flows**: `PACKAGE_USAGE_STATS` requires a manual system settings redirect; Health Connect permissions use the runtime permission contract
4. **Multiple Health Connect permissions in one prompt**: pass a `Set` with all required `HealthPermission` strings to `launcher.launch(...)` — the user only sees items they haven't granted
5. **Health Connect manifest setup**: Both `ACTION_SHOW_PERMISSIONS_RATIONALE` (Android 9–13) and the `VIEW_PERMISSION_USAGE` activity-alias (Android 14+) must be declared for the permission dialog to work
6. **Step counting & distance deduplication**: Use `queryStepCounting()` / `queryDistance()` rather than summing `StepsRecord` / `DistanceRecord` entries manually — Health Connect's aggregation API handles deduplication across Google Fit, the phone step counter, and other sources. Both use the same hourly-bucket shape, so `distanceSessionLines()` is a near-copy of `stepSessionLines()`
7. **Multi-source fusion for meditation**: `queryMeditation()` merges HealthConnect `MindfulnessSessionRecord`s with foreground sessions of known meditation apps. Overlapping sessions are deduplicated, and the result's `sources` list reflects which sources contributed. If HealthConnect is unavailable, the call gracefully falls back to UsageStats-only.
8. **Exercise from Health Connect**: `queryExercise()` reads `ExerciseSessionRecord`s written by any fitness app (Strava, Google Fit, Samsung Health, manual log, etc.). Each session exposes both `exerciseTypeId` (the raw HC int) and `exerciseType` (a snake_case string) so the app can choose programmatic or display-friendly treatment. The sample uses a title-case transform (`"strength_training"` → `"Strength Training"`) for display.

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

**Supported meditation apps:**
- Calm, Headspace, Insight Timer, Balance, Waking Up
- Smiling Mind, Ten Percent Happier, Medito, MEISOON, Mindvalley

**Movie watching:**
- Set `letterboxdUsername` in `MainActivity.kt` to your Letterboxd username
- Each detected film shows its title and release `year` (from the feed's `letterboxd:filmTitle` and `letterboxd:filmYear` elements) plus its TMDB id (`tmdb:<id>`) parsed from the feed's `tmdb:movieId` element — use it to look the film up in The Movie Database. Films the feed hasn't linked to TMDB show just the title.
- Each `MovieSession` also exposes the user's own log data: `rating` (0.5–5.0 stars, `null` when unrated), `review` (plain text, `null` when there's no written review), `posterUrl` (the poster image URL from the feed, `null` when omitted), `isRewatch`, and `isLiked`. The sample renders these as `★★★★½`, a `♥` marker, a `↻` marker, a `🖼` poster URL line, and a quoted review line.

**Step counting:**
- Requires Health Connect to be installed (built-in on Android 14+; available via Google Play on Android 9–13)
- Grant the `Steps` permission when prompted

**Distance:**
- Requires Health Connect; distance is written by fitness/step apps (Google Fit, Pixel, Fitbit, etc.) or logged manually
- Grant the `Distance` permission when prompted
- Reported in kilometres (rolled up from per-hour buckets); sub-kilometre hours render in meters

**Meditation:**
- Either: use one of the supported meditation apps (detected via UsageStats), or
- Have a meditation app that writes `MindfulnessSessionRecord`s to Health Connect (e.g. Calm), or log a session manually in the Health Connect app
- Grant the `Mindfulness sessions` permission when prompted for best results (both sources merged)

**Exercise:**
- Have any fitness app that writes `ExerciseSessionRecord`s to Health Connect (Strava, Google Fit, Samsung Health, Peloton, etc.), or log a workout manually in the Health Connect app
- Grant the `Exercise sessions` permission when prompted
- All exercise types recognised by Health Connect are supported (running, cycling, strength training, yoga, swimming, etc.)

## Notes

- The sample app requires Android API 26+ (Android 8.0)
- The library itself supports Android API 21+; the higher sample app minimum is due to Health Connect and `java.time` usage
- `PACKAGE_USAGE_STATS` is a protected permission that requires user action in system settings
- Queries always cover today from midnight in the device's local timezone
- On Android 10+, session times use precise `ACTIVITY_RESUMED`/`ACTIVITY_PAUSED` events
- `MindfulnessSessionRecord` requires a recent `androidx.health.connect:connect-client` release. On older HC installations, the meditation query transparently falls back to UsageStats-only.
- `ExerciseSessionRecord` is authoritative — confidence is fixed at `0.99` because these records are written by fitness apps or entered manually by the user. Sessions shorter than one minute still appear in `sessions` (rounded to `0 min`) so the session count remains accurate.
