package com.bugsy.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.Dp
import org.jetbrains.compose.ui.tooling.preview.Preview
import bugsy.ui.generated.resources.Res
import bugsy.ui.generated.resources.action_fix
import bugsy.ui.generated.resources.action_save
import bugsy.ui.generated.resources.mock_form_error_invalid_pattern
import bugsy.ui.generated.resources.mock_form_label_body
import bugsy.ui.generated.resources.mock_form_label_delay
import bugsy.ui.generated.resources.mock_form_label_headers
import bugsy.ui.generated.resources.mock_form_label_method
import bugsy.ui.generated.resources.mock_form_label_name
import bugsy.ui.generated.resources.mock_form_label_one_shot
import bugsy.ui.generated.resources.mock_form_label_status
import bugsy.ui.generated.resources.mock_form_label_url_pattern
import bugsy.ui.generated.resources.mock_form_title_edit
import bugsy.ui.generated.resources.mock_form_title_new
import bugsy.ui.generated.resources.tab_mocks
import bugsy.ui.generated.resources.toast_invalid_pattern_subtitle
import bugsy.ui.generated.resources.toast_invalid_pattern_title
import com.bugsy.core.model.MockRule
import com.bugsy.ui.components.BugsyToastAction
import com.bugsy.ui.components.BugsyToastData
import com.bugsy.ui.components.BugsyToastHost
import com.bugsy.ui.components.BugsyToastType
import com.bugsy.ui.theme.BugsyColors
import com.bugsy.ui.theme.BugsySpacing
import com.bugsy.ui.theme.BugsyTheme
import com.bugsy.ui.theme.BugsyTypography
import com.bugsy.ui.theme.highlightJsonLenient
import com.bugsy.ui.theme.prettyPrintJson
import org.jetbrains.compose.resources.stringResource
import kotlin.random.Random

private val MOCK_METHODS = listOf(null, "GET", "POST", "PUT", "DELETE", "PATCH")

/**
 * Create/edit form for a [MockRule], pushed in place of the Mocks list the same way `RequestDetailScreen`
 * replaces `RequestListScreen`. A `null` [MockRule.method] shows as "ANY" in the chip row.
 */
