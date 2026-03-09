# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [1.0.0] - 2026-03-09

### Added
- Initial release of Tracker library for Android
- Language learning habit tracking with automatic app usage detection
- Support for 13 popular language learning apps (Duolingo, Anki, Kanji Study, Renshuu, and more)
- `Tracker` class with fluent Builder API for easy configuration
- Async query support with Kotlin coroutines (`queryAsync()`)
- Callback-based query API for flexibility
- Confidence scoring system (0.0-1.0) with HIGH/MEDIUM/LOW levels
- Smart aggregation with overlap deduplication for same-app sessions
- Multiple apps tracked separately (no cross-app deduplication)
- Probability-based confidence combination using formula: 1 - ∏(1 - p_i)
- Duration tracking in minutes per day
- Summary statistics (total days, language learning days, average duration)
- App information with both package names and human-readable app names
- Data quality reporting with reliability levels and recommendations
- Permission checking for PACKAGE_USAGE_STATS (non-intrusive, never auto-requests)
- Android 11+ package visibility support with proper manifest queries
- ProGuard/R8 consumer rules for code shrinking compatibility
- Comprehensive sample application demonstrating library integration
- Full test coverage
- Complete API documentation with KDoc

### Technical Details
- Min SDK: Android 5.0 (API 21)
- Target SDK: Android 14+ (API 36)
- Language: Kotlin
- License: Apache License 2.0
- Dependencies: Kotlin Coroutines

[1.0.0]: https://github.com/yourusername/tracker/releases/tag/v1.0.0
