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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
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
import com.irlrpg.app.domain.model.AnalyticsRange
import com.irlrpg.app.domain.model.AnalyticsState
import com.irlrpg.app.domain.model.CategoryBreakdown
import com.irlrpg.app.domain.model.RangeSummary
import com.irlrpg.app.ui.design.ClayBar
import com.irlrpg.app.ui.design.ClayBarChart
import com.irlrpg.app.ui.design.ClayButton
import com.irlrpg.app.ui.design.ClayCard
import com.irlrpg.app.ui.design.ClayDivider
import com.irlrpg.app.ui.design.ClayIconPill
import com.irlrpg.app.ui.design.ClayMiniStat
import com.irlrpg.app.ui.design.ClayPanel
import com.irlrpg.app.ui.design.ClayProgressBar
import com.irlrpg.app.ui.design.ClayScaffold
import com.irlrpg.app.ui.design.ClaySegmentedControl
import com.irlrpg.app.ui.design.ClaySectionTitle
import com.irlrpg.app.ui.design.ClayTopBar
import com.irlrpg.app.ui.theme.ClayInk
import com.irlrpg.app.ui.theme.ClayPalette
import com.irlrpg.app.ui.theme.ClayShape
import java.time.LocalDate

/**
 * Analytics screen.
 *
 * One filter (Today / Week / Month / Year) drives every card below it, and every
 * number is arithmetic over the recorded log rows — there are no sample series,
 * no smoothing, and no placeholder achievements. Charts show the range's own
 * buckets: days for a week, weeks of the month for a month, months for a year.
 * When there is nothing recorded yet, the whole screen collapses to one compact
 * panel rather than a column of empty cards.
 */
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
            verticalArrangement = Arrangement.spacedBy(16.dp),
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

            if (current.lifetime.totalCompletions == 0 && current.lifetime.totalXp == 0) {
                // Compact empty state: one panel, no chart cards, no fake series.
                item {
                    ClayCard(
                        shape = ClayShape.Card,
                        elevation = 12.dp,
                        glowAlpha = 0.14f,
                        contentPadding = PaddingValues(18.dp),
                    ) {
                        ClaySectionTitle(
                            title = "No recorded activity yet",
                            eyebrow = "Empty",
                            caption = "Analytics are built only from rows you create by logging quests",
                        )
                        Spacer(Modifier.height(12.dp))
                        ClayPanel(
                            shape = ClayShape.Small,
                            tint = ClayPalette.SurfaceMuted.copy(alpha = 0.6f),
                        ) {
                            Text(
                                text = "Log progress once and this screen fills in with real daily, weekly, monthly and yearly summaries.",
                                style = MaterialTheme.typography.bodySmall,
                                color = ClayInk.Muted,
                            )
                        }
                        Spacer(Modifier.height(14.dp))
                        ClayButton(
                            text = "Create your first quest",
                            onClick = onCreateQuest,
                            accent = ClayPalette.Sky,
                            fillWidth = true,
                            height = 48.dp,
                            leadingEmoji = "🧭",
                        )
                    }
                }
                item { LifetimeCard(current) }
                return@LazyColumn
            }

            val summary = viewModel.summaryFor(current, range)

            item { RangeSummaryCard(range, summary, current) }

            if (summary.buckets.any { it.xp > 0 }) {
                item { RangeChartCard(range, summary) }
            }

            item { CategoryCard(range, summary) }

            item { ComparisonCard(range, summary, current) }

            item { ConsistencyCard(current) }

            if (current.readingMinutes > 0) {
                item { ReadingCard(current) }
            }

            item { LifetimeCard(current) }
        }
    }
}

// ------------------------------------------------------------------- summary

