package com.irlrpg.app.ui.dashboard

import androidx.compose.animation.animateContentSize
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
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalConfiguration
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
import com.irlrpg.app.ui.components.AchievementBanner
import com.irlrpg.app.ui.components.BossVictoryOverlay
import com.irlrpg.app.ui.components.CelebrationOverlay
import com.irlrpg.app.ui.components.LevelUpOverlay
import com.irlrpg.app.ui.components.QuickLogSheet
import com.irlrpg.app.ui.components.StepLogSheet
import com.irlrpg.app.ui.design.ClayAvatarPedestal
import com.irlrpg.app.ui.design.ClayBarChart
import com.irlrpg.app.ui.design.ClayBar
import com.irlrpg.app.ui.design.ClayButton
import com.irlrpg.app.ui.design.ClayButtonStyle
import com.irlrpg.app.ui.design.ClayCard
import com.irlrpg.app.ui.design.ClayDivider
import com.irlrpg.app.ui.design.ClayDot
import com.irlrpg.app.ui.design.ClayIconPill
import com.irlrpg.app.ui.design.ClayMiniStat
import com.irlrpg.app.ui.design.ClayPanel
import com.irlrpg.app.ui.design.ClayProgressBar
import com.irlrpg.app.ui.design.ClayScaffold
import com.irlrpg.app.ui.design.ClaySectionTitle
import com.irlrpg.app.ui.theme.ClayAccent
import com.irlrpg.app.ui.theme.ClayInk
import com.irlrpg.app.ui.theme.ClayPalette
import com.irlrpg.app.ui.theme.ClayShape
import java.time.LocalDate

/**
 * Home Dashboard.
 *
 * Hierarchy is deliberate, in this order:
 *   1. who you are right now (character + level + next-level ring),
 *   2. what today asks of you (one primary action, then today's quests),
 *   3. how this week is going (compact chart, link to Analytics),
 *   4. where else to go (challenges / skill tree / awards / history).
 *
 * Exactly one prominent create-quest action exists on this screen (inside the
 * hero); the empty state is a quiet line rather than a second button, and the
 * analytics summary here is intentionally a preview so the Analytics screen is
 * not duplicated.
 */
@Composable
fun DashboardScreen(
    viewModel: DashboardViewModel,
    bottomBar: @Composable () -> Unit,
    onNewQuest: () -> Unit,
    onOpenQuest: (Long) -> Unit,
    onOpenAnalytics: () -> Unit,
    onOpenQuests: () -> Unit,
    onOpenSkillTree: () -> Unit = {},
    onOpenChallenges: () -> Unit = {},
    onOpenAchievements: () -> Unit = {},
    onOpenHistory: () -> Unit = {},
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val celebration by viewModel.celebration.collectAsStateWithLifecycle()
    val busyQuestId by viewModel.busyQuestId.collectAsStateWithLifecycle()
    val unlocked by viewModel.unlocked.collectAsStateWithLifecycle()
    val victory by viewModel.victory.collectAsStateWithLifecycle()
    val cosmetics by viewModel.cosmetics.collectAsStateWithLifecycle()
    var quickLogItem by remember { mutableStateOf<QuestWithProgress?>(null) }
    var stepLogItem by remember { mutableStateOf<QuestWithProgress?>(null) }

    val compact = LocalConfiguration.current.screenWidthDp < 360

    LifecycleResumeEffect(Unit) {
        viewModel.refreshDay()
        onPauseOrDispose { }
    }

    ClayScaffold(
        topBar = { GreetingHeader(state, cosmetics) },
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
                    top = padding.calculateTopPadding() + 2.dp,
                    bottom = padding.calculateBottomPadding() + 28.dp,
                ),
                verticalArrangement = Arrangement.spacedBy(18.dp),
            ) {
                item { CharacterHero(current, cosmetics, compact, onNewQuest) }

                item {
                    TodayPanel(
                        state = current,
                        onOpenQuests = onOpenQuests,
                    )
                }

                item {
                    ClaySectionTitle(
                        title = "Today's quests",
                        eyebrow = "Focus",
                        caption = when {
                            current.plannedToday == 0 -> "Nothing scheduled yet"
                            current.completedToday == current.plannedToday -> "All done for today — enjoy it"
                            else -> "${current.completedToday} of ${current.plannedToday} complete"
                        },
                        action = {
                            TextButton(onClick = onOpenQuests) {
                                Text(
                                    text = "Quest board",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = ClayPalette.Mint.deep,
                                )
                            }
                        },
                    )
                }

                item {
                    TodayQuestList(
                        state = current,
                        busyQuestId = busyQuestId,
                        onOpen = onOpenQuest,
                        onQuickLog = { item -> quickLogItem = item },
                        onStepLog = { item -> stepLogItem = item },
                        onCreate = onNewQuest,
                    )
                }

                item { WeekPanel(current, compact = compact, onOpenAnalytics = onOpenAnalytics) }

                item { ShortcutRow(onOpenChallenges, onOpenSkillTree, onOpenAchievements, onOpenHistory) }

                // Only shown when real completions exist — never an empty shelf.
                if (current.recentCompletions.isNotEmpty()) {
                    item {
                        RecentMilestones(
                            completions = current.recentCompletions,
                            onOpenHistory = onOpenHistory,
                        )
                    }
                }
            }
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

    stepLogItem?.let { item ->
        StepLogSheet(
            item = item,
            steps = state?.stepsFor(item.quest.id).orEmpty(),
            onDismiss = { stepLogItem = null },
            onLog = { stepId, amount ->
                viewModel.logStep(item.quest.id, stepId, amount)
                stepLogItem = null
            },
        )
    }

    // Celebration order matters: quest → level up → challenge → achievements.
    CelebrationOverlay(result = celebration, onDismiss = viewModel::dismissCelebration)

    val player = state?.player
    celebration?.takeIf { it.levelUp && player != null }?.let { result ->
        val snapshot = requireNotNull(player)
        LevelUpOverlay(
            newLevel = result.newLevel,
            rankTitle = snapshot.rankTitle,
            rankEmoji = snapshot.rankEmoji,
            totalXp = result.totalXp,
            xpIntoLevel = snapshot.xpIntoLevel,
            xpForLevel = snapshot.xpForCurrentLevel,
            onDismiss = viewModel::dismissCelebration,
        )
    }

    victory?.let { (win, count) ->
        BossVictoryOverlay(
            victory = win,
            objectivesTotal = count.coerceAtLeast(win.objectivesCompleted),
            onDismiss = viewModel::dismissVictory,
        )
    }

    AchievementBanner(
        newlyEarned = unlocked,
        onShown = viewModel::acknowledgeAchievement,
    )
}

