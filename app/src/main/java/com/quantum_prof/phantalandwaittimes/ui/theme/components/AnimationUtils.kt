package com.quantum_prof.phantalandwaittimes.ui.theme.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import kotlin.math.*
import kotlin.random.Random

// 🎆 KRASSE ANIMATIONEN FÜR PHANTASIALAND APP! 🎆

/**
 * 🌟 Pulsierender Glow-Effekt für wichtige Elemente
 */
@Composable
fun Modifier.pulsingGlow(
    glowColor: Color = Color.Cyan,
    animationDuration: Int = 2000
): Modifier = composed {
    val infiniteTransition = rememberInfiniteTransition(label = "glow")
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(animationDuration, easing = EaseInOutCubic),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glow_alpha"
    )

    val scale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(animationDuration, easing = EaseInOutCubic),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glow_scale"
    )

    this
        .scale(scale)
        .drawBehind {
            val brush = Brush.radialGradient(
                colors = listOf(
                    glowColor.copy(alpha = glowAlpha),
                    Color.Transparent
                ),
                radius = size.maxDimension * 0.7f
            )
            drawRect(brush = brush)
        }
}

/**
 * 🚀 Partikel-Explosion Effekt
 */
@Composable
fun ParticleExplosion(
    isActive: Boolean,
    particleColor: Color = Color.Yellow,
    particleCount: Int = 30,
    modifier: Modifier = Modifier
) {
    val particles = remember {
        List(particleCount) {
            Particle(
                Random.nextFloat() * 360f, // angle
                Random.nextFloat() * 100f + 50f, // speed
                Random.nextFloat() * 10f + 5f // size
            )
        }
    }

    val animationProgress by animateFloatAsState(
        targetValue = if (isActive) 1f else 0f,
        animationSpec = tween(1500, easing = EaseOutQuart),
        label = "particle_explosion"
    )

    Canvas(modifier = modifier.fillMaxSize()) {
        if (animationProgress > 0f) {
            particles.forEach { particle ->
                val distance = particle.speed * animationProgress
                val x = center.x + cos(particle.angle * PI / 180) * distance
                val y = center.y + sin(particle.angle * PI / 180) * distance
                val alpha = (1f - animationProgress).coerceAtLeast(0f)

                drawCircle(
                    color = particleColor.copy(alpha = alpha),
                    radius = particle.size * (1f + animationProgress),
                    center = Offset(x.toFloat(), y.toFloat())
                )
            }
        }
    }
}

data class Particle(
    val angle: Float,
    val speed: Float,
    val size: Float
)

/**
 * 🌊 Liquid/Morphing Animation
 */
@Composable
fun Modifier.liquidMorph(
    isActive: Boolean = true,
    color: Color = Color.Blue.copy(alpha = 0.3f),
    animationDuration: Int = 3000
): Modifier = composed {
    val infiniteTransition = rememberInfiniteTransition(label = "liquid")

    val morphProgress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(animationDuration, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "morph_progress"
    )

    this.drawBehind {
        if (isActive) {
            val path = Path().apply {
                val width = size.width
                val height = size.height
                val amplitude = height * 0.1f
                val frequency = 4f

                moveTo(0f, height * 0.3f)

                for (x in 0..width.toInt() step 5) {
                    val normalizedX = x / width
                    val wave1 = sin(normalizedX * frequency * PI + morphProgress * 2 * PI) * amplitude
                    val wave2 = cos(normalizedX * frequency * 1.5f * PI + morphProgress * 3 * PI) * amplitude * 0.5f
                    val y = height * 0.3f + wave1 + wave2
                    lineTo(x.toFloat(), y.toFloat())
                }

                lineTo(width, height)
                lineTo(0f, height)
                close()
            }

            drawPath(
                path = path,
                brush = Brush.verticalGradient(
                    colors = listOf(
                        color,
                        color.copy(alpha = 0.1f)
                    )
                )
            )
        }
    }
}

/**
 * ⚡ Lightning/Electric Effect
 */
@Composable
fun Modifier.electricEffect(
    isActive: Boolean = true,
    color: Color = Color.Cyan,
    intensity: Float = 1f
): Modifier = composed {
    this.drawBehind {
        if (isActive && Random.nextFloat() < 0.7f) {
            val strokeWidth = 3f * intensity
            val segments = 8
            val path = Path()

            var currentX = 0f
            var currentY = size.height * 0.5f
            path.moveTo(currentX, currentY)

            repeat(segments) {
                val nextX = currentX + size.width / segments
                val nextY = currentY + (Random.nextFloat() - 0.5f) * size.height * 0.3f * intensity

                // Zickzack-Linie für Blitz-Effekt
                val midX = (currentX + nextX) / 2
                val controlY = nextY + (Random.nextFloat() - 0.5f) * size.height * 0.1f

                path.quadraticTo(midX, controlY, nextX, nextY)
                currentX = nextX
                currentY = nextY
            }

            drawPath(
                path = path,
                color = color.copy(alpha = 0.8f),
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
            )

            // Glow-Effekt
            drawPath(
                path = path,
                color = color.copy(alpha = 0.3f),
                style = Stroke(width = strokeWidth * 3, cap = StrokeCap.Round)
            )
        }
    }
}

/**
 * 🌟 Rotating Halo Effect
 */
