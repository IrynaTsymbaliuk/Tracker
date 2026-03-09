---
name: New Metric Suggestion
about: Propose a new habit metric to track
title: '[METRIC] '
labels: enhancement, metric
assignees: ''
---

## Metric Name

What habit do you want to track? (e.g., Workout/Exercise, Reading, Meditation, Sleep)

## Description

Describe the habit and why it would be valuable to track.

## Data Sources

How could this habit be detected automatically?

- [ ] App usage (UsageStats API) - List specific apps
- [ ] Screen time patterns
- [ ] Device sensors (e.g., step counter, heart rate)
- [ ] Other: ___________

## Apps to Track

If using app usage detection, list the apps that indicate this habit:

1. App Name (package: com.example.app)
2. App Name (package: com.example.app2)
3. ...

## Detection Logic

How should the library determine if the habit occurred on a given day?

- **Minimum Duration**: ___ minutes
- **Confidence Multipliers**: High confidence apps vs. low confidence apps
- **Special Rules**: Any unique logic needed?

## Expected Output

What information should be returned for this metric?

```kotlin
data class YourMetricResult(
    val occurred: Boolean,
    val confidence: Float,
    val confidenceLevel: ConfidenceLevel,
    val durationMinutes: Int?,
    // What other fields would be useful?
)
```

## Similar Metrics

Are there similar habits or metrics this could be grouped with?

## Would You Like to Contribute?

- [ ] I'm willing to implement the Collector for this metric
- [ ] I'm willing to implement the Aggregator for this metric
- [ ] I can help with testing
- [ ] I can provide sample data

## Additional Context

Any other information, research, or references.

## Checklist

- [ ] I have searched existing issues for similar metrics
- [ ] This metric can be detected automatically (no manual input)
- [ ] This metric respects user privacy
