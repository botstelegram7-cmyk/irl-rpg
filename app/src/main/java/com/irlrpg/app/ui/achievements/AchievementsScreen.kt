package com.irlrpg.app.ui.achievements

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
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
import com.irlrpg.app.domain.model.AchievementCategory
import com.irlrpg.app.domain.model.AchievementTier
import com.irlrpg.app.domain.model.AchievementView
import com.irlrpg.app.ui.design.ClayCard
import com.irlrpg.app.ui.design.ClayChip
import com.irlrpg.app.ui.design.ClayDivider
import com.irlrpg.app.ui.design.ClayDot
import com.irlrpg.app.ui.design.ClayEmptyState
import com.irlrpg.app.ui.design.ClayProgressBar
import com.irlrpg.app.ui.design.ClayScaffold
import com.irlrpg.app.ui.design.ClayTopBar
import com.irlrpg.app.ui.theme.ClayAccent
import com.irlrpg.app.ui.theme.ClayInk
import com.irlrpg.app.ui.theme.ClayPalette
import com.irlrpg.app.ui.theme.ClayShape
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/**
 * Achievements gallery.
 *
 * Locked entries show live, honest progress ("12 of 25 pages logged") and earned
 * entries show the date the activity was actually recorded — nothing here is
 * pre-marked complete, and no sample data is seeded.
 */
@Composable
fun AchievementsScreen(viewModel: AchievementsViewModel) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var category by remember { mutableStateOf<AchievementCategory?>(null) }

    val visible = state.achievements
        .filter { category == null || it.def.category == category }
        .sortedWith(
            compareByDescending<AchievementView> { it.earned }
                .thenBy { it.def.category.ordinal }
                .thenBy { it.def.tier.ordinal }
        )

    ClayScaffold(
        topBar = {
            ClayTopBar(
                title = "Achievements",
                subtitle = "${state.earnedCount} of ${state.totalCount} unlocked from your logged activity",
                accent = ClayPalette.Butter,
            )
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(top = padding.calculateTopPadding()),
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 120.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                ClayCard(
                    shape = ClayShape.Hero,
                    elevation = 16.dp,
                    glowAlpha = 0.22f,
                    contentPadding = PaddingValues(18.dp),
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(58.dp)
                                .clip(ClayShape.Tile)
                                .background(ClayPalette.Butter.soft),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(text = "🏅", fontSize = 26.sp)
                        }
                        Spacer(Modifier.width(14.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "${state.earnedCount} unlocked",
                                style = MaterialTheme.typography.titleMedium,
                                color = ClayInk.Primary,
                            )
                            Text(
                                text = if (state.recentCount > 0) {
                                    "${state.recentCount} earned in the last 7 days"
                                } else {
                                    "Keep logging quests — progress updates instantly"
                                },
                                style = MaterialTheme.typography.labelSmall,
                                color = ClayInk.Muted,
                            )
                        }
                    }
                    Spacer(Modifier.height(14.dp))
                    ClayProgressBar(
                        progress = if (state.totalCount == 0) 0f else state.earnedCount.toFloat() / state.totalCount,
                        accent = ClayPalette.Butter,
                        height = 12.dp,
                    )
                }
            }

            item {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    item {
                        ClayChip(
                            text = "All",
                            selected = category == null,
                            onClick = { category = null },
                            accent = ClayPalette.Lavender,
                        )
                    }
                    items(AchievementCategory.entries.toList()) { entry ->
                        ClayChip(
                            text = entry.label,
                            selected = category == entry,
                            onClick = { category = entry },
                            accent = categoryAccent(entry),
                            leadingEmoji = entry.emoji,
                        )
                    }
                }
            }

            if (visible.isEmpty()) {
                item {
                    ClayEmptyState(
                        emoji = "🌱",
                        title = "Nothing here yet",
                        message = "Earned achievements appear the moment the underlying activity is logged — nothing is ever pre-marked as complete.",
                    )
                }
            }

            // Achievements are grouped into one card per category with flat rows:
            // a wall of 30 raised cards told the eye nothing about progress.
            val groups = AchievementCategory.entries.mapNotNull { group ->
                visible.filter { it.def.category == group }.takeIf { it.isNotEmpty() }
            }
            items(groups, key = { group -> group.first().def.category.name }) { group ->
                AchievementGroup(group)
            }
        }
    }
}

