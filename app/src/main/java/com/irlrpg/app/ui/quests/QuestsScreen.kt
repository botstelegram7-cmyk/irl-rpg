package com.irlrpg.app.ui.quests

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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.irlrpg.app.data.local.entity.CategoryEntity
import com.irlrpg.app.data.repository.QuestProgress
import com.irlrpg.app.domain.model.QuestWithProgress
import com.irlrpg.app.domain.model.ScheduleKind
import com.irlrpg.app.domain.model.TrackingKind
import com.irlrpg.app.ui.common.accentFor
import com.irlrpg.app.ui.components.CelebrationOverlay
import com.irlrpg.app.ui.components.ClayConfirmDialog
import com.irlrpg.app.ui.components.ClayToast
import com.irlrpg.app.ui.components.QuickLogSheet
import com.irlrpg.app.ui.design.ClayButton
import com.irlrpg.app.ui.design.ClayButtonStyle
import com.irlrpg.app.ui.design.ClayCard
import com.irlrpg.app.ui.design.ClayChip
import com.irlrpg.app.ui.design.ClayEmptyState
import com.irlrpg.app.ui.design.ClayFab
import com.irlrpg.app.ui.design.ClayIconButton
import com.irlrpg.app.ui.design.ClayIconTile
import com.irlrpg.app.ui.design.ClayProgressBar
import com.irlrpg.app.ui.design.ClayScaffold
import com.irlrpg.app.ui.design.ClaySectionHeader
import com.irlrpg.app.ui.design.ClayTopBar
import com.irlrpg.app.ui.theme.ClayInk
import com.irlrpg.app.ui.theme.ClayPalette
import com.irlrpg.app.ui.theme.ClayShape

