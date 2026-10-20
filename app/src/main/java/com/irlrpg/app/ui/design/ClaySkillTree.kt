package com.irlrpg.app.ui.design

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.irlrpg.app.domain.game.SkillNodeView
import com.irlrpg.app.domain.model.SkillNodeState
import com.irlrpg.app.ui.theme.ClayInk
import com.irlrpg.app.ui.theme.ClayPalette
import com.irlrpg.app.ui.theme.ClayShape

/**
 * Geometry for the skill tree in *virtual* (unscaled) coordinates.
 * Kept separate from drawing so it stays unit-testable and cheap.
 */
object SkillTreeLayout {
    const val NODE_SIZE = 96f   // dp
    const val COL_SPACING = 138f
    const val ROW_SPACING = 152f
    const val PADDING = 32f

    data class Placement(
        val id: Long,
        val x: Float,
        val y: Float,
        val rootId: Long,
        val depth: Int,
    )

    /**
     * Assigns every node a position.
     *
     * Each root branch gets its own horizontal lane. Within a branch, nodes are
     * stacked vertically with breathing room so labels never overlap. Depth still
     * moves nodes to the right; a subtle zig-zag keeps the graph hand-placed.
     */
    fun place(nodes: List<SkillNodeView>): Map<Long, Placement> {
        if (nodes.isEmpty()) return emptyMap()
        val byId = nodes.associateBy { it.node.id }
        val childrenByParent = nodes.groupBy { it.node.parentId }

        val roots = nodes.filter { it.node.parentId == null }
            .sortedWith(compareBy({ it.node.sortOrder }, { it.node.id }))

        val placements = LinkedHashMap<Long, Placement>()

        // Global vertical slot counter — each leaf row advances the cursor,
        // guaranteeing no two nodes share a y.
        var rowSlot = 0

        fun walk(id: Long, depth: Int, rootId: Long): Int {
            val view = byId[id] ?: return rowSlot
            val children = childrenByParent[id].orEmpty()
                .sortedWith(compareBy({ it.node.sortOrder }, { it.node.id }))
            if (children.isEmpty()) {
                val zig = if (depth % 2 == 0) 0f else 10f
                placements[id] = Placement(
                    id = id,
                    x = PADDING + depth * COL_SPACING,
                    y = PADDING + rowSlot * ROW_SPACING + zig,
                    rootId = rootId,
                    depth = depth,
                )
                rowSlot++
                return rowSlot - 1
            }
            // Place children first so we can centre the parent among them.
            val childRows = mutableListOf<Int>()
            children.forEach { child ->
                childRows += walk(child.node.id, depth + 1, rootId)
            }
            val firstY = PADDING + childRows.first() * ROW_SPACING
            val lastY = PADDING + childRows.last() * ROW_SPACING
            val parentY = (firstY + lastY) / 2f
            placements[id] = Placement(
                id = id,
                x = PADDING + depth * COL_SPACING,
                y = parentY,
                rootId = rootId,
                depth = depth,
            )
            return (parentY / ROW_SPACING).toInt()
        }

        roots.forEach { root -> walk(root.node.id, 0, root.node.id) }

        // Defensive: any orphan whose parent vanished gets its own lane.
        nodes.filter { !placements.containsKey(it.node.id) }.forEach { orphan ->
            placements[orphan.node.id] = Placement(
                id = orphan.node.id,
                x = PADDING,
                y = PADDING + rowSlot * ROW_SPACING,
                rootId = orphan.node.id,
                depth = 0,
            )
            rowSlot++
        }
        return placements
    }

    fun canvasSize(placements: Map<Long, Placement>): Size {
        if (placements.isEmpty()) return Size(0f, 0f)
        val maxX = placements.values.maxOf { it.x } + NODE_SIZE + PADDING
        val maxY = placements.values.maxOf { it.y } + NODE_SIZE + PADDING * 2
        return Size(maxX, maxY)
    }
}

/**
 * Interactive skill tree.
 *
 * A single `Canvas` draws every connection (cheap: one draw pass, no per-node
 * composables for lines) and the nodes are ordinary composables placed with
 * absolute offsets inside one transformed box. Pan/zoom is a single
 * `graphicsLayer` transform, so scrolling a 40-node tree costs the same as
 * scrolling a card list on a low-end device.
 */
