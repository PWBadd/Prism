// Prism — author: PWBad's | planning: Claude (Anthropic) | code: DeepSeek
package com.shtuchki.glassbox.ui.screens

import android.content.Intent
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.FolderOpen
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.shtuchki.glassbox.R
import com.shtuchki.glassbox.data.Repo
import com.shtuchki.glassbox.data.ThemeData
import com.shtuchki.glassbox.player.PlayerViewModel
import com.shtuchki.glassbox.ui.glass.AppBackground
import com.shtuchki.glassbox.ui.glass.GlassIconButton
import com.shtuchki.glassbox.ui.glass.GlassPillButton
import com.shtuchki.glassbox.ui.glass.glass
import com.shtuchki.glassbox.ui.theme.StockThemes
import com.shtuchki.glassbox.ui.theme.resolveTheme
import com.shtuchki.glassbox.ui.theme.themeColor
import com.shtuchki.glassbox.util.currentLanguage
import java.util.Locale

private fun effectiveLang(): String {
    val c = currentLanguage()
    return if (c == "system") Locale.getDefault().language else c
}

@Composable
fun ThemesScreen(vm: PlayerViewModel) {
    val data by Repo.data.collectAsState()
    val context = LocalContext.current
    val systemDark = isSystemInDarkTheme()
    val isDarkNow = when (data.themeMode) { "dark" -> true; "light" -> false; else -> systemDark }
    val activeId = resolveTheme(data, isDarkNow).id
    val lang = effectiveLang()
    var importing by remember { mutableStateOf(false) }

    AppBackground {
        Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()) {
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                GlassIconButton(Icons.AutoMirrored.Rounded.ArrowBack, stringResource(R.string.back), { vm.pop() })
                Spacer(Modifier.width(14.dp))
                Text(
                    stringResource(R.string.themes_title),
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                GlassIconButton(Icons.Rounded.FolderOpen, stringResource(R.string.theme_import), { importing = true }, size = 44.dp)
                Spacer(Modifier.width(8.dp))
                GlassIconButton(
                    Icons.Rounded.Add, stringResource(R.string.theme_new),
                    {
                        val base = resolveTheme(data, isDarkNow)
                        val fresh = base.copy(id = "custom_" + System.currentTimeMillis(), name = "My theme", nameRu = "Моя тема")
                        Repo.upsertTheme(fresh)
                        Repo.selectTheme(fresh)
                        vm.push("editor:" + fresh.id)
                    },
                    accent = true, size = 44.dp
                )
            }

            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 24.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (data.customThemes.isNotEmpty()) {
                    item(span = { GridItemSpan(maxLineSpan) }) { SectionTitle(stringResource(R.string.themes_mine)) }
                    items(data.customThemes, key = { it.id }) { t ->
                        ThemeCard(
                            t, lang, t.id == activeId,
                            onClick = { Repo.selectTheme(t) },
                            onEdit = { Repo.selectTheme(t); vm.push("editor:" + t.id) },
                            onShare = {
                                val send = Intent(Intent.ACTION_SEND).apply {
                                    type = "text/plain"
                                    putExtra(Intent.EXTRA_TEXT, Repo.themeToText(t))
                                }
                                context.startActivity(Intent.createChooser(send, null))
                            },
                            onDelete = { Repo.deleteTheme(t.id) }
                        )
                    }
                }
                item(span = { GridItemSpan(maxLineSpan) }) { SectionTitle(stringResource(R.string.themes_dark)) }
                items(StockThemes.all.filter { it.isDark }, key = { it.id }) { t ->
                    ThemeCard(t, lang, t.id == activeId, onClick = { Repo.selectTheme(t) })
                }
                item(span = { GridItemSpan(maxLineSpan) }) { SectionTitle(stringResource(R.string.themes_light)) }
                items(StockThemes.all.filter { !it.isDark }, key = { it.id }) { t ->
                    ThemeCard(t, lang, t.id == activeId, onClick = { Repo.selectTheme(t) })
                }
            }
        }
    }

    if (importing) ImportThemeDialog(onDismiss = { importing = false })
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text,
        modifier = Modifier.padding(top = 10.dp, bottom = 2.dp),
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
}

