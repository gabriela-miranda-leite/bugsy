@file:OptIn(org.jetbrains.compose.resources.ExperimentalResourceApi::class)

package com.bugsy.ui

import app.cash.paparazzi.DeviceConfig
import app.cash.paparazzi.Paparazzi
import com.android.ide.common.rendering.api.SessionParams.RenderingMode
import com.bugsy.ui.components.BugsyTab
import com.bugsy.ui.components.BugsyTabBar
import com.bugsy.ui.components.CodeBlock
import com.bugsy.ui.components.DragHandle
import com.bugsy.ui.components.MetricCard
import com.bugsy.ui.components.MethodBadge
import com.bugsy.ui.components.MockRuleRow
import com.bugsy.ui.components.RequestRow
import com.bugsy.ui.screens.BugsySheetHeader
import com.bugsy.ui.screens.DebugOverlay
import com.bugsy.ui.screens.DeviceInfoScreen
import com.bugsy.ui.screens.FlagsScreen
import com.bugsy.ui.screens.MockRuleDetailScreen
import com.bugsy.ui.screens.MockRuleFormScreen
import com.bugsy.ui.screens.MocksScreen
import com.bugsy.ui.screens.RequestDetailScreen
import com.bugsy.ui.screens.RequestListScreen
import com.bugsy.ui.screens.SettingsScreen
import com.bugsy.ui.theme.BugsyColors
import com.bugsy.ui.theme.BugsyTheme
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.height
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import java.util.Locale

// V_SCROLL needs a finite ceiling — LazyColumn throws an exception with infinite height.
private val MaxSnapshotHeight = 5_000.dp
private val OverlayContainerHeight = 800.dp

class BugsyScreenshotTest {

    @get:Rule
    val paparazzi = Paparazzi(deviceConfig = DeviceConfig.PIXEL_5, renderingMode = RenderingMode.SHRINK)

    @Before
    fun setUpComposeResourcesContext() {
        // Compose Resources' stringResource() picks the locale from the JVM's default Locale,
        // not from Paparazzi's DeviceConfig — force it to English so snapshots are deterministic
        // regardless of the machine running the tests.
        Locale.setDefault(Locale.ENGLISH)

        // Paparazzi doesn't trigger the ContentProvider that compose-resources uses to
        // initialize the Context on Android — without this, stringResource() breaks.
        // AndroidContextProvider is `internal` in Kotlin but public in bytecode, so
        // we set the Context via reflection.
        val providerClass = Class.forName("org.jetbrains.compose.resources.AndroidContextProvider")
        val companion = providerClass.getDeclaredField("Companion").apply { isAccessible = true }.get(null)
        companion.javaClass.getDeclaredMethod("setANDROID_CONTEXT", android.content.Context::class.java)
            .apply { isAccessible = true }
            .invoke(companion, paparazzi.context)
    }

    private fun snapshotOnSurface(content: @androidx.compose.runtime.Composable () -> Unit) {
        paparazzi.snapshot {
            BugsyTheme {
                Box(Modifier.fillMaxWidth().heightIn(max = MaxSnapshotHeight).background(BugsyColors.Surface)) {
                    content()
                }
            }
        }
    }

    @Test
    fun methodBadges() {
        snapshotOnSurface {
            Column(Modifier.padding(16.dp)) {
                listOf("GET", "POST", "PUT", "DELETE", "PATCH", "HEAD").forEach { method ->
                    MethodBadge(method, modifier = Modifier.padding(bottom = 8.dp))
                }
            }
        }
    }

    @Test
    fun requestRow() {
        snapshotOnSurface {
            Column { PreviewFixtures.requests.forEach { RequestRow(request = it, onClick = {}) } }
        }
    }

    @Test
    fun requestRowSlow() {
        snapshotOnSurface {
            RequestRow(request = PreviewFixtures.getRequestSlow, onClick = {})
        }
    }

    @Test
    fun requestRowError() {
        snapshotOnSurface {
            Column {
                RequestRow(request = PreviewFixtures.postRequestUnauthorized, onClick = {})
                RequestRow(request = PreviewFixtures.postRequestError, onClick = {})
            }
        }
    }

    @Test
    fun bugsyTabBar() {
        snapshotOnSurface {
            BugsyTabBar(selected = BugsyTab.Requests, onSelect = {})
        }
    }

    @Test
    fun mockRuleRow() {
        snapshotOnSurface {
            Column { PreviewFixtures.mockRules.forEach { MockRuleRow(rule = it, onClick = {}, onToggleActive = {}, onDelete = {}) } }
        }
    }

    @Test
    fun mocksScreenEmpty() {
        snapshotOnSurface {
            MocksScreen(rules = emptyList(), onAddRule = {}, onToggleActive = { _, _ -> }, onRemoveRule = {})
        }
    }

