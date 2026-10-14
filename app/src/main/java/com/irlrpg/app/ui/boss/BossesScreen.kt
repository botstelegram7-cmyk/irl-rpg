package com.irlrpg.app.ui.boss

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.irlrpg.app.domain.model.BossProgress
import com.irlrpg.app.domain.model.BossTemplate
import com.irlrpg.app.ui.components.BossVictoryOverlay
import com.irlrpg.app.ui.components.ClayConfirmDialog
import com.irlrpg.app.ui.components.ClayToast
import com.irlrpg.app.ui.design.ClayButton
import com.irlrpg.app.ui.design.ClayButtonStyle
import com.irlrpg.app.ui.design.ClayCard
import com.irlrpg.app.ui.design.ClayChip
import com.irlrpg.app.ui.design.ClayEmptyState
import com.irlrpg.app.ui.design.ClayIconButton
import com.irlrpg.app.ui.design.ClayProgressBar
import com.irlrpg.app.ui.design.ClayScaffold
import com.irlrpg.app.ui.design.ClaySectionHeader
import com.irlrpg.app.ui.design.ClayStepper
import com.irlrpg.app.ui.design.ClayTextField
import com.irlrpg.app.ui.quests.EmojiRow
import com.irlrpg.app.ui.theme.ClayInk
import com.irlrpg.app.ui.theme.ClayPalette
import com.irlrpg.app.ui.theme.ClayShape

/**
 * Boss Challenges board.
 *
 * A "boss" is a significant real-life goal with several objectives. There is no
 * combat: you make progress by logging the quests the objectives are linked to,
 * or by ticking a manual objective. Rewards are paid once, when every required
 * objective is complete, and only after the engine verifies it against the log.
 */
