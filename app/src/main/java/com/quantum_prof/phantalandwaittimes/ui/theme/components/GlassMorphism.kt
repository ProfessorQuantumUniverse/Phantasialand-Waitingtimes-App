package com.quantum_prof.phantalandwaittimes.ui.theme.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.quantum_prof.phantalandwaittimes.ui.theme.*
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun GlassMorphismCard(
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 20.dp,
    borderWidth: Dp = 1.5.dp,
    elevated: Boolean = false,
    glowEffect: Boolean = true,
    content: @Composable () -> Unit
) {
    // Animated glow effect
    val infiniteTransition = rememberInfiniteTransition(label = "glow")
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.7f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glowAlpha"
    )

    val backgroundBrush = if (elevated) {
        Brush.radialGradient(
            colors = listOf(
                GlassBackgroundElevated,
                GlassBackground,
                Color.Transparent
            ),
            radius = 600f
        )
    } else {
        Brush.linearGradient(
            colors = listOf(
                GlassHighlight.copy(alpha = 0.1f),
                GlassBackground,
                GlassBackground.copy(alpha = 0.05f)
            ),
            start = Offset(0f, 0f),
            end = Offset(1000f, 1000f)
        )
    }

    val borderBrush = Brush.sweepGradient(
        colors = listOf(
            GlassBorder,
            Color.Transparent,
            GlassBorder.copy(alpha = 0.3f),
            Color.Transparent,
            GlassBorder
        )
    )

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(cornerRadius))
            .then(
                if (glowEffect) {
                    Modifier.drawBehind {
                        drawGlowEffect(
                            cornerRadius = cornerRadius.toPx(),
                            glowRadius = 40.dp.toPx(),
                            alpha = glowAlpha * 0.4f
                        )
                    }
                } else Modifier
            )
            .background(backgroundBrush)
            .border(
                width = borderWidth,
                brush = borderBrush,
                shape = RoundedCornerShape(cornerRadius)
            )
    ) {
        content()
    }
}

@Composable
fun GlassMorphismSurface(
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 24.dp,
    intensity: Float = 1f,
    content: @Composable () -> Unit
) {
    val backgroundBrush = Brush.radialGradient(
        colors = listOf(
            GlassHighlight.copy(alpha = 0.15f * intensity),
            GlassBackground.copy(alpha = 0.8f * intensity),
            GlassBackground.copy(alpha = 0.4f * intensity),
            Color.Transparent
        ),
        radius = 1200f
    )

    val borderBrush = Brush.linearGradient(
        colors = listOf(
            GlassBorder.copy(alpha = 0.8f * intensity),
            Color.Transparent,
            GlassBorder.copy(alpha = 0.4f * intensity)
        )
    )

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(cornerRadius))
            .background(backgroundBrush)
            .border(
                width = (1.5f * intensity).dp,
                brush = borderBrush,
                shape = RoundedCornerShape(cornerRadius)
            )
    ) {
        content()
    }
}

@Composable
fun BlurredBackground(
    modifier: Modifier = Modifier,
    blurRadius: Dp = 30.dp,
    darkOverlay: Boolean = true
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .blur(radius = blurRadius)
            .background(
                brush = Brush.verticalGradient(
                    colors = if (darkOverlay) {
                        listOf(
                            Background,
                            BackgroundSecondary,
                            BackgroundGradientEnd
                        )
                    } else {
                        listOf(
                            Background.copy(alpha = 0.6f),
                            BackgroundSecondary.copy(alpha = 0.8f)
                        )
                    }
                )
            )
    )
}

@Composable
fun AnimatedGlassMorphismCard(
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 20.dp,
    isHighlighted: Boolean = false,
    content: @Composable () -> Unit
) {
    val scale by animateFloatAsState(
        targetValue = if (isHighlighted) 1.02f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "cardScale"
    )

    val glowIntensity by animateFloatAsState(
        targetValue = if (isHighlighted) 1.5f else 1f,
        animationSpec = tween(300),
        label = "glowIntensity"
    )

    GlassMorphismCard(
        modifier = modifier.scale(scale),
        cornerRadius = cornerRadius,
        elevated = isHighlighted,
        glowEffect = true,
        content = content
    )
}

private fun DrawScope.drawGlowEffect(
    cornerRadius: Float,
    glowRadius: Float,
    alpha: Float
) {
    val glowBrush = Brush.radialGradient(
        colors = listOf(
            Primary.copy(alpha = alpha),
            Primary.copy(alpha = alpha * 0.5f),
            Color.Transparent
        ),
        radius = glowRadius
    )

    drawRoundRect(
        brush = glowBrush,
        size = size.copy(
            width = size.width + glowRadius,
            height = size.height + glowRadius
        ),
        topLeft = Offset(-glowRadius / 2, -glowRadius / 2),
        cornerRadius = CornerRadius(cornerRadius + glowRadius / 4)
    )
}
