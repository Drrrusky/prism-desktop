package com.prism.music.data.model

import com.prism.music.R
import com.prism.music.tr
import com.prism.music.trp

/**
 * [ModelText] on Android: the resources and translations Models.kt read directly before, unchanged. Installed by
 * PrismApp.onCreate; also the default, so unit tests that never start the app still get Strings' English fallback.
 * Moves to androidMain with the shared module (same package).
 */
object AndroidModelText : ModelText {
    override fun kind(kind: String): String? = when (kind) {
        "Song" -> R.string.kind_song
        "Video" -> R.string.kind_video
        "Album" -> R.string.kind_album
        "Single" -> R.string.kind_single
        "EP" -> R.string.kind_ep
        "Artist" -> R.string.kind_artist
        "Playlist" -> R.string.kind_playlist
        "Episode" -> R.string.kind_episode
        "Podcast" -> R.string.kind_podcast
        "Profile" -> R.string.kind_profile
        "Station" -> R.string.kind_station
        else -> null
    }?.let { tr(it) }

    override fun songCount(n: Int): String = trp(R.plurals.n_songs, n)
}

/** Android's default: becomes `actual` once [ModelText.kt]'s declaration is `expect`. */
internal fun platformDefaultModelText(): ModelText = AndroidModelText
