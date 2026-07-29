package com.quantum_prof.phantalandwaittimes.ui.theme.components

import androidx.compose.animation.core.EaseInOutSine
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer

/**
 * The small set of motion helpers the UI actually uses.
 *
 * The previous version of this file carried a dozen decorative effects (fire, lightning, rainbow
 * waves, particle explosions, starbursts) of which only a few were referenced, and several ran
 * `rememberInfiniteTransition` per list row — every visible card animating forever costs frames
 * and battery for no information gain. What remains is limited to motion that communicates state.
 */

/**
 * A soft pulse behind an icon. Reserved for genuinely live state such as an armed alert, so that
 * movement on screen still means something.
 */
@Composable
fun Modifier.pulsingGlow(
    glowColor: Color,
    animationDurationMillis: Int = 1_800
): Modifier = composed {
    val transition = rememberInfiniteTransition(label = "pulsingGlow")
    val alpha by transition.animateFloat(
        initialValue = 0.12f,
        targetValue = 0.38f,
        animationSpec = infiniteRepeatable(
            animation = tween(animationDurationMillis, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulsingGlowAlpha"
    )

    drawBehind {
        val radius = size.minDimension * 0.75f
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    glowColor.copy(alpha = alpha),
                    glowColor.copy(alpha = alpha * 0.4f),
                    Color.Transparent
                ),
                radius = radius
            ),
            radius = radius,
            center = center
        )
    }
}

/**
 * A short springy scale-up when an item becomes a favourite, settling back to its normal size.
 * Runs once per state change rather than continuously.
 */
@Composable
fun Modifier.favoriteEmphasis(isFavorite: Boolean): Modifier = composed {
    val scale by animateFloatAsState(
        targetValue = if (isFavorite) 1.03f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "favoriteEmphasis"
    )

    if (scale == 1f) {
        // Skip the graphics layer entirely when there is nothing to transform.
        this
    } else {
        graphicsLayer {
            scaleX = scale
            scaleY = scale
        }
    }
}
