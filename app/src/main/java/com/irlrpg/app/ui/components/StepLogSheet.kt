package com.irlrpg.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.irlrpg.app.data.local.entity.QuestStepEntity
import com.irlrpg.app.domain.model.QuestWithProgress
import com.irlrpg.app.ui.design.ClayButton
import com.irlrpg.app.ui.design.ClayButtonStyle
import com.irlrpg.app.ui.design.ClayProgressBar
import com.irlrpg.app.ui.design.ClaySectionHeader
import com.irlrpg.app.ui.theme.ClayInk
import com.irlrpg.app.ui.theme.ClayPalette
import com.irlrpg.app.ui.theme.ClayShape

/**
 * Check-in sheet for a multi-step quest.
 *
 * Only the *current* step is offered, so progress stays ordered and there is no
 * way to stack progress on a later step to farm XP faster.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StepLogSheet(
    item: QuestWithProgress,
    steps: List<QuestStepEntity>,
    onDismiss: () -> Unit,
    onLog: (stepId: Long, amount: Long) -> Unit,
) {
    val required = steps.filter { it.isRequired }.ifEmpty { steps }
    val currentStep = required.firstOrNull { step ->
        !item.completedStepIds.contains(step.id)
    }
    val accent = ClayPalette.Mint

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = ClayPalette.SurfaceCream,
        shape = ClayShape.Hero,
        dragHandle = null,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 600.dp)
                .verticalScroll(rememberScrollState())
                .navigationBarsPadding()
                .padding(horizontal = 20.dp, vertical = 22.dp),
        ) {
            ClaySectionHeader(
                title = "${item.quest.emoji} ${item.quest.title}",
                subtitle = "${item.stepsCompleted} of ${item.stepsTotal} steps finished ${com.irlrpg.app.data.repository.QuestProgress.periodLabel(item.quest)}",
            )
            Spacer(Modifier.height(12.dp))
            ClayProgressBar(progress = item.fraction, accent = accent, height = 12.dp)

            Spacer(Modifier.height(18.dp))
            required.forEachIndexed { index, step ->
                val done = item.completedStepIds.contains(step.id)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .clip(ClayShape.Pill)
                            .background(if (done) accent.base else ClayPalette.SurfaceMuted),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = if (done) "✓" else "${index + 1}",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = if (done) ClayInk.OnAccent else ClayInk.Muted,
                        )
                    }
                    Spacer(Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = step.title,
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (done) ClayInk.Muted else ClayInk.Primary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Text(
                            text = buildString {
                                append("target ${step.targetAmount}")
                                if (!step.isRequired) append(" · optional")
                                if (step.id == currentStep?.id) append(" · up next")
                            },
                            style = MaterialTheme.typography.labelSmall,
                            color = if (step.id == currentStep?.id) accent.deep else ClayInk.Faint,
                        )
                    }
                    if (!done && step.id == currentStep?.id) {
                        ClayButton(
                            text = "Log ${step.targetAmount}",
                            onClick = { onLog(step.id, step.targetAmount.coerceAtLeast(1L)) },
                            accent = accent,
                            style = ClayButtonStyle.Soft,
                            height = 38.dp,
                        )
                    }
                }
            }

            if (currentStep == null) {
                Spacer(Modifier.height(10.dp))
                Text(
                    text = "Every step is done — the completion bonus is already recorded.",
                    style = MaterialTheme.typography.bodySmall,
                    color = accent.deep,
                )
            }

            Spacer(Modifier.height(18.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                ClayButton(
                    text = "Close",
                    onClick = onDismiss,
                    accent = ClayPalette.Lavender,
                    style = ClayButtonStyle.Soft,
                    modifier = Modifier.weight(1f),
                )
                if (currentStep != null) {
                    ClayButton(
                        text = "Log a partial amount",
                        onClick = { onLog(currentStep.id, 1L) },
                        accent = ClayPalette.Sky,
                        modifier = Modifier.weight(1.2f),
                    )
                }
            }
            Spacer(Modifier.height(6.dp))
            Text(
                text = "Steps are logged in order, so a later step cannot be farmed before the earlier ones are done.",
                style = MaterialTheme.typography.labelSmall,
                color = ClayInk.Faint,
            )
        }
    }
}
