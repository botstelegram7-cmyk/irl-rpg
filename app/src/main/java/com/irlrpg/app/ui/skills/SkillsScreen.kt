package com.irlrpg.app.ui.skills

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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.irlrpg.app.data.local.entity.AttributeEntity
import com.irlrpg.app.domain.model.AttributeKind
import com.irlrpg.app.ui.components.ClayConfirmDialog
import com.irlrpg.app.ui.components.ClayToast
import com.irlrpg.app.ui.design.ClayButton
import com.irlrpg.app.ui.design.ClayButtonStyle
import com.irlrpg.app.ui.design.ClayCard
import com.irlrpg.app.ui.design.ClayChip
import com.irlrpg.app.ui.design.ClayDivider
import com.irlrpg.app.ui.design.ClayDot
import com.irlrpg.app.ui.design.ClayIconButton
import com.irlrpg.app.ui.design.ClayMiniStat
import com.irlrpg.app.ui.design.ClayPanel
import com.irlrpg.app.ui.design.ClayProgressBar
import com.irlrpg.app.ui.design.ClayScaffold
import com.irlrpg.app.ui.design.ClaySectionTitle
import com.irlrpg.app.ui.design.ClayTextField
import com.irlrpg.app.ui.design.ClayTopBar
import com.irlrpg.app.ui.quests.EmojiRow
import com.irlrpg.app.ui.theme.ClayAccent
import com.irlrpg.app.ui.theme.ClayInk
import com.irlrpg.app.ui.theme.ClayPalette
import com.irlrpg.app.ui.theme.ClayShape

/**
 * Skills screen.
 *
 * Two clearly separated groups — the six core attributes and the player's own
 * skills — each rendered as flat rows inside a single card, which removes the
 * "one raised card per skill" wall. Every number and bar here is derived from
 * the quest log (XP, completions, active quests); nothing is decorative.
 *
 * The list layout is deliberately two-level (group → row) so a future
 * interactive tree view can replace the "Skills" section without touching the
 * header, dialogs, or data flow.
 */
@Composable
fun SkillsScreen(
    viewModel: SkillsViewModel,
    bottomBar: @Composable () -> Unit,
    onOpenSkillTree: () -> Unit = {},
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val message by viewModel.message.collectAsStateWithLifecycle()
    var showAdd by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf<AttributeEntity?>(null) }
    var pendingDelete by remember { mutableStateOf<AttributeEntity?>(null) }

    val core = state.rows.filter { it.attribute.isBuiltIn }.sortedByDescending { it.xp }
    val custom = state.rows.filterNot { it.attribute.isBuiltIn }.sortedByDescending { it.xp }
    val totalXp = state.rows.sumOf { it.xp }
    val totalEntries = state.rows.sumOf { it.completions }
    val best = state.rows.maxByOrNull { it.xp }

    ClayScaffold(
        topBar = {
            ClayTopBar(
                title = "Skills",
                subtitle = "XP lands on the skill each quest belongs to",
                accent = ClayPalette.Butter,
            )
        },
        bottomBar = bottomBar,
    ) { padding ->
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
                ClayCard(
                    shape = ClayShape.Card,
                    elevation = 14.dp,
                    glowAlpha = 0.2f,
                    contentPadding = PaddingValues(18.dp),
                ) {
                    ClaySectionTitle(
                        title = "Skill profile",
                        eyebrow = "Recorded",
                        caption = "Recomputed from ${totalEntries} completed entries",
                    )
                    Spacer(Modifier.height(14.dp))
                    Row {
                        ClayMiniStat(
                            value = "$totalXp",
                            label = "XP in skills",
                            emoji = "⚡",
                            accent = ClayPalette.Butter,
                            modifier = Modifier.weight(1f),
                        )
                        ClayMiniStat(
                            value = "${state.rows.size}",
                            label = "Skills tracked",
                            emoji = "🧠",
                            accent = ClayPalette.Lavender,
                            modifier = Modifier.weight(1f),
                        )
                        ClayMiniStat(
                            value = best?.attribute?.emoji ?: "—",
                            label = best?.attribute?.name ?: "No data yet",
                            accent = ClayPalette.Mint,
                            modifier = Modifier.weight(1f),
                        )
                    }
                    Spacer(Modifier.height(16.dp))
                    ClayButton(
                        text = "Open the skill tree",
                        onClick = onOpenSkillTree,
                        accent = ClayPalette.Mint,
                        fillWidth = true,
                        height = 48.dp,
                        leadingEmoji = "🌳",
                    )
                    Spacer(Modifier.height(10.dp))
                    ClayButton(
                        text = "Add a custom skill",
                        onClick = { showAdd = true },
                        accent = ClayPalette.Lavender,
                        style = ClayButtonStyle.Soft,
                        fillWidth = true,
                        height = 46.dp,
                        leadingEmoji = "✨",
                    )
                }
            }

            if (core.isNotEmpty()) {
                item {
                    SkillGroup(
                        title = "Core skills",
                        eyebrow = "Built in",
                        caption = "Six universal attributes every quest can feed into",
                        rows = core,
                        maxXp = state.maxXp,
                        showShare = true,
                        onEdit = { editing = it },
                        onDelete = { pendingDelete = it },
                    )
                }
            }

            item {
                if (custom.isEmpty()) {
                    ClayPanel(
                        shape = ClayShape.Card,
                        tint = ClayPalette.SurfaceCream,
                        contentPadding = PaddingValues(18.dp),
                        onClick = { showAdd = true },
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            ClayDot(emoji = "✨", accent = ClayPalette.Lavender, size = 38.dp)
                            Spacer(Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "No custom skills yet",
                                    style = MaterialTheme.typography.titleSmall,
                                    color = ClayInk.Primary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                                Text(
                                    text = "Track something specific — guitar, chess, a language, a side project.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = ClayInk.Muted,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                        }
                    }
                } else {
                    SkillGroup(
                        title = "Your skills",
                        eyebrow = "Custom",
                        caption = "Skills you added yourself — remove them any time",
                        rows = custom,
                        maxXp = state.maxXp,
                        showShare = totalXp > 0,
                        onEdit = { editing = it },
                        onDelete = { pendingDelete = it },
                    )
                }
            }

            if (state.rows.isEmpty() && !state.loading) {
                item {
                    ClayPanel(
                        shape = ClayShape.Card,
                        tint = ClayPalette.SurfaceMuted.copy(alpha = 0.6f),
                        contentPadding = PaddingValues(16.dp),
                    ) {
                        Text(
                            text = "Skills appear as soon as your first quest is completed. Add a custom skill to start tracking something new right away.",
                            style = MaterialTheme.typography.bodySmall,
                            color = ClayInk.Muted,
                        )
                    }
                }
            }
        }
    }

    if (showAdd) {
        SkillDialog(
            title = "New skill",
            initialName = "",
            initialEmoji = "✨",
            initialAccent = "lavender",
            onDismiss = { showAdd = false },
            onConfirm = { name, emoji, accentId ->
                viewModel.addSkill(name, emoji, accentId)
                showAdd = false
            },
        )
    }

    editing?.let { attribute ->
        SkillDialog(
            title = "Edit \"${attribute.name}\"",
            initialName = attribute.name,
            initialEmoji = attribute.emoji,
            initialAccent = attribute.accentId,
            onDismiss = { editing = null },
            onConfirm = { name, emoji, accentId ->
                viewModel.updateSkill(attribute, name, emoji, accentId)
                editing = null
            },
        )
    }

    pendingDelete?.let { attribute ->
        ClayConfirmDialog(
            title = "Remove \"${attribute.name}\"?",
            message = "Quests that pointed at this skill keep their history and will now fold into their category colours.",
            confirmLabel = "Remove skill",
            onConfirm = {
                viewModel.deleteSkill(attribute)
                pendingDelete = null
            },
            onDismiss = { pendingDelete = null },
        )
    }

    ClayToast(message = message, onShown = viewModel::consumeMessage, bottomPadding = 104.dp)
}

