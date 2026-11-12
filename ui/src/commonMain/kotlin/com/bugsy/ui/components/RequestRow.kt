package com.bugsy.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.bugsy.core.model.DebugRequest
import com.bugsy.ui.theme.BugsyColors
import com.bugsy.ui.theme.BugsySpacing
import com.bugsy.ui.theme.BugsyTypography
import com.bugsy.ui.theme.formatDuration
import com.bugsy.ui.theme.statusColor

@Composable
fun RequestRow(
    request: DebugRequest,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = BugsySpacing.rowHorizontal, vertical = BugsySpacing.rowVertical)
            .drawBehind {
                val s = BugsySpacing.strokeBorder.toPx()
                drawLine(
                    BugsyColors.BorderSubtle,
                    Offset(0f, size.height - s / 2),
                    Offset(size.width, size.height - s / 2),
                    s,
                )
            },
    ) {
        MethodBadge(request.method)
        Text(
            text = request.url,
            style = BugsyTypography.Code.copy(color = BugsyColors.TextDimmed),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        Text(
            text = request.statusCode?.toString() ?: "—",
            style = BugsyTypography.BodySmall.copy(fontWeight = FontWeight.Medium),
            color = statusColor(request.statusCode),
        )
        Text(
            text = formatDuration(request.durationMs),
            style = BugsyTypography.Caption,
            color = BugsyColors.TextDisabled,
            modifier = Modifier.widthIn(min = 38.dp),
            textAlign = TextAlign.End,
        )
    }
}
