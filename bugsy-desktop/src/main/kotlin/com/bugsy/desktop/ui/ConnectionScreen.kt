package com.bugsy.desktop.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.bugsy.desktop.ui.theme.BugsyDesktopColors
import com.bugsy.desktop.ui.theme.BugsyDesktopSpacing
import com.bugsy.desktop.ui.theme.BugsyDesktopTypography
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Smartphone
import androidx.compose.material3.Icon
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import com.bugsy.desktop.pairing.DiscoveredDevice
import com.bugsy.desktop.pairing.PairingCode
import com.bugsy.desktop.pairing.QrCodeImage
import kotlinx.coroutines.delay

/**
 * Pairing screen (DEB-64): a QR code and 6-digit code for the phone to scan or type, the phones that answered the
 * network broadcast, and the manual IP/port/token fallback.
 */
@Composable
fun ConnectionScreen(
    pairingCode: PairingCode,
    qrText: String?,
    isPairingAvailable: Boolean,
    devices: List<DiscoveredDevice>,
    isScanning: Boolean,
    pendingDevice: DiscoveredDevice?,
    onRefreshDevices: () -> Unit,
    onConnectDevice: (DiscoveredDevice) -> Unit,
    onConnect: (host: String, port: Int, token: String) -> Unit,
) {
    Column(Modifier.fillMaxSize().background(BugsyDesktopColors.Background)) {
        ConnectTopBar()

        Row(Modifier.weight(1f).fillMaxWidth()) {
            Box(Modifier.weight(1f).fillMaxHeight(), contentAlignment = Alignment.Center) {
                PairingPanel(pairingCode, qrText, isPairingAvailable)
            }
            Box(Modifier.weight(1f).fillMaxHeight().background(BugsyDesktopColors.Surface).padding(BugsyDesktopSpacing.xl)) {
                DevicesPanel(devices, isScanning, pendingDevice, onRefreshDevices, onConnectDevice, onConnect)
            }
        }

        ConnectBottomBar(waitingForPhone = pendingDevice != null)
    }
}

@Composable
private fun ConnectTopBar() {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = Modifier.fillMaxWidth().padding(horizontal = BugsyDesktopSpacing.lg, vertical = BugsyDesktopSpacing.md),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(BugsyDesktopSpacing.sm)) {
            BugsyIcon(modifier = Modifier.size(BugsyDesktopSpacing.connectLogoSize))
            Text("Bugsy", style = BugsyDesktopTypography.Heading, color = BugsyDesktopColors.TextPrimary)
        }
        Text("⚙", style = BugsyDesktopTypography.Heading, color = BugsyDesktopColors.TextDisabled)
    }
}

@Composable
private fun PairingPanel(pairingCode: PairingCode, qrText: String?, isPairingAvailable: Boolean) {
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) {
        while (true) {
            now = System.currentTimeMillis()
            delay(250)
        }
    }
    val secondsLeft = ((pairingCode.expiresAtMs - now + 999) / 1000).coerceAtLeast(0)

    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.width(BugsyDesktopSpacing.connectPairingPanelWidth)) {
        Text("Connect to your app", style = BugsyDesktopTypography.Title, color = BugsyDesktopColors.TextPrimary)
        androidx.compose.foundation.layout.Spacer(Modifier.height(BugsyDesktopSpacing.sm))
        Text(
            "Open the Bugsy overlay on your phone and scan,\nor enter the details below.",
            style = BugsyDesktopTypography.Body,
            color = BugsyDesktopColors.TextDisabled,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
        )
        androidx.compose.foundation.layout.Spacer(Modifier.height(BugsyDesktopSpacing.xl))

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .background(BugsyDesktopColors.SurfaceCard, RoundedCornerShape(BugsyDesktopSpacing.controlCorner))
                .border(BugsyDesktopSpacing.controlBorder, BugsyDesktopColors.Border, RoundedCornerShape(BugsyDesktopSpacing.controlCorner))
                .padding(BugsyDesktopSpacing.xl),
        ) {
            when {
                !isPairingAvailable -> PairingNotice("Pairing is unavailable: another Bugsy Desktop is already using port 7790. Use manual connection.")
                qrText == null -> PairingNotice("No network found. Connect to the same Wi-Fi as your phone, or use manual connection.")
                else -> {
                    QrCodeImage(
                        text = qrText,
                        cornerRadius = BugsyDesktopSpacing.qrCodeCorner,
                        quietZone = BugsyDesktopSpacing.qrCodePadding,
                        modifier = Modifier.size(BugsyDesktopSpacing.qrCodeSize),
                    )
                    androidx.compose.foundation.layout.Spacer(Modifier.height(BugsyDesktopSpacing.md))
                    Text("Pairing code", style = BugsyDesktopTypography.Caption, color = BugsyDesktopColors.TextDisabled)
                    Text(
                        "${pairingCode.code.take(3)} ${pairingCode.code.drop(3)}",
                        style = BugsyDesktopTypography.Title.copy(fontFamily = BugsyDesktopTypography.Code.fontFamily),
                        color = BugsyDesktopColors.TextPrimary,
                    )
                    Text(
                        "expires in ${secondsLeft / 60}:${(secondsLeft % 60).toString().padStart(2, '0')}",
                        style = BugsyDesktopTypography.Caption,
                        color = BugsyDesktopColors.TextDisabled,
                    )
                }
            }
        }
    }
}

