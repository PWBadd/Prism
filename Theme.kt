// Prism — author: PWBad's | planning: Claude (Anthropic) | code: DeepSeek
package com.shtuchki.glassbox.ui.theme

import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalContext
import com.shtuchki.glassbox.data.AppData
import com.shtuchki.glassbox.data.Repo
import com.shtuchki.glassbox.data.ThemeData

/** Параметры «стекла»: заливка, рамка и блик. */
@Immutable
class GlassStyle(
    val tint: Color,
    val borderTop: Color,
    val borderBottom: Color,
    val highlight: Color
)

val LocalGlass = staticCompositionLocalOf {
    GlassStyle(Color.White.copy(alpha = 0.09f), Color.White.copy(alpha = 0.38f), Color.White.copy(alpha = 0.06f), Color.White.copy(alpha = 0.14f))
}

/** 20 готовых тем: 10 тёмных (от насыщенных к пастельным) и 10 светлых. */
object StockThemes {
    private fun dark(id: String, en: String, ru: String, bg: Long, surface: Long, accent: Long, text: Long, text2: Long) =
        ThemeData(id, en, ru, true, bg, surface, accent, text, text2, 0x17FFFFFF, 0x61FFFFFF)

    private fun light(id: String, en: String, ru: String, bg: Long, surface: Long, accent: Long, text: Long, text2: Long) =
        ThemeData(id, en, ru, false, bg, surface, accent, text, text2, 0x8CFFFFFF, 0xF2FFFFFF)

    val all: List<ThemeData> = listOf(
        dark("dark_amethyst", "Amethyst", "Аметист", 0xFF0D0A1A, 0xFF16122A, 0xFFC4A1FF, 0xFFF1ECFF, 0xFFB9B1D6),
        dark("dark_cyan", "Neon Cyan", "Неон", 0xFF061418, 0xFF0C2229, 0xFF22E3F2, 0xFFE6FBFF, 0xFF8DBFC7),
        dark("dark_crimson", "Crimson", "Багрянец", 0xFF160608, 0xFF260C10, 0xFFFF4D6A, 0xFFFFEDF0, 0xFFCFA0A8),
        dark("dark_emerald", "Emerald", "Изумруд", 0xFF04130D, 0xFF0A2218, 0xFF2EE59D, 0xFFE8FFF4, 0xFF8FC7AE),
        dark("dark_ember", "Ember", "Уголь", 0xFF150B04, 0xFF26150A, 0xFFFF9A3D, 0xFFFFF1E3, 0xFFD1AE8D),
        dark("dark_ocean", "Deep Ocean", "Глубина", 0xFF060C1C, 0xFF0D1730, 0xFF5B8CFF, 0xFFEAF0FF, 0xFF9DAED6),
        dark("dark_graphite", "Graphite", "Графит", 0xFF0F0F10, 0xFF1A1A1C, 0xFFE0E0E0, 0xFFF4F4F5, 0xFF9C9CA3),
        dark("dark_rose", "Dusty Rose", "Пыльная роза", 0xFF1E1519, 0xFF2B1E24, 0xFFF4B8CC, 0xFFFBEFF3, 0xFFC9A9B5),
        dark("dark_mint", "Mint Mist", "Мятный туман", 0xFF121C19, 0xFF1B2A25, 0xFFA8E6CF, 0xFFEFFAF5, 0xFFA3C4B7),
        dark("dark_lavender", "Lavender Haze", "Лавандовая дымка", 0xFF1A1723, 0xFF252132, 0xFFCDBBF7, 0xFFF3EFFC, 0xFFB5ADCB),

        light("light_lilac", "Lilac", "Сирень", 0xFFF6F2FF, 0xFFFFFFFF, 0xFF6D3FE0, 0xFF1B1530, 0xFF6B6385),
        light("light_sky", "Sky", "Небо", 0xFFEEF7FF, 0xFFFFFFFF, 0xFF0284C7, 0xFF0B1B2B, 0xFF5A7288),
        light("light_rose", "Rose", "Роза", 0xFFFFF1F4, 0xFFFFFFFF, 0xFFE11D48, 0xFF2A0B14, 0xFF8A5964),
        light("light_emerald", "Emerald", "Изумруд", 0xFFEFFBF5, 0xFFFFFFFF, 0xFF059669, 0xFF08231A, 0xFF4F7A68),
        light("light_amber", "Amber", "Янтарь", 0xFFFFF8EB, 0xFFFFFFFF, 0xFFD97706, 0xFF2B1A05, 0xFF8A6B3D),
        light("light_coral", "Coral", "Коралл", 0xFFFFF3F0, 0xFFFFFFFF, 0xFFF2593B, 0xFF2D120B, 0xFF8F5E54),
        light("light_paper", "Paper", "Бумага", 0xFFF5F5F5, 0xFFFFFFFF, 0xFF111827, 0xFF111827, 0xFF6B7280),
        light("light_peach", "Peach", "Персик", 0xFFFFF3EE, 0xFFFFFFFF, 0xFFE8927C, 0xFF3A211B, 0xFF9A7468),
        light("light_mint", "Mint", "Мята", 0xFFF0FAF6, 0xFFFFFFFF, 0xFF4DB6A0, 0xFF16302A, 0xFF5E8A80),
        light("light_cotton", "Cotton Candy", "Сахарная вата", 0xFFFFF0F8, 0xFFFFFFFF, 0xFFD77FB0, 0xFF37172B, 0xFF9A6F89)
    )
}

