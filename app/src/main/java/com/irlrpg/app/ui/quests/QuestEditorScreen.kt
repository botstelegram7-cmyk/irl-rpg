package com.irlrpg.app.ui.quests

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import androidx.compose.material3.Icon
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.irlrpg.app.domain.game.XpEngine
import com.irlrpg.app.domain.model.QuestDifficulty
import com.irlrpg.app.domain.model.QuestKind
import com.irlrpg.app.domain.model.ScheduleKind
import com.irlrpg.app.domain.model.TrackingKind
import com.irlrpg.app.domain.model.UnitKind
import com.irlrpg.app.ui.design.ClayButton
import com.irlrpg.app.ui.design.ClayButtonStyle
import com.irlrpg.app.ui.design.ClayCard
import com.irlrpg.app.ui.design.ClayChip
import com.irlrpg.app.ui.design.ClayIconButton
import com.irlrpg.app.ui.design.ClayPanel
import com.irlrpg.app.ui.design.ClayScaffold
import com.irlrpg.app.ui.design.ClaySectionHeader
import com.irlrpg.app.ui.design.ClaySectionTitle
import com.irlrpg.app.ui.design.ClayStepper
import com.irlrpg.app.ui.design.ClayTextField
import com.irlrpg.app.ui.design.ClayTopBar
import com.irlrpg.app.ui.theme.ClayInk
import com.irlrpg.app.ui.theme.ClayPalette
import com.irlrpg.app.ui.theme.ClayShape

