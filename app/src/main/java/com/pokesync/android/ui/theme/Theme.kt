package com.pokesync.android.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColorScheme = lightColorScheme(
    primary = HomeTeal,
    onPrimary = Color.White,
    primaryContainer = HomeMintBg,
    onPrimaryContainer = HomeTealDark,
    secondary = PokeBlue,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFDBE1FF),
    onSecondaryContainer = Color(0xFF001258),
    tertiary = HomeYellow,
    onTertiary = Color(0xFF3D2F00),
    surface = Color.White,
    onSurface = Color(0xFF1A1A1A),
    surfaceVariant = HomeSlotEmpty,
    onSurfaceVariant = Color(0xFF444444),
    background = HomeMintBg,
    onBackground = Color(0xFF1A1A1A),
    outline = HomeTealDark.copy(alpha = 0.4f),
)

private val DarkColorScheme = darkColorScheme(
    primary = HomeTealLight,
    onPrimary = HomeDarkBg,
    primaryContainer = HomeTealDark,
    onPrimaryContainer = Color.White,
    secondary = Color(0xFF9DAEFF),
    onSecondary = Color(0xFF001587),
    tertiary = HomeYellow,
    onTertiary = Color(0xFF3D2F00),
    surface = HomeDarkSurface,
    onSurface = Color(0xFFE8E8E8),
    surfaceVariant = HomeSlotEmptyDark,
    onSurfaceVariant = Color(0xFFB0BEC5),
    background = HomeDarkBg,
    onBackground = Color(0xFFE8E8E8),
    outline = HomeTealLight.copy(alpha = 0.3f),
)

@Composable
fun PokeSyncTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme,
        typography = Typography,
        content = content,
    )
}
