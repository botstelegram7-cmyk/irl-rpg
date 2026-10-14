package com.irlrpg.app.ui.profile

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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
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
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.irlrpg.app.core.DateX
import com.irlrpg.app.data.local.entity.ProgressionEventEntity
import com.irlrpg.app.domain.game.CosmeticCatalog
import com.irlrpg.app.ui.components.ClayToast
import com.irlrpg.app.ui.design.ClayAvatarPedestal
import com.irlrpg.app.ui.design.ClayButton
import com.irlrpg.app.ui.design.ClayButtonStyle
import com.irlrpg.app.ui.design.ClayCard
import com.irlrpg.app.ui.design.ClayDivider
import com.irlrpg.app.ui.design.ClayDot
import com.irlrpg.app.ui.design.ClayIconButton
import com.irlrpg.app.ui.design.ClayIconPill
import com.irlrpg.app.ui.design.ClayIconTile
import com.irlrpg.app.ui.design.ClayMiniStat
import com.irlrpg.app.ui.design.ClayPanel
import com.irlrpg.app.ui.design.ClayProgressBar
import com.irlrpg.app.ui.design.ClayScaffold
import com.irlrpg.app.ui.design.ClaySectionTitle
import com.irlrpg.app.ui.design.ClayTextField
import com.irlrpg.app.ui.design.ClayTopBar
import com.irlrpg.app.ui.theme.ClayAccent
import com.irlrpg.app.ui.theme.ClayInk
import com.irlrpg.app.ui.theme.ClayPalette
import com.irlrpg.app.ui.theme.ClayShape
import java.time.LocalDate

/**
 * Character screen.
 *
 * The avatar is a layered 2D illustration ([ClayAvatarPedestal]) whose ring
 * colour is driven by the equipped cosmetic frame — so cosmetics added later
 * change the artwork without touching this layout. Level, rank and XP are shown
 * once, in one place, and the remaining rows are quiet statistics rather than a
 * wall of raised cards.
 */
