package com.irlrpg.app.ui.design

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DropdownMenu
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.irlrpg.app.ui.theme.ClayAccent
import com.irlrpg.app.ui.theme.ClayInk
import com.irlrpg.app.ui.theme.ClayPalette
import com.irlrpg.app.ui.theme.ClayShape

/**
 * Second-tier clay surfaces.
 *
 * The dashboard and detail screens used to nest full raised cards inside other
 * raised cards, which flattens the visual hierarchy and makes everything look
 * equally important. These components exist to fix that:
 *
 *  - [ClayPanel]     — a *flat* tinted well for nested or secondary content.
 *  - [ClayMiniStat]  — a quiet number + label that needs no container at all.
 *  - [ClayIconPill]  — a compact metadata chip (deadline, difficulty, XP).
 *  - [ClayDivider]   — a hairline used instead of extra spacing.
 *
 * Only one raised surface should exist per region; everything inside it uses
 * these flatter primitives. Nothing here adds elevation, so lists stay cheap.
 */

/** Flat, subtly tinted well — the correct surface for content inside a card. */
@Composable
fun ClayPanel(
    modifier: Modifier = Modifier,
    shape: Shape = ClayShape.Medium,
    tint: Color = ClayPalette.SurfaceMuted,
    contentPadding: PaddingValues = PaddingValues(horizontal = 14.dp, vertical = 12.dp),
    onClick: (() -> Unit)? = null,
    content: @Composable androidx.compose.foundation.layout.ColumnScope.() -> Unit,
) {
    val interaction = remember { MutableInteractionSource() }
    Column(
        modifier = modifier
            .clip(shape)
            .background(tint)
            .border(1.dp, Color.White.copy(alpha = 0.55f), shape)
            .then(
                if (onClick != null) {
                    Modifier.clickable(
                        interactionSource = interaction,
                        indication = null,
                        onClick = onClick,
                    )
                } else Modifier
            )
            .padding(contentPadding),
        content = content,
    )
}

/**
 * Quiet statistic: value on top, label underneath, no container. Used in rows
 * of three or four where separate cards would add noise without adding meaning.
 */
@Composable
fun ClayMiniStat(
    value: String,
    label: String,
    modifier: Modifier = Modifier,
    emoji: String? = null,
    accent: ClayAccent = ClayPalette.Lavender,
    valueSize: TextUnit = 20.sp,
    align: TextAlign = TextAlign.Start,
) {
    Column(
        modifier = modifier,
        horizontalAlignment = if (align == TextAlign.Center) Alignment.CenterHorizontally else Alignment.Start,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (emoji != null) {
                Text(text = emoji, fontSize = 13.sp)
                Spacer(Modifier.width(5.dp))
            }
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium,
                fontSize = valueSize,
                fontWeight = FontWeight.Bold,
                color = accent.deep,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = ClayInk.Muted,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            textAlign = align,
        )
    }
}

/** Compact metadata chip: emoji + short label on a flat pastel surface. */
@Composable
fun ClayIconPill(
    text: String,
    accent: ClayAccent,
    modifier: Modifier = Modifier,
    emoji: String? = null,
    emphasised: Boolean = false,
) {
    Row(
        modifier = modifier
            .clip(ClayShape.Pill)
            .background(if (emphasised) accent.soft else ClayPalette.SurfaceMuted.copy(alpha = 0.75f))
            .padding(horizontal = 10.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (emoji != null) {
            Text(text = emoji, fontSize = 11.sp)
            Spacer(Modifier.width(4.dp))
        }
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = if (emphasised) FontWeight.SemiBold else FontWeight.Medium,
            color = if (emphasised) accent.deep else ClayInk.Secondary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

/**
 * Chip that opens a dropdown. Two of these replace a row of horizontally
 * scrolling filter chips, which keeps headers compact on small screens.
 */
@Composable
fun ClayDropdownChip(
    text: String,
    accent: ClayAccent,
    modifier: Modifier = Modifier,
    emoji: String? = null,
    active: Boolean = false,
    content: @Composable androidx.compose.foundation.layout.ColumnScope.(dismiss: () -> Unit) -> Unit,
) {
    var open by remember { mutableStateOf(false) }
    Box(modifier = modifier) {
        ClayChip(
            text = text,
            selected = active,
            onClick = { open = true },
            accent = accent,
            leadingEmoji = emoji,
        )
        DropdownMenu(
            expanded = open,
            onDismissRequest = { open = false },
            containerColor = ClayPalette.SurfaceCream,
            shape = ClayShape.Medium,
        ) {
            content { open = false }
        }
    }
}

/** Hairline separator that keeps the clay warmth instead of a hard rule. */
@Composable
fun ClayDivider(
    modifier: Modifier = Modifier,
    color: Color = ClayInk.Faint.copy(alpha = 0.22f),
    verticalPadding: Dp = 10.dp,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = verticalPadding)
            .height(1.dp)
            .background(color),
    )
}

/**
 * Section eyebrow + title used to break a screen into clear chapters without a
 * card per chapter.
 */
@Composable
fun ClaySectionTitle(
    title: String,
    modifier: Modifier = Modifier,
    eyebrow: String? = null,
    caption: String? = null,
    action: (@Composable () -> Unit)? = null,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Bottom,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            if (eyebrow != null) {
                Text(
                    text = eyebrow.uppercase(),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = ClayInk.Faint,
                    letterSpacing = 1.2.sp,
                    maxLines = 1,
                )
            }
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = ClayInk.Primary,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            if (caption != null) {
                Text(
                    text = caption,
                    style = MaterialTheme.typography.bodySmall,
                    color = ClayInk.Muted,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        if (action != null) {
            Spacer(Modifier.width(12.dp))
            action()
        }
    }
}

/** Row of up to four quiet statistics with hairlines between them. */
@Composable
fun ClayStatRow(
    modifier: Modifier = Modifier,
    content: @Composable androidx.compose.foundation.layout.RowScope.() -> Unit,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        content = content,
    )
}

/** Small circular number/emoji badge used in timelines and step lists. */
@Composable
fun ClayDot(
    modifier: Modifier = Modifier,
    size: Dp = 34.dp,
    accent: ClayAccent = ClayPalette.Lavender,
    emoji: String? = null,
    text: String? = null,
) {
    Box(
        modifier = modifier
            .size(size)
            .clip(ClayShape.Pill)
            .background(accent.soft)
            .border(1.dp, Color.White.copy(alpha = 0.6f), ClayShape.Pill),
        contentAlignment = Alignment.Center,
    ) {
        when {
            emoji != null -> Text(text = emoji, fontSize = (size.value / 2.2f).sp)
            text != null -> Text(
                text = text,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = accent.deep,
                maxLines = 1,
            )
        }
    }
}

/** Rounded thumb used for progress previews inside flat panels. */
@Composable
fun ClayTrack(
    progress: Float,
    accent: ClayAccent,
    modifier: Modifier = Modifier,
    height: Dp = 8.dp,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .clip(RoundedCornerShape(percent = 50))
            .background(ClayPalette.SurfaceMuted),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxWidth(progress.coerceIn(0f, 1f))
                .height(height)
                .clip(RoundedCornerShape(percent = 50))
                .background(accent.base.copy(alpha = 0.85f)),
        )
    }
}