/** One group of skills: a single card with flat rows and hairline separators. */
@Composable
private fun SkillGroup(
    title: String,
    eyebrow: String,
    caption: String,
    rows: List<SkillRow>,
    maxXp: Int,
    showShare: Boolean,
    onEdit: (AttributeEntity) -> Unit,
    onDelete: (AttributeEntity) -> Unit,
) {
    ClayCard(
        shape = ClayShape.Card,
        elevation = 12.dp,
        glowAlpha = 0.14f,
        contentPadding = PaddingValues(18.dp),
    ) {
        ClaySectionTitle(title = title, eyebrow = eyebrow, caption = caption)
        Spacer(Modifier.height(6.dp))
        rows.forEachIndexed { index, row ->
            if (index > 0) ClayDivider(verticalPadding = 0.dp)
            SkillRowItem(
                row = row,
                maxXp = maxXp,
                showShare = showShare,
                onEdit = { onEdit(row.attribute) },
                onDelete = { onDelete(row.attribute) },
            )
        }
    }
}

@Composable
private fun SkillRowItem(
    row: SkillRow,
    maxXp: Int,
    showShare: Boolean,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
) {
    var menuOpen by remember { mutableStateOf(false) }
    val accent = accentForSkill(row.attribute)
    // Bar length is relative to the strongest skill — a real comparison between
    // recorded totals, never an invented target.
    val bar = if (maxXp <= 0) 0f else (row.xp.toFloat() / maxXp.toFloat()).coerceIn(0f, 1f)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ClayDot(emoji = row.attribute.emoji, accent = accent, size = 42.dp)
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = row.attribute.name,
                style = MaterialTheme.typography.titleSmall,
                color = ClayInk.Primary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = buildString {
                    append("${row.xp} XP")
                    append(" · ${row.completions} ${if (row.completions == 1) "completion" else "completions"}")
                    append(
                        when {
                            row.activeQuestCount > 0 -> " · ${row.activeQuestCount} active"
                            row.questCount > 0 -> " · ${row.questCount} quests"
                            else -> " · no quests yet"
                        }
                    )
                },
                style = MaterialTheme.typography.labelSmall,
                color = ClayInk.Muted,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(7.dp))
            ClayProgressBar(progress = bar, accent = accent, height = 8.dp)
            if (showShare && row.xp > 0) {
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "${(row.share * 100).toInt()}% of recorded skill XP",
                    style = MaterialTheme.typography.labelSmall,
                    color = ClayInk.Faint,
                    maxLines = 1,
                )
            }
        }
        Spacer(Modifier.width(8.dp))
        Box {
            ClayIconButton(
                icon = Icons.Rounded.MoreVert,
                onClick = { menuOpen = true },
                contentDescription = "Skill options",
                accent = accent,
                size = 38.dp,
            )
            DropdownMenu(
                expanded = menuOpen,
                onDismissRequest = { menuOpen = false },
                containerColor = ClayPalette.SurfaceCream,
                shape = ClayShape.Medium,
            ) {
                DropdownMenuItem(
                    text = { Text("Rename", style = MaterialTheme.typography.bodyMedium) },
                    onClick = { menuOpen = false; onEdit() },
                )
                if (!row.attribute.isBuiltIn) {
                    DropdownMenuItem(
                        text = {
                            Text(
                                "Remove",
                                style = MaterialTheme.typography.bodyMedium,
                                color = ClayPalette.Blush.deep,
                            )
                        },
                        onClick = { menuOpen = false; onDelete() },
                    )
                }
            }
        }
    }
}

