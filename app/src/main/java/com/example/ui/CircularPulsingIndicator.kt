package com.example.ui

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.CyanJarvis
import com.example.ui.theme.GreenSecure
import com.example.ui.theme.TextSlate
import kotlin.math.cos
import kotlin.math.sin

/**
 * Custom circular pulsing animation component providing visual feedback
 * when Jarvis is actively listening, processing, speaking, or standby.
 */
@Composable
fun CircularPulsingIndicator(
    isListening: Boolean,
    isProcessing: Boolean,
    isSpeaking: Boolean = false,
    audioRms: Float = 0f,
    modifier: Modifier = Modifier,
    indicatorSize: Dp = 160.dp,
    showStatusText: Boolean = true,
    onClick: (() -> Unit)? = null
) {
    val infiniteTransition = rememberInfiniteTransition(label = "circular_pulse_transition")

    // Continuous smooth rotation for outer tech ring
    val rotationAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = if (isProcessing) 2500 else if (isListening) 4000 else 8000,
                easing = LinearEasing
            ),
            repeatMode = RepeatMode.Restart
        ),
        label = "tech_ring_rotation"
    )

    // Pulsing expand animation (0f to 1f)
    val pulseProgress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = if (isListening) 1200 else if (isProcessing) 800 else 2000,
                easing = FastOutSlowInEasing
            ),
            repeatMode = RepeatMode.Restart
        ),
        label = "pulse_progress"
    )

    // Core breathing scale
    val coreBreathingScale by infiniteTransition.animateFloat(
        initialValue = 0.92f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = if (isListening) 600 else 1400,
                easing = FastOutSlowInEasing
            ),
            repeatMode = RepeatMode.Reverse
        ),
        label = "core_scale"
    )

    // Audio reactivity mapping
    val smoothRms by animateFloatAsState(
        targetValue = audioRms.coerceIn(0f, 1f),
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "smooth_rms_reaction"
    )

    // Dynamic colors based on active state
    val primaryColor = when {
        isListening -> Color(0xFF00F0FF) // Vibrant Neon Cyan
        isProcessing -> Color(0xFFA855F7) // Holographic Violet
        isSpeaking -> GreenSecure // Bio Green
        else -> CyanJarvis.copy(alpha = 0.6f)
    }

    val secondaryColor = when {
        isListening -> GreenSecure
        isProcessing -> Color(0xFFEC4899) // Pink Glow
        isSpeaking -> Color(0xFF00F0FF)
        else -> Color(0xFF3B82F6) // Soft Blue
    }

    val statusLabel = when {
        isListening -> "LISTENING..."
        isProcessing -> "PROCESSING..."
        isSpeaking -> "SPEAKING..."
        else -> "STANDBY"
    }

    val contentScale = if (isListening) (coreBreathingScale + smoothRms * 0.25f) else coreBreathingScale

    Surface(
        onClick = { onClick?.invoke() },
        enabled = onClick != null,
        shape = CircleShape,
        color = Color.Transparent,
        modifier = modifier
            .size(indicatorSize)
            .testTag("circular_pulsing_indicator")
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            // Canvas for rings, pulsing shockwaves, and rotating arc segments
            Canvas(modifier = Modifier.fillMaxSize()) {
                val canvasWidth = this.size.width
                val canvasHeight = this.size.height
                val centerOffset = Offset(canvasWidth / 2f, canvasHeight / 2f)
                val baseRadius = (minOf(canvasWidth, canvasHeight) / 2f) * 0.55f

                // 1. Multiple Pulsing Shockwave Rings (Expanding & Fading out)
                val ringCount = 3
                for (i in 0 until ringCount) {
                    val ringOffset = (pulseProgress + i.toFloat() / ringCount) % 1f
                    val ringRadius = baseRadius + ringOffset * (baseRadius * 0.85f) + (smoothRms * 20.dp.toPx())
                    val ringAlpha = (1f - ringOffset).coerceIn(0f, 0.8f)

                    drawCircle(
                        color = primaryColor.copy(alpha = ringAlpha * 0.45f),
                        radius = ringRadius,
                        center = centerOffset,
                        style = Stroke(
                            width = (3.dp.toPx() * (1f - ringOffset)).coerceAtLeast(1f),
                            pathEffect = PathEffect.dashPathEffect(
                                floatArrayOf(15.dp.toPx(), 8.dp.toPx())
                            )
                        )
                    )
                }

                // 2. Audio Reactive Radial Glow Disk
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            primaryColor.copy(alpha = if (isListening || isProcessing) 0.35f + smoothRms * 0.3f else 0.15f),
                            secondaryColor.copy(alpha = 0.1f),
                            Color.Transparent
                        ),
                        center = centerOffset,
                        radius = baseRadius * 1.3f + (smoothRms * 15.dp.toPx())
                    ),
                    radius = baseRadius * 1.3f,
                    center = centerOffset
                )

                // 3. Rotating Tech HUD Arcs
                rotate(degrees = rotationAngle, pivot = centerOffset) {
                    // Outer dotted ring
                    drawCircle(
                        color = primaryColor.copy(alpha = 0.5f),
                        radius = baseRadius * 1.15f,
                        center = centerOffset,
                        style = Stroke(
                            width = 1.5.dp.toPx(),
                            pathEffect = PathEffect.dashPathEffect(
                                floatArrayOf(8.dp.toPx(), 12.dp.toPx())
                            )
                        )
                    )

                    // Accent arc 1
                    drawArc(
                        color = primaryColor,
                        startAngle = 0f,
                        sweepAngle = 70f,
                        useCenter = false,
                        topLeft = Offset(centerOffset.x - baseRadius * 1.25f, centerOffset.y - baseRadius * 1.25f),
                        size = androidx.compose.ui.geometry.Size(baseRadius * 2.5f, baseRadius * 2.5f),
                        style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
                    )

                    // Accent arc 2 (opposite side)
                    drawArc(
                        color = secondaryColor,
                        startAngle = 180f,
                        sweepAngle = 90f,
                        useCenter = false,
                        topLeft = Offset(centerOffset.x - baseRadius * 1.25f, centerOffset.y - baseRadius * 1.25f),
                        size = androidx.compose.ui.geometry.Size(baseRadius * 2.5f, baseRadius * 2.5f),
                        style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round)
                    )
                }

                // 4. Counter-rotating inner arc if processing
                if (isProcessing) {
                    rotate(degrees = -rotationAngle * 1.5f, pivot = centerOffset) {
                        drawArc(
                            color = secondaryColor,
                            startAngle = 45f,
                            sweepAngle = 120f,
                            useCenter = false,
                            topLeft = Offset(centerOffset.x - baseRadius * 0.95f, centerOffset.y - baseRadius * 0.95f),
                            size = androidx.compose.ui.geometry.Size(baseRadius * 1.9f, baseRadius * 1.9f),
                            style = Stroke(width = 2.5.dp.toPx(), cap = StrokeCap.Round)
                        )
                    }
                }
            }

            // 5. Center Core Orb with Icon & State Label
            Box(
                modifier = Modifier
                    .size(indicatorSize * 0.52f)
                    .scale(contentScale)
                    .background(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                primaryColor.copy(alpha = 0.85f),
                                primaryColor.copy(alpha = 0.4f),
                                Color(0xFF070F1E)
                            )
                        ),
                        shape = CircleShape
                    )
                    .border(
                        width = 2.dp,
                        brush = Brush.sweepGradient(
                            listOf(primaryColor, secondaryColor, primaryColor)
                        ),
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    val icon = when {
                        isListening -> Icons.Default.Mic
                        isProcessing -> Icons.Default.Psychology
                        isSpeaking -> Icons.Default.GraphicEq
                        else -> Icons.Default.AutoAwesome
                    }

                    Icon(
                        imageVector = icon,
                        contentDescription = statusLabel,
                        tint = Color.White,
                        modifier = Modifier.size(indicatorSize * 0.18f)
                    )

                    if (showStatusText) {
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = statusLabel,
                            fontSize = (indicatorSize.value * 0.055f).sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            color = Color.White,
                            letterSpacing = 0.5.sp
                        )
                    }
                }
            }
        }
    }
}
