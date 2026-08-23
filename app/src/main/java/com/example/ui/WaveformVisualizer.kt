package com.example.ui

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*
import kotlin.math.*

@Composable
fun RealtimeListeningWaveform(
    audioRms: Float,
    partialText: String = "",
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "waveform_anim")
    
    // Continuous flowing phase
    val phase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = (2 * Math.PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1800, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "phase_anim"
    )

    // Pulsing aura animation
    val pulseGlow by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 0.95f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 900, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_glow"
    )

    // Smooth animated RMS response
    val animatedRms by animateFloatAsState(
        targetValue = audioRms.coerceIn(0f, 1f),
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioLowBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "smooth_rms"
    )

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(MaterialTheme.colorScheme.surface)
            .border(1.dp, CyanJarvis.copy(alpha = 0.35f), RoundedCornerShape(20.dp))
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Status header with live recording indicator
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Pulsing red-neon recording dot
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .background(
                            Color(0xFFFF3366).copy(alpha = pulseGlow),
                            CircleShape
                        )
                )
                Text(
                    text = "SPEECH RECOGNITION ACTIVE",
                    color = CyanJarvis,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    letterSpacing = 1.5.sp
                )
            }

            // Energy meter indicator
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                repeat(4) { idx ->
                    val active = animatedRms > (idx * 0.22f)
                    Box(
                        modifier = Modifier
                            .width(4.dp)
                            .height((8 + idx * 3).dp)
                            .background(
                                color = if (active) GreenSecure else MaterialTheme.colorScheme.outline.copy(alpha = 0.4f),
                                shape = RoundedCornerShape(2.dp)
                            )
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Dynamic multi-layered waveform canvas (Layered sine waves + Frequency Equalizer Bars)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp),
            contentAlignment = Alignment.Center
        ) {
            // Layer 1: Fluid Sine Wave Lines on Canvas
            Canvas(modifier = Modifier.fillMaxSize()) {
                val width = size.width
                val height = size.height
                val midY = height / 2f
                val effectiveAmp = (12f + animatedRms * 32f).dp.toPx()

                val path1 = Path()
                val path2 = Path()

                val steps = 60
                val dx = width / steps

                for (i in 0..steps) {
                    val x = i * dx
                    val normalizedX = (i.toFloat() / steps) * (2 * Math.PI.toFloat())
                    
                    val y1 = midY + sin(normalizedX * 2f + phase) * effectiveAmp * sin((i.toFloat() / steps) * Math.PI.toFloat())
                    val y2 = midY + sin(normalizedX * 3f - phase * 1.3f) * (effectiveAmp * 0.65f) * sin((i.toFloat() / steps) * Math.PI.toFloat())

                    if (i == 0) {
                        path1.moveTo(x, y1)
                        path2.moveTo(x, y2)
                    } else {
                        path1.lineTo(x, y1)
                        path2.lineTo(x, y2)
                    }
                }

                // Draw secondary softer glow wave
                drawPath(
                    path = path2,
                    brush = Brush.horizontalGradient(
                        colors = listOf(
                            TertiaryJarvis.copy(alpha = 0.1f),
                            TertiaryJarvis.copy(alpha = 0.7f),
                            CyanJarvis.copy(alpha = 0.7f),
                            TertiaryJarvis.copy(alpha = 0.1f)
                        )
                    ),
                    style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round)
                )

                // Draw primary vibrant neon cyan wave
                drawPath(
                    path = path1,
                    brush = Brush.horizontalGradient(
                        colors = listOf(
                            CyanJarvis.copy(alpha = 0.1f),
                            CyanJarvis,
                            GreenSecure,
                            CyanJarvis.copy(alpha = 0.1f)
                        )
                    ),
                    style = Stroke(width = 3.5.dp.toPx(), cap = StrokeCap.Round)
                )
            }

            // Layer 2: Equalizer Spectrum Bars in center
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                val totalBars = 21
                for (i in 0 until totalBars) {
                    val distFromCenter = kotlin.math.abs(i - totalBars / 2).toFloat() / (totalBars / 2)
                    val bellCurve = (1f - distFromCenter * 0.65f).coerceIn(0.2f, 1f)
                    val barPhaseOffset = i * 0.35f
                    val waveSine = (sin(phase * 2f + barPhaseOffset) + 1f) / 2f
                    
                    val minHeight = 6f
                    val maxHeight = 46f
                    val dynamicHeight = minHeight + (maxHeight - minHeight) * (animatedRms * 0.7f + waveSine * 0.3f) * bellCurve

                    Box(
                        modifier = Modifier
                            .width(3.5.dp)
                            .height(dynamicHeight.dp)
                            .background(
                                brush = Brush.verticalGradient(
                                    colors = listOf(
                                        CyanJarvis,
                                        if (animatedRms > 0.4f) GreenSecure else TertiaryJarvis
                                    )
                                ),
                                shape = RoundedCornerShape(2.dp)
                            )
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Live transcription feedback or hint
        if (partialText.isNotBlank()) {
            Text(
                text = "\"$partialText\"",
                color = MaterialTheme.colorScheme.onBackground,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                fontStyle = FontStyle.Italic
            )
        } else {
            Text(
                text = "Speak now — Android SpeechRecognizer listening...",
                color = TextSlate,
                fontSize = 12.sp,
                fontWeight = FontWeight.Normal
            )
        }
    }
}

