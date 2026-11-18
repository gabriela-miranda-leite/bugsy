package com.bugsy.desktop.ui

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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.DeleteOutline
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.bugsy.desktop.ui.theme.BugsyDesktopColors
import com.bugsy.desktop.ui.theme.BugsyDesktopSpacing
import com.bugsy.desktop.ui.theme.BugsyDesktopTypography
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlin.math.ceil

/**
 * A local copy of `:ui`'s `BugsyToastOverlay`/`BugsyToastHost` (DEB-60) — same reasoning as the rest of this
 * module not depending on `:ui`. Same 5 types/behavior, top-aligned (no bottom bar to sit above here either).
 */
enum class DesktopToastType { Success, Destructive, Info, Warning, Error }

data class DesktopToastAction(val label: String, val onClick: () -> Unit)

data class DesktopToastData(
    val type: DesktopToastType,
    val title: String,
    val subtitle: String? = null,
    val action: DesktopToastAction? = null,
    val durationMs: Long = 3_000L,
    private val nonce: Long = nextNonce(),
) {
    private companion object {
        private var counter = 0L
        fun nextNonce(): Long = counter++
    }
}

object DesktopToastHost {
    private val _current = MutableStateFlow<DesktopToastData?>(null)
    val current: StateFlow<DesktopToastData?> = _current

    fun show(toast: DesktopToastData) {
        _current.value = toast
    }

    fun dismiss() {
        _current.value = null
    }
}

@Composable
fun DesktopToastOverlay(modifier: Modifier = Modifier) {
    val toast by DesktopToastHost.current.collectAsState()
    Box(modifier.fillMaxSize()) {
        AnimatedVisibility(
            visible = toast != null,
            enter = fadeIn() + slideInVertically(initialOffsetY = { -it / 2 }),
            exit = fadeOut() + slideOutVertically(targetOffsetY = { -it / 2 }),
            modifier = Modifier.align(Alignment.TopCenter).width(420.dp).padding(top = BugsyDesktopSpacing.lg),
        ) {
            toast?.let { data -> ToastCard(data) }
        }
    }
}

@Composable
private fun ToastCard(data: DesktopToastData) {
    val progress = remember(data) { Animatable(1f) }
    LaunchedEffect(data) {
        progress.snapTo(1f)
        progress.animateTo(0f, tween(durationMillis = data.durationMs.toInt(), easing = LinearEasing))
        DesktopToastHost.dismiss()
    }
    val showCountdown = data.type == DesktopToastType.Destructive && data.action != null

    Column(
        Modifier.fillMaxWidth().background(BugsyDesktopColors.SurfaceElevated, RoundedCornerShape(BugsyDesktopSpacing.controlCorner)).padding(BugsyDesktopSpacing.md),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(BugsyDesktopSpacing.md)) {
            ToastIcon(data.type)
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(BugsyDesktopSpacing.xs)) {
                Text(data.title, style = BugsyDesktopTypography.Body.copy(fontWeight = FontWeight.Medium), color = BugsyDesktopColors.TextPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                if (data.subtitle != null) {
                    Text(data.subtitle, style = BugsyDesktopTypography.Code.copy(color = BugsyDesktopColors.TextDisabled), maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
            if (data.action != null) {
                Text(
                    data.action.label.uppercase(),
                    style = BugsyDesktopTypography.Caption.copy(fontWeight = FontWeight.Bold),
                    color = BugsyDesktopColors.Brand,
                    modifier = Modifier.clickable { data.action.onClick(); DesktopToastHost.dismiss() },
                )
            }
        }
        if (showCountdown) {
            Spacer(Modifier.height(BugsyDesktopSpacing.sm))
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(BugsyDesktopSpacing.sm)) {
                Box(Modifier.weight(1f).height(4.dp).background(BugsyDesktopColors.SwitchOff, RoundedCornerShape(2.dp))) {
                    Box(Modifier.fillMaxWidth(progress.value).height(4.dp).background(BugsyDesktopColors.Brand, RoundedCornerShape(2.dp)))
                }
                val secondsLeft = ceil(progress.value * (data.durationMs / 1_000f)).toInt().coerceAtLeast(0)
                Text("${secondsLeft}s", style = BugsyDesktopTypography.Caption, color = BugsyDesktopColors.TextDisabled)
            }
        }
    }
}

@Composable
private fun ToastIcon(type: DesktopToastType) {
    val (icon, color) = when (type) {
        DesktopToastType.Success -> Icons.Outlined.Check to BugsyDesktopColors.Status2xx
        DesktopToastType.Destructive -> Icons.Outlined.DeleteOutline to BugsyDesktopColors.Status4xx
        DesktopToastType.Info -> Icons.Outlined.Info to BugsyDesktopColors.GetText
        DesktopToastType.Warning -> Icons.Outlined.WarningAmber to BugsyDesktopColors.Status3xx
        DesktopToastType.Error -> Icons.Outlined.ErrorOutline to BugsyDesktopColors.Status4xx
    }
    Box(contentAlignment = Alignment.Center, modifier = Modifier.size(36.dp).background(color, CircleShape)) {
        Icon(icon, contentDescription = null, tint = BugsyDesktopColors.Surface, modifier = Modifier.size(20.dp))
    }
}
