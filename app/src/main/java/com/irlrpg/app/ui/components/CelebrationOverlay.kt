package com.irlrpg.app.ui.components

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.irlrpg.app.domain.game.LogResult
import com.irlrpg.app.ui.design.ClayButton
import com.irlrpg.app.ui.design.ClayButtonStyle
import com.irlrpg.app.ui.design.ClayCard
import com.irlrpg.app.ui.design.ClayProgressBar
import com.irlrpg.app.ui.design.ClayRewardBlob
import com.irlrpg.app.ui.theme.ClayInk
import com.irlrpg.app.ui.theme.ClayPalette
import com.irlrpg.app.ui.theme.ClayShape

/**
 * Reward moment after a check-in: the XP that was actually banked, the real
 * progress bar, and an honest note when a period's target was already reached
 * (progress is stored, but no extra XP is granted).
 */
@Composable
fun CelebrationOverlay(
    result: LogResult?,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var visible by remember(result) { mutableStateOf(result != null) }
    LaunchedEffect(result) {
        if (result != null) visible = true
    }
    if (result == null) return

    BackHandler { onDismiss() }

    val accent = when {
        result.levelUp -> ClayPalette.Butter
        result.completedNow -> ClayPalette.Mint
        else -> ClayPalette.Lavender
    }

    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(),
        exit = fadeOut(),
        modifier = modifier,
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0x553B3450))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onDismiss,
                ),
            contentAlignment = Alignment.Center,
        ) {
            AnimatedVisibility(
                visible = visible,
                enter = scaleIn(
                    initialScale = 0.72f,
                    animationSpec = spring(dampingRatio = 0.55f, stiffness = Spring.StiffnessMediumLow),
                ) + fadeIn(),
            ) {
                Box(modifier = Modifier.padding(horizontal = 26.dp)) {
                ClayCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = {},
                        ),
                    shape = ClayShape.Hero,
                    elevation = 22.dp,
                    contentPadding = PaddingValues(horizontal = 22.dp, vertical = 26.dp),
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        ClayRewardBlob(emoji = result.emoji, accent = accent)

                        Spacer(Modifier.height(14.dp))
                        Text(
                            text = if (result.xpGained > 0) "+${result.xpGained} XP" else "Progress saved",
                            style = MaterialTheme.typography.displaySmall,
                            color = accent.deep,
                        )
                        Text(
                            text = result.questTitle,
                            style = MaterialTheme.typography.titleMedium,
                            color = ClayInk.Primary,
                            textAlign = TextAlign.Center,
                        )

                        if (result.levelUp) {
                            Spacer(Modifier.height(12.dp))
                            CelebrationBadge(
                                text = "🎉 Level up · Level ${result.newLevel}",
                                accent = ClayPalette.Butter,
                            )
                        }
                        if (result.completedNow) {
                            Spacer(Modifier.height(12.dp))
                            CelebrationBadge(
                                text = "✅ Target reached · ${result.target} ${result.unitLabel}",
                                accent = ClayPalette.Mint,
                            )
                        }
                        if (result.alreadyComplete) {
                            Spacer(Modifier.height(12.dp))
                            CelebrationBadge(
                                text = "Already complete — recorded, no extra XP",
                                accent = ClayPalette.Sky,
                            )
                        }

                        Spacer(Modifier.height(18.dp))
                        ClayProgressBar(
                            progress = result.progressFraction,
                            accent = accent,
                            height = 12.dp,
                        )
                        Spacer(Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Text(
                                text = "${result.progressAfter.coerceAtMost(result.target)} / ${result.target} ${result.unitLabel}",
                                style = MaterialTheme.typography.bodySmall,
                                color = ClayInk.Secondary,
                            )
                            Text(
                                text = "${result.totalXp} XP total",
                                style = MaterialTheme.typography.bodySmall,
                                color = ClayInk.Muted,
                            )
                        }

                        Spacer(Modifier.height(22.dp))
                        ClayButton(
                            text = "Nice!",
                            onClick = onDismiss,
                            accent = accent,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }
                }
            }
        }
    }
}

@Composable
private fun CelebrationBadge(text: String, accent: com.irlrpg.app.ui.theme.ClayAccent) {
    Box(
        modifier = Modifier
            .clip(ClayShape.Pill)
            .background(accent.soft)
            .padding(horizontal = 14.dp, vertical = 7.dp),
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            color = accent.deep,
            textAlign = TextAlign.Center,
        )
    }
}
