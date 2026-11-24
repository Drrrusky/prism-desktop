package com.prism.music.data.model

import kotlin.concurrent.Volatile

/**
 * The words Models.kt shows, from the platform (Windows port): Android reads them from its resources, Windows from its
 * own tables. Nothing here knows a platform, so this file moves to the shared module as it is.
 */
interface ModelText {
    /** YouTube's English kind ("Album", "EP"...) in the listener's language, or null for a kind Prism doesn't name. */
    fun kind(kind: String): String?

    /** "1 song", "12 songs". */
    fun songCount(n: Int): String

    companion object {
        @Volatile private var installed: ModelText? = null

        /** What Models.kt reads: what the app installed at start, else the platform's own default (so code that runs before, or without, the app's start still gets words). */
        val current: ModelText get() = installed ?: platformDefaultModelText()

        /** The app's explicit choice at start; null goes back to the platform's default (a test's reset). */
        fun install(text: ModelText?) {
            installed = text
        }
    }
}

// The platform's ModelText when none was installed, declared once per platform: today's single module has it as a plain
// function (ModelTextAndroid.kt); in the shared module this becomes `internal expect fun platformDefaultModelText(): ModelText`
// here and an `actual` in each platform's source set (same package).