@Composable
fun QuestsScreen(
    viewModel: QuestsViewModel,
    bottomBar: @Composable () -> Unit,
    onCreateQuest: () -> Unit,
    onOpenQuest: (Long) -> Unit,
    onEditQuest: (Long) -> Unit,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val message by viewModel.message.collectAsStateWithLifecycle()
    val celebration by viewModel.celebration.collectAsStateWithLifecycle()
    var quickLogItem by remember { mutableStateOf<QuestWithProgress?>(null) }
    var showCategories by remember { mutableStateOf(false) }
    var pendingDelete by remember { mutableStateOf<QuestWithProgress?>(null) }

    LifecycleResumeEffect(Unit) {
        viewModel.refreshDay()
        onPauseOrDispose { }
    }

    ClayScaffold(
        topBar = {
            ClayTopBar(
                title = "Quest board",
                subtitle = if (state.loading) "Loading…" else "${state.quests.size} quests · ${state.categories.size} categories",
                accent = ClayPalette.Mint,
            )
        },
        bottomBar = bottomBar,
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize()) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    start = 20.dp,
                    end = 20.dp,
                    top = padding.calculateTopPadding() + 4.dp,
                    bottom = padding.calculateBottomPadding() + 96.dp,
                ),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                item {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(QuestFilter.entries.toList()) { filter ->
                            ClayChip(
                                text = filter.label,
                                selected = state.filter == filter,
                                onClick = { viewModel.setFilter(filter) },
                                accent = ClayPalette.Mint,
                            )
                        }
                    }
                }

                item {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        item {
                            ClayChip(
                                text = "All categories",
                                selected = state.categoryFilter == null,
                                onClick = { viewModel.setCategoryFilter(null) },
                                accent = ClayPalette.Lavender,
                            )
                        }
                        items(state.categories, key = { it.id }) { category ->
                            ClayChip(
                                text = category.name,
                                selected = state.categoryFilter == category.id,
                                onClick = { viewModel.setCategoryFilter(category.id) },
                                accent = ClayPalette.accentById(category.accentId),
                                leadingEmoji = category.emoji,
                            )
                        }
                        item {
                            ClayChip(
                                text = "Manage",
                                selected = false,
                                onClick = { showCategories = true },
                                accent = ClayPalette.Sky,
                                leadingEmoji = "⚙️",
                            )
                        }
                    }
                }

                item {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(QuestSort.entries.toList()) { sort ->
                            ClayChip(
                                text = "Sort: ${sort.label}",
                                selected = state.sort == sort,
                                onClick = { viewModel.setSort(sort) },
                                accent = ClayPalette.Sky,
                            )
                        }
                    }
                }

                if (state.quests.isEmpty() && !state.loading) {
                    item {
                        ClayEmptyState(
                            emoji = if (state.filter == QuestFilter.Paused) "⏸️" else "🧩",
                            title = when (state.filter) {
                                QuestFilter.All -> "No quests yet"
                                QuestFilter.Today -> "Nothing scheduled today"
                                QuestFilter.Active -> "No active quests"
                                QuestFilter.Paused -> "No paused quests"
                            },
                            message = "Activities are yours to define — reading pages, writing words, coding hours, training sets, practising an instrument, anything measurable.",
                            actionLabel = "Create a quest",
                            onAction = onCreateQuest,
                        )
                    }
                } else {
                    items(state.quests, key = { it.quest.id }) { item ->
                        QuestBoardCard(
                            item = item,
                            onOpen = { onOpenQuest(item.quest.id) },
                            onLog = {
                                if (item.quest.trackingKind == TrackingKind.Milestone.name) {
                                    viewModel.log(item.quest.id, 1L)
                                } else {
                                    quickLogItem = item
                                }
                            },
                            onEdit = { onEditQuest(item.quest.id) },
                            onDuplicate = { viewModel.duplicateQuest(item.quest.id) },
                            onToggleActive = { viewModel.toggleActive(item.quest.id) },
                            onDelete = { pendingDelete = item },
                        )
                    }
                }
            }

            ClayFab(
                emoji = "✨",
                onClick = onCreateQuest,
                contentDescription = "New quest",
                accent = ClayPalette.Mint,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 20.dp, bottom = 104.dp),
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

    CelebrationOverlay(
        result = celebration,
        onDismiss = viewModel::dismissCelebration,
    )

    if (showCategories) {
        CategoriesSheet(
            categories = state.categories,
            questCounts = state.quests.groupingBy { it.quest.categoryId }.eachCount(),
            onDismiss = { showCategories = false },
            onAdd = viewModel::addCategory,
            onRename = viewModel::renameCategory,
            onDelete = viewModel::deleteCategory,
        )
    }

    pendingDelete?.let { item ->
        ClayConfirmDialog(
            title = "Delete \"${item.quest.title}\"?",
            message = "This removes the quest and its recorded progress (${item.completions} completions). Player XP is corrected accordingly.",
            confirmLabel = "Delete quest",
            onConfirm = {
                viewModel.deleteQuest(item.quest.id)
                pendingDelete = null
            },
            onDismiss = { pendingDelete = null },
        )
    }

    ClayToast(
        message = message,
        onShown = viewModel::consumeMessage,
        bottomPadding = 104.dp,
    )
}