@Composable
fun MockRuleFormScreen(
    initial: MockRule?,
    onBack: () -> Unit,
    onSave: (MockRule) -> Unit,
    prefillSourceLabel: String? = null,
) {
    val isEditingExisting = initial != null && prefillSourceLabel == null
    var name by remember { mutableStateOf(initial?.name.orEmpty()) }
    var urlPattern by remember { mutableStateOf(initial?.urlPattern.orEmpty()) }
    var method by remember { mutableStateOf(initial?.method) }
    var statusCode by remember { mutableStateOf((initial?.statusCode ?: 200).toString()) }
    var headersText by remember { mutableStateOf(initial?.responseHeaders.toHeaderLines()) }
    var responseBody by remember { mutableStateOf(prettyPrintJson(initial?.responseBody.orEmpty())) }
    var delayMs by remember { mutableStateOf((initial?.delayMs ?: 0L).toString()) }
    var isOneShot by remember { mutableStateOf(initial?.isOneShot ?: (prefillSourceLabel != null)) }
    var patternError by remember { mutableStateOf(false) }

    val invalidPatternToastTitle = stringResource(Res.string.toast_invalid_pattern_title)
    val invalidPatternToastSubtitle = stringResource(Res.string.toast_invalid_pattern_subtitle)
    val fixActionLabel = stringResource(Res.string.action_fix)

    fun save() {
        val isValidPattern = urlPattern.isNotBlank() && runCatching { Regex(urlPattern) }.isSuccess
        if (!isValidPattern) {
            patternError = true
            BugsyToastHost.show(
                BugsyToastData(
                    type = BugsyToastType.Error,
                    title = invalidPatternToastTitle,
                    subtitle = invalidPatternToastSubtitle,
                    action = BugsyToastAction(fixActionLabel) {},
                ),
            )
            return
        }
        onSave(
            MockRule(
                id = initial?.id ?: "mock-${Random.nextLong()}",
                urlPattern = urlPattern,
                name = name.trim().ifBlank { null },
                method = method,
                statusCode = statusCode.toIntOrNull() ?: 200,
                responseHeaders = headersText.toHeaderMap(),
                responseBody = responseBody,
                delayMs = delayMs.toLongOrNull() ?: 0L,
                isOneShot = isOneShot,
                isActive = initial?.isActive ?: true,
            ),
        )
    }

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
                Text(stringResource(Res.string.tab_mocks), style = BugsyTypography.Body, color = BugsyColors.Brand)
            }
            Spacer(Modifier.weight(1f))
            Text(
                stringResource(if (isEditingExisting) Res.string.mock_form_title_edit else Res.string.mock_form_title_new),
                style = BugsyTypography.Body.copy(fontWeight = FontWeight.Medium),
                color = BugsyColors.TextPrimary,
            )
        }

        LazyColumn(
            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = BugsySpacing.lg, vertical = BugsySpacing.xl),
            verticalArrangement = Arrangement.spacedBy(BugsySpacing.lg),
            modifier = Modifier.weight(1f),
        ) {
            if (prefillSourceLabel != null) {
                item { PrefillBanner(prefillSourceLabel) }
            }
            item {
                FormField(
                    label = stringResource(Res.string.mock_form_label_name),
                    value = name,
                    onValueChange = { name = it },
                )
            }
            item {
                FormField(
                    label = stringResource(Res.string.mock_form_label_url_pattern),
                    value = urlPattern,
                    onValueChange = { urlPattern = it; patternError = false },
                    isError = patternError,
                    errorText = if (patternError) stringResource(Res.string.mock_form_error_invalid_pattern) else null,
                )
            }
            item {
                Column(verticalArrangement = Arrangement.spacedBy(BugsySpacing.sm)) {
                    FieldLabel(stringResource(Res.string.mock_form_label_method))
                    Row(horizontalArrangement = Arrangement.spacedBy(BugsySpacing.sm)) {
                        MOCK_METHODS.forEach { candidate ->
                            MethodChip(label = candidate ?: "ANY", selected = method == candidate, onClick = { method = candidate })
                        }
                    }
                }
            }
            item {
                FormField(
                    label = stringResource(Res.string.mock_form_label_status),
                    value = statusCode,
                    onValueChange = { statusCode = it.filter(Char::isDigit) },
                    keyboardType = KeyboardType.Number,
                )
            }
            item {
                FormField(
                    label = stringResource(Res.string.mock_form_label_headers),
                    value = headersText,
                    onValueChange = { headersText = it },
                    singleLine = false,
                    minHeight = BugsySpacing.mockHeadersFieldMinHeight,
                )
            }
            item {
                FormField(
                    label = stringResource(Res.string.mock_form_label_body),
                    value = responseBody,
                    onValueChange = { responseBody = it },
                    singleLine = false,
                    minHeight = BugsySpacing.mockBodyFieldMinHeight,
                    highlightJson = true,
                )
            }
            item {
                FormField(
                    label = stringResource(Res.string.mock_form_label_delay),
                    value = delayMs,
                    onValueChange = { delayMs = it.filter(Char::isDigit) },
                    keyboardType = KeyboardType.Number,
                )
            }
            item {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(BugsySpacing.md),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(
                        stringResource(Res.string.mock_form_label_one_shot),
                        style = BugsyTypography.Body,
                        color = BugsyColors.TextSecondary,
                        modifier = Modifier.weight(1f),
                    )
                    Switch(
                        checked = isOneShot,
                        onCheckedChange = { isOneShot = it },
                        colors = SwitchDefaults.colors(
                            checkedTrackColor = BugsyColors.SwitchOn,
                            checkedThumbColor = BugsyColors.TextPrimary,
                            checkedBorderColor = BugsyColors.SwitchOn,
                            uncheckedTrackColor = BugsyColors.SwitchOff,
                            uncheckedThumbColor = BugsyColors.TextPrimary,
                            uncheckedBorderColor = BugsyColors.SwitchOff,
                        ),
                    )
                }
            }
            item {
                Button(
                    onClick = ::save,
                    colors = ButtonDefaults.buttonColors(containerColor = BugsyColors.Brand, contentColor = BugsyColors.Surface),
                    shape = RoundedCornerShape(BugsySpacing.cornerButtonLarge),
                    modifier = Modifier.fillMaxWidth().height(BugsySpacing.resetAllButtonHeight),
                ) {
                    Text(stringResource(Res.string.action_save), style = BugsyTypography.Body.copy(fontWeight = FontWeight.Medium))
                }
            }
        }
    }
}

@Composable
private fun FieldLabel(text: String) {
    Text(text, style = BugsyTypography.Caption, color = BugsyColors.TextDimmed)
}

