package com.bugsy.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.bugsy.ui.theme.BugsyColors
import com.bugsy.ui.theme.BugsySpacing
import com.bugsy.ui.theme.BugsyTheme
import com.bugsy.ui.theme.BugsyTypography
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.jetbrains.compose.ui.tooling.preview.Preview
import kotlin.math.ceil

enum class BugsyToastType { Success, Destructive, Info, Warning, Error }

data class BugsyToastAction(val label: String, val onClick: () -> Unit)

/** @param durationMs how long the toast stays up before auto-dismissing; a 5s default fits an Undo window. */
data class BugsyToastData(
    val type: BugsyToastType,
    val title: String,
    val subtitle: String? = null,
    val action: BugsyToastAction? = null,
    val durationMs: Long = 3_000L,
    private val nonce: Long = nextNonce(),
) {
    private companion object {
        // Ensures each show() restarts the auto-dismiss timer, even for two toasts with identical content.
        private var counter = 0L
        fun nextNonce(): Long = counter++
    }
}

/** Single-toast host: showing a new one replaces whatever is up — only one is ever visible at a time. */
object BugsyToastHost {
    private val _current = MutableStateFlow<BugsyToastData?>(null)
    val current: StateFlow<BugsyToastData?> = _current

    fun show(toast: BugsyToastData) {
        _current.value = toast
    }

    fun dismiss() {
        _current.value = null
    }
}

/**
 * Renders [BugsyToastHost]'s current toast, top-aligned with a 16dp margin, auto-dismissing after its
 * duration (a destructive toast with an Undo action also shows a countdown bar driving that same timer).
 * Top, not bottom: this overlay has no bottom tab bar to sit above, and a bottom toast would cover a
 * screen's primary action button (e.g. the mock form's "Save").
 */
@Composable
fun BugsyToastOverlay(modifier: Modifier = Modifier) {
    val toast by BugsyToastHost.current.collectAsState()
    Box(modifier.fillMaxSize()) {
        AnimatedVisibility(
            visible = toast != null,
            enter = fadeIn() + slideInVertically(initialOffsetY = { -it / 2 }),
            exit = fadeOut() + slideOutVertically(targetOffsetY = { -it / 2 }),
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .padding(horizontal = BugsySpacing.lg, vertical = BugsySpacing.lg),
        ) {
            toast?.let { data -> BugsyToastCard(data) }
        }
    }
}

@Composable
internal fun BugsyToastCard(data: BugsyToastData) {
    val progress = remember(data) { Animatable(1f) }
    LaunchedEffect(data) {
        progress.snapTo(1f)
        progress.animateTo(0f, tween(durationMillis = data.durationMs.toInt(), easing = LinearEasing))
        BugsyToastHost.dismiss()
    }
    val showCountdown = data.type == BugsyToastType.Destructive && data.action != null

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(BugsyColors.SurfaceElevated, RoundedCornerShape(BugsySpacing.cornerCard))
            .padding(BugsySpacing.md),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(BugsySpacing.md)) {
            ToastIcon(data.type)
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(BugsySpacing.xs)) {
                Text(
                    data.title,
                    style = BugsyTypography.Body.copy(fontWeight = FontWeight.Medium),
                    color = BugsyColors.TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (data.subtitle != null) {
                    Text(
                        data.subtitle,
                        style = BugsyTypography.Code.copy(color = BugsyColors.TextDisabled),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            if (data.action != null) {
                Text(
                    data.action.label.uppercase(),
                    style = BugsyTypography.Caption.copy(fontWeight = FontWeight.Bold),
                    color = BugsyColors.Brand,
                    modifier = Modifier.clickable {
                        data.action.onClick()
                        BugsyToastHost.dismiss()
                    },
                )
            }
        }
        if (showCountdown) {
            Spacer(Modifier.height(BugsySpacing.sm))
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(BugsySpacing.sm)) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(ToastProgressHeight)
                        .background(BugsyColors.SwitchOff, RoundedCornerShape(ToastProgressHeight / 2)),
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(progress.value)
                            .height(ToastProgressHeight)
                            .background(BugsyColors.Brand, RoundedCornerShape(ToastProgressHeight / 2)),
                    )
                }
                val secondsLeft = ceil(progress.value * (data.durationMs / 1_000f)).toInt().coerceAtLeast(0)
                Text("${secondsLeft}s", style = BugsyTypography.Caption, color = BugsyColors.TextDisabled)
            }
        }
    }
}

@Composable
private fun ToastIcon(type: BugsyToastType) {
    val (icon, color) = when (type) {
        BugsyToastType.Success -> Icons.Outlined.Check to BugsyColors.Status2xx
        BugsyToastType.Destructive -> Icons.Outlined.DeleteOutline to BugsyColors.Status4xx
        BugsyToastType.Info -> Icons.Outlined.Info to BugsyColors.GetText
        BugsyToastType.Warning -> Icons.Outlined.WarningAmber to BugsyColors.Status3xx
        BugsyToastType.Error -> Icons.Outlined.ErrorOutline to BugsyColors.Status4xx
    }
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier.size(ToastIconSize).background(color, CircleShape),
    ) {
        Icon(icon, contentDescription = null, tint = BugsyColors.Surface, modifier = Modifier.size(BugsySpacing.iconSizeMedium))
    }
}

private val ToastIconSize = 36.dp
private val ToastProgressHeight = 4.dp

@Preview
@Composable
private fun BugsyToastPreview() {
    BugsyTheme {
        Column(
            verticalArrangement = Arrangement.spacedBy(BugsySpacing.md),
            modifier = Modifier.background(BugsyColors.Surface).padding(BugsySpacing.lg),
        ) {
            BugsyToastCard(BugsyToastData(type = BugsyToastType.Success, title = "Mock rule saved", subtitle = "GET /api/v1/feed.* -> 200"))
            BugsyToastCard(
                BugsyToastData(
                    type = BugsyToastType.Destructive,
                    title = "Mock rule deleted",
                    subtitle = "POST /api/v1/auth/login",
                    action = BugsyToastAction("Undo") {},
                    durationMs = 4_000L,
                ),
            )
            BugsyToastCard(BugsyToastData(type = BugsyToastType.Info, title = "Mock disabled", subtitle = "/api/v1/payments/.*"))
            BugsyToastCard(BugsyToastData(type = BugsyToastType.Warning, title = "Mock used up (ONCE)", subtitle = "/api/v1/auth/login · 401"))
            BugsyToastCard(
                BugsyToastData(
                    type = BugsyToastType.Error,
                    title = "Invalid regex pattern",
                    subtitle = "Check the URL match field",
                    action = BugsyToastAction("Fix") {},
                ),
            )
            BugsyToastCard(
                BugsyToastData(
                    type = BugsyToastType.Success,
                    title = "HAR exported",
                    subtitle = "48 requests",
                    action = BugsyToastAction("Share") {},
                ),
            )
        }
    }
}
