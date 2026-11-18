package com.bugsy.desktop.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.bugsy.desktop.ui.theme.BugsyDesktopColors
import com.bugsy.desktop.ui.theme.BugsyDesktopSpacing
import com.bugsy.desktop.ui.theme.BugsyDesktopTypography
import com.bugsy.desktop.ui.theme.highlightJsonLenient
import com.bugsy.desktop.ui.theme.prettyPrintJson
import com.bugsy.desktop.wire.WireMockRule
import kotlin.random.Random

private val MOCK_METHODS = listOf(null, "GET", "POST", "PUT", "PATCH", "DELETE")

/**
 * Mirrors `:ui`'s `MockRuleFormScreen` (DEB-61), with the reference design's layout for response headers: a
 * list of key/value rows with "+ Add header" and a delete icon per row, instead of mobile's single multi-line
 * "Key: Value" text field — same data (`Map<String, String>`), different editing affordance, matching the
 * mockup's "03-mocks.jpg"/"04-mock-da-request.jpg" screens.
 *
 * Import/export (mobile's `MockRuleFileSerializer`, share-sheet/file-picker driven) and the separate read-only
 * detail screen (hit count, last matches) aren't implemented yet — this panel only covers create/edit, which is
 * what the reference design shows.
 */
@Composable
fun MockRuleFormPanel(
    initial: WireMockRule?,
    prefillSourceLabel: String?,
    onSave: (WireMockRule) -> Unit,
    onDelete: (() -> Unit)?,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val isEditingExisting = initial != null && prefillSourceLabel == null
    var name by remember(initial) { mutableStateOf(initial?.name.orEmpty()) }
    var urlPattern by remember(initial) { mutableStateOf(initial?.urlPattern.orEmpty()) }
    var method by remember(initial) { mutableStateOf(initial?.method) }
    var statusCode by remember(initial) { mutableStateOf((initial?.statusCode ?: 200).toString()) }
    var delayMs by remember(initial) { mutableStateOf((initial?.delayMs ?: 0L).toString()) }
    val headerRows = remember(initial) {
        mutableStateListOf(*(initial?.responseHeaders?.takeIf { it.isNotEmpty() } ?: mapOf("Content-Type" to "application/json")).toList().toTypedArray())
    }
    var responseBody by remember(initial) { mutableStateOf(prettyPrintJson(initial?.responseBody.orEmpty())) }
    var isOneShot by remember(initial) { mutableStateOf(initial?.isOneShot ?: (prefillSourceLabel != null)) }
    var patternError by remember(initial) { mutableStateOf(false) }

    fun save() {
        val isValidPattern = urlPattern.isNotBlank() && runCatching { Regex(urlPattern) }.isSuccess
        if (!isValidPattern) {
            patternError = true
            DesktopToastHost.show(DesktopToastData(type = DesktopToastType.Error, title = "Invalid regex pattern", subtitle = "Check the URL pattern field"))
            return
        }
        onSave(
            WireMockRule(
                id = initial?.id ?: "mock-${Random.nextLong()}",
                urlPattern = urlPattern,
                name = name.trim().ifBlank { null },
                method = method,
                statusCode = statusCode.toIntOrNull() ?: 200,
                responseHeaders = headerRows.filter { it.first.isNotBlank() }.toMap(),
                responseBody = responseBody,
                delayMs = delayMs.toLongOrNull() ?: 0L,
                isOneShot = isOneShot,
                isActive = initial?.isActive ?: true,
            ),
        )
    }

    Column(modifier.fillMaxSize().background(BugsyDesktopColors.Background)) {
        Text(
            if (isEditingExisting) "Edit mock rule" else "New mock rule",
            style = BugsyDesktopTypography.Heading,
            color = BugsyDesktopColors.TextPrimary,
            modifier = Modifier.padding(BugsyDesktopSpacing.xl),
        )

        LazyColumn(
            contentPadding = PaddingValues(horizontal = BugsyDesktopSpacing.xl, vertical = BugsyDesktopSpacing.sm),
            verticalArrangement = Arrangement.spacedBy(BugsyDesktopSpacing.lg),
            modifier = Modifier.weight(1f),
        ) {
            if (prefillSourceLabel != null) {
                item { PrefillBanner(prefillSourceLabel) }
            }
            item {
                FormField(label = "Name (optional — defaults to the route)", value = name, onValueChange = { name = it })
            }
            item {
                FormField(
                    label = "URL pattern (regex)",
                    value = urlPattern,
                    onValueChange = { urlPattern = it; patternError = false },
                    isError = patternError,
                    errorText = if (patternError) "Invalid regex pattern" else null,
                )
            }
            item {
                Column(verticalArrangement = Arrangement.spacedBy(BugsyDesktopSpacing.sm)) {
                    FieldLabel("Method")
                    Row(horizontalArrangement = Arrangement.spacedBy(BugsyDesktopSpacing.sm)) {
                        MOCK_METHODS.forEach { candidate ->
                            MethodChip(label = candidate ?: "ANY", selected = method == candidate, onClick = { method = candidate })
                        }
                    }
                }
            }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(BugsyDesktopSpacing.lg)) {
                    Box(Modifier.weight(1f)) {
                        FormField(label = "Status code", value = statusCode, onValueChange = { statusCode = it.filter(Char::isDigit) }, keyboardType = KeyboardType.Number)
                    }
                    Box(Modifier.weight(1f)) {
                        FormField(label = "Response delay (ms)", value = delayMs, onValueChange = { delayMs = it.filter(Char::isDigit) }, keyboardType = KeyboardType.Number)
                    }
                }
            }
            item {
                Column(verticalArrangement = Arrangement.spacedBy(BugsyDesktopSpacing.sm)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                        FieldLabel("Response headers")
                        TextButton(onClick = { headerRows.add("" to "") }) {
                            Icon(Icons.Outlined.Add, contentDescription = null, tint = BugsyDesktopColors.Brand, modifier = Modifier.size(16.dp))
                            Text("Add header", style = BugsyDesktopTypography.Caption, color = BugsyDesktopColors.Brand)
                        }
                    }
                    headerRows.forEachIndexed { index, (key, value) ->
                        Row(horizontalArrangement = Arrangement.spacedBy(BugsyDesktopSpacing.sm), verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                            Box(Modifier.weight(1f)) { PlainField(value = key, onValueChange = { headerRows[index] = it to value }) }
                            Box(Modifier.weight(1f)) { PlainField(value = value, onValueChange = { headerRows[index] = key to it }) }
                            IconButton(onClick = { headerRows.removeAt(index) }, modifier = Modifier.size(28.dp)) {
                                Icon(Icons.Outlined.DeleteOutline, contentDescription = "Remove header", tint = BugsyDesktopColors.TextDisabled, modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                }
            }
            item {
                FormField(
                    label = "Response body",
                    value = responseBody,
                    onValueChange = { responseBody = it },
                    singleLine = false,
                    minHeight = BugsyDesktopSpacing.mockBodyFieldMinHeight,
                    highlightJson = true,
                )
            }
            item {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(BugsyDesktopSpacing.md), modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.weight(1f)) {
                        Text("Use only once", style = BugsyDesktopTypography.Body, color = BugsyDesktopColors.TextSecondary)
                        Text("This mock will be used for a single request", style = BugsyDesktopTypography.Caption, color = BugsyDesktopColors.TextDisabled)
                    }
                    Switch(
                        checked = isOneShot,
                        onCheckedChange = { isOneShot = it },
                        colors = SwitchDefaults.colors(
                            checkedTrackColor = BugsyDesktopColors.SwitchOn,
                            checkedThumbColor = BugsyDesktopColors.TextPrimary,
                            checkedBorderColor = BugsyDesktopColors.SwitchOn,
                            uncheckedTrackColor = BugsyDesktopColors.SwitchOff,
                            uncheckedThumbColor = BugsyDesktopColors.TextPrimary,
                            uncheckedBorderColor = BugsyDesktopColors.SwitchOff,
                        ),
                    )
                }
            }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(BugsyDesktopSpacing.md), modifier = Modifier.fillMaxWidth().padding(bottom = BugsyDesktopSpacing.xl)) {
                    if (onDelete != null) {
                        OutlinedButton(
                            onClick = onDelete,
                            border = androidx.compose.foundation.BorderStroke(BugsyDesktopSpacing.controlBorder, BugsyDesktopColors.Status4xx),
                            modifier = Modifier.weight(1f),
                        ) {
                            Icon(Icons.Outlined.DeleteOutline, contentDescription = null, tint = BugsyDesktopColors.Status4xx, modifier = Modifier.size(16.dp))
                            Text("Delete", style = BugsyDesktopTypography.Body, color = BugsyDesktopColors.Status4xx, modifier = Modifier.padding(start = BugsyDesktopSpacing.xs))
                        }
                    } else {
                        OutlinedButton(onClick = onCancel, modifier = Modifier.weight(1f)) {
                            Text("Cancel", style = BugsyDesktopTypography.Body, color = BugsyDesktopColors.TextSecondary)
                        }
                    }
                    Button(
                        onClick = ::save,
                        colors = ButtonDefaults.buttonColors(containerColor = BugsyDesktopColors.Brand, contentColor = BugsyDesktopColors.Surface),
                        modifier = Modifier.weight(1f),
                    ) {
                        Text("Save rule", style = BugsyDesktopTypography.Body.copy(fontWeight = FontWeight.Medium))
                    }
                }
            }
        }
    }
}

