package com.bugsy.desktop.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.sp
import com.bugsy.desktop.RequestQuickFilter
import com.bugsy.desktop.SLOW_REQUEST_THRESHOLD_MS
import com.bugsy.desktop.ui.theme.BugsyDesktopColors
import com.bugsy.desktop.ui.theme.BugsyDesktopSpacing
import com.bugsy.desktop.ui.theme.BugsyDesktopTypography
import com.bugsy.desktop.ui.theme.formatDuration
import com.bugsy.desktop.ui.theme.statusColor
import com.bugsy.desktop.wire.WireDebugRequest

/**
 * Mirrors `:ui`'s `RequestListScreen`/`RequestRow` (DEB-60 — per the user's own "see mobile's features and
 * implement them" direction, this now follows the real mobile screen rather than only the reference mockup):
 * the same quick filters (All/Errors/Slow/Mocked), the same error/slow row accent + tint, and the same
 * SLOW/MOCK tags. "Once" isn't shown — the wire protocol doesn't carry `MockMetadata.KEY_ONE_SHOT` yet.
 */
@Composable
fun RequestListScreen(
    requests: List<WireDebugRequest>,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    quickFilter: RequestQuickFilter,
    onQuickFilterChange: (RequestQuickFilter) -> Unit,
    selectedRequestId: String?,
    onSelectRequest: (String) -> Unit,
    onExportHar: () -> Unit,
    onClear: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier.fillMaxSize().background(BugsyDesktopColors.Surface)) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(BugsyDesktopSpacing.sm),
            modifier = Modifier.fillMaxWidth().padding(BugsyDesktopSpacing.md),
        ) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = onSearchQueryChange,
                placeholder = { Text("Filter by URL or method", style = BugsyDesktopTypography.Body, color = BugsyDesktopColors.TextDisabled) },
                singleLine = true,
                textStyle = BugsyDesktopTypography.Body.copy(color = BugsyDesktopColors.TextPrimary),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = BugsyDesktopColors.Brand,
                    unfocusedBorderColor = BugsyDesktopColors.Border,
                    focusedContainerColor = BugsyDesktopColors.SurfaceCard,
                    unfocusedContainerColor = BugsyDesktopColors.SurfaceCard,
                ),
                modifier = Modifier.weight(1f),
            )
            ToolbarIconButton(Icons.Outlined.Download, "Export .HAR", enabled = requests.isNotEmpty(), tint = BugsyDesktopColors.Brand, onClick = onExportHar)
            ToolbarIconButton(Icons.Outlined.DeleteOutline, "Clear", enabled = requests.isNotEmpty(), tint = BugsyDesktopColors.Status4xx, onClick = onClear)
        }

        Row(
            horizontalArrangement = Arrangement.spacedBy(BugsyDesktopSpacing.filterChipSpacing),
            modifier = Modifier.fillMaxWidth().padding(horizontal = BugsyDesktopSpacing.md, vertical = BugsyDesktopSpacing.xs),
        ) {
            RequestQuickFilter.entries.forEach { filter ->
                FilterChip(label = filter.label(), selected = filter == quickFilter, onClick = { onQuickFilterChange(filter) })
            }
        }
        HorizontalDivider(color = BugsyDesktopColors.BorderHeader)

        if (requests.isEmpty()) {
            Box(Modifier.fillMaxSize()) {
                Text(
                    if (searchQuery.isBlank()) "No requests yet." else "No requests match \"$searchQuery\".",
                    style = BugsyDesktopTypography.Body,
                    color = BugsyDesktopColors.TextDisabled,
                    modifier = Modifier.align(Alignment.Center),
                )
            }
            return@Column
        }

        LazyColumn(Modifier.fillMaxSize()) {
            items(requests, key = { it.id }) { request ->
                RequestRow(request, selected = request.id == selectedRequestId, onClick = { onSelectRequest(request.id) })
            }
        }
    }
}

private fun RequestQuickFilter.label(): String = when (this) {
    RequestQuickFilter.ALL -> "All"
    RequestQuickFilter.ERRORS -> "Errors"
    RequestQuickFilter.SLOW -> "Slow"
    RequestQuickFilter.MOCKED -> "Mocked"
}

@Composable
private fun ToolbarIconButton(icon: androidx.compose.ui.graphics.vector.ImageVector, description: String, enabled: Boolean, tint: androidx.compose.ui.graphics.Color, onClick: () -> Unit) {
    IconButton(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier
            .size(BugsyDesktopSpacing.navItemHeight)
            .border(BugsyDesktopSpacing.controlBorder, BugsyDesktopColors.Border, RoundedCornerShape(BugsyDesktopSpacing.controlCorner))
            .background(BugsyDesktopColors.SurfaceCard, RoundedCornerShape(BugsyDesktopSpacing.controlCorner)),
    ) {
        Icon(icon, contentDescription = description, tint = if (enabled) tint else BugsyDesktopColors.TextDisabled)
    }
}