/**
 * Core attributes get a fixed colour each, so the same skill always reads in the
 * same colour across dashboard, skills and analytics. Custom skills use the
 * colour chosen by the player.
 */
private fun accentForSkill(attribute: AttributeEntity): ClayAccent = when (attribute.attributeKind) {
    AttributeKind.Strength.name -> ClayPalette.Peach
    AttributeKind.Intellect.name -> ClayPalette.Sky
    AttributeKind.Focus.name -> ClayPalette.Lavender
    AttributeKind.Vitality.name -> ClayPalette.Mint
    AttributeKind.Creativity.name -> ClayPalette.Butter
    AttributeKind.Discipline.name -> ClayPalette.Blush
    else -> ClayPalette.accentById(attribute.accentId)
}

@Composable
private fun SkillDialog(
    title: String,
    initialName: String,
    initialEmoji: String,
    initialAccent: String,
    onDismiss: () -> Unit,
    onConfirm: (String, String, String) -> Unit,
) {
    var name by remember { mutableStateOf(initialName) }
    var emoji by remember { mutableStateOf(initialEmoji) }
    var accentId by remember { mutableStateOf(initialAccent) }
    val suggestions = listOf("Guitar" to "🎸", "Chess" to "♟️", "Spanish" to "🇪🇸", "Drawing" to "🖌️", "Cooking" to "🍳")

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = ClayPalette.SurfacePure,
        shape = ClayShape.Card,
        title = {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = ClayInk.Primary,
            )
        },
        text = {
            Column {
                ClayTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = "Skill name",
                    placeholder = "e.g. Guitar, Chess, Spanish",
                    accent = ClayPalette.accentById(accentId),
                )
                if (initialName.isBlank()) {
                    Spacer(Modifier.height(10.dp))
                    Text(
                        text = "Quick start",
                        style = MaterialTheme.typography.labelSmall,
                        color = ClayInk.Faint,
                        modifier = Modifier.padding(start = 4.dp, bottom = 6.dp),
                    )
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(suggestions) { (label, suggestionEmoji) ->
                            ClayChip(
                                text = label,
                                selected = name.equals(label, ignoreCase = true),
                                onClick = {
                                    name = label
                                    emoji = suggestionEmoji
                                },
                                accent = ClayPalette.accentById(accentId),
                                leadingEmoji = suggestionEmoji,
                            )
                        }
                    }
                }
                Spacer(Modifier.height(14.dp))
                Text(
                    text = "Icon",
                    style = MaterialTheme.typography.labelMedium,
                    color = ClayInk.Secondary,
                    modifier = Modifier.padding(start = 4.dp, bottom = 8.dp),
                )
                EmojiRow(selected = emoji, onSelect = { emoji = it })
                Spacer(Modifier.height(14.dp))
                Text(
                    text = "Colour",
                    style = MaterialTheme.typography.labelMedium,
                    color = ClayInk.Secondary,
                    modifier = Modifier.padding(start = 4.dp, bottom = 8.dp),
                )
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(ClayPalette.accentRotation.map { ClayPalette.accentIdOf(it) }) { id ->
                        ClayChip(
                            text = id.replaceFirstChar { it.uppercase() },
                            selected = accentId == id,
                            onClick = { accentId = id },
                            accent = ClayPalette.accentById(id),
                        )
                    }
                }
            }
        },
        confirmButton = {
            ClayButton(
                text = "Save",
                onClick = { onConfirm(name, emoji, accentId) },
                accent = ClayPalette.accentById(accentId),
                height = 44.dp,
                enabled = name.isNotBlank(),
            )
        },
        dismissButton = {
            ClayButton(
                text = "Cancel",
                onClick = onDismiss,
                accent = ClayPalette.Lavender,
                style = ClayButtonStyle.Plain,
                height = 44.dp,
            )
        },
    )
}
