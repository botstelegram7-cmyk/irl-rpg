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
import com.irlrpg.app.ui.design.ClayCard
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
            if (state.questOptions.isNotEmpty()) {
                item {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        item {
                            ClayChip(
                                text = "All quests",
                                selected = questFilter == null,
                                onClick = { questFilter = null },
                                accent = ClayPalette.Lavender,
                            )
                        }
                        items(state.questOptions, key = { it.questId }) { option ->
                            ClayChip(
                                text = option.title,
                                selected = questFilter == option.questId,
                                onClick = { questFilter = option.questId },
                                accent = ClayPalette.Sky,
                                leadingEmoji = option.emoji,
                            )
                        }
                    }
                }
            }

            if (visible.isEmpty()) {
                item {
                    ClayEmptyState(
                        emoji = "🕰️",
                        title = "No activity yet",
                        message = "Every quest completion you log shows up here with its XP, so you can see exactly how your progress was earned.",
                    )
                }
            }

            grouped.forEach { (date, entries) ->
                item(key = "day-$date") {
                    DayHeader(date = date, xp = entries.sumOf { it.xp }, count = entries.size)
                }
                items(entries, key = { it.logId }) { entry ->
                    HistoryRow(entry)
                }
            }
        }
    }
}

@Composable
private fun DayHeader(date: LocalDate, xp: Int, count: Int) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(top = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = formatDay(date),
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            color = ClayInk.Secondary,
            modifier = Modifier.weight(1f),
        )
        Text(
            text = "$count events · +$xp XP",
            style = MaterialTheme.typography.labelSmall,
            color = ClayPalette.Butter.deep,
        )
    }
}

@Composable
private fun HistoryRow(entry: CompletionHistoryEntry) {
    val accent = if (entry.levelUp) ClayPalette.Butter else ClayPalette.Sky
    ClayCard(
        shape = ClayShape.Medium,
        elevation = 8.dp,
        glowAlpha = if (entry.levelUp) 0.18f else 0.1f,
        contentPadding = PaddingValues(14.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(ClayShape.Tile)
                    .background(accent.soft),
                contentAlignment = Alignment.Center,
            ) {
                Text(text = entry.emoji, fontSize = 18.sp)
            }
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
                        append(entry.amountLabel.ifBlank { "completed" })
                        append(" · quest completed")
                        if (entry.levelUp) append(" · level up")
                    },
                    style = MaterialTheme.typography.labelSmall,
                    color = if (entry.levelUp) ClayPalette.Butter.deep else ClayInk.Muted,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Spacer(Modifier.width(10.dp))
            Text(
                text = if (entry.xp > 0) "+${entry.xp}" else "—",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = if (entry.xp > 0) ClayPalette.Butter.deep else ClayInk.Faint,
            )
        }
    }
}

private val dayFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("EEE d MMM")

private fun formatDay(date: LocalDate): String {
    val today = LocalDate.now()
    return when (date) {
        today -> "Today · ${date.format(dayFormatter)}"
        today.minusDays(1) -> "Yesterday · ${date.format(dayFormatter)}"
        else -> date.format(dayFormatter)
    }
}
