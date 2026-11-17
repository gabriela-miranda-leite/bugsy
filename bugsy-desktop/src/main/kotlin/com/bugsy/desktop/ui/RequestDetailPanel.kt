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
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import com.bugsy.desktop.ui.theme.BugsyDesktopColors
import com.bugsy.desktop.ui.theme.BugsyDesktopSpacing
import com.bugsy.desktop.ui.theme.BugsyDesktopTypography
import com.bugsy.desktop.ui.theme.formatDuration
import com.bugsy.desktop.ui.theme.highlightJsonPretty
import com.bugsy.desktop.ui.theme.statusColor
import com.bugsy.desktop.wire.WireDebugRequest
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Mirrors `:ui`'s `RequestDetailScreen` (DEB-60): metrics, headers, bodies (JSON-highlighted), cURL, and a
 * "Create mock from this response" action. No reveal/mask toggle here, unlike mobile — the wire protocol never
 * carries `DebugRequest.secrets` (DEB-58's `@Transient`), so there's no unmasked version to reveal; what's shown
 * is already mobile's default-masked view, same data either way.
 */
@Composable
fun RequestDetailPanel(request: WireDebugRequest?, modifier: Modifier = Modifier) {
    val clipboard = LocalClipboardManager.current
    fun copy(text: String) = clipboard.setText(AnnotatedString(text))

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
                    SelectableText(
                        request.url,
                        style = BugsyDesktopTypography.Code,
                        color = BugsyDesktopColors.TextSecondary,
                        modifier = Modifier.weight(1f),
                    )
                    CopyButton(onClick = { copy(request.url) })
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
                item { SectionLabel("REQUEST HEADERS", onCopy = { copy(request.requestHeaders.asText()) }) }
                items(request.requestHeaders.entries.toList()) { (k, v) -> KeyValueRow(k, v, onCopy = { copy("$k: $v") }) }
            }

            if (!request.requestBody.isNullOrBlank()) {
                item { SectionLabel("REQUEST BODY", onCopy = { copy(request.requestBody) }) }
                item { CodeBlock(highlightJsonPretty(request.requestBody)) }
            }

            if (request.responseHeaders.isNotEmpty()) {
                item { SectionLabel("RESPONSE HEADERS", onCopy = { copy(request.responseHeaders.asText()) }) }
                items(request.responseHeaders.entries.toList()) { (k, v) -> KeyValueRow(k, v, onCopy = { copy("$k: $v") }) }
            }

            if (!request.responseBody.isNullOrBlank()) {
                item { SectionLabel("RESPONSE BODY", onCopy = { copy(request.responseBody) }) }
                item { CodeBlock(highlightJsonPretty(request.responseBody)) }
            }

            if (request.curlCommand.isNotBlank()) {
                item { SectionLabel("cURL", onCopy = { copy(request.curlCommand) }) }
                item { CodeBlock(AnnotatedString(request.curlCommand)) }
            }

            item { CreateMockButton() }
        }
    }
}

private fun Map<String, String>.asText(): String = entries.joinToString("\n") { (k, v) -> "$k: $v" }

@Composable
private fun SectionLabel(text: String, onCopy: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
        Text(text, style = BugsyDesktopTypography.Label, color = BugsyDesktopColors.TextDisabled)
        CopyButton(onClick = onCopy)
    }
}

@Composable
private fun CopyButton(onClick: () -> Unit) {
    IconButton(onClick = onClick, modifier = Modifier.size(28.dp)) {
        Icon(Icons.Outlined.ContentCopy, contentDescription = "Copy", tint = BugsyDesktopColors.TextDisabled, modifier = Modifier.size(16.dp))
    }
}

@Composable
private fun SelectableText(text: String, style: androidx.compose.ui.text.TextStyle, color: Color, modifier: Modifier = Modifier) {
    SelectionContainer(modifier) { Text(text, style = style, color = color) }
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
private fun KeyValueRow(key: String, value: String, onCopy: () -> Unit) {
    Column {
        Row(
            Modifier.fillMaxWidth().padding(vertical = BugsyDesktopSpacing.keyValueRowVertical),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(BugsyDesktopSpacing.sm),
        ) {
            SelectionContainer(Modifier.weight(BugsyDesktopSpacing.keyValueKeyWeight)) {
                Text(key, style = BugsyDesktopTypography.Code, color = BugsyDesktopColors.TextDimmed)
            }
            SelectionContainer(Modifier.weight(BugsyDesktopSpacing.keyValueValueWeight)) {
                Text(value, style = BugsyDesktopTypography.Code, color = BugsyDesktopColors.TextSecondary)
            }
            CopyButton(onClick = onCopy)
        }
        HorizontalDivider(color = BugsyDesktopColors.BorderSubtle)
    }
}

@Composable
private fun CodeBlock(text: AnnotatedString) {
    Box(
        Modifier
            .fillMaxWidth()
            .background(BugsyDesktopColors.Background, RoundedCornerShape(BugsyDesktopSpacing.codeBlockCorner))
            .border(BugsyDesktopSpacing.codeBlockBorder, BugsyDesktopColors.Border, RoundedCornerShape(BugsyDesktopSpacing.codeBlockCorner))
            .padding(BugsyDesktopSpacing.codeBlockPadding),
    ) {
        SelectionContainer { Text(text, style = BugsyDesktopTypography.Code, color = BugsyDesktopColors.TextMuted) }
    }
}

/**
 * Present for visual/feature parity with mobile's "Create mock from this response", but not wired to anything
 * yet: that needs `BugsyWsClient` to be able to *send* a `BugsyCommand.AddMockRule` (it only ever receives
 * events today) plus a mock-editing UI, neither of which exist on desktop yet — deferred to the Mocks tab work.
 */
@Composable
private fun CreateMockButton() {
    var showHint by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    Column {
        Button(
            onClick = {
                showHint = true
                scope.launch { delay(2_500); showHint = false }
            },
            colors = ButtonDefaults.buttonColors(containerColor = BugsyDesktopColors.Brand, contentColor = BugsyDesktopColors.Surface),
            modifier = Modifier.fillMaxWidth().height(44.dp),
        ) {
            Text("Create mock from this response", style = BugsyDesktopTypography.Body)
        }
        if (showHint) {
            Text(
                "Not available yet — mock management is coming to the desktop app in a future update.",
                style = BugsyDesktopTypography.Caption,
                color = BugsyDesktopColors.TextDisabled,
                modifier = Modifier.padding(top = BugsyDesktopSpacing.xs),
            )
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
