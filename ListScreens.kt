// Prism — author: PWBad's | planning: Claude (Anthropic) | code: DeepSeek
package com.shtuchki.glassbox.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.AddPhotoAlternate
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.QueueMusic
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.shtuchki.glassbox.R
import com.shtuchki.glassbox.data.Playlist
import com.shtuchki.glassbox.data.Repo
import com.shtuchki.glassbox.data.SmartLists
import com.shtuchki.glassbox.data.Track
import com.shtuchki.glassbox.player.PlayerViewModel
import com.shtuchki.glassbox.ui.AddToPlaylistDialog
import com.shtuchki.glassbox.ui.NameDialog
import com.shtuchki.glassbox.ui.TrackRow
import com.shtuchki.glassbox.ui.glass.AppBackground
import com.shtuchki.glassbox.ui.glass.GlassIconButton
import com.shtuchki.glassbox.ui.glass.GlassPillButton
import com.shtuchki.glassbox.ui.glass.glass
import com.shtuchki.glassbox.ui.rememberImagePicker
import kotlin.random.Random

/** Выбирает, какой список показать, по строке-адресу ("fav", "pl:ID", "artist:NAME" ...). */
@Composable
fun ListDestination(dest: String, vm: PlayerViewModel) {
    val audio by vm.audio.collectAsState()
    val video by vm.video.collectAsState()
    val data by Repo.data.collectAsState()
    val all = remember(audio, video) { audio + video }
    val byId = remember(all) { all.associateBy { it.mediaId } }
    val unknownArtist = stringResource(R.string.unknown_artist)
    val unknownAlbum = stringResource(R.string.unknown_album)
    val key = dest.substringAfter(":", "")

    when {
        dest == "fav" -> TracksScreen(vm, stringResource(R.string.tile_favorites), data.favorites.mapNotNull { byId[it] })
        dest == "recent" -> TracksScreen(vm, stringResource(R.string.tile_recent), data.recents.mapNotNull { byId[it] })
        dest == "smart:added" -> TracksScreen(vm, stringResource(R.string.smart_added), SmartLists.recentlyAdded(all))
        dest == "smart:most" -> TracksScreen(vm, stringResource(R.string.smart_most), SmartLists.mostPlayed(all, data))
        dest == "smart:old" -> TracksScreen(vm, stringResource(R.string.smart_old), SmartLists.notPlayedForAWhile(all, data))
        dest.startsWith("artist:") -> TracksScreen(vm, key.ifBlank { unknownArtist }, audio.filter { it.artist == key })
        dest.startsWith("album:") -> TracksScreen(vm, key.ifBlank { unknownAlbum }, audio.filter { it.album == key })
        dest.startsWith("pl:") -> {
            val p = data.playlists.firstOrNull { it.id == key }
            if (p == null) {
                TracksScreen(vm, "", emptyList())
            } else {
                TracksScreen(vm, p.name, p.trackIds.mapNotNull { byId[it] }, playlist = p)
            }
        }
        else -> TracksScreen(vm, "", emptyList())
    }
}

