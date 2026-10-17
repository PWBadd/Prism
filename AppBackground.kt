// Prism — author: PWBad's | planning: Claude (Anthropic) | code: DeepSeek
package com.shtuchki.glassbox.ui.glass

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import com.shtuchki.glassbox.perf.PerformanceManager

/**
 * Фон приложения: градиент от цвета акцента (он может идти от обложки) и мягкое пятно.
 * Если телефон греется, пятно отключается.
 */
@Composable
fun AppBackground(
    modifier: Modifier = Modifier,
    strength: Float = 0.14f,
    content: @Composable BoxScope.() -> Unit
) {
    val bg = MaterialTheme.colorScheme.background
    val primary = MaterialTheme.colorScheme.primary
    val lite by PerformanceManager.lite.collectAsState()
    val top by animateColorAsState(lerp(bg, primary, strength), tween(500), label = "bgTop")
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(bg)
            .drawBehind {
                drawRect(Brush.verticalGradient(listOf(top, bg), endY = size.height * 0.8f))
                if (!lite) {
                    drawRect(
                        Brush.radialGradient(
                            colors = listOf(primary.copy(alpha = 0.16f), Color.Transparent),
                            center = Offset(size.width * 0.92f, size.height * 0.12f),
                            radius = size.width * 0.85f
                        )
                    )
                }
            },
        content = content
    )
}
