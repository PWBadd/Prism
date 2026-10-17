// Glassbox — author: PWBad's | planning: Claude (Anthropic) | code: DeepSeek
package com.shtuchki.glassbox.ui

import android.content.Context
import android.graphics.Bitmap
import android.util.LruCache
import android.util.Size
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Movie
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.palette.graphics.Palette
import com.shtuchki.glassbox.data.Track
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.ConcurrentHashMap

/** Кэш обложек в памяти (до ~24 МБ). */
object CoverCache {
    private val cache = object : LruCache<String, Bitmap>(24 * 1024 * 1024) {
        override fun sizeOf(key: String, value: Bitmap): Int = value.byteCount
    }

    private fun key(track: Track, px: Int) = "${track.mediaId}@$px"

    fun peek(track: Track, px: Int): Bitmap? = cache.get(key(track, px))

    suspend fun load(context: Context, track: Track, px: Int): Bitmap? = withContext(Dispatchers.IO) {
        val k = key(track, px)
        cache.get(k)?.let { return@withContext it }
        val raw = try {
            context.contentResolver.loadThumbnail(track.uri, Size(px, px), null)
        } catch (e: Exception) {
            null
        }
        val bmp = raw?.let {
            if (it.config == Bitmap.Config.HARDWARE) it.copy(Bitmap.Config.ARGB_8888, false) else it
        }
        if (bmp != null) cache.put(k, bmp)
        bmp
    }
}

/** Цвет обложки (для «темы под обложку»). Считается один раз на трек. */
object AccentCache {
    private val map = ConcurrentHashMap<String, Int>()

    suspend fun get(context: Context, track: Track): Color? {
        map[track.mediaId]?.let { return if (it == 0) null else Color(it) }
        val bmp = CoverCache.load(context, track, 256)
        val rgb = if (bmp == null) {
            0
        } else {
            withContext(Dispatchers.Default) {
                val p = Palette.from(bmp).maximumColorCount(16).generate()
                p.vibrantSwatch?.rgb ?: p.dominantSwatch?.rgb ?: 0
            }
        }
        map[track.mediaId] = rgb
        return if (rgb == 0) null else Color(rgb)
    }
}

/** Обложка трека (или красивая заглушка). */
@Composable
fun CoverImage(
    track: Track?,
    sizePx: Int,
    shape: Shape,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val key = track?.mediaId
    var bitmap by remember(key, sizePx) {
        mutableStateOf(if (track != null) CoverCache.peek(track, sizePx) else null)
    }
    LaunchedEffect(key, sizePx) {
        if (track != null && bitmap == null) bitmap = CoverCache.load(context, track, sizePx)
    }
    val scheme = MaterialTheme.colorScheme
    val image = remember(bitmap) { bitmap?.asImageBitmap() }
    Box(
        modifier = modifier
            .clip(shape)
            .background(
                Brush.linearGradient(
                    listOf(scheme.primary.copy(alpha = 0.50f), scheme.secondary.copy(alpha = 0.32f))
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        if (image != null) {
            Image(
                bitmap = image,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            Icon(
                imageVector = if (track?.isVideo == true) Icons.Rounded.Movie else Icons.Rounded.MusicNote,
                contentDescription = null,
                tint = Color.White.copy(alpha = 0.9f),
                modifier = Modifier.fillMaxSize(0.45f)
            )
        }
    }
}