@Composable
fun ResponsiveMicWaveIndicator(
    audioRms: Float,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "fab_wave")
    val phase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 6.28f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "fab_phase"
    )

    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(3.dp)
    ) {
        val barCount = 4
        for (i in 0 until barCount) {
            val wave = (sin(phase + i * 0.8f) + 1f) / 2f
            val h = 6f + (18f * (audioRms.coerceIn(0f, 1f) * 0.7f + wave * 0.3f))
            Box(
                modifier = Modifier
                    .width(3.dp)
                    .height(h.dp)
                    .background(Color.White, RoundedCornerShape(1.5.dp))
            )
        }
    }
}

/**
 * Custom Canvas-based visualization for active Gemini voice streaming sessions.
 * Displays multi-harmonic fluid sine waves, frequency spectrum bars, and orbiting energy particles.
 */
@Composable
fun GeminiStreamingAudioWaveform(
    audioRms: Float = 0.5f,
    statusMessage: String = "GEMINI LIVE VOICE STREAM ACTIVE",
    isAiStreaming: Boolean = true,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "gemini_wave_anim")

    // Continuous wave phase shift
    val phase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = (2 * Math.PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1500, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "gemini_phase"
    )

    // Pulsing aura glow
    val pulseGlow by infiniteTransition.animateFloat(
        initialValue = 0.35f,
        targetValue = 0.9f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "gemini_pulse"
    )

    // Animated particle orbit angle
    val orbitAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = (2 * Math.PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 4000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "gemini_orbit"
    )

    // Smooth responsive amplitude
    val smoothRms by animateFloatAsState(
        targetValue = audioRms.coerceIn(0.15f, 1f),
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioLowBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "gemini_rms"
    )

    val primaryColor = if (isAiStreaming) CyanJarvis else Color(0xFF10B981)
    val secondaryColor = if (isAiStreaming) TertiaryJarvis else Color(0xFF3B82F6)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 6.dp)
            .clip(RoundedCornerShape(22.dp))
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        primaryColor.copy(alpha = 0.12f),
                        MaterialTheme.colorScheme.surface.copy(alpha = 0.95f)
                    )
                )
            )
            .border(1.dp, primaryColor.copy(alpha = 0.4f * pulseGlow), RoundedCornerShape(22.dp))
            .padding(14.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Status header with high-tech badge
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .background(
                            color = primaryColor.copy(alpha = pulseGlow),
                            shape = CircleShape
                        )
                )
                Text(
                    text = statusMessage.uppercase(),
                    color = primaryColor,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    letterSpacing = 1.2.sp
                )
            }

            Text(
                text = "16kHz STREAM",
                color = TextSlate,
                fontSize = 10.sp,
                fontFamily = FontFamily.Monospace
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Custom Canvas Visualization Area
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(80.dp),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val w = size.width
                val h = size.height
                val midY = h / 2f
                val baseAmp = (14f + smoothRms * 36f).dp.toPx()

                // 1. Draw central glowing aura background
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            primaryColor.copy(alpha = 0.25f * pulseGlow),
                            Color.Transparent
                        ),
                        center = Offset(w / 2f, midY),
                        radius = w * 0.45f
                    ),
                    radius = w * 0.45f,
                    center = Offset(w / 2f, midY)
                )

                // 2. Draw Multi-Harmonic Bezier Sine Waves
                val steps = 80
                val dx = w / steps

                val wavePath1 = Path()
                val wavePath2 = Path()
                val wavePath3 = Path()

                for (i in 0..steps) {
                    val x = i * dx
                    val normX = (i.toFloat() / steps) * (2 * Math.PI.toFloat())
                    val envelope = sin((i.toFloat() / steps) * Math.PI.toFloat())

                    // Primary Fundamental Wave
                    val y1 = midY + sin(normX * 2.5f + phase) * baseAmp * envelope
                    // Secondary Formant Harmonic
                    val y2 = midY + sin(normX * 4f - phase * 1.4f) * (baseAmp * 0.65f) * envelope
                    // High-frequency Shimmer
                    val y3 = midY + sin(normX * 7f + phase * 2.2f) * (baseAmp * 0.35f) * envelope

                    if (i == 0) {
                        wavePath1.moveTo(x, y1)
                        wavePath2.moveTo(x, y2)
                        wavePath3.moveTo(x, y3)
                    } else {
                        wavePath1.lineTo(x, y1)
                        wavePath2.lineTo(x, y2)
                        wavePath3.lineTo(x, y3)
                    }
                }

                // Draw Wave 3 (Shimmer Accent)
                drawPath(
                    path = wavePath3,
                    brush = Brush.horizontalGradient(
                        colors = listOf(
                            Color.Transparent,
                            GreenSecure.copy(alpha = 0.6f),
                            Color.Transparent
                        )
                    ),
                    style = Stroke(width = 1.5.dp.toPx(), cap = StrokeCap.Round)
                )

                // Draw Wave 2 (Secondary Violet Gradient)
                drawPath(
                    path = wavePath2,
                    brush = Brush.horizontalGradient(
                        colors = listOf(
                            Color.Transparent,
                            secondaryColor.copy(alpha = 0.8f),
                            primaryColor.copy(alpha = 0.8f),
                            Color.Transparent
                        )
                    ),
                    style = Stroke(width = 2.5.dp.toPx(), cap = StrokeCap.Round)
                )

                // Draw Wave 1 (Primary Neon Cyan Wave)
                drawPath(
                    path = wavePath1,
                    brush = Brush.horizontalGradient(
                        colors = listOf(
                            primaryColor.copy(alpha = 0.2f),
                            primaryColor,
                            Color.White,
                            primaryColor,
                            primaryColor.copy(alpha = 0.2f)
                        )
                    ),
                    style = Stroke(width = 4.dp.toPx(), cap = StrokeCap.Round)
                )

                // 3. Draw Orbiting Audio Energy Particle Sparks
                val particles = 6
                for (p in 0 until particles) {
                    val pAngle = orbitAngle + (p * Math.PI.toFloat() * 2f / particles)
                    val rx = (w / 2f) + (w * 0.38f) * cos(pAngle)
                    val ry = midY + (baseAmp * 0.6f) * sin(pAngle * 2f)
                    val pRadius = (3.5f + 2f * sin(pAngle * 3f)).dp.toPx()

                    drawCircle(
                        color = Color.White.copy(alpha = 0.85f),
                        radius = pRadius,
                        center = Offset(rx, ry)
                    )
                    drawCircle(
                        color = primaryColor.copy(alpha = 0.5f),
                        radius = pRadius * 2f,
                        center = Offset(rx, ry)
                    )
                }

                // 4. Draw Center Spectrum Analyzer Columns
                val barCount = 18
                val barWidth = 3.5.dp.toPx()
                val totalBarsWidth = barCount * barWidth * 2.2f
                val startX = (w - totalBarsWidth) / 2f

                for (b in 0 until barCount) {
                    val bx = startX + b * barWidth * 2.2f
                    val distFromCenter = abs(b.toFloat() - barCount / 2f) / (barCount / 2f)
                    val bellFactor = (1f - distFromCenter * 0.6f).coerceIn(0.2f, 1f)
                    val bHeight = (8.dp.toPx() + (38.dp.toPx() * smoothRms * bellFactor * (sin(phase * 2.5f + b * 0.4f) + 1f) / 2f))

                    drawLine(
                        brush = Brush.verticalGradient(
                            colors = listOf(primaryColor, secondaryColor)
                        ),
                        start = Offset(bx, midY - bHeight / 2f),
                        end = Offset(bx, midY + bHeight / 2f),
                        strokeWidth = barWidth,
                        cap = StrokeCap.Round
                    )
                }
            }
        }
    }
}
