package com.irlrpg.app.ui.design

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
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
import com.irlrpg.app.ui.theme.clayButtonBrush

/** Tactile press feedback shared by every interactive clay surface. */
@Composable
fun rememberClayPress(interaction: MutableInteractionSource): Modifier {
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.965f else 1f,
        animationSpec = spring(dampingRatio = 0.55f, stiffness = 900f),
        label = "clayPress",
    )
    return Modifier.scale(scale)
}

/**
 * The base clay surface: warm cream slab, tinted elevation shadow, soft top
 * highlight and an optional tap action with squishy press feedback.
 */
@Composable
fun ClayCard(
    modifier: Modifier = Modifier,
    shape: Shape = ClayShape.Card,
    color: Color = ClayPalette.SurfaceCream,
    elevation: Dp = 14.dp,
    glowAlpha: Float = 0.22f,
    contentPadding: PaddingValues = PaddingValues(18.dp),
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val interaction = remember { MutableInteractionSource() }
    val pressModifier = if (onClick != null) rememberClayPress(interaction) else Modifier
    Column(
        modifier = modifier
            .then(pressModifier)
            .clay(shape = shape, elevation = elevation, glowAlpha = glowAlpha)
            .clip(shape)
            .background(color, shape)
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

enum class ClayButtonStyle { Filled, Soft, Plain }

/** Tactile clay button with a three-stop gradient and squishy press state. */
@Composable
fun ClayButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    accent: ClayAccent = ClayPalette.Lavender,
    style: ClayButtonStyle = ClayButtonStyle.Filled,
    enabled: Boolean = true,
    icon: ImageVector? = null,
    leadingEmoji: String? = null,
    shape: Shape = ClayShape.Pill,
    height: Dp = 52.dp,
    fillWidth: Boolean = false,
) {
    val interaction = remember { MutableInteractionSource() }
    val pressModifier = rememberClayPress(interaction)
    val contentColor = when (style) {
        ClayButtonStyle.Filled -> Color.White
        ClayButtonStyle.Soft -> accent.deep
        ClayButtonStyle.Plain -> accent.deep
    }
    val backgroundColor = when (style) {
        ClayButtonStyle.Filled -> Color.Transparent
        ClayButtonStyle.Soft -> accent.soft
        ClayButtonStyle.Plain -> ClayPalette.SurfaceCream.copy(alpha = 0.7f)
    }
    Row(
        modifier = modifier
            .then(if (fillWidth) Modifier.fillMaxWidth() else Modifier)
            .height(height)
            .then(pressModifier)
            .clay(
                shape = shape,
                elevation = if (style == ClayButtonStyle.Filled) 12.dp else 8.dp,
                glowAlpha = if (style == ClayButtonStyle.Filled) 0.32f else 0.16f,
            )
            .clip(shape)
            .background(
                brush = if (style == ClayButtonStyle.Filled) clayButtonBrush(accent) else androidx.compose.ui.graphics.SolidColor(backgroundColor),
                shape = shape,
            )
            .clickable(
                interactionSource = interaction,
                indication = null,
                enabled = enabled,
                onClick = onClick,
            )
            .padding(horizontal = 22.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        val alpha = if (enabled) 1f else 0.45f
        if (leadingEmoji != null) {
            Text(text = leadingEmoji, fontSize = 17.sp, modifier = Modifier.padding(end = 8.dp))
        }
        if (icon != null) {
            androidx.compose.material3.Icon(
                imageVector = icon,
                contentDescription = null,
                tint = contentColor.copy(alpha = alpha),
                modifier = Modifier
                    .size(19.dp)
                    .padding(end = 2.dp),
            )
            Spacer(Modifier.width(8.dp))
        }
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge,
            color = contentColor.copy(alpha = alpha),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

/** Small circular clay button used for back / close / add actions. */
@Composable
fun ClayIconButton(
    icon: ImageVector,
    onClick: () -> Unit,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    accent: ClayAccent = ClayPalette.Lavender,
    size: Dp = 46.dp,
) {
    val interaction = remember { MutableInteractionSource() }
    val pressModifier = rememberClayPress(interaction)
    Box(
        modifier = modifier
            .size(size)
            .then(pressModifier)
            .clay(shape = CircleShape, elevation = 10.dp, glowAlpha = 0.24f)
            .clip(CircleShape)
            .background(ClayPalette.SurfaceCream)
            .clickable(
                interactionSource = interaction,
                indication = null,
                onClick = onClick,
            ),
        contentAlignment = Alignment.Center,
    ) {
        androidx.compose.material3.Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = accent.deep,
            modifier = Modifier.size(size * 0.46f),
        )
    }
}

/** Pastel clay tile holding an emoji — used for activities, categories, rewards. */
@Composable
fun ClayIconTile(
    emoji: String,
    accent: ClayAccent,
    modifier: Modifier = Modifier,
    size: Dp = 50.dp,
    shape: Shape = ClayShape.Tile,
    fontSize: TextUnit = 22.sp,
) {
    Box(
        modifier = modifier
            .size(size)
            .clay(shape = shape, elevation = 3.dp, glowAlpha = 0.12f, bevelWidth = 1.5.dp)
            .clip(shape)
            .background(accent.soft),
        contentAlignment = Alignment.Center,
    ) {
        Text(text = emoji, fontSize = fontSize, textAlign = TextAlign.Center)
    }
}

/** Animated clay progress bar with bevelled track. */
@Composable
fun ClayProgressBar(
    progress: Float,
    accent: ClayAccent,
    modifier: Modifier = Modifier,
    height: Dp = 13.dp,
    trackColor: Color = ClayPalette.SurfaceMuted,
) {
    val animated by animateFloatAsState(
        targetValue = progress.coerceIn(0f, 1f),
        animationSpec = tween(durationMillis = 700, easing = FastOutSlowInEasing),
        label = "clayProgress",
    )
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .clip(ClayShape.Pill)
            .clayInset(shape = ClayShape.Pill, color = trackColor),
    ) {
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .fillMaxWidth(animated)
                .clip(ClayShape.Pill)
                .background(
                    brush = androidx.compose.ui.graphics.Brush.horizontalGradient(
                        colors = listOf(accent.base.copy(alpha = 0.85f), accent.base),
                    ),
                )
                .border(
                    width = 1.5.dp,
                    color = Color.White.copy(alpha = 0.35f),
                    shape = ClayShape.Pill,
                ),
        )
    }
}

/** Selectable clay chip (used for categories, filters and accents). */
@Composable
fun ClayChip(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    accent: ClayAccent = ClayPalette.Lavender,
    leadingEmoji: String? = null,
) {
    val interaction = remember { MutableInteractionSource() }
    val pressModifier = rememberClayPress(interaction)
    val container by animateColorAsState(
        targetValue = if (selected) accent.soft else ClayPalette.SurfaceCream,
        animationSpec = tween(220),
        label = "chipContainer",
    )
    val label by animateColorAsState(
        targetValue = if (selected) accent.deep else ClayInk.Secondary,
        animationSpec = tween(220),
        label = "chipLabel",
    )
    Row(
        modifier = modifier
            .then(pressModifier)
            .clay(
                shape = ClayShape.Pill,
                elevation = if (selected) 9.dp else 5.dp,
                glowAlpha = if (selected) 0.22f else 0.10f,
                bevelWidth = 1.5.dp,
            )
            .clip(ClayShape.Pill)
            .background(container, ClayShape.Pill)
            .clickable(
                interactionSource = interaction,
                indication = null,
                onClick = onClick,
            )
            .padding(horizontal = 15.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (leadingEmoji != null) {
            Text(text = leadingEmoji, fontSize = 14.sp, modifier = Modifier.padding(end = 6.dp))
        }
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
            color = label,
            maxLines = 1,
        )
    }
}

/** Pill segmented control with a sliding clay selection. */
@Composable
fun <T> ClaySegmentedControl(
    options: List<T>,
    selected: T,
    onSelect: (T) -> Unit,
    label: (T) -> String,
    modifier: Modifier = Modifier,
    accent: ClayAccent = ClayPalette.Lavender,
) {
    Row(
        modifier = modifier
            .clip(ClayShape.Pill)
            .clayInset(shape = ClayShape.Pill, color = ClayPalette.SurfaceMuted)
            .padding(4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        options.forEach { option ->
            val isSelected = option == selected
            val interaction = remember { MutableInteractionSource() }
            val pressModifier = rememberClayPress(interaction)
            Box(
                modifier = Modifier
                    .weight(1f)
                    .then(pressModifier)
                    .clip(ClayShape.Pill)
                    .background(
                        color = if (isSelected) ClayPalette.SurfacePure else Color.Transparent,
                        shape = ClayShape.Pill,
                    )
                    .then(
                        if (isSelected) {
                            Modifier.clayShadow(elevation = 7.dp, shape = ClayShape.Pill)
                                .clayBevel(shape = ClayShape.Pill, width = 1.5.dp)
                        } else Modifier
                    )
                    .clickable(
                        interactionSource = interaction,
                        indication = null,
                        onClick = { onSelect(option) },
                    )
                    .padding(vertical = 10.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = label(option),
                    style = MaterialTheme.typography.labelMedium,
                    color = if (isSelected) accent.deep else ClayInk.Muted,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                    maxLines = 1,
                )
            }
        }
    }
}

/** Soft inset text field. */
@Composable
fun ClayTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    placeholder: String? = null,
    accent: ClayAccent = ClayPalette.Lavender,
    keyboardType: KeyboardType = KeyboardType.Text,
    singleLine: Boolean = true,
    suffix: String? = null,
    maxLines: Int = 1,
    leadingIcon: ImageVector? = null,
) {
    Column(modifier = modifier) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = ClayInk.Secondary,
            modifier = Modifier.padding(start = 4.dp, bottom = 6.dp),
        )
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.fillMaxWidth(),
            placeholder = placeholder?.let {
                {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.bodyMedium,
                        color = ClayInk.Faint,
                    )
                }
            },
            singleLine = singleLine,
            maxLines = maxLines,
            shape = ClayShape.Small,
            leadingIcon = leadingIcon?.let { icon ->
                {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = if (value.isBlank()) ClayInk.Faint else accent.deep,
                        modifier = Modifier.size(20.dp),
                    )
                }
            },
            keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
            suffix = suffix?.let {
                {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.labelMedium,
                        color = ClayInk.Muted,
                    )
                }
            },
            textStyle = MaterialTheme.typography.bodyLarge,
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = ClayPalette.SurfacePure,
                unfocusedContainerColor = ClayPalette.SurfaceCream,
                focusedBorderColor = accent.base,
                unfocusedBorderColor = Color(0xFFE6DEF0),
                cursorColor = accent.base,
                focusedTextColor = ClayInk.Primary,
                unfocusedTextColor = ClayInk.Primary,
            ),
        )
    }
}