@Composable
fun Modifier.rotatingHalo(
    color: Color = Color.Yellow,
    rotationDuration: Int = 4000
): Modifier = composed {
    val infiniteTransition = rememberInfiniteTransition(label = "halo")

    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(rotationDuration, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "halo_rotation"
    )

    this.drawBehind {
        rotate(rotation, center) {
            val radius = size.minDimension * 0.6f
            val strokeWidth = 4f

            // Äußerer Ring
            drawCircle(
                color = color.copy(alpha = 0.6f),
                radius = radius,
                style = Stroke(width = strokeWidth),
                center = center
            )

            // Innerer Ring
            drawCircle(
                color = color.copy(alpha = 0.3f),
                radius = radius * 0.8f,
                style = Stroke(width = strokeWidth * 0.5f),
                center = center
            )

            // Leuchtende Punkte
            repeat(8) { i ->
                val angle = (360f / 8f) * i * PI / 180
                val pointX = center.x + cos(angle) * radius
                val pointY = center.y + sin(angle) * radius

                drawCircle(
                    color = color,
                    radius = 6f,
                    center = Offset(pointX.toFloat(), pointY.toFloat())
                )
            }
        }
    }
}

/**
 * 🎨 Rainbow Wave Background
 */
@Composable
fun Modifier.rainbowWave(
    isActive: Boolean = true,
    animationDuration: Int = 5000
): Modifier = composed {
    val infiniteTransition = rememberInfiniteTransition(label = "rainbow")

    val waveProgress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(animationDuration, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "wave_progress"
    )

    this.drawBehind {
        if (isActive) {
            val colors = listOf(
                Color.Red, Color.Magenta, Color.Blue,
                Color.Cyan, Color.Green, Color.Yellow, Color.Red
            )

            val brush = Brush.horizontalGradient(
                colors = colors,
                startX = -size.width + (waveProgress * size.width * 2),
                endX = size.width + (waveProgress * size.width * 2)
            )

            drawRect(brush = brush, alpha = 0.3f)
        }
    }
}

/**
 * 🔥 Fire Effect - Alternative Implementierung ohne Offset-Probleme
 */
@Composable
fun Modifier.fireEffect(
    isActive: Boolean = true,
    intensity: Float = 1f
): Modifier = composed {
    val infiniteTransition = rememberInfiniteTransition(label = "fire")

    val fireProgress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween((1000 / intensity).toInt(), easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "fire_progress"
    )

    this.drawBehind {
        if (isActive) {
            // Alternative Fire-Effekt mit drawCircle anstelle von drawLine
            val flames = 15
            repeat(flames) { i ->
                val x = (size.width / flames) * i + size.width / flames / 2
                val baseHeight = size.height * 0.3f
                val flameHeight = baseHeight + Random.nextFloat() * size.height * 0.5f * intensity
                val flicker = sin(fireProgress * 2 * PI + i) * 5f

                // Erstelle Flammen als Serie von Kreisen
                val segments = 8
                repeat(segments) { segment ->
                    val segmentHeight = flameHeight * (segment + 1) / segments
                    val y = size.height - segmentHeight
                    val flickerX = x + flicker * (1f - segment.toFloat() / segments)
                    val alpha = (1f - segment.toFloat() / segments) * 0.8f
                    val radius = (size.width / flames * 0.4f) * intensity * (1f - segment.toFloat() / segments * 0.5f)

                    // Rote Flamme
                    drawCircle(
                        color = Color.Red.copy(alpha = alpha),
                        radius = radius,
                        center = center.copy(x = flickerX.toFloat(), y = y.toFloat())
                    )

                    // Gelber Kern (nur bei den unteren Segmenten)
                    if (segment < segments / 2) {
                        drawCircle(
                            color = Color.Yellow.copy(alpha = alpha * 0.7f),
                            radius = radius * 0.6f,
                            center = center.copy(x = flickerX.toFloat(), y = y.toFloat())
                        )
                    }
                }
            }
        }
    }
}

/**
 * 🌟 Starburst Animation
 */
@Composable
fun StarburstAnimation(
    isActive: Boolean,
    color: Color = Color.White,
    modifier: Modifier = Modifier
) {
    val animationProgress by animateFloatAsState(
        targetValue = if (isActive) 1f else 0f,
        animationSpec = tween(1000, easing = EaseOutBack),
        label = "starburst"
    )

    Canvas(modifier = modifier.fillMaxSize()) {
        if (animationProgress > 0f) {
            val rayCount = 12
            val maxLength = size.minDimension * 0.4f * animationProgress

            repeat(rayCount) { i ->
                val angle = (360f / rayCount) * i * PI / 180
                val length = maxLength * (0.7f + 0.3f * sin(animationProgress * PI))

                val startX = center.x + cos(angle) * 20f
                val startY = center.y + sin(angle) * 20f
                val endX = center.x + cos(angle) * length
                val endY = center.y + sin(angle) * length

                drawLine(
                    color = color.copy(alpha = 1f - animationProgress * 0.5f),
                    start = Offset(startX.toFloat(), startY.toFloat()),
                    end = Offset(endX.toFloat(), endY.toFloat()),
                    strokeWidth = 8f * (1f - animationProgress * 0.5f),
                    cap = StrokeCap.Round
                )
            }
        }
    }
}

/**
 * 🎪 Entrance Animation für Liste
 */
fun enterAnimation() = slideInVertically(
    initialOffsetY = { it },
    animationSpec = tween(600, easing = EaseOutBack)
) + fadeIn(tween(400))

fun exitAnimation() = slideOutVertically(
    targetOffsetY = { -it },
    animationSpec = tween(400, easing = EaseInBack)
) + fadeOut(tween(300))

/**
 * 🎭 Card Flip Animation
 */
@Composable
fun Modifier.flipCard(
    isFlipped: Boolean,
    animationDuration: Int = 600
): Modifier = composed {
    val rotation by animateFloatAsState(
        targetValue = if (isFlipped) 180f else 0f,
        animationSpec = tween(animationDuration, easing = EaseInOutCubic),
        label = "card_flip"
    )

    this.graphicsLayer {
        rotationY = rotation
        cameraDistance = 12f * density
    }
}
