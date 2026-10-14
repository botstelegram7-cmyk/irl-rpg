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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.irlrpg.app.ui.components.ClayToast
import com.irlrpg.app.ui.design.ClayAvatar
import com.irlrpg.app.ui.design.ClayCard
import com.irlrpg.app.ui.design.ClayProgressBar
import com.irlrpg.app.ui.design.ClayScaffold
import com.irlrpg.app.ui.design.ClaySectionHeader
import com.irlrpg.app.ui.design.ClayTopBar
import com.irlrpg.app.ui.theme.ClayInk
import com.irlrpg.app.ui.theme.ClayPalette
import com.irlrpg.app.ui.theme.ClayShape

/**
 * Customize the character: avatar frame and title.
 *
 * Cosmetics are decorative only. Locked entries explain exactly which achievement
 * unlocks them and how far along that achievement currently is, and nothing can be
 * bought — real-life progress is never behind a paywall.
 */
@Composable
fun CustomizeScreen(
    viewModel: CustomizeViewModel,
    onBack: () -> Unit,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val message by viewModel.message.collectAsStateWithLifecycle()

    ClayScaffold(
        topBar = {
            ClayTopBar(
                title = "Customize",
                subtitle = "${state.unlockedCount} of ${state.totalCount} rewards unlocked",
                onBack = onBack,
                accent = ClayPalette.Lavender,
            )
        },
    ) { padding ->
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            modifier = Modifier.fillMaxSize().padding(top = padding.calculateTopPadding()),
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 120.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item(span = { androidx.compose.foundation.lazy.grid.GridItemSpan(2) }) {
                ClayCard(
                    shape = ClayShape.Hero,
                    elevation = 16.dp,
                    glowAlpha = 0.24f,
                    contentPadding = PaddingValues(18.dp),
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        ClayAvatar(
                            emoji = "🧭",
                            level = state.level,
                            progress = 0f,
                            accent = ClayPalette.Lavender,
                            avatarSize = 78.dp,
                        )
                        Spacer(Modifier.width(16.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = state.playerName,
                                style = MaterialTheme.typography.titleMedium,
                                color = ClayInk.Primary,
                            )
                            Text(
                                text = state.titles.firstOrNull { it.selected }?.item?.name
                                    ?: state.rankTitle,
                                style = MaterialTheme.typography.labelMedium,
                                color = ClayPalette.Lavender.deep,
                            )
                            Spacer(Modifier.height(10.dp))
                            ClayProgressBar(
                                progress = if (state.totalCount == 0) 0f
                                else state.unlockedCount.toFloat() / state.totalCount,
                                accent = ClayPalette.Butter,
                                height = 10.dp,
                            )
                        }
                    }
                }
            }

            item(span = { androidx.compose.foundation.lazy.grid.GridItemSpan(2) }) {
                ClaySectionHeader(
                    title = "Avatar frames",
                    subtitle = "Equip any unlocked frame — it changes only how your avatar looks",
                )
            }
            items(state.frames, key = { it.item.code }) { row ->
                CosmeticTile(
                    row = row,
                    onClick = { viewModel.selectFrame(row.item) },
                )
            }

            item(span = { androidx.compose.foundation.lazy.grid.GridItemSpan(2) }) {
                ClaySectionHeader(
                    title = "Titles",
                    subtitle = "Shown under your name on the dashboard. Tap an equipped title to remove it.",
                )
            }
            items(state.titles, key = { it.item.code }) { row ->
                CosmeticTile(
                    row = row,
                    onClick = { viewModel.selectTitle(row.item) },
                )
            }
        }
    }

    ClayToast(message = message, onShown = viewModel::consumeMessage, bottomPadding = 108.dp)
}

@Composable
private fun CosmeticTile(row: CosmeticRow, onClick: () -> Unit) {
    val accent = ClayPalette.accentById(row.item.accentId)
    ClayCard(
        modifier = Modifier.fillMaxWidth(),
        shape = ClayShape.Card,
        elevation = if (row.selected) 16.dp else 8.dp,
        glowAlpha = if (row.selected) 0.3f else 0.08f,
        onClick = onClick,
        contentPadding = PaddingValues(14.dp),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            contentAlignment = Alignment.Center,
        ) {
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(ClayShape.Pill)
                    .background(if (row.unlocked) accent.soft else ClayPalette.SurfaceMuted),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = if (row.unlocked) row.item.emoji else "🔒",
                    fontSize = 24.sp,
                )
            }
        }
        Spacer(Modifier.height(10.dp))
        Text(
            text = row.item.name,
            style = MaterialTheme.typography.titleSmall,
            color = if (row.unlocked) ClayInk.Primary else ClayInk.Muted,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = when {
                row.selected -> "Equipped"
                row.unlocked -> "Tap to equip"
                else -> row.requirement
            },
            style = MaterialTheme.typography.labelSmall,
            color = when {
                row.selected -> ClayPalette.Mint.deep
                row.unlocked -> ClayInk.Faint
                else -> ClayPalette.Peach.deep
            },
            maxLines = 3,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
            fontWeight = if (row.selected) FontWeight.SemiBold else FontWeight.Normal,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}
