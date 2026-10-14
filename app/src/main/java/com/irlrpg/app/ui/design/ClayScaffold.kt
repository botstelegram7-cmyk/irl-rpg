package com.irlrpg.app.ui.design

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.irlrpg.app.ui.theme.ClayAccent
import com.irlrpg.app.ui.theme.ClayBackgroundBrush
import com.irlrpg.app.ui.theme.ClayInk
import com.irlrpg.app.ui.theme.ClayPalette
import com.irlrpg.app.ui.theme.ClayShape

/** A navigation destination rendered in the floating clay bottom bar. */
data class ClayDestination(
    val route: String,
    val label: String,
    val glyph: ClayGlyphType,
    val accent: ClayAccent,
)

/**
 * Page background: warm ivory wash plus three very soft pastel light pools.
 * Drawn cheaply with radial gradients (no bitmaps, no blur passes), so it stays
 * smooth even on budget devices.
 */
@Composable
fun ClayBackground(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit,
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(ClayBackgroundBrush),
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            drawRect(
                brush = Brush.radialGradient(
                    colors = listOf(ClayPalette.Lavender.base.copy(alpha = 0.16f), Color.Transparent),
                    center = Offset(w * 0.92f, h * 0.06f),
                    radius = w * 0.75f,
                ),
                size = size,
            )
            drawRect(
                brush = Brush.radialGradient(
                    colors = listOf(ClayPalette.Sky.base.copy(alpha = 0.13f), Color.Transparent),
                    center = Offset(w * 0.04f, h * 0.34f),
                    radius = w * 0.62f,
                ),
                size = size,
            )
            drawRect(
                brush = Brush.radialGradient(
                    colors = listOf(ClayPalette.Mint.base.copy(alpha = 0.12f), Color.Transparent),
                    center = Offset(w * 0.86f, h * 0.94f),
                    radius = w * 0.7f,
                ),
                size = size,
            )
        }
        content()
    }
}

@Composable
fun ClayScaffold(
    modifier: Modifier = Modifier,
    topBar: @Composable () -> Unit = {},
    bottomBar: @Composable () -> Unit = {},
    content: @Composable (PaddingValues) -> Unit,
) {
    ClayBackground(modifier = modifier) {
        Scaffold(
            containerColor = Color.Transparent,
            topBar = topBar,
            bottomBar = bottomBar,
            content = content,
        )
    }
}

/** Simple in-page header with optional back button and trailing actions. */
@Composable
fun ClayTopBar(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    onBack: (() -> Unit)? = null,
    accent: ClayAccent = ClayPalette.Lavender,
    actions: @Composable RowScope.() -> Unit = {},
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (onBack != null) {
            ClayIconButton(
                icon = Icons.Rounded.ArrowBack,
                onClick = onBack,
                contentDescription = "Back",
                accent = accent,
                size = 44.dp,
            )
            Spacer(Modifier.width(12.dp))
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                color = ClayInk.Primary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = ClayInk.Muted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        actions()
    }
}

/** Floating clay bottom navigation bar with an expanding selected pill. */
@Composable
fun ClayBottomBar(
    destinations: List<ClayDestination>,
    selectedRoute: String?,
    onSelect: (ClayDestination) -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(start = 16.dp, end = 16.dp, bottom = 12.dp),
        contentAlignment = Alignment.Center,
    ) {
        Row(
            modifier = Modifier
                .clay(shape = ClayShape.Hero, elevation = 18.dp, glowAlpha = 0.30f)
                .clip(ClayShape.Hero)
                .background(ClayPalette.SurfaceCream.copy(alpha = 0.98f))
                .padding(horizontal = 10.dp, vertical = 9.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            destinations.forEach { destination ->
                val selected = destination.route == selectedRoute
                ClayBarItem(
                    destination = destination,
                    selected = selected,
                    onClick = { onSelect(destination) },
                )
            }
        }
    }
}

@Composable
private fun ClayBarItem(
    destination: ClayDestination,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val interaction = remember { MutableInteractionSource() }
    val pressModifier = rememberClayPress(interaction)
    val container by animateColorAsState(
        targetValue = if (selected) destination.accent.soft else Color.Transparent,
        animationSpec = tween(250),
        label = "barContainer",
    )
    val glyphTint by animateColorAsState(
        targetValue = if (selected) destination.accent.deep else ClayInk.Faint,
        animationSpec = tween(250),
        label = "barGlyph",
    )
    Row(
        modifier = Modifier
            .then(pressModifier)
            .clip(ClayShape.Pill)
            .background(container, ClayShape.Pill)
            .clickable(
                interactionSource = interaction,
                indication = null,
                onClick = onClick,
            )
            .padding(horizontal = if (selected) 13.dp else 11.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ClayGlyph(
            type = destination.glyph,
            tint = glyphTint,
            filled = selected,
            modifier = Modifier.size(22.dp),
        )
        AnimatedVisibility(
            visible = selected,
            enter = fadeIn(tween(200)) + expandHorizontally(tween(240)),
            exit = fadeOut(tween(140)) + shrinkHorizontally(tween(180)),
        ) {
            Row {
                Spacer(Modifier.width(7.dp))
                Text(
                    text = destination.label,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = destination.accent.deep,
                    maxLines = 1,
                )
            }
        }
    }
}

/** Small floating action button used on the Quests screen. */
@Composable
fun ClayFab(
    emoji: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    accent: ClayAccent = ClayPalette.Lavender,
    contentDescription: String = "Create",
) {
    val interaction = remember { MutableInteractionSource() }
    val pressModifier = rememberClayPress(interaction)
    Row(
        modifier = modifier
            .height(56.dp)
            .then(pressModifier)
            .clay(shape = ClayShape.Pill, elevation = 16.dp, glowAlpha = 0.34f)
            .clip(ClayShape.Pill)
            .background(
                brush = com.irlrpg.app.ui.theme.clayButtonBrush(accent),
                shape = ClayShape.Pill,
            )
            .clickable(
                interactionSource = interaction,
                indication = null,
                onClick = onClick,
            )
            .padding(horizontal = 20.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(text = emoji, style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.width(8.dp))
        Text(
            text = contentDescription,
            style = MaterialTheme.typography.labelLarge,
            color = Color.White,
            maxLines = 1,
        )
    }
}