@Composable
fun ProfileScreen(
    viewModel: ProfileViewModel,
    bottomBar: @Composable () -> Unit,
    onOpenSkills: () -> Unit,
    onOpenSkillTree: () -> Unit = {},
    onOpenAchievements: () -> Unit = {},
    onOpenChallenges: () -> Unit = {},
    onOpenHistory: () -> Unit = {},
    onOpenCustomize: () -> Unit = {},
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val message by viewModel.message.collectAsStateWithLifecycle()
    var renaming by remember { mutableStateOf(false) }
    var nameDraft by remember { mutableStateOf("") }
    val compact = LocalConfiguration.current.screenWidthDp < 360

    ClayScaffold(
        topBar = {
            ClayTopBar(
                title = "Character",
                subtitle = "Stored locally on this device",
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
        val player = state.player
        val frame = CosmeticCatalog.byCode(player?.stats?.cosmeticFrame)
        val title = CosmeticCatalog.byCode(player?.stats?.displayTitle)
        val frameAccent = frame?.let { ClayPalette.accentById(it.accentId) } ?: ClayPalette.Blush

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
                    shape = ClayShape.Hero,
                    elevation = 18.dp,
                    glowAlpha = 0.3f,
                    contentPadding = PaddingValues(20.dp),
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        ClayAvatarPedestal(
                            emoji = frame?.emoji ?: "🌱",
                            accent = ClayPalette.Blush,
                            frameAccent = frameAccent,
                            progress = player?.progress ?: 0f,
                            size = if (compact) 124.dp else 152.dp,
                            badge = "LV ${player?.level ?: 1}",
                        )
                        Spacer(Modifier.width(if (compact) 8.dp else 14.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = player?.stats?.name ?: "Adventurer",
                                style = MaterialTheme.typography.titleLarge,
                                color = ClayInk.Primary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            Text(
                                text = "${player?.rankEmoji ?: "🌱"} ${player?.rankTitle ?: "Wanderer"}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = ClayInk.Secondary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            Spacer(Modifier.height(8.dp))
                            ClayIconPill(
                                text = title?.name ?: "Rank title",
                                accent = ClayPalette.Lavender,
                                emoji = if (title != null) "🎖️" else null,
                                emphasised = title != null,
                            )
                        }
                    }

                    Spacer(Modifier.height(16.dp))
                    ClayPanel(
                        shape = ClayShape.Small,
                        tint = ClayPalette.SurfaceMuted.copy(alpha = 0.65f),
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            ClayMiniStat(
                                value = "${player?.xpIntoLevel ?: 0}",
                                label = "XP into level",
                                accent = ClayPalette.Lavender,
                                modifier = Modifier.weight(1f),
                            )
                            ClayMiniStat(
                                value = "${player?.xpForCurrentLevel ?: 0}",
                                label = "Level cost",
                                accent = ClayPalette.Sky,
                                modifier = Modifier.weight(1f),
                            )
                            ClayMiniStat(
                                value = "${player?.stats?.totalXp ?: 0}",
                                label = "Total XP",
                                accent = ClayPalette.Butter,
                                modifier = Modifier.weight(1f),
                            )
                        }
                        Spacer(Modifier.height(10.dp))
                        ClayProgressBar(
                            progress = player?.progress ?: 0f,
                            accent = ClayPalette.Lavender,
                            height = 11.dp,
                        )
                        Spacer(Modifier.height(7.dp))
                        Text(
                            text = "Next level at ${player?.nextLevelXp ?: 0} XP — progress is computed from your recorded quests only.",
                            style = MaterialTheme.typography.labelSmall,
                            color = ClayInk.Muted,
                            maxLines = 2,
                        )
                    }

                    Spacer(Modifier.height(14.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        ClayButton(
                            text = "Rename",
                            onClick = {
                                nameDraft = player?.stats?.name ?: ""
                                renaming = true
                            },
                            accent = ClayPalette.Lavender,
                            style = ClayButtonStyle.Soft,
                            height = 42.dp,
                            modifier = Modifier.weight(1f),
                        )
                        ClayButton(
                            text = "Skills",
                            onClick = onOpenSkills,
                            accent = ClayPalette.Mint,
                            style = ClayButtonStyle.Soft,
                            height = 42.dp,
                            modifier = Modifier.weight(1f),
                        )
                        ClayButton(
                            text = "Looks",
                            onClick = onOpenCustomize,
                            accent = ClayPalette.Peach,
                            style = ClayButtonStyle.Soft,
                            height = 42.dp,
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
            }

            item {
                ClayCard(
                    shape = ClayShape.Card,
                    elevation = 12.dp,
                    glowAlpha = 0.14f,
                    contentPadding = PaddingValues(18.dp),
                ) {
                    ClaySectionTitle(
                        title = "Growth",
                        eyebrow = "Overview",
                        caption = "Counts come straight from your quest log",
                    )
                    Spacer(Modifier.height(12.dp))
                    Row {
                        ClayMiniStat(
                            value = "${state.levelUps}",
                            label = "Level-ups",
                            emoji = "🎉",
                            accent = ClayPalette.Butter,
                            modifier = Modifier.weight(1f),
                        )
                        ClayMiniStat(
                            value = "${state.activeQuestCount}/${state.questCount}",
                            label = "Active quests",
                            emoji = "🧭",
                            accent = ClayPalette.Lavender,
                            modifier = Modifier.weight(1f),
                        )
                        ClayMiniStat(
                            value = "${state.logCount}",
                            label = "Logged entries",
                            emoji = "📝",
                            accent = ClayPalette.Sky,
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
            }

            item {
                ClayCard(
                    shape = ClayShape.Card,
                    elevation = 12.dp,
                    glowAlpha = 0.14f,
                    contentPadding = PaddingValues(18.dp),
                ) {
                    ClaySectionTitle(
                        title = "Grow your character",
                        eyebrow = "Sections",
                        caption = "Everything below is driven by the activity you record",
                    )
                    Spacer(Modifier.height(8.dp))
                    ProfileLink("🌳", "Skill tree", "Nodes, prerequisites and mastery", ClayPalette.Mint, onOpenSkillTree)
                    ProfileLink("🏔️", "Challenges", "Multi-objective goals with one reward", ClayPalette.Peach, onOpenChallenges)
                    ProfileLink("🏅", "Achievements", "Unlocked from your real history", ClayPalette.Butter, onOpenAchievements)
                    ProfileLink("🕰️", "Completion history", "Every logged entry, day by day", ClayPalette.Sky, onOpenHistory)
                    ProfileLink("🎨", "Customize", "Titles and avatar frames", ClayPalette.Lavender, onOpenCustomize, isLast = true)
                }
            }

            item {
                ClayCard(
                    shape = ClayShape.Card,
                    elevation = 12.dp,
                    glowAlpha = 0.14f,
                    contentPadding = PaddingValues(18.dp),
                ) {
                    ClaySectionTitle(
                        title = "Progression history",
                        eyebrow = "Timeline",
                        caption = if (state.history.isEmpty()) {
                            "Your first completion starts the log"
                        } else {
                            "Newest first · ${state.history.size} recorded events"
                        },
                    )
                    if (state.history.isEmpty()) {
                        // Compact, single-line empty state — no card inside a card.
                        Spacer(Modifier.height(12.dp))
                        ClayPanel(
                            shape = ClayShape.Small,
                            tint = ClayPalette.SurfaceMuted.copy(alpha = 0.6f),
                        ) {
                            Text(
                                text = "Nothing recorded yet. Complete a quest and the timeline fills in automatically.",
                                style = MaterialTheme.typography.bodySmall,
                                color = ClayInk.Muted,
                            )
                        }
                    } else {
                        Spacer(Modifier.height(6.dp))
                        state.history.take(6).forEachIndexed { index, event ->
                            if (index > 0) ClayDivider(verticalPadding = 0.dp)
                            TimelineRow(
                                event = event,
                                isLast = index == state.history.take(6).lastIndex,
                            )
                        }
                    }
                }
            }

            item {
                ClayCard(
                    shape = ClayShape.Card,
                    elevation = 10.dp,
                    glowAlpha = 0.1f,
                    contentPadding = PaddingValues(18.dp),
                ) {
                    ClaySectionTitle(
                        title = "About & your data",
                        eyebrow = "Offline",
                        caption = "No account, no ads, no tracking, no paid service",
                    )
                    Spacer(Modifier.height(12.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        MiniCount("Quests", state.questCount, ClayPalette.Lavender, Modifier.weight(1f))
                        MiniCount("Categories", state.categoriesCount, ClayPalette.Sky, Modifier.weight(1f))
                        MiniCount("Skills", state.skillCount, ClayPalette.Butter, Modifier.weight(1f))
                    }
                    Spacer(Modifier.height(12.dp))
                    state.player?.stats?.let { stats ->
                        Text(
                            text = "Tracking since ${DateX.fullDate(LocalDate.ofEpochDay(stats.createdAtEpochDay))} · everything stays on this device",
                            style = MaterialTheme.typography.labelSmall,
                            color = ClayInk.Faint,
                            maxLines = 2,
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
private fun ProfileLink(
    emoji: String,
    title: String,
    subtitle: String,
    accent: ClayAccent,
    onClick: () -> Unit,
    isLast: Boolean = false,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(ClayShape.Medium)
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Flat tinted tile: rows inside a card should not raise another card.
        Box(
            modifier = Modifier
                .size(42.dp)
                .clip(ClayShape.Pill)
                .background(accent.soft.copy(alpha = 0.7f)),
            contentAlignment = Alignment.Center,
        ) {
            Text(text = emoji, fontSize = 19.sp)
        }
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                color = ClayInk.Primary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.labelSmall,
                color = ClayInk.Muted,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Icon(
            imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
            contentDescription = null,
            tint = ClayInk.Faint,
            modifier = Modifier.size(20.dp),
        )
    }
    if (!isLast) ClayDivider(verticalPadding = 0.dp, color = ClayInk.Faint.copy(alpha = 0.16f))
}

@Composable
private fun TimelineRow(event: ProgressionEventEntity, isLast: Boolean) {
    val isLevelUp = event.type == "level_up"
    val accent = when {
        isLevelUp -> ClayPalette.Butter
        event.type == "undo" -> ClayPalette.Peach
        else -> ClayPalette.Mint
    }
    Row(modifier = Modifier.fillMaxWidth()) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            ClayDot(
                emoji = if (isLevelUp) "🎉" else if (event.type == "undo") "↩️" else "⚡",
                accent = accent,
                size = 34.dp,
            )
            if (!isLast) {
                Box(
                    modifier = Modifier
                        .size(width = 2.dp, height = 30.dp)
                        .background(ClayInk.Faint.copy(alpha = 0.25f)),
                )
            }
        }
        Spacer(Modifier.width(12.dp))
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(top = 4.dp, bottom = 14.dp),
        ) {
            Text(
                text = event.title,
                style = MaterialTheme.typography.bodyMedium,
                color = ClayInk.Primary,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = listOfNotNull(
                    event.detail,
                    DateX.relativeLabel(LocalDate.ofEpochDay(event.epochDay)),
                ).joinToString(" · "),
                style = MaterialTheme.typography.labelSmall,
                color = ClayInk.Muted,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
        if (event.amount != 0) {
            Spacer(Modifier.width(8.dp))
            Box(
                modifier = Modifier
                    .padding(top = 4.dp)
                    .clip(ClayShape.Pill)
                    .background(accent.soft)
                    .padding(horizontal = 10.dp, vertical = 5.dp),
            ) {
                Text(
                    text = if (event.amount > 0) "+${event.amount} XP" else "${event.amount} XP",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = accent.deep,
                    maxLines = 1,
                )
            }
        }
    }
}

@Composable
private fun MiniCount(
    label: String,
    value: Int,
    accent: ClayAccent,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .clip(ClayShape.Small)
            .background(accent.soft.copy(alpha = 0.6f))
            .padding(horizontal = 12.dp, vertical = 9.dp),
    ) {
        Text(
            text = "$value",
            style = MaterialTheme.typography.titleSmall,
            color = ClayInk.Primary,
            maxLines = 1,
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = ClayInk.Muted,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}
