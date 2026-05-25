package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary = BluePrimary,
    secondary = LavenderSecondary,
    tertiary = TealTertiary,
    background = DarkBackground,
    surface = DarkSurface,
    surfaceVariant = DarkSurfaceVariant,
    error = CoralError,
    onBackground = TextWhite,
    onSurface = TextGray,
    onPrimary = Color.White,
    onSecondary = Color.White,
    outline = OutlineGray
)

private val LightColorScheme = lightColorScheme(
    primary = BluePrimary,
    secondary = LavenderSecondary,
    tertiary = TealTertiary,
    background = Color(0xFFF9FAFC),
    surface = Color.White,
    surfaceVariant = Color(0xFFF1F3F9),
    error = CoralError,
    onBackground = Color(0xFF0F172A),
    onSurface = Color(0xFF475569),
    onPrimary = Color.White,
    onSecondary = Color.White,
    outline = Color(0xFFE2E8F0)
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false, // Disable dynamic colors to enforce the RCC visual theme
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