// ---------------------------------------------------------------------- hero

@Composable
private fun CharacterHero(
    state: DashboardState,
    cosmetics: CosmeticsView,
    compact: Boolean,
    onCreateQuest: () -> Unit,
) {
    val player = state.player
    val frame = com.irlrpg.app.domain.game.CosmeticCatalog.byCode(player.stats.cosmeticFrame)
    val frameAccent = frame?.let { ClayPalette.accentById(it.accentId) } ?: ClayPalette.Lavender
    val equippedTitle = com.irlrpg.app.domain.game.CosmeticCatalog.byCode(player.stats.displayTitle)

    ClayCard(
        shape = ClayShape.Hero,
        elevation = 18.dp,
        glowAlpha = 0.26f,
        contentPadding = PaddingValues(20.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            ClayAvatarPedestal(
                emoji = frame?.emoji ?: "🧭",
                accent = ClayPalette.Lavender,
                frameAccent = frameAccent,
                progress = player.progress,
                size = if (compact) 116.dp else 138.dp,
                badge = "LV ${player.level}",
            )
            Spacer(Modifier.width(if (compact) 10.dp else 16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = equippedTitle?.name ?: player.rankTitle,
                    style = MaterialTheme.typography.labelMedium,
                    color = if (equippedTitle != null) ClayPalette.Lavender.deep else ClayInk.Secondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = "${player.rankEmoji} ${player.rankTitle}",
                    style = MaterialTheme.typography.titleMedium,
                    color = ClayInk.Primary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.height(8.dp))
                ClayProgressBar(progress = player.progress, accent = ClayPalette.Lavender, height = 10.dp)
                Spacer(Modifier.height(6.dp))
                Text(
                    text = "${player.xpIntoLevel} / ${player.xpForCurrentLevel} XP to level ${player.level + 1}",
                    style = MaterialTheme.typography.labelSmall,
                    color = ClayInk.Muted,
                    maxLines = 2,
                )
            }
        }

        Spacer(Modifier.height(16.dp))
        // The single prominent action on this screen.
        ClayButton(
            text = "New quest",
            onClick = onCreateQuest,
            accent = ClayPalette.Lavender,
            icon = Icons.Rounded.Add,
            fillWidth = true,
            height = 50.dp,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = "${player.stats.totalXp} XP lifetime · ${state.lifetime.totalCompletions} completions recorded",
            style = MaterialTheme.typography.labelSmall,
            color = ClayInk.Faint,
            maxLines = 2,
        )
    }
}

