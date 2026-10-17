// Prism — author: PWBad's | planning: Claude (Anthropic) | code: DeepSeek
package com.shtuchki.glassbox.ui

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.shtuchki.glassbox.R
import com.shtuchki.glassbox.player.PlayerViewModel
import com.shtuchki.glassbox.ui.glass.AppBackground
import com.shtuchki.glassbox.ui.glass.GlassPillButton
import com.shtuchki.glassbox.ui.glass.glass
import com.shtuchki.glassbox.ui.screens.LibraryScreen
import com.shtuchki.glassbox.ui.screens.ListDestination
import com.shtuchki.glassbox.ui.screens.NowPlayingScreen
import com.shtuchki.glassbox.ui.screens.PlaylistsScreen
import com.shtuchki.glassbox.ui.screens.SettingsScreen
import com.shtuchki.glassbox.ui.screens.ThemeEditorScreen
import com.shtuchki.glassbox.ui.screens.ThemesScreen

@Composable
fun AppRoot(vm: PlayerViewModel) {
    val hasAudio by vm.hasAudioPermission.collectAsState()
    val stack by vm.nav.collectAsState()
    val dest = stack.last()
    var asked by rememberSaveable { mutableStateOf(false) }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { vm.refreshLibrary() }

    val requestAll = {
        permissionLauncher.launch(
            arrayOf(
                Manifest.permission.READ_MEDIA_AUDIO,
                Manifest.permission.READ_MEDIA_VIDEO,
                Manifest.permission.POST_NOTIFICATIONS
            )
        )
    }

    LaunchedEffect(hasAudio) {
        if (!hasAudio && !asked) {
            asked = true
            requestAll()
        }
    }

    BackHandler(enabled = stack.size > 1) { vm.pop() }

    if (!hasAudio) {
        val context = LocalContext.current
        PermissionScreen(
            onGrant = { requestAll() },
            onOpenSettings = {
                context.startActivity(
                    Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                        data = Uri.fromParts("package", context.packageName, null)
                    }
                )
            }
        )
    } else {
        AnimatedContent(
            targetState = dest,
            transitionSpec = {
                when {
                    targetState == "player" ->
                        (slideInVertically(tween(380)) { it } + fadeIn(tween(250))) togetherWith fadeOut(tween(200))
                    initialState == "player" ->
                        fadeIn(tween(250)) togetherWith (slideOutVertically(tween(320)) { it } + fadeOut(tween(250)))
                    else -> fadeIn(tween(220)) togetherWith fadeOut(tween(180))
                }
            },
            label = "screens"
        ) { d ->
            when {
                d == "lib" -> LibraryScreen(vm)
                d == "player" -> NowPlayingScreen(vm)
                d == "settings" -> SettingsScreen(vm)
                d == "themes" -> ThemesScreen(vm)
                d == "playlists" -> PlaylistsScreen(vm)
                d.startsWith("editor:") -> ThemeEditorScreen(vm, d.substringAfter(":"))
                else -> ListDestination(d, vm)
            }
        }
    }
}

@Composable
private fun PermissionScreen(onGrant: () -> Unit, onOpenSettings: () -> Unit) {
    AppBackground {
        Column(
            modifier = Modifier.fillMaxSize().systemBarsPadding().padding(28.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Column(
                modifier = Modifier.glass(RoundedCornerShape(32.dp)).padding(28.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(Icons.Rounded.MusicNote, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(56.dp))
                Spacer(Modifier.height(16.dp))
                Text(
                    stringResource(R.string.perm_title),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(10.dp))
                Text(
                    stringResource(R.string.perm_text),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(22.dp))
                GlassPillButton(stringResource(R.string.perm_grant), Icons.Rounded.MusicNote, onGrant, accent = true)
                Spacer(Modifier.height(10.dp))
                GlassPillButton(stringResource(R.string.perm_open_settings), Icons.Rounded.Settings, onOpenSettings)
            }
        }
    }
}
