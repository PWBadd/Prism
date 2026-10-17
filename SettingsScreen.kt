// Prism — author: PWBad's | planning: Claude (Anthropic) | code: DeepSeek
package com.shtuchki.glassbox.ui.screens

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Backup
import androidx.compose.material.icons.rounded.Language
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.Restore
import androidx.compose.material.icons.rounded.Send
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.shtuchki.glassbox.Credits
import com.shtuchki.glassbox.R
import com.shtuchki.glassbox.data.Repo
import com.shtuchki.glassbox.player.PlayerViewModel
import com.shtuchki.glassbox.ui.glass.AppBackground
import com.shtuchki.glassbox.ui.glass.GlassChip
import com.shtuchki.glassbox.ui.glass.GlassIconButton
import com.shtuchki.glassbox.ui.glass.GlassPillButton
import com.shtuchki.glassbox.ui.glass.glass
import com.shtuchki.glassbox.util.currentLanguage
import com.shtuchki.glassbox.util.setLanguage

@Composable
fun SettingsScreen(vm: PlayerViewModel) {
    val context = LocalContext.current
    val scheme = MaterialTheme.colorScheme
    val data by Repo.data.collectAsState()
    var lang by remember { mutableStateOf(currentLanguage()) }
    val versionName = remember {
        runCatching { context.packageManager.getPackageInfo(context.packageName, 0).versionName }
            .getOrNull().orEmpty()
    }
    val okText = stringResource(R.string.backup_ok)
    val failText = stringResource(R.string.backup_fail)

    val exportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        if (uri != null) {
            val ok = runCatching {
                context.contentResolver.openOutputStream(uri)?.use { it.write(Repo.exportJson().toByteArray()) }
            }.isSuccess
            Toast.makeText(context, if (ok) okText else failText, Toast.LENGTH_SHORT).show()
        }
    }
    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            val text = runCatching {
                context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
            }.getOrNull()
            val ok = text != null && Repo.importJson(text)
            Toast.makeText(context, if (ok) okText else failText, Toast.LENGTH_SHORT).show()
        }
    }

    AppBackground {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                GlassIconButton(Icons.AutoMirrored.Rounded.ArrowBack, stringResource(R.string.back), { vm.pop() })
                Spacer(Modifier.width(14.dp))
                Text(stringResource(R.string.settings), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.height(20.dp))

            // Язык
            Card {
                CardTitle(Icons.Rounded.Language, stringResource(R.string.language))
                Spacer(Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    GlassChip(stringResource(R.string.language_system), lang == "system", { lang = "system"; setLanguage("system") })
                    GlassChip(stringResource(R.string.language_ru), lang == "ru", { lang = "ru"; setLanguage("ru") })
                    GlassChip(stringResource(R.string.language_en), lang == "en", { lang = "en"; setLanguage("en") })
                }
            }
            Spacer(Modifier.height(14.dp))

            // Тема
            Card {
                CardTitle(Icons.Rounded.Palette, stringResource(R.string.settings_theme_mode))
                Spacer(Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    GlassChip(stringResource(R.string.mode_system), data.themeMode == "system", { Repo.update { it.copy(themeMode = "system") } })
                    GlassChip(stringResource(R.string.mode_light), data.themeMode == "light", { Repo.update { it.copy(themeMode = "light") } })
                    GlassChip(stringResource(R.string.mode_dark), data.themeMode == "dark", { Repo.update { it.copy(themeMode = "dark") } })
                }
                Spacer(Modifier.height(12.dp))
                GlassPillButton(stringResource(R.string.settings_choose_theme), Icons.Rounded.Palette, onClick = { vm.push("themes") }, accent = true)
                Spacer(Modifier.height(10.dp))
                SwitchRow(stringResource(R.string.theme_from_art), stringResource(R.string.theme_from_art_sub), data.themeFromArt) { v ->
                    Repo.update { it.copy(themeFromArt = v) }
                }
            }
            Spacer(Modifier.height(14.dp))

            // Воспроизведение
            Card {
                Text(stringResource(R.string.settings_playback), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(10.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(stringResource(R.string.crossfade), modifier = Modifier.weight(1f))
                    Text(
                        if (data.crossfadeSec == 0) stringResource(R.string.crossfade_off)
                        else stringResource(R.string.crossfade_value, data.crossfadeSec),
                        color = scheme.primary,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                Slider(
                    value = data.crossfadeSec.toFloat(),
                    onValueChange = { v -> Repo.update { it.copy(crossfadeSec = v.toInt()) } },
                    valueRange = 0f..12f,
                    steps = 11,
                    colors = SliderDefaults.colors(thumbColor = scheme.primary, activeTrackColor = scheme.primary)
                )
                SwitchRow(stringResource(R.string.fade_on_pause), "", data.fadeOnPause) { v ->
                    Repo.update { it.copy(fadeOnPause = v) }
                }
            }
            Spacer(Modifier.height(14.dp))

            // Производительность
            Card {
                Text(stringResource(R.string.settings_perf), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    GlassChip(stringResource(R.string.perf_auto), data.perfMode == "auto", { Repo.update { it.copy(perfMode = "auto") } })
                    GlassChip(stringResource(R.string.perf_eco), data.perfMode == "eco", { Repo.update { it.copy(perfMode = "eco") } })
                    GlassChip(stringResource(R.string.perf_max), data.perfMode == "max", { Repo.update { it.copy(perfMode = "max") } })
                }
            }
            Spacer(Modifier.height(14.dp))

            // Резервная копия
            Card {
                Text(stringResource(R.string.settings_backup), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    GlassPillButton(stringResource(R.string.backup_export), Icons.Rounded.Backup, onClick = { exportLauncher.launch("prism-backup.json") })
                    GlassPillButton(stringResource(R.string.backup_import), Icons.Rounded.Restore, onClick = { importLauncher.launch(arrayOf("*/*")) })
                }
            }
            Spacer(Modifier.height(14.dp))

            // Авторство
            Card {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Image(painterResource(R.drawable.prism_logo), null, modifier = Modifier.size(48.dp))
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text(Credits.APP_NAME, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                        if (versionName.isNotEmpty()) {
                            Text(stringResource(R.string.version, versionName), style = MaterialTheme.typography.bodySmall, color = scheme.onSurfaceVariant)
                        }
                    }
                }
                Spacer(Modifier.height(14.dp))
                Text(stringResource(R.string.credits_author, Credits.AUTHOR))
                Spacer(Modifier.height(4.dp))
                Text(stringResource(R.string.credits_assistant, Credits.ASSISTANT))
                Spacer(Modifier.height(4.dp))
                Text(stringResource(R.string.credits_coder, Credits.CODER))
                Spacer(Modifier.height(14.dp))
                GlassPillButton(
                    text = stringResource(R.string.telegram_channel) + "  " + Credits.TELEGRAM_HANDLE,
                    icon = Icons.Rounded.Send,
                    accent = true,
                    onClick = {
                        try {
                            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(Credits.TELEGRAM_URL)))
                        } catch (e: ActivityNotFoundException) {
                        }
                    }
                )
                Spacer(Modifier.height(12.dp))
                Text(stringResource(R.string.made_by), style = MaterialTheme.typography.bodySmall, color = scheme.onSurfaceVariant)
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun Card(content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = Modifier.fillMaxWidth().glass(RoundedCornerShape(26.dp)).padding(18.dp),
        content = content
    )
}

@Composable
private fun CardTitle(icon: androidx.compose.ui.graphics.vector.ImageVector, text: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(22.dp))
        Spacer(Modifier.width(10.dp))
        Text(text, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun SwitchRow(title: String, sub: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(title)
            if (sub.isNotEmpty()) {
                Text(sub, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        Switch(checked = checked, onCheckedChange = onChange)
    }
}
