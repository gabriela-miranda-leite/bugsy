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
import com.bugsy.ui.DebugViewModel
import com.bugsy.ui.components.BugsyTab
import com.bugsy.ui.components.BugsyTabBar
import com.bugsy.ui.components.DragHandle
import com.bugsy.ui.theme.BugsyColors
import com.bugsy.ui.theme.BugsySpacing
import com.bugsy.ui.theme.BugsyTheme
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

private const val SCRIM_ALPHA = 0.4f
private const val ENTER_MS = 240
private const val EXIT_MS = 200
private const val DISMISS_DRAG_FRACTION = 0.25f

/**
 * Modal de altura fixa (80% da área disponível) ancorado embaixo, sobre um fundo escurecido. Abre deslizando
 * de baixo e fecha arrastando para baixo (pelo handle, cabeçalho ou abas), tocando no fundo ou no X.
 * [onDismiss] é chamado quando a animação de saída termina.
 */
@Composable
fun DebugOverlay(
    viewModel: DebugViewModel,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    animateEntrance: Boolean = true,
) {
    var selectedTab by remember { mutableStateOf(BugsyTab.Requests) }
    var showSettings by remember { mutableStateOf(false) }
    val requests by viewModel.requests.collectAsState()
    val flags by viewModel.flags.collectAsState()

    LaunchedEffect(viewModel) { viewModel.refreshFlags() }

    BugsyTheme {
        val scope = rememberCoroutineScope()
        val offsetY = remember { Animatable(0f) }
        var sheetHeightPx by remember { mutableStateOf(0) }
        var entered by remember { mutableStateOf(!animateEntrance) }

        // Entrada: mede o modal, posiciona-o abaixo da tela e desliza até a posição final.
        // Roda uma única vez: não pode reiniciar quando o tamanho muda um pixel (ex.: chegada dos insets).
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
            // Altura fixa (80% do espaço disponível) em todas as telas; o conteúdo rola dentro dela.
            Column(
                Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(BugsySpacing.overlayHeightFraction)
                    .onSizeChanged { sheetHeightPx = it.height }
                    .graphicsLayer { translationY = translation }
                    .clip(RoundedCornerShape(topStart = BugsySpacing.sheetCorner, topEnd = BugsySpacing.sheetCorner))
                    .background(BugsyColors.Surface)
                    // Engole os toques para não dispensar o modal ao tocar numa área vazia dele.
                    .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {}
                    .navigationBarsPadding(),
            ) {
                // Arrastar para baixo (handle, cabeçalho ou abas) fecha o modal; soltar antes do limiar o devolve.
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
                        BugsyTab.Requests -> RequestListScreen(requests = requests, onClear = viewModel::clearRequests)
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

@Preview
@Composable
private fun DebugOverlayPreview() {
    DebugOverlay(viewModel = remember { DebugViewModel() }, onDismiss = {})
}
