package com.irlrpg.app.ui.design

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.irlrpg.app.ui.theme.ClayShadow

/**
 * Core claymorphism surface modifiers.
 *
 * Layering (bottom to top):
 *  1. [clayAmbientGlow]  — a wide, very soft tinted pool under the surface that
 *     gives the "sitting on warm light" feel on every API level.
 *  2. [clayShadow]       — real Compose elevation shadow using lavender-tinted
 *     ambient/spot colours instead of black.
 *  3. [clayBevel]        — inner top highlight stroke (light) + inner bottom
 *     shade, the classic soft-UI bevel that reads as moulded clay.
 */

/** A wide, soft tinted pool drawn behind the surface. */
fun Modifier.clayAmbientGlow(
    shape: Shape = RoundedCornerShape(28.dp),
    color: Color = ClayShadow.ambient,
    alpha: Float = 0.30f,
    spread: Float = 1.16f,
    sink: Dp = 10.dp,
): Modifier = drawBehind {
    val w = size.width
    val h = size.height
    val radius = (w.coerceAtLeast(h) * 0.5f) * spread
    val center = Offset(w / 2f, h * 0.98f + sink.toPx() * 0.25f)
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(color.copy(alpha = alpha), Color.Transparent),
            center = center,
            radius = radius,
        ),
        radius = radius,
        center = center,
    )
}

/** Real elevation shadow, tinted so it never reads as black smudge. */
fun Modifier.clayShadow(
    elevation: Dp = 12.dp,
    shape: Shape = RoundedCornerShape(28.dp),
    ambient: Color = ClayShadow.ambient.copy(alpha = 0.42f),
    spot: Color = ClayShadow.spot.copy(alpha = 0.34f),
): Modifier = shadow(
    elevation = elevation,
    shape = shape,
    clip = false,
    ambientColor = ambient,
    spotColor = spot,
)

/**
 * Soft-UI bevel: a light inner stroke along the top edge and a gentle inner
 * shade along the bottom edge, both following [shape].
 */
fun Modifier.clayBevel(
    shape: Shape = RoundedCornerShape(28.dp),
    highlight: Color = Color.White.copy(alpha = 0.92f),
    shade: Color = ClayShadow.spot.copy(alpha = 0.10f),
    width: Dp = 2.dp,
): Modifier = drawBehind {
    val outline = shape.createOutline(size, layoutDirection, this)
    val strokeWidth = width.toPx()
    drawOutline(
        outline = outline,
        brush = Brush.verticalGradient(
            colors = listOf(highlight, Color.Transparent, shade),
            startY = 0f,
            endY = size.height,
        ),
        style = Stroke(width = strokeWidth),
    )
}

/** Convenience: the full clay stack in one call. */
fun Modifier.clay(
    shape: Shape = RoundedCornerShape(28.dp),
    elevation: Dp = 12.dp,
    glowAlpha: Float = 0.26f,
    bevelWidth: Dp = 2.dp,
): Modifier = this
    .clayAmbientGlow(shape = shape, alpha = glowAlpha)
    .clayShadow(elevation = elevation, shape = shape)
    .clayBevel(shape = shape, width = bevelWidth)

/** Inset/track look for progress tracks and wells carved into the surface. */
fun Modifier.clayInset(
    shape: Shape = RoundedCornerShape(percent = 50),
    color: Color = Color(0xFFE9E1F2),
): Modifier = this
    .drawBehind {
        val outline = shape.createOutline(size, layoutDirection, this)
        drawOutline(outline = outline, color = color)
        drawOutline(
            outline = outline,
            brush = Brush.verticalGradient(
                colors = listOf(
                    ClayShadow.spot.copy(alpha = 0.16f),
                    Color.Transparent,
                    Color.White.copy(alpha = 0.65f),
                ),
            ),
            style = Stroke(width = 1.5.dp.toPx()),
        )
    }