/** Numeric stepper with clay +/- buttons. */
@Composable
fun ClayStepper(
    value: Int,
    onValueChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
    range: IntRange = 1..999,
    step: Int = 1,
    accent: ClayAccent = ClayPalette.Lavender,
    suffix: String? = null,
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        MiniStepButton(
            symbol = "−",
            accent = accent,
            enabled = value - step >= range.first,
            onClick = { onValueChange((value - step).coerceIn(range)) },
        )
        Column(
            modifier = Modifier
                .padding(horizontal = 14.dp)
                .width(78.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = value.toString(),
                style = MaterialTheme.typography.titleLarge,
                color = ClayInk.Primary,
            )
            if (suffix != null) {
                Text(
                    text = suffix,
                    style = MaterialTheme.typography.labelSmall,
                    color = ClayInk.Muted,
                )
            }
        }
        MiniStepButton(
            symbol = "+",
            accent = accent,
            enabled = value + step <= range.last,
            onClick = { onValueChange((value + step).coerceIn(range)) },
        )
    }
}

@Composable
private fun MiniStepButton(
    symbol: String,
    accent: ClayAccent,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    val interaction = remember { MutableInteractionSource() }
    val pressModifier = rememberClayPress(interaction)
    Box(
        modifier = Modifier
            .size(44.dp)
            .then(pressModifier)
            .clay(shape = CircleShape, elevation = 8.dp, glowAlpha = 0.18f, bevelWidth = 1.5.dp)
            .clip(CircleShape)
            .background(accent.soft)
            .clickable(
                interactionSource = interaction,
                indication = null,
                enabled = enabled,
                onClick = onClick,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = symbol,
            style = MaterialTheme.typography.titleLarge,
            color = accent.deep.copy(alpha = if (enabled) 1f else 0.4f),
        )
    }
}

