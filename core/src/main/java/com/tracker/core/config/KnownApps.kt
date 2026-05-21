package com.tracker.core.config

/**
 * Static configuration of known apps for habit tracking.
 *
 * Each app has:
 * - Package name for identification
 * - Confidence multiplier (base confidence when detected)
 */
object KnownApps {

    val languageLearning: Map<String, AppMetadata> = listOf(
        AppMetadata("com.mindtwisted.kanjistudy", 0.85f),
        AppMetadata("com.lulilanguages.j5a", 0.83f),
        AppMetadata("com.ichi2.anki", 0.85f),
        AppMetadata("com.eup.mytest", 0.80f),
        AppMetadata("mobi.eup.jpnews", 0.75f),
        AppMetadata("com.duolingo", 0.80f),
        AppMetadata("com.eup.heyjapan", 0.80f),
        AppMetadata("com.appsci.tenwords", 0.83f),
        AppMetadata("com.languagedrops.drops.international", 0.82f),
        AppMetadata("com.languagedrops.drops.learn.learning.speak.language.japanese.kanji.katakana.hiragana.romaji.words", 0.82f),
        AppMetadata("com.lingodeer", 0.83f),
        AppMetadata("com.lulilanguages.j5KjAnd", 0.83f),
        AppMetadata("com.renshuu.renshuu_org", 0.84f),
    ).associateBy { it.packageName }

    val reading: Map<String, AppMetadata> = listOf(
        AppMetadata("com.google.android.apps.books", 0.80f),
        AppMetadata("com.amazon.kindle", 0.82f),
    ).associateBy { it.packageName }

    val socialMedia: Map<String, AppMetadata> = listOf(
        AppMetadata("com.facebook.katana", 0.95f),
        AppMetadata("com.instagram.android", 0.95f),
        AppMetadata("com.twitter.android", 0.90f),
        AppMetadata("com.zhiliaoapp.musically", 0.95f),
        AppMetadata("com.snapchat.android", 0.90f),
        AppMetadata("com.linkedin.android", 0.85f),
        AppMetadata("com.reddit.frontpage", 0.85f),
        AppMetadata("com.pinterest", 0.80f),
        AppMetadata("com.whatsapp", 0.75f),
        AppMetadata("org.telegram.messenger", 0.75f),
        AppMetadata("com.discord", 0.80f),
        AppMetadata("com.instagram.barcelona", 0.90f),
        AppMetadata("org.joinmastodon.android", 0.85f),
        AppMetadata("xyz.blueskyweb.app", 0.85f),
        AppMetadata("com.tumblr", 0.80f),
    ).associateBy { it.packageName }

    /**
     * Known meditation apps. Pure meditation apps (Calm, Headspace, Insight Timer, Balance,
     * Waking Up, Smiling Mind, Ten Percent Happier, Medito, MEISOON) score 0.90–0.95 because
     * foreground time almost always maps to an active meditation session. Mindvalley is broader
     * than meditation, so its confidence is lower.
     */
    val meditation: Map<String, AppMetadata> = listOf(
        AppMetadata("com.calm.android", 0.95f),
        AppMetadata("com.getsomeheadspace.android", 0.95f),
        AppMetadata("com.spotlightsix.zentimerlite2", 0.93f),
        AppMetadata("com.elevatelabs.geonosis", 0.93f),
        AppMetadata("org.wakingup.android", 0.93f),
        AppMetadata("com.smilingmind.app", 0.92f),
        AppMetadata("com.tenpercent.tph", 0.93f),
        AppMetadata("jp.relook.meisoon", 0.90f),
        AppMetadata("meditation.relax.sleep.sounds", 0.93f),
        AppMetadata("com.mindvalley.mva", 0.85f),
    ).associateBy { it.packageName }

}
