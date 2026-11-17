package com.bugsy.desktop.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
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
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.bugsy.desktop.ui.theme.BugsyDesktopColors
import com.bugsy.desktop.ui.theme.BugsyDesktopSpacing
import com.bugsy.desktop.ui.theme.BugsyDesktopTypography
import kotlinx.coroutines.delay
import kotlin.random.Random

/**
 * The "Devices on your network" list and the QR/pairing code are visual-only for now (DEB-60): neither device
 * discovery (mDNS/NSD on the mobile side) nor a short-pairing-code handshake exist yet — only "Manual
 * connection" actually connects. See PR discussion on #68/#60 for the phasing.
 */
@Composable
fun ConnectionScreen(onConnect: (host: String, port: Int, token: String) -> Unit) {
    Column(Modifier.fillMaxSize().background(BugsyDesktopColors.Background)) {
        ConnectTopBar()

        Row(Modifier.weight(1f).fillMaxWidth()) {
            Box(Modifier.weight(1f).fillMaxHeight(), contentAlignment = Alignment.Center) {
                PairingPanel()
            }
            Box(Modifier.weight(1f).fillMaxHeight().background(BugsyDesktopColors.Surface).padding(BugsyDesktopSpacing.xl)) {
                DevicesPanel(onConnect = onConnect)
            }
        }

        ConnectBottomBar()
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
            Text("🐛", style = BugsyDesktopTypography.Heading)
            Text("Bugsy", style = BugsyDesktopTypography.Heading, color = BugsyDesktopColors.TextPrimary)
        }
        Text("⚙", style = BugsyDesktopTypography.Heading, color = BugsyDesktopColors.TextDisabled)
    }
}

@Composable
private fun PairingPanel() {
    var secondsLeft by remember { mutableStateOf(60) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(1_000)
            secondsLeft = if (secondsLeft <= 0) 60 else secondsLeft - 1
        }
    }

    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.width(380.dp)) {
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
            DecorativeQrCode(modifier = Modifier.size(180.dp))
            androidx.compose.foundation.layout.Spacer(Modifier.height(BugsyDesktopSpacing.md))
            Text("Pairing code", style = BugsyDesktopTypography.Caption, color = BugsyDesktopColors.TextDisabled)
            Text("482 913", style = BugsyDesktopTypography.Title.copy(fontFamily = BugsyDesktopTypography.Code.fontFamily), color = BugsyDesktopColors.TextPrimary)
            Text(
                "expires in 0:${secondsLeft.toString().padStart(2, '0')}",
                style = BugsyDesktopTypography.Caption,
                color = BugsyDesktopColors.TextDisabled,
            )
        }
    }
}

/** Not a real, scannable QR — there's no camera-scan flow on mobile to consume one yet. Purely decorative,
 * so the layout matches the reference design without pulling in a QR-generation dependency for nothing. */
@Composable
private fun DecorativeQrCode(modifier: Modifier = Modifier) {
    val cells = remember { List(49) { Random.nextBoolean() } }
    Canvas(modifier.aspectRatio(1f).background(Color.White, RoundedCornerShape(4.dp)).padding(8.dp)) {
        val gridSize = 7
        val cellSize = Size(size.width / gridSize, size.height / gridSize)
        for (row in 0 until gridSize) {
            for (col in 0 until gridSize) {
                val isFinderCorner = (row < 2 && col < 2) || (row < 2 && col > gridSize - 3) || (row > gridSize - 3 && col < 2)
                val filled = isFinderCorner || cells[row * gridSize + col]
                if (filled) {
                    drawRect(
                        color = Color.Black,
                        topLeft = androidx.compose.ui.geometry.Offset(col * cellSize.width, row * cellSize.height),
                        size = cellSize,
                    )
                }
            }
        }
    }
}

@Composable
private fun DevicesPanel(onConnect: (host: String, port: Int, token: String) -> Unit) {
    Column(Modifier.width(420.dp)) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("Devices on your network", style = BugsyDesktopTypography.Heading, color = BugsyDesktopColors.TextPrimary)
            Text("↻", style = BugsyDesktopTypography.Heading, color = BugsyDesktopColors.TextDisabled)
        }
        androidx.compose.foundation.layout.Spacer(Modifier.height(BugsyDesktopSpacing.lg))
        Text(
            "No devices found on your network yet.",
            style = BugsyDesktopTypography.Body,
            color = BugsyDesktopColors.TextDisabled,
        )

        androidx.compose.foundation.layout.Spacer(Modifier.height(BugsyDesktopSpacing.xxl))
        ManualConnectionSection(onConnect = onConnect)
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
private fun ConnectBottomBar() {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = Modifier.fillMaxWidth().height(BugsyDesktopSpacing.statusBarHeight).padding(horizontal = BugsyDesktopSpacing.statusBarPaddingHorizontal),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(BugsyDesktopSpacing.xs)) {
            Box(Modifier.size(8.dp).background(BugsyDesktopColors.Status4xx, androidx.compose.foundation.shape.CircleShape))
            Text("Disconnected", style = BugsyDesktopTypography.Caption, color = BugsyDesktopColors.TextDisabled)
        }
        Text(BugsyDesktopVersion, style = BugsyDesktopTypography.Caption, color = BugsyDesktopColors.TextDisabled)
    }
}
