// Prism — author: PWBad's | planning: Claude (Anthropic) | code: DeepSeek
package com.shtuchki.glassbox.player

import android.Manifest
import android.app.Application
import android.content.ComponentName
import android.content.pm.PackageManager
import androidx.compose.ui.graphics.Color
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.C
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.shtuchki.glassbox.data.MediaScanner
import com.shtuchki.glassbox.data.Repo
import com.shtuchki.glassbox.data.Track
import com.shtuchki.glassbox.data.toMediaItem
import com.shtuchki.glassbox.ui.AccentCache
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class PlayerState(
    val mediaId: String? = null,
    val isPlaying: Boolean = false,
    val durationMs: Long = 0L,
    val shuffle: Boolean = false,
    val repeatMode: Int = Player.REPEAT_MODE_OFF,
    val speed: Float = 1f
)

class PlayerViewModel(private val app: Application) : AndroidViewModel(app) {

    private val _audio = MutableStateFlow<List<Track>>(emptyList())
    val audio: StateFlow<List<Track>> = _audio.asStateFlow()

    private val _video = MutableStateFlow<List<Track>>(emptyList())
    val video: StateFlow<List<Track>> = _video.asStateFlow()

    private val _loading = MutableStateFlow(false)
    val loading: StateFlow<Boolean> = _loading.asStateFlow()

    private val _hasAudioPermission = MutableStateFlow(granted(Manifest.permission.READ_MEDIA_AUDIO))
    val hasAudioPermission: StateFlow<Boolean> = _hasAudioPermission.asStateFlow()

    private val _state = MutableStateFlow(PlayerState())
    val state: StateFlow<PlayerState> = _state.asStateFlow()

    private val _position = MutableStateFlow(0L)
    val position: StateFlow<Long> = _position.asStateFlow()

    private val _player = MutableStateFlow<Player?>(null)
    val player: StateFlow<Player?> = _player.asStateFlow()

    // Навигация: стек экранов живёт здесь, поэтому переживает смену языка
    private val _nav = MutableStateFlow(listOf("lib"))
    val nav: StateFlow<List<String>> = _nav.asStateFlow()

    private val _abA = MutableStateFlow<Long?>(null)
    val abA: StateFlow<Long?> = _abA.asStateFlow()
    private val _abB = MutableStateFlow<Long?>(null)
    val abB: StateFlow<Long?> = _abB.asStateFlow()

    private val _audioOnly = MutableStateFlow(false)
    val audioOnly: StateFlow<Boolean> = _audioOnly.asStateFlow()

    private val _artAccent = MutableStateFlow<Color?>(null)
    /** Цвет обложки текущего трека (для «темы под обложку»). */
    val artAccent: StateFlow<Color?> = _artAccent.asStateFlow()

    val currentTrack: StateFlow<Track?> = combine(_state, _audio, _video) { s, a, v ->
        val id = s.mediaId
        if (id == null) null else (a.firstOrNull { it.mediaId == id } ?: v.firstOrNull { it.mediaId == id })
    }.stateIn(viewModelScope, SharingStarted.Eagerly, null)

    private var controller: MediaController? = null
    private var countedId: String? = null

    private val listener = object : Player.Listener {
        override fun onEvents(player: Player, events: Player.Events) {
            sync(player)
        }
    }

    init {
        val token = SessionToken(app, ComponentName(app, PlaybackService::class.java))
        val future = MediaController.Builder(app, token).buildAsync()
        future.addListener({
            val c = try {
                future.get()
            } catch (e: Exception) {
                null
            }
            if (c != null) {
                controller = c
                c.addListener(listener)
                _player.value = c
                sync(c)
            }
        }, ContextCompat.getMainExecutor(app))

        // Позиция и повтор A-B
        viewModelScope.launch {
            while (isActive) {
                val ab = _abA.value != null && _abB.value != null
                delay(if (ab) 100 else 250)
                val c = controller
                if (c != null && c.isPlaying) {
                    val pos = c.currentPosition
                    _position.value = pos
                    val a = _abA.value
                    val b = _abB.value
                    if (a != null && b != null && b > a && pos >= b) c.seekTo(a)
                }
            }
        }

        // Цвет обложки для темы (считается один раз на трек)
        viewModelScope.launch {
            currentTrack.collectLatest { t ->
                _artAccent.value = if (t == null || t.isVideo) null else AccentCache.get(app, t)
            }
        }
    }

    private fun granted(permission: String): Boolean =
        ContextCompat.checkSelfPermission(app, permission) == PackageManager.PERMISSION_GRANTED

