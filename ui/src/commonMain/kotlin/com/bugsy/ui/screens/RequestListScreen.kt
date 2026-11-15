package com.bugsy.ui.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.defaultMinSize
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
import bugsy.ui.generated.resources.filter_all
import bugsy.ui.generated.resources.filter_errors
import bugsy.ui.generated.resources.filter_mocked
import bugsy.ui.generated.resources.filter_slow
import bugsy.ui.generated.resources.loading_requests_title
import bugsy.ui.generated.resources.loading_subtitle_default
import bugsy.ui.generated.resources.mocks_banner_active
import com.bugsy.core.DebugRepository
import com.bugsy.core.MockMetadata
import com.bugsy.core.model.DebugRequest
import com.bugsy.ui.PreviewFixtures
import com.bugsy.ui.components.BugsySearchBar
import com.bugsy.ui.components.EmptyState
import com.bugsy.ui.components.LoadingState
import com.bugsy.ui.components.RequestRow
import com.bugsy.ui.theme.BugsyColors
import com.bugsy.ui.theme.BugsySpacing
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.outlined.Download
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.text.font.FontWeight
import bugsy.ui.generated.resources.action_export_har
import bugsy.ui.generated.resources.action_manage
import bugsy.ui.generated.resources.action_share
import bugsy.ui.generated.resources.cd_clear_requests
import bugsy.ui.generated.resources.toast_har_exported_subtitle
import bugsy.ui.generated.resources.toast_har_exported_title
import com.bugsy.ui.components.BugsyToastAction
import com.bugsy.ui.components.BugsyToastData
import com.bugsy.ui.components.BugsyToastHost
import com.bugsy.ui.components.BugsyToastType
import com.bugsy.ui.settings.BugsySettings
import com.bugsy.ui.theme.BugsyTheme
import com.bugsy.ui.theme.BugsyTypography
import org.jetbrains.compose.resources.stringResource

enum class RequestCategoryFilter(val labelRes: org.jetbrains.compose.resources.StringResource) {
    All(Res.string.filter_all),
    Errors(Res.string.filter_errors),
    Slow(Res.string.filter_slow),
    Mocked(Res.string.filter_mocked),
}

private fun DebugRequest.matchesCategory(category: RequestCategoryFilter): Boolean = when (category) {
    RequestCategoryFilter.All -> true
    RequestCategoryFilter.Errors -> (statusCode ?: 0) >= 400
    RequestCategoryFilter.Slow -> {
        val threshold = DebugRepository.slowRequestThresholdMs
        threshold > 0 && durationMs >= threshold
    }
    RequestCategoryFilter.Mocked -> metadata[MockMetadata.KEY_MOCKED] == MockMetadata.VALUE_TRUE
}

fun filterRequests(
    requests: List<DebugRequest>,
    filter: String,
    category: RequestCategoryFilter = RequestCategoryFilter.All,
): List<DebugRequest> = requests
    .filter { it.matchesCategory(category) }
    .filter { filter.isBlank() || it.url.contains(filter, ignoreCase = true) || it.method.contains(filter, ignoreCase = true) }

/** Shares [requests] as a HAR file with their real values. */
typealias HarExporter = (requests: List<DebugRequest>) -> Unit

/**
 * @param onExportHar when non-null, shows an "Export HAR" button in the top bar. `null` (e.g. on iOS) hides it.
 * @param activeMockCount shows an "N mocks active · intercepting" banner (with a "Manage" link to [onManageMocks])
 * when greater than zero.
 * @param onCreateMock when non-null, shows a "Create mock from this response" button on the detail screen.
 */
