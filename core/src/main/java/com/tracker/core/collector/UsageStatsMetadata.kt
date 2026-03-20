package com.tracker.core.collector

/**
 * Type-safe metadata for usage stats evidence.
 *
 * @property packageName App package identifier
 * @property appName Human-readable app name
 */
data class UsageStatsMetadata(
    val packageName: String,
    val appName: String
) {
    /** Converts to map for Evidence compatibility. */
    fun toMap(): Map<String, Any> = mapOf(
        KEY_PACKAGE_NAME to packageName,
        KEY_APP_NAME to appName
    )

    companion object {
        const val KEY_PACKAGE_NAME = "packageName"
        const val KEY_APP_NAME = "appName"

        /**
         * Extracts UsageStatsMetadata from a metadata map.
         *
         * @return UsageStatsMetadata if all fields are valid, null otherwise
         */
        fun fromMap(map: Map<String, Any>): UsageStatsMetadata? {
            return try {
                UsageStatsMetadata(
                    packageName = map[KEY_PACKAGE_NAME] as? String ?: return null,
                    appName = map[KEY_APP_NAME] as? String ?: return null
                )
            } catch (e: Exception) {
                null
            }
        }
    }
}