@Composable
private fun ThemeCard(
    t: ThemeData,
    lang: String,
    selected: Boolean,
    onClick: () -> Unit,
    onEdit: (() -> Unit)? = null,
    onShare: (() -> Unit)? = null,
    onDelete: (() -> Unit)? = null
) {
    val scheme = MaterialTheme.colorScheme
    val shape = RoundedCornerShape(22.dp)
    val bg = themeColor(t.background)
    val acc = themeColor(t.accent)
    Column(
        Modifier.fillMaxWidth()
            .clip(shape)
            .then(if (selected) Modifier.border(2.dp, scheme.primary, shape) else Modifier.border(1.dp, scheme.onBackground.copy(alpha = 0.12f), shape))
            .clickable(onClick = onClick)
    ) {
        Box(Modifier.fillMaxWidth().height(86.dp).background(bg), contentAlignment = Alignment.CenterStart) {
            Row(Modifier.padding(14.dp), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(34.dp).clip(CircleShape).background(acc))
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Box(Modifier.width(54.dp).height(8.dp).clip(CircleShape).background(themeColor(t.text)))
                    Box(Modifier.width(38.dp).height(8.dp).clip(CircleShape).background(themeColor(t.textSecondary)))
                }
            }
            Box(Modifier.align(Alignment.BottomEnd).padding(10.dp).width(36.dp).height(14.dp).clip(CircleShape).background(themeColor(t.surface)))
            if (selected) {
                Box(Modifier.align(Alignment.TopEnd).padding(8.dp).size(22.dp).clip(CircleShape).background(acc), contentAlignment = Alignment.Center) {
                    Icon(Icons.Rounded.Check, null, tint = if (t.isDark) Color.Black else Color.White, modifier = Modifier.size(14.dp))
                }
            }
        }
        Row(
            Modifier.fillMaxWidth().background(scheme.surface.copy(alpha = 0.6f)).padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                t.displayName(lang),
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (onEdit != null) {
                Icon(Icons.Rounded.Edit, stringResource(R.string.edit), modifier = Modifier.size(20.dp).clickable { onEdit() })
                Spacer(Modifier.width(8.dp))
            }
            if (onShare != null) {
                Icon(Icons.Rounded.Share, stringResource(R.string.share), modifier = Modifier.size(20.dp).clickable { onShare() })
                Spacer(Modifier.width(8.dp))
            }
            if (onDelete != null) {
                Icon(Icons.Rounded.Delete, stringResource(R.string.delete), modifier = Modifier.size(20.dp).clickable { onDelete() })
            }
        }
    }
}

@Composable
private fun ImportThemeDialog(onDismiss: () -> Unit) {
    val context = LocalContext.current
    var text by remember { mutableStateOf("") }
    val okText = stringResource(R.string.theme_import_ok)
    val failText = stringResource(R.string.theme_import_fail)
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            val s = runCatching {
                context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
            }.getOrNull()
            if (s != null) text = s
        }
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
        title = { Text(stringResource(R.string.theme_import)) },
        text = {
            Column {
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    placeholder = { Text(stringResource(R.string.theme_paste_hint)) },
                    maxLines = 5,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(10.dp))
                GlassPillButton(
                    stringResource(R.string.theme_from_file), Icons.Rounded.FolderOpen,
                    onClick = { picker.launch(arrayOf("*/*")) }
                )
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val ok = Repo.importThemeText(text) != null
                Toast.makeText(context, if (ok) okText else failText, Toast.LENGTH_SHORT).show()
                if (ok) onDismiss()
            }) { Text(stringResource(R.string.import_label)) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } }
    )
}