/** Section heading with an optional text action. */
@Composable
fun ClaySectionHeader(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = ClayInk.Primary,
            )
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = ClayInk.Muted,
                    modifier = Modifier.padding(top = 1.dp),
                )
            }
        }
        if (actionLabel != null && onAction != null) {
            TextButton(onClick = onAction) {
                Text(
                    text = actionLabel,
                    style = MaterialTheme.typography.labelMedium,
                    color = ClayPalette.Lavender.deep,
                )
            }
        }
    }
}

/** Compact stat tile used on the dashboard and analytics screens. */
@Composable
fun ClayStatTile(
    emoji: String,
    value: String,
    label: String,
    accent: ClayAccent,
    modifier: Modifier = Modifier,
) {
    ClayCard(
        modifier = modifier,
        shape = ClayShape.Medium,
        elevation = 10.dp,
        contentPadding = PaddingValues(14.dp),
    ) {
        ClayIconTile(emoji = emoji, accent = accent, size = 38.dp, fontSize = 17.sp)
        Spacer(Modifier.height(10.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.titleLarge,
            color = ClayInk.Primary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = ClayInk.Muted,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

/** Friendly empty state — never a blank screen. */
@Composable
fun ClayEmptyState(
    emoji: String,
    title: String,
    message: String,
    modifier: Modifier = Modifier,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
) {
    ClayCard(
        modifier = modifier.fillMaxWidth(),
        shape = ClayShape.Card,
        elevation = 10.dp,
        contentPadding = PaddingValues(horizontal = 22.dp, vertical = 26.dp),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            ClayIconTile(emoji = emoji, accent = ClayPalette.Lavender, size = 62.dp, fontSize = 28.sp)
            Spacer(Modifier.height(14.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = ClayInk.Primary,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = ClayInk.Muted,
                textAlign = TextAlign.Center,
            )
            if (actionLabel != null && onAction != null) {
                Spacer(Modifier.height(18.dp))
                ClayButton(
                    text = actionLabel,
                    onClick = onAction,
                    accent = ClayPalette.Lavender,
                    style = ClayButtonStyle.Soft,
                )
            }
        }
    }
}

/** Row wrapper that keeps `RowScope` content readable inside clay cards. */
@Composable
fun ClayRow(
    modifier: Modifier = Modifier,
    content: @Composable RowScope.() -> Unit,
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        content = content,
    )
}

/**
 * Local override handy for pastel tiles where content should inherit the deep
 * accent ink instead of the default onSurface colour.
 */
@Composable
fun ClayAccentInk(accent: ClayAccent, content: @Composable () -> Unit) {
    CompositionLocalProvider(LocalContentColor provides accent.deep, content = content)
}

@Composable
fun ClayVerticalSpacer(height: Dp) {
    Spacer(Modifier.height(height))
}

internal val ClayFillMaxSize: Modifier
    get() = Modifier.fillMaxSize()

internal fun rounded(radius: Dp): Shape = RoundedCornerShape(radius)
