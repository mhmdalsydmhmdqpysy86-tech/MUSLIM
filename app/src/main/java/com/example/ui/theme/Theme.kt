package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp

private val LightColorScheme = lightColorScheme(
    primary = Teal600,
    onPrimary = Color.White,
    primaryContainer = Teal50,
    onPrimaryContainer = Slate900,
    secondary = Sky600,
    onSecondary = Color.White,
    secondaryContainer = Sky50,
    onSecondaryContainer = Slate900,
    tertiary = WarningAmber500,
    onTertiary = Color.White,
    tertiaryContainer = WarningAmber50,
    onTertiaryContainer = WarningAmber600,
    background = Slate50,
    onBackground = Slate900,
    surface = Color.White,
    onSurface = Slate800,
    surfaceVariant = Slate100,
    onSurfaceVariant = Slate600,
    outline = Slate200,
    outlineVariant = Slate200,
    error = AlertRed600,
    onError = Color.White,
    errorContainer = AlertRed50,
    onErrorContainer = AlertRed600
)

private val DarkColorScheme = darkColorScheme(
    primary = Teal400,
    onPrimary = Slate950,
    primaryContainer = Slate800,
    onPrimaryContainer = Teal100,
    secondary = Sky400,
    onSecondary = Slate950,
    secondaryContainer = Slate800,
    onSecondaryContainer = Sky50,
    tertiary = WarningAmber500,
    onTertiary = Slate950,
    tertiaryContainer = Slate800,
    onTertiaryContainer = WarningAmber100,
    background = Slate950,
    onBackground = Slate50,
    surface = Slate900,
    onSurface = Slate100,
    surfaceVariant = Slate800,
    onSurfaceVariant = Slate400,
    outline = Slate700,
    outlineVariant = Slate800,
    error = AlertRed500,
    onError = Color.White,
    errorContainer = Color(0xFF450A0A),
    onErrorContainer = AlertRed100
)

val AppShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(22.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

@Composable
fun OptiCleanTheme(
    darkTheme: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            shapes = AppShapes,
            content = content
        )
    }
}
