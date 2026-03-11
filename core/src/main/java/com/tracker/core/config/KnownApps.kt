package com.tracker.core.config

/**
 * Static configuration of known apps for habit tracking.
 *
 * Each app has:
 * - Package name for identification
 * - Confidence multiplier (base confidence when detected)
 *
 * Note: Minimum session duration is defined per-habit in HabitConfig, not per-app.
 */
object KnownApps {

    /**
     * Map of known language learning apps.
     * Key: package name
     * Value: app metadata
     */
    val languageLearning: Map<String, AppMetadata> = mapOf(
        "com.mindtwisted.kanjistudy" to AppMetadata(
            packageName = "com.mindtwisted.kanjistudy",
            confidenceMultiplier = 0.85f
        ),
        "com.lulilanguages.j5a" to AppMetadata(
            packageName = "com.lulilanguages.j5a",
            confidenceMultiplier = 0.83f
        ),
        "com.ichi2.anki" to AppMetadata(
            packageName = "com.ichi2.anki",
            confidenceMultiplier = 0.85f
        ),
        "com.eup.mytest" to AppMetadata(
            packageName = "com.eup.mytest",
            confidenceMultiplier = 0.80f
        ),
        "mobi.eup.jpnews" to AppMetadata(
            packageName = "mobi.eup.jpnews",
            confidenceMultiplier = 0.75f
        ),
        "com.duolingo" to AppMetadata(
            packageName = "com.duolingo",
            confidenceMultiplier = 0.80f
        ),
        "com.eup.heyjapan" to AppMetadata(
            packageName = "com.eup.heyjapan",
            confidenceMultiplier = 0.80f
        ),
        "com.appsci.tenwords" to AppMetadata(
            packageName = "com.appsci.tenwords",
            confidenceMultiplier = 0.83f
        ),
        "com.languagedrops.drops.international" to AppMetadata(
            packageName = "com.languagedrops.drops.international",
            confidenceMultiplier = 0.82f
        ),
        "com.languagedrops.drops.learn.learning.speak.language.japanese.kanji.katakana.hiragana.romaji.words" to AppMetadata(
            packageName = "com.languagedrops.drops.learn.learning.speak.language.japanese.kanji.katakana.hiragana.romaji.words",
            confidenceMultiplier = 0.82f
        ),
        "com.lingodeer" to AppMetadata(
            packageName = "com.lingodeer",
            confidenceMultiplier = 0.83f
        ),
        "com.lulilanguages.j5KjAnd" to AppMetadata(
            packageName = "com.lulilanguages.j5KjAnd",
            confidenceMultiplier = 0.83f
        ),
        "com.renshuu.renshuu_org" to AppMetadata(
            packageName = "com.renshuu.renshuu_org",
            confidenceMultiplier = 0.84f
        )
    )

    /**
     * Map of known reading apps.
     * Key: package name
     * Value: app metadata
     */
    val reading: Map<String, AppMetadata> = mapOf(
        "com.google.android.apps.books" to AppMetadata(
            packageName = "com.google.android.apps.books",
            confidenceMultiplier = 0.80f
        ),
        "com.amazon.kindle" to AppMetadata(
            packageName = "com.amazon.kindle",
            confidenceMultiplier = 0.82f
        )
    )

    /**
     * Check if a package is a known language learning app.
     */
    fun isLanguageLearningApp(packageName: String): Boolean {
        return languageLearning.containsKey(packageName)
    }

    /**
     * Get metadata for a language learning app.
     */
    fun getLanguageLearningApp(packageName: String): AppMetadata? {
        return languageLearning[packageName]
    }

    /**
     * Check if a package is a known reading app.
     */
    fun isReadingApp(packageName: String): Boolean {
        return reading.containsKey(packageName)
    }

    /**
     * Get metadata for a reading app.
     */
    fun getReadingApp(packageName: String): AppMetadata? {
        return reading[packageName]
    }
}
