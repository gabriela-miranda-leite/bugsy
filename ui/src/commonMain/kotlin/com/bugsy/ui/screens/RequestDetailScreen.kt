package com.bugsy.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.OutlinedButton
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
import org.jetbrains.compose.ui.tooling.preview.Preview
import bugsy.ui.generated.resources.Res
import bugsy.ui.generated.resources.action_copy_url
import bugsy.ui.generated.resources.action_mask_sensitive
import bugsy.ui.generated.resources.action_reveal_sensitive
import bugsy.ui.generated.resources.label_duration
import bugsy.ui.generated.resources.label_request_body
import bugsy.ui.generated.resources.label_request_headers
import bugsy.ui.generated.resources.label_response_body
import bugsy.ui.generated.resources.label_response_headers
import bugsy.ui.generated.resources.label_status
import bugsy.ui.generated.resources.label_time
import bugsy.ui.generated.resources.tab_requests
import com.bugsy.core.model.DebugRequest
import com.bugsy.ui.PreviewFixtures
import com.bugsy.ui.components.CodeBlock
import com.bugsy.ui.components.CopyIconButton
import com.bugsy.ui.components.SectionHeader
import com.bugsy.ui.components.ExpandableSection
import com.bugsy.ui.components.KeyValueRow
import com.bugsy.ui.components.MetricCard
import com.bugsy.ui.components.MethodBadge
import com.bugsy.ui.components.SectionLabel
import com.bugsy.ui.theme.BugsyColors
import com.bugsy.ui.theme.BugsySpacing
import com.bugsy.ui.theme.BugsyTheme
import com.bugsy.ui.theme.BugsyTypography
import com.bugsy.ui.theme.formatDuration
import com.bugsy.ui.theme.prettyPrintJson
import com.bugsy.ui.theme.statusColor
import org.jetbrains.compose.resources.stringResource

/**
 * @param revealed when `true` (and the request has real values in memory), the screen, and everything copied
 * from it, shows the sensitive data unmasked. Controlled by the show/hide button in the top bar.
 */