@Composable
fun RequestListScreen(
    requests: List<DebugRequest>,
    onClear: () -> Unit = {},
    isLoading: Boolean = false,
    onExportHar: HarExporter? = null,
    activeMockCount: Int = 0,
    onManageMocks: () -> Unit = {},
    onCreateMock: ((DebugRequest) -> Unit)? = null,
) {
    if (isLoading) {
        LoadingState(
            title = stringResource(Res.string.loading_requests_title),
            subtitle = stringResource(Res.string.loading_subtitle_default),
        )
        return
    }

    var filter by remember { mutableStateOf("") }
    var category by remember { mutableStateOf(RequestCategoryFilter.All) }
    var selected by remember { mutableStateOf<DebugRequest?>(null) }
    // The show/hide choice for sensitive data applies to all requests while the overlay is open.
    // The starting point (and any change to it) comes from Settings.
    val revealDefault by BugsySettings.revealSensitiveByDefault.collectAsState()
    var revealed by remember { mutableStateOf(revealDefault) }
    LaunchedEffect(revealDefault) { revealed = revealDefault }

    selected?.let { req ->
        RequestDetailScreen(
            request = req,
            onBack = { selected = null },
            revealed = revealed,
            onRevealedChange = { revealed = it },
            onCreateMock = onCreateMock?.let { { it(req) } },
        )
        return
    }

    val filtered = remember(requests, filter, category) { filterRequests(requests, filter, category) }

    Column {
        if (activeMockCount > 0) {
            MocksActiveBanner(count = activeMockCount, onManage = onManageMocks)
        }
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(BugsySpacing.sm),
            modifier = Modifier.padding(horizontal = BugsySpacing.lg, vertical = BugsySpacing.searchBarVerticalMargin),
        ) {
            BugsySearchBar(value = filter, onValueChange = { filter = it }, modifier = Modifier.weight(1f))
            if (onExportHar != null) {
                val toastTitle = stringResource(Res.string.toast_har_exported_title)
                val toastSubtitle = stringResource(Res.string.toast_har_exported_subtitle, requests.size)
                val shareActionLabel = stringResource(Res.string.action_share)
                IconButton(
                    onClick = {
                        onExportHar(requests)
                        BugsyToastHost.show(
                            BugsyToastData(
                                type = BugsyToastType.Success,
                                title = toastTitle,
                                subtitle = toastSubtitle,
                                action = BugsyToastAction(shareActionLabel) { onExportHar(requests) },
                            ),
                        )
                    },
                    enabled = requests.isNotEmpty(),
                    modifier = Modifier
                        .size(BugsySpacing.controlHeight)
                        .border(BugsySpacing.strokeBorder, BugsyColors.Border, RoundedCornerShape(BugsySpacing.cornerCard))
                        .background(BugsyColors.SurfaceCard, RoundedCornerShape(BugsySpacing.cornerCard)),
                ) {
                    Icon(
                        Icons.Outlined.Download,
                        contentDescription = stringResource(Res.string.action_export_har),
                        tint = if (requests.isNotEmpty()) BugsyColors.Brand else BugsyColors.TextDisabled,
                        modifier = Modifier.size(BugsySpacing.iconSizeMedium),
                    )
                }
            }
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
        Row(
            horizontalArrangement = Arrangement.spacedBy(BugsySpacing.sm),
            modifier = Modifier.padding(horizontal = BugsySpacing.lg, vertical = BugsySpacing.sm),
        ) {
            RequestCategoryFilter.entries.forEach { entry ->
                FilterChip(label = stringResource(entry.labelRes), selected = category == entry, onClick = { category = entry })
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
                    RequestRow(request = req, onClick = { selected = req })
                }
            }
        }
    }
}

@Composable
private fun MocksActiveBanner(count: Int, onManage: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = Modifier
            .fillMaxWidth()
            .background(BugsyColors.Brand.copy(alpha = 0.14f))
            .padding(horizontal = BugsySpacing.lg, vertical = BugsySpacing.sm),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(BugsySpacing.xs)) {
            Box(Modifier.size(BugsySpacing.mockActiveDotSize).background(BugsyColors.Brand, androidx.compose.foundation.shape.CircleShape))
            Text(
                stringResource(Res.string.mocks_banner_active, count),
                style = BugsyTypography.Caption.copy(fontWeight = FontWeight.Medium),
                color = BugsyColors.Brand,
            )
        }
        Text(
            stringResource(Res.string.action_manage),
            style = BugsyTypography.Caption.copy(fontWeight = FontWeight.Medium),
            color = BugsyColors.Brand,
            modifier = Modifier.clickable(onClick = onManage),
        )
    }
}

@Composable
private fun FilterChip(label: String, selected: Boolean, onClick: () -> Unit) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .defaultMinSize(minHeight = BugsySpacing.filterChipMinHeight)
            .border(
                BugsySpacing.strokeBorder,
                if (selected) BugsyColors.Brand else BugsyColors.Border,
                RoundedCornerShape(BugsySpacing.cornerButtonSmall),
            )
            .background(if (selected) BugsyColors.Brand else BugsyColors.SurfaceCard, RoundedCornerShape(BugsySpacing.cornerButtonSmall))
            .clickable(onClick = onClick)
            .padding(horizontal = BugsySpacing.filterChipPaddingHorizontal),
    ) {
        Text(
            label,
            style = BugsyTypography.Caption.copy(fontWeight = FontWeight.Medium),
            color = if (selected) BugsyColors.Surface else BugsyColors.TextDisabled,
        )
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
private fun RequestListScreenWithExportPreview() {
    BugsyTheme {
        Box(Modifier.background(BugsyColors.Surface)) {
            RequestListScreen(requests = PreviewFixtures.requests, onExportHar = {})
        }
    }
}

@Preview
@Composable
private fun RequestListScreenWithMocksBannerPreview() {
    BugsyTheme {
        Box(Modifier.background(BugsyColors.Surface)) {
            RequestListScreen(
                requests = listOf(PreviewFixtures.getRequestMocked, PreviewFixtures.postRequestMockedOnce) + PreviewFixtures.requests,
                activeMockCount = 3,
                onCreateMock = {},
            )
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
