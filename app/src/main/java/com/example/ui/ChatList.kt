package com.example.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.ChatEntity
import com.example.ui.theme.GreenSecure
import com.example.ui.theme.JarvisBubbleTheme
import com.example.ui.theme.TextSlate
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun JarvisChatList(
    chatHistory: List<ChatEntity>,
    isThinking: Boolean = false,
    theme: JarvisBubbleTheme = JarvisBubbleTheme.ELEGANT_DARK,
    modifier: Modifier = Modifier
) {
    val listState = rememberLazyListState()

    LaunchedEffect(chatHistory.size, isThinking) {
        val totalCount = chatHistory.size + (if (isThinking) 1 else 0)
        if (totalCount > 0) {
            listState.animateScrollToItem(totalCount - 1)
        }
    }

    LazyColumn(
        state = listState,
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        reverseLayout = false
    ) {
        // Bolt performance optimization: Provide stable primary key (chat.id) to LazyColumn
        // items. Without explicit keys, Jetpack Compose defaults to positional index keys,
        // causing unnecessary recomposition of existing items when new messages arrive.
        items(chatHistory, key = { it.id }) { chat ->
            val isUser = chat.role == "user"
            if (isUser) {
                UserMessageCard(chat = chat, theme = theme)
            } else {
                AiMessageCard(chat = chat, theme = theme)
            }
        }

        if (isThinking) {
            item {
                JarvisTypingIndicatorBubble(theme = theme)
            }
        }
    }
}

/**
 * Distinct User Message Bubble with dynamic, animated backgrounds responding to user's selected theme.
 * Smooth Compose color transition animations & continuous cosmic nebula shimmer effects.
 */
@Composable
fun UserMessageCard(
    chat: ChatEntity,
    theme: JarvisBubbleTheme = JarvisBubbleTheme.ELEGANT_DARK,
    modifier: Modifier = Modifier
) {
    val timeString = remember(chat.timestamp) {
        try {
            val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())
            sdf.format(Date(chat.timestamp))
        } catch (e: Exception) {
            ""
        }
    }

    // Compose transition animations for smooth theme color switching
    val animatedPrimary by animateColorAsState(targetValue = theme.primaryColor, animationSpec = tween(600), label = "user_primary")
    val animatedSecondary by animateColorAsState(targetValue = theme.secondaryColor, animationSpec = tween(600), label = "user_secondary")
    val animatedGlow by animateColorAsState(targetValue = theme.glowColor, animationSpec = tween(600), label = "user_glow")
    val animatedBgStart by animateColorAsState(targetValue = theme.bgStartColor, animationSpec = tween(600), label = "user_bg_start")

    // Continuous ambient animation transition inside the bubble
    val infiniteTransition = rememberInfiniteTransition(label = "user_bubble_anim")
    
    val phaseShift by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 6.2831853f,
        animationSpec = infiniteRepeatable(
            animation = tween(12000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "phase_shift"
    )

    val pulseGlow by infiniteTransition.animateFloat(
        initialValue = 0.45f,
        targetValue = 0.85f,
        animationSpec = infiniteRepeatable(
            animation = tween(2200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_glow"
    )

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 5.dp),
        horizontalArrangement = Arrangement.End,
        verticalAlignment = Alignment.Bottom
    ) {
        Card(
            shape = RoundedCornerShape(
                topStart = 18.dp,
                topEnd = 18.dp,
                bottomStart = 18.dp,
                bottomEnd = 4.dp
            ),
            colors = CardDefaults.cardColors(
                containerColor = Color.Transparent,
                contentColor = MaterialTheme.colorScheme.onBackground
            ),
            elevation = CardDefaults.cardElevation(
                defaultElevation = 2.dp,
                pressedElevation = 4.dp
            ),
            border = BorderStroke(
                width = 1.dp,
                color = animatedPrimary.copy(alpha = 0.45f * pulseGlow)
            ),
            modifier = Modifier
                .widthIn(max = 290.dp)
                .drawBehind {
                    val w = size.width
                    val h = size.height

                    // 1. Dynamic Animated Theme Nebula Gradient Canvas Background
                    drawRect(
                        brush = Brush.linearGradient(
                            colors = listOf(
                                animatedBgStart.copy(alpha = 0.88f),
                                animatedPrimary.copy(alpha = 0.22f),
                                animatedSecondary.copy(alpha = 0.16f),
                                animatedGlow.copy(alpha = 0.10f)
                            ),
                            start = Offset(sin(phaseShift) * w * 0.3f, 0f),
                            end = Offset(w + cos(phaseShift) * w * 0.3f, h)
                        )
                    )

                    // 2. Animated Cosmic Nebula / Particle Dust Specks on Message Background
                    val particles = 4
                    for (i in 0 until particles) {
                        val angle = phaseShift + (i * 1.57f)
                        val px = (0.2f + 0.25f * i + 0.15f * sin(angle)) * w
                        val py = (0.3f + 0.2f * i + 0.12f * cos(angle)) * h
                        val radius = (4f + 3f * sin(angle * 2f)) * density

                        drawCircle(
                            brush = Brush.radialGradient(
                                colors = listOf(
                                    animatedGlow.copy(alpha = 0.35f * pulseGlow),
                                    Color.Transparent
                                ),
                                center = Offset(px, py),
                                radius = radius * 2.5f
                            ),
                            radius = radius * 2.5f,
                            center = Offset(px, py)
                        )
                    }
                }
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)
            ) {
                // Header tag
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "YOU",
                        color = animatedPrimary,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        letterSpacing = 1.sp
                    )

                    if (timeString.isNotBlank()) {
                        Text(
                            text = timeString,
                            color = TextSlate.copy(alpha = 0.7f),
                            fontSize = 9.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                // Message Text
                Text(
                    text = chat.content,
                    color = MaterialTheme.colorScheme.onBackground,
                    fontSize = 14.sp,
                    lineHeight = 20.sp,
                    fontWeight = FontWeight.Normal
                )
            }
        }

        Spacer(modifier = Modifier.width(6.dp))

        // User Avatar Badge with Theme Accent
        Box(
            modifier = Modifier
                .padding(bottom = 2.dp)
                .size(26.dp)
                .background(animatedPrimary.copy(alpha = 0.2f), CircleShape)
                .border(1.dp, animatedPrimary.copy(alpha = 0.6f * pulseGlow), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Person,
                contentDescription = "User",
                tint = animatedPrimary,
                modifier = Modifier.size(14.dp)
            )
        }
    }
}