@Composable
fun TracksScreen(vm: PlayerViewModel, title: String, tracks: List<Track>, playlist: Playlist? = null) {
    val current by vm.currentTrack.collectAsState()
    val state by vm.state.collectAsState()
    val data by Repo.data.collectAsState()
    val context = LocalContext.current
    val scheme = MaterialTheme.colorScheme
    var addTo by remember { mutableStateOf<String?>(null) }
    var renaming by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf(false) }
    val pickBanner = rememberImagePicker { uri ->
        if (playlist != null) Repo.saveImage(context, uri, "banner")?.let { Repo.setBanner(playlist.id, it) }
    }
    val banner = Repo.imageFile(context, playlist?.bannerFile)

    AppBackground {
        Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()) {
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                GlassIconButton(Icons.AutoMirrored.Rounded.ArrowBack, stringResource(R.string.back), { vm.pop() })
                Spacer(Modifier.width(14.dp))
                Text(
                    title,
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (playlist != null) {
                    GlassIconButton(Icons.Rounded.Edit, stringResource(R.string.rename), { renaming = true }, size = 44.dp)
                    Spacer(Modifier.width(8.dp))
                    GlassIconButton(Icons.Rounded.Delete, stringResource(R.string.delete), { confirmDelete = true }, size = 44.dp)
                }
            }

            LazyColumn(contentPadding = PaddingValues(bottom = 24.dp)) {
                if (playlist != null) {
                    item {
                        val shape = RoundedCornerShape(26.dp)
                        Box(
                            Modifier.fillMaxWidth().padding(horizontal = 16.dp).height(170.dp).clip(shape)
                                .background(Brush.linearGradient(listOf(scheme.primary.copy(alpha = 0.55f), scheme.primary.copy(alpha = 0.15f))))
                        ) {
                            if (banner != null && banner.exists()) {
                                AsyncImage(model = banner, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                            } else {
                                Icon(Icons.Rounded.QueueMusic, null, tint = scheme.onBackground.copy(alpha = 0.7f), modifier = Modifier.align(Alignment.Center).size(56.dp))
                            }
                            GlassPillButton(
                                text = stringResource(R.string.change_banner),
                                icon = Icons.Rounded.AddPhotoAlternate,
                                onClick = pickBanner,
                                modifier = Modifier.align(Alignment.BottomEnd).padding(12.dp)
                            )
                        }
                        Spacer(Modifier.height(10.dp))
                    }
                }
                item {
                    Row(
                        Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        GlassPillButton(
                            stringResource(R.string.play_all), Icons.Rounded.PlayArrow,
                            onClick = {
                                if (tracks.isNotEmpty()) {
                                    vm.playQueue(tracks, 0, shuffle = false)
                                    if (tracks.first().isVideo) vm.push("player")
                                }
                            },
                            accent = true
                        )
                        GlassPillButton(
                            stringResource(R.string.shuffle), Icons.Rounded.Shuffle,
                            onClick = {
                                if (tracks.isNotEmpty()) {
                                    vm.playQueue(tracks, Random.nextInt(tracks.size), shuffle = true)
                                    if (tracks.first().isVideo) vm.push("player")
                                }
                            }
                        )
                    }
                }
                if (tracks.isEmpty()) {
                    item {
                        Text(
                            stringResource(R.string.empty_list),
                            modifier = Modifier.fillMaxWidth().padding(32.dp),
                            color = scheme.onSurfaceVariant
                        )
                    }
                }
                itemsIndexed(tracks, key = { i, t -> t.mediaId + "#" + i }) { index, t ->
                    val isCurrent = current?.mediaId == t.mediaId
                    val extra = if (playlist != null) listOf(
                        stringResource(R.string.menu_remove) to { Repo.removeFromPlaylist(playlist.id, t.mediaId) },
                        stringResource(R.string.menu_move_up) to { Repo.moveInPlaylist(playlist.id, t.mediaId, -1) },
                        stringResource(R.string.menu_move_down) to { Repo.moveInPlaylist(playlist.id, t.mediaId, 1) }
                    ) else emptyList()
                    TrackRow(
                        track = t,
                        isCurrent = isCurrent,
                        isPlaying = isCurrent && state.isPlaying,
                        favorite = t.mediaId in data.favorites,
                        onClick = {
                            vm.playQueue(tracks, index)
                            if (t.isVideo) vm.push("player")
                        },
                        onAddToPlaylist = { addTo = t.mediaId },
                        onPlayNext = { vm.playNext(t) },
                        extraMenu = extra
                    )
                }
            }
        }
    }

    addTo?.let { AddToPlaylistDialog(trackId = it, onDismiss = { addTo = null }) }
    if (renaming && playlist != null) {
        NameDialog(
            title = stringResource(R.string.rename),
            initial = playlist.name,
            confirmLabel = stringResource(R.string.save),
            onConfirm = { Repo.renamePlaylist(playlist.id, it); renaming = false },
            onDismiss = { renaming = false }
        )
    }
    if (confirmDelete && playlist != null) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { confirmDelete = false },
            containerColor = scheme.surface,
            title = { Text(stringResource(R.string.delete) + "?") },
            text = { Text(playlist.name) },
            confirmButton = {
                androidx.compose.material3.TextButton(onClick = {
                    confirmDelete = false
                    Repo.deletePlaylist(playlist.id)
                    vm.pop()
                }) { Text(stringResource(R.string.delete)) }
            },
            dismissButton = {
                androidx.compose.material3.TextButton(onClick = { confirmDelete = false }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }
}

@Composable
fun PlaylistsScreen(vm: PlayerViewModel) {
    val data by Repo.data.collectAsState()
    val context = LocalContext.current
    val scheme = MaterialTheme.colorScheme
    var creating by remember { mutableStateOf(false) }

    AppBackground {
        Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()) {
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                GlassIconButton(Icons.AutoMirrored.Rounded.ArrowBack, stringResource(R.string.back), { vm.pop() })
                Spacer(Modifier.width(14.dp))
                Text(
                    stringResource(R.string.playlists_title),
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                GlassIconButton(Icons.Rounded.Add, stringResource(R.string.new_playlist), { creating = true }, accent = true)
            }
            LazyColumn(contentPadding = PaddingValues(bottom = 24.dp)) {
                item { PlaylistRow(stringResource(R.string.smart_added), "", null, Icons.Rounded.History) { vm.push("smart:added") } }
                item { PlaylistRow(stringResource(R.string.smart_most), "", null, Icons.Rounded.Star) { vm.push("smart:most") } }
                item { PlaylistRow(stringResource(R.string.smart_old), "", null, Icons.Rounded.History) { vm.push("smart:old") } }
                itemsIndexed(data.playlists, key = { _, p -> p.id }) { _, p ->
                    val file = Repo.imageFile(context, p.bannerFile)
                    PlaylistRow(
                        p.name,
                        context.resources.getQuantityString(R.plurals.tracks_count, p.trackIds.size, p.trackIds.size),
                        file?.takeIf { it.exists() },
                        Icons.Rounded.QueueMusic
                    ) { vm.push("pl:" + p.id) }
                }
            }
        }
    }
    if (creating) {
        NameDialog(
            title = stringResource(R.string.new_playlist),
            initial = "",
            confirmLabel = stringResource(R.string.create),
            onConfirm = { name -> Repo.createPlaylist(name); creating = false },
            onDismiss = { creating = false }
        )
    }
}

@Composable
private fun PlaylistRow(
    name: String,
    sub: String,
    image: java.io.File?,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit
) {
    val scheme = MaterialTheme.colorScheme
    val shape = RoundedCornerShape(24.dp)
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 5.dp)
            .glass(shape).clip(shape).clickable(onClick = onClick).padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            Modifier.size(64.dp).clip(RoundedCornerShape(16.dp))
                .background(Brush.linearGradient(listOf(scheme.primary.copy(alpha = 0.55f), scheme.primary.copy(alpha = 0.2f)))),
            contentAlignment = Alignment.Center
        ) {
            if (image != null) {
                AsyncImage(model = image, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
            } else {
                Icon(icon, null, tint = scheme.onBackground, modifier = Modifier.size(28.dp))
            }
        }
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            if (sub.isNotEmpty()) Text(sub, style = MaterialTheme.typography.bodyMedium, color = scheme.onSurfaceVariant)
        }
    }
}
