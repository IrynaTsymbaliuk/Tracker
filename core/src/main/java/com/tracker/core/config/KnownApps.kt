package com.tracker.core.config

/**
 * Static configuration of known language learning apps.
 *
 * Each app has:
 * - Package name for identification
 * - Confidence multiplier (base confidence when detected)
 * - Minimum session duration to count as evidence
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
            confidenceMultiplier = 0.85f,
            minSessionMinutes = 5
        ),
        "com.lulilanguages.j5a" to AppMetadata(
            packageName = "com.lulilanguages.j5a",
            confidenceMultiplier = 0.83f,
            minSessionMinutes = 5
        ),
        "com.ichi2.anki" to AppMetadata(
            packageName = "com.ichi2.anki",
            confidenceMultiplier = 0.85f,
            minSessionMinutes = 5
        ),
        "com.eup.mytest" to AppMetadata(
            packageName = "com.eup.mytest",
            confidenceMultiplier = 0.80f,
            minSessionMinutes = 5
        ),
        "mobi.eup.jpnews" to AppMetadata(
            packageName = "mobi.eup.jpnews",
            confidenceMultiplier = 0.75f,
            minSessionMinutes = 10
        ),
        "com.duolingo" to AppMetadata(
            packageName = "com.duolingo",
            confidenceMultiplier = 0.80f,
            minSessionMinutes = 5
        ),
        "com.eup.heyjapan" to AppMetadata(
            packageName = "com.eup.heyjapan",
            confidenceMultiplier = 0.80f,
            minSessionMinutes = 5
        ),
        "com.appsci.tenwords" to AppMetadata(
            packageName = "com.appsci.tenwords",
            confidenceMultiplier = 0.83f,
            minSessionMinutes = 5
        ),
        "com.languagedrops.drops.international" to AppMetadata(
            packageName = "com.languagedrops.drops.international",
            confidenceMultiplier = 0.82f,
            minSessionMinutes = 5
        ),
        "com.languagedrops.drops.learn.learning.speak.language.japanese.kanji.katakana.hiragana.romaji.words" to AppMetadata(
            packageName = "com.languagedrops.drops.learn.learning.speak.language.japanese.kanji.katakana.hiragana.romaji.words",
            confidenceMultiplier = 0.82f,
            minSessionMinutes = 5
        ),
        "com.lingodeer" to AppMetadata(
            packageName = "com.lingodeer",
            confidenceMultiplier = 0.83f,
            minSessionMinutes = 5
        ),
        "com.lulilanguages.j5KjAnd" to AppMetadata(
            packageName = "com.lulilanguages.j5KjAnd",
            confidenceMultiplier = 0.83f,
            minSessionMinutes = 5
        ),
        "com.renshuu.renshuu_org" to AppMetadata(
            packageName = "com.renshuu.renshuu_org",
            confidenceMultiplier = 0.84f,
            minSessionMinutes = 5
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
}