@Composable
fun QuestEditorScreen(
    viewModel: QuestEditorViewModel,
    onBack: () -> Unit,
    onSaved: () -> Unit,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(state.saved) {
        if (state.saved) onSaved()
    }

    // Section expand states - Basic + Tracking always open, others collapsible
    var scheduleExpanded by remember { mutableStateOf(true) }
    var goalExpanded by remember { mutableStateOf(true) }
    var difficultyExpanded by remember { mutableStateOf(false) }
    var rewardExpanded by remember { mutableStateOf(true) }

    ClayScaffold(
        topBar = {
            ClayTopBar(
                title = if (state.editing) "Edit quest" else "New quest",
                subtitle = "You decide exactly what counts as progress",
                onBack = onBack,
                accent = ClayPalette.Lavender,
            )
        },
        bottomBar = {
            QuestEditorBottomBar(
                state = state,
                onSave = viewModel::save,
                onCancel = onBack,
            )
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .imePadding(),
            contentPadding = PaddingValues(
                start = 16.dp,
                end = 16.dp,
                top = padding.calculateTopPadding() + 4.dp,
                bottom = padding.calculateBottomPadding() + 16.dp,
            ),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            // ── 1. Basic Information (always visible) ───────────────────────
            item {
                ClayCard(shape = ClayShape.Card, elevation = 12.dp, glowAlpha = 0.18f) {
                    EditorSectionTitle(number = "1", title = "Basic information", subtitle = "What are you working on?")
                    Spacer(Modifier.height(14.dp))
                    Text(
                        text = "Icon",
                        style = MaterialTheme.typography.labelMedium,
                        color = ClayInk.Secondary,
                        modifier = Modifier.padding(start = 4.dp, bottom = 8.dp),
                    )
                    EmojiRow(selected = state.emoji, onSelect = viewModel::setEmoji)
                    Spacer(Modifier.height(14.dp))
                    ClayTextField(
                        value = state.title,
                        onValueChange = viewModel::setTitle,
                        label = "Quest title",
                        placeholder = "e.g. Read a novel · Practise scales · Ship a feature",
                    )
                    Spacer(Modifier.height(12.dp))
                    ClayTextField(
                        value = state.description,
                        onValueChange = viewModel::setDescription,
                        label = "Description (optional)",
                        placeholder = "What exactly counts, and what does done look like?",
                        singleLine = false,
                        maxLines = 3,
                    )
                    Spacer(Modifier.height(14.dp))
                    Text(
                        text = "Category",
                        style = MaterialTheme.typography.labelMedium,
                        color = ClayInk.Secondary,
                        modifier = Modifier.padding(start = 4.dp, bottom = 8.dp),
                    )
                    CategoryChips(
                        categories = state.categories,
                        selectedId = state.categoryId,
                        onSelect = viewModel::setCategory,
                    )
                }
            }

            // ── 2. Tracking (always visible) ────────────────────────────────
            item {
                ClayCard(shape = ClayShape.Card, elevation = 12.dp, glowAlpha = 0.18f) {
                    EditorSectionTitle(number = "2", title = "Tracking", subtitle = state.trackingKind.helper)
                    Spacer(Modifier.height(12.dp))
                    WrappingChips(
                        items = TrackingKind.entries.toList(),
                        selected = state.trackingKind,
                        onSelect = viewModel::setTrackingKind,
                        label = { it.label },
                        emoji = { it.emoji },
                        accent = ClayPalette.Sky,
                    )

                    if (state.trackingKind != TrackingKind.Milestone) {
                        Spacer(Modifier.height(16.dp))
                        Text(
                            text = "Target per period",
                            style = MaterialTheme.typography.labelMedium,
                            color = ClayInk.Secondary,
                        )
                        Spacer(Modifier.height(8.dp))
                        // Responsive: stepper + exact field adapt to width
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            ClayStepper(
                                value = (state.targetText.toIntOrNull() ?: 1).coerceAtLeast(1),
                                onValueChange = { viewModel.setTargetText(it.toString()) },
                                range = 1..9_999,
                                step = targetStep(state.targetText.toIntOrNull() ?: 1, state.unit),
                                accent = ClayPalette.Sky,
                                suffix = state.unitLabel.take(12),
                            )
                            Spacer(Modifier.width(10.dp))
                            ClayTextField(
                                value = state.targetText,
                                onValueChange = viewModel::setTargetText,
                                label = "Exact",
                                modifier = Modifier.weight(1f),
                                keyboardType = KeyboardType.Number,
                                accent = ClayPalette.Sky,
                            )
                        }

                        Spacer(Modifier.height(14.dp))
                        Text(
                            text = "Unit",
                            style = MaterialTheme.typography.labelMedium,
                            color = ClayInk.Secondary,
                            modifier = Modifier.padding(start = 4.dp, bottom = 8.dp),
                        )
                        WrappingChips(
                            items = UnitKind.entries.toList(),
                            selected = state.unit,
                            onSelect = viewModel::setUnit,
                            label = { it.label },
                            accent = ClayPalette.Mint,
                        )
                        if (state.unit == UnitKind.Custom) {
                            Spacer(Modifier.height(12.dp))
                            ClayTextField(
                                value = state.customUnit,
                                onValueChange = viewModel::setCustomUnit,
                                label = "Your unit name",
                                placeholder = "e.g. songs, sketches, problems",
                                accent = ClayPalette.Mint,
                            )
                        }
                    } else {
                        Spacer(Modifier.height(12.dp))
                        ClayPanel(
                            tint = ClayPalette.SurfaceMuted.copy(alpha = 0.6f),
                            contentPadding = PaddingValues(12.dp),
                        ) {
                            Text(
                                text = "Milestone quests are finished once — perfect for \u201Cread this novel\u201D, \u201Cship this project\u201D or \u201Cpass this level\u201D.",
                                style = MaterialTheme.typography.bodySmall,
                                color = ClayInk.Muted,
                            )
                        }
                    }
                }
            }

            // ── 3. Schedule (collapsible) ───────────────────────────────────
            item {
                ClayCard(shape = ClayShape.Card, elevation = 10.dp, glowAlpha = 0.14f) {
                    ExpandableHeader(
                        number = "3",
                        title = "Schedule",
                        subtitle = state.scheduleKind.helper,
                        expanded = scheduleExpanded,
                        onToggle = { scheduleExpanded = !scheduleExpanded },
                    )
                    if (scheduleExpanded) {
                        Spacer(Modifier.height(12.dp))
                        WrappingChips(
                            items = ScheduleKind.entries.toList(),
                            selected = state.scheduleKind,
                            onSelect = viewModel::setSchedule,
                            label = { it.label },
                            emoji = { it.emoji },
                            accent = ClayPalette.Butter,
                        )
                        if (state.scheduleKind == ScheduleKind.SelectedDays) {
                            Spacer(Modifier.height(14.dp))
                            Text(
                                text = "Pick your days",
                                style = MaterialTheme.typography.labelMedium,
                                color = ClayInk.Secondary,
                                modifier = Modifier.padding(start = 4.dp, bottom = 8.dp),
                            )
                            DayPicker(days = state.days, onToggle = viewModel::toggleDay)
                        }
                    }
                }
            }

            // ── 4. Goal Configuration (collapsible) ─────────────────────────
            item {
                ClayCard(shape = ClayShape.Card, elevation = 10.dp, glowAlpha = 0.14f) {
                    ExpandableHeader(
                        number = "4",
                        title = "Goal shape",
                        subtitle = state.questKind.helper,
                        expanded = goalExpanded,
                        onToggle = { goalExpanded = !goalExpanded },
                    )
                    if (goalExpanded) {
                        Spacer(Modifier.height(12.dp))
                        WrappingChips(
                            items = QuestKind.entries.toList(),
                            selected = state.questKind,
                            onSelect = viewModel::setQuestKind,
                            label = { it.label },
                            emoji = { it.emoji },
                            accent = ClayPalette.Lavender,
                        )

                        if (state.isMultiStep) {
                            Spacer(Modifier.height(14.dp))
                            Text(
                                text = "Steps — each one tracks its own progress",
                                style = MaterialTheme.typography.labelMedium,
                                color = ClayInk.Secondary,
                            )
                            Spacer(Modifier.height(8.dp))
                            state.steps.forEachIndexed { index, step ->
                                StepRow(
                                    index = index,
                                    step = step,
                                    onTitle = { viewModel.updateStepTitle(index, it) },
                                    onTarget = { viewModel.updateStepTarget(index, it) },
                                    onToggleRequired = { viewModel.toggleStepRequired(index) },
                                    onRemove = { viewModel.removeStep(index) },
                                )
                            }
                            ClayButton(
                                text = "Add step",
                                onClick = viewModel::addStep,
                                accent = ClayPalette.Lavender,
                                style = ClayButtonStyle.Soft,
                                icon = Icons.Rounded.Add,
                                fillWidth = true,
                                height = 42.dp,
                            )
                            Spacer(Modifier.height(8.dp))
                            Text(
                                text = "XP is awarded per step logged; the completion bonus lands once per period when the last required step is finished.",
                                style = MaterialTheme.typography.labelSmall,
                                color = ClayInk.Faint,
                            )
                        }

                        if (state.isLongTerm) {
                            Spacer(Modifier.height(14.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "Planned length",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = ClayInk.Secondary,
                                    modifier = Modifier.weight(1f),
                                )
                                ClayStepper(
                                    value = state.durationDays,
                                    onValueChange = viewModel::setDurationDays,
                                    range = 1..730,
                                    step = 5,
                                    accent = ClayPalette.Peach,
                                    suffix = "days",
                                )
                            }
                            Spacer(Modifier.height(6.dp))
                            Text(
                                text = "Progress accumulates in one lifetime bucket from the day the quest started.",
                                style = MaterialTheme.typography.labelSmall,
                                color = ClayInk.Faint,
                            )
                        }
                    }
                }
            }

            // ── 5. Difficulty & Deadline (collapsible) ───────────────────────
            item {
                ClayCard(shape = ClayShape.Card, elevation = 10.dp, glowAlpha = 0.14f) {
                    ExpandableHeader(
                        number = "5",
                        title = "Difficulty & deadline",
                        subtitle = "${state.difficulty.label} · ${if (state.deadlineInDays == null) "no deadline" else "${state.deadlineInDays} days"}",
                        expanded = difficultyExpanded,
                        onToggle = { difficultyExpanded = !difficultyExpanded },
                    )
                    if (difficultyExpanded) {
                        Spacer(Modifier.height(12.dp))
                        Text(
                            text = "Difficulty",
                            style = MaterialTheme.typography.labelMedium,
                            color = ClayInk.Secondary,
                            modifier = Modifier.padding(start = 4.dp, bottom = 8.dp),
                        )
                        WrappingChips(
                            items = QuestDifficulty.entries.toList(),
                            selected = state.difficulty,
                            onSelect = viewModel::setDifficulty,
                            label = { "${it.label} · ${it.suggestedXp} XP" },
                            emoji = { it.emoji },
                            accentFor = { ClayPalette.accentById(it.accentId) },
                        )

                        Spacer(Modifier.height(14.dp))
                        Text(
                            text = "Deadline",
                            style = MaterialTheme.typography.labelMedium,
                            color = ClayInk.Secondary,
                            modifier = Modifier.padding(start = 4.dp, bottom = 8.dp),
                        )
                        WrappingChips(
                            items = listOf(null) + listOf(7, 14, 30, 60, 90, 180, 365),
                            selected = state.deadlineInDays,
                            onSelect = viewModel::setDeadlineDays,
                            label = { if (it == null) "No deadline" else "$it days" },
                            accentFor = { if (it == null) ClayPalette.Mint else ClayPalette.Peach },
                        )
                        if (state.deadlineInDays != null) {
                            Spacer(Modifier.height(10.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                ClayStepper(
                                    value = state.deadlineInDays ?: 0,
                                    onValueChange = { viewModel.setDeadlineDays(it) },
                                    range = 0..1_095,
                                    step = 5,
                                    accent = ClayPalette.Peach,
                                    suffix = "days",
                                )
                                Spacer(Modifier.width(10.dp))
                                Text(
                                    text = "Deadlines only label and sort — they never punish you.",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = ClayInk.Faint,
                                    modifier = Modifier.weight(1f),
                                )
                            }
                        }
                    }
                }
            }

            // ── 6. Rewards & Skill (always visible, explicit breakdown) ──────
            item {
                ClayCard(shape = ClayShape.Card, elevation = 12.dp, glowAlpha = 0.18f) {
                    EditorSectionTitle(number = "6", title = "Reward & skill", subtitle = "What this quest earns and where it counts")
                    Spacer(Modifier.height(14.dp))

                    // XP reward control
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "Base reward",
                            style = MaterialTheme.typography.labelMedium,
                            color = ClayInk.Secondary,
                            modifier = Modifier.weight(1f),
                        )
                        ClayStepper(
                            value = state.xpReward,
                            onValueChange = viewModel::setXpReward,
                            range = 10..1_000,
                            step = 10,
                            accent = ClayPalette.Butter,
                            suffix = "XP",
                        )
                    }

                    // ── Explicit reward breakdown ──────────────────────────
                    Spacer(Modifier.height(14.dp))
                    RewardBreakdown(state = state)

                    // ── Skill assignment ───────────────────────────────────
                    Spacer(Modifier.height(16.dp))
                    Text(
                        text = "Which skill does it level?",
                        style = MaterialTheme.typography.labelMedium,
                        color = ClayInk.Secondary,
                        modifier = Modifier.padding(start = 4.dp, bottom = 8.dp),
                    )
                    val selectedAttribute = state.attributes.firstOrNull { it.attributeKind == state.attributeName || it.name == state.attributeName }
                    if (selectedAttribute != null) {
                        WrappingChips(
                            items = state.attributes,
                            selected = selectedAttribute,
                            onSelect = { attr -> viewModel.setAttribute(attr.attributeKind ?: attr.name) },
                            label = { it.name },
                            emoji = { it.emoji },
                            accentFor = { ClayPalette.accentById(it.accentId) },
                            key = { it.id },
                        )
                    } else if (state.attributes.isNotEmpty()) {
                        WrappingChips(
                            items = state.attributes,
                            selected = state.attributes.first(),
                            onSelect = { attr -> viewModel.setAttribute(attr.attributeKind ?: attr.name) },
                            label = { it.name },
                            emoji = { it.emoji },
                            accentFor = { ClayPalette.accentById(it.accentId) },
                            key = { it.id },
                        )
                    }

                    // ── Optional notes ─────────────────────────────────────
                    Spacer(Modifier.height(16.dp))
                    ClayTextField(
                        value = state.notes,
                        onValueChange = viewModel::setNotes,
                        label = "Notes (optional)",
                        placeholder = "Why this matters, or how you'll do it",
                        singleLine = false,
                        maxLines = 4,
                        accent = ClayPalette.Lavender,
                    )
                }
            }

            // ── Summary hint (replaces second set of buttons) ──────────────
            item {
                ClayPanel(
                    tint = ClayPalette.SurfaceMuted.copy(alpha = 0.55f),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                ) {
                    Text(
                        text = questSummaryLine(state),
                        style = MaterialTheme.typography.bodySmall,
                        color = ClayInk.Muted,
                    )
                }
            }
        }
    }
}

