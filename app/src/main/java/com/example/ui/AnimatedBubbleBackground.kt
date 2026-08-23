package com.example.ui

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import com.example.ui.theme.JarvisBubbleTheme
import kotlin.math.cos
import kotlin.math.sin

data class BubbleParticle(
    val baseNormalizedX: Float,
    val baseNormalizedY: Float,
    val baseRadius: Float,
    val floatSpeed: Float,
    val pulseSpeed: Float,
    val phaseOffset: Float,
    val alphaMultiplier: Float
)

@Composable
fun AnimatedBubbleBackground(
    theme: JarvisBubbleTheme = JarvisBubbleTheme.ARC_REACTOR,
    isListening: Boolean = false,
    audioRms: Float = 0f,
    modifier: Modifier = Modifier
) {
    // Generate stable particles
    val particles = remember {
        List(28) { index ->
            val random = kotlin.random.Random(index * 37 + 101)
            BubbleParticle(
                baseNormalizedX = random.nextFloat(),
                baseNormalizedY = random.nextFloat(),
                baseRadius = random.nextFloat() * 45f + 12f,
                floatSpeed = random.nextFloat() * 0.8f + 0.3f,
                pulseSpeed = random.nextFloat() * 1.5f + 0.8f,
                phaseOffset = random.nextFloat() * 6.28f,
                alphaMultiplier = random.nextFloat() * 0.5f + 0.5f
            )
        }
    }

    val infiniteTransition = rememberInfiniteTransition(label = "bubble_pulse_transition")
    
    // Global continuous time animation for smooth pulsing
    val animationTime by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 6.2831853f * 10f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 30000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "time_float"
    )

    // Hologram rotation
    val hologramRotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 40000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "hologram_rot"
    )

    // Audio boost when speaking/listening
    val rmsNormalized = (audioRms / 10f).coerceIn(0f, 1f)

    Canvas(modifier = modifier.fillMaxSize()) {
        val width = size.width
        val height = size.height

        // 1. Draw Theme Background Gradient
        drawRect(
            brush = Brush.radialGradient(
                colors = listOf(
                    theme.bgStartColor,
                    theme.bgEndColor
                ),
                center = Offset(width * 0.5f, height * 0.35f),
                radius = width * 1.2f
            )
        )

        // 2. Draw Subtle AR Holographic Scan Grid / Targeting Rings
        drawHolographicRings(
            theme = theme,
            rotationDeg = hologramRotation,
            isListening = isListening,
            rms = rmsNormalized
        )

        // 3. Draw Animated Pulsing Floating Bubbles
        particles.forEachIndexed { i, p ->
            val time = animationTime * p.floatSpeed + p.phaseOffset
            val xOffset = sin(time * 0.7f) * 30f
            val yOffset = cos(time * 0.5f) * 45f

            val px = (p.baseNormalizedX * width + xOffset).mod(width)
            val py = (p.baseNormalizedY * height + yOffset).mod(height)

            // Dynamic pulsing radius
            val pulseFactor = 1f + 0.25f * sin(animationTime * p.pulseSpeed + p.phaseOffset) + (rmsNormalized * 0.35f)
            val currentRadius = p.baseRadius * pulseFactor

            val bubbleAlpha = (0.07f + 0.05f * sin(time * 1.2f)) * p.alphaMultiplier * (if (isListening) 1.4f else 1.0f)
            val strokeAlpha = (bubbleAlpha * 2.2f).coerceIn(0.05f, 0.45f)

            // Inner soft glow gradient
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        theme.glowColor.copy(alpha = bubbleAlpha.coerceIn(0f, 1f)),
                        theme.primaryColor.copy(alpha = (bubbleAlpha * 0.4f).coerceIn(0f, 1f)),
                        Color.Transparent
                    ),
                    center = Offset(px, py),
                    radius = currentRadius * 1.3f
                ),
                radius = currentRadius * 1.3f,
                center = Offset(px, py)
            )

            // Outer bubble rim stroke
            drawCircle(
                color = theme.primaryColor.copy(alpha = strokeAlpha),
                radius = currentRadius,
                center = Offset(px, py),
                style = Stroke(width = if (i % 3 == 0) 1.5f else 1f)
            )

            // Specular reflection highlight on bubble top-left
            val highlightRadius = currentRadius * 0.25f
            val highlightOffset = Offset(px - currentRadius * 0.35f, py - currentRadius * 0.35f)
            drawCircle(
                color = Color.White.copy(alpha = (strokeAlpha * 0.8f).coerceIn(0f, 0.35f)),
                radius = highlightRadius,
                center = highlightOffset
            )
        }
    }
}

private fun DrawScope.drawHolographicRings(
    theme: JarvisBubbleTheme,
    rotationDeg: Float,
    isListening: Boolean,
    rms: Float
) {
    val centerX = size.width * 0.5f
    val centerY = size.height * 0.45f
    val baseRadius = size.width * 0.42f
    val pulseExpand = if (isListening) (rms * 20f) else 0f

    // Concentric Dashed AR Rings
    drawCircle(
        color = theme.primaryColor.copy(alpha = 0.06f + (rms * 0.08f)),
        radius = (baseRadius * 0.85f) + pulseExpand,
        center = Offset(centerX, centerY),
        style = Stroke(
            width = 1f,
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 16f), rotationDeg)
        )
    )

    drawCircle(
        color = theme.secondaryColor.copy(alpha = 0.04f),
        radius = (baseRadius * 1.15f) - pulseExpand,
        center = Offset(centerX, centerY),
        style = Stroke(
            width = 1f,
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(20f, 24f), -rotationDeg * 0.7f)
        )
    )

    // Subtle center glow beacon
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(
                theme.primaryColor.copy(alpha = if (isListening) 0.12f + (rms * 0.1f) else 0.05f),
                Color.Transparent
            ),
            center = Offset(centerX, centerY),
            radius = baseRadius * 0.9f
        ),
        radius = baseRadius * 0.9f,
        center = Offset(centerX, centerY)
    )
}