    @Test
    fun mocksScreenWithData() {
        snapshotOnSurface {
            MocksScreen(rules = PreviewFixtures.mockRules, onAddRule = {}, onToggleActive = { _, _ -> }, onRemoveRule = {})
        }
    }

    @Test
    fun mockRuleDetailScreen() {
        snapshotOnSurface {
            MockRuleDetailScreen(
                rule = PreviewFixtures.mockRules.first(),
                matches = emptyList(),
                onBack = {},
                onToggleActive = {},
                onEdit = {},
                onDelete = {},
            )
        }
    }

    @Test
    fun mockRuleFormScreenNew() {
        snapshotOnSurface {
            MockRuleFormScreen(initial = null, onBack = {}, onSave = {})
        }
    }

    @Test
    fun mockRuleFormScreenPrefilled() {
        snapshotOnSurface {
            MockRuleFormScreen(
                initial = PreviewFixtures.mockRules[1],
                prefillSourceLabel = "Pre-filled from POST /api/v1/auth/login · 401",
                onBack = {},
                onSave = {},
            )
        }
    }

    @Test
    fun metricCard() {
        snapshotOnSurface {
            MetricCard("Status", "200", valueColor = BugsyColors.Status2xx, modifier = Modifier.padding(16.dp))
        }
    }

    @Test
    fun codeBlock() {
        snapshotOnSurface {
            Box(Modifier.padding(16.dp)) {
                CodeBlock(text = "curl https://api.bugsy.dev/v1/users/42", onCopy = {})
            }
        }
    }

    @Test
    fun bugsySheetHeader() {
        snapshotOnSurface {
            BugsySheetHeader(onSettings = {}, onDismiss = {})
        }
    }

    @Test
    fun requestListScreenWithData() {
        snapshotOnSurface {
            RequestListScreen(requests = PreviewFixtures.requests)
        }
    }

    @Test
    fun requestListScreenWithExport() {
        snapshotOnSurface {
            RequestListScreen(requests = PreviewFixtures.requests, onExportHar = {})
        }
    }

    @Test
    fun requestListScreenWithMocksBanner() {
        snapshotOnSurface {
            RequestListScreen(
                requests = listOf(PreviewFixtures.getRequestMocked, PreviewFixtures.postRequestMockedOnce) + PreviewFixtures.requests,
                activeMockCount = 3,
                onCreateMock = {},
            )
        }
    }

    @Test
    fun requestListScreenEmpty() {
        snapshotOnSurface {
            RequestListScreen(requests = emptyList())
        }
    }

    @Test
    fun requestListScreenLoading() {
        snapshotOnSurface {
            RequestListScreen(requests = emptyList(), isLoading = true)
        }
    }

    @Test
    fun requestDetailScreen() {
        snapshotOnSurface {
            RequestDetailScreen(request = PreviewFixtures.postRequestError, onBack = {})
        }
    }

    @Test
    fun requestDetailScreenWithSecrets() {
        snapshotOnSurface {
            RequestDetailScreen(request = PreviewFixtures.postRequestWithSecrets, onBack = {})
        }
    }

    @Test
    fun requestDetailScreenRevealed() {
        snapshotOnSurface {
            RequestDetailScreen(request = PreviewFixtures.postRequestWithSecrets, onBack = {}, revealed = true)
        }
    }

    @Test
    fun requestDetailScreenSlow() {
        snapshotOnSurface {
            RequestDetailScreen(request = PreviewFixtures.getRequestSlow, onBack = {})
        }
    }

    @Test
    fun requestDetailScreenWithCreateMock() {
        snapshotOnSurface {
            RequestDetailScreen(request = PreviewFixtures.postRequestError, onBack = {}, onCreateMock = {})
        }
    }

    @Test
    fun requestDetailScreenLoading() {
        // The back-button header stays, only the body below it shows the loading state.
        snapshotOnSurface {
            RequestDetailScreen(request = PreviewFixtures.postRequestError, onBack = {}, isLoading = true)
        }
    }

    @Test
    fun settingsScreen() {
        snapshotOnSurface { SettingsScreen() }
    }

    @Test
    fun debugOverlay() {
        paparazzi.snapshot {
            // The overlay takes up 80% of the available height: the container fixes that height (~800dp screen).
            Box(Modifier.fillMaxWidth().height(OverlayContainerHeight)) {
                DebugOverlay(viewModel = DebugViewModel(), onDismiss = {}, animateEntrance = false)
            }
        }
    }

    @Test
    fun debugOverlayRequestDetail() {
        // Reproduces the DebugOverlay composition (drag handle + header + fixed tabs) with
        // RequestDetailScreen as the body, as it looks after tapping a row in the list.
        paparazzi.snapshot {
            BugsyTheme {
                Column(Modifier.fillMaxWidth().heightIn(max = MaxSnapshotHeight).background(BugsyColors.Surface)) {
                    DragHandle()
                    BugsySheetHeader(onSettings = {}, onDismiss = {})
                    BugsyTabBar(selected = BugsyTab.Requests, onSelect = {})
                    RequestDetailScreen(request = PreviewFixtures.postRequestError, onBack = {})
                }
            }
        }
    }