@Composable
private fun RangeSummaryCard(
    range: AnalyticsRange,
    summary: RangeSummary,
    state: AnalyticsState,
) {
    ClayCard(
        shape = ClayShape.Card,
        elevation = 14.dp,
        glowAlpha = 0.2f,
        contentPadding = PaddingValues(18.dp),
    ) {
        ClaySectionTitle(
            title = "${range.label} summary",
            eyebrow = "Recorded",
            caption = rangeCaption(range),
        )
        Spacer(Modifier.height(14.dp))
        Row(verticalAlignment = Alignment.Bottom) {
            Text(
                text = "${summary.xp}",
                style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.Bold,
                color = ClayPalette.Sky.deep,
                maxLines = 1,
            )
            Spacer(Modifier.width(6.dp))
            Text(
                text = "XP",
                style = MaterialTheme.typography.titleSmall,
                color = ClayInk.Muted,
                modifier = Modifier.padding(bottom = 6.dp),
            )
            Spacer(Modifier.weight(1f))
            DeltaPill(summary.xp - summary.previousXp, summary.previousLabel)
        }
        Spacer(Modifier.height(14.dp))
        Row {
            ClayMiniStat(
                value = "${summary.completions}",
                label = "Targets reached",
                emoji = "✅",
                accent = ClayPalette.Mint,
                modifier = Modifier.weight(1f),
            )
            ClayMiniStat(
                value = if (summary.minutes > 0) DateX.formatMinutes(summary.minutes) else "—",
                label = "Time tracked",
                emoji = "⏳",
                accent = ClayPalette.Sky,
                modifier = Modifier.weight(1f),
            )
            ClayMiniStat(
                value = "${summary.activeDays}",
                label = "Active days",
                emoji = "📅",
                accent = ClayPalette.Lavender,
                modifier = Modifier.weight(1f),
            )
            ClayMiniStat(
                value = "${summary.entries}",
                label = "Entries logged",
                emoji = "📝",
                accent = ClayPalette.Butter,
                modifier = Modifier.weight(1f),
            )
        }
        if (range == AnalyticsRange.Today) {
            Spacer(Modifier.height(14.dp))
            ClayDivider(verticalPadding = 0.dp)
            Spacer(Modifier.height(12.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Today's scheduled quests",
                        style = MaterialTheme.typography.labelMedium,
                        color = ClayInk.Secondary,
                    )
                    Text(
                        text = "${(state.completionRate * 100).toInt()}% finished so far",
                        style = MaterialTheme.typography.labelSmall,
                        color = ClayInk.Muted,
                    )
                }
                Spacer(Modifier.width(12.dp))
                Box(modifier = Modifier.width(120.dp)) {
                    ClayProgressBar(
                        progress = state.completionRate,
                        accent = ClayPalette.Lavender,
                        height = 9.dp,
                    )
                }
            }
        }
    }
}

private fun rangeCaption(range: AnalyticsRange): String = when (range) {
    AnalyticsRange.Today -> "Everything logged on ${DateX.fullDate(LocalDate.now())}"
    AnalyticsRange.Week -> "Monday to today, from a week starting ${DateX.dayAndMonth(DateX.startOfWeek(LocalDate.now()))}"
    AnalyticsRange.Month -> "Since the 1st of ${DateX.shortMonthLabel(LocalDate.now())}"
    AnalyticsRange.Year -> "Since 1 January ${LocalDate.now().year}"
}

@Composable
private fun DeltaPill(delta: Int, previousLabel: String) {
    val up = delta > 0
    val flat = delta == 0
    val accent = when {
        flat -> ClayPalette.Lavender
        up -> ClayPalette.Mint
        else -> ClayPalette.Peach
    }
    ClayIconPill(
        text = when {
            flat -> "Same as $previousLabel"
            up -> "+$delta XP vs $previousLabel"
            else -> "$delta XP vs $previousLabel"
        },
        accent = accent,
        emoji = when {
            flat -> "➖"
            up -> "▲"
            else -> "▼"
        },
        emphasised = !flat,
    )
}

// ---------------------------------------------------------------------- chart

@Composable
private fun RangeChartCard(range: AnalyticsRange, summary: RangeSummary) {
    val today = LocalDate.now()
    val bars = summary.buckets.map { bucket ->
        ClayBar(
            label = bucket.label,
            value = bucket.xp,
            highlight = when (range) {
                AnalyticsRange.Week -> bucket.date == today
                AnalyticsRange.Month -> today.toEpochDay() in bucket.date.toEpochDay()..(bucket.date.toEpochDay() + 6)
                else -> bucket.date.month == today.month
            },
        )
    }
    ClayCard(
        shape = ClayShape.Card,
        elevation = 12.dp,
        glowAlpha = 0.14f,
        contentPadding = PaddingValues(18.dp),
    ) {
        ClaySectionTitle(
            title = "${range.label} trend",
            eyebrow = "Chart",
            caption = summary.bucketCaption,
        )
        Spacer(Modifier.height(12.dp))
        ClayBarChart(
            bars = bars,
            accent = ClayPalette.Sky,
            barArea = if (range == AnalyticsRange.Year || range == AnalyticsRange.Month) 84.dp else 96.dp,
            showValues = range != AnalyticsRange.Year,
        )
        Spacer(Modifier.height(10.dp))
        Text(
            text = "Bars show XP earned in each bucket. Busiest bucket: ${
                summary.buckets.maxByOrNull { it.xp }?.let { "${it.label} · ${it.xp} XP" } ?: "—"
            }",
            style = MaterialTheme.typography.labelSmall,
            color = ClayInk.Faint,
            maxLines = 2,
        )
    }
}

