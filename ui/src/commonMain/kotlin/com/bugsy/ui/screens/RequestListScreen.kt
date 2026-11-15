package com.bugsy.ui.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.foundation.background
import androidx.compose.ui.Modifier
import org.jetbrains.compose.ui.tooling.preview.Preview
import bugsy.ui.generated.resources.Res
import bugsy.ui.generated.resources.empty_no_requests_filtered
import bugsy.ui.generated.resources.empty_no_requests_subtitle
import bugsy.ui.generated.resources.empty_no_requests_title
import bugsy.ui.generated.resources.loading_requests_title
import bugsy.ui.generated.resources.loading_subtitle_default
import com.bugsy.core.model.DebugRequest
import com.bugsy.ui.PreviewFixtures
import com.bugsy.ui.components.BugsySearchBar
import com.bugsy.ui.components.EmptyState
import com.bugsy.ui.components.LoadingState
import com.bugsy.ui.components.RequestRow
import com.bugsy.ui.theme.BugsyColors
import com.bugsy.ui.theme.BugsySpacing
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.outlined.IosShare
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import bugsy.ui.generated.resources.action_cancel_selection
import bugsy.ui.generated.resources.action_export_har_count
import com.bugsy.ui.components.HarExportDialog
import com.bugsy.ui.theme.BugsyTypography
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import bugsy.ui.generated.resources.cd_clear_requests
import com.bugsy.ui.settings.BugsySettings
import com.bugsy.ui.theme.BugsyTheme
import org.jetbrains.compose.resources.stringResource

fun filterRequests(requests: List<DebugRequest>, filter: String): List<DebugRequest> =
    if (filter.isBlank()) {
        requests
    } else {
        requests.filter {
            it.url.contains(filter, ignoreCase = true) || it.method.contains(filter, ignoreCase = true)
        }
    }

/** Shares [requests] as a HAR file; the flag says whether to write real values instead of the masked ones. */
typealias HarExporter = (requests: List<DebugRequest>, revealSecrets: Boolean) -> Unit

/**
 * @param onExportHar when non-null, long press on a row starts a multi-selection with an "Export HAR (N)" bar, and
 * the detail screen gets an "Export HAR" button. `null` (e.g. on iOS) hides all of it.
 */
@Composable
fun RequestListScreen(
    requests: List<DebugRequest>,
    onClear: () -> Unit = {},
    isLoading: Boolean = false,
    onExportHar: HarExporter? = null,
    initialSelection: Set<String>? = null,
) {
    if (isLoading) {
        LoadingState(
            title = stringResource(Res.string.loading_requests_title),
            subtitle = stringResource(Res.string.loading_subtitle_default),
        )
        return
    }

    var filter by remember { mutableStateOf("") }
    var selected by remember { mutableStateOf<DebugRequest?>(null) }
    // The show/hide choice for sensitive data applies to all requests while the overlay is open.
    // The starting point (and any change to it) comes from Settings.
    val revealDefault by BugsySettings.revealSensitiveByDefault.collectAsState()
    var revealed by remember { mutableStateOf(revealDefault) }
    LaunchedEffect(revealDefault) { revealed = revealDefault }

    // Ids of the rows picked for export; `null` means not in selection mode.
    var selection by remember { mutableStateOf(initialSelection) }
    var pendingExport by remember { mutableStateOf<List<DebugRequest>?>(null) }

    fun requestExport(toExport: List<DebugRequest>) {
        val exporter = onExportHar ?: return
        // Only ask when real values would actually be available to write.
        if (toExport.any { it.secrets != null }) pendingExport = toExport else exporter(toExport, false)
    }

    selected?.let { req ->
        RequestDetailScreen(
            request = req,
            onBack = { selected = null },
            revealed = revealed,
            onRevealedChange = { revealed = it },
            onExportHar = onExportHar?.let { { requestExport(listOf(req)) } },
        )
        pendingExport?.let { ExportDialog(it, onExportHar, onDone = { pendingExport = null }) }
        return
    }

    val filtered = remember(requests, filter) { filterRequests(requests, filter) }

    Column {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(BugsySpacing.sm),
            modifier = Modifier.padding(horizontal = BugsySpacing.lg, vertical = BugsySpacing.searchBarVerticalMargin),
        ) {
            BugsySearchBar(value = filter, onValueChange = { filter = it }, modifier = Modifier.weight(1f))
            IconButton(
                onClick = onClear,
                enabled = requests.isNotEmpty(),
                modifier = Modifier
                    .size(BugsySpacing.controlHeight)
                    .border(BugsySpacing.strokeBorder, BugsyColors.Border, RoundedCornerShape(BugsySpacing.cornerCard))
                    .background(BugsyColors.SurfaceCard, RoundedCornerShape(BugsySpacing.cornerCard)),
            ) {
                Icon(
                    Icons.Outlined.DeleteOutline,
                    contentDescription = stringResource(Res.string.cd_clear_requests),
                    tint = if (requests.isNotEmpty()) BugsyColors.Status4xx else BugsyColors.TextDisabled,
                    modifier = Modifier.size(BugsySpacing.iconSizeMedium),
                )
            }
        }
        if (filtered.isEmpty()) {
            if (filter.isNotBlank()) {
                EmptyState(title = stringResource(Res.string.empty_no_requests_filtered, filter))
            } else {
                EmptyState(
                    title = stringResource(Res.string.empty_no_requests_title),
                    subtitle = stringResource(Res.string.empty_no_requests_subtitle),
                )
            }
        } else {
            LazyColumn(Modifier.fillMaxWidth()) {
                items(filtered, key = { it.id }) { req ->
                    val current = selection
                    RequestRow(
                        request = req,
                        onClick = {
                            if (current == null) selected = req else selection = current.toggle(req.id)
                        },
                        selectionMode = current != null,
                        selected = current?.contains(req.id) == true,
                        onLongClick = if (onExportHar != null && current == null) ({ selection = setOf(req.id) }) else null,
                    )
                }
            }
        }
        selection?.let { ids ->
            // Ids can disappear while selecting (the list is live, or "clear" ran): only export what still exists.
            val chosen = requests.filter { it.id in ids }
            SelectionBar(
                count = chosen.size,
                onExport = { requestExport(chosen) },
                onCancel = { selection = null },
            )
        }
    }
    pendingExport?.let {
        ExportDialog(it, onExportHar, onDone = { pendingExport = null; selection = null })
    }
}

