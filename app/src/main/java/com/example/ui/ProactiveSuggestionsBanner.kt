package com.example.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.CyanJarvis
import com.example.ui.theme.TertiaryJarvis
import com.example.ui.theme.TextSlate
import com.example.util.ContextualSuggestion

@Composable
fun ProactiveSuggestionsBanner(
    suggestions: List<ContextualSuggestion>,
    onSuggestionClicked: (ContextualSuggestion) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    var isVisible by remember { mutableStateOf(true) }

    if (suggestions.isEmpty() || !isVisible) return

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 6.dp)
            .background(
                brush = Brush.horizontalGradient(
                    colors = listOf(
                        CyanJarvis.copy(alpha = 0.12f),
                        TertiaryJarvis.copy(alpha = 0.10f),
                        MaterialTheme.colorScheme.surface.copy(alpha = 0.9f)
                    )
                ),
                shape = RoundedCornerShape(18.dp)
            )
            .padding(12.dp)
    ) {
        // Banner Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.AutoAwesome,
                    contentDescription = "Contextual Intelligence",
                    tint = CyanJarvis,
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    text = "PROACTIVE CONTEXT SUGGESTIONS",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = CyanJarvis,
                    fontFamily = FontFamily.Monospace,
                    letterSpacing = 1.sp
                )
            }

            IconButton(
                onClick = {
                    isVisible = false
                    onDismiss()
                },
                modifier = Modifier.size(22.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Dismiss Suggestions",
                    tint = TextSlate,
                    modifier = Modifier.size(14.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Horizontal Row of Proactive Cards
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            contentPadding = PaddingValues(horizontal = 2.dp)
        ) {
            items(suggestions, key = { it.id }) { suggestion ->
                ProactiveSuggestionCard(
                    suggestion = suggestion,
                    onClick = { onSuggestionClicked(suggestion) }
                )
            }
        }
    }
}

@Composable
fun ProactiveSuggestionCard(
    suggestion: ContextualSuggestion,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        onClick = onClick,
        modifier = modifier
            .width(260.dp)
            .height(110.dp),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f)
        ),
        border = BorderStroke(1.dp, CyanJarvis.copy(alpha = 0.35f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(10.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Badge & Category
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = suggestion.icon,
                        contentDescription = null,
                        tint = CyanJarvis,
                        modifier = Modifier.size(14.dp)
                    )
                    Text(
                        text = suggestion.title,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1
                    )
                }
            }

            Text(
                text = suggestion.description,
                fontSize = 11.sp,
                color = TextSlate,
                maxLines = 2,
                lineHeight = 14.sp
            )

            // Bottom Action Tag
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = CircleShape,
                    color = CyanJarvis.copy(alpha = 0.15f)
                ) {
                    Text(
                        text = suggestion.badgeText.uppercase(),
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = CyanJarvis,
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Text(
                        text = "Execute",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = CyanJarvis
                    )
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = "Execute action",
                        tint = CyanJarvis,
                        modifier = Modifier.size(12.dp)
                    )
                }
            }
        }
    }
}