@Composable
private fun FieldLabel(text: String) {
    Text(text, style = BugsyDesktopTypography.Caption, color = BugsyDesktopColors.TextDimmed)
}

@Composable
private fun PrefillBanner(label: String) {
    Box(
        Modifier.fillMaxWidth().background(BugsyDesktopColors.Brand.copy(alpha = 0.12f), RoundedCornerShape(BugsyDesktopSpacing.controlCorner)).padding(horizontal = BugsyDesktopSpacing.md, vertical = BugsyDesktopSpacing.sm),
    ) {
        Text(label, style = BugsyDesktopTypography.Caption, color = BugsyDesktopColors.Brand)
    }
}

@Composable
private fun MethodChip(label: String, selected: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .border(BugsyDesktopSpacing.controlBorder, if (selected) BugsyDesktopColors.Brand else BugsyDesktopColors.Border, RoundedCornerShape(BugsyDesktopSpacing.controlCorner))
            .background(if (selected) BugsyDesktopColors.Brand.copy(alpha = 0.12f) else BugsyDesktopColors.SurfaceCard, RoundedCornerShape(BugsyDesktopSpacing.controlCorner))
            .clickable(onClick = onClick)
            .padding(horizontal = BugsyDesktopSpacing.badgePaddingHorizontal.times(2), vertical = BugsyDesktopSpacing.badgePaddingVertical.times(2)),
    ) {
        Text(label, style = BugsyDesktopTypography.Caption.copy(fontWeight = FontWeight.Medium), color = if (selected) BugsyDesktopColors.Brand else BugsyDesktopColors.TextDisabled)
    }
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
    Column(verticalArrangement = Arrangement.spacedBy(BugsyDesktopSpacing.fieldLabelSpacing)) {
        FieldLabel(label)
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            textStyle = BugsyDesktopTypography.Code.copy(color = BugsyDesktopColors.TextSecondary),
            singleLine = singleLine,
            cursorBrush = SolidColor(BugsyDesktopColors.Brand),
            keyboardOptions = KeyboardOptions(keyboardType = keyboardType, imeAction = ImeAction.Done),
            visualTransformation = if (highlightJson) JsonHighlightTransformation else VisualTransformation.None,
            decorationBox = { inner ->
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .let { if (minHeight != null) it.heightIn(min = minHeight) else it }
                        .background(BugsyDesktopColors.SurfaceCard, RoundedCornerShape(BugsyDesktopSpacing.controlCorner))
                        .border(BugsyDesktopSpacing.controlBorder, if (isError) BugsyDesktopColors.Status4xx else BugsyDesktopColors.Border, RoundedCornerShape(BugsyDesktopSpacing.controlCorner))
                        .padding(horizontal = BugsyDesktopSpacing.md, vertical = BugsyDesktopSpacing.sm),
                ) {
                    inner()
                }
            },
            modifier = Modifier.fillMaxWidth(),
        )
        if (errorText != null) {
            Text(errorText, style = BugsyDesktopTypography.Caption, color = BugsyDesktopColors.Status4xx)
        }
    }
}

@Composable
private fun PlainField(value: String, onValueChange: (String) -> Unit) {
    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        textStyle = BugsyDesktopTypography.Code.copy(color = BugsyDesktopColors.TextSecondary),
        singleLine = true,
        cursorBrush = SolidColor(BugsyDesktopColors.Brand),
        decorationBox = { inner ->
            Box(
                Modifier
                    .fillMaxWidth()
                    .background(BugsyDesktopColors.SurfaceCard, RoundedCornerShape(BugsyDesktopSpacing.controlCorner))
                    .border(BugsyDesktopSpacing.controlBorder, BugsyDesktopColors.Border, RoundedCornerShape(BugsyDesktopSpacing.controlCorner))
                    .padding(horizontal = BugsyDesktopSpacing.sm, vertical = BugsyDesktopSpacing.sm),
            ) { inner() }
        },
        modifier = Modifier.fillMaxWidth(),
    )
}

private object JsonHighlightTransformation : VisualTransformation {
    override fun filter(text: androidx.compose.ui.text.AnnotatedString) =
        TransformedText(highlightJsonLenient(text.text), OffsetMapping.Identity)
}