private fun Set<String>.toggle(id: String): Set<String> = if (id in this) this - id else this + id

@Composable
private fun ExportDialog(requests: List<DebugRequest>, exporter: HarExporter?, onDone: () -> Unit) {
    HarExportDialog(
        onExportMasked = { exporter?.invoke(requests, false); onDone() },
        onExportReal = { exporter?.invoke(requests, true); onDone() },
        onDismiss = onDone,
    )
}

@Composable
private fun SelectionBar(count: Int, onExport: () -> Unit, onCancel: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(BugsySpacing.sm),
        modifier = Modifier
            .fillMaxWidth()
            .background(BugsyColors.SurfaceCard)
            .padding(horizontal = BugsySpacing.lg, vertical = BugsySpacing.md),
    ) {
        TextButton(onClick = onCancel) {
            Text(stringResource(Res.string.action_cancel_selection), style = BugsyTypography.Body, color = BugsyColors.TextDisabled)
        }
        Spacer(Modifier.weight(1f))
        Button(
            onClick = onExport,
            enabled = count > 0,
            colors = ButtonDefaults.buttonColors(
                containerColor = BugsyColors.Brand,
                contentColor = BugsyColors.Surface,
                disabledContainerColor = BugsyColors.SurfaceElevated,
                disabledContentColor = BugsyColors.TextDisabled,
            ),
            shape = RoundedCornerShape(BugsySpacing.cornerButtonSmall),
        ) {
            Icon(Icons.Outlined.IosShare, contentDescription = null, modifier = Modifier.size(BugsySpacing.iconSizeSmall))
            Spacer(Modifier.width(BugsySpacing.codeBlockButtonIconSpacing))
            Text(stringResource(Res.string.action_export_har_count, count), style = BugsyTypography.Body)
        }
    }
}

@Preview
@Composable
private fun RequestListScreenPreview() {
    BugsyTheme {
        Box(Modifier.background(BugsyColors.Surface)) {
            RequestListScreen(requests = PreviewFixtures.requests)
        }
    }
}

@Preview
@Composable
private fun RequestListScreenEmptyPreview() {
    BugsyTheme {
        Box(Modifier.background(BugsyColors.Surface)) {
            RequestListScreen(requests = emptyList())
        }
    }
}

@Preview
@Composable
private fun RequestListScreenLoadingPreview() {
    BugsyTheme {
        Box(Modifier.background(BugsyColors.Surface)) {
            RequestListScreen(requests = emptyList(), isLoading = true)
        }
    }
}
