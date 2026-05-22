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
        AppMetadata("com.ichi2.anki", 0.92f), // AnkiDroid Flashcards
        AppMetadata("com.babbel.mobile.android.en", 0.82f), // Babbel
        AppMetadata("com.david.android.languageswitch", 0.90f), // Beelinguapp
        AppMetadata("com.busuu.android.enc", 0.88f), // Busuu
        AppMetadata("com.cambly.cambly", 0.86f), // Cambly
        AppMetadata("com.clozemaster.v2", 0.96f), // Clozemaster
        AppMetadata(
            "com.languagedrops.drops.kids.letters.fun.write.words.learn.abc.alphabet.chinese.english.french.droplets",
            0.94f
        ), // Droplets: Kids Language Learning
        AppMetadata(
            "com.languagedrops.drops.international",
            0.90f
        ), // Drops: Language Learning Games (all languages)
        AppMetadata(
            "com.languagedrops.drops.learn.learning.speak.language.english.american.us.usa.words",
            0.92f
        ), // Drops: Learn American English
        AppMetadata(
            "com.languagedrops.drops.learn.learning.speak.language.arabic.words",
            0.90f
        ), // Drops: Learn Arabic
        AppMetadata(
            "com.languagedrops.drops.learn.learning.speak.language.portuguese.brazilian.words",
            0.92f
        ), // Drops: Learn Brazilian Portuguese
        AppMetadata(
            "com.languagedrops.drops.learn.learning.speak.language.english.british.gb.words",
            0.90f
        ), // Drops: Learn British English
        AppMetadata(
            "com.languagedrops.drops.learn.learning.speak.language.chinese.cantonese.hanzi.pinyin.words",
            0.90f
        ), // Drops: Learn Cantonese Chinese
        AppMetadata(
            "com.languagedrops.drops.learn.learning.speak.language.dutch.words",
            0.92f
        ), // Drops: Learn Dutch
        AppMetadata(
            "com.languagedrops.drops.learn.learning.speak.language.estonian.words",
            0.96f
        ), // Drops: Learn Estonian
        AppMetadata(
            "com.languagedrops.drops.learn.learning.speak.language.portuguese.european.words",
            0.92f
        ), // Drops: Learn European Portuguese
        AppMetadata(
            "com.languagedrops.drops.learn.learning.speak.language.spanish.castilian.words",
            0.92f
        ), // Drops: Learn European Spanish
        AppMetadata(
            "com.languagedrops.drops.learn.learning.speak.language.finnish.finland.words",
            0.92f
        ), // Drops: Learn Finnish
        AppMetadata(
            "com.languagedrops.drops.learn.learning.speak.language.french.words",
            0.92f
        ), // Drops: Learn French
        AppMetadata(
            "com.languagedrops.drops.learn.learning.speak.language.german.words",
            0.90f
        ), // Drops: Learn German
        AppMetadata(
            "com.languagedrops.drops.learn.learning.speak.language.el.greek.greece.words",
            0.90f
        ), // Drops: Learn Greek
        AppMetadata(
            "com.languagedrops.drops.learn.learning.speak.language.hebrew.words",
            0.92f
        ), // Drops: Learn Hebrew
        AppMetadata(
            "com.languagedrops.drops.learn.learning.speak.language.hungarian.words",
            0.90f
        ), // Drops: Learn Hungarian
        AppMetadata(
            "com.languagedrops.drops.learn.learning.speak.language.icelandic.words",
            0.92f
        ), // Drops: Learn Icelandic
        AppMetadata(
            "com.languagedrops.drops.learn.learning.speak.language.italian.words",
            0.90f
        ), // Drops: Learn Italian
        AppMetadata(
            "com.languagedrops.drops.learn.learning.speak.language.japanese.kanji.katakana.hiragana.romaji.words",
            0.92f
        ), // Drops: Learn Japanese
        AppMetadata(
            "com.languagedrops.drops.learn.learning.speak.language.korean.hangul.words",
            0.94f
        ), // Drops: Learn Korean
        AppMetadata(
            "com.languagedrops.drops.learn.learning.speak.language.chinese.mandarin.hanzi.pinyin.words",
            0.92f
        ), // Drops: Learn Mandarin Chinese
        AppMetadata(
            "com.languagedrops.drops.learn.learning.speak.language.norwegian.words",
            0.92f
        ), // Drops: Learn Norwegian
        AppMetadata(
            "com.languagedrops.drops.learn.learning.speak.language.swedish.words",
            0.92f
        ), // Drops: Learn Swedish
        AppMetadata(
            "com.languagedrops.drops.learn.learning.speak.language.tagalog.filipino.words",
            0.92f
        ), // Drops: Learn Tagalog
        AppMetadata(
            "com.languagedrops.drops.learn.learning.speak.language.thai.words",
            0.90f
        ), // Drops: Learn Thai
        AppMetadata(
            "com.languagedrops.drops.learn.learning.speak.language.turkish.words",
            0.92f
        ), // Drops: Learn Turkish
        AppMetadata("com.duolingo", 0.90f), // Duolingo
        AppMetadata("com.ef.efhello", 0.95f), // EF Hello: Language Learning
        AppMetadata("us.nobarriers.elsa", 0.92f), // ELSA Speak
        AppMetadata("com.funeasylearn.ukrainian", 0.88f), // FunEasyLearn: Learn Ukrainian
        AppMetadata("com.glossika.ai", 0.86f), // Glossika
        AppMetadata("com.hellotalk", 0.86f), // HelloTalk
        AppMetadata("com.lang8.hinative", 0.88f), // HiNative
        AppMetadata("com.italki.app", 0.84f), // italki
        AppMetadata(
            "com.languagelearning.tutor.speakfluently",
            0.86f
        ), // Learn Fast & Speak Fluently
        AppMetadata("ru.ipartner.lingo", 0.80f), // Learn Languages — LinGo Play
        AppMetadata("com.lingo.play.ukrainian", 0.76f), // Learn Ukrainian — LinGo Play
        AppMetadata("com.luvlingua.learnukrainian", 0.88f), // Learn Ukrainian Language (LuvLingua)
        AppMetadata("com.lingodeer", 0.92f), // LingoDeer
        AppMetadata("com.lingopie.android.stg", 0.86f), // Lingopie
        AppMetadata("com.linguist", 0.86f), // LingQ
        AppMetadata("io.lingvist.android", 0.84f), // Lingvist
        AppMetadata("com.memrise.android.memrisecompanion", 0.88f), // Memrise
        AppMetadata("com.atistudios.mondly.languages", 0.92f), // Mondly
        AppMetadata("com.mosalingua.enfree", 0.92f), // MosaLingua
        AppMetadata("com.simonandschuster.pimsleur.unified.android", 0.90f), // Pimsleur
        AppMetadata("com.preply", 0.88f), // Preply
        AppMetadata("com.quizlet.quizletandroid", 0.90f), // Quizlet
        AppMetadata("com.renshuu.renshuu_org", 0.96f), // renshuu — Japanese learning
        AppMetadata("air.com.rosettastone.mobile.CoursePlayer", 0.78f), // Rosetta Stone
        AppMetadata("com.owlab.speakly", 0.87f), // Speakly
        AppMetadata("ai.talkpal", 0.86f), // Talkpal — AI Language Learning
    ).associateBy { it.packageName }

    val reading: Map<String, AppMetadata> = listOf(
        AppMetadata("com.abuk", 0.88f), // Абук (Abuk)
        AppMetadata("com.amazon.kindle", 0.82f), // Amazon Kindle
        AppMetadata("com.audible.application", 0.90f), // Audible
        AppMetadata("com.audioteka", 0.84f), // Audioteka
        AppMetadata("com.blinkslabs.blinkist.android", 0.86f), // Blinkist
        AppMetadata("com.bookmate", 0.86f), // Bookmate
        AppMetadata("ua.booknet", 0.76f), // Booknet
        AppMetadata("com.reader.books", 0.94f), // eBoox
        AppMetadata("com.empik.empikgo", 0.84f), // Empik Go
        AppMetadata("com.prestigio.ereader", 0.78f), // eReader Prestigio
        AppMetadata("com.scribd.app.reader0", 0.88f), // Everand
        AppMetadata("org.geometerplus.zlibrary.ui.android", 0.80f), // FBReader
        AppMetadata("com.fullreader", 0.82f), // FullReader
        AppMetadata("com.colt", 0.86f), // Galatea
        AppMetadata("com.google.android.apps.books", 0.80f), // Google Play Books
        AppMetadata("com.kobobooks.android", 0.88f), // Kobo
        AppMetadata("com.overdrive.mobile.android.libby", 0.94f), // Libby
        AppMetadata("app.librivox.android", 0.82f), // LibriVox
        AppMetadata("com.flyersoft.moonreader", 0.86f), // Moon+ Reader
        AppMetadata("com.obreey.reader", 0.86f), // PocketBook Reader
        AppMetadata("org.readera", 0.94f), // ReadEra
        AppMetadata("ak.alizandro.smartaudiobookplayer", 0.92f), // Smart AudioBook Player
        AppMetadata("com.sonnar.sonnarlibrary", 0.90f), // Sonnar Ukrainian Library
        AppMetadata("grit.storytel.app", 0.88f), // Storytel
        AppMetadata("com.cherkaskyi.underbooks", 0.92f), // UnderBooks
        AppMetadata("wp.wattpad", 0.86f), // Wattpad
        AppMetadata("ua.yakaboo", 0.86f), // Якабу (Yakaboo)
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
