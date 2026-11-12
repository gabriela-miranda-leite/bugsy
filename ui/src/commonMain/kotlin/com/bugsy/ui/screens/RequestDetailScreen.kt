package com.bugsy.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import com.bugsy.core.model.DebugRequest
import com.bugsy.ui.components.CodeBlock
import com.bugsy.ui.components.ExpandableSection
import com.bugsy.ui.components.KeyValueRow
import com.bugsy.ui.components.MetricCard
import com.bugsy.ui.components.MethodBadge
import com.bugsy.ui.components.SectionLabel
import com.bugsy.ui.theme.BugsyColors
import com.bugsy.ui.theme.BugsySpacing
import com.bugsy.ui.theme.BugsyTypography
import com.bugsy.ui.theme.formatDuration
import com.bugsy.ui.theme.statusColor

@Composable
fun RequestDetailScreen(request: DebugRequest, onBack: () -> Unit) {
    val clipboard = LocalClipboardManager.current

    Column {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
                .drawBehind {
                    val s = BugsySpacing.strokeBorder.toPx()
                    drawLine(BugsyColors.Border, Offset(0f, size.height), Offset(size.width, size.height), s)
                }
                .padding(start = BugsySpacing.sm),
        ) {
            TextButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = null, tint = BugsyColors.Brand, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(2.dp))
                Text("Requests", style = BugsyTypography.Body, color = BugsyColors.Brand)
            }
        }

        LazyColumn(
            contentPadding = PaddingValues(horizontal = BugsySpacing.lg, vertical = BugsySpacing.md),
            verticalArrangement = Arrangement.spacedBy(BugsySpacing.lg),
        ) {
            item {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(BugsySpacing.sm)) {
                    MethodBadge(request.method)
                    Text(request.url, style = BugsyTypography.Code.copy(color = BugsyColors.TextSecondary), modifier = Modifier.weight(1f))
                }
            }

            item {
                Row(horizontalArrangement = Arrangement.spacedBy(BugsySpacing.sm)) {
                    MetricCard(
                        "Status",
                        request.statusCode?.toString() ?: "—",
                        valueColor = statusColor(request.statusCode),
                        modifier = Modifier.weight(1f),
                    )
                    MetricCard("Duration", formatDuration(request.durationMs), modifier = Modifier.weight(1f))
                    MetricCard("Time", request.timestamp.formatHms(), modifier = Modifier.weight(1f))
                }
            }

            if (request.requestHeaders.isNotEmpty()) {
                item { SectionLabel("Request headers") }
                items(request.requestHeaders.entries.toList()) { (k, v) -> KeyValueRow(k, v) }
            }

            if (request.requestBody?.isNotBlank() == true) {
                item {
                    ExpandableSection("Request body", initiallyExpanded = true) {
                        CodeBlock(request.requestBody ?: "")
                    }
                }
            }

            if (request.responseBody?.isNotBlank() == true) {
                item {
                    val long = (request.responseBody?.length ?: 0) > 400
                    ExpandableSection("Response body", initiallyExpanded = !long) {
                        CodeBlock(request.responseBody ?: "")
                    }
                }
            }

            item {
                CodeBlock(
                    text = request.curlCommand,
                    onCopy = { clipboard.setText(AnnotatedString(request.curlCommand)) },
                )
            }
        }
    }
}

private fun Long.formatHms(): String {
    val totalSeconds = this / 1000
    val hours = (totalSeconds / 3600) % 24
    val minutes = (totalSeconds / 60) % 60
    val seconds = totalSeconds % 60
    return "${hours.pad()}:${minutes.pad()}:${seconds.pad()}"
}

private fun Long.pad(): String = this.toString().padStart(2, '0')
