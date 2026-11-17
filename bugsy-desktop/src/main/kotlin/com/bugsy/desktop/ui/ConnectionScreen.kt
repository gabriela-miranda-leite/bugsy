package com.bugsy.desktop.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import com.bugsy.desktop.ui.theme.BugsyDesktopColors

@Composable
fun ConnectionScreen(onConnect: (host: String, port: Int, token: String) -> Unit) {
    var host by remember { mutableStateOf("localhost") }
    var port by remember { mutableStateOf("7788") }
    var token by remember { mutableStateOf("") }
    val portValue = port.toIntOrNull()

    Box(Modifier.fillMaxSize().background(BugsyDesktopColors.Surface), contentAlignment = Alignment.Center) {
        Column(
            modifier = Modifier.width(360.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text("Connect to app", style = MaterialTheme.typography.headlineSmall, color = BugsyDesktopColors.TextPrimary)
            OutlinedTextField(
                value = host,
                onValueChange = { host = it },
                label = { Text("Device IP") },
                singleLine = true,
                textStyle = TextStyle(color = BugsyDesktopColors.TextPrimary),
                colors = fieldColors(),
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = port,
                onValueChange = { port = it },
                label = { Text("Port") },
                singleLine = true,
                textStyle = TextStyle(color = BugsyDesktopColors.TextPrimary),
                colors = fieldColors(),
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = token,
                onValueChange = { token = it },
                label = { Text("Token (shown in the app's overlay)") },
                singleLine = true,
                textStyle = TextStyle(color = BugsyDesktopColors.TextPrimary),
                colors = fieldColors(),
                modifier = Modifier.fillMaxWidth(),
            )
            Button(
                onClick = { portValue?.let { onConnect(host, it, token) } },
                enabled = host.isNotBlank() && token.isNotBlank() && portValue != null,
                colors = ButtonDefaults.buttonColors(containerColor = BugsyDesktopColors.Brand, contentColor = BugsyDesktopColors.Surface),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Connect")
            }
        }
    }
}

@Composable
private fun fieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = BugsyDesktopColors.Brand,
    unfocusedBorderColor = BugsyDesktopColors.Border,
    focusedLabelColor = BugsyDesktopColors.Brand,
    unfocusedLabelColor = BugsyDesktopColors.TextDisabled,
    focusedContainerColor = BugsyDesktopColors.SurfaceCard,
    unfocusedContainerColor = BugsyDesktopColors.SurfaceCard,
)
