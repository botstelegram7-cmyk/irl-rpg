package com.irlrpg.app.ui.components

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.irlrpg.app.domain.game.BossVictory
import com.irlrpg.app.domain.model.AchievementDef
import com.irlrpg.app.ui.design.ClayButton
import com.irlrpg.app.ui.design.ClayCard
import com.irlrpg.app.ui.design.ClayProgressBar
import com.irlrpg.app.ui.design.ClayRewardBlob
import com.irlrpg.app.ui.theme.ClayAccent
import com.irlrpg.app.ui.theme.ClayInk
import com.irlrpg.app.ui.theme.ClayPalette
import com.irlrpg.app.ui.theme.ClayShape
import com.irlrpg.app.ui.theme.clayButtonBrush
import kotlinx.coroutines.delay

/**
 * Level-up sequence.
 *
 * Shown only after the engine confirms a real level change; there is no path that
 * triggers it from a UI event alone. Animations are transform/alpha only (a soft
 * scale-in plus two slow floating orbs), so it stays smooth on budget hardware and
 * does not block the UI thread.
 */
@Composable
fun LevelUpOverlay(
    newLevel: Int,
    rankTitle: String,
    rankEmoji: String,
    totalXp: Int,
    xpIntoLevel: Int,
    xpForLevel: Int,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    BackHandler { onDismiss() }
    val float = rememberInfiniteTransition(label = "levelUpFloat")
    val drift by float.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "drift",
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0x663B3450))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onDismiss,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Box(modifier = Modifier.padding(horizontal = 24.dp)) {
            ClayCard(
                modifier = Modifier.fillMaxWidth(),
                shape = ClayShape.Hero,
                elevation = 24.dp,
                glowAlpha = 0.4f,
                contentPadding = PaddingValues(horizontal = 22.dp, vertical = 28.dp),
            ) {
                Box(modifier = Modifier.fillMaxWidth()) {
                    Canvas(modifier = Modifier.fillMaxWidth().height(120.dp)) {
                        val centers = listOf(
                            Offset(size.width * 0.12f, size.height * (0.24f + drift * 0.06f)),
                            Offset(size.width * 0.88f, size.height * (0.76f - drift * 0.08f)),
                            Offset(size.width * 0.74f, size.height * (0.18f + drift * 0.05f)),
                            Offset(size.width * 0.24f, size.height * (0.82f - drift * 0.06f)),
                        )
                        centers.forEachIndexed { index, center ->
                            val radius = size.minDimension * (0.05f + index * 0.012f)
                            drawCircle(
                                brush = Brush.radialGradient(
                                    colors = listOf(
                                        ClayPalette.Butter.base.copy(alpha = 0.55f),
                                        Color.Transparent,
                                    ),
                                    center = center,
                                    radius = radius * 2.4f,
                                ),
                                radius = radius * 2.4f,
                                center = center,
                            )
                        }
                    }
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        ClayRewardBlob(emoji = rankEmoji, accent = ClayPalette.Butter, blobSize = 104.dp)
                        Spacer(Modifier.height(14.dp))
                        Text(
                            text = "LEVEL UP",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = ClayPalette.Butter.deep,
                            letterSpacing = 3.sp,
                        )
                        Text(
                            text = "Level $newLevel",
                            style = MaterialTheme.typography.displaySmall,
                            color = ClayInk.Primary,
                        )
                        Spacer(Modifier.height(6.dp))
                        Box(
                            modifier = Modifier
                                .clip(ClayShape.Pill)
                                .background(ClayPalette.Butter.soft)
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                        ) {
                            Text(
                                text = "New rank · $rankTitle",
                                style = MaterialTheme.typography.labelLarge,
                                color = ClayPalette.Butter.deep,
                            )
                        }
                        Spacer(Modifier.height(20.dp))
                        ClayProgressBar(
                            progress = if (xpForLevel > 0) xpIntoLevel.toFloat() / xpForLevel.toFloat() else 0f,
                            accent = ClayPalette.Butter,
                            height = 12.dp,
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = "$xpIntoLevel / $xpForLevel XP towards level ${newLevel + 1} · $totalXp XP lifetime",
                            style = MaterialTheme.typography.labelSmall,
                            color = ClayInk.Muted,
                            textAlign = TextAlign.Center,
                        )
                        Spacer(Modifier.height(22.dp))
                        ClayButton(
                            text = "Keep going",
                            onClick = onDismiss,
                            accent = ClayPalette.Butter,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }
            }
        }
    }
}

/**
 * Achievement banner. Achievements arrive from real activity only, and several
 * can land at once, so they are queued and shown one at a time.
 */
