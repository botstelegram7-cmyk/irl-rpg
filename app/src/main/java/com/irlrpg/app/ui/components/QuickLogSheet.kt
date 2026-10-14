package com.irlrpg.app.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.irlrpg.app.domain.game.XpEngine
import com.irlrpg.app.domain.model.QuestWithProgress
import com.irlrpg.app.domain.model.TrackingKind
import com.irlrpg.app.domain.model.UnitKind
import com.irlrpg.app.ui.design.ClayButton
import com.irlrpg.app.ui.design.ClayButtonStyle
import com.irlrpg.app.ui.design.ClayChip
import com.irlrpg.app.ui.design.ClayIconTile
import com.irlrpg.app.ui.design.ClayProgressBar
import com.irlrpg.app.ui.design.ClayStepper
import com.irlrpg.app.ui.theme.ClayAccent
import com.irlrpg.app.ui.theme.ClayInk
import com.irlrpg.app.ui.theme.ClayPalette
import com.irlrpg.app.ui.theme.ClayShape

/**
 * Records real, user-entered progress. Nothing is auto-filled or guessed: the
 * stepper starts from a sensible step size for the unit and the primary button
 * states exactly what will be logged.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuickLogSheet(
    item: QuestWithProgress,
    accent: ClayAccent,
    onDismiss: () -> Unit,
    onLog: (Long) -> Unit,
) {
    val quest = item.quest
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val step = remember(quest.id) { stepFor(quest.unit, quest.targetAmount) }
    val initial = remember(quest.id) {
        when {
            quest.trackingKind == TrackingKind.Milestone.name -> 1
            item.remaining > 0 -> minOf(item.remaining, step.toLong()).coerceAtLeast(1L).toInt()
            else -> step
        }
    }
    var amount by remember(quest.id) { mutableStateOf(initial) }
    val perStepXp = XpEngine.perStepXp(
        TrackingKind.fromName(quest.trackingKind),
        quest.xpReward,
        quest.targetAmount,
    )

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = ClayPalette.SurfaceCream,
        shape = ClayShape.Hero,
        dragHandle = null,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 22.dp, vertical = 22.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                ClayIconTile(emoji = quest.emoji, accent = accent, size = 48.dp)
                Spacer(Modifier.width(14.dp))
                Column {
                    Text(
                        text = quest.title,
                        style = MaterialTheme.typography.titleMedium,
                        color = ClayInk.Primary,
                    )
                    Text(
                        text = "Log progress · ${item.progress.coerceAtMost(item.target)}/${item.target} done for this period",
                        style = MaterialTheme.typography.bodySmall,
                        color = ClayInk.Muted,
                    )
                }
            }

            Spacer(Modifier.height(18.dp))
            ClayProgressBar(progress = item.fraction, accent = accent, height = 11.dp)

            Spacer(Modifier.height(22.dp))
            Text(
                text = "How much did you do?",
                style = MaterialTheme.typography.labelMedium,
                color = ClayInk.Secondary,
            )
            Spacer(Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                ClayStepper(
                    value = amount,
                    onValueChange = { amount = it },
                    range = 1..9_999,
                    step = step,
                    accent = accent,
                    suffix = item.quest.let { q ->
                        if (UnitKind.fromName(q.unit) == UnitKind.Custom) q.customUnit ?: "unit"
                        else UnitKind.fromName(q.unit).short
                    },
                )
            }

            Spacer(Modifier.height(16.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                ClayChip(
                    text = "+$step",
                    selected = false,
                    onClick = { amount = (amount + step).coerceAtMost(9_999) },
                    accent = accent,
                )
                if (item.remaining > 0) {
                    ClayChip(
                        text = "Finish (${item.remaining})",
                        selected = false,
                        onClick = { amount = item.remaining.coerceAtMost(9_999L).toInt() },
                        accent = accent,
                    )
                }
                ClayChip(
                    text = "Reset",
                    selected = false,
                    onClick = { amount = initial },
                    accent = accent,
                )
            }

            Spacer(Modifier.height(18.dp))
            Text(
                text = "Earns about ${perStepXp * amount} XP · completion adds a bonus once",
                style = MaterialTheme.typography.bodySmall,
                color = ClayInk.Muted,
            )

            Spacer(Modifier.height(20.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                ClayButton(
                    text = "Cancel",
                    onClick = onDismiss,
                    accent = accent,
                    style = ClayButtonStyle.Soft,
                    modifier = Modifier.weight(1f),
                )
                ClayButton(
                    text = "Log $amount",
                    onClick = { onLog(amount.toLong()) },
                    accent = accent,
                    modifier = Modifier.weight(1.2f),
                )
            }
        }
    }
}

private fun stepFor(unitName: String, target: Long): Int {
    if (target <= 5L) return 1
    return when (UnitKind.fromName(unitName)) {
        UnitKind.Minutes -> if (target <= 20L) 5 else 10
        UnitKind.Hours -> 1
        UnitKind.Pages, UnitKind.Chapters -> if (target <= 10L) 1 else 5
        UnitKind.Words, UnitKind.Steps -> if (target <= 100L) 10 else 100
        UnitKind.Reps -> 5
        UnitKind.Kilometres -> 1
        UnitKind.Percent -> 10
        UnitKind.Points -> if (target <= 50L) 5 else 10
        UnitKind.Sessions -> 1
        UnitKind.Custom -> 1
    }
}
