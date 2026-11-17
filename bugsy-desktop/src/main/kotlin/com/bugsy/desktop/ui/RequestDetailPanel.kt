package com.bugsy.desktop.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bugsy.desktop.ui.theme.BugsyDesktopColors
import com.bugsy.desktop.ui.theme.formatDuration
import com.bugsy.desktop.ui.theme.statusColor
import com.bugsy.desktop.wire.WireDebugRequest

@Composable
fun RequestDetailPanel(request: WireDebugRequest?, modifier: Modifier = Modifier) {
    Box(modifier.fillMaxSize().background(BugsyDesktopColors.Background)) {
        if (request == null) {
            Text(
                "Select a request to see its details.",
                color = BugsyDesktopColors.TextDisabled,
                modifier = Modifier.align(Alignment.Center),
            )
            return@Box
        }

        LazyColumn(contentPadding = androidx.compose.foundation.layout.PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
            item {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    MethodBadge(request.method)
                    Text(
                        request.url,
                        color = BugsyDesktopColors.TextSecondary,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 12.sp,
                        modifier = Modifier.weight(1f),
                    )
                }
            }

            item {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
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
                item { CodeBlock(request.requestBody) }
            }

            if (request.responseHeaders.isNotEmpty()) {
                item { SectionLabel("RESPONSE HEADERS") }
                items(request.responseHeaders.entries.toList()) { (k, v) -> KeyValueRow(k, v) }
            }

            if (!request.responseBody.isNullOrBlank()) {
                item { SectionLabel("RESPONSE BODY") }
                item { CodeBlock(request.responseBody) }
            }

            if (request.curlCommand.isNotBlank()) {
                item { SectionLabel("cURL") }
                item { CodeBlock(request.curlCommand) }
            }
        }
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(text, color = BugsyDesktopColors.TextDisabled, fontSize = 11.sp, fontWeight = FontWeight.Medium, letterSpacing = 0.5.sp)
}

@Composable
private fun MetricCard(label: String, value: String, valueColor: androidx.compose.ui.graphics.Color, modifier: Modifier = Modifier) {
    Column(
        modifier
            .background(BugsyDesktopColors.SurfaceCard, RoundedCornerShape(8.dp))
            .padding(10.dp),
    ) {
        Text(label, color = BugsyDesktopColors.TextDisabled, fontSize = 10.sp, letterSpacing = 0.5.sp)
        Text(value, color = valueColor, fontSize = 18.sp, fontWeight = FontWeight.Medium, fontFamily = FontFamily.Monospace)
    }
}

@Composable
private fun KeyValueRow(key: String, value: String) {
    Column {
        Row(Modifier.fillMaxWidth().padding(vertical = 6.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(key, color = BugsyDesktopColors.TextDimmed, fontFamily = FontFamily.Monospace, fontSize = 12.sp, modifier = Modifier.weight(0.4f))
            Text(value, color = BugsyDesktopColors.TextSecondary, fontFamily = FontFamily.Monospace, fontSize = 12.sp, modifier = Modifier.weight(0.6f))
        }
        HorizontalDivider(color = BugsyDesktopColors.BorderSubtle)
    }
}

@Composable
private fun CodeBlock(text: String) {
    Box(
        Modifier
            .fillMaxWidth()
            .background(BugsyDesktopColors.Background, RoundedCornerShape(8.dp))
            .border(0.5.dp, BugsyDesktopColors.Border, RoundedCornerShape(8.dp))
            .padding(10.dp),
    ) {
        Text(text, color = BugsyDesktopColors.TextMuted, fontFamily = FontFamily.Monospace, fontSize = 12.sp)
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
