// Prism — author: PWBad's | planning: Claude (Anthropic) | code: DeepSeek
package com.shtuchki.glassbox.ui

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.shtuchki.glassbox.R
import com.shtuchki.glassbox.data.Repo
import com.shtuchki.glassbox.data.Track

/** Открывает выбор картинки из галереи (Photo Picker). Возвращает функцию «открыть». */
@Composable
fun rememberImagePicker(onPicked: (Uri) -> Unit): () -> Unit {
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) onPicked(uri)
    }
    return { launcher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) }
}

@Composable
fun NameDialog(
    title: String,
    initial: String,
    confirmLabel: String,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var text by remember { mutableStateOf(initial) }
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
        title = { Text(title) },
        text = { OutlinedTextField(value = text, onValueChange = { text = it }, singleLine = true) },
        confirmButton = {
            TextButton(onClick = { if (text.isNotBlank()) onConfirm(text.trim()) }) { Text(confirmLabel) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } }
    )
}

/** Выбор плейлиста для трека (или создание нового). */
@Composable
fun AddToPlaylistDialog(trackId: String, onDismiss: () -> Unit) {
    val data by Repo.data.collectAsState()
    var creating by remember { mutableStateOf(false) }
    if (creating) {
        NameDialog(
            title = stringResource(R.string.new_playlist),
            initial = "",
            confirmLabel = stringResource(R.string.create),
            onConfirm = { name ->
                val id = Repo.createPlaylist(name)
                Repo.addToPlaylist(id, trackId)
                onDismiss()
            },
            onDismiss = { creating = false }
        )
    } else {
        AlertDialog(
            onDismissRequest = onDismiss,
            containerColor = MaterialTheme.colorScheme.surface,
            title = { Text(stringResource(R.string.menu_add_playlist)) },
            text = {
                Column(Modifier.heightIn(max = 320.dp).verticalScroll(rememberScrollState())) {
                    Row(
                        Modifier.fillMaxWidth().clickable { creating = true }.padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "+  " + stringResource(R.string.new_playlist),
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    data.playlists.forEach { p ->
                        Row(
                            Modifier.fillMaxWidth().clickable {
                                Repo.addToPlaylist(p.id, trackId)
                                onDismiss()
                            }.padding(vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(p.name, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } }
        )
    }
}

/** Строка трека: обложка, название, исполнитель | альбом, меню «⋮». */
@Composable
fun TrackRow(
    track: Track,
    isCurrent: Boolean,
    isPlaying: Boolean,
    favorite: Boolean,
    onClick: () -> Unit,
    onAddToPlaylist: () -> Unit,
    onPlayNext: () -> Unit,
    extraMenu: List<Pair<String, () -> Unit>> = emptyList()
) {
    val scheme = MaterialTheme.colorScheme
    val unknown = stringResource(R.string.unknown_artist)
    val shape = RoundedCornerShape(20.dp)
    var menu by remember { mutableStateOf(false) }
    val subtitle = if (track.isVideo) "" else listOf(track.artist.ifBlank { unknown }, track.album)
        .filter { it.isNotBlank() }.joinToString(" | ")

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 2.dp)
            .clip(shape)
            .then(if (isCurrent) Modifier.background(scheme.primary.copy(alpha = 0.13f), shape) else Modifier)
            .clickable(onClick = onClick)
            .padding(start = 8.dp, top = 8.dp, bottom = 8.dp, end = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        CoverImage(track, 160, RoundedCornerShape(14.dp), Modifier.size(56.dp))
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(
                track.title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = if (isCurrent) scheme.primary else scheme.onBackground,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (subtitle.isNotEmpty()) {
                Text(
                    subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = scheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        if (isPlaying) {
            Icon(Icons.Rounded.PlayArrow, null, tint = scheme.primary, modifier = Modifier.size(20.dp))
        }
        if (favorite) {
            Icon(Icons.Rounded.Favorite, null, tint = scheme.primary, modifier = Modifier.padding(start = 4.dp).size(16.dp))
        }
        Box {
            Box(
                Modifier.size(44.dp).clip(CircleShape).clickable { menu = true },
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Rounded.MoreVert, stringResource(R.string.more), tint = scheme.onSurfaceVariant)
            }
            DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                DropdownMenuItem(
                    text = { Text(stringResource(if (favorite) R.string.menu_unfavorite else R.string.menu_favorite)) },
                    leadingIcon = {
                        Icon(if (favorite) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder, null)
                    },
                    onClick = { menu = false; Repo.toggleFavorite(track.mediaId) }
                )
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.menu_add_playlist)) },
                    onClick = { menu = false; onAddToPlaylist() }
                )
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.menu_play_next)) },
                    onClick = { menu = false; onPlayNext() }
                )
                extraMenu.forEach { (label, action) ->
                    DropdownMenuItem(text = { Text(label) }, onClick = { menu = false; action() })
                }
            }
        }
    }
}
