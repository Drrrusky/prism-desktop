

package com.prism.music

import com.prism.music.data.model.AlbumItem
import com.prism.music.data.model.ArtistItem
import com.prism.music.data.model.MoodItem
import com.prism.music.data.model.PlaylistItem
import com.prism.music.data.model.PodcastItem
import com.prism.music.data.model.Shelf
import com.prism.music.data.model.Song
import com.prism.music.data.model.SongItem
import com.prism.music.data.model.formatDuration
import com.prism.music.data.model.speedDialOrderLine
import org.junit.Assert.assertEquals
import org.junit.Test

class ModelsFormattingTest {

    @Test
    fun formatsDurationWithoutHours() {
        assertEquals("02:05", formatDuration(125))
    }

    @Test
    fun formatsDurationWithHours() {
        assertEquals("1:01:05", formatDuration(3665))
    }

    @Test
    fun returnsEmptyForZeroOrNegativeDuration() {
        assertEquals("", formatDuration(0))
        assertEquals("", formatDuration(-10))
    }

    @Test
    fun reportsAllSpeedDialItemKindsInOrder() {
        val items = listOf(
            SongItem(Song(id = "song", title = "Song")),
            AlbumItem("album", "Album", "", null),
            PlaylistItem("playlist", "Playlist", "", null),
            ArtistItem("artist", "Artist", "", null),
            PodcastItem("podcast", "Podcast", "", null),
            MoodItem("mood", "Mood", null, 0L),
        )
        val shelf = Shelf(title = "Speed dial", items = items)

        assertEquals(
            "; Speed dial's order: song album playlist artist podcast mood",
            listOf(shelf).speedDialOrderLine()
        )
    }

    @Test
    fun limitsSpeedDialOrderToTwelveItems() {
        val items = List(13) { index ->
            SongItem(Song(id = "song$index", title = "Song $index"))
        }
        val shelf = Shelf(title = "Speed dial", items = items)

        assertEquals(
            "; Speed dial's order: " + List(12) { "song" }.joinToString(" "),
            listOf(shelf).speedDialOrderLine()
        )
    }

    @Test
    fun returnsEmptyOrderWhenSpeedDialIsMissing() {
        assertEquals(
            "; Speed dial's order: ",
            emptyList<Shelf>().speedDialOrderLine()
        )
    }
}