@Composable
fun BossesScreen(viewModel: BossViewModel) {
    val board by viewModel.board.collectAsStateWithLifecycle()
    val progress by viewModel.progress.collectAsStateWithLifecycle()
    val victory by viewModel.victory.collectAsStateWithLifecycle()
    val draft by viewModel.draft.collectAsStateWithLifecycle()
    val picker by viewModel.templatePicker.collectAsStateWithLifecycle()
    val message by viewModel.message.collectAsStateWithLifecycle()

    var confirmDelete by remember { mutableStateOf<Long?>(null) }
    var showFinished by remember { mutableStateOf(false) }

    val active = progress.filter { it.boss.status == "ACTIVE" }
    val completed = progress.filter { it.boss.status == "COMPLETED" }
    val archived = progress.filter { it.boss.status == "ARCHIVED" }

    ClayScaffold(
        topBar = {
            com.irlrpg.app.ui.design.ClayTopBar(
                title = "Challenges",
                subtitle = if (active.isEmpty()) {
                    "Big goals, broken into objectives"
                } else {
                    "${active.size} in progress · ${completed.size} completed"
                },
                accent = ClayPalette.Peach,
            )
        },
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(top = padding.calculateTopPadding())) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 120.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                item {
                    ClayCard(
                        shape = ClayShape.Hero,
                        elevation = 16.dp,
                        glowAlpha = 0.24f,
                        contentPadding = PaddingValues(18.dp),
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(text = "🏔️", fontSize = 24.sp)
                            Spacer(Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Set a challenge",
                                    style = MaterialTheme.typography.titleSmall,
                                    color = ClayInk.Primary,
                                )
                                Text(
                                    text = "Pick a ready-made plan or build one from your own objectives.",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = ClayInk.Muted,
                                )
                            }
                        }
                        Spacer(Modifier.height(14.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            ClayButton(
                                text = "Build custom",
                                onClick = viewModel::openBuilder,
                                accent = ClayPalette.Peach,
                                modifier = Modifier.weight(1f),
                            )
                            ClayButton(
                                text = "Templates",
                                onClick = { viewModel.startTemplate(viewModel.templates().first()) },
                                accent = ClayPalette.Lavender,
                                style = ClayButtonStyle.Soft,
                                modifier = Modifier.weight(1f),
                            )
                        }
                    }
                }

                if (active.isEmpty() && completed.isEmpty()) {
                    item {
                        ClayEmptyState(
                            emoji = "🏔️",
                            title = "No challenges yet",
                            message = "A challenge turns a big goal into countable objectives, then pays a single reward when every required part is done.",
                            actionLabel = "Start from a template",
                            onAction = { viewModel.startTemplate(viewModel.templates().first()) },
                        )
                    }
                }

                items(active, key = { it.boss.id }) { item ->
                    BossCard(
                        progress = item,
                        onLogManual = viewModel::logObjective,
                        onArchive = viewModel::archive,
                        onDelete = { confirmDelete = item.boss.id },
                        onReactivate = viewModel::reactivate,
                    )
                }

                if (completed.isNotEmpty()) {
                    item {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            ClaySectionHeader(
                                title = "Completed",
                                subtitle = "${completed.size} conquered",
                                modifier = Modifier.weight(1f),
                            )
                            ClayChip(
                                text = if (showFinished) "Hide" else "Show",
                                selected = showFinished,
                                onClick = { showFinished = !showFinished },
                                accent = ClayPalette.Butter,
                            )
                        }
                    }
                    if (showFinished) {
                        items(completed, key = { it.boss.id }) { item ->
                            BossCard(
                                progress = item,
                                onLogManual = viewModel::logObjective,
                                onArchive = viewModel::archive,
                                onDelete = { confirmDelete = item.boss.id },
                                onReactivate = viewModel::reactivate,
                            )
                        }
                    }
                }

                if (archived.isNotEmpty()) {
                    item {
                        ClaySectionHeader(
                            title = "Archived",
                            subtitle = "Kept for your history — reactivate any time",
                        )
                    }
                    items(archived, key = { it.boss.id }) { item ->
                        BossCard(
                            progress = item,
                            onLogManual = viewModel::logObjective,
                            onArchive = viewModel::archive,
                            onDelete = { confirmDelete = item.boss.id },
                            onReactivate = viewModel::reactivate,
                        )
                    }
                }
            }

            Box(modifier = Modifier.align(Alignment.BottomEnd).padding(end = 20.dp, bottom = 104.dp)) {
                ClayIconButton(
                    icon = Icons.Rounded.Add,
                    onClick = viewModel::openBuilder,
                    contentDescription = "New challenge",
                    accent = ClayPalette.Peach,
                    size = 56.dp,
                )
            }

            victory?.let { (win, total) ->
                BossVictoryOverlay(
                    victory = win,
                    objectivesTotal = total,
                    onDismiss = viewModel::dismissVictory,
                )
            }

            ClayToast(message = message, onShown = viewModel::consumeMessage, bottomPadding = 112.dp)
        }
    }

    picker?.let { template ->
        TemplateSheet(
            template = template,
            onDismiss = viewModel::dismissTemplate,
            onStart = viewModel::confirmTemplate,
            onPick = viewModel::startTemplate,
        )
    }

    draft?.let { current ->
        BuilderSheet(
            state = current,
            linkableQuests = board.linkableQuests,
            onChange = viewModel::updateDraft,
            onAddRow = viewModel::addObjectiveRow,
            onRemoveRow = viewModel::removeObjectiveRow,
            onDismiss = viewModel::closeBuilder,
            onSave = viewModel::saveDraft,
        )
    }

    confirmDelete?.let { id ->
        ClayConfirmDialog(
            title = "Delete this challenge?",
            message = "The challenge and its objectives are removed. XP already earned stays in your history.",
            confirmLabel = "Delete",
            onConfirm = {
                viewModel.delete(id)
                confirmDelete = null
            },
            onDismiss = { confirmDelete = null },
        )
    }
}

