package com.bugsy.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import com.bugsy.ui.theme.BugsySpacing
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
    modifier: Modifier = Modifier,
) {
    var selectedTab by remember { mutableStateOf(BugsyTab.Requests) }
    val requests by viewModel.requests.collectAsState()
    val flags by viewModel.flags.collectAsState()

    LaunchedEffect(viewModel) { viewModel.refreshFlags() }

    BugsyTheme {
        // Altura fixa (80% do espaço disponível) em todas as telas; o conteúdo rola dentro dela.
        Column(
            modifier
                .fillMaxWidth()
                .fillMaxHeight(BugsySpacing.overlayHeightFraction)
                .clip(RoundedCornerShape(topStart = BugsySpacing.sheetCorner, topEnd = BugsySpacing.sheetCorner))
                .background(BugsyColors.Surface)
                // Engole os toques para não dispensar o modal ao tocar numa área vazia dele.
                .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {}
                .navigationBarsPadding(),
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
            Box(Modifier.weight(1f).fillMaxWidth()) {
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
}

@Preview
@Composable
private fun DebugOverlayPreview() {
    DebugOverlay(viewModel = remember { DebugViewModel() }, onDismiss = {})
}
