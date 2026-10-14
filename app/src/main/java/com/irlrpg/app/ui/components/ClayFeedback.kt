package com.irlrpg.app.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.irlrpg.app.ui.design.ClayButton
import com.irlrpg.app.ui.design.ClayButtonStyle
import com.irlrpg.app.ui.design.clay
import com.irlrpg.app.ui.theme.ClayAccent
import com.irlrpg.app.ui.theme.ClayInk
import com.irlrpg.app.ui.theme.ClayPalette
import com.irlrpg.app.ui.theme.ClayShape
import kotlinx.coroutines.delay

/** Lightweight clay toast — used instead of snackbars so the styling stays on-brand. */
@Composable
fun ClayToast(
    message: String?,
    onShown: () -> Unit,
    modifier: Modifier = Modifier,
    bottomPadding: Dp = 28.dp,
) {
    var visible by remember { mutableStateOf(false) }
    var displayed by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(message) {
        if (message != null) {
            displayed = message
            visible = true
            delay(2600)
            visible = false
            delay(240)
            displayed = null
            onShown()
        }
    }

    if (displayed != null) {
        Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.BottomCenter) {
            AnimatedVisibility(
                visible = visible,
                enter = fadeIn() + slideInVertically { it / 3 },
                exit = fadeOut() + slideOutVertically { it / 3 },
                modifier = Modifier.padding(bottom = bottomPadding),
            ) {
                Row(
                    modifier = Modifier
                        .padding(horizontal = 24.dp)
                        .clip(ClayShape.Pill)
                        .clay(shape = ClayShape.Pill, elevation = 14.dp, glowAlpha = 0.24f)
                        .background(ClayPalette.SurfacePure)
                        .padding(horizontal = 18.dp, vertical = 13.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = displayed.orEmpty(),
                        style = MaterialTheme.typography.bodyMedium,
                        color = ClayInk.Primary,
                    )
                }
            }
        }
    }
}

/** Clay-styled confirmation dialog used for every destructive action. */
@Composable
fun ClayConfirmDialog(
    title: String,
    message: String,
    confirmLabel: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    accent: ClayAccent = ClayPalette.Blush,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = ClayPalette.SurfacePure,
        shape = ClayShape.Card,
        title = {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = ClayInk.Primary,
            )
        },
        text = {
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = ClayInk.Secondary,
            )
        },
        confirmButton = {
            ClayButton(
                text = confirmLabel,
                onClick = onConfirm,
                accent = accent,
                height = 44.dp,
            )
        },
        dismissButton = {
            ClayButton(
                text = "Cancel",
                onClick = onDismiss,
                accent = ClayPalette.Lavender,
                style = ClayButtonStyle.Plain,
                height = 44.dp,
            )
        },
    )
}

/** Stacked label + value used inside clay cards. */
@Composable
fun LabeledValue(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = ClayInk.Muted,
        )
        Spacer(Modifier.height(2.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = ClayInk.Primary,
        )
    }
}

/** Row of small stat pills, wrapping as needed. */
@Composable
fun StatPillRow(
    items: List<Pair<String, String>>,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items.forEach { (label, value) ->
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(ClayShape.Small)
                    .background(ClayPalette.SurfaceMuted.copy(alpha = 0.55f))
                    .padding(horizontal = 10.dp, vertical = 9.dp),
            ) {
                Column {
                    Text(
                        text = value,
                        style = MaterialTheme.typography.labelLarge,
                        color = ClayInk.Primary,
                        maxLines = 1,
                    )
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelSmall,
                        color = ClayInk.Muted,
                        maxLines = 1,
                    )
                }
            }
        }
    }
}

/** Thin spacer used between stacked cards. */
@Composable
fun CardGap(height: Dp = 14.dp) {
    Spacer(Modifier.height(height).width(1.dp))
}
