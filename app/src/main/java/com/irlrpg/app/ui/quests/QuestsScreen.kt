package com.irlrpg.app.ui.quests

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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.irlrpg.app.data.local.entity.CategoryEntity
import com.irlrpg.app.data.repository.QuestProgress
import com.irlrpg.app.domain.model.QuestDifficulty
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
import com.irlrpg.app.ui.design.ClayDivider
import com.irlrpg.app.ui.design.ClayDot
import com.irlrpg.app.ui.design.ClayDropdownChip
import com.irlrpg.app.ui.design.ClayFab
import com.irlrpg.app.ui.design.ClayIconButton
import com.irlrpg.app.ui.design.ClayIconPill
import com.irlrpg.app.ui.design.ClayPanel
import com.irlrpg.app.ui.design.ClayProgressBar
import com.irlrpg.app.ui.design.ClayScaffold
import com.irlrpg.app.ui.design.ClaySectionHeader
import com.irlrpg.app.ui.design.ClaySectionTitle
import com.irlrpg.app.ui.design.ClaySegmentedControl
import com.irlrpg.app.ui.design.ClayTextField
import com.irlrpg.app.ui.design.ClayTopBar
import com.irlrpg.app.ui.theme.ClayInk
import com.irlrpg.app.ui.theme.ClayPalette
import com.irlrpg.app.ui.theme.ClayShape

