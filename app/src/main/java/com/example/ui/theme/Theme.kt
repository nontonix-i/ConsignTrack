package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFF3ECF8E),
    onPrimary = Color(0xFF042114),
    primaryContainer = SupabaseGreenMuted,
    onPrimaryContainer = SupabaseGreenLight,
    secondary = SupabaseGreenDark,
    onSecondary = Color.White,
    tertiary = BlueInfo,
    onTertiary = Color.Black,
    background = Color(0xFF121316),
    onBackground = Color(0xFFF4F4F5),
    surface = Color(0xFF181A1F),
    onSurface = Color(0xFFF4F4F5),
    surfaceVariant = Color(0xFF22252B),
    onSurfaceVariant = Color(0xFFA1A1AA),
    outline = Color(0xFF2D3139),
    outlineVariant = Color(0xFF23262D),
    error = RoseError,
    onError = Color.White
)

private val LightColorScheme = lightColorScheme(
    primary = SupabaseGreenDark,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFDCFCE7),
    onPrimaryContainer = Color(0xFF14532D),
    secondary = Color(0xFF0F766E),
    onSecondary = Color.White,
    tertiary = BlueInfo,
    onTertiary = Color.White,
    background = LightBg,
    onBackground = TextPrimaryLight,
    surface = LightSurface,
    onSurface = TextPrimaryLight,
    surfaceVariant = LightSurfaceElevated,
    onSurfaceVariant = TextSecondaryLight,
    outline = LightBorder,
    outlineVariant = Color(0xFFCBD5E1),
    error = RoseError,
    onError = Color.White
)

@Composable
fun ConsignTrackTheme(
    darkTheme: Boolean = true, // Default to sleek Supabase dark mode for professional feel
    content: @Composable () -> Unit
) {
    isDarkThemeActive = darkTheme
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}

// Keep backward compatibility
@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    ConsignTrackTheme(darkTheme = darkTheme, content = content)
}
