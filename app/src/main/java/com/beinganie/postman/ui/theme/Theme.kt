package com.beinganie.postman.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColorScheme = lightColorScheme(
    primary = InkBlue,
    onPrimary = Paper,
    primaryContainer = SkyBlue,
    onPrimaryContainer = Midnight,
    secondary = Mint,
    onSecondary = Midnight,
    secondaryContainer = Color(0xFFD9F8EF),
    onSecondaryContainer = Midnight,
    tertiary = Coral,
    background = Paper,
    onBackground = Midnight,
    surface = Paper,
    onSurface = Midnight,
    surfaceContainer = Cloud,
    surfaceContainerHighest = Color(0xFFDCE3F2),
    surfaceBright = Color(0xFFFFFFFF),
    onSurfaceVariant = Slate,
    outlineVariant = Color(0xFFC7D0E2),
)

private val DarkColorScheme = darkColorScheme(
    primary = SkyBlue,
    onPrimary = Midnight,
    primaryContainer = Color(0xFF1639A6),
    onPrimaryContainer = Paper,
    secondary = Mint,
    onSecondary = Midnight,
    secondaryContainer = Color(0xFF165B4A),
    onSecondaryContainer = Paper,
    tertiary = Coral,
    background = Color(0xFF09111F),
    onBackground = Paper,
    surface = Color(0xFF09111F),
    onSurface = Paper,
    surfaceContainer = Color(0xFF1A2437),
    surfaceContainerHighest = Color(0xFF22304B),
    surfaceBright = Color(0xFF132036),
    onSurfaceVariant = Color(0xFFB5C0D7),
    outlineVariant = Color(0xFF32405D),
)

@Composable
fun PostmanTheme(
    darkTheme: Boolean = false,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit,
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content,
    )
}