    @Test
    fun debugOverlayMocksEmpty() {
        // Reproduces the DebugOverlay composition (drag handle + header + fixed tabs) with
        // MocksScreen (empty state) as the body — in isolation, the screen looks cut off at the top.
        paparazzi.snapshot {
            BugsyTheme {
                Column(Modifier.fillMaxWidth().heightIn(max = MaxSnapshotHeight).background(BugsyColors.Surface)) {
                    DragHandle()
                    BugsySheetHeader(onSettings = {}, onDismiss = {})
                    BugsyTabBar(selected = BugsyTab.Mocks, onSelect = {})
                    MocksScreen(rules = emptyList(), onAddRule = {}, onToggleActive = { _, _ -> }, onRemoveRule = {})
                }
            }
        }
    }

    @Test
    fun debugOverlayMocksWithData() {
        paparazzi.snapshot {
            BugsyTheme {
                Column(Modifier.fillMaxWidth().heightIn(max = MaxSnapshotHeight).background(BugsyColors.Surface)) {
                    DragHandle()
                    BugsySheetHeader(onSettings = {}, onDismiss = {})
                    BugsyTabBar(selected = BugsyTab.Mocks, onSelect = {})
                    MocksScreen(rules = PreviewFixtures.mockRules, onAddRule = {}, onToggleActive = { _, _ -> }, onRemoveRule = {})
                }
            }
        }
    }

    @Test
    fun debugOverlayDeviceInfo() {
        // Reproduces the DebugOverlay composition (drag handle + header + fixed tabs) with
        // DeviceInfoScreen as the body — in isolation, the screen looks cut off at the top.
        paparazzi.snapshot {
            BugsyTheme {
                Column(Modifier.fillMaxWidth().heightIn(max = MaxSnapshotHeight).background(BugsyColors.Surface)) {
                    DragHandle()
                    BugsySheetHeader(onSettings = {}, onDismiss = {})
                    BugsyTabBar(selected = BugsyTab.Device, onSelect = {})
                    DeviceInfoScreen(info = PreviewFixtures.deviceInfo)
                }
            }
        }
    }

    @Test
    fun debugOverlayDeviceInfoLoading() {
        paparazzi.snapshot {
            BugsyTheme {
                Column(Modifier.fillMaxWidth().heightIn(max = MaxSnapshotHeight).background(BugsyColors.Surface)) {
                    DragHandle()
                    BugsySheetHeader(onSettings = {}, onDismiss = {})
                    BugsyTabBar(selected = BugsyTab.Device, onSelect = {})
                    DeviceInfoScreen(info = null)
                }
            }
        }
    }

    @Test
    fun debugOverlayFlagsEmpty() {
        // Reproduces the DebugOverlay composition (drag handle + header + fixed tabs) with
        // FlagsScreen (empty state) as the body — in isolation, the screen looks cut off at the top.
        paparazzi.snapshot {
            BugsyTheme {
                Column(Modifier.fillMaxWidth().heightIn(max = MaxSnapshotHeight).background(BugsyColors.Surface)) {
                    DragHandle()
                    BugsySheetHeader(onSettings = {}, onDismiss = {})
                    BugsyTabBar(selected = BugsyTab.Flags, onSelect = {})
                    FlagsScreen(flags = emptyList(), onOverride = { _, _ -> }, onResetAll = {})
                }
            }
        }
    }

    @Test
    fun debugOverlayFlagsWithData() {
        paparazzi.snapshot {
            BugsyTheme {
                Column(Modifier.fillMaxWidth().heightIn(max = MaxSnapshotHeight).background(BugsyColors.Surface)) {
                    DragHandle()
                    BugsySheetHeader(onSettings = {}, onDismiss = {})
                    BugsyTabBar(selected = BugsyTab.Flags, onSelect = {})
                    FlagsScreen(flags = PreviewFixtures.flags, onOverride = { _, _ -> }, onResetAll = {})
                }
            }
        }
    }

    @Test
    fun debugOverlayFlagsLoading() {
        paparazzi.snapshot {
            BugsyTheme {
                Column(Modifier.fillMaxWidth().heightIn(max = MaxSnapshotHeight).background(BugsyColors.Surface)) {
                    DragHandle()
                    BugsySheetHeader(onSettings = {}, onDismiss = {})
                    BugsyTabBar(selected = BugsyTab.Flags, onSelect = {})
                    FlagsScreen(flags = emptyList(), onOverride = { _, _ -> }, onResetAll = {}, isLoading = true)
                }
            }
        }
    }
}