@Composable
private fun FilterChip(label: String, selected: Boolean, onClick: () -> Unit) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .defaultMinSize(minHeight = 32.dp())
            .border(
                BugsyDesktopSpacing.controlBorder,
                if (selected) BugsyDesktopColors.Brand else BugsyDesktopColors.Border,
                RoundedCornerShape(BugsyDesktopSpacing.filterChipCorner),
            )
            .background(if (selected) BugsyDesktopColors.Brand else BugsyDesktopColors.SurfaceCard, RoundedCornerShape(BugsyDesktopSpacing.filterChipCorner))
            .clickable(onClick = onClick)
            .padding(horizontal = BugsyDesktopSpacing.filterChipPaddingHorizontal),
    ) {
        Text(
            label,
            style = BugsyDesktopTypography.Label,
            color = if (selected) BugsyDesktopColors.Surface else BugsyDesktopColors.TextDimmed,
        )
    }
}

private fun Int.dp() = androidx.compose.ui.unit.Dp(this.toFloat())

@Composable
private fun RequestRow(request: WireDebugRequest, selected: Boolean, onClick: () -> Unit) {
    val isSlow = request.durationMs >= SLOW_REQUEST_THRESHOLD_MS
    val isError = (request.statusCode ?: 0) >= 400
    val accentColor = when {
        isError -> statusColor(request.statusCode)
        isSlow -> BugsyDesktopColors.Status3xx
        else -> null
    }
    val (host, path) = splitHostAndPath(request.url)

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(BugsyDesktopSpacing.md),
        modifier = Modifier
            .fillMaxWidth()
            .background(
                when {
                    selected -> BugsyDesktopColors.SurfaceCard
                    accentColor != null -> accentColor.copy(alpha = 0.06f)
                    else -> BugsyDesktopColors.Surface
                },
            )
            .clickable(onClick = onClick)
            .drawBehind {
                drawLine(BugsyDesktopColors.BorderSubtle, Offset(0f, size.height), Offset(size.width, size.height), 1f)
                if (accentColor != null) drawLine(accentColor, Offset(1.5f, 0f), Offset(1.5f, size.height), 3f)
            }
            .padding(horizontal = BugsyDesktopSpacing.requestRowHorizontal, vertical = BugsyDesktopSpacing.requestRowVertical),
    ) {
        MethodBadge(request.method)
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(BugsyDesktopSpacing.xs)) {
            Text(
                path,
                style = BugsyDesktopTypography.Code.copy(color = BugsyDesktopColors.TextSecondary, fontWeight = FontWeight.Medium),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(host, style = BugsyDesktopTypography.Caption, color = BugsyDesktopColors.TextDisabled, maxLines = 1, overflow = TextOverflow.Ellipsis)
            if (request.isMocked) MockTag()
        }
        Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(BugsyDesktopSpacing.xs)) {
            Text(request.statusCode?.toString() ?: "—", style = BugsyDesktopTypography.Body.copy(fontWeight = FontWeight.Medium), color = statusColor(request.statusCode))
            Text(formatDuration(request.durationMs), style = BugsyDesktopTypography.Caption, color = BugsyDesktopColors.TextDisabled)
            if (isSlow) SlowTag()
        }
        Icon(Icons.AutoMirrored.Outlined.KeyboardArrowRight, contentDescription = null, tint = BugsyDesktopColors.TextDisabled)
    }
}

@Composable
private fun SlowTag() = Tag("SLOW", BugsyDesktopColors.Status3xx)

@Composable
private fun MockTag() = Tag("MOCK", BugsyDesktopColors.PatchText)

@Composable
private fun Tag(text: String, color: androidx.compose.ui.graphics.Color) {
    Box(Modifier.background(color.copy(alpha = 0.16f), RoundedCornerShape(BugsyDesktopSpacing.badgeCorner)).padding(horizontal = 6.dp(), vertical = 2.dp())) {
        Text(text, style = BugsyDesktopTypography.CodeBold.copy(fontFamily = FontFamily.Default, fontSize = 10.sp), color = color)
    }
}

/** "api.dev/v1/users" -> ("api.dev", "/v1/users"); with no path, the path is "/". */
private fun splitHostAndPath(url: String): Pair<String, String> {
    val withoutScheme = url.substringAfter("://", missingDelimiterValue = url).substringBefore('#').substringBefore('?')
    val slash = withoutScheme.indexOf('/')
    return if (slash < 0) withoutScheme to "/" else withoutScheme.substring(0, slash) to withoutScheme.substring(slash)
}
