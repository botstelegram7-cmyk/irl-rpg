package com.irlrpg.app.ui.history

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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
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
import com.irlrpg.app.domain.model.CompletionHistoryEntry
import androidx.compose.material3.DropdownMenuItem
import com.irlrpg.app.ui.design.ClayCard
import com.irlrpg.app.ui.design.ClayDivider
import com.irlrpg.app.ui.design.ClayDot
import com.irlrpg.app.ui.design.ClayDropdownChip
import com.irlrpg.app.ui.design.ClayIconPill
import com.irlrpg.app.ui.design.ClayMiniStat
import com.irlrpg.app.ui.design.ClayPanel
import com.irlrpg.app.ui.design.ClaySectionTitle
import com.irlrpg.app.ui.design.ClayChip
import com.irlrpg.app.ui.design.ClayEmptyState
import com.irlrpg.app.ui.design.ClayScaffold
import com.irlrpg.app.ui.design.ClayTopBar
import com.irlrpg.app.ui.theme.ClayInk
import com.irlrpg.app.ui.theme.ClayPalette
import com.irlrpg.app.ui.theme.ClayShape
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/**
 * Completion history: one row per recorded activity event, newest first.
 * This is the audit trail that proves achievements and challenge rewards came
 * from real work rather than from app opens.
 */
@Composable
fun HistoryScreen(viewModel: HistoryViewModel) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var questFilter by remember { mutableStateOf<Long?>(null) }

    val visible = state.entries.filter { questFilter == null || it.questId == questFilter }
    val grouped = visible.groupBy { it.date }

    ClayScaffold(
        topBar = {
            ClayTopBar(
                title = "History",
                subtitle = "${state.entries.size} logged events · ${state.totalXp} XP earned from them",
                accent = ClayPalette.Sky,
            )
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(top = padding.calculateTopPadding()),
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 120.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                HistorySummary(state = state, visible = visible)
            }

            if (state.questOptions.isNotEmpty()) {
                item {
                    val selected = state.questOptions.firstOrNull { it.questId == questFilter }
                    Row {
                        ClayDropdownChip(
                            text = selected?.let { it.title.take(24) } ?: "All quests",
                            emoji = selected?.emoji ?: "🧭",
                            accent = ClayPalette.Sky,
                            active = questFilter != null,
                            modifier = Modifier.weight(1f),
                        ) { dismiss ->
                            DropdownMenuItem(
                                text = { Text("All quests", style = MaterialTheme.typography.bodyMedium) },
                                onClick = { questFilter = null; dismiss() },
                            )
                            state.questOptions.forEach { option ->
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            text = "${option.emoji}  ${option.title}",
                                            style = MaterialTheme.typography.bodyMedium,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                        )
                                    },
                                    onClick = { questFilter = option.questId; dismiss() },
                                )
                            }
                        }
                    }
                }
            }

            if (visible.isEmpty()) {
                item {
                    ClayPanel(
                        shape = ClayShape.Card,
                        tint = ClayPalette.SurfaceCream,
                        contentPadding = PaddingValues(18.dp),
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            ClayDot(emoji = "🕰️", accent = ClayPalette.Sky, size = 40.dp)
                            Spacer(Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = if (questFilter == null) "No activity yet" else "Nothing logged for this quest",
                                    style = MaterialTheme.typography.titleSmall,
                                    color = ClayInk.Primary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                                Text(
                                    text = "Every completion you log lands here with the XP it earned — the audit trail behind your level.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = ClayInk.Muted,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                        }
                    }
                }
            }

            grouped.forEach { (date, entries) ->
                item(key = "day-$date") {
                    ClayCard(
                        shape = ClayShape.Card,
                        elevation = 12.dp,
                        glowAlpha = 0.12f,
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
                    ) {
                        DayHeader(date = date, xp = entries.sumOf { it.xp }, count = entries.size)
                        Spacer(Modifier.height(4.dp))
                        entries.forEachIndexed { index, entry ->
                            if (index > 0) ClayDivider(verticalPadding = 0.dp)
                            HistoryRow(entry)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun HistorySummary(state: HistoryUiState, visible: List<CompletionHistoryEntry>) {
    val days = visible.map { it.date }.distinct().size
    ClayCard(
        shape = ClayShape.Card,
        elevation = 14.dp,
        glowAlpha = 0.18f,
        contentPadding = PaddingValues(18.dp),
    ) {
        ClaySectionTitle(
            title = "Audit trail",
            eyebrow = "History",
            caption = "One row per logged entry — the proof behind every level and award",
        )
        Spacer(Modifier.height(12.dp))
        Row {
            ClayMiniStat(
                value = "${visible.size}",
                label = "Entries shown",
                emoji = "📝",
                accent = ClayPalette.Sky,
                modifier = Modifier.weight(1f),
            )
            ClayMiniStat(
                value = "${visible.sumOf { it.xp }}",
                label = "XP earned",
                emoji = "⚡",
                accent = ClayPalette.Butter,
                modifier = Modifier.weight(1f),
            )
            ClayMiniStat(
                value = "$days",
                label = "Active days",
                emoji = "📅",
                accent = ClayPalette.Lavender,
                modifier = Modifier.weight(1f),
            )
            ClayMiniStat(
                value = "${if (days > 0) visible.sumOf { it.xp } / days else 0}",
                label = "XP per day",
                emoji = "📈",
                accent = ClayPalette.Mint,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun DayHeader(date: LocalDate, xp: Int, count: Int) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = formatDay(date),
            style = MaterialTheme.typography.titleSmall,
            color = ClayInk.Primary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        Text(
            text = "$count ${if (count == 1) "entry" else "entries"} · +$xp XP",
            style = MaterialTheme.typography.labelSmall,
            color = ClayPalette.Butter.deep,
            maxLines = 1,
        )
    }
}

@Composable
private fun HistoryRow(entry: CompletionHistoryEntry) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ClayDot(
            emoji = if (entry.levelUp) "🎉" else entry.emoji,
            accent = if (entry.levelUp) ClayPalette.Butter else ClayPalette.Mint,
            size = 36.dp,
        )
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = entry.questTitle,
                style = MaterialTheme.typography.bodyMedium,
                color = ClayInk.Primary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = buildString {
                    if (entry.amountLabel.isNotBlank()) append(entry.amountLabel)
                    if (entry.levelUp) {
                        if (isNotEmpty()) append(" · ")
                        append("level up 🎉")
                    }
                }.ifBlank { "Logged" },
                style = MaterialTheme.typography.labelSmall,
                color = ClayInk.Muted,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Spacer(Modifier.width(8.dp))
        ClayIconPill(
            text = "+${entry.xp}",
            accent = ClayPalette.Butter,
            emoji = "⚡",
            emphasised = true,
        )
    }
}

private val dayFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("EEE d MMM yyyy")

private fun formatDay(date: LocalDate): String {
    val today = LocalDate.now()
    return when (date) {
        today -> "Today · ${date.format(dayFormatter)}"
        today.minusDays(1) -> "Yesterday · ${date.format(dayFormatter)}"
        else -> date.format(dayFormatter)
    }
}