// ── Bottom bar: exactly one primary Create action ───────────────────────────

@Composable
private fun QuestEditorBottomBar(
    state: QuestEditorState,
    onSave: () -> Unit,
    onCancel: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 16.dp, vertical = 10.dp),
    ) {
        if (state.error != null) {
            Text(
                text = state.error.orEmpty(),
                style = MaterialTheme.typography.labelMedium,
                color = ClayPalette.Blush.deep,
                modifier = Modifier.padding(bottom = 8.dp),
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            ClayButton(
                text = "Cancel",
                onClick = onCancel,
                accent = ClayPalette.Lavender,
                style = ClayButtonStyle.Soft,
                modifier = Modifier.weight(1f),
                height = 50.dp,
            )
            ClayButton(
                text = when {
                    state.saving -> "Saving…"
                    state.editing -> "Save changes"
                    else -> "Create quest"
                },
                onClick = onSave,
                accent = ClayPalette.Lavender,
                modifier = Modifier.weight(1.45f),
                height = 50.dp,
                enabled = !state.saving,
            )
        }
    }
}

// ── Reward breakdown: explicit math ────────────────────────────────────────

@Composable
private fun RewardBreakdown(state: QuestEditorState) {
    val perUnit = XpEngine.perStepXp(state.trackingKind, state.xpReward, state.target)
    val bonus = XpEngine.completionBonus(state.xpReward)

    ClayPanel(
        tint = ClayPalette.Butter.soft.copy(alpha = 0.55f),
        contentPadding = PaddingValues(14.dp),
    ) {
        Text(
            text = "How XP is calculated",
            style = MaterialTheme.typography.labelMedium,
            color = ClayInk.Primary,
        )
        Spacer(Modifier.height(8.dp))
        if (state.trackingKind == TrackingKind.Milestone) {
            RewardLine(label = "On completion", value = "${state.xpReward} XP", accent = ClayPalette.Butter)
            RewardLine(label = "Completion bonus", value = "+$bonus XP", accent = ClayPalette.Mint)
            Spacer(Modifier.height(6.dp))
            Text(
                text = "Milestone quests pay once per period; reopening them adds no XP until the next period.",
                style = MaterialTheme.typography.labelSmall,
                color = ClayInk.Muted,
            )
        } else {
            // Example e.g. 30 min target, 60 XP reward -> 2 XP/min
            RewardLine(
                label = "Per 1 ${state.unitLabel}",
                value = "$perUnit XP",
                accent = ClayPalette.Butter,
            )
            RewardLine(
                label = "Full target (${state.target} ${state.unitLabel})",
                value = "${state.xpReward} XP",
                accent = ClayPalette.Sky,
            )
            RewardLine(label = "Completion bonus (once)", value = "+$bonus XP", accent = ClayPalette.Mint)
            Spacer(Modifier.height(8.dp))
            Text(
                text = buildString {
                    append("Progress XP is proportional: logging half the target earns half the reward. ")
                    append("After the target is reached, further logs add history but no XP. ")
                    append("The bonus is paid exactly once per period.")
                },
                style = MaterialTheme.typography.labelSmall,
                color = ClayInk.Muted,
            )
        }
        Spacer(Modifier.height(6.dp))
        Text(
            text = "XP never comes from just opening the app or tapping \u201Ccomplete\u201D — only recorded progress counts. Quest instances (${
                when {
                    state.scheduleKind == ScheduleKind.Daily -> "one per day"
                    state.scheduleKind == ScheduleKind.Weekly -> "one per week"
                    state.questKind == QuestKind.LongTerm -> "one lifetime bucket"
                    else -> "one per period"
                }
            }) block duplicate rewards.",
            style = MaterialTheme.typography.labelSmall,
            color = ClayInk.Faint,
        )
    }
}