@Composable
fun AchievementBanner(
    newlyEarned: List<AchievementDef>,
    onShown: (AchievementDef) -> Unit,
    modifier: Modifier = Modifier,
    bottomPadding: androidx.compose.ui.unit.Dp = 116.dp,
) {
    var current by remember { mutableStateOf<AchievementDef?>(null) }
    var visible by remember { mutableStateOf(false) }

    LaunchedEffect(newlyEarned) {
        if (newlyEarned.isEmpty()) return@LaunchedEffect
        newlyEarned.forEach { def ->
            current = def
            visible = true
            delay(3200)
            visible = false
            delay(240)
            onShown(def)
        }
        current = null
    }

    val def = current ?: return
    val accent = ClayPalette.accentById(def.tier.accentId)

    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.BottomCenter) {
        AnimatedVisibility(
            visible = visible,
            enter = fadeIn() + slideInVertically { it / 2 } + scaleIn(initialScale = 0.9f),
            exit = fadeOut() + slideOutVertically { it / 2 },
            modifier = Modifier.padding(bottom = bottomPadding),
        ) {
            Row(
                modifier = Modifier
                    .padding(horizontal = 20.dp)
                    .clip(ClayShape.Card)
                    .background(ClayPalette.SurfacePure)
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .clip(CircleShape)
                        .background(accent.soft),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(text = def.emoji, fontSize = 22.sp)
                }
                Spacer(Modifier.width(14.dp))
                Column(modifier = Modifier.width(210.dp)) {
                    Text(
                        text = "Achievement unlocked · ${def.tier.label}",
                        style = MaterialTheme.typography.labelSmall,
                        color = accent.deep,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        text = def.name,
                        style = MaterialTheme.typography.titleSmall,
                        color = ClayInk.Primary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = def.description,
                        style = MaterialTheme.typography.labelSmall,
                        color = ClayInk.Muted,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}

/** Victory sequence for a boss challenge — same language as the level-up overlay. */
@Composable
fun BossVictoryOverlay(
    victory: BossVictory,
    objectivesTotal: Int,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    BackHandler { onDismiss() }
    var visible by remember(victory.bossId) { mutableStateOf(false) }
    LaunchedEffect(victory.bossId) { visible = true }

    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(),
        exit = fadeOut(),
        modifier = modifier,
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0x663B3450))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onDismiss,
                ),
            contentAlignment = Alignment.Center,
        ) {
            AnimatedVisibility(
                visible = visible,
                enter = scaleIn(initialScale = 0.74f, animationSpec = spring(dampingRatio = 0.58f)) + fadeIn(),
            ) {
                Box(modifier = Modifier.padding(horizontal = 24.dp)) {
                    ClayCard(
                        modifier = Modifier.fillMaxWidth(),
                        shape = ClayShape.Hero,
                        elevation = 24.dp,
                        glowAlpha = 0.36f,
                        contentPadding = PaddingValues(horizontal = 22.dp, vertical = 28.dp),
                    ) {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            ClayRewardBlob(emoji = victory.emoji, accent = ClayPalette.Peach, blobSize = 112.dp)
                            Spacer(Modifier.height(12.dp))
                            Text(
                                text = "CHALLENGE COMPLETE",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = ClayPalette.Peach.deep,
                                letterSpacing = 2.sp,
                            )
                            Text(
                                text = victory.title,
                                style = MaterialTheme.typography.headlineSmall,
                                color = ClayInk.Primary,
                                textAlign = TextAlign.Center,
                            )
                            Spacer(Modifier.height(10.dp))
                            Box(
                                modifier = Modifier
                                    .clip(ClayShape.Pill)
                                    .background(ClayPalette.Peach.soft)
                                    .padding(horizontal = 14.dp, vertical = 8.dp),
                            ) {
                                Text(
                                    text = "${victory.objectivesCompleted} of $objectivesTotal objectives cleared",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = ClayPalette.Peach.deep,
                                )
                            }
                            Spacer(Modifier.height(16.dp))
                            Text(
                                text = "+${victory.xpAwarded} XP",
                                style = MaterialTheme.typography.headlineMedium,
                                color = ClayPalette.Butter.deep,
                            )
                            Spacer(Modifier.height(20.dp))
                            ClayButton(
                                text = "Well earned",
                                onClick = onDismiss,
                                accent = ClayPalette.Peach,
                                modifier = Modifier.fillMaxWidth(),
                            )
                        }
                    }
                }
            }
        }
    }
}

/** Rounded gradient pill used inside overlays. */
@Composable
internal fun GradientPill(text: String, accent: ClayAccent, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .clip(ClayShape.Pill)
            .background(clayButtonBrush(accent))
            .padding(horizontal = 14.dp, vertical = 8.dp),
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium,
            color = Color.White,
            fontWeight = FontWeight.SemiBold,
        )
    }
}