// ----------------------------------------------------------------- categories

@Composable
private fun CategoryCard(range: AnalyticsRange, summary: RangeSummary) {
    ClayCard(
        shape = ClayShape.Card,
        elevation = 12.dp,
        glowAlpha = 0.14f,
        contentPadding = PaddingValues(18.dp),
    ) {
        ClaySectionTitle(
            title = "Where the time went",
            eyebrow = "Categories",
            caption = if (summary.categories.isEmpty()) {
                "No entries in this range yet"
            } else {
                "Share of XP and time across your categories · ${range.label.lowercase()} range"
            },
        )
        if (summary.categories.isEmpty()) {
            Spacer(Modifier.height(10.dp))
            Text(
                text = "Categorise a quest and log it — the split appears here automatically.",
                style = MaterialTheme.typography.bodySmall,
                color = ClayInk.Muted,
            )
            return@ClayCard
        }
        Spacer(Modifier.height(6.dp))
        val ranked = summary.categories.filter { it.xp > 0 || it.minutes > 0 }
            .sortedByDescending { if (summary.minutes > 0) it.minutes else it.xp }
        val leader = ranked.firstOrNull()
        val leaderValue = leader?.let { if (summary.minutes > 0) it.minutes else it.xp }?.coerceAtLeast(1) ?: 1
        ranked.forEachIndexed { index, category ->
            if (index > 0) ClayDivider(verticalPadding = 0.dp)
            CategoryRow(
                category = category,
                fraction = ((if (summary.minutes > 0) category.minutes else category.xp).toFloat() / leaderValue.toFloat())
                    .coerceIn(0f, 1f),
                useMinutes = summary.minutes > 0,
            )
        }
    }
}

@Composable
private fun CategoryRow(
    category: CategoryBreakdown,
    fraction: Float,
    useMinutes: Boolean,
) {
    val accent = ClayPalette.accentById(category.accentId)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(38.dp)
                .clip(ClayShape.Pill)
                .background(accent.soft.copy(alpha = 0.75f)),
            contentAlignment = Alignment.Center,
        ) {
            Text(text = category.emoji, fontSize = 17.sp)
        }
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = category.name,
                style = MaterialTheme.typography.titleSmall,
                color = ClayInk.Primary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = buildString {
                    append("${category.xp} XP")
                    if (category.minutes > 0) append(" · ${DateX.formatMinutes(category.minutes)}")
                    append(" · ${category.completions} done")
                },
                style = MaterialTheme.typography.labelSmall,
                color = ClayInk.Muted,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(6.dp))
            ClayProgressBar(progress = fraction, accent = accent, height = 7.dp)
        }
        if (useMinutes && category.minutes > 0) {
            Spacer(Modifier.width(10.dp))
            Text(
                text = DateX.formatMinutes(category.minutes),
                style = MaterialTheme.typography.labelMedium,
                color = accent.deep,
                maxLines = 1,
            )
        }
    }
}

// ----------------------------------------------------------------- comparison

@Composable
private fun ComparisonCard(range: AnalyticsRange, summary: RangeSummary, state: AnalyticsState) {
    ClayCard(
        shape = ClayShape.Card,
        elevation = 12.dp,
        glowAlpha = 0.12f,
        contentPadding = PaddingValues(18.dp),
    ) {
        ClaySectionTitle(
            title = "Compared with ${summary.previousLabel}",
            eyebrow = "History",
            caption = "Same length period, straight from the log",
        )
        Spacer(Modifier.height(12.dp))
        CompareRow("XP", summary.xp, summary.previousXp)
        ClayDivider(verticalPadding = 0.dp)
        CompareRow("Targets reached", summary.completions, summary.previousCompletions)
        Spacer(Modifier.height(12.dp))
        ClayPanel(
            shape = ClayShape.Small,
            tint = ClayPalette.SurfaceMuted.copy(alpha = 0.6f),
        ) {
            Text(
                text = when (range) {
                    AnalyticsRange.Today ->
                        "Longest streak so far: ${state.streak.longest} days · current: ${state.streak.current} days"
                    else ->
                        "Average per active day this range: ${
                            if (summary.activeDays > 0) "${summary.xp / summary.activeDays} XP" else "—"
                        }"
                },
                style = MaterialTheme.typography.labelMedium,
                color = ClayInk.Secondary,
                maxLines = 2,
            )
        }
    }
}

