package com.tracker.core.types

/**
 * A coarse, non-clinical sleep-quality band derived from **sleep efficiency** — the fraction of
 * time in bed that was actually spent asleep (`asleepMinutes / timeInBedMinutes`). Sleep
 * efficiency is the standard proxy for sleep quality; roughly 85%+ is considered healthy for
 * adults.
 *
 * This is a convenience label only. It is **not** a medical assessment. Sources that write no
 * sleep stages give no awake data to measure efficiency against, so their sessions report
 * [UNKNOWN]. Apps that want their own scoring can ignore this and use the raw
 * `com.tracker.core.result.SleepSession` stage minutes (deep/REM/light/awake) directly.
 *
 * Banding (by efficiency `e`):
 * - [EXCELLENT] — `e >= 0.90`
 * - [GOOD] — `0.85 <= e < 0.90`
 * - [FAIR] — `0.75 <= e < 0.85`
 * - [POOR] — `e < 0.75`
 * - [UNKNOWN] — no stage data, so efficiency cannot be measured
 */
enum class SleepQuality {
    UNKNOWN,
    POOR,
    FAIR,
    GOOD,
    EXCELLENT;

    companion object {
        /**
         * Bands a sleep-efficiency fraction (0.0–1.0) into a [SleepQuality]. Returns [UNKNOWN]
         * when [efficiency] is null (no stage data to measure against).
         */
        fun fromEfficiency(efficiency: Double?): SleepQuality = when {
            efficiency == null -> UNKNOWN
            efficiency >= 0.90 -> EXCELLENT
            efficiency >= 0.85 -> GOOD
            efficiency >= 0.75 -> FAIR
            else -> POOR
        }
    }
}