@Composable
private fun PairingNotice(text: String) {
    Text(
        text,
        style = BugsyDesktopTypography.Body,
        color = BugsyDesktopColors.TextDisabled,
        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
        modifier = Modifier.width(BugsyDesktopSpacing.qrCodeSize + BugsyDesktopSpacing.xl),
    )
}

@Composable
private fun DevicesPanel(
    devices: List<DiscoveredDevice>,
    isScanning: Boolean,
    pendingDevice: DiscoveredDevice?,
    onRefresh: () -> Unit,
    onConnectDevice: (DiscoveredDevice) -> Unit,
    onConnect: (host: String, port: Int, token: String) -> Unit,
) {
    Column(Modifier.width(BugsyDesktopSpacing.connectFormPanelWidth)) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("Devices on your network", style = BugsyDesktopTypography.Heading, color = BugsyDesktopColors.TextPrimary)
            Icon(
                Icons.Outlined.Refresh,
                contentDescription = "Refresh devices",
                tint = if (isScanning) BugsyDesktopColors.Brand else BugsyDesktopColors.TextDisabled,
                modifier = Modifier.size(20.dp).clip(RoundedCornerShape(4.dp)).clickable(enabled = !isScanning, onClick = onRefresh),
            )
        }
        androidx.compose.foundation.layout.Spacer(Modifier.height(BugsyDesktopSpacing.lg))
        if (devices.isEmpty()) {
            Text(
                if (isScanning) "Looking for devices..." else "No devices found on your network yet.",
                style = BugsyDesktopTypography.Body,
                color = BugsyDesktopColors.TextDisabled,
            )
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(BugsyDesktopSpacing.sm)) {
                devices.forEach { device ->
                    DeviceRow(device, waiting = pendingDevice == device, enabled = pendingDevice == null, onConnect = { onConnectDevice(device) })
                }
            }
            if (pendingDevice != null) {
                androidx.compose.foundation.layout.Spacer(Modifier.height(BugsyDesktopSpacing.sm))
                Text(
                    "Approve the request in the Bugsy overlay on ${pendingDevice.name}.",
                    style = BugsyDesktopTypography.Caption,
                    color = BugsyDesktopColors.TextDisabled,
                )
            }
        }

        androidx.compose.foundation.layout.Spacer(Modifier.height(BugsyDesktopSpacing.xxl))
        ManualConnectionSection(onConnect = onConnect)
    }
}

@Composable
private fun DeviceRow(device: DiscoveredDevice, waiting: Boolean, enabled: Boolean, onConnect: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(BugsyDesktopSpacing.md),
        modifier = Modifier
            .fillMaxWidth()
            .background(BugsyDesktopColors.SurfaceCard, RoundedCornerShape(BugsyDesktopSpacing.controlCorner))
            .border(BugsyDesktopSpacing.controlBorder, BugsyDesktopColors.Border, RoundedCornerShape(BugsyDesktopSpacing.controlCorner))
            .padding(BugsyDesktopSpacing.md),
    ) {
        Icon(Icons.Outlined.Smartphone, contentDescription = null, tint = BugsyDesktopColors.TextDisabled, modifier = Modifier.size(24.dp))
        Box(Modifier.size(BugsyDesktopSpacing.statusDotSize).background(BugsyDesktopColors.Status2xx, androidx.compose.foundation.shape.CircleShape))
        Column(Modifier.weight(1f)) {
            Text(device.name, style = BugsyDesktopTypography.Heading, color = BugsyDesktopColors.TextPrimary)
            Text(
                listOfNotNull(device.os, device.host).joinToString(" · "),
                style = BugsyDesktopTypography.Code,
                color = BugsyDesktopColors.TextDisabled,
            )
        }
        Button(
            onClick = onConnect,
            enabled = enabled,
            colors = ButtonDefaults.buttonColors(containerColor = BugsyDesktopColors.Brand, contentColor = BugsyDesktopColors.Surface),
        ) {
            Text(if (waiting) "Waiting..." else "Connect", style = BugsyDesktopTypography.Body)
        }
    }
}

