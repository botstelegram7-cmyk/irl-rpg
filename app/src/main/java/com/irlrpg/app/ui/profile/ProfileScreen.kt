package com.irlrpg.app.ui.profile

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
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material3.AlertDialog
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.irlrpg.app.core.DateX
import com.irlrpg.app.data.local.entity.ProgressionEventEntity
import com.irlrpg.app.ui.components.ClayToast
import com.irlrpg.app.ui.design.ClayAvatar
import com.irlrpg.app.ui.design.ClayButton
import com.irlrpg.app.ui.design.ClayButtonStyle
import com.irlrpg.app.ui.design.ClayCard
import com.irlrpg.app.ui.design.ClayIconButton
import com.irlrpg.app.ui.design.ClayIconTile
import com.irlrpg.app.ui.design.ClayProgressBar
import com.irlrpg.app.ui.design.ClayScaffold
import com.irlrpg.app.ui.design.ClaySectionHeader
import com.irlrpg.app.ui.design.ClayTextField
import com.irlrpg.app.ui.design.ClayTopBar
import com.irlrpg.app.ui.theme.ClayInk
import com.irlrpg.app.ui.theme.ClayPalette
import com.irlrpg.app.ui.theme.ClayShape
import java.time.LocalDate

@Composable
fun ProfileScreen(
    viewModel: ProfileViewModel,
    bottomBar: @Composable () -> Unit,
    onOpenSkills: () -> Unit,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val message by viewModel.message.collectAsStateWithLifecycle()
    var renaming by remember { mutableStateOf(false) }
    var nameDraft by remember { mutableStateOf("") }

    ClayScaffold(
        topBar = {
            ClayTopBar(
                title = "Character",
                subtitle = "Your progression, stored locally on this device",
                accent = ClayPalette.Blush,
                actions = {
                    ClayIconButton(
                        icon = Icons.Rounded.Edit,
                        onClick = {
                            nameDraft = state.player?.stats?.name ?: ""
                            renaming = true
                        },
                        contentDescription = "Rename character",
                        accent = ClayPalette.Blush,
                        size = 42.dp,
                    )
                },
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
            val player = state.player
            item {
                ClayCard(shape = ClayShape.Hero, elevation = 18.dp, glowAlpha = 0.3f, contentPadding = PaddingValues(22.dp)) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        ClayAvatar(
                            emoji = player?.rankEmoji ?: "🌱",
                            level = player?.level ?: 1,
                            progress = player?.progress ?: 0f,
                            accent = ClayPalette.Blush,
                            avatarSize = 120.dp,
                        )
                        Spacer(Modifier.height(14.dp))
                        Text(
                            text = player?.stats?.name ?: "Adventurer",
                            style = MaterialTheme.typography.headlineSmall,
                            color = ClayInk.Primary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Text(
                            text = "${player?.rankTitle ?: "Wanderer"} · Level ${player?.level ?: 1}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = ClayPalette.Lavender.deep,
                        )
                        Spacer(Modifier.height(16.dp))
                        ClayProgressBar(
                            progress = player?.progress ?: 0f,
                            accent = ClayPalette.Lavender,
                            height = 13.dp,
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = "${player?.xpIntoLevel ?: 0} / ${player?.xpForCurrentLevel ?: 0} XP towards level ${(player?.level ?: 1) + 1} · ${
                                player?.stats?.totalXp ?: 0
                            } XP lifetime",
                            style = MaterialTheme.typography.labelSmall,
                            color = ClayInk.Muted,
                        )
                        Spacer(Modifier.height(16.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            ClayButton(
                                text = "Rename",
                                onClick = {
                                    nameDraft = player?.stats?.name ?: ""
                                    renaming = true
                                },
                                accent = ClayPalette.Lavender,
                                style = ClayButtonStyle.Soft,
                                height = 44.dp,
                            )
                            ClayButton(
                                text = "Skills",
                                onClick = onOpenSkills,
                                accent = ClayPalette.Butter,
                                style = ClayButtonStyle.Soft,
                                height = 44.dp,
                            )
                        }
                    }
                }
            }

            item {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    StatTile("⚡", "${state.player?.stats?.totalXp ?: 0}", "Total XP", ClayPalette.Butter, Modifier.weight(1f))
                    StatTile("🎉", "${state.levelUps}", "Level-ups earned", ClayPalette.Mint, Modifier.weight(1f))
                }
            }

            item {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    StatTile("🧩", "${state.activeQuestCount}/${state.questCount}", "Active quests", ClayPalette.Lavender, Modifier.weight(1f))
                    StatTile("📝", "${state.logCount}", "Logged entries", ClayPalette.Sky, Modifier.weight(1f))
                }
            }

            item {
                ClayCard(shape = ClayShape.Card, elevation = 12.dp) {
                    ClaySectionHeader(
                        title = "Progression history",
                        subtitle = if (state.history.isEmpty()) "Level-ups and XP awards will be listed here" else "Newest first",
                    )
                    Spacer(Modifier.height(12.dp))
                    if (state.history.isEmpty()) {
                        Text(
                            text = "Nothing recorded yet — your first quest completion starts the log.",
                            style = MaterialTheme.typography.bodySmall,
                            color = ClayInk.Muted,
                        )
                    }
                }
            }

            items(state.history.take(15), key = { it.id }) { event ->
                HistoryRow(event)
            }

            item {
                ClayCard(shape = ClayShape.Card, elevation = 12.dp) {
                    ClaySectionHeader(
                        title = "About & your data",
                        subtitle = "IRL RPG works fully offline",
                    )
                    Spacer(Modifier.height(12.dp))
                    Text(
                        text = "No account, no ads, no tracking and no paid service. Everything you record lives in a local SQLite database on this device and stays there.",
                        style = MaterialTheme.typography.bodySmall,
                        color = ClayInk.Muted,
                    )
                    Spacer(Modifier.height(14.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        MiniCount("Quests", state.questCount, ClayPalette.Lavender)
                        MiniCount("Categories", state.categoriesCount, ClayPalette.Sky)
                        MiniCount("Skills", state.skillCount, ClayPalette.Butter)
                    }
                    Spacer(Modifier.height(12.dp))
                    state.player?.stats?.let { stats ->
                        Text(
                            text = "Tracking since ${DateX.fullDate(LocalDate.ofEpochDay(stats.createdAtEpochDay))}",
                            style = MaterialTheme.typography.labelSmall,
                            color = ClayInk.Faint,
                        )
                    }
                }
            }
        }
    }

    if (renaming) {
        AlertDialog(
            onDismissRequest = { renaming = false },
            containerColor = ClayPalette.SurfacePure,
            shape = ClayShape.Card,
            title = {
                Text(
                    text = "Character name",
                    style = MaterialTheme.typography.titleMedium,
                    color = ClayInk.Primary,
                )
            },
            text = {
                ClayTextField(
                    value = nameDraft,
                    onValueChange = { nameDraft = it },
                    label = "Name",
                    placeholder = "Adventurer",
                    accent = ClayPalette.Blush,
                )
            },
            confirmButton = {
                ClayButton(
                    text = "Save",
                    onClick = {
                        viewModel.rename(nameDraft)
                        renaming = false
                    },
                    accent = ClayPalette.Blush,
                    height = 44.dp,
                )
            },
            dismissButton = {
                ClayButton(
                    text = "Cancel",
                    onClick = { renaming = false },
                    accent = ClayPalette.Lavender,
                    style = ClayButtonStyle.Plain,
                    height = 44.dp,
                )
            },
        )
    }

    ClayToast(message = message, onShown = viewModel::consumeMessage, bottomPadding = 104.dp)
}