@Composable
private fun BossCard(
    progress: BossProgress,
    onLogManual: (Long, Long) -> Unit,
    onArchive: (Long) -> Unit,
    onDelete: (Long) -> Unit,
    onReactivate: (Long) -> Unit,
) {
    val boss = progress.boss
    val accent = ClayPalette.accentById(boss.accentId)
    val fraction by animateFloatAsState(
        targetValue = progress.fraction,
        animationSpec = tween(600),
        label = "bossProgress",
    )
    var expanded by remember { mutableStateOf(progress.isCompleted) }

    ClayCard(
        shape = ClayShape.Card,
        elevation = if (progress.isCompleted) 10.dp else 16.dp,
        glowAlpha = if (progress.isCompleted) 0.12f else 0.2f,
        contentPadding = PaddingValues(18.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(ClayShape.Medium)
                    .background(
                        Brush.linearGradient(
                            listOf(accent.soft, accent.soft.copy(alpha = 0.6f)),
                        )
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Text(text = boss.emoji, fontSize = 26.sp)
            }
            Spacer(Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = boss.title,
                    style = MaterialTheme.typography.titleSmall,
                    color = ClayInk.Primary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = buildString {
                        append("${progress.completedRequired} of ${progress.required.size} required objectives")
                        progress.daysLeft?.let { days ->
                            append(" · ")
                            append(
                                when {
                                    progress.isCompleted -> "completed"
                                    days < 0 -> "${-days}d overdue"
                                    days == 0L -> "due today"
                                    else -> "${days}d left"
                                }
                            )
                        }
                    },
                    style = MaterialTheme.typography.labelSmall,
                    color = if (progress.isOverdue) ClayPalette.Blush.deep else ClayInk.Muted,
                )
            }
            Text(
                text = "+${boss.rewardXp} XP",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = ClayPalette.Butter.deep,
            )
        }

        Spacer(Modifier.height(12.dp))
        ClayProgressBar(progress = fraction, accent = accent, height = 12.dp)
        Spacer(Modifier.height(8.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "${(fraction * 100).toInt()}% complete",
                style = MaterialTheme.typography.labelSmall,
                color = ClayInk.Secondary,
                modifier = Modifier.weight(1f),
            )
            ClayChip(
                text = if (expanded) "Hide objectives" else "Objectives",
                selected = expanded,
                onClick = { expanded = !expanded },
                accent = accent,
            )
        }

        AnimatedVisibility(visible = expanded, enter = fadeIn(), exit = fadeOut()) {
            Column {
                Spacer(Modifier.height(10.dp))
                progress.objectives.forEach { objective ->
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Box(
                            modifier = Modifier
                                .size(22.dp)
                                .clip(ClayShape.Pill)
                                .background(if (objective.isComplete) accent.base else ClayPalette.SurfaceMuted),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                text = if (objective.isComplete) "✓" else "",
                                style = MaterialTheme.typography.labelSmall,
                                color = ClayInk.OnAccent,
                            )
                        }
                        Spacer(Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = objective.objective.title,
                                style = MaterialTheme.typography.bodySmall,
                                color = ClayInk.Primary,
                            )
                            Text(
                                text = buildString {
                                    append("${objective.progress.coerceAtMost(objective.target)} / ${objective.target}")
                                    if (!objective.objective.isRequired) append(" · optional")
                                    objective.linkedQuestTitle?.let { append(" · from \"$it\"") }
                                    if (objective.objective.metric == "MANUAL") append(" · manual")
                                },
                                style = MaterialTheme.typography.labelSmall,
                                color = ClayInk.Faint,
                            )
                        }
                        if (objective.objective.metric == "MANUAL" && !objective.isComplete) {
                            ClayButton(
                                text = "+1",
                                onClick = { onLogManual(objective.objective.id, 1L) },
                                accent = accent,
                                style = ClayButtonStyle.Soft,
                                height = 38.dp,
                            )
                        }
                    }
                }
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (progress.isCompleted || progress.boss.status == "ARCHIVED") {
                        ClayButton(
                            text = "Reactivate",
                            onClick = { onReactivate(boss.id) },
                            accent = ClayPalette.Mint,
                            style = ClayButtonStyle.Soft,
                            height = 38.dp,
                        )
                    } else {
                        ClayButton(
                            text = "Archive",
                            onClick = { onArchive(boss.id) },
                            accent = ClayPalette.Sky,
                            style = ClayButtonStyle.Soft,
                            height = 38.dp,
                        )
                    }
                    ClayButton(
                        text = "Delete",
                        onClick = { onDelete(boss.id) },
                        accent = ClayPalette.Blush,
                        style = ClayButtonStyle.Soft,
                        height = 38.dp,
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TemplateSheet(
    template: BossTemplate,
    onDismiss: () -> Unit,
    onStart: (java.time.LocalDate) -> Unit,
    onPick: (BossTemplate) -> Unit,
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = ClayPalette.SurfaceCream,
        shape = ClayShape.Hero,
        dragHandle = null,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 620.dp)
                .verticalScroll(rememberScrollState())
                .navigationBarsPadding()
                .padding(horizontal = 20.dp, vertical = 22.dp),
        ) {
            ClaySectionHeader(
                title = "Challenge templates",
                subtitle = "Starting points you can rename, re-target and re-link afterwards",
            )
            Spacer(Modifier.height(12.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                items(BossTemplatePresets, key = { it.code }) { preset ->
                    ClayChip(
                        text = preset.title,
                        selected = preset.code == template.code,
                        onClick = { onPick(preset) },
                        accent = ClayPalette.accentById(preset.accentId),
                        leadingEmoji = preset.emoji,
                    )
                }
            }
            Spacer(Modifier.height(16.dp))
            ClayCard(shape = ClayShape.Card, elevation = 14.dp, contentPadding = PaddingValues(18.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(text = template.emoji, fontSize = 28.sp)
                    Spacer(Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = template.title,
                            style = MaterialTheme.typography.titleSmall,
                            color = ClayInk.Primary,
                        )
                        Text(
                            text = template.description,
                            style = MaterialTheme.typography.labelSmall,
                            color = ClayInk.Muted,
                        )
                    }
                }
                Spacer(Modifier.height(14.dp))
                template.objectiveTitles.forEach { (title, target) ->
                    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                        Text(text = "•", color = ClayPalette.accentById(template.accentId).deep)
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = "$title — target $target",
                            style = MaterialTheme.typography.bodySmall,
                            color = ClayInk.Secondary,
                        )
                    }
                }
                Spacer(Modifier.height(14.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "Reward",
                        style = MaterialTheme.typography.labelMedium,
                        color = ClayInk.Secondary,
                        modifier = Modifier.weight(1f),
                    )
                    Text(
                        text = "${template.rewardXp} XP",
                        style = MaterialTheme.typography.labelLarge,
                        color = ClayPalette.Butter.deep,
                    )
                }
                Text(
                    text = "Suggested length: ${template.durationDays} days · paid once, when every required objective is complete",
                    style = MaterialTheme.typography.labelSmall,
                    color = ClayInk.Faint,
                )
            }
            Spacer(Modifier.height(18.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                ClayButton(
                    text = "Cancel",
                    onClick = onDismiss,
                    accent = ClayPalette.Lavender,
                    style = ClayButtonStyle.Soft,
                    modifier = Modifier.weight(1f),
                )
                ClayButton(
                    text = "Add challenge",
                    onClick = {
                        onStart(java.time.LocalDate.now().plusDays(template.durationDays.toLong()))
                    },
                    accent = ClayPalette.accentById(template.accentId),
                    modifier = Modifier.weight(1.2f),
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BuilderSheet(
    state: BossDraft,
    linkableQuests: List<Pair<Long, String>>,
    onChange: ((BossDraft) -> BossDraft) -> Unit,
    onAddRow: () -> Unit,
    onRemoveRow: (Int) -> Unit,
    onDismiss: () -> Unit,
    onSave: () -> Unit,
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = ClayPalette.SurfaceCream,
        shape = ClayShape.Hero,
        dragHandle = null,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 680.dp)
                .verticalScroll(rememberScrollState())
                .navigationBarsPadding()
                .padding(horizontal = 20.dp, vertical = 22.dp),
        ) {
            ClaySectionHeader(
                title = "New challenge",
                subtitle = "Required objectives decide completion. Optional ones just add a nudge.",
            )
            Spacer(Modifier.height(14.dp))
            ClayTextField(
                value = state.title,
                onValueChange = { value -> onChange { it.copy(title = value, error = null) } },
                label = "Challenge title",
                placeholder = "e.g. Finish the certification",
            )
            Spacer(Modifier.height(12.dp))
            ClayTextField(
                value = state.description,
                onValueChange = { value -> onChange { it.copy(description = value) } },
                label = "Why it matters (optional)",
                placeholder = "The reason you are taking this on",
                singleLine = false,
                maxLines = 3,
            )
            Spacer(Modifier.height(12.dp))
            EmojiRow(selected = state.emoji, onSelect = { emoji -> onChange { it.copy(emoji = emoji) } })

            Spacer(Modifier.height(16.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "Length",
                    style = MaterialTheme.typography.labelMedium,
                    color = ClayInk.Secondary,
                    modifier = Modifier.weight(1f),
                )
                ClayStepper(
                    value = state.durationDays,
                    onValueChange = { days -> onChange { it.copy(durationDays = days) } },
                    range = 1..730,
                    step = 5,
                    accent = ClayPalette.Peach,
                    suffix = "days",
                )
            }
            Spacer(Modifier.height(10.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "Reward",
                    style = MaterialTheme.typography.labelMedium,
                    color = ClayInk.Secondary,
                    modifier = Modifier.weight(1f),
                )
                ClayStepper(
                    value = state.rewardXp,
                    onValueChange = { xp -> onChange { it.copy(rewardXp = xp) } },
                    range = 50..5_000,
                    step = 50,
                    accent = ClayPalette.Butter,
                    suffix = "XP",
                )
            }

            Spacer(Modifier.height(18.dp))
            ClaySectionHeader(
                title = "Objectives",
                subtitle = "Link an objective to a quest to fill it automatically from your logs",
            )
            Spacer(Modifier.height(10.dp))
            state.objectives.forEachIndexed { index, objective ->
                ClayCard(shape = ClayShape.Medium, elevation = 10.dp, contentPadding = PaddingValues(14.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "Objective ${index + 1}",
                            style = MaterialTheme.typography.labelSmall,
                            color = ClayInk.Muted,
                            modifier = Modifier.weight(1f),
                        )
                        if (state.objectives.size > 1) {
                            ClayButton(
                                text = "Remove",
                                onClick = { onRemoveRow(index) },
                                accent = ClayPalette.Blush,
                                style = ClayButtonStyle.Soft,
                                height = 36.dp,
                            )
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    ClayTextField(
                        value = objective.title,
                        onValueChange = { value ->
                            onChange { draft ->
                                draft.copy(
                                    objectives = draft.objectives.mapIndexed { i, item ->
                                        if (i == index) item.copy(title = value) else item
                                    },
                                    error = null,
                                )
                            }
                        },
                        label = "What must be true",
                        placeholder = "e.g. Complete 24 practice sessions",
                    )
                    Spacer(Modifier.height(10.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "Target",
                            style = MaterialTheme.typography.labelSmall,
                            color = ClayInk.Secondary,
                            modifier = Modifier.width(56.dp),
                        )
                        ClayTextField(
                            value = objective.target,
                            onValueChange = { value ->
                                onChange { draft ->
                                    draft.copy(
                                        objectives = draft.objectives.mapIndexed { i, item ->
                                            if (i == index) item.copy(target = value.filter { it.isDigit() }) else item
                                        }
                                    )
                                }
                            },
                            label = "",
                            placeholder = "1",
                            modifier = Modifier.width(96.dp),
                        )
                    }
                    Spacer(Modifier.height(10.dp))
                    Text(
                        text = "Counts from",
                        style = MaterialTheme.typography.labelSmall,
                        color = ClayInk.Secondary,
                    )
                    Spacer(Modifier.height(6.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        item {
                            ClayChip(
                                text = "Manual ticks",
                                selected = objective.questId == null,
                                onClick = {
                                    onChange { draft ->
                                        draft.copy(
                                            objectives = draft.objectives.mapIndexed { i, item ->
                                                if (i == index) item.copy(questId = null) else item
                                            }
                                        )
                                    }
                                },
                                accent = ClayPalette.Sky,
                            )
                        }
                        items(linkableQuests, key = { it.first }) { (questId, title) ->
                            ClayChip(
                                text = title,
                                selected = objective.questId == questId,
                                onClick = {
                                    onChange { draft ->
                                        draft.copy(
                                            objectives = draft.objectives.mapIndexed { i, item ->
                                                if (i == index) item.copy(questId = questId) else item
                                            }
                                        )
                                    }
                                },
                                accent = ClayPalette.Mint,
                                leadingEmoji = "🎯",
                            )
                        }
                    }
                }
                Spacer(Modifier.height(10.dp))
            }

            ClayButton(
                text = "Add objective",
                onClick = onAddRow,
                accent = ClayPalette.Lavender,
                style = ClayButtonStyle.Soft,
                modifier = Modifier.fillMaxWidth(),
            )

            if (state.error != null) {
                Spacer(Modifier.height(12.dp))
                Text(
                    text = state.error.orEmpty(),
                    style = MaterialTheme.typography.labelMedium,
                    color = ClayPalette.Blush.deep,
                    textAlign = TextAlign.Start,
                )
            }

            Spacer(Modifier.height(18.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                ClayButton(
                    text = "Cancel",
                    onClick = onDismiss,
                    accent = ClayPalette.Lavender,
                    style = ClayButtonStyle.Soft,
                    modifier = Modifier.weight(1f),
                )
                ClayButton(
                    text = "Create challenge",
                    onClick = onSave,
                    accent = ClayPalette.Peach,
                    modifier = Modifier.weight(1.3f),
                )
            }
        }
    }
}

/** Templates are read from the domain catalogue; shown as chips in the sheet. */
private val BossTemplatePresets: List<BossTemplate>
    get() = com.irlrpg.app.domain.game.BossTemplates.all
