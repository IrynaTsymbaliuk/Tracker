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
 * Aggregates evidence into daily habit results.
 *
 * @param dayMillis Start of day timestamp (00:00:00)
 * @param evidence List of evidence for this day
 * @return Aggregated result with confidence score
 */
fun aggregate(dayMillis: Long, evidence: List<Evidence>): LanguageLearningResult {
    // Implementation
}
```

## Adding New Metrics

To add a new metric (e.g., workout tracking):

### 1. Add the Metric Type

```kotlin
// core/src/main/java/com/tracker/core/types/Metric.kt
enum class Metric {
    LANGUAGE_LEARNING,
    WORKOUT  // Add new metric
}
```

### 2. Create a Collector

```kotlin
// core/src/main/java/com/tracker/core/collector/WorkoutCollector.kt
class WorkoutCollector(
    context: Context,
    private val permissionManager: PermissionManager
) : Collector {
    override suspend fun collect(fromMillis: Long, toMillis: Long): Result<List<Evidence>> {
        // Implementation to collect workout app usage
    }
}
```

### 3. Create an Aggregator

```kotlin
// core/src/main/java/com/tracker/core/aggregator/WorkoutAggregator.kt
class WorkoutAggregator : Aggregator<WorkoutResult> {
    override fun aggregate(dayMillis: Long, evidence: List<Evidence>): WorkoutResult? {
        // Implementation to aggregate workout evidence
    }
}
```

### 4. Create a Result Type

```kotlin
// core/src/main/java/com/tracker/core/result/WorkoutResult.kt
data class WorkoutResult(
    override val occurred: Boolean,
    override val confidence: Float,
    override val confidenceLevel: ConfidenceLevel,
    val durationMinutes: Int?,
    val workoutType: String?
) : HabitResult
```

### 5. Register in HabitEngine

```kotlin
// core/src/main/java/com/tracker/core/engine/HabitEngine.kt
val collectors = mapOf(
    Metric.LANGUAGE_LEARNING to UsageStatsCollector(context, permissionManager),
    Metric.WORKOUT to WorkoutCollector(context, permissionManager)
)

val aggregators = mapOf<Metric, Aggregator<out HabitResult>>(
    Metric.LANGUAGE_LEARNING to LanguageLearningAggregator(),
    Metric.WORKOUT to WorkoutAggregator()
)
```

### 6. Add Tests

Create comprehensive tests for the new metric:
- Collector tests
- Aggregator tests
- Integration tests

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
- Follow the [Code of Conduct](CODE_OF_CONDUCT.md)
- Ask questions if anything is unclear
- Help others when you can

## License

By contributing, you agree that your contributions will be licensed under the Apache License 2.0.

## Questions?

- Open a [Discussion](https://github.com/IrynaTsymbaliuk/Tracker/discussions)
- Comment on an existing issue
- Ask in your pull request

Thank you for contributing to Tracker! 🎉
