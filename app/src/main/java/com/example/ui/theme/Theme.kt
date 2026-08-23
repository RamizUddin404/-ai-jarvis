package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = PrimaryJarvis,
    secondary = SecondaryJarvis,
    tertiary = TertiaryJarvis,
    background = BackgroundDark,
    surface = SurfaceDark,
    onPrimary = BackgroundDark,
    onSecondary = BackgroundDark,
    onTertiary = Color.White,
    onBackground = TextWhite,
    onSurface = TextWhite,
    outline = BorderDark
)

private val LightColorScheme = lightColorScheme(
    primary = PrimaryJarvis,
    secondary = SecondaryJarvis,
    tertiary = TertiaryJarvis,
    background = Color(0xFFF1F5F9), // Light background
    surface = Color.White,
    onPrimary = Color.White,
    onSecondary = Color.White,
    onTertiary = Color.White,
    onBackground = Color(0xFF05070A),
    onSurface = Color(0xFF0A0D14),
    outline = Color(0xFFE2E8F0)
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = true,
    content: @Composable () -> Unit
) {
    val colors = if (darkTheme) DarkColorScheme else LightColorScheme
    MaterialTheme(
        colorScheme = colors,
        typography = Typography,
        content = content
    )
}