/**
 * Distinct AI Message Bubble with dynamic, animated backgrounds responding to user's selected theme.
 * Left-aligned, futuristic neural core styling, header status indicator, and dynamic theme glow.
 */
@Composable
fun AiMessageCard(
    chat: ChatEntity,
    theme: JarvisBubbleTheme = JarvisBubbleTheme.ELEGANT_DARK,
    modifier: Modifier = Modifier
) {
    val timeString = remember(chat.timestamp) {
        try {
            val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())
            sdf.format(Date(chat.timestamp))
        } catch (e: Exception) {
            ""
        }
    }

    // Smooth Compose color transitions for active theme
    val animatedPrimary by animateColorAsState(targetValue = theme.primaryColor, animationSpec = tween(600), label = "ai_primary")
    val animatedSecondary by animateColorAsState(targetValue = theme.secondaryColor, animationSpec = tween(600), label = "ai_secondary")
    val animatedGlow by animateColorAsState(targetValue = theme.glowColor, animationSpec = tween(600), label = "ai_glow")
    val animatedBgStart by animateColorAsState(targetValue = theme.bgStartColor, animationSpec = tween(600), label = "ai_bg_start")

    // Infinite animation transition for continuous background aura movement
    val infiniteTransition = rememberInfiniteTransition(label = "ai_bubble_anim")

    val phaseShift by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 6.2831853f,
        animationSpec = infiniteRepeatable(
            animation = tween(15000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "ai_phase_shift"
    )

    val auraPulse by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.75f,
        animationSpec = infiniteRepeatable(
            animation = tween(2800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "ai_aura_pulse"
    )

    val surfaceColor = MaterialTheme.colorScheme.surface

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 5.dp),
        horizontalArrangement = Arrangement.Start,
        verticalAlignment = Alignment.Bottom
    ) {
        // AI Glowing Avatar responding to selected theme
        Box(
            modifier = Modifier
                .padding(bottom = 2.dp)
                .size(26.dp)
                .background(
                    Brush.radialGradient(
                        colors = listOf(animatedPrimary.copy(alpha = 0.35f * auraPulse), Color.Transparent)
                    ),
                    CircleShape
                )
                .border(1.dp, animatedPrimary.copy(alpha = 0.7f * auraPulse), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.AutoAwesome,
                contentDescription = "J.A.R.V.I.S. Core",
                tint = animatedPrimary,
                modifier = Modifier.size(14.dp)
            )
        }

        Spacer(modifier = Modifier.width(6.dp))

        Card(
            shape = RoundedCornerShape(
                topStart = 18.dp,
                topEnd = 18.dp,
                bottomStart = 4.dp,
                bottomEnd = 18.dp
            ),
            colors = CardDefaults.cardColors(
                containerColor = Color.Transparent,
                contentColor = MaterialTheme.colorScheme.onSurface
            ),
            elevation = CardDefaults.cardElevation(
                defaultElevation = 2.dp,
                pressedElevation = 4.dp
            ),
            border = BorderStroke(
                width = 1.dp,
                color = animatedPrimary.copy(alpha = 0.35f * auraPulse)
            ),
            modifier = Modifier
                .widthIn(max = 295.dp)
                .drawBehind {
                    val w = size.width
                    val h = size.height

                    // Dynamic Animated AI Theme Background Canvas Gradient
                    drawRect(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                animatedGlow.copy(alpha = 0.15f * auraPulse),
                                animatedBgStart.copy(alpha = 0.85f),
                                surfaceColor.copy(alpha = 0.95f)
                            ),
                            center = Offset(w * 0.15f, h * 0.2f),
                            radius = w * 1.1f
                        )
                    )

                    // Subtle Theme Halo Shimmer Arc along top border
                    val arcX = (0.2f + 0.6f * sin(phaseShift)) * w
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                animatedPrimary.copy(alpha = 0.25f * auraPulse),
                                Color.Transparent
                            ),
                            center = Offset(arcX, 0f),
                            radius = w * 0.4f
                        ),
                        radius = w * 0.4f,
                        center = Offset(arcX, 0f)
                    )
                }
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)
            ) {
                // Header Node metadata
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .background(GreenSecure, CircleShape)
                        )
                        Text(
                            text = "J.A.R.V.I.S.",
                            color = animatedPrimary,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            letterSpacing = 1.2.sp
                        )
                    }

                    if (timeString.isNotBlank()) {
                        Text(
                            text = timeString,
                            color = TextSlate.copy(alpha = 0.7f),
                            fontSize = 9.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                // AI Response Body
                Text(
                    text = chat.content,
                    color = MaterialTheme.colorScheme.onBackground,
                    fontSize = 14.sp,
                    lineHeight = 20.sp,
                    fontWeight = FontWeight.Normal
                )
            }
        }
    }
}

