package com.bugsy.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import org.jetbrains.compose.ui.tooling.preview.Preview
import com.bugsy.ui.DebugViewModel
import com.bugsy.ui.components.BugsyTab
import com.bugsy.ui.components.BugsyTabBar
import com.bugsy.ui.components.DragHandle
import com.bugsy.ui.theme.BugsyColors
import com.bugsy.ui.theme.BugsyTheme

@Composable
fun DebugOverlay(
    viewModel: DebugViewModel,
    onDismiss: () -> Unit,
) {
    var selectedTab by remember { mutableStateOf(BugsyTab.Requests) }
    val requests by viewModel.requests.collectAsState()
    val flags by viewModel.flags.collectAsState()

    LaunchedEffect(viewModel) { viewModel.refreshFlags() }

    BugsyTheme {
        Column(
            Modifier.fillMaxWidth().background(BugsyColors.Surface),
        ) {
            DragHandle()
            BugsySheetHeader(
                requestCount = requests.size,
                onClear = viewModel::clearRequests,
                onDismiss = onDismiss,
            )
            BugsyTabBar(
                selected = selectedTab,
                requestCount = requests.size,
                onSelect = { selectedTab = it },
            )
            when (selectedTab) {
                BugsyTab.Requests -> RequestListScreen(requests = requests)
                BugsyTab.Flags -> FlagsScreen(
                    flags = flags,
                    onOverride = viewModel::overrideFlag,
                    onResetAll = viewModel::resetAllFlags,
                )
                BugsyTab.Device -> DeviceInfoScreen(info = viewModel.deviceInfo)
            }
        }
    }
}

@Preview
@Composable
private fun DebugOverlayPreview() {
    DebugOverlay(viewModel = remember { DebugViewModel() }, onDismiss = {})
}
