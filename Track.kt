// Prism — author: PWBad's | planning: Claude (Anthropic) | code: DeepSeek
package com.shtuchki.glassbox.data

import android.net.Uri
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata

/** Один трек или видео из библиотеки устройства. */
data class Track(
    val id: Long,
    val uri: Uri,
    val title: String,
    val artist: String,
    val album: String,
    val durationMs: Long,
    val isVideo: Boolean,
    val dateAdded: Long = 0L
) {
    val mediaId: String get() = (if (isVideo) "video:" else "audio:") + id
}

fun Track.toMediaItem(): MediaItem {
    val meta = MediaMetadata.Builder()
        .setTitle(title)
        .setArtist(artist.takeIf { it.isNotBlank() })
        .setAlbumTitle(album.takeIf { it.isNotBlank() })
        .build()
    return MediaItem.Builder()
        .setMediaId(mediaId)
        .setUri(uri)
        .setMediaMetadata(meta)
        .build()
}
