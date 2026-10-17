// Prism — author: PWBad's | planning: Claude (Anthropic) | code: DeepSeek
package com.shtuchki.glassbox.player

import android.content.Intent
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.ForwardingPlayer
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import com.shtuchki.glassbox.data.Repo
import com.shtuchki.glassbox.perf.PerformanceManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * Плеер-обёртка с плавными затуханиями:
 *  - пауза/старт: громкость плавно падает/растёт;
 *  - конец трека: затухает, новый трек плавно нарастает (как в VK Music);
 *  - таймер сна: долгое затухание и пауза.
 */
class FadingPlayer(private val exo: ExoPlayer) : ForwardingPlayer(exo) {

    private val handler = Handler(Looper.getMainLooper())
    private var fadeRunnable: Runnable? = null
    private var endFading = false

    init {
        exo.addListener(object : Player.Listener {
            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                if (endFading) {
                    endFading = false
                    ramp(exo.volume, 1f, (crossfadeMs() / 2).coerceAtLeast(500L))
                }
            }
        })
    }

    private fun crossfadeMs(): Long {
        var ms = Repo.data.value.crossfadeSec * 1000L
        if (PerformanceManager.lite.value) ms = minOf(ms, 2000L)
        return ms
    }

    fun cancelFade() {
        fadeRunnable?.let { handler.removeCallbacks(it) }
        fadeRunnable = null
    }

    private fun ramp(from: Float, to: Float, durationMs: Long, onDone: () -> Unit = {}) {
        cancelFade()
        if (durationMs <= 0L) {
            exo.volume = to
            onDone()
            return
        }
        val start = SystemClock.uptimeMillis()
        val r = object : Runnable {
            override fun run() {
                val t = ((SystemClock.uptimeMillis() - start).toFloat() / durationMs).coerceIn(0f, 1f)
                exo.volume = from + (to - from) * t
                if (t < 1f) {
                    handler.postDelayed(this, 30)
                } else {
                    fadeRunnable = null
                    onDone()
                }
            }
        }
        fadeRunnable = r
        handler.post(r)
    }

    private fun fadeEnabled() = Repo.data.value.fadeOnPause

    override fun play() {
        if (fadeEnabled()) {
            exo.volume = 0f
            exo.play()
            ramp(0f, 1f, 350)
        } else {
            cancelFade()
            exo.volume = 1f
            exo.play()
        }
    }

    override fun pause() {
        if (fadeEnabled() && exo.isPlaying) {
            ramp(exo.volume, 0f, 300) {
                exo.pause()
                exo.volume = 1f
            }
        } else {
            cancelFade()
            exo.pause()
            exo.volume = 1f
        }
    }

    override fun setPlayWhenReady(playWhenReady: Boolean) {
        if (playWhenReady) play() else pause()
    }

    /** Вызывается каждые ~150 мс, пока играет музыка: затухание в конце трека. */
    fun tick() {
        if (!exo.isPlaying || fadeRunnable != null) return
        val fadeMs = crossfadeMs()
        if (fadeMs <= 0L) {
            if (endFading) {
                endFading = false
                exo.volume = 1f
            }
            return
        }
        val duration = exo.duration
        if (duration == C.TIME_UNSET || duration < fadeMs * 3) return
        val remaining = duration - exo.currentPosition
        if (remaining in 0..fadeMs) {
            endFading = true
            exo.volume = (remaining.toFloat() / fadeMs).coerceIn(0f, 1f)
        } else if (endFading) {
            endFading = false
            exo.volume = 1f
        }
    }

    /** Таймер сна: долгое затухание и пауза. */
    fun fadeOutAndPause() {
        ramp(exo.volume, 0f, 4000) {
            exo.pause()
            exo.volume = 1f
        }
    }
}

/**
 * Сервис воспроизведения. Держит ExoPlayer и MediaSession:
 * музыка играет в фоне, есть уведомление и управление с экрана блокировки.
 */
class PlaybackService : MediaSessionService() {

    private var mediaSession: MediaSession? = null
    private var fading: FadingPlayer? = null
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    override fun onCreate() {
        super.onCreate()
        val exo = ExoPlayer.Builder(this)
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(C.USAGE_MEDIA)
                    .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
                    .build(),
                true
            )
            .setHandleAudioBecomingNoisy(true)
            .build()
        val fp = FadingPlayer(exo)
        fading = fp
        mediaSession = MediaSession.Builder(this, fp).build()

        scope.launch {
            while (isActive) {
                delay(150)
                fp.tick()
            }
        }
        scope.launch {
            Engine.sleepEndAt.collectLatest { end ->
                if (end != null) {
                    val wait = end - System.currentTimeMillis()
                    if (wait > 0) delay(wait)
                    fp.fadeOutAndPause()
                    Engine.sleepEndAt.value = null
                }
            }
        }
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? = mediaSession

    override fun onTaskRemoved(rootIntent: Intent?) {
        val player = mediaSession?.player
        if (player == null || !player.playWhenReady || player.mediaItemCount == 0) {
            stopSelf()
        }
    }

    override fun onDestroy() {
        scope.cancel()
        fading?.cancelFade()
        mediaSession?.run {
            player.release()
            release()
        }
        mediaSession = null
        super.onDestroy()
    }
}
