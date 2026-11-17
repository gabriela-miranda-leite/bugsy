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
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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

/**
 * Mirrors `:ui`'s `RequestDetailScreen` (DEB-60): metrics, headers, bodies (JSON-highlighted), cURL, a
 * reveal/mask toggle for secrets, and a "Create mock from this response" action.
 */
@Composable
fun RequestDetailPanel(request: WireDebugRequest?, onCreateMock: (WireDebugRequest) -> Unit, modifier: Modifier = Modifier) {
    val clipboard = LocalClipboardManager.current
    fun copy(text: String) = clipboard.setText(AnnotatedString(text))
    var revealed by remember(request?.id) { mutableStateOf(false) }

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

        val secrets = request.secrets
        val requestHeaders = if (revealed) secrets?.requestHeaders ?: request.requestHeaders else request.requestHeaders
        val requestBody = if (revealed) secrets?.requestBody ?: request.requestBody else request.requestBody
        val responseHeaders = if (revealed) secrets?.responseHeaders ?: request.responseHeaders else request.responseHeaders
        val responseBody = if (revealed) secrets?.responseBody ?: request.responseBody else request.responseBody
        val curlCommand = if (revealed) secrets?.curlCommand ?: request.curlCommand else request.curlCommand

        LazyColumn(contentPadding = PaddingValues(BugsyDesktopSpacing.xl), verticalArrangement = Arrangement.spacedBy(BugsyDesktopSpacing.xl)) {
            item {
                RevealToggleRow(
                    revealed = revealed,
                    available = secrets != null,
                    onToggle = { revealed = !revealed },
                )
            }

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

            if (requestHeaders.isNotEmpty()) {
                item { SectionLabel("REQUEST HEADERS", onCopy = { copy(requestHeaders.asText()) }) }
                items(requestHeaders.entries.toList()) { (k, v) -> KeyValueRow(k, v, onCopy = { copy("$k: $v") }) }
            }

            if (!requestBody.isNullOrBlank()) {
                item { SectionLabel("REQUEST BODY", onCopy = { copy(requestBody) }) }
                item { CodeBlock(highlightJsonPretty(requestBody)) }
            }

            if (responseHeaders.isNotEmpty()) {
                item { SectionLabel("RESPONSE HEADERS", onCopy = { copy(responseHeaders.asText()) }) }
                items(responseHeaders.entries.toList()) { (k, v) -> KeyValueRow(k, v, onCopy = { copy("$k: $v") }) }
            }

            if (!responseBody.isNullOrBlank()) {
                item { SectionLabel("RESPONSE BODY", onCopy = { copy(responseBody) }) }
                item { CodeBlock(highlightJsonPretty(responseBody)) }
            }

            if (curlCommand.isNotBlank()) {
                item { SectionLabel("cURL", onCopy = { copy(curlCommand) }) }
                item { CodeBlock(AnnotatedString(curlCommand)) }
            }

            item { CreateMockButton(onClick = { onCreateMock(request) }) }
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
    val flash = rememberCopyFlash()
    IconButton(onClick = { onClick(); flash.flash() }, modifier = Modifier.size(28.dp)) {
        Icon(
            if (flash.copied) Icons.Outlined.Check else Icons.Outlined.ContentCopy,
            contentDescription = "Copy",
            tint = if (flash.copied) BugsyDesktopColors.Status2xx else BugsyDesktopColors.TextDisabled,
            modifier = Modifier.size(16.dp),
        )
    }
}

/**
 * Mirrors mobile's reveal/mask toggle (DEB-60): the real values now travel over the wire too (`core`'s
 * `RequestSecrets` is `@Serializable`, gated by `BugsyConfig.allowRevealingSecrets` same as mobile's own
 * toggle), so flipping this swaps every header/body/cURL block below between masked and real. Disabled when
 * [available] is `false` — the host app had secrets turned off, or this particular request never had any
 * masked in the first place.
 */
@Composable
private fun RevealToggleRow(revealed: Boolean, available: Boolean, onToggle: () -> Unit) {
    Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) {
        OutlinedButton(onClick = onToggle, enabled = available) {
            Icon(
                if (revealed) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility,
                contentDescription = null,
                tint = if (available) BugsyDesktopColors.TextSecondary else BugsyDesktopColors.TextDisabled,
                modifier = Modifier.size(16.dp),
            )
            Text(
                when {
                    !available -> "No sensitive data for this request"
                    revealed -> "Hide sensitive data"
                    else -> "Show sensitive data"
                },
                style = BugsyDesktopTypography.Caption,
                color = if (available) BugsyDesktopColors.TextSecondary else BugsyDesktopColors.TextDisabled,
                modifier = Modifier.padding(start = BugsyDesktopSpacing.xs),
            )
        }
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

/** Mirrors mobile's "Create mock from this response" (DEB-61): switches to the Mocks tab with a pre-filled,
 * one-shot rule open in the form panel — see `DesktopViewModel.createMockFromRequest`. */
@Composable
private fun CreateMockButton(onClick: () -> Unit) {
    Button(
        onClick = onClick,
        colors = ButtonDefaults.buttonColors(containerColor = BugsyDesktopColors.Brand, contentColor = BugsyDesktopColors.Surface),
        modifier = Modifier.fillMaxWidth().height(44.dp),
    ) {
        Text("Create mock from this response", style = BugsyDesktopTypography.Body)
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