/**
 * Animated Typing Indicator Bubble with dynamic theme colors.
 */
@Composable
fun JarvisTypingIndicatorBubble(
    theme: JarvisBubbleTheme = JarvisBubbleTheme.ELEGANT_DARK,
    modifier: Modifier = Modifier
) {
    val animatedPrimary by animateColorAsState(targetValue = theme.primaryColor, animationSpec = tween(600), label = "typing_primary")
    val animatedSecondary by animateColorAsState(targetValue = theme.secondaryColor, animationSpec = tween(600), label = "typing_secondary")

    val infiniteTransition = rememberInfiniteTransition(label = "typing_anim")

    // Blinking cursor opacity
    val cursorAlpha by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(450, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "cursor_blink"
    )

    // Pulse border glow
    val borderGlow by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.8f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "border_glow"
    )

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 5.dp),
        horizontalArrangement = Arrangement.Start,
        verticalAlignment = Alignment.Bottom
    ) {
        // Glowing AI Node Avatar
        Box(
            modifier = Modifier
                .padding(bottom = 2.dp)
                .size(26.dp)
                .background(animatedPrimary.copy(alpha = 0.2f), CircleShape)
                .border(1.dp, animatedPrimary.copy(alpha = borderGlow), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .background(animatedPrimary, CircleShape)
            )
        }

        Spacer(modifier = Modifier.width(6.dp))

        // Typing indicator card
        Card(
            shape = RoundedCornerShape(
                topStart = 18.dp,
                topEnd = 18.dp,
                bottomStart = 4.dp,
                bottomEnd = 18.dp
            ),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.onSurface
            ),
            elevation = CardDefaults.cardElevation(
                defaultElevation = 2.dp
            ),
            border = BorderStroke(
                width = 1.dp,
                color = animatedPrimary.copy(alpha = borderGlow * 0.7f)
            ),
            modifier = Modifier.widthIn(max = 280.dp)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // 3 Animated Bouncing Dots with Theme Colors
                BouncingDotsIndicator(primaryColor = animatedPrimary, secondaryColor = animatedSecondary)

                // Thinking / streaming text label
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "Formulating response",
                        color = TextSlate,
                        fontSize = 13.sp,
                        fontFamily = FontFamily.Monospace
                    )
                    Text(
                        text = " ▌",
                        color = animatedPrimary.copy(alpha = cursorAlpha),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
fun BouncingDotsIndicator(
    primaryColor: Color = com.example.ui.theme.CyanJarvis,
    secondaryColor: Color = GreenSecure,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "dots_transition")

    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        val dotCount = 3
        for (i in 0 until dotCount) {
            // Staggered bounce offset
            val offsetY by infiniteTransition.animateFloat(
                initialValue = 0f,
                targetValue = -6f,
                animationSpec = infiniteRepeatable(
                    animation = keyframes {
                        durationMillis = 1000
                        0f at 0
                        -6f at (200 + i * 150) using FastOutSlowInEasing
                        0f at (400 + i * 150) using FastOutSlowInEasing
                        0f at 1000
                    },
                    repeatMode = RepeatMode.Restart
                ),
                label = "dot_offset_$i"
            )

            val alpha by infiniteTransition.animateFloat(
                initialValue = 0.35f,
                targetValue = 1f,
                animationSpec = infiniteRepeatable(
                    animation = keyframes {
                        durationMillis = 1000
                        0.35f at 0
                        1f at (200 + i * 150) using FastOutSlowInEasing
                        0.35f at (400 + i * 150) using FastOutSlowInEasing
                        0.35f at 1000
                    },
                    repeatMode = RepeatMode.Restart
                ),
                label = "dot_alpha_$i"
            )

            Box(
                modifier = Modifier
                    .offset(y = offsetY.dp)
                    .size(7.dp)
                    .background(
                        color = if (i == 1) secondaryColor.copy(alpha = alpha) else primaryColor.copy(alpha = alpha),
                        shape = CircleShape
                    )
            )
        }
    }
}
