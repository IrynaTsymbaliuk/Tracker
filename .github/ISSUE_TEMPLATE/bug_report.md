---
name: Bug Report
about: Report a bug or unexpected behavior
title: '[BUG] '
labels: bug
assignees: ''
---

## Bug Description

A clear and concise description of the bug.

## Steps to Reproduce

1. Initialize Tracker with...
2. Call method...
3. Observe the error...

## Expected Behavior

What you expected to happen.

## Actual Behavior

What actually happened.

## Code Sample

```kotlin
// Minimal code to reproduce the issue
val tracker = Tracker.Builder(context)
    .requestMetrics(Metric.LANGUAGE_LEARNING)
    .build()
```

## Environment

- **Library Version**: 1.0.0
- **Android Version**: (e.g., Android 13)
- **Device**: (e.g., Pixel 6)
- **Min SDK**: (e.g., 21)

## Logs/Stack Trace

```
Paste any relevant error messages or stack traces here
```

## Additional Context

Any other information that might help diagnose the issue.

## Checklist

- [ ] I have searched existing issues to avoid duplicates
- [ ] I am using the latest version of the library
- [ ] I have included a minimal code sample to reproduce the issue
