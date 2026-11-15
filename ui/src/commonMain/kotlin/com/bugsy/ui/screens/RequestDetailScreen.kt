package com.bugsy.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
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
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.ui.tooling.preview.Preview
import bugsy.ui.generated.resources.Res
import bugsy.ui.generated.resources.action_copy_url
import bugsy.ui.generated.resources.action_create_mock_from_response
import bugsy.ui.generated.resources.action_mask_sensitive
import bugsy.ui.generated.resources.action_reveal_sensitive
import bugsy.ui.generated.resources.label_duration
import bugsy.ui.generated.resources.label_request_body
import bugsy.ui.generated.resources.label_request_headers
import bugsy.ui.generated.resources.label_response_body
import bugsy.ui.generated.resources.label_response_headers
import bugsy.ui.generated.resources.label_status
import bugsy.ui.generated.resources.label_time
import bugsy.ui.generated.resources.loading_request_details_title
import bugsy.ui.generated.resources.loading_subtitle_default
import bugsy.ui.generated.resources.tab_requests
import bugsy.ui.generated.resources.tooltip_slow_request
import com.bugsy.core.DebugRepository
import com.bugsy.core.model.DebugRequest
import com.bugsy.ui.PreviewFixtures
import com.bugsy.ui.components.CodeBlock
import com.bugsy.ui.components.CopyIconButton
import com.bugsy.ui.components.SectionHeader
import com.bugsy.ui.components.ExpandableSection
import com.bugsy.ui.components.KeyValueRow
import com.bugsy.ui.components.LoadingState
import com.bugsy.ui.components.MetricCard
import com.bugsy.ui.components.MethodBadge
import com.bugsy.ui.components.SectionLabel
import com.bugsy.ui.theme.BugsyColors
import com.bugsy.ui.theme.BugsySpacing
import com.bugsy.ui.theme.BugsyTheme
import com.bugsy.ui.theme.BugsyTypography
import com.bugsy.ui.theme.formatDuration
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
    isLoading: Boolean = false,
    onCreateMock: (() -> Unit)? = null,
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

        if (isLoading) {
            LoadingState(
                title = stringResource(Res.string.loading_request_details_title),
                subtitle = stringResource(Res.string.loading_subtitle_default),
            )
            return@Column
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
                    val slowThreshold = DebugRepository.slowRequestThresholdMs
                    val isSlow = slowThreshold > 0 && request.durationMs >= slowThreshold
                    if (isSlow) {
                        SlowDurationMetric(
                            durationMs = request.durationMs,
                            thresholdMs = slowThreshold,
                            modifier = Modifier.weight(1f),
                        )
                    } else {
                        MetricCard(
                            stringResource(Res.string.label_duration),
                            formatDuration(request.durationMs),
                            modifier = Modifier.weight(1f),
                        )
                    }
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
                    ExpandableSection(stringResource(Res.string.label_request_body), initiallyExpanded = requestBody.length <= 400) {
                        CodeBlock(requestBody, json = true)
                    }
                }
            }

            if (responseHeaders.isNotEmpty()) {
                item { SectionHeader(stringResource(Res.string.label_response_headers), onCopy = { copy(responseHeaders.asText()) }) }
                items(responseHeaders.entries.toList()) { (k, v) -> KeyValueRow(k, v, onCopy = { copy("$k: $v") }) }
            }

            if (responseBody?.isNotBlank() == true) {
                item {
                    ExpandableSection(stringResource(Res.string.label_response_body), initiallyExpanded = responseBody.length <= 400) {
                        CodeBlock(responseBody, json = true)
                    }
                }
            }

            item {
                CodeBlock(text = curl, onCopy = { copy(curl) })
            }

            if (onCreateMock != null) {
                item {
                    Button(
                        onClick = onCreateMock,
                        colors = ButtonDefaults.buttonColors(containerColor = BugsyColors.Brand, contentColor = BugsyColors.Surface),
                        shape = RoundedCornerShape(BugsySpacing.cornerButtonLarge),
                        modifier = Modifier.fillMaxWidth().height(BugsySpacing.mockCreateButtonHeight),
                    ) {
                        Text(stringResource(Res.string.action_create_mock_from_response), style = BugsyTypography.Body)
                    }
                }
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

/** Duration metric card for a slow request: tapping it shows a tooltip with the duration and the threshold. */
@Composable
private fun SlowDurationMetric(durationMs: Long, thresholdMs: Long, modifier: Modifier = Modifier) {
    var showTooltip by remember { mutableStateOf(false) }
    Box(modifier = modifier) {
        MetricCard(
            label = stringResource(Res.string.label_duration),
            value = formatDuration(durationMs),
            valueColor = BugsyColors.Status3xx,
            modifier = Modifier.fillMaxWidth().clickable { showTooltip = !showTooltip },
        )
        if (showTooltip) {
            SlowTooltip(
                text = stringResource(Res.string.tooltip_slow_request, formatDuration(durationMs), formatDuration(thresholdMs)),
                modifier = Modifier.align(Alignment.TopCenter).offset(y = -SlowTooltipOffset),
            )
        }
    }
}

@Composable
private fun SlowTooltip(text: String, modifier: Modifier = Modifier) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = modifier) {
        Box(
            modifier = Modifier
                .background(BugsyColors.Status3xx, RoundedCornerShape(BugsySpacing.cornerButtonSmall))
                .padding(horizontal = BugsySpacing.sm, vertical = BugsySpacing.xs),
        ) {
            Text(text, style = BugsyTypography.Caption, color = BugsyColors.Background, maxLines = 1)
        }
        Box(
            modifier = Modifier
                .size(SlowTooltipArrowSize)
                .offset(y = -(SlowTooltipArrowSize / 2))
                .rotate(45f)
                .background(BugsyColors.Status3xx),
        )
    }
}

private val SlowTooltipOffset = 8.dp
private val SlowTooltipArrowSize = 8.dp

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

@Preview
@Composable
private fun RequestDetailScreenSlowPreview() {
    BugsyTheme {
        Box(Modifier.background(BugsyColors.Surface)) {
            RequestDetailScreen(request = PreviewFixtures.getRequestSlow, onBack = {})
        }
    }
}

@Preview
@Composable
private fun RequestDetailScreenWithCreateMockPreview() {
    BugsyTheme {
        Box(Modifier.background(BugsyColors.Surface)) {
            RequestDetailScreen(request = PreviewFixtures.postRequestError, onBack = {}, onCreateMock = {})
        }
    }
}

@Preview
@Composable
private fun RequestDetailScreenLoadingPreview() {
    BugsyTheme {
        Box(Modifier.background(BugsyColors.Surface)) {
            RequestDetailScreen(request = PreviewFixtures.postRequestError, onBack = {}, isLoading = true)
        }
    }
}
