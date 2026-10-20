package com.irlrpg.app.ui.skills

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.irlrpg.app.data.repository.NodeQuestLink
import com.irlrpg.app.domain.game.SkillNodeView
import com.irlrpg.app.domain.model.SkillNodeState
import com.irlrpg.app.domain.model.SkillUnlockRule
import com.irlrpg.app.ui.components.ClayConfirmDialog
import com.irlrpg.app.ui.components.ClayToast
import com.irlrpg.app.ui.design.ClayButton
import com.irlrpg.app.ui.design.ClayButtonStyle
import com.irlrpg.app.ui.design.ClayCard
import com.irlrpg.app.ui.design.ClayChip
import com.irlrpg.app.ui.design.ClayDivider
import com.irlrpg.app.ui.design.ClayIconButton
import com.irlrpg.app.ui.design.ClayIconTile
import com.irlrpg.app.ui.design.ClayProgressBar
import com.irlrpg.app.ui.design.ClayScaffold
import com.irlrpg.app.ui.design.ClaySectionHeader
import com.irlrpg.app.ui.design.ClaySkillTree
import com.irlrpg.app.ui.design.ClayStepper
import com.irlrpg.app.ui.design.ClayTextField
import com.irlrpg.app.ui.design.ClayTopBar
import com.irlrpg.app.ui.design.SkillTreeLegend
import com.irlrpg.app.ui.quests.EmojiRow
import com.irlrpg.app.ui.theme.ClayInk
import com.irlrpg.app.ui.theme.ClayPalette
import com.irlrpg.app.ui.theme.ClayShape

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SkillTreeScreen(
    viewModel: SkillTreeViewModel,
    onBack: () -> Unit,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val selected by viewModel.selected.collectAsStateWithLifecycle()
    val editor by viewModel.editor.collectAsStateWithLifecycle()
    val attributes by viewModel.attributes.collectAsStateWithLifecycle()
    val message by viewModel.message.collectAsStateWithLifecycle()

    var scale by remember { mutableFloatStateOf(0.85f) }
    var offset by remember { mutableStateOf(Offset(24f, 12f)) }
    var confirmDeleteId by remember { mutableStateOf<Long?>(null) }
    // Fallback list index used when gestures are unavailable (accessibility / mouse)
    var fallbackIndex by remember { mutableStateOf<Int?>(null) }

    fun resetView() {
        scale = 0.85f
        offset = Offset(24f, 12f)
    }

    ClayScaffold(
        topBar = {
            ClayTopBar(
                title = "Skill tree",
                subtitle = "${state.mastered} mastered · ${state.inProgress} in progress · ${state.locked} locked",
                onBack = onBack,
                accent = ClayPalette.Mint,
            )
        },
    ) { padding ->
        // One scroll container so the graph + summary + legend move together
        // and the graph never fights the page scroll.
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = 16.dp,
                end = 16.dp,
                top = padding.calculateTopPadding() + 4.dp,
                bottom = padding.calculateBottomPadding() + 24.dp,
            ),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            // ── Graph viewport ──────────────────────────────────────────────
            item {
                SkillTreeViewport(
                    nodes = state.nodes,
                    selectedId = selected?.node?.id,
                    onSelect = { viewModel.select(it) },
                    scale = scale,
                    offset = offset,
                    onScaleChange = { scale = it },
                    onOffsetChange = { offset = it },
                    onResetView = { resetView() },
                    onAddRoot = { viewModel.openCreate(null) },
                    fallbackIndex = fallbackIndex,
                    onFallbackSelect = { index ->
                        fallbackIndex = index
                        state.nodes.getOrNull(index)?.let { viewModel.select(it) }
                    },
                )
            }

            // ── Summary + legend (inside their own card, not floating) ─────
            item {
                ClayCard(
                    shape = ClayShape.Card,
                    elevation = 10.dp,
                    glowAlpha = 0.14f,
                    contentPadding = PaddingValues(16.dp),
                ) {
                    ClaySectionHeader(
                        title = "Tree mastery",
                        subtitle = "${state.mastered} of ${state.total} nodes mastered · ${state.totalTreeXp} XP mapped to skills",
                    )
                    Spacer(Modifier.height(12.dp))
                    ClayProgressBar(progress = state.mastery, accent = ClayPalette.Mint, height = 10.dp)
                    Spacer(Modifier.height(14.dp))
                    SkillTreeLegend(
                        locked = state.locked,
                        available = state.available,
                        inProgress = state.inProgress,
                        mastered = state.mastered,
                    )
                    Spacer(Modifier.height(10.dp))
                    Text(
                        text = "Drag to pan · pinch to zoom · tap a node for details and unlock conditions",
                        style = MaterialTheme.typography.labelSmall,
                        color = ClayInk.Faint,
                    )
                    // Accessibility: non-gesture node picker
                    if (state.nodes.isNotEmpty()) {
                        Spacer(Modifier.height(12.dp))
                        ClayDivider(verticalPadding = 6.dp)
                        Text(
                            text = "Or pick a skill to inspect",
                            style = MaterialTheme.typography.labelSmall,
                            color = ClayInk.Muted,
                        )
                        Spacer(Modifier.height(8.dp))
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(state.nodes, key = { it.node.id }) { node ->
                                val isSelected = selected?.node?.id == node.node.id
                                ClayChip(
                                    text = node.node.name,
                                    selected = isSelected,
                                    onClick = { viewModel.select(node) },
                                    accent = ClayPalette.accentById(node.node.accentId),
                                    leadingEmoji = node.node.emoji,
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    selected?.let { view ->
        SkillNodeSheet(
            view = view,
            quests = state.questsByAttribute[view.node.attributeId].orEmpty(),
            onDismiss = { viewModel.select(null) },
            onEdit = {
                viewModel.openEdit(view)
                viewModel.select(null)
            },
            onAddChild = {
                viewModel.openCreate(view)
                viewModel.select(null)
            },
            onDelete = { confirmDeleteId = view.node.id },
        )
    }

    editor?.let { editState ->
        NodeEditorSheet(
            state = editState,
            nodes = state.nodes,
            attributes = attributes,
            onDismiss = viewModel::dismissEditor,
            onChange = viewModel::updateEditor,
            onSave = viewModel::saveEditor,
        )
    }

    confirmDeleteId?.let { id ->
        ClayConfirmDialog(
            title = "Remove this skill node?",
            message = "Its child nodes are kept and become branch roots, so no skill or XP is lost.",
            confirmLabel = "Remove node",
            onConfirm = {
                viewModel.deleteNode(id)
                confirmDeleteId = null
            },
            onDismiss = { confirmDeleteId = null },
        )
    }

    ClayToast(
        message = message,
        onShown = viewModel::consumeMessage,
        bottomPadding = 32.dp,
    )
}

/**
 * The graph's own clearly defined viewport.
 *
 * - The card *is* the viewport: its bounds + inset are the drawable area so
 *   nodes can never be clipped at screen edges.
 * - Controls live in a dedicated toolbar row *below* the canvas, never as a
 *   floating overlay on top of nodes.
 * - On small screens the viewport height shrinks but stays usable.
 */
@Composable
private fun SkillTreeViewport(
    nodes: List<SkillNodeView>,
    selectedId: Long?,
    onSelect: (SkillNodeView) -> Unit,
    scale: Float,
    offset: Offset,
    onScaleChange: (Float) -> Unit,
    onOffsetChange: (Offset) -> Unit,
    onResetView: () -> Unit,
    onAddRoot: () -> Unit,
    fallbackIndex: Int?,
    onFallbackSelect: (Int) -> Unit,
) {
    val config = LocalConfiguration.current
    val isSmallScreen = config.screenWidthDp < 360
    // Minimum height so the graph has room even on small phones; the card grows
    // if the computed canvas is taller.
    val viewportHeight = if (isSmallScreen) 280.dp else 380.dp

    ClayCard(
        shape = ClayShape.Card,
        elevation = 12.dp,
        glowAlpha = 0.16f,
        contentPadding = PaddingValues(0.dp),
    ) {
        // ── Canvas area: inset from the card edge so nothing touches the border
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .height(viewportHeight)
                .clip(ClayShape.Card)
                .background(ClayPalette.SurfaceCream.copy(alpha = 0.72f))
                .clipToBounds()
                .padding(8.dp)
                .pointerInput(nodes) {
                    detectTransformGestures { _, pan, zoom, _ ->
                        onScaleChange((scale * zoom).coerceIn(0.5f, 1.6f))
                        onOffsetChange(
                            Offset(
                                x = (offset.x + pan.x).coerceIn(-2200f, 1200f),
                                y = (offset.y + pan.y).coerceIn(-2200f, 1200f),
                            )
                        )
                    }
                },
        ) {
            if (nodes.isEmpty()) {
                // Empty state lives *inside* the viewport so the toolbar still shows
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        ClayIconTile(emoji = "🌳", accent = ClayPalette.Mint, size = 56.dp, fontSize = 26.sp)
                        Spacer(Modifier.height(10.dp))
                        Text(
                            text = "Your tree is empty",
                            style = MaterialTheme.typography.titleSmall,
                            color = ClayInk.Primary,
                        )
                        Text(
                            text = "Add a root skill to start a branch",
                            style = MaterialTheme.typography.bodySmall,
                            color = ClayInk.Muted,
                        )
                    }
                }
            } else {
                Box(
                    modifier = Modifier
                        .clipToBounds()
                        .fillMaxSize(),
                ) {
                    ClaySkillTree(
                        nodes = nodes,
                        selectedId = selectedId,
                        onSelect = onSelect,
                        scale = scale,
                        offset = offset,
                    )
                }
            }
        }

        // ── Dedicated toolbar: never covers nodes
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (nodes.isNotEmpty()) {
                ClayButton(
                    text = "Reset view",
                    onClick = onResetView,
                    accent = ClayPalette.Sky,
                    style = ClayButtonStyle.Soft,
                    height = 38.dp,
                    leadingEmoji = "↺",
                )
                // Zoom controls alternative to pinch
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    ClayIconButton(
                        icon = Icons.Rounded.Add,
                        onClick = { onScaleChange((scale * 1.18f).coerceIn(0.5f, 1.6f)) },
                        contentDescription = "Zoom in",
                        accent = ClayPalette.Sky,
                        size = 38.dp,
                    )
                    // Zoom-out = smaller step so users don't overshoot
                    ClayIconButton(
                        icon = Icons.Rounded.Refresh,
                        onClick = { onScaleChange((scale * 0.85f).coerceIn(0.5f, 1.6f)) },
                        contentDescription = "Zoom out",
                        accent = ClayPalette.Sky,
                        size = 38.dp,
                    )
                }
            }
            Spacer(Modifier.weight(1f))
            ClayButton(
                text = if (nodes.isEmpty()) "Add root skill" else "Add skill",
                onClick = onAddRoot,
                accent = ClayPalette.Mint,
                height = 38.dp,
                leadingEmoji = "＋",
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SkillNodeSheet(
    view: SkillNodeView,
    quests: List<NodeQuestLink>,
    onDismiss: () -> Unit,
    onEdit: () -> Unit,
    onAddChild: () -> Unit,
    onDelete: () -> Unit,
) {
    val accent = ClayPalette.accentById(view.node.accentId)
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = ClayPalette.SurfaceCream,
        shape = ClayShape.Hero,
        dragHandle = null,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 640.dp)
                .verticalScroll(rememberScrollState())
                .navigationBarsPadding()
                .padding(horizontal = 20.dp, vertical = 22.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                ClayIconTile(emoji = view.node.emoji, accent = accent, size = 54.dp, fontSize = 24.sp)
                Spacer(Modifier.width(14.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = view.node.name,
                        style = MaterialTheme.typography.titleMedium,
                        color = ClayInk.Primary,
                    )
                    Text(
                        text = "${view.state.label} · ${view.attributeName ?: "no attribute linked"}",
                        style = MaterialTheme.typography.labelSmall,
                        color = accent.deep,
                    )
                }
                StateBadge(view.state)
            }

            if (view.node.description != null) {
                Spacer(Modifier.height(12.dp))
                Text(
                    text = view.node.description.orEmpty(),
                    style = MaterialTheme.typography.bodyMedium,
                    color = ClayInk.Secondary,
                )
            }

            Spacer(Modifier.height(18.dp))
            ClaySectionHeader(
                title = "Mastery",
                subtitle = if (view.state == SkillNodeState.Locked) {
                    "Progress starts once this node unlocks"
                } else {
                    "${view.xp} of ${view.masteryXp} XP"
                },
            )
            Spacer(Modifier.height(10.dp))
            ClayProgressBar(progress = view.fraction, accent = accent, height = 12.dp)
            Spacer(Modifier.height(8.dp))
            Text(
                text = if (view.state == SkillNodeState.Locked) {
                    "0 of ${view.masteryXp} XP"
                } else {
                    "${(view.masteryXp - view.xp).coerceAtLeast(0)} XP left to master this skill"
                },
                style = MaterialTheme.typography.labelSmall,
                color = ClayInk.Muted,
            )

            Spacer(Modifier.height(18.dp))
            ClayCard(shape = ClayShape.Medium, elevation = 8.dp, glowAlpha = 0.12f, contentPadding = PaddingValues(16.dp)) {
                Text(
                    text = if (view.state == SkillNodeState.Locked) "Why is this locked?" else "Unlock requirement",
                    style = MaterialTheme.typography.titleSmall,
                    color = ClayInk.Primary,
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    text = view.lockReason ?: view.requirementText,
                    style = MaterialTheme.typography.bodySmall,
                    color = if (view.state == SkillNodeState.Locked) ClayPalette.Peach.deep else ClayInk.Secondary,
                )
                if (view.state == SkillNodeState.Locked) {
                    Spacer(Modifier.height(10.dp))
                    ClayProgressBar(progress = view.requirementProgress, accent = ClayPalette.Peach, height = 10.dp)
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "${(view.requirementProgress * 100).toInt()}% towards unlock",
                        style = MaterialTheme.typography.labelSmall,
                        color = ClayInk.Muted,
                    )
                } else {
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = when (view.state) {
                            SkillNodeState.Available -> "Ready — any XP in ${view.attributeName ?: "its attribute"} now counts"
                            SkillNodeState.InProgress -> "${(view.fraction * 100).toInt()}% mastered"
                            SkillNodeState.Mastered -> "Mastered at ${view.masteryXp} XP"
                            else -> ""
                        },
                        style = MaterialTheme.typography.labelSmall,
                        color = ClayInk.Muted,
                    )
                }
            }

            // Current progress detail
            Spacer(Modifier.height(14.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                // XP pillar
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = "Current XP", style = MaterialTheme.typography.labelSmall, color = ClayInk.Muted)
                    Text(text = "${view.xp}", style = MaterialTheme.typography.titleMedium, color = accent.deep)
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = "To master", style = MaterialTheme.typography.labelSmall, color = ClayInk.Muted)
                    Text(text = "${view.masteryXp}", style = MaterialTheme.typography.titleMedium, color = ClayInk.Primary)
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = "Linked skill", style = MaterialTheme.typography.labelSmall, color = ClayInk.Muted)
                    Text(
                        text = view.attributeName ?: "—",
                        style = MaterialTheme.typography.labelMedium,
                        color = ClayInk.Primary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }

            if (quests.isNotEmpty()) {
                Spacer(Modifier.height(18.dp))
                ClaySectionHeader(
                    title = "Quests that feed this skill",
                    subtitle = "XP from these quests builds this node",
                )
                Spacer(Modifier.height(10.dp))
                quests.take(6).forEach { quest ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(text = quest.emoji, fontSize = 15.sp)
                        Spacer(Modifier.width(10.dp))
                        Text(
                            text = quest.title,
                            style = MaterialTheme.typography.bodyMedium,
                            color = ClayInk.Primary,
                            modifier = Modifier.weight(1f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        if (!quest.isActive) {
                            Text(
                                text = "paused",
                                style = MaterialTheme.typography.labelSmall,
                                color = ClayInk.Faint,
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(20.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                ClayButton(
                    text = "Add child",
                    onClick = onAddChild,
                    accent = accent,
                    style = ClayButtonStyle.Soft,
                    modifier = Modifier.weight(1f),
                )
                ClayButton(
                    text = "Edit",
                    onClick = onEdit,
                    accent = accent,
                    style = ClayButtonStyle.Soft,
                    modifier = Modifier.weight(1f),
                )
                ClayButton(
                    text = "Remove",
                    onClick = onDelete,
                    accent = ClayPalette.Blush,
                    style = ClayButtonStyle.Soft,
                    modifier = Modifier.weight(1.1f),
                )
            }
        }
    }
}

@Composable
private fun StateBadge(state: SkillNodeState) {
    val accent = when (state) {
        SkillNodeState.Mastered -> ClayPalette.Butter
        SkillNodeState.InProgress -> ClayPalette.Lavender
        SkillNodeState.Available -> ClayPalette.Mint
        SkillNodeState.Locked -> ClayPalette.Peach
    }
    Box(
        modifier = Modifier
            .clip(ClayShape.Pill)
            .background(accent.soft)
            .padding(horizontal = 12.dp, vertical = 6.dp),
    ) {
        Text(
            text = state.label,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold,
            color = accent.deep,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun NodeEditorSheet(
    state: NodeEditorState,
    nodes: List<SkillNodeView>,
    attributes: List<com.irlrpg.app.data.local.entity.AttributeEntity>,
    onDismiss: () -> Unit,
    onChange: ((NodeEditorState) -> NodeEditorState) -> Unit,
    onSave: () -> Unit,
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = ClayPalette.SurfaceCream,
        shape = ClayShape.Hero,
        dragHandle = null,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 660.dp)
                .verticalScroll(rememberScrollState())
                .navigationBarsPadding()
                .padding(horizontal = 20.dp, vertical = 22.dp),
        ) {
            ClaySectionHeader(
                title = if (state.editing) "Edit skill node" else "New skill node",
                subtitle = "Nodes level up from the XP your quests earn in the linked attribute",
            )
            Spacer(Modifier.height(14.dp))

            ClayTextField(
                value = state.name,
                onValueChange = { value -> onChange { it.copy(name = value, error = null) } },
                label = "Skill name",
                placeholder = "e.g. Music theory, Chess tactics, Public speaking",
            )
            Spacer(Modifier.height(12.dp))
            Text(
                text = "Icon",
                style = MaterialTheme.typography.labelMedium,
                color = ClayInk.Secondary,
                modifier = Modifier.padding(start = 4.dp, bottom = 8.dp),
            )
            EmojiRow(selected = state.emoji, onSelect = { emoji -> onChange { it.copy(emoji = emoji) } })
            Spacer(Modifier.height(12.dp))
            ClayTextField(
                value = state.description,
                onValueChange = { value -> onChange { it.copy(description = value) } },
                label = "Description (optional)",
                placeholder = "What mastering this skill means to you",
                singleLine = false,
                maxLines = 3,
            )

            Spacer(Modifier.height(18.dp))
            Text(
                text = "Parent skill",
                style = MaterialTheme.typography.labelMedium,
                color = ClayInk.Secondary,
                modifier = Modifier.padding(start = 4.dp, bottom = 8.dp),
            )
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                item {
                    ClayChip(
                        text = "None (branch root)",
                        selected = state.parentId == null,
                        onClick = { onChange { it.copy(parentId = null) } },
                        accent = ClayPalette.Mint,
                    )
                }
                items(nodes.filter { it.node.id != state.nodeId }, key = { it.node.id }) { candidate ->
                    ClayChip(
                        text = candidate.node.name,
                        selected = state.parentId == candidate.node.id,
                        onClick = { onChange { it.copy(parentId = candidate.node.id) } },
                        accent = ClayPalette.accentById(candidate.node.accentId),
                        leadingEmoji = candidate.node.emoji,
                    )
                }
            }

            Spacer(Modifier.height(18.dp))
            Text(
                text = "Linked attribute (source of XP)",
                style = MaterialTheme.typography.labelMedium,
                color = ClayInk.Secondary,
                modifier = Modifier.padding(start = 4.dp, bottom = 8.dp),
            )
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(attributes, key = { it.id }) { attribute ->
                    ClayChip(
                        text = attribute.name,
                        selected = state.attributeId == attribute.id,
                        onClick = { onChange { it.copy(attributeId = attribute.id) } },
                        accent = ClayPalette.accentById(attribute.accentId),
                        leadingEmoji = attribute.emoji,
                    )
                }
            }

            Spacer(Modifier.height(18.dp))
            Text(
                text = "Unlock condition",
                style = MaterialTheme.typography.labelMedium,
                color = ClayInk.Secondary,
                modifier = Modifier.padding(start = 4.dp, bottom = 8.dp),
            )
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(SkillUnlockRule.entries.toList()) { rule ->
                    ClayChip(
                        text = rule.label,
                        selected = state.unlockRule == rule,
                        onClick = { onChange { it.copy(unlockRule = rule) } },
                        accent = ClayPalette.Peach,
                    )
                }
            }
            Spacer(Modifier.height(8.dp))
            Text(
                text = state.unlockRule.helper,
                style = MaterialTheme.typography.labelSmall,
                color = ClayInk.Muted,
            )

            when (state.unlockRule) {
                SkillUnlockRule.ParentMastery -> {
                    Spacer(Modifier.height(14.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        ClayStepper(
                            value = state.unlockParentMastery,
                            onValueChange = { value -> onChange { it.copy(unlockParentMastery = value) } },
                            range = 0..5_000,
                            step = 40,
                            accent = ClayPalette.Peach,
                            suffix = "parent XP",
                        )
                    }
                }
                SkillUnlockRule.TotalXp -> {
                    Spacer(Modifier.height(14.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        ClayStepper(
                            value = state.requiredAnyXp,
                            onValueChange = { value -> onChange { it.copy(requiredAnyXp = value) } },
                            range = 0..100_000,
                            step = 100,
                            accent = ClayPalette.Peach,
                            suffix = "lifetime XP",
                        )
                    }
                }
                else -> Unit
            }

            Spacer(Modifier.height(18.dp))
            ClaySectionHeader(
                title = "Mastery requirement",
                subtitle = "XP needed in the linked attribute to master this node",
            )
            Spacer(Modifier.height(10.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                ClayStepper(
                    value = state.masteryXp,
                    onValueChange = { value -> onChange { it.copy(masteryXp = value) } },
                    range = 30..20_000,
                    step = 20,
                    accent = ClayPalette.accentById(state.accentId),
                    suffix = "XP",
                )
            }

            Spacer(Modifier.height(16.dp))
            Text(
                text = "Colour",
                style = MaterialTheme.typography.labelMedium,
                color = ClayInk.Secondary,
                modifier = Modifier.padding(start = 4.dp, bottom = 8.dp),
            )
            LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                items(ClayPalette.accentRotation.map { ClayPalette.accentIdOf(it) }) { id ->
                    ClayChip(
                        text = id.replaceFirstChar { it.uppercase() },
                        selected = state.accentId == id,
                        onClick = { onChange { it.copy(accentId = id) } },
                        accent = ClayPalette.accentById(id),
                    )
                }
            }

            if (state.error != null) {
                Spacer(Modifier.height(14.dp))
                Text(
                    text = state.error.orEmpty(),
                    style = MaterialTheme.typography.labelMedium,
                    color = ClayPalette.Blush.deep,
                )
            }

            Spacer(Modifier.height(20.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                ClayButton(
                    text = "Cancel",
                    onClick = onDismiss,
                    accent = ClayPalette.Lavender,
                    style = ClayButtonStyle.Soft,
                    modifier = Modifier.weight(1f),
                )
                ClayButton(
                    text = if (state.editing) "Save node" else "Add node",
                    onClick = onSave,
                    accent = ClayPalette.accentById(state.accentId),
                    modifier = Modifier.weight(1.3f),
                )
            }
        }
    }
}
