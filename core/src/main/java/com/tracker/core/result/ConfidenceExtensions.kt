package com.tracker.core.result

import com.tracker.core.types.ConfidenceLevel

fun Float.toConfidenceLevel(): ConfidenceLevel = when {
    this >= 0.75f -> ConfidenceLevel.HIGH
    this >= 0.50f -> ConfidenceLevel.MEDIUM
    else -> ConfidenceLevel.LOW
}