@Composable
fun RequestDetailScreen(
    request: DebugRequest,
    onBack: () -> Unit,
    revealed: Boolean = false,
    onRevealedChange: (Boolean) -> Unit = {},
) {
    val clipboard = LocalClipboardManager.current
    val secrets = request.secrets
    val showReal = revealed && secrets != null

    val requestHeaders = if (showReal) secrets!!.requestHeaders else request.requestHeaders
    val responseHeaders = if (showReal) secrets!!.responseHeaders else request.responseHeaders
    val requestBody = if (showReal) secrets!!.requestBody else request.requestBody
    val responseBody = if (showReal) secrets!!.responseBody else request.responseBody
    val curl = if (showReal) secrets!!.curlCommand else request.curlCommand
    fun copy(text: String) = clipboard.setText(AnnotatedString(text))

    Column {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
                .drawBehind {
                    val s = BugsySpacing.strokeBorder.toPx()
                    drawLine(BugsyColors.BorderHeader, Offset(0f, size.height), Offset(size.width, size.height), s)
                }
                .padding(start = BugsySpacing.md, end = BugsySpacing.lg, top = BugsySpacing.xs, bottom = BugsySpacing.xs),
        ) {
            TextButton(onClick = onBack, contentPadding = PaddingValues(horizontal = BugsySpacing.md, vertical = BugsySpacing.md)) {
                Icon(
                    Icons.AutoMirrored.Outlined.ArrowBack,
                    contentDescription = null,
                    tint = BugsyColors.Brand,
                    modifier = Modifier.size(BugsySpacing.iconSizeMedium),
                )
                Spacer(Modifier.width(BugsySpacing.backButtonIconSpacing))
                Text(stringResource(Res.string.tab_requests), style = BugsyTypography.Body, color = BugsyColors.Brand)
            }
            Spacer(Modifier.weight(1f))
            if (secrets != null) {
                RevealToggle(showReal = showReal, onToggle = { onRevealedChange(!showReal) })
            }
        }

        LazyColumn(
            contentPadding = PaddingValues(horizontal = BugsySpacing.lg, vertical = BugsySpacing.xl),
            verticalArrangement = Arrangement.spacedBy(BugsySpacing.xl),
        ) {
            item {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(BugsySpacing.sm)) {
                    MethodBadge(request.method)
                    Text(request.url, style = BugsyTypography.Code.copy(color = BugsyColors.TextSecondary), modifier = Modifier.weight(1f))
                    CopyIconButton(onCopy = { copy(request.url) }, contentDescription = stringResource(Res.string.action_copy_url))
                }
            }

            item {
                Row(horizontalArrangement = Arrangement.spacedBy(BugsySpacing.sm)) {
                    MetricCard(
                        stringResource(Res.string.label_status),
                        request.statusCode?.toString() ?: "—",
                        valueColor = statusColor(request.statusCode),
                        modifier = Modifier.weight(1f),
                    )
                    MetricCard(
                        stringResource(Res.string.label_duration),
                        formatDuration(request.durationMs),
                        modifier = Modifier.weight(1f),
                    )
                    MetricCard(
                        stringResource(Res.string.label_time),
                        request.timestamp.formatHms(),
                        modifier = Modifier.weight(1f),
                    )
                }
            }

            if (requestHeaders.isNotEmpty()) {
                item { SectionHeader(stringResource(Res.string.label_request_headers), onCopy = { copy(requestHeaders.asText()) }) }
                items(requestHeaders.entries.toList()) { (k, v) -> KeyValueRow(k, v, onCopy = { copy("$k: $v") }) }
            }

            if (requestBody?.isNotBlank() == true) {
                item {
                    val body = prettyPrintJson(requestBody)
                    ExpandableSection(stringResource(Res.string.label_request_body), initiallyExpanded = requestBody.length <= 400) {
                        CodeBlock(body)
                    }
                }
            }

            if (responseHeaders.isNotEmpty()) {
                item { SectionHeader(stringResource(Res.string.label_response_headers), onCopy = { copy(responseHeaders.asText()) }) }
                items(responseHeaders.entries.toList()) { (k, v) -> KeyValueRow(k, v, onCopy = { copy("$k: $v") }) }
            }

            if (responseBody?.isNotBlank() == true) {
                item {
                    val body = prettyPrintJson(responseBody)
                    ExpandableSection(stringResource(Res.string.label_response_body), initiallyExpanded = responseBody.length <= 400) {
                        CodeBlock(body)
                    }
                }
            }

            item {
                CodeBlock(text = curl, onCopy = { copy(curl) })
            }
        }
    }
}

@Composable
private fun RevealToggle(showReal: Boolean, onToggle: () -> Unit) {
    val tint = if (showReal) BugsyColors.Brand else BugsyColors.TextDisabled
    OutlinedButton(
        onClick = onToggle,
        border = BorderStroke(BugsySpacing.strokeBorder, if (showReal) BugsyColors.Brand else BugsyColors.BorderButton),
        contentPadding = PaddingValues(horizontal = BugsySpacing.md, vertical = BugsySpacing.sm),
        shape = RoundedCornerShape(BugsySpacing.cornerButtonSmall),
    ) {
        Icon(
            if (showReal) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility,
            contentDescription = null,
            tint = tint,
            modifier = Modifier.size(BugsySpacing.iconSizeSmall),
        )
        Spacer(Modifier.width(BugsySpacing.codeBlockButtonIconSpacing))
        Text(
            stringResource(if (showReal) Res.string.action_mask_sensitive else Res.string.action_reveal_sensitive),
            style = BugsyTypography.Caption,
            color = tint,
            maxLines = 1,
        )
    }
}

private fun Map<String, String>.asText(): String = entries.joinToString("\n") { (name, value) -> "$name: $value" }

private fun Long.formatHms(): String {
    val totalSeconds = this / 1000
    val hours = (totalSeconds / 3600) % 24
    val minutes = (totalSeconds / 60) % 60
    val seconds = totalSeconds % 60
    return "${hours.pad()}:${minutes.pad()}:${seconds.pad()}"
}

private fun Long.pad(): String = this.toString().padStart(2, '0')

@Preview
@Composable
private fun RequestDetailScreenPreview() {
    BugsyTheme {
        Box(Modifier.background(BugsyColors.Surface)) {
            RequestDetailScreen(request = PreviewFixtures.postRequestError, onBack = {})
        }
    }
}