/**
 * Quest board.
 *
 * The board is organised by category (each category is one card with flat rows)
 * rather than one raised card per quest, which keeps long lists calm. Filtering
 * is now a single compact bar: a segmented control for the four primary filters
 * and two dropdown chips for category and sorting — no more three rows of
 * horizontally overflowing chips.
 */
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

    val primaryFilters = listOf(QuestFilter.All, QuestFilter.Today, QuestFilter.Active, QuestFilter.Paused)
    val secondaryFilters = listOf(QuestFilter.Challenges, QuestFilter.MultiStep)
    val filtersActive = state.query.isNotBlank() ||
        state.categoryFilter != null ||
        state.filter != QuestFilter.All

    ClayScaffold(
        topBar = {
            ClayTopBar(
                title = "Quest board",
                subtitle = if (state.loading) {
                    "Loading…"
                } else {
                    "${state.quests.size} shown · ${state.categories.size} categories · offline"
                },
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
                    ClayTextField(
                        value = state.query,
                        onValueChange = viewModel::setQuery,
                        label = "",
                        placeholder = "Search quests, notes or categories",
                        leadingIcon = Icons.Rounded.Search,
                        accent = ClayPalette.Mint,
                    )
                }

                item {
                    ClaySegmentedControl(
                        options = primaryFilters,
                        selected = if (state.filter in primaryFilters) state.filter else QuestFilter.All,
                        onSelect = viewModel::setFilter,
                        label = { it.label },
                        accent = ClayPalette.Mint,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }

                item {
                    FilterControls(
                        state = state,
                        secondaryFilters = secondaryFilters,
                        onCategory = viewModel::setCategoryFilter,
                        onSort = viewModel::setSort,
                        onFilter = viewModel::setFilter,
                        onManageCategories = { showCategories = true },
                    )
                }

                if (filtersActive) {
                    item {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = activeFilterSummary(state),
                                style = MaterialTheme.typography.labelSmall,
                                color = ClayInk.Muted,
                                modifier = Modifier.weight(1f),
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                            )
                            TextButton(onClick = viewModel::clearFilters) {
                                Text(
                                    text = "Clear",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = ClayPalette.Mint.deep,
                                )
                            }
                        }
                    }
                }

                if (state.quests.isEmpty() && !state.loading) {
                    item {
                        ClayPanel(
                            shape = ClayShape.Card,
                            tint = ClayPalette.SurfaceCream,
                            contentPadding = PaddingValues(18.dp),
                            onClick = onCreateQuest,
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                ClayDot(emoji = if (filtersActive) "🔍" else "🧩", accent = ClayPalette.Mint, size = 40.dp)
                                Spacer(Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = if (filtersActive) "Nothing matches these filters" else "No quests yet",
                                        style = MaterialTheme.typography.titleSmall,
                                        color = ClayInk.Primary,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                    Text(
                                        text = if (filtersActive) {
                                            "Adjust the filters or clear them to see the rest of your board."
                                        } else {
                                            "Reading pages, writing words, coding hours, workouts, practice — anything measurable."
                                        },
                                        style = MaterialTheme.typography.bodySmall,
                                        color = ClayInk.Muted,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                    Spacer(Modifier.height(6.dp))
                                    Text(
                                        text = if (filtersActive) "Tap to create a new quest" else "Tap to create your first quest",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = ClayPalette.Mint.deep,
                                        maxLines = 1,
                                    )
                                }
                            }
                        }
                    }
                } else {
                    val sections = groupByCategory(state.quests, state.categories)
                    items(sections, key = { it.key }) { section ->
                        QuestSection(
                            section = section,
                            onOpen = onOpenQuest,
                            onLog = { item ->
                                if (item.quest.trackingKind == TrackingKind.Milestone.name && !item.isComplete) {
                                    viewModel.log(item.quest.id, 1L)
                                } else {
                                    quickLogItem = item
                                }
                            },
                            onEdit = onEditQuest,
                            onDuplicate = viewModel::duplicateQuest,
                            onToggleActive = viewModel::toggleActive,
                            onDelete = { pendingDelete = it },
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

private fun activeFilterSummary(state: QuestsUiState): String = buildList {
    if (state.query.isNotBlank()) add("search \"${state.query.trim()}\"")
    if (state.categoryFilter != null) {
        val name = state.categories.firstOrNull { it.id == state.categoryFilter }?.name
        if (name != null) add("category $name")
    }
    if (state.filter != QuestFilter.All) add("filter ${state.filter.label}")
}.joinToString(" · ").let { if (it.isBlank()) "Showing everything" else it }

/** Category-scoped grouping: the section key is stable so LazyColumn can reuse it. */
private data class QuestSectionData(
    val key: String,
    val title: String,
    val emoji: String,
    val categoryId: Long?,
    val items: List<QuestWithProgress>,
)

private fun groupByCategory(
    quests: List<QuestWithProgress>,
    categories: List<CategoryEntity>,
): List<QuestSectionData> {
    val byCategory = quests.groupBy { it.quest.categoryId }
    val ordered = mutableListOf<QuestSectionData>()
    categories.forEach { category ->
        byCategory[category.id]?.takeIf { it.isNotEmpty() }?.let { rows ->
            ordered += QuestSectionData(
                key = "cat-${category.id}",
                title = category.name,
                emoji = category.emoji,
                categoryId = category.id,
                items = rows,
            )
        }
    }
    byCategory[null]?.takeIf { it.isNotEmpty() }?.let { rows ->
        ordered += QuestSectionData("cat-none", "Uncategorised", "🗂️", null, rows)
    }
    return ordered
}

// ------------------------------------------------------- compact filter bar

@Composable
private fun FilterControls(
    state: QuestsUiState,
    secondaryFilters: List<QuestFilter>,
    onCategory: (Long?) -> Unit,
    onSort: (QuestSort) -> Unit,
    onFilter: (QuestFilter) -> Unit,
    onManageCategories: () -> Unit,
) {
    val selectedCategory = state.categories.firstOrNull { it.id == state.categoryFilter }

    Column {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ClayDropdownChip(
                text = selectedCategory?.let { "Category: ${it.name}" } ?: "All categories",
                emoji = selectedCategory?.emoji ?: "🗂️",
                accent = ClayPalette.Lavender,
                active = state.categoryFilter != null,
                modifier = Modifier.weight(1f),
            ) { dismiss ->
                DropdownMenuItem(
                    text = { Text("All categories", style = MaterialTheme.typography.bodyMedium) },
                    onClick = { onCategory(null); dismiss() },
                )
                state.categories.forEach { category ->
                    DropdownMenuItem(
                        text = {
                            Text(
                                text = "${category.emoji}  ${category.name}",
                                style = MaterialTheme.typography.bodyMedium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        },
                        onClick = { onCategory(category.id); dismiss() },
                    )
                }
                DropdownMenuItem(
                    text = { Text("Manage categories…", style = MaterialTheme.typography.bodyMedium) },
                    onClick = { onManageCategories(); dismiss() },
                )
            }
            ClayDropdownChip(
                text = "Sort: ${state.sort.label}",
                emoji = "↕️",
                accent = ClayPalette.Sky,
                active = false,
                modifier = Modifier.weight(1f),
            ) { dismiss ->
                QuestSort.entries.forEach { sort ->
                    DropdownMenuItem(
                        text = { Text(sort.label, style = MaterialTheme.typography.bodyMedium) },
                        onClick = { onSort(sort); dismiss() },
                    )
                }
            }
        }
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            secondaryFilters.forEach { filter ->
                ClayChip(
                    text = filter.label,
                    selected = state.filter == filter,
                    onClick = { onFilter(if (state.filter == filter) QuestFilter.All else filter) },
                    accent = if (filter == QuestFilter.Challenges) ClayPalette.Peach else ClayPalette.Butter,
                    leadingEmoji = if (filter == QuestFilter.Challenges) "🏔️" else "🪜",
                )
            }
        }
    }
}

// ------------------------------------------------------------------ sections

@Composable
private fun QuestSection(
    section: QuestSectionData,
    onOpen: (Long) -> Unit,
    onLog: (QuestWithProgress) -> Unit,
    onEdit: (Long) -> Unit,
    onDuplicate: (Long) -> Unit,
    onToggleActive: (Long) -> Unit,
    onDelete: (QuestWithProgress) -> Unit,
) {
    val accent = section.categoryId?.let { id ->
        section.items.firstOrNull()?.category?.accentId?.let { ClayPalette.accentById(it) }
    } ?: ClayPalette.Lavender
    val completed = section.items.count { it.isComplete }

    ClayCard(
        shape = ClayShape.Card,
        elevation = 12.dp,
        glowAlpha = 0.14f,
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            ClayDot(emoji = section.emoji, accent = accent, size = 34.dp)
            Spacer(Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = section.title,
                    style = MaterialTheme.typography.titleSmall,
                    color = ClayInk.Primary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = "${section.items.size} ${if (section.items.size == 1) "quest" else "quests"}" +
                        if (completed > 0) " · $completed done today" else "",
                    style = MaterialTheme.typography.labelSmall,
                    color = ClayInk.Muted,
                    maxLines = 1,
                )
            }
        }
        Spacer(Modifier.height(6.dp))
        section.items.forEachIndexed { index, item ->
            if (index > 0) ClayDivider(verticalPadding = 0.dp)
            QuestRow(
                item = item,
                onOpen = { onOpen(item.quest.id) },
                onLog = { onLog(item) },
                onEdit = { onEdit(item.quest.id) },
                onDuplicate = { onDuplicate(item.quest.id) },
                onToggleActive = { onToggleActive(item.quest.id) },
                onDelete = { onDelete(item) },
            )
        }
    }
}

@Composable
private fun QuestRow(
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
    val interaction = remember { MutableInteractionSource() }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(ClayShape.Small)
            .clickable(interactionSource = interaction, indication = null, onClick = onOpen)
            .padding(vertical = 12.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            ClayDot(emoji = quest.emoji, accent = accent, size = 38.dp)
            Spacer(Modifier.width(11.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = quest.title,
                    style = MaterialTheme.typography.titleSmall,
                    color = if (quest.isActive) ClayInk.Primary else ClayInk.Muted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = buildString {
                        append(
                            if (item.isLongTerm) "Long-term"
                            else ScheduleKind.fromName(quest.scheduleKind).label
                        )
                        append(" · ${QuestDifficulty.fromName(quest.difficulty).label}")
                        append(" · +${quest.xpReward} XP")
                        if (item.isMultiStep) append(" · ${item.stepsCompleted}/${item.stepsTotal} steps")
                    },
                    style = MaterialTheme.typography.labelSmall,
                    color = ClayInk.Muted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Spacer(Modifier.width(8.dp))

            // Primary row action: log progress (or a tick once the target is met).
            LogAction(complete = item.isComplete, accent = accent, onLog = onLog)

            Spacer(Modifier.width(4.dp))
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
                        text = { Text("Open details", style = MaterialTheme.typography.bodyMedium) },
                        onClick = { menuOpen = false; onOpen() },
                    )
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

        Spacer(Modifier.height(10.dp))
        if (quest.trackingKind != TrackingKind.Milestone.name || item.isComplete) {
            ClayProgressBar(
                progress = item.fraction,
                accent = if (item.isComplete) ClayPalette.Mint else accent,
                height = 9.dp,
            )
            Spacer(Modifier.height(7.dp))
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = progressSummary(item),
                style = MaterialTheme.typography.labelSmall,
                color = ClayInk.Secondary,
                modifier = Modifier.weight(1f),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            if (!quest.isActive) {
                ClayIconPill(text = "Paused", accent = ClayPalette.Sky, emoji = "⏸️")
            } else if (item.isOverdue) {
                ClayIconPill(text = "Overdue", accent = ClayPalette.Peach, emoji = "⏰")
            } else if (item.isComplete) {
                ClayIconPill(text = "Done", accent = ClayPalette.Mint, emoji = "✅", emphasised = true)
            } else if (item.daysLeft != null) {
                ClayIconPill(
                    text = if (item.daysLeft!! <= 0L) "Due today" else "${item.daysLeft}d left",
                    accent = ClayPalette.Butter,
                    emoji = "🗓️",
                )
            }
        }
    }
}

/** Thin wrapper that keeps the log affordance visually consistent with the dashboard. */
@Composable
private fun LogAction(
    complete: Boolean,
    accent: com.irlrpg.app.ui.theme.ClayAccent,
    onLog: () -> Unit,
) {
    val interaction = remember { MutableInteractionSource() }
    Box(
        modifier = Modifier
            .size(40.dp)
            .clip(ClayShape.Pill)
            .background(if (complete) ClayPalette.Mint.soft else accent.soft)
            .clickable(
                enabled = !complete,
                interactionSource = interaction,
                indication = null,
                onClick = onLog,
            ),
        contentAlignment = Alignment.Center,
    ) {
        if (complete) {
            Icon(
                imageVector = Icons.Rounded.Check,
                contentDescription = "Completed",
                tint = ClayPalette.Mint.deep,
                modifier = Modifier.size(18.dp),
            )
        } else {
            Text(text = "＋", color = accent.deep, fontSize = 17.sp)
        }
    }
}

private fun progressSummary(item: QuestWithProgress): String = buildString {
    when {
        item.isMultiStep -> {
            append("${item.stepsCompleted} of ${item.stepsTotal} steps")
            append(" · ${QuestProgress.periodLabel(item.quest)}")
        }
        item.quest.trackingKind == TrackingKind.Milestone.name ->
            append(if (item.isComplete) "Milestone reached" else "One-off milestone")
        else -> {
            append("${item.progress.coerceAtMost(item.target)}/${item.target} ")
            append(item.quest.customUnit ?: "")
            append(" · ${QuestProgress.periodLabel(item.quest)}")
        }
    }
    item.daysLeft?.let { days ->
        append(" · ")
        append(
            when {
                item.isComplete -> "done"
                days < 0 -> "${-days} days overdue"
                days == 0L -> "due today"
                else -> "$days days left"
            }
        )
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