@Composable
private fun QuestBoardCard(
    item: QuestWithProgress,
    onOpen: () -> Unit,
    onLog: () -> Unit,
    onEdit: () -> Unit,
    onDuplicate: () -> Unit,
    onToggleActive: () -> Unit,
    onDelete: () -> Unit,
) {
    var menuOpen by remember { mutableStateOf(false) }
    val accent = accentFor(item)
    val quest = item.quest

    ClayCard(
        shape = ClayShape.Card,
        elevation = if (quest.isActive) 13.dp else 7.dp,
        glowAlpha = if (quest.isActive) 0.2f else 0.1f,
        contentPadding = PaddingValues(16.dp),
        onClick = onOpen,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            ClayIconTile(
                emoji = quest.emoji,
                accent = accent,
                size = 48.dp,
            )
            Spacer(Modifier.width(13.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = quest.title,
                    style = MaterialTheme.typography.titleSmall,
                    color = if (quest.isActive) ClayInk.Primary else ClayInk.Muted,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = buildString {
                        append(item.category?.let { "${it.emoji} ${it.name}" } ?: "🗂️ Uncategorised")
                        append(" · ")
                        append(ScheduleKind.fromName(quest.scheduleKind).label)
                        append(" · ")
                        append("${quest.xpReward} XP")
                    },
                    style = MaterialTheme.typography.labelSmall,
                    color = ClayInk.Muted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Box {
                ClayIconButton(
                    icon = Icons.Rounded.MoreVert,
                    onClick = { menuOpen = true },
                    contentDescription = "Quest options",
                    accent = ClayPalette.Lavender,
                    size = 38.dp,
                )
                DropdownMenu(
                    expanded = menuOpen,
                    onDismissRequest = { menuOpen = false },
                    containerColor = ClayPalette.SurfaceCream,
                    shape = ClayShape.Medium,
                ) {
                    DropdownMenuItem(
                        text = { Text("Edit quest", style = MaterialTheme.typography.bodyMedium) },
                        onClick = { menuOpen = false; onEdit() },
                    )
                    DropdownMenuItem(
                        text = { Text("Duplicate", style = MaterialTheme.typography.bodyMedium) },
                        onClick = { menuOpen = false; onDuplicate() },
                    )
                    DropdownMenuItem(
                        text = {
                            Text(
                                if (quest.isActive) "Pause" else "Resume",
                                style = MaterialTheme.typography.bodyMedium,
                            )
                        },
                        onClick = { menuOpen = false; onToggleActive() },
                    )
                    DropdownMenuItem(
                        text = {
                            Text(
                                "Delete",
                                style = MaterialTheme.typography.bodyMedium,
                                color = ClayPalette.Blush.deep,
                            )
                        },
                        onClick = { menuOpen = false; onDelete() },
                    )
                }
            }
        }

        if (quest.trackingKind != TrackingKind.Milestone.name || item.isComplete) {
            Spacer(Modifier.height(12.dp))
            ClayProgressBar(
                progress = item.fraction,
                accent = if (item.isComplete) ClayPalette.Mint else accent,
                height = 10.dp,
            )
        }

        Spacer(Modifier.height(12.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = buildString {
                    if (quest.trackingKind == TrackingKind.Milestone.name) {
                        append(if (item.isComplete) "Completed" else "One-off milestone")
                    } else {
                        append("${item.progress.coerceAtMost(item.target)}/${item.target} ")
                        append(quest.customUnit ?: "")
                        append(" ")
                        append(QuestProgress.periodLabel(quest))
                    }
                }.trim(),
                style = MaterialTheme.typography.labelSmall,
                color = ClayInk.Secondary,
                modifier = Modifier.weight(1f),
            )
            if (item.isComplete) {
                Box(
                    modifier = Modifier
                        .clip(ClayShape.Pill)
                        .background(ClayPalette.Mint.soft)
                        .padding(horizontal = 12.dp, vertical = 7.dp),
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Rounded.Check,
                            contentDescription = null,
                            tint = ClayPalette.Mint.deep,
                            modifier = Modifier.size(15.dp),
                        )
                        Spacer(Modifier.width(5.dp))
                        Text(
                            text = "Done",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = ClayPalette.Mint.deep,
                        )
                    }
                }
            } else if (quest.isActive) {
                ClayButton(
                    text = if (quest.trackingKind == TrackingKind.Milestone.name) "Complete" else "Log progress",
                    onClick = onLog,
                    accent = accent,
                    height = 40.dp,
                    style = ClayButtonStyle.Soft,
                )
            } else {
                ClayButton(
                    text = "Paused",
                    onClick = onToggleActive,
                    accent = ClayPalette.Sky,
                    height = 40.dp,
                    style = ClayButtonStyle.Soft,
                )
            }
        }
    }
}

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
private fun CategoriesSheet(
    categories: List<CategoryEntity>,
    questCounts: Map<Long?, Int>,
    onDismiss: () -> Unit,
    onAdd: (String, String, String) -> Unit,
    onRename: (CategoryEntity, String, String, String) -> Unit,
    onDelete: (CategoryEntity) -> Unit,
) {
    var editing by remember { mutableStateOf<CategoryEntity?>(null) }
    var name by remember { mutableStateOf("") }
    var emoji by remember { mutableStateOf("📚") }
    var accentId by remember { mutableStateOf("lavender") }
    var pendingDelete by remember { mutableStateOf<CategoryEntity?>(null) }

    androidx.compose.material3.ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = ClayPalette.SurfaceCream,
        shape = ClayShape.Hero,
        dragHandle = null,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 22.dp),
        ) {
            ClaySectionHeader(
                title = if (editing == null) "Your categories" else "Edit category",
                subtitle = "Group activities your way — rename, re-colour or remove any of them",
            )
            Spacer(Modifier.height(14.dp))

            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(categories, key = { it.id }) { category ->
                    ClayChip(
                        text = "${category.name} · ${questCounts[category.id] ?: 0}",
                        selected = editing?.id == category.id,
                        onClick = {
                            editing = category
                            name = category.name
                            emoji = category.emoji
                            accentId = category.accentId
                        },
                        accent = ClayPalette.accentById(category.accentId),
                        leadingEmoji = category.emoji,
                    )
                }
            }

            Spacer(Modifier.height(18.dp))
            com.irlrpg.app.ui.design.ClayTextField(
                value = name,
                onValueChange = { name = it },
                label = "Category name",
                placeholder = "e.g. Music practice",
                accent = ClayPalette.accentById(accentId),
            )
            Spacer(Modifier.height(14.dp))
            Text(
                text = "Icon",
                style = MaterialTheme.typography.labelMedium,
                color = ClayInk.Secondary,
                modifier = Modifier.padding(start = 4.dp, bottom = 8.dp),
            )
            EmojiRow(selected = emoji, onSelect = { emoji = it })
            Spacer(Modifier.height(14.dp))
            Text(
                text = "Colour",
                style = MaterialTheme.typography.labelMedium,
                color = ClayInk.Secondary,
                modifier = Modifier.padding(start = 4.dp, bottom = 8.dp),
            )
            LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                items(ClayPalette.accentRotation.map { ClayPalette.accentIdOf(it) }) { id ->
                    val accent = ClayPalette.accentById(id)
                    ClayChip(
                        text = id.replaceFirstChar { it.uppercase() },
                        selected = accentId == id,
                        onClick = { accentId = id },
                        accent = accent,
                    )
                }
            }

            Spacer(Modifier.height(20.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                if (editing != null) {
                    ClayButton(
                        text = "Delete",
                        onClick = { pendingDelete = editing },
                        accent = ClayPalette.Blush,
                        style = ClayButtonStyle.Soft,
                        modifier = Modifier.weight(1f),
                    )
                }
                ClayButton(
                    text = if (editing == null) "Add category" else "Save changes",
                    onClick = {
                        val current = editing
                        if (current == null) onAdd(name, emoji, accentId) else onRename(current, name, emoji, accentId)
                        editing = null
                        name = ""
                    },
                    modifier = Modifier.weight(1.4f),
                    accent = ClayPalette.accentById(accentId),
                )
            }
            if (editing != null) {
                Spacer(Modifier.height(8.dp))
                ClayButton(
                    text = "Cancel editing",
                    onClick = {
                        editing = null
                        name = ""
                    },
                    style = ClayButtonStyle.Plain,
                    accent = ClayPalette.Lavender,
                    height = 42.dp,
                )
            }
            Spacer(Modifier.height(12.dp))
            Text(
                text = "Deleting a category keeps its quests — they simply become uncategorised.",
                style = MaterialTheme.typography.labelSmall,
                color = ClayInk.Faint,
            )
        }
    }

    pendingDelete?.let { category ->
        ClayConfirmDialog(
            title = "Delete \"${category.name}\"?",
            message = "Quests in this category keep their history and become uncategorised.",
            confirmLabel = "Delete category",
            onConfirm = {
                onDelete(category)
                pendingDelete = null
                editing = null
            },
            onDismiss = { pendingDelete = null },
        )
    }
}

@Composable
internal fun EmojiRow(
    selected: String,
    onSelect: (String) -> Unit,
    emojis: List<String> = defaultEmojis,
) {
    LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        items(emojis) { item ->
            val isSelected = item == selected
            Box(
                modifier = Modifier
                    .size(if (isSelected) 50.dp else 44.dp)
                    .clip(ClayShape.Tile)
                    .background(if (isSelected) ClayPalette.Lavender.soft else ClayPalette.SurfaceMuted.copy(alpha = 0.6f))
                    .clickable { onSelect(item) },
                contentAlignment = Alignment.Center,
            ) {
                Text(text = item, fontSize = if (isSelected) 22.sp else 19.sp)
            }
        }
    }
}

internal val defaultEmojis = listOf(
    "📘", "📚", "✍️", "💻", "🎓", "🧠", "💪", "🏃", "🧘", "🎨",
    "🎸", "🎹", "🗣️", "🧮", "🔬", "📈", "🛠️", "🍳", "🌱", "🚀",
    "🧩", "⏱️", "🎯", "🏅",
)