    private fun sync(p: Player) {
        val id = p.currentMediaItem?.mediaId
        if (id != null && id != countedId && p.playbackState != Player.STATE_IDLE) {
            countedId = id
            Repo.recordPlay(id)
            clearAB()
        }
        _state.value = PlayerState(
            mediaId = id,
            isPlaying = p.isPlaying,
            durationMs = if (p.duration == C.TIME_UNSET) 0L else p.duration,
            shuffle = p.shuffleModeEnabled,
            repeatMode = p.repeatMode,
            speed = p.playbackParameters.speed
        )
        _position.value = p.currentPosition
    }

    // ---------- навигация ----------
    fun push(dest: String) {
        if (_nav.value.last() != dest) _nav.value = _nav.value + dest
    }

    fun pop() {
        if (_nav.value.size > 1) _nav.value = _nav.value.dropLast(1)
    }

    // ---------- библиотека ----------
    fun refreshLibrary() {
        val hasA = granted(Manifest.permission.READ_MEDIA_AUDIO)
        val hasV = granted(Manifest.permission.READ_MEDIA_VIDEO)
        _hasAudioPermission.value = hasA
        if (!hasA && !hasV) {
            _audio.value = emptyList()
            _video.value = emptyList()
            return
        }
        viewModelScope.launch {
            _loading.value = true
            val (a, v) = withContext(Dispatchers.IO) {
                val aa = if (hasA) runCatching { MediaScanner.scanAudio(app) }.getOrDefault(emptyList()) else emptyList()
                val vv = if (hasV) runCatching { MediaScanner.scanVideo(app) }.getOrDefault(emptyList()) else emptyList()
                aa to vv
            }
            _audio.value = a
            _video.value = v
            _loading.value = false
        }
    }

    // ---------- управление ----------
    fun playQueue(tracks: List<Track>, startIndex: Int, shuffle: Boolean? = null) {
        val c = controller ?: return
        if (tracks.isEmpty()) return
        if (shuffle != null) c.shuffleModeEnabled = shuffle
        c.setMediaItems(tracks.map { it.toMediaItem() }, startIndex.coerceIn(0, tracks.size - 1), 0L)
        c.prepare()
        c.play()
    }

    fun playNext(track: Track) {
        val c = controller ?: return
        if (c.mediaItemCount == 0) {
            playQueue(listOf(track), 0)
        } else {
            c.addMediaItem(c.currentMediaItemIndex + 1, track.toMediaItem())
        }
    }

    fun togglePlay() {
        val c = controller ?: return
        if (c.isPlaying) c.pause() else c.play()
    }

    fun next() {
        controller?.seekToNext()
    }

    fun previous() {
        controller?.seekToPrevious()
    }

    fun seekTo(ms: Long) {
        controller?.seekTo(ms)
        _position.value = ms
    }

    fun toggleShuffle() {
        val c = controller ?: return
        c.shuffleModeEnabled = !c.shuffleModeEnabled
    }

    fun cycleRepeat() {
        val c = controller ?: return
        c.repeatMode = when (c.repeatMode) {
            Player.REPEAT_MODE_OFF -> Player.REPEAT_MODE_ALL
            Player.REPEAT_MODE_ALL -> Player.REPEAT_MODE_ONE
            else -> Player.REPEAT_MODE_OFF
        }
    }

    fun setSpeed(speed: Float) {
        controller?.setPlaybackSpeed(speed)
    }

    fun setAudioOnly(on: Boolean) {
        val c = controller ?: return
        c.trackSelectionParameters = c.trackSelectionParameters.buildUpon()
            .setTrackTypeDisabled(C.TRACK_TYPE_VIDEO, on)
            .build()
        _audioOnly.value = on
    }

    // ---------- повтор A-B ----------
    fun markAB() {
        val pos = controller?.currentPosition ?: return
        when {
            _abA.value == null -> _abA.value = pos
            _abB.value == null && pos > (_abA.value ?: 0L) -> _abB.value = pos
        }
    }

    fun clearAB() {
        _abA.value = null
        _abB.value = null
    }

    // ---------- таймер сна ----------
    fun startSleepTimer(minutes: Int) {
        Engine.sleepEndAt.value = System.currentTimeMillis() + minutes * 60_000L
    }

    fun cancelSleepTimer() {
        Engine.sleepEndAt.value = null
    }

    // ---------- очередь ----------
    fun queue(): List<Pair<String, String>> {
        val c = controller ?: return emptyList()
        return (0 until c.mediaItemCount).map {
            val m = c.getMediaItemAt(it).mediaMetadata
            (m.title?.toString() ?: "") to (m.artist?.toString() ?: "")
        }
    }

    fun currentIndex(): Int = controller?.currentMediaItemIndex ?: 0

    fun playQueueIndex(i: Int) {
        controller?.seekToDefaultPosition(i)
        controller?.play()
    }

    override fun onCleared() {
        controller?.removeListener(listener)
        controller?.release()
        controller = null
        super.onCleared()
    }
}
