package com.irlrpg.app.ui.dashboard

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.KeyboardArrowRight
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.irlrpg.app.core.DateX
import com.irlrpg.app.domain.model.DashboardState
import com.irlrpg.app.domain.model.QuestWithProgress
import com.irlrpg.app.domain.model.RecentCompletion
import com.irlrpg.app.domain.model.TrackingKind
import com.irlrpg.app.ui.common.accentFor
import com.irlrpg.app.ui.components.CelebrationOverlay
import com.irlrpg.app.ui.components.QuickLogSheet
import com.irlrpg.app.ui.design.ClayBar
import com.irlrpg.app.ui.design.ClayBarChart
import com.irlrpg.app.ui.design.ClayButton
import com.irlrpg.app.ui.design.ClayButtonStyle
import com.irlrpg.app.ui.design.ClayCard
import com.irlrpg.app.ui.design.ClayEmptyState
import com.irlrpg.app.ui.design.ClayFab
import com.irlrpg.app.ui.design.ClayIconTile
import com.irlrpg.app.ui.design.ClayProgressBar
import com.irlrpg.app.ui.design.ClayScaffold
import com.irlrpg.app.ui.design.ClaySectionHeader
import com.irlrpg.app.ui.design.ClayStatTile
import com.irlrpg.app.ui.theme.ClayAccent
import com.irlrpg.app.ui.theme.ClayInk
import com.irlrpg.app.ui.theme.ClayPalette
import com.irlrpg.app.ui.theme.ClayShape
import java.time.LocalDate

@Composable
fun DashboardScreen(
    viewModel: DashboardViewModel,
    bottomBar: @Composable () -> Unit,
    onNewQuest: () -> Unit,
    onOpenQuest: (Long) -> Unit,
    onOpenAnalytics: () -> Unit,
    onOpenQuests: () -> Unit,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val celebration by viewModel.celebration.collectAsStateWithLifecycle()
    val busyQuestId by viewModel.busyQuestId.collectAsStateWithLifecycle()
    var quickLogItem by remember { mutableStateOf<QuestWithProgress?>(null) }

    LifecycleResumeEffect(Unit) {
        viewModel.refreshDay()
        onPauseOrDispose { }
    }

    ClayScaffold(
        topBar = { GreetingHeader(state) },
        bottomBar = bottomBar,
    ) { padding ->
        val current = state
        if (current == null) {
            LoadingDashboard(padding)
            return@ClayScaffold
        }
        Box(modifier = Modifier.fillMaxSize()) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    start = 20.dp,
                    end = 20.dp,
                    top = padding.calculateTopPadding() + 4.dp,
                    bottom = padding.calculateBottomPadding() + 96.dp,
                ),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                item { PlayerHeroCard(current) }

                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        ClayStatTile(
                            emoji = "⚡",
                            value = "${current.today.xp}",
                            label = "XP today",
                            accent = ClayPalette.Butter,
                            modifier = Modifier.weight(1f),
                        )
                        ClayStatTile(
                            emoji = "✅",
                            value = "${current.completedToday}/${current.plannedToday}",
                            label = "Quests today",
                            accent = ClayPalette.Mint,
                            modifier = Modifier.weight(1f),
                        )
                        ClayStatTile(
                            emoji = "⏳",
                            value = if (current.today.minutes > 0) DateX.formatMinutes(current.today.minutes) else "—",
                            label = "Time tracked",
                            accent = ClayPalette.Sky,
                            modifier = Modifier.weight(1f),
                        )
                    }
                }

                item {
                    ClaySectionHeader(
                        title = "Today's quests",
                        subtitle = if (current.plannedToday == 0) {
                            "Nothing scheduled — create your first quest"
                        } else {
                            "${current.completedToday} of ${current.plannedToday} complete"
                        },
                        actionLabel = "New",
                        onAction = onNewQuest,
                    )
                }

                if (current.todayQuests.isEmpty()) {
                    item {
                        ClayEmptyState(
                            emoji = "🧭",
                            title = "Your quest board is empty",
                            message = "Create an activity you want to grow — reading, coding, training, writing, anything measurable.",
                            actionLabel = "Create a quest",
                            onAction = onNewQuest,
                        )
                    }
                } else {
                    items(current.todayQuests, key = { it.quest.id }) { item ->
                        TodayQuestCard(
                            item = item,
                            busy = busyQuestId == item.quest.id,
                            onOpen = { onOpenQuest(item.quest.id) },
                            onQuickLog = {
                                if (item.quest.trackingKind == TrackingKind.Milestone.name) {
                                    viewModel.log(item.quest.id, 1L)
                                } else {
                                    quickLogItem = item
                                }
                            },
                        )
                    }
                }

                item { WeeklySummaryCard(current) }
                item { RecentAchievementsCard(current.recentCompletions) }
                item { AnalyticsShortcutCard(current, onOpenAnalytics, onOpenQuests) }
            }

            ClayFab(
                emoji = "✨",
                onClick = onNewQuest,
                contentDescription = "New quest",
                accent = ClayPalette.Lavender,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 20.dp, bottom = 20.dp),
            )
        }
    }

    quickLogItem?.let { item ->
        QuickLogSheet(
            item = item,
            accent = accentFor(item),
            onDismiss = { quickLogItem = null },
            onLog = { amount ->
                viewModel.log(item.quest.id, amount)
                quickLogItem = null
            },
        )
    }

    CelebrationOverlay(result = celebration, onDismiss = viewModel::dismissCelebration)
}