@Composable
private fun CompareRow(label: String, current: Int, previous: Int) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                color = ClayInk.Primary,
                maxLines = 1,
            )
            Text(
                text = "now $current · before $previous",
                style = MaterialTheme.typography.labelSmall,
                color = ClayInk.Muted,
                maxLines = 1,
            )
        }
        val delta = current - previous
        val accent = if (delta >= 0) ClayPalette.Mint else ClayPalette.Peach
        ClayIconPill(
            text = if (delta > 0) "+$delta" else "$delta",
            accent = accent,
            emoji = when {
                delta > 0 -> "▲"
                delta < 0 -> "▼"
                else -> "➖"
            },
            emphasised = delta != 0,
        )
    }
}

// ---------------------------------------------------------------- consistency

@Composable
private fun ConsistencyCard(state: AnalyticsState) {
    ClayCard(
        shape = ClayShape.Card,
        elevation = 12.dp,
        glowAlpha = 0.12f,
        contentPadding = PaddingValues(18.dp),
    ) {
        ClaySectionTitle(
            title = "Consistency",
            eyebrow = "Habits",
            caption = "Days with at least one completed target",
        )
        Spacer(Modifier.height(12.dp))
        Row {
            ClayMiniStat(
                value = "${state.streak.current}",
                label = "Current streak",
                emoji = "🔥",
                accent = ClayPalette.Peach,
                modifier = Modifier.weight(1f),
            )
            ClayMiniStat(
                value = "${state.streak.longest}",
                label = "Longest streak",
                emoji = "🏔️",
                accent = ClayPalette.Lavender,
                modifier = Modifier.weight(1f),
            )
            ClayMiniStat(
                value = "${(state.consistencyRate * 100).toInt()}%",
                label = "Days active",
                emoji = "📈",
                accent = ClayPalette.Mint,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun ReadingCard(state: AnalyticsState) {
    ClayCard(
        shape = ClayShape.Card,
        elevation = 12.dp,
        glowAlpha = 0.12f,
        contentPadding = PaddingValues(18.dp),
    ) {
        ClaySectionTitle(
            title = "Reading",
            eyebrow = "Your reading category",
            caption = "Derived from quests you filed under a reading-style category",
        )
        Spacer(Modifier.height(12.dp))
        Row {
            ClayMiniStat(
                value = "${state.readingPages}",
                label = "Pages logged",
                emoji = "📖",
                accent = ClayPalette.Butter,
                modifier = Modifier.weight(1f),
            )
            ClayMiniStat(
                value = DateX.formatMinutes(state.readingMinutes),
                label = "Reading time",
                emoji = "⏳",
                accent = ClayPalette.Sky,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

// ------------------------------------------------------------------- lifetime

@Composable
private fun LifetimeCard(state: AnalyticsState) {
    ClayCard(
        shape = ClayShape.Card,
        elevation = 10.dp,
        glowAlpha = 0.1f,
        contentPadding = PaddingValues(18.dp),
    ) {
        ClaySectionTitle(
            title = "All time",
            eyebrow = "Lifetime",
            caption = state.lifetime.firstDay?.let { "Tracking since ${DateX.fullDate(it)}" }
                ?: "Nothing recorded yet",
            action = null,
        )
        Spacer(Modifier.height(12.dp))
        Row {
            ClayMiniStat(
                value = "${state.lifetime.totalXp}",
                label = "Total XP",
                emoji = "⚡",
                accent = ClayPalette.Butter,
                modifier = Modifier.weight(1f),
            )
            ClayMiniStat(
                value = "${state.lifetime.totalCompletions}",
                label = "Targets reached",
                emoji = "✅",
                accent = ClayPalette.Mint,
                modifier = Modifier.weight(1f),
            )
            ClayMiniStat(
                value = DateX.formatMinutes(state.lifetime.totalMinutes),
                label = "Time tracked",
                emoji = "⏳",
                accent = ClayPalette.Sky,
                modifier = Modifier.weight(1f),
            )
            ClayMiniStat(
                value = "${state.lifetime.activeDays}",
                label = "Active days",
                emoji = "📅",
                accent = ClayPalette.Lavender,
                modifier = Modifier.weight(1f),
            )
        }
    }
}
