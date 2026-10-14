package com.irlrpg.app.ui.design

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.ui.draw.rotate
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
import kotlin.math.cos
import kotlin.math.sin

/**
 * Lightweight 2D "dimensional" artwork.
 *
 * Everything here is drawn with Canvas primitives and cheap alpha gradients, so it
 * reads like a soft 3D clay render without a 3D engine, textures, or extra
 * dependencies — a few hundred triangles at most, on one layer, animated only by
 * transform/alpha so it stays smooth on budget hardware.
 */

/**
 * The character illustration: a floating clay avatar over a layered pedestal,
 * with an optional cosmetic frame ring and a gentle idle bob.
 *
 * @param progress fills the cosmetic ring (character level progress).
 * @param frameAccent ring colour — driven by the equipped cosmetic frame.
 */
@Composable
fun ClayAvatarPedestal(
    emoji: String,
    accent: ClayAccent,
    progress: Float,
    modifier: Modifier = Modifier,
    size: Dp = 168.dp,
    frameAccent: ClayAccent = accent,
    ringThickness: Dp = 6.dp,
    animate: Boolean = true,
    badge: String? = null,
) {
    val transition = rememberInfiniteTransition(label = "pedestal")
    val animatedBob by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(3200, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "bob",
    )
    // The idle bob is the only running animation in the app; disabling it is a
    // single flag so a future "reduce motion" setting can switch it off.
    val bob = if (animate) animatedBob else 0.5f
    val ring by animateFloatAsState(
        targetValue = progress.coerceIn(0f, 1f),
        animationSpec = tween(900, easing = FastOutSlowInEasing),
        label = "ring",
    )

    Box(modifier = modifier.size(size), contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = this.size.width
            val h = this.size.height
            val cx = w / 2f
            val float = bob * h * 0.02f
            val avatarRadius = w * 0.27f
            val avatarCenter = Offset(cx, h * 0.42f - float)

            // 1. Soft ambient pool under the whole illustration.
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(accent.base.copy(alpha = 0.20f), Color.Transparent),
                    center = Offset(cx, h * 0.80f),
                    radius = w * 0.52f,
                ),
                radius = w * 0.52f,
                center = Offset(cx, h * 0.80f),
            )

            // 2. Lower pedestal disc (wide, warm, low contrast).
            drawOval(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color.White.copy(alpha = 0.92f),
                        accent.soft.copy(alpha = 0.95f),
                    ),
                    startY = h * 0.72f,
                    endY = h * 0.94f,
                ),
                topLeft = Offset(cx - w * 0.34f, h * 0.74f),
                size = Size(w * 0.68f, h * 0.18f),
            )
            // 3. Upper pedestal disc — narrower, catches the light.
            drawOval(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color.White.copy(alpha = 0.98f),
                        accent.soft.copy(alpha = 0.85f),
                    ),
                    startY = h * 0.66f,
                    endY = h * 0.84f,
                ),
                topLeft = Offset(cx - w * 0.25f, h * 0.68f),
                size = Size(w * 0.50f, h * 0.14f),
            )
            // 4. Rim highlight on the pedestal edge.
            drawOval(
                color = Color.White.copy(alpha = 0.75f),
                topLeft = Offset(cx - w * 0.25f, h * 0.68f),
                size = Size(w * 0.50f, h * 0.14f),
                style = Stroke(width = (w * 0.006f).coerceAtLeast(1f)),
            )

            // 5. Contact shadow the avatar casts onto the pedestal.
            drawOval(
                brush = Brush.radialGradient(
                    colors = listOf(ClayPalette.WarmIvoryDeep.copy(alpha = 0.55f), Color.Transparent),
                    center = Offset(cx, h * 0.70f),
                    radius = avatarRadius * 0.9f,
                ),
                topLeft = Offset(cx - avatarRadius * 0.85f, h * 0.675f),
                size = Size(avatarRadius * 1.7f, avatarRadius * 0.34f),
            )

            // 6. Avatar body: a moulded clay ball with a top highlight and a
            //    warmer bottom bounce, which is what sells the 3D read.
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color.White.copy(alpha = 0.95f),
                        accent.soft,
                    ),
                    center = Offset(avatarCenter.x - avatarRadius * 0.35f, avatarCenter.y - avatarRadius * 0.45f),
                    radius = avatarRadius * 1.5f,
                ),
                radius = avatarRadius,
                center = avatarCenter,
            )
            drawCircle(
                brush = Brush.verticalGradient(
                    colors = listOf(Color.Transparent, accent.base.copy(alpha = 0.22f)),
                    startY = avatarCenter.y,
                    endY = avatarCenter.y + avatarRadius,
                ),
                radius = avatarRadius,
                center = avatarCenter,
            )
            drawCircle(
                color = Color.White.copy(alpha = 0.55f),
                radius = avatarRadius,
                center = avatarCenter,
                style = Stroke(width = (w * 0.008f).coerceAtLeast(1f)),
            )

            // 7. Cosmetic ring around the avatar (level progress).
            if (ring > 0f) {
                val ringRadius = avatarRadius * 1.20f
                val stroke = ringThickness.toPx()
                drawArc(
                    color = ClayPalette.SurfaceMuted,
                    startAngle = -90f,
                    sweepAngle = 360f,
                    useCenter = false,
                    topLeft = Offset(cx - ringRadius, avatarCenter.y - ringRadius),
                    size = Size(ringRadius * 2f, ringRadius * 2f),
                    style = Stroke(stroke, cap = StrokeCap.Round),
                )
                drawArc(
                    brush = Brush.sweepGradient(
                        listOf(frameAccent.base, frameAccent.deep, frameAccent.base),
                    ),
                    startAngle = -90f,
                    sweepAngle = 360f * ring,
                    useCenter = false,
                    topLeft = Offset(cx - ringRadius, avatarCenter.y - ringRadius),
                    size = Size(ringRadius * 2f, ringRadius * 2f),
                    style = Stroke(stroke, cap = StrokeCap.Round),
                )
            }

            // 8. Two slow drifting motes for life (no particle system, 2 circles).
            val drift = if (animate) bob else 0.5f
            listOf(
                Offset(cx + w * 0.30f, h * (0.24f + drift * 0.04f)) to w * 0.035f,
                Offset(cx - w * 0.33f, h * (0.34f - drift * 0.05f)) to w * 0.026f,
            ).forEachIndexed { index, (center, radius) ->
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            if (index == 0) ClayPalette.Butter.base.copy(alpha = 0.75f)
                            else ClayPalette.Sky.base.copy(alpha = 0.6f),
                            Color.Transparent,
                        ),
                        center = center,
                        radius = radius * 2.2f,
                    ),
                    radius = radius * 2.2f,
                    center = center,
                )
            }
        }

        // Emoji sits on the avatar body, slightly above the pedestal.
        Text(
            text = emoji,
            fontSize = (size.value * 0.22f).sp,
            modifier = Modifier.padding(bottom = size * 0.10f),
        )

        if (badge != null) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = size * 0.055f)
                    .clip(ClayShape.Pill)
                    .background(accent.deep)
                    .padding(horizontal = 12.dp, vertical = 3.dp),
            ) {
                Text(
                    text = badge,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    maxLines = 1,
                )
            }
        }
    }
}

