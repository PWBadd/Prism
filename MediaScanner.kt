// Prism — author: PWBad's | planning: Claude (Anthropic) | code: DeepSeek
package com.shtuchki.glassbox.data

import android.content.ContentUris
import android.content.Context
import android.provider.MediaStore

/** Сканирует аудио и видео устройства через MediaStore. */
object MediaScanner {

    private fun clean(tag: String?): String =
        if (tag == null || tag == "<unknown>") "" else tag

    fun scanAudio(context: Context): List<Track> {
        val result = ArrayList<Track>()
        val collection = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
        val projection = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.ALBUM,
            MediaStore.Audio.Media.DURATION,
            MediaStore.Audio.Media.DATE_ADDED
        )
        val selection = "${MediaStore.Audio.Media.IS_RINGTONE} = 0 AND " +
            "${MediaStore.Audio.Media.IS_ALARM} = 0 AND " +
            "${MediaStore.Audio.Media.IS_NOTIFICATION} = 0 AND " +
            "${MediaStore.Audio.Media.DURATION} >= 5000"

        context.contentResolver.query(collection, projection, selection, null, null)?.use { c ->
            val idCol = c.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
            val titleCol = c.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
            val artistCol = c.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
            val albumCol = c.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM)
            val durCol = c.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
            val dateCol = c.getColumnIndexOrThrow(MediaStore.Audio.Media.DATE_ADDED)
            while (c.moveToNext()) {
                val id = c.getLong(idCol)
                result.add(
                    Track(
                        id = id,
                        uri = ContentUris.withAppendedId(collection, id),
                        title = clean(c.getString(titleCol)),
                        artist = clean(c.getString(artistCol)),
                        album = clean(c.getString(albumCol)),
                        durationMs = c.getLong(durCol),
                        isVideo = false,
                        dateAdded = c.getLong(dateCol)
                    )
                )
            }
        }
        result.sortBy { it.title.lowercase() }
        return result
    }

    fun scanVideo(context: Context): List<Track> {
        val result = ArrayList<Track>()
        val collection = MediaStore.Video.Media.EXTERNAL_CONTENT_URI
        val projection = arrayOf(
            MediaStore.Video.Media._ID,
            MediaStore.Video.Media.TITLE,
            MediaStore.Video.Media.DURATION,
            MediaStore.Video.Media.DATE_ADDED
        )
        val selection = "${MediaStore.Video.Media.DURATION} >= 1000"

        context.contentResolver.query(collection, projection, selection, null, null)?.use { c ->
            val idCol = c.getColumnIndexOrThrow(MediaStore.Video.Media._ID)
            val titleCol = c.getColumnIndexOrThrow(MediaStore.Video.Media.TITLE)
            val durCol = c.getColumnIndexOrThrow(MediaStore.Video.Media.DURATION)
            val dateCol = c.getColumnIndexOrThrow(MediaStore.Video.Media.DATE_ADDED)
            while (c.moveToNext()) {
                val id = c.getLong(idCol)
                result.add(
                    Track(
                        id = id,
                        uri = ContentUris.withAppendedId(collection, id),
                        title = clean(c.getString(titleCol)),
                        artist = "",
                        album = "",
                        durationMs = c.getLong(durCol),
                        isVideo = true,
                        dateAdded = c.getLong(dateCol)
                    )
                )
            }
        }
        result.sortBy { it.title.lowercase() }
        return result
    }
}
