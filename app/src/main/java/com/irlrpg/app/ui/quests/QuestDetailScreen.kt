package com.irlrpg.app.ui.quests

import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.irlrpg.app.core.DateX
import com.irlrpg.app.data.local.entity.QuestLogEntity
import com.irlrpg.app.domain.model.ScheduleKind
import com.irlrpg.app.domain.model.TrackingKind
import com.irlrpg.app.domain.model.UnitKind
import com.irlrpg.app.ui.common.accentFor
import com.irlrpg.app.ui.components.CelebrationOverlay
import com.irlrpg.app.ui.components.ClayConfirmDialog
import com.irlrpg.app.ui.components.ClayToast
import com.irlrpg.app.ui.design.ClayButton
import com.irlrpg.app.ui.design.ClayButtonStyle
import com.irlrpg.app.ui.design.ClayCard
import com.irlrpg.app.ui.design.ClayIconButton
import com.irlrpg.app.ui.design.ClayIconTile
import com.irlrpg.app.ui.design.ClayProgressBar
import com.irlrpg.app.ui.design.ClayScaffold
import com.irlrpg.app.ui.design.ClaySectionHeader
import com.irlrpg.app.ui.design.ClayStepper
import com.irlrpg.app.ui.design.ClayTopBar
import com.irlrpg.app.ui.theme.ClayInk
import com.irlrpg.app.ui.theme.ClayPalette
import com.irlrpg.app.ui.theme.ClayShape
import java.time.LocalDate