// --------------------------------------------------------------------- today

@Composable
private fun TodayPanel(state: DashboardState, onOpenQuests: () -> Unit) {
    ClayCard(
        shape = ClayShape.Card,
        elevation = 12.dp,
        glowAlpha = 0.16f,
        contentPadding = PaddingValues(18.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            ClayMiniStat(
                value = "${state.today.xp}",
                label = "XP today",
                emoji = "⚡",
                accent = ClayPalette.Butter,
                modifier = Modifier.weight(1f),
            )
            MiniDivider()
            ClayMiniStat(
                value = "${state.completedToday}/${state.plannedToday}",
                label = "Quests",
                emoji = "✅",
                accent = ClayPalette.Mint,
                modifier = Modifier.weight(1f),
            )
            MiniDivider()
            ClayMiniStat(
                value = if (state.today.minutes > 0) DateX.formatMinutes(state.today.minutes) else "—",
                label = "Tracked",
                emoji = "⏳",
                accent = ClayPalette.Sky,
                modifier = Modifier.weight(1f),
            )
            MiniDivider()
            ClayMiniStat(
                value = if (state.streak.current > 0) "${state.streak.current}d" else "—",
                label = "Streak",
                emoji = "🔥",
                accent = ClayPalette.Peach,
                modifier = Modifier.weight(1f),
            )
        }
        Spacer(Modifier.height(14.dp))
        ClayPanel(
            shape = ClayShape.Small,
            tint = ClayPalette.SurfaceMuted.copy(alpha = 0.6f),
            onClick = onOpenQuests,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = if (state.streak.activeToday) {
                        "Today is already logged — anything else adds to it."
                    } else {
                        "No activity logged today yet."
                    },
                    style = MaterialTheme.typography.labelMedium,
                    color = ClayInk.Secondary,
                    modifier = Modifier.weight(1f),
                    maxLines = 2,
                )
                Text(
                    text = "Open board",
                    style = MaterialTheme.typography.labelMedium,
                    color = ClayPalette.Mint.deep,
                    maxLines = 1,
                )
            }
        }
    }
}

@Composable
private fun MiniDivider() {
    Box(
        modifier = Modifier
            .height(34.dp)
            .width(1.dp)
            .background(ClayInk.Faint.copy(alpha = 0.22f)),
    )
}

@Composable
private fun TodayQuestList(
    state: DashboardState,
    busyQuestId: Long?,
    onOpen: (Long) -> Unit,
    onQuickLog: (QuestWithProgress) -> Unit,
    onStepLog: (QuestWithProgress) -> Unit,
    onCreate: () -> Unit,
) {
    if (state.todayQuests.isEmpty()) {
        // Compact, single-message empty state: the create action lives in the
        // hero, so it is not repeated here.
        ClayPanel(
            shape = ClayShape.Card,
            tint = ClayPalette.SurfaceCream,
            contentPadding = PaddingValues(18.dp),
            onClick = onCreate,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                ClayDot(emoji = "🧭", accent = ClayPalette.Lavender, size = 38.dp)
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Nothing scheduled for today",
                        style = MaterialTheme.typography.titleSmall,
                        color = ClayInk.Primary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = "Tap to create a quest — reading, coding, training, anything measurable.",
                        style = MaterialTheme.typography.bodySmall,
                        color = ClayInk.Muted,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
        return
    }

    ClayCard(
        shape = ClayShape.Card,
        elevation = 12.dp,
        glowAlpha = 0.14f,
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
    ) {
        state.todayQuests.forEachIndexed { index, item ->
            if (index > 0) ClayDivider(verticalPadding = 0.dp)
            QuestRow(
                item = item,
                busy = busyQuestId == item.quest.id,
                onOpen = { onOpen(item.quest.id) },
                onLog = {
                    when {
                        item.isMultiStep -> onStepLog(item)
                        item.quest.trackingKind == TrackingKind.Milestone.name -> onQuickLog(item)
                        else -> onQuickLog(item)
                    }
                },
            )
        }
    }
}

@Composable
private fun QuestRow(
    item: QuestWithProgress,
    busy: Boolean,
    onOpen: () -> Unit,
    onLog: () -> Unit,
) {
    val accent = accentFor(item)
    val quest = item.quest
    val interaction = remember { MutableInteractionSource() }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(ClayShape.Small)
            .clickable(interactionSource = interaction, indication = null, onClick = onOpen)
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ClayDot(emoji = quest.emoji, accent = accent, size = 40.dp)
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = quest.title,
                style = MaterialTheme.typography.titleSmall,
                color = if (item.isComplete) ClayInk.Muted else ClayInk.Primary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(3.dp))
            Text(
                text = progressLine(item),
                style = MaterialTheme.typography.labelSmall,
                color = ClayInk.Muted,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (!item.isComplete && item.fraction > 0f) {
                Spacer(Modifier.height(7.dp))
                ClayProgressBar(progress = item.fraction, accent = accent, height = 7.dp)
            }
        }
        Spacer(Modifier.width(10.dp))
        Column(horizontalAlignment = Alignment.End) {
            ClayIconPill(
                text = "+${quest.xpReward}",
                accent = ClayPalette.Butter,
                emoji = "⚡",
                emphasised = true,
            )
            Spacer(Modifier.height(6.dp))
            LogAction(
                complete = item.isComplete,
                busy = busy,
                accent = accent,
                onLog = onLog,
            )
        }
    }
}

