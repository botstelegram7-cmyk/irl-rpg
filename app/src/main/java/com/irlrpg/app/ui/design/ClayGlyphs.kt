package com.irlrpg.app.ui.design

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke

/**
 * Hand-drawn navigation glyphs.
 *
 * These are drawn straight onto a [Canvas] instead of pulling in an icon
 * dependency: it keeps the APK lean, guarantees identical rendering on every
 * device, and lets the strokes share the rounded, soft-UI language of the rest
 * of the interface.
 */
@Composable
fun ClayGlyph(
    type: ClayGlyphType,
    tint: Color,
    modifier: Modifier = Modifier,
    filled: Boolean = false,
) {
    Canvas(modifier = modifier) {
        val stroke = size.minDimension * 0.095f
        when (type) {
            ClayGlyphType.Home -> drawHome(tint, stroke, filled)
            ClayGlyphType.Quests -> drawQuests(tint, stroke, filled)
            ClayGlyphType.Skills -> drawSkills(tint, stroke, filled)
            ClayGlyphType.Analytics -> drawAnalytics(tint, stroke, filled)
            ClayGlyphType.Profile -> drawProfile(tint, stroke, filled)
        }
    }
}

enum class ClayGlyphType { Home, Quests, Skills, Analytics, Profile }

private fun DrawScope.drawHome(tint: Color, stroke: Float, filled: Boolean) {
    val w = size.width
    val h = size.height
    val roof = Path().apply {
        moveTo(w * 0.10f, h * 0.46f)
        lineTo(w * 0.50f, h * 0.13f)
        lineTo(w * 0.90f, h * 0.46f)
    }
    drawPath(
        path = roof,
        color = tint,
        style = Stroke(width = stroke, cap = StrokeCap.Round, join = StrokeJoin.Round),
    )
    val body = Path().apply {
        moveTo(w * 0.22f, h * 0.44f)
        lineTo(w * 0.22f, h * 0.86f)
        lineTo(w * 0.78f, h * 0.86f)
        lineTo(w * 0.78f, h * 0.44f)
    }
    drawPath(
        path = body,
        color = tint,
        style = if (filled) {
            Stroke(width = stroke, cap = StrokeCap.Round, join = StrokeJoin.Round)
        } else {
            Stroke(width = stroke, cap = StrokeCap.Round, join = StrokeJoin.Round)
        },
    )
    if (filled) {
        drawCircle(color = tint, radius = w * 0.075f, center = Offset(w * 0.50f, h * 0.66f))
    }
}

private fun DrawScope.drawQuests(tint: Color, stroke: Float, filled: Boolean) {
    val w = size.width
    val h = size.height
    val rows = listOf(0.26f, 0.50f, 0.74f)
    rows.forEachIndexed { index, y ->
        val dotX = w * 0.17f
        if (filled && index == 0) {
            drawCircle(color = tint, radius = w * 0.085f, center = Offset(dotX, h * y))
        } else {
            drawCircle(
                color = tint,
                radius = w * 0.075f,
                center = Offset(dotX, h * y),
                style = Stroke(width = stroke * 0.85f),
            )
        }
        drawLine(
            color = tint,
            start = Offset(w * 0.34f, h * y),
            end = Offset(w * 0.88f, h * y),
            strokeWidth = stroke,
            cap = StrokeCap.Round,
        )
    }
}

private fun DrawScope.drawSkills(tint: Color, stroke: Float, filled: Boolean) {
    val w = size.width
    val h = size.height
    val sparkle = Path().apply {
        moveTo(w * 0.50f, h * 0.08f)
        quadraticTo(w * 0.58f, h * 0.42f, w * 0.92f, h * 0.50f)
        quadraticTo(w * 0.58f, h * 0.58f, w * 0.50f, h * 0.92f)
        quadraticTo(w * 0.42f, h * 0.58f, w * 0.08f, h * 0.50f)
        quadraticTo(w * 0.42f, h * 0.42f, w * 0.50f, h * 0.08f)
        close()
    }
    if (filled) {
        drawPath(sparkle, tint)
    } else {
        drawPath(
            sparkle,
            tint,
            style = Stroke(width = stroke, cap = StrokeCap.Round, join = StrokeJoin.Round),
        )
    }
}

private fun DrawScope.drawAnalytics(tint: Color, stroke: Float, filled: Boolean) {
    val w = size.width
    val h = size.height
    val barWidth = w * 0.16f
    val radius = barWidth * 0.5f
    val bars = listOf(
        Triple(0.14f, 0.60f, 0.88f),
        Triple(0.42f, 0.38f, 0.88f),
        Triple(0.70f, 0.16f, 0.88f),
    )
    bars.forEach { (x, top, bottom) ->
        val topLeft = Offset(w * x, h * top)
        val barSize = Size(barWidth, h * (bottom - top))
        if (filled) {
            drawRoundRect(
                color = tint,
                topLeft = topLeft,
                size = barSize,
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(radius, radius),
            )
        } else {
            drawRoundRect(
                color = tint,
                topLeft = topLeft,
                size = barSize,
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(radius, radius),
                style = Stroke(width = stroke * 0.9f),
            )
        }
    }
}

private fun DrawScope.drawProfile(tint: Color, stroke: Float, filled: Boolean) {
    val w = size.width
    val h = size.height
    drawCircle(
        color = tint,
        radius = w * 0.155f,
        center = Offset(w * 0.50f, h * 0.30f),
        style = if (filled) {
            androidx.compose.ui.graphics.drawscope.Fill
        } else {
            Stroke(width = stroke)
        },
    )
    val shoulders = Path().apply {
        moveTo(w * 0.16f, h * 0.90f)
        quadraticTo(w * 0.50f, h * 0.50f, w * 0.84f, h * 0.90f)
    }
    drawPath(
        path = shoulders,
        color = tint,
        style = Stroke(width = stroke, cap = StrokeCap.Round, join = StrokeJoin.Round),
    )
}