@Composable
fun QuestDetailScreen(
    viewModel: QuestDetailViewModel,
    onBack: () -> Unit,
    onEdit: (Long) -> Unit,
    onDeleted: () -> Unit,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val celebration by viewModel.celebration.collectAsStateWithLifecycle()
    val message by viewModel.message.collectAsStateWithLifecycle()
    var menuOpen by remember { mutableStateOf(false) }
    var confirmReset by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf(false) }

    LaunchedEffect(state.deleted) {
        if (state.deleted) onDeleted()
    }

    val quest = state.quest
    val accent = state.progress?.let { accentFor(it) } ?: ClayPalette.Lavender

    ClayScaffold(
        topBar = {
            ClayTopBar(
                title = quest?.title ?: "Quest",
                subtitle = quest?.let {
                    buildString {
                        append(state.progress?.category?.let { c -> "${c.emoji} ${c.name}" } ?: "🗂️ Uncategorised")
                        append(" · ")
                        append(ScheduleKind.fromName(it.scheduleKind).label)
                    }
                },
                onBack = onBack,
                accent = accent,
                actions = {
                    Box {
                        ClayIconButton(
                            icon = Icons.Rounded.MoreVert,
                            onClick = { menuOpen = true },
                            contentDescription = "Quest options",
                            accent = accent,
                            size = 42.dp,
                        )
                        DropdownMenu(
                            expanded = menuOpen,
                            onDismissRequest = { menuOpen = false },
                            containerColor = ClayPalette.SurfaceCream,
                            shape = ClayShape.Medium,
                        ) {
                            DropdownMenuItem(
                                text = { Text("Edit quest", style = MaterialTheme.typography.bodyMedium) },
                                onClick = { menuOpen = false; quest?.let { onEdit(it.id) } },
                            )
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        if (quest?.isActive == true) "Pause quest" else "Resume quest",
                                        style = MaterialTheme.typography.bodyMedium,
                                    )
                                },
                                onClick = { menuOpen = false; viewModel.toggleActive() },
                            )
                            DropdownMenuItem(
                                text = { Text("Reset progress", style = MaterialTheme.typography.bodyMedium) },
                                onClick = { menuOpen = false; confirmReset = true },
                            )
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        "Delete quest",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = ClayPalette.Blush.deep,
                                    )
                                },
                                onClick = { menuOpen = false; confirmDelete = true },
                            )
                        }
                    }
                },
            )
        },
    ) { padding ->
        if (quest == null) {
            Box(modifier = Modifier.fillMaxSize())
            return@ClayScaffold
        }
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = 20.dp,
                end = 20.dp,
                top = padding.calculateTopPadding() + 4.dp,
                bottom = padding.calculateBottomPadding() + 32.dp,
            ),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item {
                ClayCard(shape = ClayShape.Hero, elevation = 16.dp, glowAlpha = 0.26f, contentPadding = PaddingValues(20.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        ClayIconTile(
                            emoji = quest.emoji,
                            accent = if (state.progress?.isComplete == true) ClayPalette.Mint else accent,
                            size = 58.dp,
                            fontSize = 26.sp,
                        )
                        Spacer(Modifier.width(16.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = quest.title,
                                style = MaterialTheme.typography.titleMedium,
                                color = ClayInk.Primary,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                            )
                            Text(
                                text = "${quest.xpReward} XP · ${quest.attributeKind}",
                                style = MaterialTheme.typography.bodySmall,
                                color = ClayInk.Muted,
                            )
                        }
                    }
                    if (quest.notes != null) {
                        Spacer(Modifier.height(12.dp))
                        Text(
                            text = quest.notes.orEmpty(),
                            style = MaterialTheme.typography.bodyMedium,
                            color = ClayInk.Secondary,
                        )
                    }
                    Spacer(Modifier.height(16.dp))
                    val progress = state.progress
                    if (progress != null && quest.trackingKind != TrackingKind.Milestone.name) {
                        ClayProgressBar(progress = progress.fraction, accent = accent, height = 14.dp)
                        Spacer(Modifier.height(10.dp))
                        Text(
                            text = "${progress.progress.coerceAtMost(progress.target)} / ${progress.target} ${unitLabelOf(quest.customUnit, quest.unit)} ${state.periodLabel}",
                            style = MaterialTheme.typography.titleSmall,
                            color = ClayInk.Primary,
                        )
                    } else if (progress != null) {
                        Text(
                            text = if (progress.isComplete) "✅ Completed" else "⏳ Not completed yet",
                            style = MaterialTheme.typography.titleSmall,
                            color = ClayInk.Primary,
                        )
                    }
                    Spacer(Modifier.height(14.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        DetailStat("Completions", "${state.completions}", ClayPalette.Mint)
                        DetailStat("XP earned", "${state.xpFromQuest}", ClayPalette.Butter)
                        DetailStat("Entries", "${state.logs.size}", ClayPalette.Sky)
                    }
                }
            }

            item {
                ClayCard(shape = ClayShape.Card, elevation = 14.dp) {
                    ClaySectionHeader(
                        title = "Log progress",
                        subtitle = when {
                            state.progress?.isComplete == true ->
                                "Target reached — extra entries are saved but earn no XP this period"
                            quest.trackingKind == TrackingKind.Milestone.name ->
                                "Completing it once awards ${quest.xpReward} XP"
                            else -> "Remaining: ${state.progress?.remaining ?: 0} ${unitLabelOf(quest.customUnit, quest.unit)}"
                        },
                    )
                    Spacer(Modifier.height(16.dp))
                    var amount by remember(quest.id) {
                        mutableStateOf(
                            (state.progress?.remaining?.takeIf { it in 1..30L } ?: 1L).toInt()
                        )
                    }
                    if (quest.trackingKind == TrackingKind.Milestone.name) {
                        ClayButton(
                            text = if (state.progress?.isComplete == true) "Already completed" else "Mark complete",
                            onClick = { viewModel.log(1L) },
                            accent = ClayPalette.Mint,
                            fillWidth = true,
                            height = 52.dp,
                            enabled = state.progress?.isComplete != true,
                        )
                    } else {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            ClayStepper(
                                value = amount,
                                onValueChange = { amount = it },
                                range = 1..9_999,
                                step = stepForUnit(quest.unit, quest.targetAmount),
                                accent = accent,
                                suffix = shortUnit(quest.unit, quest.customUnit),
                            )
                        }
                        Spacer(Modifier.height(14.dp))
                        ClayButton(
                            text = "Log $amount ${shortUnit(quest.unit, quest.customUnit)}",
                            onClick = { viewModel.log(amount.toLong()) },
                            accent = accent,
                            fillWidth = true,
                            height = 50.dp,
                        )
                    }
                }
            }

            item {
                ClaySectionHeader(
                    title = "History",
                    subtitle = if (state.logs.isEmpty()) {
                        "Nothing recorded yet"
                    } else {
                        "${state.logs.size} recorded ${if (state.logs.size == 1) "entry" else "entries"} · newest first"
                    },
                )
            }

            if (state.logs.isEmpty()) {
                item {
                    ClayCard(shape = ClayShape.Card, elevation = 10.dp) {
                        Text(
                            text = "Every entry you log is stored with its date and XP, so analytics stay traceable.",
                            style = MaterialTheme.typography.bodySmall,
                            color = ClayInk.Muted,
                        )
                    }
                }
            } else {
                items(state.logs.take(30), key = { it.id }) { log ->
                    LogRow(
                        log = log,
                        accent = accent,
                        onUndo = { viewModel.undo(log.id) },
                    )
                }
            }
        }
    }

    if (confirmReset) {
        ClayConfirmDialog(
            title = "Reset progress?",
            message = "All recorded entries for this quest are removed and your XP total is corrected to match. This cannot be undone.",
            confirmLabel = "Reset progress",
            onConfirm = {
                viewModel.resetProgress()
                confirmReset = false
            },
            onDismiss = { confirmReset = false },
        )
    }

    if (confirmDelete) {
        ClayConfirmDialog(
            title = "Delete \"${quest?.title.orEmpty()}\"?",
            message = "The quest and its ${state.logs.size} recorded entries are removed, and player XP is corrected.",
            confirmLabel = "Delete quest",
            onConfirm = {
                viewModel.deleteQuest()
                confirmDelete = false
            },
            onDismiss = { confirmDelete = false },
        )
    }

    CelebrationOverlay(result = celebration, onDismiss = viewModel::dismissCelebration)
    ClayToast(message = message, onShown = viewModel::consumeMessage)
}

