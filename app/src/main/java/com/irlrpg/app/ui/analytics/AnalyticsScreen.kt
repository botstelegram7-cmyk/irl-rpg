package com.irlrpg.app.ui.analytics

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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.irlrpg.app.core.DateX
import com.irlrpg.app.domain.model.AnalyticsState
import com.irlrpg.app.domain.model.CategoryBreakdown
import com.irlrpg.app.ui.design.ClayBar
import com.irlrpg.app.ui.design.ClayBarChart
import com.irlrpg.app.ui.design.ClayCard
import com.irlrpg.app.ui.design.ClayEmptyState
import com.irlrpg.app.ui.design.ClayIconTile
import com.irlrpg.app.ui.design.ClayProgressBar
import com.irlrpg.app.ui.design.ClayScaffold
import com.irlrpg.app.ui.design.ClaySectionHeader
import com.irlrpg.app.ui.design.ClaySegmentedControl
import com.irlrpg.app.ui.design.ClayStatTile
import com.irlrpg.app.ui.design.ClayTopBar
import com.irlrpg.app.ui.theme.ClayInk
import com.irlrpg.app.ui.theme.ClayPalette
import com.irlrpg.app.ui.theme.ClayShape
import java.time.LocalDate

@Composable
fun AnalyticsScreen(
    viewModel: AnalyticsViewModel,
    bottomBar: @Composable () -> Unit,
    onCreateQuest: () -> Unit,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val range by viewModel.range.collectAsStateWithLifecycle()

    LifecycleResumeEffect(Unit) {
        viewModel.refreshDay()
        onPauseOrDispose { }
    }

    ClayScaffold(
        topBar = {
            ClayTopBar(
                title = "Analytics",
                subtitle = "Computed from your recorded entries — nothing simulated",
                accent = ClayPalette.Sky,
            )
        },
        bottomBar = bottomBar,
    ) { padding ->
        val current = state
        if (current == null) {
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
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            item {
                ClaySegmentedControl(
                    options = AnalyticsRange.entries.toList(),
                    selected = range,
                    onSelect = viewModel::setRange,
                    label = { it.label },
                    accent = ClayPalette.Sky,
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            val summary = viewModel.summaryFor(current, range)
            item {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        ClayStatTile(
                            emoji = "⚡",
                            value = "${summary.xp}",
                            label = "XP · ${range.label.lowercase()}",
                            accent = ClayPalette.Butter,
                            modifier = Modifier.weight(1f),
                        )
                        ClayStatTile(
                            emoji = "✅",
                            value = "${summary.completions}",
                            label = "Targets reached",
                            accent = ClayPalette.Mint,
                            modifier = Modifier.weight(1f),
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        ClayStatTile(
                            emoji = "⏳",
                            value = if (summary.minutes > 0) DateX.formatMinutes(summary.minutes) else "—",
                            label = "Time tracked",
                            accent = ClayPalette.Sky,
                            modifier = Modifier.weight(1f),
                        )
                        ClayStatTile(
                            emoji = "🎯",
                            value = "${(current.completionRate * 100).toInt()}%",
                            label = "Today's quests done",
                            accent = ClayPalette.Lavender,
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
            }

            if (current.lifetime.totalCompletions == 0) {
                item {
                    ClayEmptyState(
                        emoji = "📊",
                        title = "Analytics start with your first completed target",
                        message = "Log progress on a quest and this screen fills with real daily, weekly and category insight. No sample numbers are ever shown.",
                        actionLabel = "Create a quest",
                        onAction = onCreateQuest,
                    )
                }
            }

            item { WeeklyTrendCard(current) }
            item { ComparisonCard(current) }
            item { GoalsCard(current) }
            item { CategoryCard(current) }

            if (current.readingPages > 0 || current.readingMinutes > 0) {
                item { ReadingCard(current) }
            }

            item { ConsistencyCard(current) }

            item {
                ClayCard(shape = ClayShape.Card, elevation = 10.dp) {
                    Text(
                        text = "Missing a day costs you nothing here. These stats exist to show momentum, not to punish gaps.",
                        style = MaterialTheme.typography.bodySmall,
                        color = ClayInk.Muted,
                    )
                }
            }
        }
    }
}

@Composable
private fun WeeklyTrendCard(state: AnalyticsState) {
    val today = LocalDate.now()
    val bars = state.week.map { day ->
        ClayBar(label = day.label, value = day.xp, highlight = day.date == today)
    }
    ClayCard(shape = ClayShape.Card, elevation = 14.dp) {
        ClaySectionHeader(
            title = "Weekly trend",
            subtitle = "XP per day · ${DateX.weekDaysOf(today).first().let { DateX.dayAndMonth(it) }} – ${
                DateX.dayAndMonth(DateX.weekDaysOf(today).last())
            }",
        )
        Spacer(Modifier.height(16.dp))
        ClayBarChart(bars = bars, accent = ClayPalette.Sky, barArea = 116.dp)
        Spacer(Modifier.height(16.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "${state.weekXp} XP this week",
                style = MaterialTheme.typography.titleSmall,
                color = ClayInk.Primary,
                modifier = Modifier.weight(1f),
            )
            if (state.lastWeekXp > 0) {
                val delta = state.weekOverWeekDelta
                Box(
                    modifier = Modifier
                        .clip(ClayShape.Pill)
                        .background(
                            if (delta >= 0) ClayPalette.Mint.soft else ClayPalette.Peach.soft
                        )
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                ) {
                    Text(
                        text = "${if (delta >= 0) "▲" else "▼"} ${kotlin.math.abs(delta)} vs last week",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = if (delta >= 0) ClayPalette.Mint.deep else ClayPalette.Peach.deep,
                    )
                }
            } else {
                Text(
                    text = "Last week: no data",
                    style = MaterialTheme.typography.labelSmall,
                    color = ClayInk.Faint,
                )
            }
        }
    }
}

@Composable
private fun ComparisonCard(state: AnalyticsState) {
    ClayCard(shape = ClayShape.Card, elevation = 12.dp) {
        ClaySectionHeader(title = "Today vs yesterday", subtitle = "Same metrics, measured the same way")
        Spacer(Modifier.height(14.dp))
        ComparisonRow("XP earned", state.today.xp, state.yesterday.xp, ClayPalette.Butter)
        Spacer(Modifier.height(10.dp))
        ComparisonRow("Targets reached", state.today.completions, state.yesterday.completions, ClayPalette.Mint)
        Spacer(Modifier.height(10.dp))
        ComparisonRow("Minutes tracked", state.today.minutes, state.yesterday.minutes, ClayPalette.Sky)
    }
}

@Composable
private fun ComparisonRow(
    label: String,
    today: Int,
    yesterday: Int,
    accent: com.irlrpg.app.ui.theme.ClayAccent,
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = ClayInk.Secondary,
            modifier = Modifier.weight(1f),
        )
        Text(
            text = "today $today",
            style = MaterialTheme.typography.labelMedium,
            color = accent.deep,
        )
        Spacer(Modifier.width(12.dp))
        Text(
            text = "yesterday $yesterday",
            style = MaterialTheme.typography.labelMedium,
            color = ClayInk.Faint,
        )
    }
}

@Composable
private fun GoalsCard(state: AnalyticsState) {
    ClayCard(shape = ClayShape.Card, elevation = 12.dp) {
        ClaySectionHeader(
            title = "Progress towards today's goals",
            subtitle = "${(state.completionRate * 100).toInt()}% of today's scheduled quests reached their target",
        )
        Spacer(Modifier.height(14.dp))
        ClayProgressBar(progress = state.completionRate, accent = ClayPalette.Lavender, height = 12.dp)
        Spacer(Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            MiniStat("Month XP", "${state.monthXp}", ClayPalette.Butter)
            MiniStat("Month targets", "${state.monthCompletions}", ClayPalette.Mint)
            MiniStat("Year XP", "${state.yearXp}", ClayPalette.Sky)
        }
    }
}

@Composable
private fun MiniStat(label: String, value: String, accent: com.irlrpg.app.ui.theme.ClayAccent) {
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
private fun CategoryCard(state: AnalyticsState) {
    ClayCard(shape = ClayShape.Card, elevation = 12.dp) {
        ClaySectionHeader(
            title = "Where your time goes",
            subtitle = if (state.categories.isEmpty()) {
                "Category splits appear once you log time-based or counted activity"
            } else {
                "Split by your own categories — minutes and completions"
            },
        )
        Spacer(Modifier.height(14.dp))
        if (state.categories.isEmpty()) {
            Text(
                text = "Nothing recorded yet.",
                style = MaterialTheme.typography.bodySmall,
                color = ClayInk.Muted,
            )
        } else {
            val maxMinutes = state.categories.maxOf { it.minutes }.coerceAtLeast(1)
            state.categories.forEachIndexed { index, item ->
                if (index > 0) Spacer(Modifier.height(16.dp))
                CategoryRow(item, maxMinutes)
            }
        }
    }
}

@Composable
private fun CategoryRow(item: CategoryBreakdown, maxMinutes: Int) {
    val accent = ClayPalette.accentById(item.accentId)
    Row(verticalAlignment = Alignment.CenterVertically) {
        ClayIconTile(emoji = item.emoji, accent = accent, size = 38.dp, fontSize = 17.sp)
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = item.name,
                style = MaterialTheme.typography.titleSmall,
                color = ClayInk.Primary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(6.dp))
            ClayProgressBar(
                progress = item.minutes.toFloat() / maxMinutes.toFloat(),
                accent = accent,
                height = 8.dp,
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = buildString {
                    if (item.minutes > 0) {
                        append(DateX.formatMinutes(item.minutes))
                        append(" · ")
                    }
                    append("${item.completions} targets")
                    append(" · ${item.xp} XP")
                },
                style = MaterialTheme.typography.labelSmall,
                color = ClayInk.Muted,
            )
        }
    }
}

