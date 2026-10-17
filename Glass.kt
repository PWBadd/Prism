// Glassbox — author: PWBad's | planning: Claude (Anthropic) | code: DeepSeek
package com.shtuchki.glassbox.ui.glass

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.shtuchki.glassbox.ui.theme.LocalGlass

/**
 * «Жидкое стекло» для маленьких элементов: полупрозрачная заливка, блик сверху
 * и тонкая светлая рамка. Фон экрана НЕ размывается.
 */
@Composable
fun Modifier.glass(shape: Shape): Modifier {
    val g = LocalGlass.current
    val highlight = remember(g) { Brush.verticalGradient(0f to g.highlight, 0.7f to Color.Transparent) }
    val border = remember(g) { Brush.verticalGradient(listOf(g.borderTop, g.borderBottom)) }
    return this
        .background(g.tint, shape)
        .background(highlight, shape)
        .border(1.dp, border, shape)
}

@Composable
private fun rememberPressScale(source: InteractionSource): State<Float> {
    val pressed by source.collectIsPressedAsState()
    return animateFloatAsState(
        targetValue = if (pressed) 0.90f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium),
        label = "press"
    )
}

/** Круглая стеклянная кнопка с иконкой. */
@Composable
fun GlassIconButton(
    icon: ImageVector,
    contentDescription: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = 48.dp,
    iconSize: Dp = 24.dp,
    active: Boolean = false,
    accent: Boolean = false
) {
    val interaction = remember { MutableInteractionSource() }
    val scale = rememberPressScale(interaction)
    val scheme = MaterialTheme.colorScheme
    val tint = when {
        accent -> scheme.onPrimary
        active -> scheme.primary
        else -> scheme.onBackground
    }
    Box(
        modifier = modifier
            .size(size)
            .graphicsLayer {
                scaleX = scale.value
                scaleY = scale.value
            }
            .glass(CircleShape)
            .then(if (accent) Modifier.background(scheme.primary.copy(alpha = 0.92f), CircleShape) else Modifier)
            .clip(CircleShape)
            .clickable(interactionSource = interaction, indication = null, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, contentDescription, tint = tint, modifier = Modifier.size(iconSize))
    }
}

/** Стеклянная кнопка-«таблетка» с иконкой и текстом. */
@Composable
fun GlassPillButton(
    text: String,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    accent: Boolean = false
) {
    val interaction = remember { MutableInteractionSource() }
    val scale = rememberPressScale(interaction)
    val scheme = MaterialTheme.colorScheme
    val shape = RoundedCornerShape(50)
    val content = if (accent) scheme.primary else scheme.onBackground
    Row(
        modifier = modifier
            .graphicsLayer {
                scaleX = scale.value
                scaleY = scale.value
            }
            .glass(shape)
            .then(if (accent) Modifier.background(scheme.primary.copy(alpha = 0.20f), shape) else Modifier)
            .clip(shape)
            .clickable(interactionSource = interaction, indication = null, onClick = onClick)
            .padding(horizontal = 18.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, null, tint = content, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(8.dp))
        Text(text, color = content, style = MaterialTheme.typography.labelLarge)
    }
}

/** Стеклянная «чипса» (вкладки, переключатели). */
@Composable
fun GlassChip(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val interaction = remember { MutableInteractionSource() }
    val scale = rememberPressScale(interaction)
    val scheme = MaterialTheme.colorScheme
    val shape = RoundedCornerShape(50)
    Box(
        modifier = modifier
            .graphicsLayer {
                scaleX = scale.value
                scaleY = scale.value
            }
            .glass(shape)
            .then(if (selected) Modifier.background(scheme.primary.copy(alpha = 0.24f), shape) else Modifier)
            .clip(shape)
            .clickable(interactionSource = interaction, indication = null, onClick = onClick)
            .padding(horizontal = 18.dp, vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text,
            style = MaterialTheme.typography.labelLarge,
            color = if (selected) scheme.primary else scheme.onBackground
        )
    }
}
