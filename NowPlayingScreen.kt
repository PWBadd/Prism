// Prism — author: PWBad's | planning: Claude (Anthropic) | code: DeepSeek
package com.shtuchki.glassbox.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.QueueMusic
import androidx.compose.material.icons.rounded.Repeat
import androidx.compose.material.icons.rounded.RepeatOne
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.StarBorder
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.ui.PlayerView
import com.shtuchki.glassbox.R
import com.shtuchki.glassbox.data.Repo
import com.shtuchki.glassbox.player.Engine
import com.shtuchki.glassbox.player.PlayerViewModel
import com.shtuchki.glassbox.ui.AddToPlaylistDialog
import com.shtuchki.glassbox.ui.CoverImage
import com.shtuchki.glassbox.ui.glass.AppBackground
import com.shtuchki.glassbox.ui.glass.GlassChip
import com.shtuchki.glassbox.ui.glass.GlassIconButton
import com.shtuchki.glassbox.util.formatTime
import java.text.DateFormat
import java.util.Date

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NowPlayingScreen(vm: PlayerViewModel) {
    val track by vm.currentTrack.collectAsState()
    val state by vm.state.collectAsState()
    val pos by vm.position.collectAsState()
    val player by vm.player.collectAsState()
    val abA by vm.abA.collectAsState()
    val abB by vm.abB.collectAsState()
    val audioOnly by vm.audioOnly.collectAsState()
    val sleepEnd by Engine.sleepEndAt.collectAsState()
    val data by Repo.data.collectAsState()
    val scheme = MaterialTheme.colorScheme
    val unknown = stringResource(R.string.unknown_artist)

    var menu by remember { mutableStateOf(false) }
    var showSpeed by remember { mutableStateOf(false) }
    var showSleep by remember { mutableStateOf(false) }
    var showQueue by remember { mutableStateOf(false) }
    var addTo by remember { mutableStateOf<String?>(null) }

    val t = track
    val isFav = t != null && t.mediaId in data.favorites
    val duration = (if (state.durationMs > 0) state.durationMs else (t?.durationMs ?: 0L)).coerceAtLeast(1L)
    var dragging by remember { mutableStateOf(false) }
    var dragValue by remember { mutableFloatStateOf(0f) }
    val shownPos = if (dragging) dragValue.toLong() else pos.coerceIn(0L, duration)
    val coverShape = RoundedCornerShape(30.dp)

    AppBackground(strength = 0.40f) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 22.dp, vertical = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Верхняя панель
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                GlassIconButton(Icons.Rounded.KeyboardArrowDown, stringResource(R.string.close_player), { vm.pop() })
                Spacer(Modifier.weight(1f))
                GlassIconButton(Icons.Rounded.Palette, stringResource(R.string.themes_title), { vm.push("themes") }, size = 44.dp)
                Spacer(Modifier.size(8.dp))
                Box {
                    GlassIconButton(Icons.Rounded.MoreVert, stringResource(R.string.more), { menu = true }, size = 44.dp)
                    DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.menu_add_playlist)) },
                            onClick = { menu = false; t?.let { addTo = it.mediaId } }
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.speed) + "  " + formatSpeed(state.speed)) },
                            onClick = { menu = false; showSpeed = true }
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.sleep_timer)) },
                            onClick = { menu = false; showSleep = true }
                        )
                        DropdownMenuItem(
                            text = {
                                Text(
                                    when {
                                        abA == null -> stringResource(R.string.ab_set_a)
                                        abB == null -> stringResource(R.string.ab_set_b)
                                        else -> stringResource(R.string.ab_clear)
                                    }
                                )
                            },
                            onClick = {
                                menu = false
                                if (abA != null && abB != null) vm.clearAB() else vm.markAB()
                            }
                        )
                        if (t?.isVideo == true) {
                            DropdownMenuItem(
                                text = { Text(stringResource(if (audioOnly) R.string.audio_only_off else R.string.audio_only)) },
                                onClick = { menu = false; vm.setAudioOnly(!audioOnly) }
                            )
                        }
                    }
                }
            }

            // Обложка или видео
            Box(
                modifier = Modifier.weight(1f).fillMaxWidth().padding(vertical = 18.dp),
                contentAlignment = Alignment.Center
            ) {
                if (t?.isVideo == true && !audioOnly) {
                    VideoSurface(
                        player = player,
                        modifier = Modifier.fillMaxWidth().aspectRatio(16f / 9f).clip(RoundedCornerShape(24.dp))
                    )
                } else {
                    CoverImage(
                        track = t,
                        sizePx = 800,
                        shape = coverShape,
                        modifier = Modifier.aspectRatio(1f).shadow(28.dp, coverShape, clip = false)
                    )
                }
            }

            // Название, исполнитель, избранное
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(
                        t?.title.orEmpty(),
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (t != null && !t.isVideo) {
                        Text(
                            t.artist.ifBlank { unknown },
                            style = MaterialTheme.typography.bodyLarge,
                            color = scheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                if (t != null) {
                    GlassIconButton(
                        icon = if (isFav) Icons.Rounded.Star else Icons.Rounded.StarBorder,
                        contentDescription = stringResource(R.string.tile_favorites),
                        onClick = { Repo.toggleFavorite(t.mediaId) },
                        active = isFav
                    )
                }
            }
            Spacer(Modifier.height(10.dp))

            // Метки A-B
            if (abA != null) {
                Text(
                    "A " + formatTime(abA ?: 0L) + if (abB != null) "  -  B " + formatTime(abB ?: 0L) else "",
                    style = MaterialTheme.typography.labelMedium,
                    color = scheme.primary
                )
            }

            // Перемотка
            Slider(
                value = shownPos.toFloat(),
                onValueChange = { dragging = true; dragValue = it },
                onValueChangeFinished = { vm.seekTo(dragValue.toLong()); dragging = false },
                valueRange = 0f..duration.toFloat(),
                colors = SliderDefaults.colors(
                    thumbColor = scheme.onBackground,
                    activeTrackColor = scheme.onBackground,
                    inactiveTrackColor = scheme.onBackground.copy(alpha = 0.22f)
                )
            )
            Row(Modifier.fillMaxWidth().padding(horizontal = 6.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(formatTime(shownPos), style = MaterialTheme.typography.bodySmall, color = scheme.onSurfaceVariant)
                Text(formatTime(duration), style = MaterialTheme.typography.bodySmall, color = scheme.onSurfaceVariant)
            }
            Spacer(Modifier.height(16.dp))

            // Кнопки
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                GlassIconButton(Icons.Rounded.Shuffle, stringResource(R.string.shuffle), { vm.toggleShuffle() }, active = state.shuffle)
                GlassIconButton(Icons.Rounded.SkipPrevious, stringResource(R.string.previous), { vm.previous() }, size = 56.dp, iconSize = 28.dp)
                GlassIconButton(
                    icon = if (state.isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                    contentDescription = stringResource(if (state.isPlaying) R.string.pause else R.string.play),
                    onClick = { vm.togglePlay() },
                    size = 78.dp, iconSize = 40.dp, accent = true
                )
                GlassIconButton(Icons.Rounded.SkipNext, stringResource(R.string.next), { vm.next() }, size = 56.dp, iconSize = 28.dp)
                GlassIconButton(
                    icon = Icons.Rounded.QueueMusic,
                    contentDescription = stringResource(R.string.queue),
                    onClick = { showQueue = true }
                )
            }
            Spacer(Modifier.height(8.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                GlassIconButton(
                    icon = if (state.repeatMode == Player.REPEAT_MODE_ONE) Icons.Rounded.RepeatOne else Icons.Rounded.Repeat,
                    contentDescription = stringResource(R.string.repeat),
                    onClick = { vm.cycleRepeat() },
                    size = 44.dp,
                    active = state.repeatMode != Player.REPEAT_MODE_OFF
                )
            }
            Spacer(Modifier.height(6.dp))
        }
    }

    addTo?.let { AddToPlaylistDialog(trackId = it, onDismiss = { addTo = null }) }

    if (showSpeed) {
        AlertDialog(
            onDismissRequest = { showSpeed = false },
            containerColor = scheme.surface,
            title = { Text(stringResource(R.string.speed)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(listOf(0.5f, 0.75f, 1f, 1.25f), listOf(1.5f, 2f, 2.5f, 3f)).forEach { row ->
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            row.forEach { s ->
                                GlassChip(formatSpeed(s), state.speed == s, { vm.setSpeed(s); showSpeed = false })
                            }
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = { TextButton(onClick = { showSpeed = false }) { Text(stringResource(R.string.cancel)) } }
        )
    }

    if (showSleep) {
        AlertDialog(
            onDismissRequest = { showSleep = false },
            containerColor = scheme.surface,
            title = { Text(stringResource(R.string.sleep_timer)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    sleepEnd?.let { end ->
                        Text(
                            stringResource(R.string.sleep_until, DateFormat.getTimeInstance(DateFormat.SHORT).format(Date(end))),
                            color = scheme.primary
                        )
                    }
                    listOf(listOf(15, 30, 45), listOf(60, 90)).forEach { row ->
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            row.forEach { m ->
                                GlassChip(stringResource(R.string.sleep_minutes, m), false, { vm.startSleepTimer(m); showSleep = false })
                            }
                        }
                    }
                    if (sleepEnd != null) {
                        TextButton(onClick = { vm.cancelSleepTimer(); showSleep = false }) {
                            Text(stringResource(R.string.sleep_off))
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = { TextButton(onClick = { showSleep = false }) { Text(stringResource(R.string.cancel)) } }
        )
    }

    if (showQueue) {
        ModalBottomSheet(onDismissRequest = { showQueue = false }, containerColor = scheme.surface) {
            val items = remember(state.mediaId) { vm.queue() }
            val index = remember(state.mediaId) { vm.currentIndex() }
            Text(
                stringResource(R.string.queue),
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            LazyColumn(Modifier.fillMaxWidth().navigationBarsPadding()) {
                itemsIndexed(items) { i, item ->
                    Column(
                        Modifier.fillMaxWidth().clickable { vm.playQueueIndex(i); showQueue = false }
                            .padding(horizontal = 24.dp, vertical = 10.dp)
                    ) {
                        Text(
                            item.first,
                            color = if (i == index) scheme.primary else scheme.onSurface,
                            fontWeight = if (i == index) FontWeight.Bold else FontWeight.Normal,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (item.second.isNotEmpty()) {
                            Text(item.second, style = MaterialTheme.typography.bodySmall, color = scheme.onSurfaceVariant, maxLines = 1)
                        }
                    }
                }
            }
        }
    }
}

private fun formatSpeed(s: Float): String =
    (if (s == s.toInt().toFloat()) s.toInt().toString() else s.toString()) + "x"

/** Видео: показываем картинку из того же плеера, что играет в сервисе. */
@OptIn(UnstableApi::class)
@Composable
private fun VideoSurface(player: Player?, modifier: Modifier) {
    AndroidView(
        modifier = modifier,
        factory = { ctx -> PlayerView(ctx).apply { useController = false } },
        update = { view -> if (view.player !== player) view.player = player },
        onRelease = { view -> view.player = null }
    )
}