@Composable
private fun FormField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    isError: Boolean = false,
    errorText: String? = null,
    singleLine: Boolean = true,
    keyboardType: KeyboardType = KeyboardType.Text,
    minHeight: Dp? = null,
    highlightJson: Boolean = false,
) {
    Column(verticalArrangement = Arrangement.spacedBy(BugsySpacing.sm)) {
        FieldLabel(label)
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            textStyle = BugsyTypography.Code.copy(color = BugsyColors.TextSecondary),
            singleLine = singleLine,
            cursorBrush = SolidColor(BugsyColors.Brand),
            keyboardOptions = KeyboardOptions(keyboardType = keyboardType, imeAction = ImeAction.Done),
            visualTransformation = if (highlightJson) JsonHighlightTransformation else VisualTransformation.None,
            decorationBox = { inner ->
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .let { if (minHeight != null) it.heightIn(min = minHeight) else it.height(BugsySpacing.controlHeight) }
                        .background(BugsyColors.SurfaceCard, RoundedCornerShape(BugsySpacing.cornerCard))
                        .border(
                            BugsySpacing.strokeBorder,
                            if (isError) BugsyColors.Status4xx else BugsyColors.Border,
                            RoundedCornerShape(BugsySpacing.cornerCard),
                        )
                        .padding(horizontal = BugsySpacing.searchBarPaddingHorizontal, vertical = BugsySpacing.sm),
                ) {
                    inner()
                }
            },
            modifier = Modifier.fillMaxWidth(),
        )
        if (errorText != null) {
            Text(errorText, style = BugsyTypography.Caption, color = BugsyColors.Status4xx)
        }
    }
}

/**
 * Colors the response body field while it's being edited, without reformatting it — [highlightJsonLenient]
 * keeps the exact same characters, so the cursor never needs remapping ([OffsetMapping.Identity]).
 */
private object JsonHighlightTransformation : VisualTransformation {
    override fun filter(text: androidx.compose.ui.text.AnnotatedString) =
        TransformedText(highlightJsonLenient(text.text), OffsetMapping.Identity)
}

@Composable
private fun MethodChip(label: String, selected: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .border(
                BugsySpacing.strokeBorder,
                if (selected) BugsyColors.Brand else BugsyColors.Border,
                RoundedCornerShape(BugsySpacing.cornerButtonSmall),
            )
            .background(
                if (selected) BugsyColors.Brand.copy(alpha = 0.12f) else BugsyColors.SurfaceCard,
                RoundedCornerShape(BugsySpacing.cornerButtonSmall),
            )
            .clickable(onClick = onClick)
            .padding(horizontal = BugsySpacing.badgePaddingHorizontal, vertical = BugsySpacing.badgePaddingVertical),
    ) {
        Text(
            label,
            style = BugsyTypography.Caption.copy(fontWeight = FontWeight.Medium),
            color = if (selected) BugsyColors.Brand else BugsyColors.TextDisabled,
        )
    }
}

@Composable
private fun PrefillBanner(label: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(BugsyColors.Brand.copy(alpha = 0.12f), RoundedCornerShape(BugsySpacing.cornerCard))
            .padding(horizontal = BugsySpacing.md, vertical = BugsySpacing.sm),
    ) {
        Text(label, style = BugsyTypography.Caption, color = BugsyColors.Brand)
    }
}

/** A header map <-> one "Key: Value" per line, e.g. "Content-Type: application/json\nX-Api-Key: secret". */
internal fun Map<String, String>?.toHeaderLines(): String =
    this?.takeIf { it.isNotEmpty() }?.entries?.joinToString("\n") { (k, v) -> "$k: $v" } ?: "Content-Type: application/json"

/** Parses [toHeaderLines]' format back to a map; a line with no colon, or a blank name, is skipped. */
internal fun String.toHeaderMap(): Map<String, String> =
    lineSequence()
        .mapNotNull { line ->
            val parts = line.split(":", limit = 2).map { it.trim() }
            if (parts.size == 2 && parts[0].isNotBlank()) parts[0] to parts[1] else null
        }
        .toMap()

@Preview
@Composable
private fun MockRuleFormScreenNewPreview() {
    BugsyTheme {
        Box(Modifier.background(BugsyColors.Surface)) {
            MockRuleFormScreen(initial = null, onBack = {}, onSave = {})
        }
    }
}

@Preview
@Composable
private fun MockRuleFormScreenPrefilledPreview() {
    BugsyTheme {
        Box(Modifier.background(BugsyColors.Surface)) {
            MockRuleFormScreen(
                initial = MockRule(id = "mock-1", urlPattern = "^/api/v1/auth/login$", method = "POST", statusCode = 401),
                prefillSourceLabel = "Pre-filled from POST /api/v1/auth/login · 401",
                onBack = {},
                onSave = {},
            )
        }
    }
}

@Preview
@Composable
private fun MockRuleFormScreenEditPreview() {
    BugsyTheme {
        Box(Modifier.background(BugsyColors.Surface)) {
            MockRuleFormScreen(
                initial = MockRule(id = "1", urlPattern = "/api/users.*", method = "GET", statusCode = 200, responseBody = """{"users": []}"""),
                onBack = {},
                onSave = {},
            )
        }
    }
}