@Composable
private fun AchievementGroup(entries: List<AchievementView>) {
    val category = entries.first().def.category
    val accent = categoryAccent(category)
    val earned = entries.count { it.earned }

    ClayCard(
        shape = ClayShape.Card,
        elevation = 12.dp,
        glowAlpha = 0.14f,
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            ClayDot(emoji = category.emoji, accent = accent, size = 34.dp)
            Spacer(Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = category.label,
                    style = MaterialTheme.typography.titleSmall,
                    color = ClayInk.Primary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = "$earned of ${entries.size} unlocked",
                    style = MaterialTheme.typography.labelSmall,
                    color = ClayInk.Muted,
                    maxLines = 1,
                )
            }
            Spacer(Modifier.width(10.dp))
            Box(modifier = Modifier.width(72.dp)) {
                ClayProgressBar(
                    progress = if (entries.isEmpty()) 0f else earned.toFloat() / entries.size.toFloat(),
                    accent = accent,
                    height = 8.dp,
                )
            }
        }
        Spacer(Modifier.height(4.dp))
        entries.forEachIndexed { index, entry ->
            if (index > 0) ClayDivider(verticalPadding = 0.dp)
            AchievementRow(entry)
        }
    }
}

@Composable
private fun AchievementRow(entry: AchievementView) {
    val def = entry.def
    val accent = ClayPalette.accentById(def.tier.accentId)
    val fraction by animateFloatAsState(
        targetValue = if (entry.earned) 1f else entry.fraction,
        animationSpec = tween(500),
        label = "achievementProgress",
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Flat badge: nested cards are gone, only the group carries elevation.
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(ClayShape.Pill)
                .background(if (entry.earned) accent.soft else ClayPalette.SurfaceMuted.copy(alpha = 0.7f)),
            contentAlignment = Alignment.Center,
        ) {
            Text(text = if (entry.earned) def.emoji else "🔒", fontSize = 20.sp)
        }
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = def.name,
                    style = MaterialTheme.typography.titleSmall,
                    color = if (entry.earned) ClayInk.Primary else ClayInk.Secondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false),
                )
                Spacer(Modifier.width(8.dp))
                TierBadge(def.tier)
            }
            Spacer(Modifier.height(3.dp))
            Text(
                text = def.description,
                style = MaterialTheme.typography.bodySmall,
                color = ClayInk.Muted,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(8.dp))
            if (entry.earned) {
                Text(
                    text = buildString {
                        append("Earned ")
                        append(formatEpochDay(entry.earnedEpochDay))
                        def.unlocksCosmetic?.let { append(" · unlocks a reward in Customize") }
                    },
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = accent.deep,
                    maxLines = 2,
                )
            } else {
                ClayProgressBar(progress = fraction, accent = accent, height = 8.dp)
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "${entry.progress.coerceAtMost(def.target)} of ${def.target} recorded so far",
                    style = MaterialTheme.typography.labelSmall,
                    color = ClayInk.Faint,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
private fun TierBadge(tier: AchievementTier) {
    val accent = ClayPalette.accentById(tier.accentId)
    Box(
        modifier = Modifier
            .clip(ClayShape.Pill)
            .background(accent.soft)
            .padding(horizontal = 10.dp, vertical = 4.dp),
    ) {
        Text(
            text = tier.label,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold,
            color = accent.deep,
        )
    }
}

private fun categoryAccent(category: AchievementCategory): ClayAccent = when (category) {
    AchievementCategory.Progress -> ClayPalette.Mint
    AchievementCategory.Consistency -> ClayPalette.Peach
    AchievementCategory.Mastery -> ClayPalette.Lavender
    AchievementCategory.Challenges -> ClayPalette.Blush
    AchievementCategory.Creator -> ClayPalette.Sky
}

private val dateFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("d MMM yyyy")

private fun formatEpochDay(epochDay: Long?): String =
    if (epochDay == null || epochDay <= 0L) "—" else LocalDate.ofEpochDay(epochDay).format(dateFormatter)
