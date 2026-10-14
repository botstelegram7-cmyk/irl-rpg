package com.irlrpg.app.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/**
 * A pastel "clay" accent: [base] for fills/icons, [soft] for tinted clay tiles,
 * [deep] for text and iconography placed on [soft].
 */
data class ClayAccent(
    val base: Color,
    val soft: Color,
    val deep: Color,
)

/** Warm ivory claymorphism palette. Light theme only — that is the product direction. */
object ClayPalette {
    val WarmIvory = Color(0xFFF8F2EA)
    val WarmIvoryDeep = Color(0xFFF1EAE1)
    val SurfaceCream = Color(0xFFFFFBF6)
    val SurfacePure = Color(0xFFFFFFFF)
    val SurfaceMuted = Color(0xFFF3EDF7)

    val Lavender = ClayAccent(Color(0xFF9F86EF), Color(0xFFEDE6FD), Color(0xFF6A4CC2))
    val Sky = ClayAccent(Color(0xFF6FB6F0), Color(0xFFE0F0FD), Color(0xFF2B79BA))
    val Mint = ClayAccent(Color(0xFF55D2AC), Color(0xFFDDF6EE), Color(0xFF1B9375))
    val Peach = ClayAccent(Color(0xFFF7A97F), Color(0xFFFDEAE0), Color(0xFFC4672F))
    val Butter = ClayAccent(Color(0xFFF2C868), Color(0xFFFBF0D5), Color(0xFFAE7E15))
    val Blush = ClayAccent(Color(0xFFEF98B5), Color(0xFFFCE5ED), Color(0xFFB94B72))

    /** Ordered accents used when the user creates a category without picking a colour. */
    val accentRotation = listOf(Lavender, Sky, Mint, Peach, Butter, Blush)

    fun accentById(id: String): ClayAccent = when (id) {
        "lavender" -> Lavender
        "sky" -> Sky
        "mint" -> Mint
        "peach" -> Peach
        "butter" -> Butter
        "blush" -> Blush
        else -> Lavender
    }

    fun accentIdOf(accent: ClayAccent): String = when (accent) {
        Sky -> "sky"
        Mint -> "mint"
        Peach -> "peach"
        Butter -> "butter"
        Blush -> "blush"
        else -> "lavender"
    }
}

/** Ink colours: soft plum-grey instead of pure black — keeps the clay feel warm. */
object ClayInk {
    val Primary = Color(0xFF3B3450)
    val Secondary = Color(0xFF6D6781)
    val Muted = Color(0xFF9A93AB)
    val Faint = Color(0xFFBDB6CA)
    val OnAccent = Color(0xFFFFFFFF)
}

/** Shadow tint used under clay surfaces (a lavender-grey, never black). */
object ClayShadow {
    val ambient = Color(0xFF8E82AA)
    val spot = Color(0xFF7A6D9C)
    val track = Color(0xFFE4DCEF)
}

val ClayShapes = Shapes(
    extraSmall = RoundedCornerShape(10.dp),
    small = RoundedCornerShape(14.dp),
    medium = RoundedCornerShape(20.dp),
    large = RoundedCornerShape(26.dp),
    extraLarge = RoundedCornerShape(32.dp),
)

/** Named clay corner radii used across custom components. */
object ClayShape {
    val Tile = RoundedCornerShape(18.dp)
    val Small = RoundedCornerShape(16.dp)
    val Medium = RoundedCornerShape(22.dp)
    val Card = RoundedCornerShape(28.dp)
    val Hero = RoundedCornerShape(34.dp)
    val Pill = RoundedCornerShape(percent = 50)
}

/** Very soft page background — an ivory wash drifting into a faint lavender. */
val ClayBackgroundBrush = Brush.linearGradient(
    colors = listOf(
        ClayPalette.WarmIvory,
        Color(0xFFF7F0E9),
        Color(0xFFF4F0FA),
    )
)

/** Backgrounds for pastel clay tiles / hearts. */
fun clayAccentSurface(accent: ClayAccent): Brush = Brush.linearGradient(
    colors = listOf(accent.soft, accent.soft.copy(alpha = 0.72f)),
)

/** Tactile "pressed clay" gradient for filled buttons. */
fun clayButtonBrush(accent: ClayAccent): Brush = Brush.linearGradient(
    colors = listOf(
        accent.base.copy(alpha = 0.94f),
        accent.base,
        accent.deep.copy(alpha = 0.92f),
    ),
)