/** Редактор своей темы: меняется всё, что видно в приложении, сразу вживую. */
@Composable
fun ThemeEditorScreen(vm: PlayerViewModel, id: String) {
    val data by Repo.data.collectAsState()
    val theme = data.customThemes.firstOrNull { it.id == id }
    LaunchedEffect(theme == null) { if (theme == null) vm.pop() }
    if (theme == null) return
    val scheme = MaterialTheme.colorScheme

    AppBackground {
        Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()) {
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                GlassIconButton(Icons.AutoMirrored.Rounded.ArrowBack, stringResource(R.string.back), { vm.pop() })
                Spacer(Modifier.width(14.dp))
                Text(
                    stringResource(R.string.edit),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            }
            LazyColumn(contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp)) {
                item {
                    OutlinedTextField(
                        value = theme.name,
                        onValueChange = { Repo.upsertTheme(theme.copy(name = it, nameRu = it)) },
                        label = { Text(stringResource(R.string.theme_name)) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Row(
                        Modifier.fillMaxWidth().padding(vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(stringResource(R.string.theme_is_dark), modifier = Modifier.weight(1f))
                        Switch(checked = theme.isDark, onCheckedChange = { Repo.upsertTheme(theme.copy(isDark = it)); Repo.selectTheme(theme.copy(isDark = it)) })
                    }
                }
                item { ColorRow(stringResource(R.string.color_background), theme.background, false) { Repo.upsertTheme(theme.copy(background = it)) } }
                item { ColorRow(stringResource(R.string.color_surface), theme.surface, false) { Repo.upsertTheme(theme.copy(surface = it)) } }
                item { ColorRow(stringResource(R.string.color_accent), theme.accent, false) { Repo.upsertTheme(theme.copy(accent = it)) } }
                item { ColorRow(stringResource(R.string.color_text), theme.text, false) { Repo.upsertTheme(theme.copy(text = it)) } }
                item { ColorRow(stringResource(R.string.color_text_secondary), theme.textSecondary, false) { Repo.upsertTheme(theme.copy(textSecondary = it)) } }
                item { ColorRow(stringResource(R.string.color_glass), theme.glassTint, true) { Repo.upsertTheme(theme.copy(glassTint = it)) } }
                item { ColorRow(stringResource(R.string.color_glass_border), theme.glassBorder, true) { Repo.upsertTheme(theme.copy(glassBorder = it)) } }
                item { Spacer(Modifier.height(24.dp)) }
            }
        }
    }
}

private fun pack(a: Int, r: Int, g: Int, b: Int): Long =
    ((a.toLong() and 0xFF) shl 24) or ((r.toLong() and 0xFF) shl 16) or ((g.toLong() and 0xFF) shl 8) or (b.toLong() and 0xFF)

/** Строка цвета: нажми, чтобы открыть ползунки (прозрачность, красный, зелёный, синий). */
@Composable
private fun ColorRow(label: String, value: Long, withAlpha: Boolean, onChange: (Long) -> Unit) {
    var open by remember { mutableStateOf(false) }
    val scheme = MaterialTheme.colorScheme
    val shape = RoundedCornerShape(20.dp)
    val a = ((value shr 24) and 0xFF).toInt()
    val r = ((value shr 16) and 0xFF).toInt()
    val g = ((value shr 8) and 0xFF).toInt()
    val b = (value and 0xFF).toInt()

    Column(Modifier.fillMaxWidth().padding(vertical = 4.dp).glass(shape).clip(shape)) {
        Row(
            Modifier.fillMaxWidth().clickable { open = !open }.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                Modifier.size(34.dp).clip(CircleShape).background(Color(value.toInt()))
                    .border(1.dp, scheme.onBackground.copy(alpha = 0.3f), CircleShape)
            )
            Spacer(Modifier.width(14.dp))
            Text(label, modifier = Modifier.weight(1f), fontWeight = FontWeight.SemiBold)
            Text(
                String.format("#%08X", value and 0xFFFFFFFFL),
                style = MaterialTheme.typography.bodySmall,
                color = scheme.onSurfaceVariant
            )
        }
        if (open) {
            Column(Modifier.padding(horizontal = 14.dp).padding(bottom = 10.dp)) {
                if (withAlpha) ChannelSlider("A", a) { onChange(pack(it, r, g, b)) }
                ChannelSlider("R", r) { onChange(pack(if (withAlpha) a else 255, it, g, b)) }
                ChannelSlider("G", g) { onChange(pack(if (withAlpha) a else 255, r, it, b)) }
                ChannelSlider("B", b) { onChange(pack(if (withAlpha) a else 255, r, g, it)) }
            }
        }
    }
}

@Composable
private fun ChannelSlider(name: String, value: Int, onChange: (Int) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(name, modifier = Modifier.width(22.dp), style = MaterialTheme.typography.labelLarge)
        Slider(
            value = value.toFloat(),
            onValueChange = { onChange(it.toInt()) },
            valueRange = 0f..255f,
            modifier = Modifier.weight(1f),
            colors = SliderDefaults.colors(
                thumbColor = MaterialTheme.colorScheme.primary,
                activeTrackColor = MaterialTheme.colorScheme.primary
            )
        )
        Text(value.toString(), modifier = Modifier.width(36.dp), style = MaterialTheme.typography.bodySmall)
    }
}
