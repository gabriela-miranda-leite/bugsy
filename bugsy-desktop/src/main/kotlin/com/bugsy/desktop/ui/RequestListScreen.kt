package com.bugsy.desktop.ui

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.bugsy.desktop.ui.theme.BugsyDesktopColors
import com.bugsy.desktop.ui.theme.BugsyDesktopSpacing
import com.bugsy.desktop.ui.theme.BugsyDesktopTypography
import com.bugsy.desktop.ui.theme.formatDuration
import com.bugsy.desktop.ui.theme.statusColor
import com.bugsy.desktop.wire.WireDebugRequest

private val Methods = listOf("GET", "POST", "PUT", "DELETE", "PATCH")

@Composable
fun RequestListScreen(
    requests: List<WireDebugRequest>,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    methodFilter: String?,
    onMethodFilterChange: (String?) -> Unit,
    selectedRequestId: String?,
    onSelectRequest: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier.fillMaxSize().background(BugsyDesktopColors.Surface)) {
        OutlinedTextField(
            value = searchQuery,
            onValueChange = onSearchQueryChange,
            placeholder = { Text("Search by URL or method", style = BugsyDesktopTypography.Body, color = BugsyDesktopColors.TextDisabled) },
            singleLine = true,
            textStyle = BugsyDesktopTypography.Body.copy(color = BugsyDesktopColors.TextPrimary),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = BugsyDesktopColors.Brand,
                unfocusedBorderColor = BugsyDesktopColors.Border,
                focusedContainerColor = BugsyDesktopColors.SurfaceCard,
                unfocusedContainerColor = BugsyDesktopColors.SurfaceCard,
            ),
            modifier = Modifier.fillMaxWidth().padding(BugsyDesktopSpacing.md),
        )

        Row(
            horizontalArrangement = Arrangement.spacedBy(BugsyDesktopSpacing.filterChipSpacing),
            modifier = Modifier.fillMaxWidth().horizontalScroll(ScrollState(0))
                .padding(horizontal = BugsyDesktopSpacing.md, vertical = BugsyDesktopSpacing.xs),
        ) {
            FilterChip(label = "All", selected = methodFilter == null, onClick = { onMethodFilterChange(null) })
            Methods.forEach { method ->
                FilterChip(label = method, selected = methodFilter == method, onClick = { onMethodFilterChange(method) })
            }
        }
        HorizontalDivider(color = BugsyDesktopColors.BorderHeader)

        if (requests.isEmpty()) {
            Box(Modifier.fillMaxSize()) {
                Text(
                    "No requests yet.",
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
                HorizontalDivider(color = BugsyDesktopColors.BorderSubtle)
            }
        }
    }
}

@Composable
private fun FilterChip(label: String, selected: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .clickable(onClick = onClick)
            .background(
                if (selected) BugsyDesktopColors.Brand.copy(alpha = 0.18f) else BugsyDesktopColors.SurfaceCard,
                RoundedCornerShape(BugsyDesktopSpacing.filterChipCorner),
            )
            .border(
                BugsyDesktopSpacing.controlBorder,
                if (selected) BugsyDesktopColors.Brand else BugsyDesktopColors.Border,
                RoundedCornerShape(BugsyDesktopSpacing.filterChipCorner),
            )
            .padding(horizontal = BugsyDesktopSpacing.filterChipPaddingHorizontal, vertical = BugsyDesktopSpacing.filterChipPaddingVertical),
    ) {
        Text(
            label,
            style = BugsyDesktopTypography.Label,
            color = if (selected) BugsyDesktopColors.Brand else BugsyDesktopColors.TextDimmed,
        )
    }
}

@Composable
private fun RequestRow(request: WireDebugRequest, selected: Boolean, onClick: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(BugsyDesktopSpacing.requestRowSpacing),
        modifier = Modifier
            .fillMaxWidth()
            .background(if (selected) BugsyDesktopColors.SurfaceCard else BugsyDesktopColors.Surface)
            .clickable(onClick = onClick)
            .padding(horizontal = BugsyDesktopSpacing.requestRowHorizontal, vertical = BugsyDesktopSpacing.requestRowVertical),
    ) {
        MethodBadge(request.method)
        Text(
            request.url,
            style = BugsyDesktopTypography.CodeSmall,
            color = BugsyDesktopColors.TextDimmed,
            maxLines = 1,
            modifier = Modifier.weight(1f),
        )
        Text(request.statusCode?.toString() ?: "—", style = BugsyDesktopTypography.CodeSmall, color = statusColor(request.statusCode))
        Text(formatDuration(request.durationMs), style = BugsyDesktopTypography.Caption, color = BugsyDesktopColors.TextDisabled)
    }
}
