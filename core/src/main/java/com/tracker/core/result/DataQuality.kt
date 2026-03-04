package com.tracker.core.result

import com.tracker.core.types.DataSource

/**
 * Information about data source availability and overall reliability.
 *
 * @property availableSources Data sources that are currently accessible
 * @property missingSources Data sources that are unavailable with reasons
 * @property overallReliability Overall reliability assessment
 * @property recommendations User-friendly suggestions for improving data quality
 */
data class DataQuality(
    val availableSources: List<DataSource>,
    val missingSources: List<MissingSource>,
    val overallReliability: ReliabilityLevel,
    val recommendations: List<String>
)

/**
 * Information about a missing data source.
 *
 * @property source The missing data source
 * @property reason Why this source is unavailable
 * @property message Human-readable explanation
 */
data class MissingSource(
    val source: DataSource,
    val reason: MissingReason,
    val message: String
)

/**
 * Reason why a data source is unavailable.
 */
enum class MissingReason {
    NO_PERMISSION,
    APP_NOT_INSTALLED,
    API_UNAVAILABLE,
    OLD_ANDROID_VERSION
}

/**
 * Overall reliability assessment.
 */
enum class ReliabilityLevel {
    HIGH,    // All required sources available
    MEDIUM,  // Some sources available
    LOW      // Few or no sources available
}