@Composable
private fun RewardLine(label: String, value: String, accent: com.irlrpg.app.ui.theme.ClayAccent) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(ClayShape.Pill)
                .background(accent.base),
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = ClayInk.Secondary,
            modifier = Modifier.weight(1f),
        )
        Text(
            text = value,
            style = MaterialTheme.typography.labelMedium,
            color = accent.deep,
        )
    }
}

// ── Section helpers ─────────────────────────────────────────────────────────

@Composable
private fun EditorSectionTitle(number: String, title: String, subtitle: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(28.dp)
                .clip(ClayShape.Pill)
                .background(ClayPalette.Lavender.soft),
            contentAlignment = Alignment.Center,
        ) {
            Text(text = number, style = MaterialTheme.typography.labelMedium, color = ClayPalette.Lavender.deep)
        }
        Spacer(Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, style = MaterialTheme.typography.titleSmall, color = ClayInk.Primary)
            Text(text = subtitle, style = MaterialTheme.typography.bodySmall, color = ClayInk.Muted, maxLines = 2, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
private fun ExpandableHeader(
    number: String,
    title: String,
    subtitle: String,
    expanded: Boolean,
    onToggle: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(ClayShape.Small)
            .clickable(onClick = onToggle)
            .padding(vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(28.dp)
                .clip(ClayShape.Pill)
                .background(ClayPalette.SurfaceMuted),
            contentAlignment = Alignment.Center,
        ) {
            Text(text = number, style = MaterialTheme.typography.labelMedium, color = ClayInk.Secondary)
        }
        Spacer(Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, style = MaterialTheme.typography.titleSmall, color = ClayInk.Primary)
            Text(text = subtitle, style = MaterialTheme.typography.bodySmall, color = ClayInk.Muted, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        Icon(
            imageVector = if (expanded) Icons.Rounded.KeyboardArrowUp else Icons.Rounded.KeyboardArrowDown,
            contentDescription = if (expanded) "Collapse" else "Expand",
            tint = ClayInk.Muted,
            modifier = Modifier.size(22.dp),
        )
    }
}

// ── Wrapping chip group: FlowRow so nothing overflows horizontally ──────────

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun <T> WrappingChips(
    items: List<T>,
    selected: T,
    onSelect: (T) -> Unit,
    label: (T) -> String,
    modifier: Modifier = Modifier,
    emoji: ((T) -> String?)? = null,
    accent: com.irlrpg.app.ui.theme.ClayAccent = ClayPalette.Lavender,
    accentFor: ((T) -> com.irlrpg.app.ui.theme.ClayAccent)? = null,
    key: ((T) -> Any)? = null,
) {
    FlowRow(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items.forEach { item ->
            val isSelected = item == selected
            val itemAccent = accentFor?.invoke(item) ?: accent
            ClayChip(
                text = label(item),
                selected = isSelected,
                onClick = { onSelect(item) },
                accent = itemAccent,
                leadingEmoji = emoji?.invoke(item),
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CategoryChips(
    categories: List<com.irlrpg.app.data.local.entity.CategoryEntity>,
    selectedId: Long?,
    onSelect: (Long?) -> Unit,
) {
    FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        ClayChip(
            text = "None",
            selected = selectedId == null,
            onClick = { onSelect(null) },
            accent = ClayPalette.Lavender,
        )
        categories.forEach { category ->
            ClayChip(
                text = category.name,
                selected = selectedId == category.id,
                onClick = { onSelect(category.id) },
                accent = ClayPalette.accentById(category.accentId),
                leadingEmoji = category.emoji,
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun DayPicker(
    days: Set<Int>,
    onToggle: (Int) -> Unit,
) {
    FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(7.dp),
        verticalArrangement = Arrangement.spacedBy(7.dp),
    ) {
        dayLabels.forEachIndexed { index, label ->
            val day = index + 1
            val selected = days.contains(day)
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(ClayShape.Small)
                    .background(if (selected) ClayPalette.Butter.soft else ClayPalette.SurfaceMuted.copy(alpha = 0.6f))
                    .clickable { onToggle(day) },
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelMedium,
                    color = if (selected) ClayPalette.Butter.deep else ClayInk.Muted,
                )
            }
        }
    }
}

@Composable
private fun StepRow(
    index: Int,
    step: com.irlrpg.app.domain.model.QuestStepDraft,
    onTitle: (String) -> Unit,
    onTarget: (String) -> Unit,
    onToggleRequired: () -> Unit,
    onRemove: () -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "Step ${index + 1}",
                style = MaterialTheme.typography.labelSmall,
                color = ClayInk.Muted,
                modifier = Modifier.weight(1f),
            )
            ClayChip(
                text = if (step.isRequired) "Required" else "Optional",
                selected = step.isRequired,
                onClick = onToggleRequired,
                accent = ClayPalette.Mint,
            )
            Spacer(Modifier.width(8.dp))
            ClayIconButton(
                icon = Icons.Rounded.Close,
                onClick = onRemove,
                contentDescription = "Remove step",
                accent = ClayPalette.Blush,
                size = 34.dp,
            )
        }
        Spacer(Modifier.height(8.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            ClayTextField(
                value = step.title,
                onValueChange = onTitle,
                label = "Step title",
                modifier = Modifier.weight(1f),
                placeholder = "e.g. Outline chapter 1",
            )
            Spacer(Modifier.width(10.dp))
            ClayTextField(
                value = step.targetAmount.toString(),
                onValueChange = onTarget,
                label = "Target",
                modifier = Modifier.width(96.dp),
                keyboardType = KeyboardType.Number,
                accent = ClayPalette.Sky,
            )
        }
    }
}

private fun questSummaryLine(state: QuestEditorState): String = buildString {
    append(state.trackingKind.label)
    if (state.trackingKind != com.irlrpg.app.domain.model.TrackingKind.Milestone) {
        append(" · ${state.target} ${state.unitLabel}")
    }
    append(" · ${state.scheduleKind.label}")
    append(" · ${state.questKind.label}")
    append(" · ${state.difficulty.label}")
    append(" · ${state.xpReward} XP base")
    append(" → ${state.attributeName}")
}

private fun targetStep(target: Int, unit: UnitKind): Int = when {
    target <= 5 -> 1
    unit == UnitKind.Hours || unit == UnitKind.Sessions -> 1
    target <= 20 -> 1
    target <= 100 -> 5
    else -> 10
}

private val dayLabels = listOf("M", "T", "W", "T", "F", "S", "S")
