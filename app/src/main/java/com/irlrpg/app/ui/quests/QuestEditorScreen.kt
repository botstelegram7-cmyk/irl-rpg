package com.irlrpg.app.ui.quests

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.irlrpg.app.domain.game.XpEngine
import com.irlrpg.app.domain.model.ScheduleKind
import com.irlrpg.app.domain.model.TrackingKind
import com.irlrpg.app.domain.model.UnitKind
import com.irlrpg.app.ui.design.ClayButton
import com.irlrpg.app.ui.design.ClayButtonStyle
import com.irlrpg.app.ui.design.ClayCard
import com.irlrpg.app.ui.design.ClayChip
import com.irlrpg.app.ui.design.ClayScaffold
import com.irlrpg.app.ui.design.ClaySectionHeader
import com.irlrpg.app.ui.design.ClayStepper
import com.irlrpg.app.ui.design.ClayTextField
import com.irlrpg.app.ui.design.ClayTopBar
import com.irlrpg.app.ui.theme.ClayAccent
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
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(horizontal = 20.dp, vertical = 12.dp),
            ) {
                if (state.error != null) {
                    Text(
                        text = state.error.orEmpty(),
                        style = MaterialTheme.typography.labelMedium,
                        color = ClayPalette.Blush.deep,
                        modifier = Modifier.padding(bottom = 8.dp),
                    )
                }
                ClayButton(
                    text = when {
                        state.saving -> "Saving…"
                        state.editing -> "Save changes"
                        else -> "Create quest"
                    },
                    onClick = viewModel::save,
                    accent = ClayPalette.Lavender,
                    fillWidth = true,
                    height = 54.dp,
                    enabled = !state.saving,
                )
            }
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = 20.dp,
                end = 20.dp,
                top = padding.calculateTopPadding() + 4.dp,
                bottom = padding.calculateBottomPadding() + 20.dp,
            ),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item {
                ClayCard(shape = ClayShape.Card, elevation = 14.dp) {
                    ClaySectionHeader(
                        title = "What do you want to grow?",
                        subtitle = "Any activity counts — reading, coding, training, writing, practising",
                    )
                    Spacer(Modifier.height(14.dp))
                    Text(
                        text = "Icon",
                        style = MaterialTheme.typography.labelMedium,
                        color = ClayInk.Secondary,
                        modifier = Modifier.padding(start = 4.dp, bottom = 8.dp),
                    )
                    EmojiRow(selected = state.emoji, onSelect = viewModel::setEmoji)
                    Spacer(Modifier.height(16.dp))
                    ClayTextField(
                        value = state.title,
                        onValueChange = viewModel::setTitle,
                        label = "Quest title",
                        placeholder = "e.g. Read a novel · Practise scales · Ship a feature",
                    )
                    Spacer(Modifier.height(16.dp))
                    Text(
                        text = "Category",
                        style = MaterialTheme.typography.labelMedium,
                        color = ClayInk.Secondary,
                        modifier = Modifier.padding(start = 4.dp, bottom = 8.dp),
                    )
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        item {
                            ClayChip(
                                text = "None",
                                selected = state.categoryId == null,
                                onClick = { viewModel.setCategory(null) },
                                accent = ClayPalette.Lavender,
                            )
                        }
                        items(state.categories, key = { it.id }) { category ->
                            ClayChip(
                                text = category.name,
                                selected = state.categoryId == category.id,
                                onClick = { viewModel.setCategory(category.id) },
                                accent = ClayPalette.accentById(category.accentId),
                                leadingEmoji = category.emoji,
                            )
                        }
                    }
                }
            }

            item {
                ClayCard(shape = ClayShape.Card, elevation = 14.dp) {
                    ClaySectionHeader(
                        title = "How is it tracked?",
                        subtitle = state.trackingKind.helper,
                    )
                    Spacer(Modifier.height(14.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(TrackingKind.entries.toList()) { kind ->
                            ClayChip(
                                text = kind.label,
                                selected = state.trackingKind == kind,
                                onClick = { viewModel.setTrackingKind(kind) },
                                accent = ClayPalette.Sky,
                                leadingEmoji = kind.emoji,
                            )
                        }
                    }

                    if (state.trackingKind != TrackingKind.Milestone) {
                        Spacer(Modifier.height(18.dp))
                        Text(
                            text = "Target per period",
                            style = MaterialTheme.typography.labelMedium,
                            color = ClayInk.Secondary,
                        )
                        Spacer(Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Start,
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
                            Spacer(Modifier.width(12.dp))
                            ClayTextField(
                                value = state.targetText,
                                onValueChange = viewModel::setTargetText,
                                label = "Exact",
                                modifier = Modifier.width(110.dp),
                                keyboardType = KeyboardType.Number,
                                accent = ClayPalette.Sky,
                            )
                        }

                        Spacer(Modifier.height(16.dp))
                        Text(
                            text = "Unit",
                            style = MaterialTheme.typography.labelMedium,
                            color = ClayInk.Secondary,
                            modifier = Modifier.padding(start = 4.dp, bottom = 8.dp),
                        )
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(UnitKind.entries.toList()) { unit ->
                                ClayChip(
                                    text = unit.label,
                                    selected = state.unit == unit,
                                    onClick = { viewModel.setUnit(unit) },
                                    accent = ClayPalette.Mint,
                                )
                            }
                        }
                        if (state.unit == UnitKind.Custom) {
                            Spacer(Modifier.height(14.dp))
                            ClayTextField(
                                value = state.customUnit,
                                onValueChange = viewModel::setCustomUnit,
                                label = "Your unit name",
                                placeholder = "e.g. songs, sketches, problems",
                                accent = ClayPalette.Mint,
                            )
                        }
                    } else {
                        Spacer(Modifier.height(14.dp))
                        Text(
                            text = "Milestone quests are finished once — perfect for \"read this novel\", \"ship this project\" or \"pass this level\".",
                            style = MaterialTheme.typography.bodySmall,
                            color = ClayInk.Muted,
                        )
                    }
                }
            }

            item {
                ClayCard(shape = ClayShape.Card, elevation = 14.dp) {
                    ClaySectionHeader(
                        title = "When does it appear?",
                        subtitle = state.scheduleKind.helper,
                    )
                    Spacer(Modifier.height(14.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(ScheduleKind.entries.toList()) { schedule ->
                            ClayChip(
                                text = schedule.label,
                                selected = state.scheduleKind == schedule,
                                onClick = { viewModel.setSchedule(schedule) },
                                accent = ClayPalette.Butter,
                                leadingEmoji = schedule.emoji,
                            )
                        }
                    }
                    if (state.scheduleKind == ScheduleKind.SelectedDays) {
                        Spacer(Modifier.height(16.dp))
                        Text(
                            text = "Pick your days",
                            style = MaterialTheme.typography.labelMedium,
                            color = ClayInk.Secondary,
                            modifier = Modifier.padding(start = 4.dp, bottom = 8.dp),
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                            dayLabels.forEachIndexed { index, label ->
                                val day = index + 1
                                val selected = state.days.contains(day)
                                Box(
                                    modifier = Modifier
                                        .size(42.dp)
                                        .clip(ClayShape.Small)
                                        .background(
                                            if (selected) ClayPalette.Butter.soft else ClayPalette.SurfaceMuted.copy(alpha = 0.6f)
                                        )
                                        .clickable { viewModel.toggleDay(day) },
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
                }
            }

            item {
                ClayCard(shape = ClayShape.Card, elevation = 14.dp) {
                    ClaySectionHeader(
                        title = "Reward",
                        subtitle = "XP is only granted for real recorded progress",
                    )
                    Spacer(Modifier.height(14.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        ClayStepper(
                            value = state.xpReward,
                            onValueChange = viewModel::setXpReward,
                            range = 10..500,
                            step = 10,
                            accent = ClayPalette.Butter,
                            suffix = "XP",
                        )
                    }
                    Spacer(Modifier.height(12.dp))
                    Text(
                        text = rewardPreview(state),
                        style = MaterialTheme.typography.bodySmall,
                        color = ClayInk.Muted,
                    )

                    Spacer(Modifier.height(18.dp))
                    Text(
                        text = "Which skill does it level?",
                        style = MaterialTheme.typography.labelMedium,
                        color = ClayInk.Secondary,
                        modifier = Modifier.padding(start = 4.dp, bottom = 8.dp),
                    )
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(state.attributes, key = { it.id }) { attribute ->
                            ClayChip(
                                text = attribute.name,
                                selected = state.attributeName == attribute.attributeKind || state.attributeName == attribute.name,
                                onClick = { viewModel.setAttribute(attribute.attributeKind ?: attribute.name) },
                                accent = ClayPalette.accentById(attribute.accentId),
                                leadingEmoji = attribute.emoji,
                            )
                        }
                    }

                    Spacer(Modifier.height(18.dp))
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

            item {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    ClayButton(
                        text = "Cancel",
                        onClick = onBack,
                        accent = ClayPalette.Lavender,
                        style = ClayButtonStyle.Soft,
                        modifier = Modifier.weight(1f),
                    )
                    ClayButton(
                        text = if (state.editing) "Save" else "Create",
                        onClick = viewModel::save,
                        accent = ClayPalette.Lavender,
                        modifier = Modifier.weight(1.3f),
                        enabled = !state.saving,
                    )
                }
            }
        }
    }
}

private fun rewardPreview(state: QuestEditorState): String {
    if (state.trackingKind == TrackingKind.Milestone) {
        return "Completing it awards ${state.xpReward} XP once (plus a milestone bonus)."
    }
    val perStep = XpEngine.perStepXp(state.trackingKind, state.xpReward, state.target)
    val bonus = XpEngine.completionBonus(state.xpReward)
    return "≈ $perStep XP per 1 ${state.unitLabel}, ${state.xpReward} XP across the target, +$bonus XP when the target is reached. Extra logging after that adds no XP."
}

private fun targetStep(target: Int, unit: UnitKind): Int = when {
    target <= 5 -> 1
    unit == UnitKind.Hours || unit == UnitKind.Sessions -> 1
    target <= 20 -> 1
    target <= 100 -> 5
    else -> 10
}

private val dayLabels = listOf("M", "T", "W", "T", "F", "S", "S")

@Composable
private fun AccentPreview(accent: ClayAccent) {
    Text(
        text = "",
        style = MaterialTheme.typography.labelSmall,
        fontSize = 6.sp,
        color = accent.deep,
        maxLines = 1,
        overflow = TextOverflow.Clip,
    )
}