@Composable
fun ClaySkillTree(
    nodes: List<SkillNodeView>,
    selectedId: Long?,
    onSelect: (SkillNodeView) -> Unit,
    modifier: Modifier = Modifier,
    scale: Float = 1f,
    offset: Offset = Offset.Zero,
) {
    if (nodes.isEmpty()) return
    val placements = remember(nodes) { SkillTreeLayout.place(nodes) }
    val canvasSize = remember(placements) { SkillTreeLayout.canvasSize(placements) }
    val density = LocalDensity.current

    Box(
        modifier = modifier
            .requiredSize(
                width = with(density) { canvasSize.width.dp },
                height = with(density) { canvasSize.height.dp },
            )
            .graphicsLayerCompat(scale = scale, pan = offset),
    ) {
        // Connections first, so nodes always sit on top.
        Canvas(
            modifier = Modifier.requiredSize(
                width = with(density) { canvasSize.width.dp },
                height = with(density) { canvasSize.height.dp },
            ),
        ) {
            val nodePx = SkillTreeLayout.NODE_SIZE.dp.toPx()
            nodes.forEach { view ->
                val parentId = view.node.parentId ?: return@forEach
                val parentPlacement = placements[parentId] ?: return@forEach
                val childPlacement = placements[view.node.id] ?: return@forEach
                val startX = parentPlacement.x.dp.toPx() + nodePx
                val startY = parentPlacement.y.dp.toPx() + nodePx / 2f
                val endX = childPlacement.x.dp.toPx()
                val endY = childPlacement.y.dp.toPx() + nodePx / 2f
                val accent = ClayPalette.accentById(view.node.accentId)
                val color = when (view.state) {
                    SkillNodeState.Mastered -> accent.base
                    SkillNodeState.InProgress -> accent.base.copy(alpha = 0.65f)
                    SkillNodeState.Available -> accent.base.copy(alpha = 0.38f)
                    SkillNodeState.Locked -> ClayInk.Muted.copy(alpha = 0.42f)
                }
                val path = Path().apply {
                    moveTo(startX, startY)
                    val midX = (startX + endX) / 2f
                    cubicTo(midX, startY, midX, endY, endX, endY)
                }
                drawPath(
                    path = path,
                    color = color,
                    style = Stroke(
                        width = (if (view.state == SkillNodeState.Mastered) 5f else 3.5f).dp.toPx(),
                        cap = StrokeCap.Round,
                        pathEffect = if (view.state == SkillNodeState.Locked) {
                            PathEffect.dashPathEffect(floatArrayOf(12f, 12f))
                        } else {
                            null
                        },
                    ),
                )
            }
        }

        nodes.forEach { view ->
            val placement = placements[view.node.id] ?: return@forEach
            SkillNode(
                view = view,
                selected = view.node.id == selectedId,
                onClick = { onSelect(view) },
                modifier = Modifier.offsetInDp(placement.x, placement.y),
            )
        }
    }
}

