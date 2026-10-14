package com.irlrpg.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val ClayColorScheme = lightColorScheme(
    primary = ClayPalette.Lavender.base,
    onPrimary = ClayInk.OnAccent,
    primaryContainer = ClayPalette.Lavender.soft,
    onPrimaryContainer = ClayPalette.Lavender.deep,

    secondary = ClayPalette.Mint.base,
    onSecondary = ClayInk.OnAccent,
    secondaryContainer = ClayPalette.Mint.soft,
    onSecondaryContainer = ClayPalette.Mint.deep,

    tertiary = ClayPalette.Peach.base,
    onTertiary = ClayInk.OnAccent,
    tertiaryContainer = ClayPalette.Peach.soft,
    onTertiaryContainer = ClayPalette.Peach.deep,

    background = ClayPalette.WarmIvory,
    onBackground = ClayInk.Primary,

    surface = ClayPalette.SurfaceCream,
    onSurface = ClayInk.Primary,
    surfaceVariant = ClayPalette.SurfaceMuted,
    onSurfaceVariant = ClayInk.Secondary,
    surfaceContainer = ClayPalette.SurfaceCream,
    surfaceContainerHigh = ClayPalette.SurfacePure,
    surfaceContainerLow = ClayPalette.WarmIvoryDeep,

    outline = Color(0xFFD8CFE3),
    outlineVariant = Color(0xFFE9E2F1),

    error = Color(0xFFD9645F),
    onError = Color.White,
    errorContainer = Color(0xFFFBE4E2),
    onErrorContainer = Color(0xFF8C322D),

    scrim = Color(0x66000000),
)

/**
 * The single theme entry point for IRL RPG. Deliberately light-only: the
 * claymorphism direction is a warm ivory surface language, so a dark variant
 * would be a different design system rather than an inverted one.
 */
@Composable
fun IrlRpgTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = ClayColorScheme,
        typography = ClayTypography,
        shapes = ClayShapes,
        content = content,
    )
}