@Composable
private fun LogAction(
    complete: Boolean,
    busy: Boolean,
    accent: ClayAccent,
    onLog: () -> Unit,
) {
    val interaction = remember { MutableInteractionSource() }
    Box(
        modifier = Modifier
            .size(40.dp)
            .clip(ClayShape.Pill)
            .background(if (complete) ClayPalette.Mint.soft else accent.soft)
            .clickable(
                enabled = !complete && !busy,
                interactionSource = interaction,
                indication = null,
                onClick = onLog,
            ),
        contentAlignment = Alignment.Center,
    ) {
        when {
            complete -> Icon(
                imageVector = Icons.Rounded.Check,
                contentDescription = "Completed today",
                tint = ClayPalette.Mint.deep,
                modifier = Modifier.size(18.dp),
            )
            busy -> Text(text = "…", color = accent.deep, fontSize = 16.sp)
            else -> Text(text = "＋", color = accent.deep, fontSize = 17.sp)
        }
    }
}

private fun progressLine(item: QuestWithProgress): String = buildString {
    when {
        item.isMultiStep -> append("${item.stepsCompleted}/${item.stepsTotal} steps")
        item.quest.trackingKind == TrackingKind.Milestone.name ->
            append(if (item.isComplete) "Milestone reached" else "One-off milestone")
        else -> {
            append("${item.progress.coerceAtMost(item.target)}/${item.target} ")
            append(if (item.quest.customUnit.isNullOrBlank()) unitWord(item) else item.quest.customUnit)
        }
    }
    item.daysLeft?.let { days ->
        append(" · ")
        append(
            when {
                item.isComplete -> "done"
                days < 0 -> "${-days}d overdue"
                days == 0L -> "due today"
                else -> "${days}d left"
            }
        )
    }
}

private fun unitWord(item: QuestWithProgress): String = when (item.quest.unit.lowercase()) {
    "minutes" -> "min"
    "hours" -> "h"
    "pages" -> "pages"
    "words" -> "words"
    "sessions" -> "sessions"
    "points" -> "pts"
    "kilometers" -> "km"
    else -> com.irlrpg.app.domain.model.UnitKind.fromName(item.quest.unit).short
}

// ---------------------------------------------------------------------- week

@Composable
private fun WeekPanel(state: DashboardState, compact: Boolean, onOpenAnalytics: () -> Unit) {
    val bars = state.week.map { day ->
        ClayBar(label = day.label, value = day.xp, highlight = day.date == LocalDate.now())
    }
    ClayCard(
        shape = ClayShape.Card,
        elevation = 12.dp,
        glowAlpha = 0.14f,
        contentPadding = PaddingValues(18.dp),
    ) {
        ClaySectionTitle(
            title = "This week",
            eyebrow = "Trend",
            caption = "${state.weekXp} XP · ${state.weekCompletions} completions · ${DateX.formatMinutes(state.weekMinutes)}",
            action = {
                TextButton(onClick = onOpenAnalytics) {
                    Text(
                        text = "Analytics",
                        style = MaterialTheme.typography.labelMedium,
                        color = ClayPalette.Sky.deep,
                    )
                }
            },
        )
        Spacer(Modifier.height(12.dp))
        // Compact bars: no per-bar numbers here (Analytics shows the detail).
        ClayBarChart(
            bars = bars,
            accent = ClayPalette.Sky,
            barArea = if (compact) 62.dp else 76.dp,
            showValues = false,
        )
    }
}

