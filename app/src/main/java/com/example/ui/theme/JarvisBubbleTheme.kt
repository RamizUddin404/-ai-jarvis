package com.example.ui.theme

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

enum class JarvisBubbleTheme(
    val id: String,
    val title: String,
    val primaryColor: Color,
    val secondaryColor: Color,
    val glowColor: Color,
    val bgStartColor: Color,
    val bgEndColor: Color
) {
    ELEGANT_DARK(
        id = "elegant_dark",
        title = "Elegant Dark (Nebula)",
        primaryColor = Color(0xFFC084FC),
        secondaryColor = Color(0xFF38BDF8),
        glowColor = Color(0xFFA855F7),
        bgStartColor = Color(0xFF0D0714),
        bgEndColor = Color(0xFF050208)
    ),
    ARC_REACTOR(
        id = "arc_reactor",
        title = "Arc Cyan",
        primaryColor = Color(0xFF00F2FE),
        secondaryColor = Color(0xFF4FACFE),
        glowColor = Color(0xFF00E5FF),
        bgStartColor = Color(0xFF070D18),
        bgEndColor = Color(0xFF03070E)
    ),
    CYBER_PULSE(
        id = "cyber_pulse",
        title = "Neon Violet",
        primaryColor = Color(0xFFD946EF),
        secondaryColor = Color(0xFF8B5CF6),
        glowColor = Color(0xFFA855F7),
        bgStartColor = Color(0xFF140824),
        bgEndColor = Color(0xFF080312)
    ),
    QUANTUM_EMERALD(
        id = "quantum_emerald",
        title = "Quantum Emerald",
        primaryColor = Color(0xFF10B981),
        secondaryColor = Color(0xFF06B6D4),
        glowColor = Color(0xFF34D399),
        bgStartColor = Color(0xFF041812),
        bgEndColor = Color(0xFF020B08)
    ),
    SOLAR_FLARE(
        id = "solar_flare",
        title = "Solar Gold",
        primaryColor = Color(0xFFF59E0B),
        secondaryColor = Color(0xFFEF4444),
        glowColor = Color(0xFFFBBF24),
        bgStartColor = Color(0xFF1C0F04),
        bgEndColor = Color(0xFF0C0602)
    ),
    PLASMA_BLUE(
        id = "plasma_blue",
        title = "Deep Plasma",
        primaryColor = Color(0xFF38BDF8),
        secondaryColor = Color(0xFF3B82F6),
        glowColor = Color(0xFF60A5FA),
        bgStartColor = Color(0xFF03122B),
        bgEndColor = Color(0xFF010612)
    ),
    RUBY_PROTOCOL(
        id = "ruby_protocol",
        title = "Ruby Red",
        primaryColor = Color(0xFFF43F5E),
        secondaryColor = Color(0xFFBE123C),
        glowColor = Color(0xFFFB7185),
        bgStartColor = Color(0xFF1A050A),
        bgEndColor = Color(0xFF0A0204)
    );

    fun getBackgroundGradient(): Brush {
        return Brush.verticalGradient(
            colors = listOf(bgStartColor, bgEndColor)
        )
    }

    companion object {
        fun fromId(id: String): JarvisBubbleTheme {
            return entries.firstOrNull { it.id.equals(id, ignoreCase = true) } ?: ARC_REACTOR
        }
    }
}
