package com.bugsy.desktop.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.bugsy.desktop.ui.theme.BugsyDesktopColors
import com.bugsy.desktop.ui.theme.BugsyDesktopSpacing
import com.bugsy.desktop.ui.theme.BugsyDesktopTypography
import com.bugsy.desktop.ui.theme.formatDuration
import com.bugsy.desktop.ui.theme.highlightJsonPretty
import com.bugsy.desktop.ui.theme.statusColor
import com.bugsy.desktop.wire.WireDebugRequest

@Composable
fun RequestDetailPanel(request: WireDebugRequest?, modifier: Modifier = Modifier) {
    Box(modifier.fillMaxSize().background(BugsyDesktopColors.Background)) {
        if (request == null) {
            Text(
                "Select a request to see its details.",
                style = BugsyDesktopTypography.Body,
                color = BugsyDesktopColors.TextDisabled,
                modifier = Modifier.align(Alignment.Center),
            )
            return@Box
        }

        LazyColumn(contentPadding = PaddingValues(BugsyDesktopSpacing.xl), verticalArrangement = Arrangement.spacedBy(BugsyDesktopSpacing.xl)) {
            item {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(BugsyDesktopSpacing.sm)) {
                    MethodBadge(request.method)
                    Text(
                        request.url,
                        style = BugsyDesktopTypography.Code,
                        color = BugsyDesktopColors.TextSecondary,
                        modifier = Modifier.weight(1f),
                    )
                }
            }

            item {
                Row(horizontalArrangement = Arrangement.spacedBy(BugsyDesktopSpacing.sm), modifier = Modifier.fillMaxWidth()) {
                    MetricCard("STATUS", request.statusCode?.toString() ?: "—", statusColor(request.statusCode), Modifier.weight(1f))
                    MetricCard("DURATION", formatDuration(request.durationMs), BugsyDesktopColors.TextPrimary, Modifier.weight(1f))
                    MetricCard("TIME", request.timestamp.formatHms(), BugsyDesktopColors.TextPrimary, Modifier.weight(1f))
                }
            }

            if (request.requestHeaders.isNotEmpty()) {
                item { SectionLabel("REQUEST HEADERS") }
                items(request.requestHeaders.entries.toList()) { (k, v) -> KeyValueRow(k, v) }
            }

            if (!request.requestBody.isNullOrBlank()) {
                item { SectionLabel("REQUEST BODY") }
                item { CodeBlock(highlightJsonPretty(request.requestBody)) }
            }

            if (request.responseHeaders.isNotEmpty()) {
                item { SectionLabel("RESPONSE HEADERS") }
                items(request.responseHeaders.entries.toList()) { (k, v) -> KeyValueRow(k, v) }
            }

            if (!request.responseBody.isNullOrBlank()) {
                item { SectionLabel("RESPONSE BODY") }
                item { CodeBlock(highlightJsonPretty(request.responseBody)) }
            }

            if (request.curlCommand.isNotBlank()) {
                item { SectionLabel("cURL") }
                item { CodeBlock(androidx.compose.ui.text.AnnotatedString(request.curlCommand)) }
            }
        }
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(text, style = BugsyDesktopTypography.Label, color = BugsyDesktopColors.TextDisabled)
}

@Composable
private fun MetricCard(label: String, value: String, valueColor: Color, modifier: Modifier = Modifier) {
    Column(
        modifier
            .background(BugsyDesktopColors.SurfaceCard, RoundedCornerShape(BugsyDesktopSpacing.metricCardCorner))
            .padding(BugsyDesktopSpacing.metricCardPadding),
    ) {
        Text(label, style = BugsyDesktopTypography.Label.copy(fontSize = BugsyDesktopSpacing.metricLabelSize), color = BugsyDesktopColors.TextDisabled)
        Text(value, style = BugsyDesktopTypography.Code.copy(fontSize = BugsyDesktopSpacing.metricValueSize), color = valueColor)
    }
}

@Composable
private fun KeyValueRow(key: String, value: String) {
    Column {
        Row(
            Modifier.fillMaxWidth().padding(vertical = BugsyDesktopSpacing.keyValueRowVertical),
            horizontalArrangement = Arrangement.spacedBy(BugsyDesktopSpacing.sm),
        ) {
            Text(
                key,
                style = BugsyDesktopTypography.Code,
                color = BugsyDesktopColors.TextDimmed,
                modifier = Modifier.weight(BugsyDesktopSpacing.keyValueKeyWeight),
            )
            Text(
                value,
                style = BugsyDesktopTypography.Code,
                color = BugsyDesktopColors.TextSecondary,
                modifier = Modifier.weight(BugsyDesktopSpacing.keyValueValueWeight),
            )
        }
        HorizontalDivider(color = BugsyDesktopColors.BorderSubtle)
    }
}

@Composable
private fun CodeBlock(text: androidx.compose.ui.text.AnnotatedString) {
    Box(
        Modifier
            .fillMaxWidth()
            .background(BugsyDesktopColors.Background, RoundedCornerShape(BugsyDesktopSpacing.codeBlockCorner))
            .border(BugsyDesktopSpacing.codeBlockBorder, BugsyDesktopColors.Border, RoundedCornerShape(BugsyDesktopSpacing.codeBlockCorner))
            .padding(BugsyDesktopSpacing.codeBlockPadding),
    ) {
        Text(text, style = BugsyDesktopTypography.Code, color = BugsyDesktopColors.TextMuted)
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
