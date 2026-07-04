# Contributing to Tracker

Thank you for your interest in contributing to Tracker! We welcome contributions from the community.

## Ways to Contribute

- 🐛 **Report bugs** - Help us identify and fix issues
- 💡 **Suggest new metrics** - Propose new habits to track (workout, sleep, reading, etc.)
- 📝 **Improve documentation** - Make it easier for others to use the library
- ✨ **Add features** - Implement new functionality
- 🧪 **Write tests** - Improve test coverage
- 🔍 **Review pull requests** - Help maintain code quality

## Getting Started

### Prerequisites

- JDK 11 or higher
- Android Studio (latest stable version recommended)
- Git

### Fork and Clone

1. Fork the repository on GitHub
2. Clone your fork locally:
   ```bash
   git clone https://github.com/YOUR_USERNAME/Tracker.git
   cd Tracker
   ```
3. Add upstream remote:
   ```bash
   git remote add upstream https://github.com/IrynaTsymbaliuk/Tracker.git
   ```

### Building the Project

```bash
./gradlew build
```

### Running Tests

```bash
./gradlew test
```

All tests must pass before submitting a pull request.

## Making Changes

### 1. Create a Feature Branch

```bash
git checkout -b feature/my-awesome-feature
```

Branch naming conventions:
- `feature/description` - New features
- `fix/description` - Bug fixes
- `docs/description` - Documentation updates
- `test/description` - Test additions/fixes

### 2. Make Your Changes

- Write clean, readable code
- Follow Kotlin coding conventions
- Add KDoc comments for public APIs
- Keep methods focused and small
- Write meaningful commit messages

### 3. Add Tests

- Add unit tests for new functionality
- Ensure all tests pass: `./gradlew test`
- Aim for high test coverage on new code

### 4. Update Documentation

- Update README.md if adding user-facing features
- Update KDoc comments for API changes
- Add entries to CHANGELOG.md for notable changes

### 5. Commit Your Changes

```bash
git add .
git commit -m "Add feature: description of what you did"
```

**Commit message guidelines:**
- Use present tense ("Add feature" not "Added feature")
- Be concise but descriptive
- Reference issue numbers if applicable (e.g., "Fix #123")

### 6. Push and Create a Pull Request

```bash
git push origin feature/my-awesome-feature
```

Then open a pull request on GitHub with:
- Clear title describing the change
- Description of what changed and why
- Reference to any related issues
- Screenshots if UI-related

## Code Style

### Kotlin Conventions

Follow the [Kotlin Coding Conventions](https://kotlinlang.org/docs/coding-conventions.html):

- Use 4 spaces for indentation
- Use camelCase for function and variable names
- Use PascalCase for class names
- Use UPPER_SNAKE_CASE for constants

### Example

```kotlin
/**
 * Maps collected evidence into a public result for a query window.
 *
 * @param fromMillis Start of the query window, in milliseconds since epoch
 * @param toMillis End of the query window, in milliseconds since epoch
 * @param evidence Collected source evidence for the query window
 * @return Result for the queried window, or null when no source data is available
 */
suspend fun query(fromMillis: Long, toMillis: Long): LanguageLearningResult? {
    // Implementation
}
```

## Adding New Metrics

To add a new metric (e.g., workout tracking):

### 1. Add Result Types

```kotlin
// core/src/main/java/com/tracker/core/result/WorkoutResult.kt
data class WorkoutResult(
    override val sources: List<DataSource>,
    override val timeRange: TimeRange,
    val durationMinutes: Int,
    val sessions: List<WorkoutSession> = emptyList()
) : HabitResult()
```

### 2. Create a Collector

```kotlin
// core/src/main/java/com/tracker/core/collector/WorkoutCollector.kt
class WorkoutCollector(
    context: Context,
    private val dispatcher: CoroutineDispatcher = Dispatchers.IO
) {
    suspend fun collect(fromMillis: Long, toMillis: Long): List<DurationEvidence> =
        withContext(dispatcher) {
            // Implementation to collect workout app usage
        }
}
```

Collectors that touch Android system services, disk, or network should be safe to call from the main thread by switching to an appropriate dispatcher internally.

### 3. Create a Provider

```kotlin
// core/src/main/java/com/tracker/core/provider/WorkoutProvider.kt
class WorkoutProvider internal constructor(
    private val collector: WorkoutCollector
) : MetricProvider<WorkoutResult> {
    override suspend fun query(fromMillis: Long, toMillis: Long): WorkoutResult? {
        val evidence = collector.collect(fromMillis, toMillis).ifEmpty { return null }
        val sessions = evidence.map { ev ->
            WorkoutSession(
                startTime = ev.startTimeMillis,
                endTime = ev.endTimeMillis,
                durationMinutes = ev.durationMinutes
            )
        }
        return WorkoutResult(
            sources = evidence.map { it.source }.distinct(),
            timeRange = TimeRange(fromMillis, toMillis),
            durationMinutes = sessions.sumOf { it.durationMinutes },
            sessions = sessions.sortedBy { it.startTime }
        )
    }
}
```

### 4. Wire the Tracker Facade

```kotlin
private val workoutProvider by lazy {
    WorkoutProvider(WorkoutCollector(appContext))
}

suspend fun queryWorkout(days: Int = 1): WorkoutResult? {
    val (from, to) = queryWindow(days)
    return workoutProvider.query(from, to)
}
```

### 5. Update Configuration

If the metric is backed by known apps, add entries to `KnownApps.kt`, keep package names unique, and update the manifest `<queries>` list. If the metric needs a new Android or Health Connect permission, document the manifest and runtime request requirements in `README.md` and the sample app.

### 6. Add Tests

Create comprehensive tests for the new metric:
- Collector tests
- Provider tests
- Tracker facade tests
- Manifest/configuration parity tests, when the metric has known apps or permissions

## Pull Request Process

1. **Ensure tests pass** - `./gradlew test` must succeed
2. **Update documentation** - README, KDoc, CHANGELOG
3. **One feature per PR** - Keep changes focused
4. **Respond to feedback** - Address review comments promptly
5. **Squash commits** - Clean up commit history if requested

## Review Process

- Maintainers will review your PR within a few days
- Feedback will be provided as review comments
- Make requested changes and push to your branch
- Once approved, a maintainer will merge your PR

## Good First Issues

Look for issues labeled `good first issue` - these are great for new contributors!

## Community Guidelines

- Be respectful and constructive
- Ask questions if anything is unclear
- Help others when you can

## License

By contributing, you agree that your contributions will be licensed under the Apache License 2.0.

## Questions?

- Open a [Discussion](https://github.com/IrynaTsymbaliuk/Tracker/discussions)
- Comment on an existing issue
- Ask in your pull request

Thank you for contributing to Tracker! 🎉
