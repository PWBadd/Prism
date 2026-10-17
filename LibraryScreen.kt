// Prism — author: PWBad's | planning: Claude (Anthropic) | code: DeepSeek
package com.shtuchki.glassbox.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Language
import androidx.compose.material.icons.rounded.Menu
import androidx.compose.material.icons.rounded.Movie
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.QueueMusic
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Send
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.Sort
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import android.content.Intent
import android.net.Uri
import coil.compose.AsyncImage
import com.shtuchki.glassbox.Credits
import com.shtuchki.glassbox.R
import com.shtuchki.glassbox.data.Repo
import com.shtuchki.glassbox.data.Track
import com.shtuchki.glassbox.perf.PerformanceManager
import com.shtuchki.glassbox.player.PlayerViewModel
import com.shtuchki.glassbox.ui.AddToPlaylistDialog
import com.shtuchki.glassbox.ui.CoverImage
import com.shtuchki.glassbox.ui.NameDialog
import com.shtuchki.glassbox.ui.TrackRow
import com.shtuchki.glassbox.ui.glass.AppBackground
import com.shtuchki.glassbox.ui.glass.GlassIconButton
import com.shtuchki.glassbox.ui.glass.glass
import com.shtuchki.glassbox.ui.rememberImagePicker
import kotlinx.coroutines.launch
import kotlin.random.Random

