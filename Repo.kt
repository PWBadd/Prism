// Prism — author: PWBad's | planning: Claude (Anthropic) | code: DeepSeek
package com.shtuchki.glassbox.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.File

@Serializable
data class Playlist(
    val id: String,
    val name: String,
    val trackIds: List<String> = emptyList(),
    val bannerFile: String? = null
)

/** Тема: цвета хранятся как ARGB в Long (0xAARRGGBB). */
@Serializable
data class ThemeData(
    val id: String,
    val name: String,
    val nameRu: String = "",
    val isDark: Boolean,
    val background: Long,
    val surface: Long,
    val accent: Long,
    val text: Long,
    val textSecondary: Long,
    val glassTint: Long,
    val glassBorder: Long
) {
    fun displayName(lang: String): String = if (lang == "ru" && nameRu.isNotBlank()) nameRu else name
}

@Serializable
data class AppData(
    val favorites: List<String> = emptyList(),
    val playlists: List<Playlist> = emptyList(),
    val playCounts: Map<String, Int> = emptyMap(),
    val lastPlayed: Map<String, Long> = emptyMap(),
    val recents: List<String> = emptyList(),
    val customThemes: List<ThemeData> = emptyList(),
    val themeMode: String = "system",
    val darkThemeId: String = "dark_amethyst",
    val lightThemeId: String = "light_lilac",
    val themeFromArt: Boolean = true,
    val crossfadeSec: Int = 4,
    val fadeOnPause: Boolean = true,
    val perfMode: String = "auto",
    val userName: String = "PWBad",
    val avatarFile: String? = null
)

/** Всё, что приложение помнит: плейлисты, избранное, темы, настройки. Хранится в одном JSON-файле. */
object Repo {
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }
    private var dir: File? = null
    private val _data = MutableStateFlow(AppData())
    val data: StateFlow<AppData> = _data.asStateFlow()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var saveJob: Job? = null

    fun init(context: Context) {
        if (dir != null) return
        val d = context.applicationContext.filesDir
        dir = d
        val f = File(d, "prism.json")
        if (f.exists()) {
            runCatching { _data.value = json.decodeFromString(AppData.serializer(), f.readText()) }
        }
    }

    fun update(block: (AppData) -> AppData) {
        _data.update(block)
        scheduleSave()
    }

    private fun scheduleSave() {
        val d = dir ?: return
        saveJob?.cancel()
        saveJob = scope.launch {
            delay(300)
            runCatching {
                File(d, "prism.json").writeText(json.encodeToString(AppData.serializer(), _data.value))
            }
        }
    }

    // ---------- избранное ----------
    fun toggleFavorite(id: String) = update { d ->
        d.copy(favorites = if (id in d.favorites) d.favorites - id else listOf(id) + d.favorites)
    }

    // ---------- плейлисты ----------
    fun createPlaylist(name: String): String {
        val id = "pl_" + System.currentTimeMillis()
        update { it.copy(playlists = it.playlists + Playlist(id, name)) }
        return id
    }

    fun renamePlaylist(id: String, name: String) = update { d ->
        d.copy(playlists = d.playlists.map { if (it.id == id) it.copy(name = name) else it })
    }

    fun deletePlaylist(id: String) = update { d ->
        d.copy(playlists = d.playlists.filter { it.id != id })
    }

    fun addToPlaylist(id: String, trackId: String) = update { d ->
        d.copy(playlists = d.playlists.map {
            if (it.id == id && trackId !in it.trackIds) it.copy(trackIds = it.trackIds + trackId) else it
        })
    }

    fun removeFromPlaylist(id: String, trackId: String) = update { d ->
        d.copy(playlists = d.playlists.map {
            if (it.id == id) it.copy(trackIds = it.trackIds - trackId) else it
        })
    }

    fun moveInPlaylist(id: String, trackId: String, delta: Int) = update { d ->
        d.copy(playlists = d.playlists.map { p ->
            if (p.id != id) return@map p
            val list = p.trackIds.toMutableList()
            val from = list.indexOf(trackId)
            val to = from + delta
            if (from < 0 || to < 0 || to >= list.size) return@map p
            list.removeAt(from)
            list.add(to, trackId)
            p.copy(trackIds = list)
        })
    }

    fun setBanner(id: String, file: String) = update { d ->
        d.copy(playlists = d.playlists.map { if (it.id == id) it.copy(bannerFile = file) else it })
    }

    // ---------- темы ----------
    fun upsertTheme(t: ThemeData) = update { d ->
        val exists = d.customThemes.any { it.id == t.id }
        d.copy(customThemes = if (exists) d.customThemes.map { if (it.id == t.id) t else it } else d.customThemes + t)
    }

    fun deleteTheme(id: String) = update { d ->
        d.copy(
            customThemes = d.customThemes.filter { it.id != id },
            darkThemeId = if (d.darkThemeId == id) "dark_amethyst" else d.darkThemeId,
            lightThemeId = if (d.lightThemeId == id) "light_lilac" else d.lightThemeId
        )
    }

    fun selectTheme(t: ThemeData) = update { d ->
        val d2 = if (t.isDark) d.copy(darkThemeId = t.id) else d.copy(lightThemeId = t.id)
        if (d.themeMode != "system" && ((d.themeMode == "dark") != t.isDark)) {
            d2.copy(themeMode = if (t.isDark) "dark" else "light")
        } else d2
    }

    fun themeToText(t: ThemeData): String = "PRISM_THEME:" + json.encodeToString(ThemeData.serializer(), t)

    /** Читает тему из текста; возвращает сохранённую тему или null. */
    fun importThemeText(text: String): ThemeData? = try {
        val body = text.trim().removePrefix("PRISM_THEME:").trim()
        val t = json.decodeFromString(ThemeData.serializer(), body)
        val fresh = t.copy(id = "custom_" + System.currentTimeMillis())
        upsertTheme(fresh)
        selectTheme(fresh)
        fresh
    } catch (e: Exception) {
        null
    }

    // ---------- события воспроизведения ----------
    fun recordPlay(id: String) = update { d ->
        d.copy(
            playCounts = d.playCounts + (id to ((d.playCounts[id] ?: 0) + 1)),
            lastPlayed = d.lastPlayed + (id to System.currentTimeMillis()),
            recents = (listOf(id) + d.recents.filter { it != id }).take(100)
        )
    }

    // ---------- резервная копия ----------
    fun exportJson(): String = json.encodeToString(AppData.serializer(), _data.value)

    fun importJson(text: String): Boolean = try {
        _data.value = json.decodeFromString(AppData.serializer(), text)
        scheduleSave()
        true
    } catch (e: Exception) {
        false
    }

    // ---------- картинки (баннеры, аватар) ----------
    fun saveImage(context: Context, uri: Uri, prefix: String): String? = try {
        val folder = File(context.filesDir, "images").apply { mkdirs() }
        val name = prefix + "_" + System.currentTimeMillis() + ".jpg"
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
        var sample = 1
        while (bounds.outWidth / sample > 1600) sample *= 2
        val bmp = context.contentResolver.openInputStream(uri)?.use {
            BitmapFactory.decodeStream(it, null, BitmapFactory.Options().apply { inSampleSize = sample })
        }
        if (bmp == null) null else {
            File(folder, name).outputStream().use { bmp.compress(Bitmap.CompressFormat.JPEG, 90, it) }
            name
        }
    } catch (e: Exception) {
        null
    }

    fun imageFile(context: Context, name: String?): File? =
        if (name == null) null else File(File(context.filesDir, "images"), name)
}
