package com.bugsy.ui.screens

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import org.jetbrains.compose.ui.tooling.preview.Preview
import bugsy.ui.generated.resources.Res
import bugsy.ui.generated.resources.mock_form_prefilled_from
import com.bugsy.core.MockEngine
import com.bugsy.core.model.DebugRequest
import com.bugsy.core.model.MockRule
import com.bugsy.ui.DebugViewModel
import com.bugsy.ui.components.BugsyTab
import com.bugsy.ui.components.BugsyTabBar
import com.bugsy.ui.components.DragHandle
import com.bugsy.ui.theme.BugsyColors
import com.bugsy.ui.theme.BugsySpacing
import com.bugsy.ui.theme.BugsyTheme
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource
import kotlin.random.Random

private const val SCRIM_ALPHA = 0.4f
private const val ENTER_MS = 240
private const val EXIT_MS = 200
private const val DISMISS_DRAG_FRACTION = 0.25f

/**
 * Fixed-height modal (80% of the available area) anchored to the bottom, over a dimmed background. Opens by
 * sliding up from the bottom and closes by dragging down (via the handle, header or tabs), tapping the
 * background, or the X. [onDismiss] is called when the exit animation finishes.
 */
@Composable
fun DebugOverlay(
    viewModel: DebugViewModel,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    animateEntrance: Boolean = true,
    onExportHar: HarExporter? = null,
) {
    var selectedTab by remember { mutableStateOf(BugsyTab.Requests) }
    var showSettings by remember { mutableStateOf(false) }
    val requests by viewModel.requests.collectAsState()
    val flags by viewModel.flags.collectAsState()
    val mockRules by MockEngine.rulesFlow.collectAsState()
    val mockMatchLogs by MockEngine.matchLogsFlow.collectAsState()
    var mockPrefillSource by remember { mutableStateOf<DebugRequest?>(null) }
    val mockPrefillLabel = mockPrefillSource?.let {
        stringResource(Res.string.mock_form_prefilled_from, it.method, it.url.pathOnly(), it.statusCode?.toString() ?: "—")
    }

    LaunchedEffect(viewModel) { viewModel.refreshFlags() }

    BugsyTheme {
        val scope = rememberCoroutineScope()
        val offsetY = remember { Animatable(0f) }
        var sheetHeightPx by remember { mutableStateOf(0) }
        var entered by remember { mutableStateOf(!animateEntrance) }

        // Entrance: measure the modal, position it below the screen, and slide it to its final position.
        // Runs exactly once: it must not restart when the size changes by a pixel (e.g. insets arriving).
        LaunchedEffect(Unit) {
            if (!entered) {
                val height = snapshotFlow { sheetHeightPx }.first { it > 0 }
                offsetY.snapTo(height.toFloat())
                entered = true
                offsetY.animateTo(0f, tween(ENTER_MS))
            }
        }

        val dismiss: () -> Unit = {
            scope.launch {
                offsetY.animateTo(sheetHeightPx.toFloat(), tween(EXIT_MS))
                onDismiss()
            }
        }
        val translation = if (entered) offsetY.value else sheetHeightPx.toFloat().coerceAtLeast(OFFSCREEN_PX)
        val shown = if (sheetHeightPx > 0) (1f - translation / sheetHeightPx).coerceIn(0f, 1f) else 1f

        Box(
            modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = SCRIM_ALPHA * shown))
                .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { dismiss() },
            contentAlignment = Alignment.BottomCenter,
        ) {
            // Fixed height (80% of the available space) on all screens; the content scrolls within it.
            Column(
                Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(BugsySpacing.overlayHeightFraction)
                    .onSizeChanged { sheetHeightPx = it.height }
                    .graphicsLayer { translationY = translation }
                    .clip(RoundedCornerShape(topStart = BugsySpacing.sheetCorner, topEnd = BugsySpacing.sheetCorner))
                    .background(BugsyColors.Surface)
                    // Swallows taps so the modal isn't dismissed when tapping an empty area inside it.
                    .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {}
                    .navigationBarsPadding(),
            ) {
                // Dragging down (handle, header or tabs) closes the modal; releasing before the threshold snaps it back.
                Column(
                    Modifier.pointerInput(Unit) {
                        detectVerticalDragGestures(
                            onDragEnd = {
                                scope.launch {
                                    if (offsetY.value > sheetHeightPx * DISMISS_DRAG_FRACTION) dismiss()
                                    else offsetY.animateTo(0f, tween(ENTER_MS))
                                }
                            },
                            onDragCancel = { scope.launch { offsetY.animateTo(0f, tween(ENTER_MS)) } },
                        ) { change, dragAmount ->
                            change.consume()
                            scope.launch { offsetY.snapTo((offsetY.value + dragAmount).coerceAtLeast(0f)) }
                        }
                    },
                ) {
                    DragHandle()
                    BugsySheetHeader(
                        onSettings = { showSettings = !showSettings },
                        onDismiss = dismiss,
                        settingsActive = showSettings,
                    )
                    BugsyTabBar(
                        selected = selectedTab,
                        onSelect = {
                            selectedTab = it
                            showSettings = false
                        },
                    )
                }
                Box(Modifier.weight(1f).fillMaxWidth()) {
                    if (showSettings) {
                        SettingsScreen()
                        return@Box
                    }
                    when (selectedTab) {
                        BugsyTab.Requests -> RequestListScreen(
                            requests = requests,
                            onClear = viewModel::clearRequests,
                            onExportHar = onExportHar,
                            activeMockCount = mockRules.count { it.isActive },
                            onManageMocks = { selectedTab = BugsyTab.Mocks },
                            onCreateMock = { request ->
                                mockPrefillSource = request
                                selectedTab = BugsyTab.Mocks
                            },
                        )
                        BugsyTab.Mocks -> MocksScreen(
                            rules = mockRules,
                            onAddRule = MockEngine::addRule,
                            onToggleActive = MockEngine::setActive,
                            onRemoveRule = MockEngine::removeRule,
                            matchesFor = { id -> mockMatchLogs[id].orEmpty() },
                            prefillRule = remember(mockPrefillSource) { mockPrefillSource?.toMockRulePrefill() },
                            prefillSourceLabel = mockPrefillLabel,
                            onPrefillConsumed = { mockPrefillSource = null },
                            onNavigateToRequests = { selectedTab = BugsyTab.Requests },
                        )
                        BugsyTab.Flags -> FlagsScreen(
                            flags = flags,
                            onOverride = viewModel::overrideFlag,
                            onResetAll = viewModel::resetAllFlags,
                        )
                        BugsyTab.Device -> DeviceInfoScreen(info = viewModel.deviceInfo, customInfo = viewModel.customInfo)
                    }
                }
            }
        }
    }
}

private const val OFFSCREEN_PX = 100_000f

/** "https://api.bugsy.dev/v1/users/42?x=1" -> "/v1/users/42" (host and query stripped). */
private fun String.pathOnly(): String {
    val afterScheme = substringAfter("://", missingDelimiterValue = this)
    val withoutHost = afterScheme.substringAfter('/', missingDelimiterValue = "").let { if (it.isEmpty()) "/" else "/$it" }
    return withoutHost.substringBefore('?')
}

/** Pre-fills a new rule from [this] request: an exact-match pattern for its path, its method, status and body. */
private fun DebugRequest.toMockRulePrefill(): MockRule = MockRule(
    id = "mock-${Random.nextLong()}",
    urlPattern = "^${Regex.escape(url.pathOnly())}$",
    method = method,
    statusCode = statusCode ?: 200,
    responseHeaders = responseHeaders.ifEmpty { mapOf("Content-Type" to "application/json") },
    responseBody = responseBody.orEmpty(),
    isOneShot = true,
)

@Preview
@Composable
private fun DebugOverlayPreview() {
    DebugOverlay(viewModel = remember { DebugViewModel() }, onDismiss = {})
}