@Composable
private fun LoadingDashboard(padding: PaddingValues) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(padding)
            .padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(148.dp)
                .clip(ClayShape.Hero)
                .background(ClayPalette.SurfaceMuted.copy(alpha = 0.6f))
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(96.dp)
                .clip(ClayShape.Card)
                .background(ClayPalette.SurfaceMuted.copy(alpha = 0.45f))
        )
    }
}

@Composable
private fun GreetingHeader(state: DashboardState?) {
    val hour = remember { java.time.LocalTime.now().hour }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(start = 22.dp, end = 20.dp, top = 10.dp, bottom = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "${DateX.greeting(hour)},",
                style = MaterialTheme.typography.bodyMedium,
                color = ClayInk.Muted,
            )
            Text(
                text = state?.player?.stats?.name ?: "Adventurer",
                style = MaterialTheme.typography.headlineSmall,
                color = ClayInk.Primary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = DateX.fullDate(LocalDate.now()),
                style = MaterialTheme.typography.bodySmall,
                color = ClayInk.Muted,
            )
        }
        val streak = state?.streak
        if (streak != null) {
            Box(
                modifier = Modifier
                    .clip(ClayShape.Pill)
                    .background(ClayPalette.Peach.soft)
                    .padding(horizontal = 14.dp, vertical = 9.dp),
            ) {
                Text(
                    text = if (streak.current > 0) "🔥 ${streak.current} day streak" else "🌱 Start today",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = ClayPalette.Peach.deep,
                )
            }
        }
    }
}