@Composable
fun LibraryScreen(vm: PlayerViewModel) {
    val audio by vm.audio.collectAsState()
    val video by vm.video.collectAsState()
    val loading by vm.loading.collectAsState()
    val current by vm.currentTrack.collectAsState()
    val state by vm.state.collectAsState()
    val data by Repo.data.collectAsState()
    val warm by PerformanceManager.warm.collectAsState()

    var tab by rememberSaveable { mutableStateOf(0) }       // 0 треки, 1 видео, 2 исполнители, 3 альбомы
    var query by rememberSaveable { mutableStateOf("") }
    var sort by rememberSaveable { mutableStateOf(0) }       // 0 название, 1 исполнитель, 2 дата
    var addTo by remember { mutableStateOf<String?>(null) }
    var sortMenu by remember { mutableStateOf(false) }
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()
    val resources = LocalContext.current.resources
    val navBottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    val unknownArtist = stringResource(R.string.unknown_artist)
    val unknownAlbum = stringResource(R.string.unknown_album)

    val q = query.trim().lowercase()
    val tracksShown = remember(audio, q, sort) {
        val base = audio.filter {
            q.isEmpty() || it.title.lowercase().contains(q) || it.artist.lowercase().contains(q) ||
                it.album.lowercase().contains(q)
        }
        when (sort) {
            1 -> base.sortedBy { it.artist.lowercase() }
            2 -> base.sortedByDescending { it.dateAdded }
            else -> base
        }
    }
    val videosShown = remember(video, q) {
        video.filter { q.isEmpty() || it.title.lowercase().contains(q) }
    }
    val artists = remember(audio, q) {
        audio.groupBy { it.artist }.filter { q.isEmpty() || it.key.lowercase().contains(q) }
            .toList().sortedBy { it.first.lowercase() }
    }
    val albums = remember(audio, q) {
        audio.groupBy { it.album }.filter { q.isEmpty() || it.key.lowercase().contains(q) }
            .toList().sortedBy { it.first.lowercase() }
    }

    val activeList = if (tab == 1) videosShown else tracksShown
    val countText = when (tab) {
        1 -> resources.getQuantityString(R.plurals.videos_count, videosShown.size, videosShown.size)
        else -> resources.getQuantityString(R.plurals.tracks_count, audio.size, audio.size)
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            DrawerContent(vm) { scope.launch { drawerState.close() } }
        }
    ) {
        AppBackground {
            Box(Modifier.fillMaxSize()) {
                Column(Modifier.fillMaxSize().statusBarsPadding()) {
                    // Меню и поиск
                    Row(
                        Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, top = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        GlassIconButton(
                            icon = Icons.Rounded.Menu,
                            contentDescription = stringResource(R.string.menu),
                            onClick = { scope.launch { drawerState.open() } }
                        )
                        Spacer(Modifier.width(12.dp))
                        SearchField(query, { query = it }, Modifier.weight(1f))
                    }

                    if (warm) {
                        Text(
                            stringResource(R.string.perf_hint),
                            modifier = Modifier.padding(horizontal = 22.dp, vertical = 6.dp),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    // Плитки
                    Row(
                        Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        QuickTile(Icons.Rounded.Favorite, stringResource(R.string.tile_favorites), Modifier.weight(1f)) {
                            vm.push("fav")
                        }
                        QuickTile(Icons.Rounded.QueueMusic, stringResource(R.string.tile_playlists), Modifier.weight(1f)) {
                            vm.push("playlists")
                        }
                        QuickTile(Icons.Rounded.History, stringResource(R.string.tile_recent), Modifier.weight(1f)) {
                            vm.push("recent")
                        }
                    }

                    // Вкладки
                    Row(
                        Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        TabPill(stringResource(R.string.tab_tracks), tab == 0) { tab = 0 }
                        TabPill(stringResource(R.string.tab_video), tab == 1) { tab = 1 }
                        TabPill(stringResource(R.string.tab_artists), tab == 2) { tab = 2 }
                        TabPill(stringResource(R.string.tab_albums), tab == 3) { tab = 3 }
                    }

                    // Перемешать + сортировка
                    if (tab <= 1) {
                        Row(
                            Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                Modifier.size(46.dp).clip(CircleShape).background(MaterialTheme.colorScheme.onBackground)
                                    .clickable(enabled = activeList.isNotEmpty()) {
                                        vm.playQueue(activeList, Random.nextInt(activeList.size), shuffle = true)
                                        if (tab == 1) vm.push("player")
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Rounded.PlayArrow, null, tint = MaterialTheme.colorScheme.background)
                            }
                            Spacer(Modifier.width(12.dp))
                            Column(Modifier.weight(1f)) {
                                Text(
                                    stringResource(R.string.shuffle_all),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(countText, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            if (tab == 0) {
                                Box {
                                    GlassIconButton(
                                        icon = Icons.Rounded.Sort,
                                        contentDescription = stringResource(R.string.sort),
                                        onClick = { sortMenu = true },
                                        size = 44.dp
                                    )
                                    DropdownMenu(expanded = sortMenu, onDismissRequest = { sortMenu = false }) {
                                        DropdownMenuItem(text = { Text(stringResource(R.string.sort_title)) }, onClick = { sort = 0; sortMenu = false })
                                        DropdownMenuItem(text = { Text(stringResource(R.string.sort_artist)) }, onClick = { sort = 1; sortMenu = false })
                                        DropdownMenuItem(text = { Text(stringResource(R.string.sort_date)) }, onClick = { sort = 2; sortMenu = false })
                                    }
                                }
                            }
                        }
                    }

                    // Содержимое вкладки
                    Box(Modifier.weight(1f).fillMaxWidth()) {
                        val bottomPad = 110.dp + navBottom
                        when {
                            loading && audio.isEmpty() && video.isEmpty() ->
                                CircularProgressIndicator(Modifier.align(Alignment.Center))

                            tab <= 1 && activeList.isEmpty() ->
                                EmptyState(
                                    text = stringResource(
                                        if (q.isNotEmpty()) R.string.nothing_found
                                        else if (tab == 1) R.string.empty_video_title else R.string.empty_music_title
                                    ),
                                    sub = if (q.isNotEmpty()) "" else stringResource(
                                        if (tab == 1) R.string.empty_video_text else R.string.empty_music_text
                                    ),
                                    isVideo = tab == 1
                                )

                            tab <= 1 -> LazyColumn(
                                state = listState,
                                contentPadding = PaddingValues(bottom = bottomPad)
                            ) {
                                itemsIndexed(activeList, key = { _, t -> t.mediaId }) { index, t ->
                                    val isCurrent = current?.mediaId == t.mediaId
                                    TrackRow(
                                        track = t,
                                        isCurrent = isCurrent,
                                        isPlaying = isCurrent && state.isPlaying,
                                        favorite = t.mediaId in data.favorites,
                                        onClick = {
                                            vm.playQueue(activeList, index)
                                            if (t.isVideo) vm.push("player")
                                        },
                                        onAddToPlaylist = { addTo = t.mediaId },
                                        onPlayNext = { vm.playNext(t) }
                                    )
                                }
                            }

                            tab == 2 -> GroupList(
                                groups = artists,
                                unknown = unknownArtist,
                                bottomPad = bottomPad,
                                onClick = { vm.push("artist:" + it) }
                            )

                            else -> GroupList(
                                groups = albums,
                                unknown = unknownAlbum,
                                bottomPad = bottomPad,
                                onClick = { vm.push("album:" + it) }
                            )
                        }
                    }
                }

                // Мини-плеер
                AnimatedVisibility(
                    visible = current != null,
                    modifier = Modifier.align(Alignment.BottomCenter),
                    enter = slideInVertically { it } + fadeIn(),
                    exit = slideOutVertically { it } + fadeOut()
                ) {
                    MiniPlayer(vm = vm, track = current)
                }
            }
        }
    }

    addTo?.let { id -> AddToPlaylistDialog(trackId = id, onDismiss = { addTo = null }) }
}

@Composable
private fun SearchField(value: String, onChange: (String) -> Unit, modifier: Modifier = Modifier) {
    val scheme = MaterialTheme.colorScheme
    val shape = RoundedCornerShape(50)
    Row(
        modifier = modifier.height(48.dp).glass(shape).padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(Icons.Rounded.Search, null, tint = scheme.onSurfaceVariant, modifier = Modifier.size(22.dp))
        Spacer(Modifier.width(10.dp))
        Box(Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
            if (value.isEmpty()) {
                Text(
                    stringResource(R.string.search_hint),
                    color = scheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            BasicTextField(
                value = value,
                onValueChange = onChange,
                singleLine = true,
                textStyle = MaterialTheme.typography.bodyMedium.copy(color = scheme.onBackground),
                cursorBrush = SolidColor(scheme.primary),
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun QuickTile(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, modifier: Modifier, onClick: () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    val shape = RoundedCornerShape(22.dp)
    Column(
        modifier = modifier
            .height(86.dp)
            .glass(shape)
            .background(
                Brush.linearGradient(listOf(scheme.primary.copy(alpha = 0.28f), Color.Transparent)),
                shape
            )
            .clip(shape)
            .clickable(onClick = onClick)
            .padding(14.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Icon(icon, null, tint = scheme.onBackground, modifier = Modifier.size(26.dp))
        Text(
            label,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun TabPill(text: String, selected: Boolean, onClick: () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    val shape = RoundedCornerShape(50)
    Box(
        Modifier
            .clip(shape)
            .then(if (selected) Modifier.background(scheme.onBackground, shape) else Modifier)
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            color = if (selected) scheme.background else scheme.onSurfaceVariant,
            maxLines = 1
        )
    }
}

@Composable
private fun GroupList(
    groups: List<Pair<String, List<Track>>>,
    unknown: String,
    bottomPad: androidx.compose.ui.unit.Dp,
    onClick: (String) -> Unit
) {
    val resources = LocalContext.current.resources
    LazyColumn(contentPadding = PaddingValues(top = 6.dp, bottom = bottomPad)) {
        itemsIndexed(groups, key = { _, g -> g.first }) { _, g ->
            val (name, list) = g
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 2.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .clickable { onClick(name) }
                    .padding(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                CoverImage(list.firstOrNull(), 160, RoundedCornerShape(14.dp), Modifier.size(56.dp))
                Spacer(Modifier.width(14.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        name.ifBlank { unknown },
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        resources.getQuantityString(R.plurals.tracks_count, list.size, list.size),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
fun EmptyState(text: String, sub: String, isVideo: Boolean) {
    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            if (isVideo) Icons.Rounded.Movie else Icons.Rounded.MusicNote,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(52.dp)
        )
        Spacer(Modifier.height(14.dp))
        Text(text, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
        if (sub.isNotEmpty()) {
            Spacer(Modifier.height(6.dp))
            Text(sub, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
        }
    }
}

/** Боковое меню с профилем (как в референсе). */
@Composable
private fun DrawerContent(vm: PlayerViewModel, close: () -> Unit) {
    val data by Repo.data.collectAsState()
    val context = LocalContext.current
    var renaming by remember { mutableStateOf(false) }
    val pickAvatar = rememberImagePicker { uri ->
        Repo.saveImage(context, uri, "avatar")?.let { f -> Repo.update { it.copy(avatarFile = f) } }
    }
    val avatar = Repo.imageFile(context, data.avatarFile)

    ModalDrawerSheet(drawerContainerColor = MaterialTheme.colorScheme.background) {
        Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().padding(vertical = 16.dp)) {
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    Modifier.size(76.dp).clip(CircleShape).background(MaterialTheme.colorScheme.surface).clickable { pickAvatar() },
                    contentAlignment = Alignment.Center
                ) {
                    if (avatar != null && avatar.exists()) {
                        AsyncImage(model = avatar, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                    } else {
                        Image(painterResource(R.drawable.prism_logo), null, modifier = Modifier.fillMaxSize().padding(8.dp))
                    }
                }
                Spacer(Modifier.width(16.dp))
                Text(
                    data.userName,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.clickable { renaming = true }
                )
            }
            HorizontalDivider(Modifier.padding(horizontal = 24.dp, vertical = 12.dp))
            DrawerItem(Icons.Rounded.Settings, stringResource(R.string.settings)) { close(); vm.push("settings") }
            DrawerItem(Icons.Rounded.Palette, stringResource(R.string.themes_title)) { close(); vm.push("themes") }
            DrawerItem(Icons.Rounded.QueueMusic, stringResource(R.string.playlists_title)) { close(); vm.push("playlists") }
            DrawerItem(Icons.Rounded.Send, stringResource(R.string.telegram_channel)) {
                close()
                runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(Credits.TELEGRAM_URL))) }
            }
        }
    }
    if (renaming) {
        NameDialog(
            title = stringResource(R.string.your_name),
            initial = data.userName,
            confirmLabel = stringResource(R.string.save),
            onConfirm = { n -> Repo.update { it.copy(userName = n) }; renaming = false },
            onDismiss = { renaming = false }
        )
    }
}

@Composable
private fun DrawerItem(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, onClick: () -> Unit) {
    NavigationDrawerItem(
        label = { Text(label, style = MaterialTheme.typography.titleMedium) },
        icon = { Icon(icon, null) },
        selected = false,
        onClick = onClick,
        modifier = Modifier.padding(horizontal = 12.dp)
    )
}

/** Плавающий мини-плеер: цвет акцента, вращающаяся «пластинка». */
@Composable
private fun MiniPlayer(vm: PlayerViewModel, track: Track?) {
    val state by vm.state.collectAsState()
    val pos by vm.position.collectAsState()
    val lite by PerformanceManager.lite.collectAsState()
    val scheme = MaterialTheme.colorScheme
    val unknown = stringResource(R.string.unknown_artist)
    val duration = (if (state.durationMs > 0) state.durationMs else (track?.durationMs ?: 0L)).coerceAtLeast(1L)
    val fraction = (pos.toFloat() / duration).coerceIn(0f, 1f)
    val shape = RoundedCornerShape(36.dp)

    val infinite = rememberInfiniteTransition(label = "vinyl")
    val angle by infinite.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(14000, easing = LinearEasing), RepeatMode.Restart),
        label = "angle"
    )
    val spin = state.isPlaying && !lite

    Box(
        Modifier.fillMaxWidth().navigationBarsPadding().padding(horizontal = 14.dp, vertical = 10.dp)
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .height(74.dp)
                .clip(shape)
                .background(scheme.primary.copy(alpha = 0.94f), shape)
                .clickable { vm.push("player") }
                .padding(start = 8.dp, end = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                Modifier.size(58.dp).graphicsLayer { rotationZ = if (spin) angle else 0f },
                contentAlignment = Alignment.Center
            ) {
                CoverImage(track, 160, CircleShape, Modifier.fillMaxSize())
                Box(Modifier.size(12.dp).clip(CircleShape).background(scheme.primary))
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    track?.title.orEmpty(),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = scheme.onPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    if (track == null || track.isVideo) "" else track.artist.ifBlank { unknown },
                    style = MaterialTheme.typography.bodySmall,
                    color = scheme.onPrimary.copy(alpha = 0.75f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Box(
                Modifier.size(46.dp).clip(CircleShape).clickable { vm.togglePlay() },
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(
                    progress = { fraction },
                    modifier = Modifier.fillMaxSize(),
                    color = scheme.onPrimary,
                    trackColor = scheme.onPrimary.copy(alpha = 0.25f),
                    strokeWidth = 3.dp
                )
                Icon(
                    if (state.isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                    stringResource(if (state.isPlaying) R.string.pause else R.string.play),
                    tint = scheme.onPrimary
                )
            }
            Box(
                Modifier.size(44.dp).clip(CircleShape).clickable { vm.next() },
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Rounded.SkipNext, stringResource(R.string.next), tint = scheme.onPrimary)
            }
        }
    }
}
