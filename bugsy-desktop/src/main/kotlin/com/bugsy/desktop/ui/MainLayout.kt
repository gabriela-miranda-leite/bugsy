@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.bugsy.desktop.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.bugsy.desktop.ConnectionState
import com.bugsy.desktop.DesktopViewModel

@Composable
fun MainLayout(viewModel: DesktopViewModel) {
    val connectionState by viewModel.connectionState.collectAsState()
    val requests by viewModel.requests.collectAsState()

    Scaffold(
        topBar = { ConnectionToolbar(connectionState, onDisconnect = viewModel::disconnect) },
    ) { padding ->
        Box(Modifier.padding(padding).fillMaxSize()) {
            if (connectionState == ConnectionState.DISCONNECTED) {
                ConnectionScreen(onConnect = viewModel::connect)
            } else {
                RequestListScreen(requests)
            }
        }
    }
}

@Composable
private fun ConnectionToolbar(state: ConnectionState, onDisconnect: () -> Unit) {
    TopAppBar(
        title = { Text("Bugsy Desktop") },
        actions = {
            val (label, color) = when (state) {
                ConnectionState.DISCONNECTED -> "Disconnected" to MaterialTheme.colorScheme.error
                ConnectionState.CONNECTING -> "Connecting..." to MaterialTheme.colorScheme.tertiary
                ConnectionState.CONNECTED -> "Connected" to MaterialTheme.colorScheme.primary
                ConnectionState.RECONNECTING -> "Reconnecting..." to MaterialTheme.colorScheme.tertiary
            }
            Text(label, color = color, modifier = Modifier.padding(end = 8.dp))
            if (state != ConnectionState.DISCONNECTED) {
                TextButton(onClick = onDisconnect) { Text("Disconnect") }
            }
        },
    )
}