@Composable
private fun ManualConnectionSection(onConnect: (host: String, port: Int, token: String) -> Unit) {
    var expanded by remember { mutableStateOf(true) }
    var host by remember { mutableStateOf("localhost") }
    var port by remember { mutableStateOf("7788") }
    var token by remember { mutableStateOf("") }
    val portValue = port.toIntOrNull()

    Column {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(BugsyDesktopSpacing.sm),
            modifier = Modifier.clickable { expanded = !expanded },
        ) {
            Text(if (expanded) "⌄" else "›", style = BugsyDesktopTypography.Heading, color = BugsyDesktopColors.TextPrimary)
            Text("Manual connection", style = BugsyDesktopTypography.Heading, color = BugsyDesktopColors.TextPrimary)
        }

        if (!expanded) return@Column
        androidx.compose.foundation.layout.Spacer(Modifier.height(BugsyDesktopSpacing.md))

        Row(horizontalArrangement = Arrangement.spacedBy(BugsyDesktopSpacing.sm)) {
            LabeledField("IP address", host, { host = it }, Modifier.weight(1.4f))
            LabeledField("Port", port, { port = it }, Modifier.weight(0.8f))
            LabeledField("Token", token, { token = it }, Modifier.weight(1f), obscure = true)
        }

        androidx.compose.foundation.layout.Spacer(Modifier.height(BugsyDesktopSpacing.md))
        Button(
            onClick = { portValue?.let { onConnect(host, it, token) } },
            enabled = host.isNotBlank() && token.isNotBlank() && portValue != null,
            colors = ButtonDefaults.buttonColors(containerColor = BugsyDesktopColors.Brand, contentColor = BugsyDesktopColors.Surface),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("Connect manually", style = BugsyDesktopTypography.Body)
        }
    }
}

@Composable
private fun LabeledField(label: String, value: String, onValueChange: (String) -> Unit, modifier: Modifier = Modifier, obscure: Boolean = false) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label, style = BugsyDesktopTypography.Caption) },
        singleLine = true,
        visualTransformation = if (obscure) androidx.compose.ui.text.input.PasswordVisualTransformation() else androidx.compose.ui.text.input.VisualTransformation.None,
        textStyle = BugsyDesktopTypography.Code.copy(color = BugsyDesktopColors.TextPrimary),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = BugsyDesktopColors.Brand,
            unfocusedBorderColor = BugsyDesktopColors.Border,
            focusedLabelColor = BugsyDesktopColors.Brand,
            unfocusedLabelColor = BugsyDesktopColors.TextDisabled,
            focusedContainerColor = BugsyDesktopColors.SurfaceCard,
            unfocusedContainerColor = BugsyDesktopColors.SurfaceCard,
        ),
        modifier = modifier,
    )
}

@Composable
private fun ConnectBottomBar(waitingForPhone: Boolean) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = Modifier.fillMaxWidth().height(BugsyDesktopSpacing.statusBarHeight).padding(horizontal = BugsyDesktopSpacing.statusBarPaddingHorizontal),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(BugsyDesktopSpacing.xs)) {
            val dot = if (waitingForPhone) BugsyDesktopColors.Status3xx else BugsyDesktopColors.Status4xx
            Box(Modifier.size(BugsyDesktopSpacing.statusDotSize).background(dot, androidx.compose.foundation.shape.CircleShape))
            Text(if (waitingForPhone) "Waiting for the phone" else "Disconnected", style = BugsyDesktopTypography.Caption, color = BugsyDesktopColors.TextDisabled)
        }
        Text(BugsyDesktopVersion, style = BugsyDesktopTypography.Caption, color = BugsyDesktopColors.TextDisabled)
    }
}