// ----------------------------------------------------------------- shortcuts

@Composable
private fun ShortcutRow(
    onOpenChallenges: () -> Unit,
    onOpenSkillTree: () -> Unit,
    onOpenAchievements: () -> Unit,
    onOpenHistory: () -> Unit,
) {
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        ShortcutTile("🏔️", "Challenges", ClayPalette.Peach, onOpenChallenges, Modifier.weight(1f))
        ShortcutTile("🌳", "Skill tree", ClayPalette.Mint, onOpenSkillTree, Modifier.weight(1f))
        ShortcutTile("🏅", "Awards", ClayPalette.Butter, onOpenAchievements, Modifier.weight(1f))
        ShortcutTile("🕰️", "History", ClayPalette.Sky, onOpenHistory, Modifier.weight(1f))
    }
}

@Composable
private fun ShortcutTile(
    emoji: String,
    label: String,
    accent: ClayAccent,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    // Flat tiles: these are navigation, not content, so they get no elevation.
    ClayPanel(
        modifier = modifier,
        shape = ClayShape.Medium,
        tint = accent.soft.copy(alpha = 0.65f),
        contentPadding = PaddingValues(vertical = 12.dp, horizontal = 6.dp),
        onClick = onClick,
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(text = emoji, fontSize = 20.sp)
            Spacer(Modifier.height(5.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = accent.deep,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
            )
        }
    }
}

// ---------------------------------------------------------------- milestones

@Composable
private fun RecentMilestones(completions: List<RecentCompletion>, onOpenHistory: () -> Unit) {
    ClayCard(
        shape = ClayShape.Card,
        elevation = 10.dp,
        glowAlpha = 0.12f,
        contentPadding = PaddingValues(18.dp),
    ) {
        ClaySectionTitle(
            title = "Recent milestones",
            eyebrow = "History",
            caption = "Straight from your recorded log",
            action = {
                TextButton(onClick = onOpenHistory) {
                    Text(
                        text = "All",
                        style = MaterialTheme.typography.labelMedium,
                        color = ClayPalette.Sky.deep,
                    )
                }
            },
        )
        Spacer(Modifier.height(10.dp))
        completions.take(3).forEachIndexed { index, completion ->
            if (index > 0) ClayDivider(verticalPadding = 0.dp)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                ClayDot(
                    emoji = if (completion.levelUp) "🎉" else completion.emoji,
                    accent = if (completion.levelUp) ClayPalette.Butter else ClayPalette.Mint,
                    size = 34.dp,
                )
                Spacer(Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = completion.questTitle,
                        style = MaterialTheme.typography.bodyMedium,
                        color = ClayInk.Primary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = buildString {
                            append(DateX.relativeLabel(completion.date))
                            if (completion.amountLabel.isNotBlank()) append(" · ${completion.amountLabel}")
                            if (completion.levelUp) append(" · level up")
                        },
                        style = MaterialTheme.typography.labelSmall,
                        color = ClayInk.Muted,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                Spacer(Modifier.width(8.dp))
                Text(
                    text = "+${completion.xp}",
                    style = MaterialTheme.typography.labelMedium,
                    color = ClayPalette.Butter.deep,
                    maxLines = 1,
                )
            }
        }
    }
}

// -------------------------------------------------------------------- chrome

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
                .height(196.dp)
                .clip(ClayShape.Hero)
                .background(ClayPalette.SurfaceMuted.copy(alpha = 0.55f))
                .animateContentSize(),
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(96.dp)
                .clip(ClayShape.Card)
                .background(ClayPalette.SurfaceMuted.copy(alpha = 0.4f)),
        )
    }
}

@Composable
private fun GreetingHeader(state: DashboardState?, cosmetics: CosmeticsView) {
    val hour = remember { java.time.LocalTime.now().hour }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(start = 22.dp, end = 20.dp, top = 10.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "${DateX.greeting(hour)},",
                style = MaterialTheme.typography.bodySmall,
                color = ClayInk.Muted,
                maxLines = 1,
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
                style = MaterialTheme.typography.labelSmall,
                color = ClayInk.Faint,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        val streak = state?.streak
        if (streak != null) {
            ClayIconPill(
                text = if (streak.current > 0) "${streak.current} day streak" else "Start today",
                accent = ClayPalette.Peach,
                emoji = if (streak.current > 0) "🔥" else "🌱",
                emphasised = true,
            )
        }
    }
}