fun resolveTheme(d: AppData, isDark: Boolean): ThemeData {
    val id = if (isDark) d.darkThemeId else d.lightThemeId
    return d.customThemes.firstOrNull { it.id == id }
        ?: StockThemes.all.firstOrNull { it.id == id }
        ?: StockThemes.all.first { it.isDark == isDark }
}

/** Делает цвет обложки читаемым на фоне темы. */
fun Color.ensureContrast(dark: Boolean): Color {
    var c = this
    if (dark && c.luminance() < 0.30f) c = lerp(c, Color.White, 0.45f)
    if (!dark && c.luminance() > 0.55f) c = lerp(c, Color.Black, 0.35f)
    return c
}

fun themeColor(v: Long): Color = Color(v.toInt())

@Composable
fun PrismTheme(artAccent: Color? = null, content: @Composable () -> Unit) {
    val data by Repo.data.collectAsState()
    val systemDark = isSystemInDarkTheme()
    val dark = when (data.themeMode) {
        "dark" -> true
        "light" -> false
        else -> systemDark
    }
    val t = resolveTheme(data, dark)

    val baseAccent = themeColor(t.accent)
    val target = if (data.themeFromArt && artAccent != null) artAccent.ensureContrast(t.isDark) else baseAccent
    val accent by animateColorAsState(target, tween(600), label = "accent")

    val bg = themeColor(t.background)
    val surface = themeColor(t.surface)
    val text = themeColor(t.text)
    val text2 = themeColor(t.textSecondary)
    val onAccent = if (accent.luminance() > 0.5f) Color(0xFF111111) else Color.White

    val scheme = if (t.isDark) {
        darkColorScheme(
            primary = accent, onPrimary = onAccent, secondary = accent, background = bg, onBackground = text,
            surface = surface, onSurface = text, onSurfaceVariant = text2, surfaceVariant = surface
        )
    } else {
        lightColorScheme(
            primary = accent, onPrimary = onAccent, secondary = accent, background = bg, onBackground = text,
            surface = surface, onSurface = text, onSurfaceVariant = text2, surfaceVariant = surface
        )
    }
    val glass = GlassStyle(
        tint = themeColor(t.glassTint),
        borderTop = themeColor(t.glassBorder),
        borderBottom = themeColor(t.glassBorder).copy(alpha = 0.08f),
        highlight = Color.White.copy(alpha = if (t.isDark) 0.14f else 0.60f)
    )

    // Цвет значков в статус-баре под выбранную тему (а не под системную)
    val context = LocalContext.current
    LaunchedEffect(t.isDark) {
        val activity = context as? ComponentActivity
        if (activity != null) {
            val style = if (t.isDark) {
                SystemBarStyle.dark(android.graphics.Color.TRANSPARENT)
            } else {
                SystemBarStyle.light(android.graphics.Color.TRANSPARENT, android.graphics.Color.TRANSPARENT)
            }
            activity.enableEdgeToEdge(statusBarStyle = style, navigationBarStyle = style)
        }
    }

    CompositionLocalProvider(LocalGlass provides glass) {
        MaterialTheme(colorScheme = scheme, content = content)
    }
}
