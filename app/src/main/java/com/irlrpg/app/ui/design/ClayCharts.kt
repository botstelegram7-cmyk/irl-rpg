package com.irlrpg.app.ui.design

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.irlrpg.app.ui.theme.ClayAccent
import com.irlrpg.app.ui.theme.ClayInk
import com.irlrpg.app.ui.theme.ClayPalette
import com.irlrpg.app.ui.theme.ClayShape

data class ClayBar(
    val label: String,
    val value: Int,
    val highlight: Boolean = false,
)

/**
 * Weekly bar chart on clay: soft inset track per day, gradient fill scaled to
 * the busiest day, and value labels only where data exists (never invented).
 */
@Composable
fun ClayBarChart(
    bars: List<ClayBar>,
    accent: ClayAccent,
    modifier: Modifier = Modifier,
    barArea: Dp = 108.dp,
    showValues: Boolean = true,
    valueSuffix: String = "",
) {
    val maxValue = (bars.maxOfOrNull { it.value } ?: 0).coerceAtLeast(1)
    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(barArea),
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            bars.forEach { bar ->
                val fraction = bar.value.toFloat() / maxValue.toFloat()
                val animated by animateFloatAsState(
                    targetValue = fraction.coerceIn(0f, 1f),
                    animationSpec = tween(600, easing = FastOutSlowInEasing),
                    label = "barHeight",
                )
                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Bottom,
                ) {
                    if (showValues && bar.value > 0) {
                        Text(
                            text = if (valueSuffix.isBlank()) bar.value.toString() else "${bar.value}$valueSuffix",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (bar.highlight) accent.deep else ClayInk.Muted,
                            maxLines = 1,
                        )
                        Spacer(Modifier.height(4.dp))
                    }
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(barArea * 0.78f)
                            .clip(ClayShape.Pill)
                            .background(ClayPalette.SurfaceMuted.copy(alpha = 0.85f)),
                        contentAlignment = Alignment.BottomCenter,
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height((barArea * 0.78f * animated).coerceAtLeast(if (bar.value > 0) 8.dp else 0.dp))
                                .clip(ClayShape.Pill)
                                .background(
                                    brush = if (bar.highlight) {
                                        Brush.verticalGradient(listOf(accent.base, accent.deep))
                                    } else {
                                        Brush.verticalGradient(
                                            listOf(accent.soft, accent.base.copy(alpha = 0.75f))
                                        )
                                    },
                                ),
                        )
                    }
                }
            }
        }
        Spacer(Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            bars.forEach { bar ->
                Text(
                    text = bar.label,
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = if (bar.highlight) FontWeight.Bold else FontWeight.Medium,
                    color = if (bar.highlight) accent.deep else ClayInk.Faint,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                )
            }
        }
    }
}

/**
 * Character avatar: clay disc with a gradient XP ring, rank emoji and a level
 * badge. Pure Compose drawing — no bitmap assets, no 3D engine.
 */
@Composable
fun ClayAvatar(
    emoji: String,
    level: Int,
    progress: Float,
    accent: ClayAccent,
    modifier: Modifier = Modifier,
    avatarSize: Dp = 104.dp,
) {
    val animated by animateFloatAsState(
        targetValue = progress.coerceIn(0f, 1f),
        animationSpec = tween(800, easing = FastOutSlowInEasing),
        label = "xpRing",
    )
    Box(
        modifier = modifier.size(avatarSize),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val strokeWidth = size.minDimension * 0.075f
            val inset = strokeWidth / 2f
            val arcSize = Size(size.width - strokeWidth, size.height - strokeWidth)
            drawArc(
                color = ClayPalette.SurfaceMuted,
                startAngle = 0f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = Offset(inset, inset),
                size = arcSize,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round),
            )
            drawArc(
                brush = Brush.sweepGradient(listOf(accent.base, accent.deep, accent.base)),
                startAngle = -90f,
                sweepAngle = 360f * animated,
                useCenter = false,
                topLeft = Offset(inset, inset),
                size = arcSize,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round),
            )
        }
        Box(
            modifier = Modifier
                .size(avatarSize * 0.78f)
                .clay(shape = CircleShape, elevation = 6.dp, glowAlpha = 0.16f, bevelWidth = 2.dp)
                .clip(CircleShape)
                .background(
                    Brush.linearGradient(
                        listOf(
                            Color.White.copy(alpha = 0.9f),
                            accent.soft,
                        )
                    )
                ),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = emoji,
                fontSize = (avatarSize.value * 0.30f).sp,
                textAlign = TextAlign.Center,
            )
        }
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .clip(ClayShape.Pill)
                .background(ClayPalette.SurfacePure)
                .borderPill(accent)
                .padding(horizontal = 10.dp, vertical = 3.dp),
        ) {
            Text(
                text = "Lv $level",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = accent.deep,
            )
        }
    }
}

private fun Modifier.borderPill(accent: ClayAccent): Modifier = this.then(
    Modifier.clayBevel(shape = ClayShape.Pill, width = 1.5.dp)
)

/** Decorative floating clay blobs + sparkles used in hero areas. */
@Composable
fun ClaySparkles(
    modifier: Modifier = Modifier,
    tint: Color = ClayPalette.Butter.base,
    count: Int = 3,
) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val positions = listOf(
            Offset(w * 0.12f, h * 0.22f),
            Offset(w * 0.86f, h * 0.30f),
            Offset(w * 0.68f, h * 0.78f),
            Offset(w * 0.30f, h * 0.86f),
        )
        positions.take(count).forEachIndexed { index, center ->
            val r = (size.minDimension * (0.035f + index * 0.006f)).coerceAtLeast(2f)
            drawCircle(color = tint.copy(alpha = 0.55f), radius = r, center = center)
            drawCircle(
                color = Color.White.copy(alpha = 0.75f),
                radius = r * 0.42f,
                center = center - Offset(r * 0.28f, r * 0.28f),
            )
        }
    }
}

/** Circular clay badge used for reward artwork slots. */
@Composable
fun ClayRewardBlob(
    emoji: String,
    accent: ClayAccent,
    modifier: Modifier = Modifier,
    blobSize: Dp = 96.dp,
) {
    Box(modifier = modifier.size(blobSize), contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        accent.soft,
                        accent.base.copy(alpha = 0.45f),
                    ),
                    center = Offset(size.width * 0.38f, size.height * 0.34f),
                    radius = size.minDimension * 0.62f,
                ),
            )
        }
        ClaySparkles(modifier = Modifier.fillMaxSize(), tint = accent.base)
        Text(text = emoji, fontSize = (blobSize.value * 0.34f).sp)
    }
}