/**
 * Circular progress ring used for "XP towards next level". A ring is used here
 * (instead of a bar) because it frames the avatar and reads instantly.
 */
@Composable
fun ClayRingStat(
    progress: Float,
    accent: ClayAccent,
    centerValue: String,
    centerLabel: String,
    modifier: Modifier = Modifier,
    diameter: Dp = 104.dp,
    thickness: Dp = 11.dp,
) {
    val animated by animateFloatAsState(
        targetValue = progress.coerceIn(0f, 1f),
        animationSpec = tween(900, easing = FastOutSlowInEasing),
        label = "ringStat",
    )
    Box(modifier = modifier.size(diameter), contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val stroke = thickness.toPx()
            val inset = stroke / 2f
            val arcSize = Size(size.width - stroke, size.height - stroke)
            drawArc(
                color = ClayPalette.SurfaceMuted,
                startAngle = 0f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = Offset(inset, inset),
                size = arcSize,
                style = Stroke(stroke, cap = StrokeCap.Round),
            )
            drawArc(
                brush = Brush.linearGradient(listOf(accent.base, accent.deep)),
                startAngle = -90f,
                sweepAngle = 360f * animated,
                useCenter = false,
                topLeft = Offset(inset, inset),
                size = arcSize,
                style = Stroke(stroke, cap = StrokeCap.Round),
            )
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = centerValue,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = ClayInk.Primary,
                maxLines = 1,
            )
            Text(
                text = centerLabel,
                style = MaterialTheme.typography.labelSmall,
                color = ClayInk.Muted,
                maxLines = 1,
                textAlign = TextAlign.Center,
            )
        }
    }
}

/** Timeline dot with a connector line, used for progression milestones. */
@Composable
fun ClayMilestone(
    emoji: String,
    accent: ClayAccent,
    modifier: Modifier = Modifier,
    isLast: Boolean = false,
    connectorColor: Color = ClayInk.Faint.copy(alpha = 0.30f),
    content: @Composable () -> Unit,
) {
    Row(modifier = modifier) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            ClayDot(emoji = emoji, accent = accent, size = 34.dp)
            if (!isLast) {
                Box(
                    modifier = Modifier
                        .size(width = 2.dp, height = 42.dp)
                        .background(connectorColor),
                )
            }
        }
        Spacer(Modifier.width(12.dp))
        Box(modifier = Modifier.padding(top = 4.dp)) { content() }
    }
}

/** Sealed "trophy shelf" strip: earned reward blobs in a row. */
@Composable
fun ClayRewardShelf(
    rewards: List<Pair<String, ClayAccent>>,
    modifier: Modifier = Modifier,
    itemSize: Dp = 52.dp,
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        rewards.forEachIndexed { index, (emoji, accent) ->
            if (index > 0) Spacer(Modifier.width(8.dp))
            Box(
                modifier = Modifier
                    .size(itemSize)
                    .rotate(if (index % 2 == 0) -4f else 4f)
                    .clip(CircleShape)
                    .clay(shape = CircleShape, elevation = 8.dp, glowAlpha = 0.18f)
                    .background(
                        Brush.linearGradient(listOf(Color.White, accent.soft)),
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Text(text = emoji, fontSize = (itemSize.value / 2.3f).sp)
            }
        }
    }
}

/** Deterministic decorative arc used behind hero cards (no randomness per frame). */
@Composable
fun ClayHeroBackdrop(
    accent: ClayAccent,
    modifier: Modifier = Modifier,
    arcCount: Int = 3,
) {
    Canvas(modifier = modifier) {
        val cx = size.width * 0.86f
        val cy = size.height * 0.12f
        repeat(arcCount) { index ->
            val radius = size.minDimension * (0.45f + index * 0.22f)
            val angle = 12.0 * index
            val x = cx + (radius * 0.25f) * cos(Math.toRadians(angle)).toFloat()
            val y = cy + (radius * 0.25f) * sin(Math.toRadians(angle)).toFloat()
            drawCircle(
                color = accent.base.copy(alpha = 0.07f - index * 0.015f),
                radius = radius * 0.6f,
                center = Offset(x, y),
            )
        }
    }
}
