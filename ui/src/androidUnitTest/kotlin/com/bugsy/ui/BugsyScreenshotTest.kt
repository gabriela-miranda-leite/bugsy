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
import com.bugsy.ui.components.RequestRow
import com.bugsy.ui.screens.BugsySheetHeader
import com.bugsy.ui.screens.DebugOverlay
import com.bugsy.ui.screens.DeviceInfoScreen
import com.bugsy.ui.screens.FlagsScreen
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

// V_SCROLL precisa de um teto finito — LazyColumn lança exceção com altura infinita.
private val MaxSnapshotHeight = 5_000.dp
private val OverlayContainerHeight = 800.dp

class BugsyScreenshotTest {

    @get:Rule
    val paparazzi = Paparazzi(deviceConfig = DeviceConfig.PIXEL_5, renderingMode = RenderingMode.SHRINK)

    @Before
    fun setUpComposeResourcesContext() {
        // Paparazzi não dispara o ContentProvider que o compose-resources usa pra
        // inicializar o Context no Android — sem isso, stringResource() quebra.
        // AndroidContextProvider é `internal` em Kotlin mas público no bytecode, então
        // setamos o Context via reflection.
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
    fun bugsyTabBar() {
        snapshotOnSurface {
            BugsyTabBar(selected = BugsyTab.Requests, onSelect = {})
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
    fun requestListScreenEmpty() {
        snapshotOnSurface {
            RequestListScreen(requests = emptyList())
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
    fun settingsScreen() {
        snapshotOnSurface { SettingsScreen() }
    }

    @Test
    fun debugOverlay() {
        paparazzi.snapshot {
            // O overlay ocupa 80% da altura disponível: o container fixa essa altura (tela de ~800dp).
            Box(Modifier.fillMaxWidth().height(OverlayContainerHeight)) {
                DebugOverlay(viewModel = DebugViewModel(), onDismiss = {}, animateEntrance = false)
            }
        }
    }

    @Test
    fun debugOverlayRequestDetail() {
        // Reproduz a composição do DebugOverlay (drag handle + header + tabs fixos) com o
        // RequestDetailScreen no corpo, como fica depois de tocar numa linha da lista.
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
    fun debugOverlayDeviceInfo() {
        // Reproduz a composição do DebugOverlay (drag handle + header + tabs fixos) com o
        // DeviceInfoScreen no corpo — isolado, a tela fica parecendo cortada no topo.
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
    fun debugOverlayFlagsEmpty() {
        // Reproduz a composição do DebugOverlay (drag handle + header + tabs fixos) com o
        // FlagsScreen (empty state) no corpo — isolado, a tela fica parecendo cortada no topo.
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
}
