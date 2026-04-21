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

    /**
     * Map of known meditation apps where the user must spend time meditating.
     * Key: package name
     * Value: app metadata
     *
     * Confidence multipliers are in the 0.85–0.95 range. Pure meditation apps
     * (Calm, Headspace, Insight Timer, Balance, Waking Up, Smiling Mind, Ten
     * Percent Happier, Medito, MEISOON) score 0.90–0.95 because foreground time
     * almost always maps to an active meditation session. Mindvalley is broader
     * than meditation, so its confidence is lower.
     */
    val meditation: Map<String, AppMetadata> = mapOf(
        "com.calm.android" to AppMetadata(
            packageName = "com.calm.android",
            confidenceMultiplier = 0.95f
        ),
        "com.getsomeheadspace.android" to AppMetadata(
            packageName = "com.getsomeheadspace.android",
            confidenceMultiplier = 0.95f
        ),
        "com.spotlightsix.zentimerlite2" to AppMetadata(
            packageName = "com.spotlightsix.zentimerlite2",
            confidenceMultiplier = 0.93f
        ),
        "com.elevatelabs.geonosis" to AppMetadata(
            packageName = "com.elevatelabs.geonosis",
            confidenceMultiplier = 0.93f
        ),
        "org.wakingup.android" to AppMetadata(
            packageName = "org.wakingup.android",
            confidenceMultiplier = 0.93f
        ),
        "com.smilingmind.app" to AppMetadata(
            packageName = "com.smilingmind.app",
            confidenceMultiplier = 0.92f
        ),
        "com.tenpercent.tph" to AppMetadata(
            packageName = "com.tenpercent.tph",
            confidenceMultiplier = 0.93f
        ),
        "jp.relook.meisoon" to AppMetadata(
            packageName = "jp.relook.meisoon",
            confidenceMultiplier = 0.90f
        ),
        "meditation.relax.sleep.sounds" to AppMetadata(
            packageName = "meditation.relax.sleep.sounds",
            confidenceMultiplier = 0.93f
        ),
        "com.mindvalley.mva" to AppMetadata(
            packageName = "com.mindvalley.mva",
            confidenceMultiplier = 0.85f
        )
    )

}