@Composable
private fun ReadingCard(state: AnalyticsState) {
    ClayCard(shape = ClayShape.Card, elevation = 12.dp) {
        ClaySectionHeader(
            title = "Reading stats",
            subtitle = "From your reading category and page-counted quests",
        )
        Spacer(Modifier.height(14.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            MiniStat("Pages logged", "${state.readingPages}", ClayPalette.Sky)
            MiniStat("Reading time", DateX.formatMinutes(state.readingMinutes), ClayPalette.Mint)
            MiniStat(
                "Reading targets",
                "${state.categories.firstOrNull { it.name.contains("read", true) }?.completions ?: 0}",
                ClayPalette.Butter,
            )
        }
    }
}

@Composable
private fun ConsistencyCard(state: AnalyticsState) {
    ClayCard(shape = ClayShape.Card, elevation = 12.dp) {
        ClaySectionHeader(
            title = "Consistency",
            subtitle = "Streaks count real completed days only",
        )
        Spacer(Modifier.height(14.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            MiniStat("Current streak", "${state.streak.current}", ClayPalette.Peach)
            MiniStat("Longest streak", "${state.streak.longest}", ClayPalette.Butter)
            MiniStat("Active days", "${state.lifetime.activeDays}", ClayPalette.Mint)
        }
        Spacer(Modifier.height(14.dp))
        ClayProgressBar(progress = state.consistencyRate, accent = ClayPalette.Mint, height = 12.dp)
        Spacer(Modifier.height(8.dp))
        Text(
            text = "${(state.consistencyRate * 100).toInt()}% of days since you started have activity recorded",
            style = MaterialTheme.typography.labelSmall,
            color = ClayInk.Muted,
        )
    }
}
