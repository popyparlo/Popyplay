package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary = PixelRosePrimary,
    onPrimary = PixelOnPrimary,
    primaryContainer = PixelRoseContainer,
    onPrimaryContainer = PixelRosePrimary,
    secondary = PixelMintSecondary,
    onSecondary = PixelOnSecondary,
    secondaryContainer = PixelMintContainer,
    onSecondaryContainer = PixelMintSecondary,
    tertiary = PixelPeriwinkle,
    background = PixelDarkBg,
    onBackground = PixelTextPrimary,
    surface = PixelDarkSurface,
    onSurface = PixelTextPrimary,
    surfaceVariant = PixelDarkSurfaceVariant,
    onSurfaceVariant = PixelTextSecondary,
    outline = PixelTextMuted
)

private val LightColorScheme = lightColorScheme(
    primary = Color(0xFF8B475D),
    onPrimary = Color.White,
    primaryContainer = PixelRosePrimary,
    onPrimaryContainer = PixelOnPrimary,
    secondary = Color(0xFF3E664A),
    onSecondary = Color.White,
    background = Color(0xFFFFF7F9),
    surface = Color(0xFFFDEEF2),
    onSurface = Color(0xFF2E1720)
)

@Composable
fun PixelPlayerTheme(
    darkTheme: Boolean = true, // Default to gorgeous dark mode as in screenshots
    dynamicColor: Boolean = true,
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
