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
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.irlrpg.app.data.local.entity.AttributeEntity
import com.irlrpg.app.ui.components.ClayConfirmDialog
import com.irlrpg.app.ui.components.ClayToast
import com.irlrpg.app.ui.design.ClayButton
import com.irlrpg.app.ui.design.ClayButtonStyle
import com.irlrpg.app.ui.design.ClayCard
import com.irlrpg.app.ui.design.ClayChip
import com.irlrpg.app.ui.design.ClayEmptyState
import com.irlrpg.app.ui.design.ClayIconButton
import com.irlrpg.app.ui.design.ClayIconTile
import com.irlrpg.app.ui.design.ClayProgressBar
import com.irlrpg.app.ui.design.ClayScaffold
import com.irlrpg.app.ui.design.ClaySectionHeader
import com.irlrpg.app.ui.design.ClayTextField
import com.irlrpg.app.ui.design.ClayTopBar
import com.irlrpg.app.ui.quests.EmojiRow
import com.irlrpg.app.ui.theme.ClayInk
import com.irlrpg.app.ui.theme.ClayPalette
import com.irlrpg.app.ui.theme.ClayShape

@Composable
fun SkillsScreen(
    viewModel: SkillsViewModel,
    bottomBar: @Composable () -> Unit,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val message by viewModel.message.collectAsStateWithLifecycle()
    var showAdd by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf<AttributeEntity?>(null) }
    var pendingDelete by remember { mutableStateOf<AttributeEntity?>(null) }

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
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            item {
                ClayCard(shape = ClayShape.Card, elevation = 14.dp) {
                    val totalXp = state.rows.sumOf { it.xp }
                    val top = state.rows.maxByOrNull { it.xp }
                    ClaySectionHeader(
                        title = "Skill profile",
                        subtitle = "Built from ${state.rows.sumOf { it.completions }} completed entries",
                    )
                    Spacer(Modifier.height(14.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        MiniTile("⚡", "$totalXp", "XP in skills", ClayPalette.Butter, Modifier.weight(1f))
                        MiniTile(
                            "🏅",
                            top?.attribute?.emoji ?: "—",
                            top?.attribute?.name ?: "No data yet",
                            ClayPalette.Mint,
                            Modifier.weight(1f),
                        )
                    }
                    Spacer(Modifier.height(16.dp))
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

            if (state.rows.isEmpty() && !state.loading) {
                item {
                    ClayEmptyState(
                        emoji = "🧠",
                        title = "No skills yet",
                        message = "Skills appear automatically from the six core attributes — add your own if you want to track something specific like guitar or chess.",
                        actionLabel = "Add a skill",
                        onAction = { showAdd = true },
                    )
                }
            } else {
                items(state.rows, key = { it.attribute.id }) { row ->
                    SkillCard(
                        row = row,
                        onEdit = { editing = row.attribute },
                        onDelete = { pendingDelete = row.attribute },
                    )
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

@Composable
private fun MiniTile(
    emoji: String,
    value: String,
    label: String,
    accent: com.irlrpg.app.ui.theme.ClayAccent,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .clip(ClayShape.Small)
            .background(accent.soft.copy(alpha = 0.6f))
            .padding(12.dp),
    ) {
        Text(text = emoji, style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(4.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
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
private fun SkillCard(
    row: SkillRow,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
) {
    var menuOpen by remember { mutableStateOf(false) }
    val accent = ClayPalette.accentById(row.attribute.accentId)

    ClayCard(shape = ClayShape.Card, elevation = 12.dp) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            ClayIconTile(emoji = row.attribute.emoji, accent = accent, size = 48.dp)
            Spacer(Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = row.attribute.name,
                        style = MaterialTheme.typography.titleSmall,
                        color = ClayInk.Primary,
                    )
                    if (row.attribute.isBuiltIn) {
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = "core",
                            style = MaterialTheme.typography.labelSmall,
                            color = ClayInk.Faint,
                        )
                    }
                }
                Text(
                    text = "${row.activeQuestCount} active ${if (row.activeQuestCount == 1) "quest" else "quests"} · ${row.completions} completions",
                    style = MaterialTheme.typography.labelSmall,
                    color = ClayInk.Muted,
                )
            }
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
        Spacer(Modifier.height(14.dp))
        ClayProgressBar(progress = row.share, accent = accent, height = 10.dp)
        Spacer(Modifier.height(8.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "${row.xp} XP",
                style = MaterialTheme.typography.labelMedium,
                color = accent.deep,
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = "${(row.share * 100).toInt()}% of your skill XP",
                style = MaterialTheme.typography.labelSmall,
                color = ClayInk.Faint,
            )
        }
    }
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
                Spacer(Modifier.height(14.dp))
                Text(
                    text = "Icon",
                    style = MaterialTheme.typography.labelMedium,
                    color = ClayInk.Secondary,
                    modifier = Modifier.padding(start = 4.dp, bottom = 8.dp),
                )
                EmojiRow(selected = emoji, onSelect = { emoji = it })
                Spacer(Modifier.height(14.dp))
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