@Composable
private fun DetailStat(label: String, value: String, accent: com.irlrpg.app.ui.theme.ClayAccent) {
    Column(
        modifier = Modifier
            .clip(ClayShape.Small)
            .background(accent.soft.copy(alpha = 0.6f))
            .padding(horizontal = 12.dp, vertical = 9.dp),
    ) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleSmall,
            color = ClayInk.Primary,
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = ClayInk.Muted,
        )
    }
}

@Composable
private fun LogRow(
    log: QuestLogEntity,
    accent: com.irlrpg.app.ui.theme.ClayAccent,
    onUndo: () -> Unit,
) {
    ClayCard(
        shape = ClayShape.Medium,
        elevation = 9.dp,
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "${log.amount} ${log.unitLabel}",
                        style = MaterialTheme.typography.titleSmall,
                        color = ClayInk.Primary,
                    )
                    if (log.isCompletion) {
                        Spacer(Modifier.width(8.dp))
                        Box(
                            modifier = Modifier
                                .clip(ClayShape.Pill)
                                .background(ClayPalette.Mint.soft)
                                .padding(horizontal = 8.dp, vertical = 3.dp),
                        ) {
                            Text(
                                text = "target reached",
                                style = MaterialTheme.typography.labelSmall,
                                color = ClayPalette.Mint.deep,
                            )
                        }
                    }
                }
                Spacer(Modifier.height(2.dp))
                Text(
                    text = "${DateX.relativeLabel(LocalDate.ofEpochDay(log.epochDay))} · +${log.xpEarned} XP",
                    style = MaterialTheme.typography.labelSmall,
                    color = ClayInk.Muted,
                )
            }
            ClayButton(
                text = "Undo",
                onClick = onUndo,
                accent = accent,
                style = ClayButtonStyle.Plain,
                height = 36.dp,
            )
        }
    }
}

internal fun unitLabelOf(customUnit: String?, unitName: String): String =
    if (UnitKind.fromName(unitName) == UnitKind.Custom) customUnit ?: "unit" else UnitKind.fromName(unitName).label.lowercase()

internal fun shortUnit(unitName: String, customUnit: String?): String =
    if (UnitKind.fromName(unitName) == UnitKind.Custom) customUnit ?: "unit" else UnitKind.fromName(unitName).short

internal fun stepForUnit(unitName: String, target: Long): Int = when {
    target <= 5L -> 1
    UnitKind.fromName(unitName) == UnitKind.Minutes -> if (target <= 20L) 5 else 10
    UnitKind.fromName(unitName) == UnitKind.Pages -> if (target <= 10L) 1 else 5
    UnitKind.fromName(unitName) == UnitKind.Words -> if (target <= 100L) 10 else 100
    else -> 1
}
