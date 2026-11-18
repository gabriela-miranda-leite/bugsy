@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.bugsy.desktop.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.bugsy.desktop.ConnectionState
import com.bugsy.desktop.DesktopTab
import com.bugsy.desktop.DesktopViewModel
import com.bugsy.desktop.ui.theme.BugsyDesktopColors
import com.bugsy.desktop.ui.theme.BugsyDesktopSpacing
import com.bugsy.desktop.ui.theme.BugsyDesktopTypography

@Composable
fun MainLayout(viewModel: DesktopViewModel) {
    val connectionState by viewModel.connectionState.collectAsState()

    if (connectionState == ConnectionState.DISCONNECTED) {
        val pairingCode by viewModel.pairingCode.collectAsState()
        val qrText by viewModel.pairingQrText.collectAsState()
        val devices by viewModel.devices.collectAsState()
        val isScanning by viewModel.isScanningDevices.collectAsState()
        val pendingDevice by viewModel.pendingDeviceRequest.collectAsState()
        LaunchedEffect(Unit) { viewModel.refreshDevices() }
        ConnectionScreen(
            pairingCode = pairingCode,
            qrText = qrText,
            isPairingAvailable = viewModel.isPairingAvailable,
            devices = devices,
            isScanning = isScanning,
            pendingDevice = pendingDevice,
            onRefreshDevices = viewModel::refreshDevices,
            onConnectDevice = viewModel::connectToDevice,
            onConnect = viewModel::connect,
        )
        return
    }

    val requests by viewModel.requests.collectAsState()
    val selectedRequest by viewModel.selectedRequest.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val quickFilter by viewModel.quickFilter.collectAsState()
    val selectedTab by viewModel.selectedTab.collectAsState()
    val mockRules by viewModel.mockRules.collectAsState()
    val editingMockRule by viewModel.editingMockRule.collectAsState()
    val isMockFormOpen by viewModel.isMockFormOpen.collectAsState()
    val mockPrefillSourceLabel by viewModel.mockPrefillSourceLabel.collectAsState()
    val flags by viewModel.flags.collectAsState()
    val flagSearchQuery by viewModel.flagSearchQuery.collectAsState()
    val selectedFlag by viewModel.selectedFlag.collectAsState()
    val deviceInfo by viewModel.deviceInfo.collectAsState()
    val connectedAtEpochMs by viewModel.connectedAtEpochMs.collectAsState()

    Box(Modifier.fillMaxSize()) {
    Row(Modifier.fillMaxSize().background(BugsyDesktopColors.Background)) {
        Sidebar(selectedTab = selectedTab, onSelectTab = viewModel::selectTab)
        VerticalDivider(modifier = Modifier.width(BugsyDesktopSpacing.dividerThickness), color = BugsyDesktopColors.BorderHeader)

        Scaffold(
            topBar = { ConnectionToolbar(connectionState, onDisconnect = viewModel::disconnect) },
            bottomBar = { StatusBar(connectionState, requests.size) },
        ) { padding ->
            Column(Modifier.padding(padding).fillMaxSize()) {
                when (selectedTab) {
                    DesktopTab.REQUESTS -> Row(Modifier.weight(1f).fillMaxWidth()) {
                        RequestListScreen(
                            requests = requests,
                            searchQuery = searchQuery,
                            onSearchQueryChange = viewModel::setSearchQuery,
                            quickFilter = quickFilter,
                            onQuickFilterChange = viewModel::setQuickFilter,
                            selectedRequestId = selectedRequest?.id,
                            onSelectRequest = viewModel::selectRequest,
                            onExportHar = viewModel::exportHar,
                            onClear = viewModel::clearRequests,
                            modifier = Modifier.weight(0.4f).fillMaxHeight(),
                        )
                        VerticalDivider(modifier = Modifier.width(BugsyDesktopSpacing.dividerThickness), color = BugsyDesktopColors.BorderHeader)
                        RequestDetailPanel(
                            request = selectedRequest,
                            onCreateMock = { viewModel.createMockFromRequest(it) },
                            modifier = Modifier.weight(0.6f).fillMaxHeight(),
                        )
                    }
                    DesktopTab.MOCKS -> Row(Modifier.weight(1f).fillMaxWidth()) {
                        MocksScreen(
                            rules = mockRules,
                            selectedRuleId = editingMockRule?.id,
                            onSelectRule = viewModel::selectMockRule,
                            onNewRule = viewModel::startNewMockRule,
                            onToggleActive = viewModel::toggleMockRule,
                            onDeleteRule = viewModel::deleteMockRule,
                            onImport = viewModel::importMockRules,
                            onExport = viewModel::exportMockRules,
                            modifier = Modifier.weight(0.5f).fillMaxHeight(),
                        )
                        if (isMockFormOpen) {
                            VerticalDivider(modifier = Modifier.width(BugsyDesktopSpacing.dividerThickness), color = BugsyDesktopColors.BorderHeader)
                            MockRuleFormPanel(
                                initial = editingMockRule,
                                prefillSourceLabel = mockPrefillSourceLabel,
                                onSave = viewModel::addOrUpdateMockRule,
                                onDelete = editingMockRule?.takeIf { mockPrefillSourceLabel == null }?.let { rule -> { viewModel.deleteMockRule(rule.id) } },
                                onCancel = viewModel::closeMockForm,
                                modifier = Modifier.weight(0.5f).fillMaxHeight(),
                            )
                        }
                    }
                    DesktopTab.FLAGS -> Row(Modifier.weight(1f).fillMaxWidth()) {
                        FlagsScreen(
                            flags = flags,
                            searchQuery = flagSearchQuery,
                            onSearchQueryChange = viewModel::setFlagSearchQuery,
                            selectedKey = selectedFlag?.key,
                            onSelectFlag = viewModel::selectFlag,
                            onToggle = viewModel::toggleFlag,
                            onResetAll = viewModel::resetAllFlags,
                            modifier = Modifier.weight(0.6f).fillMaxHeight(),
                        )
                        VerticalDivider(modifier = Modifier.width(BugsyDesktopSpacing.dividerThickness), color = BugsyDesktopColors.BorderHeader)
                        FlagDetailPanel(
                            flag = selectedFlag,
                            onToggle = { enabled -> selectedFlag?.let { viewModel.toggleFlag(it.key, enabled) } },
                            onReset = { selectedFlag?.let { viewModel.resetFlag(it.key) } },
                            modifier = Modifier.weight(0.4f).fillMaxHeight(),
                        )
                    }
                    DesktopTab.DEVICE -> DeviceScreen(info = deviceInfo, connectedAtEpochMs = connectedAtEpochMs, modifier = Modifier.weight(1f))
                }
            }
        }
    }
        DesktopToastOverlay(Modifier.fillMaxSize())
    }
}

@Composable
private fun ConnectionToolbar(state: ConnectionState, onDisconnect: () -> Unit) {
    TopAppBar(
        title = { Text("Bugsy Desktop", style = BugsyDesktopTypography.Heading) },
        actions = {
            val (label, color) = when (state) {
                ConnectionState.DISCONNECTED -> "Disconnected" to BugsyDesktopColors.Status4xx
                ConnectionState.CONNECTING -> "Connecting..." to BugsyDesktopColors.Status3xx
                ConnectionState.CONNECTED -> "Connected" to BugsyDesktopColors.Status2xx
                ConnectionState.RECONNECTING -> "Reconnecting..." to BugsyDesktopColors.Status3xx
            }
            Text(label, style = BugsyDesktopTypography.Body, color = color, modifier = Modifier.padding(end = BugsyDesktopSpacing.sm))
            TextButton(onClick = onDisconnect) { Text("Disconnect", style = BugsyDesktopTypography.Body) }
        },
    )
}
