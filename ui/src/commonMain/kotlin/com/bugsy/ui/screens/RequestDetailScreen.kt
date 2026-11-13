package com.bugsy.ui.screens

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
import org.jetbrains.compose.ui.tooling.preview.Preview
import bugsy.ui.generated.resources.Res
import bugsy.ui.generated.resources.label_duration
import bugsy.ui.generated.resources.label_request_headers
import bugsy.ui.generated.resources.label_response_body
import bugsy.ui.generated.resources.label_response_headers
import bugsy.ui.generated.resources.label_status
import bugsy.ui.generated.resources.label_time
import bugsy.ui.generated.resources.tab_requests
import com.bugsy.core.model.DebugRequest
import com.bugsy.ui.PreviewFixtures
import com.bugsy.ui.components.CodeBlock
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

@Composable
fun RequestDetailScreen(request: DebugRequest, onBack: () -> Unit) {
    val clipboard = LocalClipboardManager.current

    Column {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
                .drawBehind {
                    val s = BugsySpacing.strokeBorder.toPx()
                    drawLine(BugsyColors.BorderHeader, Offset(0f, size.height), Offset(size.width, size.height), s)
                }
                .padding(start = BugsySpacing.sm),
        ) {
            TextButton(onClick = onBack) {
                Icon(
                    Icons.AutoMirrored.Outlined.ArrowBack,
                    contentDescription = null,
                    tint = BugsyColors.Brand,
                    modifier = Modifier.size(BugsySpacing.iconSizeMedium),
                )
                Spacer(Modifier.width(BugsySpacing.backButtonIconSpacing))
                Text(stringResource(Res.string.tab_requests), style = BugsyTypography.Body, color = BugsyColors.Brand)
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

            if (request.requestHeaders.isNotEmpty()) {
                item { SectionLabel(stringResource(Res.string.label_request_headers)) }
                items(request.requestHeaders.entries.toList()) { (k, v) -> KeyValueRow(k, v) }
            }

            if (request.responseHeaders.isNotEmpty()) {
                item { SectionLabel(stringResource(Res.string.label_response_headers)) }
                items(request.responseHeaders.entries.toList()) { (k, v) -> KeyValueRow(k, v) }
            }

            if (request.responseBody?.isNotBlank() == true) {
                item {
                    val long = (request.responseBody?.length ?: 0) > 400
                    ExpandableSection(stringResource(Res.string.label_response_body), initiallyExpanded = !long) {
                        CodeBlock(prettyPrintJson(request.responseBody ?: ""))
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

@Preview
@Composable
private fun RequestDetailScreenPreview() {
    BugsyTheme {
        Box(Modifier.background(BugsyColors.Surface)) {
            RequestDetailScreen(request = PreviewFixtures.postRequestError, onBack = {})
        }
    }
}