@Composable
private fun PlayerHeroCard(state: DashboardState) {
    val player = state.player
    ClayCard(
        shape = ClayShape.Hero,
        elevation = 20.dp,
        glowAlpha = 0.32f,
        contentPadding = PaddingValues(20.dp),
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            Canvas(modifier = Modifier.matchParentSize()) {
                drawRect(
                    brush = Brush.radialGradient(
                        colors = listOf(ClayPalette.Lavender.base.copy(alpha = 0.22f), Color.Transparent),
                        center = Offset(size.width * 0.92f, size.height * 0.08f),
                        radius = size.width * 0.7f,
                    )
                )
                drawRect(
                    brush = Brush.radialGradient(
                        colors = listOf(ClayPalette.Sky.base.copy(alpha = 0.18f), Color.Transparent),
                        center = Offset(size.width * 0.05f, size.height * 0.95f),
                        radius = size.width * 0.6f,
                    )
                )
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                com.irlrpg.app.ui.design.ClayAvatar(
                    emoji = player.rankEmoji,
                    level = player.level,
                    progress = player.progress,
                    accent = ClayPalette.Lavender,
                    avatarSize = 104.dp,
                )
                Spacer(Modifier.width(18.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "${player.rankTitle} · Level ${player.level}",
                        style = MaterialTheme.typography.titleMedium,
                        color = ClayInk.Primary,
                    )
                    Text(
                        text = "${player.stats.totalXp} XP lifetime",
                        style = MaterialTheme.typography.bodySmall,
                        color = ClayInk.Muted,
                    )
                    Spacer(Modifier.height(12.dp))
                    ClayProgressBar(
                        progress = player.progress,
                        accent = ClayPalette.Lavender,
                        height = 12.dp,
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = "${player.xpIntoLevel} / ${player.xpForCurrentLevel} XP to level ${player.level + 1}",
                        style = MaterialTheme.typography.labelSmall,
                        color = ClayInk.Secondary,
                    )
                    if (player.stats.totalXp == 0) {
                        Text(
                            text = "Log your first quest to start earning XP",
                            style = MaterialTheme.typography.labelSmall,
                            color = ClayInk.Faint,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TodayQuestCard(
    item: QuestWithProgress,
    busy: Boolean,
    onOpen: () -> Unit,
    onQuickLog: () -> Unit,
) {
    val accent = accentFor(item)
    val quest = item.quest
    ClayCard(
        shape = ClayShape.Card,
        elevation = if (item.isComplete) 8.dp else 13.dp,
        glowAlpha = if (item.isComplete) 0.12f else 0.22f,
        contentPadding = PaddingValues(16.dp),
        onClick = onOpen,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            ClayIconTile(
                emoji = quest.emoji,
                accent = if (item.isComplete) ClayPalette.Mint else accent,
                size = 50.dp,
            )
            Spacer(Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = quest.title,
                    style = MaterialTheme.typography.titleSmall,
                    color = ClayInk.Primary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = buildString {
                        append(item.category?.let { "${it.emoji} ${it.name}" } ?: "🗂️ Uncategorised")
                        append(" · ")
                        append(
                            if (quest.trackingKind == TrackingKind.Milestone.name) {
                                if (item.isComplete) "done" else "one-off"
                            } else {
                                "${item.progress.coerceAtMost(item.target)}/${item.target} ${item.quest.customUnit ?: ""}".trim()
                            }
                        )
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = ClayInk.Muted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Spacer(Modifier.width(10.dp))
            if (item.isComplete) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(ClayPalette.Mint.soft),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Check,
                        contentDescription = "Completed",
                        tint = ClayPalette.Mint.deep,
                        modifier = Modifier.size(22.dp),
                    )
                }
            } else {
                ClayButton(
                    text = if (quest.trackingKind == TrackingKind.Milestone.name) "Done" else "Log",
                    onClick = onQuickLog,
                    accent = accent,
                    height = 42.dp,
                    enabled = !busy,
                )
            }
        }
        if (quest.trackingKind != TrackingKind.Milestone.name) {
            Spacer(Modifier.height(12.dp))
            ClayProgressBar(
                progress = item.fraction,
                accent = if (item.isComplete) ClayPalette.Mint else accent,
                height = 9.dp,
            )
        }
    }
}

@Composable
private fun WeeklySummaryCard(state: DashboardState) {
    val today = LocalDate.now()
    val bars = state.week.map { day ->
        ClayBar(
            label = day.label,
            value = day.xp,
            highlight = day.date == today,
        )
    }
    ClayCard(shape = ClayShape.Card, elevation = 14.dp) {
        ClaySectionHeader(
            title = "This week",
            subtitle = "XP earned per day · real recorded sessions only",
        )
        Spacer(Modifier.height(16.dp))
        ClayBarChart(bars = bars, accent = ClayPalette.Sky, barArea = 104.dp)
        Spacer(Modifier.height(14.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            WeekMiniStat("⚡", "${state.weekXp}", "XP", ClayPalette.Butter, Modifier.weight(1f))
            WeekMiniStat("✅", "${state.weekCompletions}", "Done", ClayPalette.Mint, Modifier.weight(1f))
            WeekMiniStat(
                "⏳",
                if (state.weekMinutes > 0) DateX.formatMinutes(state.weekMinutes) else "—",
                "Time",
                ClayPalette.Sky,
                Modifier.weight(1f),
            )
        }
        if (state.lifetime.activeDays > 0) {
            Spacer(Modifier.height(12.dp))
            Text(
                text = "${state.lifetime.activeDays} active ${if (state.lifetime.activeDays == 1) "day" else "days"} recorded" +
                    " · longest streak ${state.streak.longest} ${if (state.streak.longest == 1) "day" else "days"}",
                style = MaterialTheme.typography.labelSmall,
                color = ClayInk.Faint,
            )
        }
    }
}

@Composable
private fun WeekMiniStat(
    emoji: String,
    value: String,
    label: String,
    accent: ClayAccent,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .clip(ClayShape.Small)
            .background(accent.soft.copy(alpha = 0.65f))
            .padding(horizontal = 10.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(text = emoji, fontSize = 14.sp)
        Spacer(Modifier.width(6.dp))
        Column {
            Text(
                text = value,
                style = MaterialTheme.typography.labelLarge,
                color = ClayInk.Primary,
                maxLines = 1,
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = ClayInk.Muted,
            )
        }
    }
}

@Composable
private fun RecentAchievementsCard(completions: List<RecentCompletion>) {
    Column {
        ClaySectionHeader(
            title = "Recent achievements",
            subtitle = if (completions.isEmpty()) "Completed quests will appear here" else "Your latest finished quests",
        )
        Spacer(Modifier.height(10.dp))
        if (completions.isEmpty()) {
            ClayCard(shape = ClayShape.Card, elevation = 10.dp) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    ClayIconTile(emoji = "🏅", accent = ClayPalette.Butter, size = 44.dp)
                    Spacer(Modifier.width(14.dp))
                    Text(
                        text = "No achievements yet. Finish a quest target and it will show up here — no placeholder trophies.",
                        style = MaterialTheme.typography.bodySmall,
                        color = ClayInk.Muted,
                    )
                }
            }
        } else {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                items(completions, key = { "${it.questTitle}-${it.date}-${it.xp}-${it.amountLabel}" }) { completion ->
                    ClayCard(
                        modifier = Modifier.width(150.dp),
                        shape = ClayShape.Medium,
                        elevation = 10.dp,
                        contentPadding = PaddingValues(14.dp),
                    ) {
                        ClayIconTile(
                            emoji = completion.emoji,
                            accent = if (completion.levelUp) ClayPalette.Butter else ClayPalette.Mint,
                            size = 40.dp,
                            fontSize = 18.sp,
                        )
                        Spacer(Modifier.height(10.dp))
                        Text(
                            text = completion.questTitle,
                            style = MaterialTheme.typography.titleSmall,
                            color = ClayInk.Primary,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = "+${completion.xp} XP",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = ClayPalette.Mint.deep,
                        )
                        Text(
                            text = "${completion.amountLabel} · ${DateX.relativeLabel(completion.date)}",
                            style = MaterialTheme.typography.labelSmall,
                            color = ClayInk.Faint,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AnalyticsShortcutCard(
    state: DashboardState,
    onOpenAnalytics: () -> Unit,
    onOpenQuests: () -> Unit,
) {
    ClayCard(
        shape = ClayShape.Card,
        elevation = 12.dp,
        contentPadding = PaddingValues(18.dp),
        onClick = onOpenAnalytics,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            ClayIconTile(emoji = "📊", accent = ClayPalette.Sky, size = 46.dp)
            Spacer(Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Analytics",
                    style = MaterialTheme.typography.titleSmall,
                    color = ClayInk.Primary,
                )
                Text(
                    text = if (state.lifetime.totalCompletions == 0) {
                        "Weekly, monthly and category insights appear once you log activity"
                    } else {
                        "${state.lifetime.totalCompletions} completions · ${state.lifetime.activeDays} active days recorded"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = ClayInk.Muted,
                    maxLines = 2,
                )
            }
            Icon(
                imageVector = Icons.Rounded.KeyboardArrowRight,
                contentDescription = null,
                tint = ClayInk.Faint,
            )
        }
        Spacer(Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            ClayButton(
                text = "All quests",
                onClick = onOpenQuests,
                accent = ClayPalette.Mint,
                style = ClayButtonStyle.Soft,
                height = 42.dp,
                modifier = Modifier.weight(1f),
            )
            ClayButton(
                text = "Open analytics",
                onClick = onOpenAnalytics,
                accent = ClayPalette.Sky,
                style = ClayButtonStyle.Soft,
                height = 42.dp,
                modifier = Modifier.weight(1.3f),
            )
        }
    }
}