@Composable
private fun StatTile(
    emoji: String,
    value: String,
    label: String,
    accent: com.irlrpg.app.ui.theme.ClayAccent,
    modifier: Modifier = Modifier,
) {
    ClayCard(
        modifier = modifier,
        shape = ClayShape.Medium,
        elevation = 10.dp,
        contentPadding = PaddingValues(14.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            ClayIconTile(emoji = emoji, accent = accent, size = 36.dp, fontSize = 16.sp)
            Spacer(Modifier.width(10.dp))
            Column {
                Text(
                    text = value,
                    style = MaterialTheme.typography.titleMedium,
                    color = ClayInk.Primary,
                    maxLines = 1,
                )
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall,
                    color = ClayInk.Muted,
                    maxLines = 1,
                )
            }
        }
    }
}

@Composable
private fun HistoryRow(event: ProgressionEventEntity) {
    val isLevelUp = event.type == "level_up"
    val accent = when {
        isLevelUp -> ClayPalette.Butter
        event.type == "undo" -> ClayPalette.Peach
        else -> ClayPalette.Mint
    }
    ClayCard(
        shape = ClayShape.Medium,
        elevation = 9.dp,
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            ClayIconTile(
                emoji = if (isLevelUp) "🎉" else if (event.type == "undo") "↩️" else "⚡",
                accent = accent,
                size = 40.dp,
                fontSize = 18.sp,
            )
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = event.title,
                    style = MaterialTheme.typography.titleSmall,
                    color = ClayInk.Primary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = listOfNotNull(
                        event.detail,
                        DateX.relativeLabel(LocalDate.ofEpochDay(event.epochDay)),
                    ).joinToString(" · "),
                    style = MaterialTheme.typography.labelSmall,
                    color = ClayInk.Muted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            if (event.amount != 0) {
                Box(
                    modifier = Modifier
                        .clip(ClayShape.Pill)
                        .background(accent.soft)
                        .padding(horizontal = 10.dp, vertical = 5.dp),
                ) {
                    Text(
                        text = if (event.amount > 0) "+${event.amount} XP" else "${event.amount} XP",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = accent.deep,
                    )
                }
            }
        }
    }
}

@Composable
private fun MiniCount(label: String, value: Int, accent: com.irlrpg.app.ui.theme.ClayAccent) {
    Column(
        modifier = Modifier
            .clip(ClayShape.Small)
            .background(accent.soft.copy(alpha = 0.6f))
            .padding(horizontal = 12.dp, vertical = 9.dp),
    ) {
        Text(
            text = "$value",
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