@Composable
private fun SkillNode(
    view: SkillNodeView,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val accent = ClayPalette.accentById(view.node.accentId)
    val interaction = remember { MutableInteractionSource() }
    val pressed = interaction.collectIsPressedAsStateCompat()
    val pressScale by animateFloatAsState(
        targetValue = if (pressed) 0.94f else if (selected) 1.06f else 1f,
        animationSpec = tween(180, easing = FastOutSlowInEasing),
        label = "nodeScale",
    )
    val container by animateColorAsState(
        targetValue = when (view.state) {
            SkillNodeState.Mastered -> accent.base
            SkillNodeState.InProgress -> accent.soft
            SkillNodeState.Available -> ClayPalette.SurfacePure
            SkillNodeState.Locked -> Color(0xFFEDE8F5)
        },
        animationSpec = tween(260, easing = FastOutSlowInEasing),
        label = "nodeContainer",
    )

    Column(
        modifier = modifier
            .size(width = SkillTreeLayout.NODE_SIZE.dp, height = SkillTreeLayout.NODE_SIZE.dp + 40.dp)
            .scale(pressScale)
            .clickable(
                interactionSource = interaction,
                indication = null,
                onClick = onClick,
            ),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(contentAlignment = Alignment.Center) {
            // Progress ring for in-progress / mastered nodes.
            if (view.state == SkillNodeState.InProgress || view.state == SkillNodeState.Mastered) {
                Canvas(modifier = Modifier.size(SkillTreeLayout.NODE_SIZE.dp + 12.dp)) {
                    val stroke = 4.dp.toPx()
                    drawArc(
                        color = ClayPalette.SurfaceMuted,
                        startAngle = -90f,
                        sweepAngle = 360f,
                        useCenter = false,
                        topLeft = Offset(stroke / 2, stroke / 2),
                        size = Size(size.width - stroke, size.height - stroke),
                        style = Stroke(stroke),
                    )
                    drawArc(
                        color = accent.deep,
                        startAngle = -90f,
                        sweepAngle = 360f * view.fraction,
                        useCenter = false,
                        topLeft = Offset(stroke / 2, stroke / 2),
                        size = Size(size.width - stroke, size.height - stroke),
                        style = Stroke(stroke, cap = StrokeCap.Round),
                    )
                }
            }
            Box(
                modifier = Modifier
                    .size(SkillTreeLayout.NODE_SIZE.dp)
                    .clay(
                        shape = CircleShape,
                        elevation = if (view.state == SkillNodeState.Locked) 4.dp else 12.dp,
                        glowAlpha = if (view.state == SkillNodeState.Locked) 0.06f else 0.22f,
                    )
                    .clip(CircleShape)
                    .background(
                        if (view.state == SkillNodeState.Mastered) {
                            Brush.linearGradient(listOf(accent.base, accent.deep))
                        } else {
                            Brush.linearGradient(listOf(container, container.copy(alpha = 0.92f)))
                        }
                    )
                    .border(
                        width = if (selected) 3.dp else 1.5.dp,
                        color = when {
                            selected -> accent.deep
                            view.state == SkillNodeState.Available -> accent.base.copy(alpha = 0.55f)
                            view.state == SkillNodeState.Locked -> ClayInk.Muted.copy(alpha = 0.35f)
                            else -> Color.White.copy(alpha = 0.55f)
                        },
                        shape = CircleShape,
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = if (view.state == SkillNodeState.Locked) "🔒" else view.node.emoji,
                    fontSize = 30.sp,
                )
            }
            if (view.state == SkillNodeState.Mastered) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .size(26.dp)
                        .clip(CircleShape)
                        .background(ClayPalette.SurfacePure)
                        .border(1.5.dp, accent.base.copy(alpha = 0.5f), CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(text = "🌟", fontSize = 13.sp)
                }
            }
        }
        Spacer(Modifier.height(6.dp))
        Text(
            text = view.node.name,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = if (view.state == SkillNodeState.Mastered) FontWeight.Bold else FontWeight.Medium,
            color = when (view.state) {
                SkillNodeState.Mastered -> accent.deep
                SkillNodeState.Locked -> ClayInk.Muted
                else -> ClayInk.Primary
            },
            textAlign = TextAlign.Center,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.width(SkillTreeLayout.NODE_SIZE.dp + 20.dp),
        )
    }
}

/** Legend explaining the four derived states. */
@Composable
fun SkillTreeLegend(
    locked: Int,
    available: Int,
    inProgress: Int,
    mastered: Int,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = "Node states",
            style = MaterialTheme.typography.labelMedium,
            color = ClayInk.Secondary,
        )
        Spacer(Modifier.height(8.dp))
        LegendRow("🔒", "Locked", locked, "prerequisites not met yet")
        LegendRow("⚪", "Available", available, "ready for its own XP")
        LegendRow("🟣", "In progress", inProgress, "earning mastery")
        LegendRow("🌟", "Mastered", mastered, "requirement reached")
    }
}

@Composable
private fun LegendRow(emoji: String, title: String, count: Int, hint: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(text = emoji, fontSize = 14.sp)
        Spacer(Modifier.width(8.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            color = ClayInk.Primary,
        )
        Spacer(Modifier.width(6.dp))
        Text(
            text = hint,
            style = MaterialTheme.typography.labelSmall,
            color = ClayInk.Faint,
            modifier = Modifier.weight(1f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            text = "$count",
            style = MaterialTheme.typography.labelMedium,
            color = ClayInk.Secondary,
        )
    }
    Spacer(Modifier.height(6.dp))
}

// --------------------------------------------------------------------- helpers

private fun Modifier.offsetInDp(x: Float, y: Float): Modifier =
    this.then(offset(x = x.dp, y = y.dp))

private fun Modifier.graphicsLayerCompat(scale: Float, pan: Offset): Modifier =
    this.then(
        graphicsLayer(
            scaleX = scale,
            scaleY = scale,
            translationX = pan.x,
            translationY = pan.y,
            clip = false,
        )
    )

@Composable
private fun MutableInteractionSource.collectIsPressedAsStateCompat(): Boolean =
    collectIsPressedAsState().value
