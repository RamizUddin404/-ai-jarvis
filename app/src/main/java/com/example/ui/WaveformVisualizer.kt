package com.example.ui

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
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
 * Supports multiple visual styles (wave, bar, line, ripple) and customizable color palettes.
 */
@Composable
fun GeminiStreamingAudioWaveform(
    audioRms: Float = 0.5f,
    statusMessage: String = "GEMINI LIVE VOICE STREAM ACTIVE",
    isAiStreaming: Boolean = true,
    waveformStyle: String = "wave", // "wave", "bar", "line", "ripple"
    waveformPalette: String = "cyan", // "cyan", "violet", "emerald", "amber", "monochrome"
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

    // Palette Color Resolution
    val (primaryColor, secondaryColor) = when (waveformPalette) {
        "violet" -> Pair(Color(0xFFA855F7), Color(0xFFEC4899))
        "emerald" -> Pair(Color(0xFF10B981), Color(0xFF06B6D4))
        "amber" -> Pair(Color(0xFFF59E0B), Color(0xFFEF4444))
        "monochrome" -> Pair(Color(0xFFE2E8F0), Color(0xFF94A3B8))
        else -> Pair(if (isAiStreaming) CyanJarvis else Color(0xFF10B981), if (isAiStreaming) TertiaryJarvis else Color(0xFF3B82F6))
    }

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
                text = "${waveformStyle.uppercase()} MODE",
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

                // Background Radial Glow
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            primaryColor.copy(alpha = 0.22f * pulseGlow),
                            Color.Transparent
                        ),
                        center = Offset(w / 2f, midY),
                        radius = w * 0.45f
                    ),
                    radius = w * 0.45f,
                    center = Offset(w / 2f, midY)
                )

                when (waveformStyle) {
                    "bar" -> {
                        // Style: Spectrum Analyzer Bars
                        val barCount = 28
                        val barGap = 4.dp.toPx()
                        val totalWidth = w - 16.dp.toPx()
                        val barWidth = (totalWidth - (barCount - 1) * barGap) / barCount
                        val startX = 8.dp.toPx()

                        for (b in 0 until barCount) {
                            val bx = startX + b * (barWidth + barGap) + barWidth / 2f
                            val normB = b.toFloat() / barCount
                            val bell = sin(normB * Math.PI.toFloat())
                            val bHeight = (6.dp.toPx() + (h * 0.75f * smoothRms * bell * (sin(phase * 2.2f + b * 0.3f) + 1f) / 2f))

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

                    "line" -> {
                        // Style: Oscilloscope Digital Line Grid
                        val gridLines = 4
                        for (g in 1..gridLines) {
                            val gy = (h / (gridLines + 1)) * g
                            drawLine(
                                color = primaryColor.copy(alpha = 0.12f),
                                start = Offset(0f, gy),
                                end = Offset(w, gy),
                                strokeWidth = 1.dp.toPx()
                            )
                        }

                        val steps = 120
                        val dx = w / steps
                        val path = Path()

                        for (i in 0..steps) {
                            val x = i * dx
                            val normX = (i.toFloat() / steps) * (4 * Math.PI.toFloat())
                            val env = sin((i.toFloat() / steps) * Math.PI.toFloat())
                            val y = midY + (sin(normX + phase * 2f) * baseAmp * 1.2f * env) +
                                    (sin(normX * 3.5f - phase) * (baseAmp * 0.4f) * env)

                            if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
                        }

                        drawPath(
                            path = path,
                            brush = Brush.horizontalGradient(
                                colors = listOf(primaryColor.copy(alpha = 0.3f), primaryColor, Color.White, primaryColor, primaryColor.copy(alpha = 0.3f))
                            ),
                            style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
                        )
                    }

                    "ripple" -> {
                        // Style: Concentric Sound Ripples
                        val maxRadius = (w.coerceAtMost(h) / 2f) * 0.95f
                        val rippleCount = 5

                        for (r in 0 until rippleCount) {
                            val progress = (phase / (2 * Math.PI).toFloat() + (r.toFloat() / rippleCount)) % 1f
                            val radius = maxRadius * progress * (0.3f + smoothRms * 0.7f)
                            val alpha = (1f - progress).coerceIn(0f, 1f) * pulseGlow

                            drawCircle(
                                color = if (r % 2 == 0) primaryColor.copy(alpha = alpha * 0.7f) else secondaryColor.copy(alpha = alpha * 0.5f),
                                radius = radius,
                                center = Offset(w / 2f, midY),
                                style = Stroke(width = (2.5f + smoothRms * 3f).dp.toPx())
                            )
                        }
                    }

                    else -> { // Default "wave"
                        // Multi-Harmonic Bezier Sine Waves
                        val steps = 80
                        val dx = w / steps

                        val wavePath1 = Path()
                        val wavePath2 = Path()
                        val wavePath3 = Path()

                        for (i in 0..steps) {
                            val x = i * dx
                            val normX = (i.toFloat() / steps) * (2 * Math.PI.toFloat())
                            val envelope = sin((i.toFloat() / steps) * Math.PI.toFloat())

                            val y1 = midY + sin(normX * 2.5f + phase) * baseAmp * envelope
                            val y2 = midY + sin(normX * 4f - phase * 1.4f) * (baseAmp * 0.65f) * envelope
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

                        drawPath(
                            path = wavePath3,
                            brush = Brush.horizontalGradient(
                                colors = listOf(Color.Transparent, secondaryColor.copy(alpha = 0.6f), Color.Transparent)
                            ),
                            style = Stroke(width = 1.5.dp.toPx(), cap = StrokeCap.Round)
                        )

                        drawPath(
                            path = wavePath2,
                            brush = Brush.horizontalGradient(
                                colors = listOf(Color.Transparent, secondaryColor.copy(alpha = 0.8f), primaryColor.copy(alpha = 0.8f), Color.Transparent)
                            ),
                            style = Stroke(width = 2.5.dp.toPx(), cap = StrokeCap.Round)
                        )

                        drawPath(
                            path = wavePath1,
                            brush = Brush.horizontalGradient(
                                colors = listOf(primaryColor.copy(alpha = 0.2f), primaryColor, Color.White, primaryColor, primaryColor.copy(alpha = 0.2f))
                            ),
                            style = Stroke(width = 4.dp.toPx(), cap = StrokeCap.Round)
                        )

                        // Center Spectrum Analyzer Columns
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
                                brush = Brush.verticalGradient(colors = listOf(primaryColor, secondaryColor)),
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
    }
}

/**
 * Customization section for the Canvas waveform visualizer (Style + Color Palette selector)
 */
@Composable
fun WaveformCustomizationSection(
    currentStyle: String,
    currentPalette: String,
    onStyleSelected: (String) -> Unit,
    onPaletteSelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = "Canvas Waveform Visualizer Style",
            color = MaterialTheme.colorScheme.onBackground,
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold
        )
        Text(
            text = "Select visualization rendering mode and neon color palette",
            color = TextSlate,
            fontSize = 12.sp
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Live Mini Preview Box
        GeminiStreamingAudioWaveform(
            audioRms = 0.7f,
            statusMessage = "PREVIEW MODE",
            isAiStreaming = true,
            waveformStyle = currentStyle,
            waveformPalette = currentPalette,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(14.dp))

        // 1. Style Selection Row
        Text(
            text = "VISUALIZATION STYLE",
            color = TextSlate,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace
        )
        Spacer(modifier = Modifier.height(6.dp))

        val styleOptions = listOf(
            Triple("wave", "Fluid Wave", Icons.Default.GraphicEq),
            Triple("bar", "Spectrum Bars", Icons.Default.BarChart),
            Triple("line", "Oscilloscope", Icons.Default.ShowChart),
            Triple("ripple", "Sound Ripples", Icons.Default.Radar)
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            styleOptions.forEach { (styleKey, label, icon) ->
                val isSelected = currentStyle == styleKey
                Surface(
                    onClick = { onStyleSelected(styleKey) },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(10.dp),
                    color = if (isSelected) CyanJarvis.copy(alpha = 0.18f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                    border = BorderStroke(1.dp, if (isSelected) CyanJarvis else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                ) {
                    Column(
                        modifier = Modifier.padding(vertical = 8.dp, horizontal = 4.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = label,
                            tint = if (isSelected) CyanJarvis else TextSlate,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = label,
                            fontSize = 10.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            color = if (isSelected) CyanJarvis else MaterialTheme.colorScheme.onSurface,
                            maxLines = 1
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // 2. Color Palette Selection Row
        Text(
            text = "COLOR PALETTE",
            color = TextSlate,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace
        )
        Spacer(modifier = Modifier.height(6.dp))

        val paletteOptions = listOf(
            Triple("cyan", "Cyan Jarvis", Pair(CyanJarvis, TertiaryJarvis)),
            Triple("violet", "Neon Violet", Pair(Color(0xFFA855F7), Color(0xFFEC4899))),
            Triple("emerald", "Cyber Emerald", Pair(Color(0xFF10B981), Color(0xFF06B6D4))),
            Triple("amber", "Arc Amber", Pair(Color(0xFFF59E0B), Color(0xFFEF4444))),
            Triple("monochrome", "Matrix Silver", Pair(Color(0xFFE2E8F0), Color(0xFF94A3B8)))
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            paletteOptions.forEach { (paletteKey, label, colors) ->
                val isSelected = currentPalette == paletteKey
                Surface(
                    onClick = { onPaletteSelected(paletteKey) },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(10.dp),
                    color = if (isSelected) colors.first.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                    border = BorderStroke(1.dp, if (isSelected) colors.first else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                ) {
                    Column(
                        modifier = Modifier.padding(vertical = 8.dp, horizontal = 2.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .background(colors.first, CircleShape)
                            )
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .background(colors.second, CircleShape)
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = label,
                            fontSize = 9.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            color = if (isSelected) colors.first else MaterialTheme.colorScheme.onSurface,
                            maxLines = 1
                        )
                    }
                }
            }
        }
    }
}

