package com.tracker.core.config

/**
 * Static configuration of known apps for habit tracking.
 *
 * Each app has:
 * - Package name for identification
 * - Confidence multiplier (base confidence when detected)
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
     * Map of known social media apps.
     * Key: package name
     * Value: app metadata
     */
    val socialMedia: Map<String, AppMetadata> = mapOf(
        "com.facebook.katana" to AppMetadata(
            packageName = "com.facebook.katana",
            confidenceMultiplier = 0.95f
        ),
        "com.instagram.android" to AppMetadata(
            packageName = "com.instagram.android",
            confidenceMultiplier = 0.95f
        ),
        "com.twitter.android" to AppMetadata(
            packageName = "com.twitter.android",
            confidenceMultiplier = 0.90f
        ),
        "com.zhiliaoapp.musically" to AppMetadata(
            packageName = "com.zhiliaoapp.musically",
            confidenceMultiplier = 0.95f
        ),
        "com.snapchat.android" to AppMetadata(
            packageName = "com.snapchat.android",
            confidenceMultiplier = 0.90f
        ),
        "com.linkedin.android" to AppMetadata(
            packageName = "com.linkedin.android",
            confidenceMultiplier = 0.85f
        ),
        "com.reddit.frontpage" to AppMetadata(
            packageName = "com.reddit.frontpage",
            confidenceMultiplier = 0.85f
        ),
        "com.pinterest" to AppMetadata(
            packageName = "com.pinterest",
            confidenceMultiplier = 0.80f
        ),
        "com.whatsapp" to AppMetadata(
            packageName = "com.whatsapp",
            confidenceMultiplier = 0.75f
        ),
        "org.telegram.messenger" to AppMetadata(
            packageName = "org.telegram.messenger",
            confidenceMultiplier = 0.75f
        ),
        "com.discord" to AppMetadata(
            packageName = "com.discord",
            confidenceMultiplier = 0.80f
        ),
        "com.instagram.barcelona" to AppMetadata(
            packageName = "com.instagram.barcelona",
            confidenceMultiplier = 0.90f
        ),
        "org.joinmastodon.android" to AppMetadata(
            packageName = "org.joinmastodon.android",
            confidenceMultiplier = 0.85f
        ),
        "xyz.blueskyweb.app" to AppMetadata(
            packageName = "xyz.blueskyweb.app",
            confidenceMultiplier = 0.85f
        ),
        "com.tumblr" to AppMetadata(
            packageName = "com.tumblr",
            confidenceMultiplier = 0.80f
        )
    )

}
