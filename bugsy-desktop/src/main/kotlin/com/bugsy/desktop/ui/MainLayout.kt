@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.bugsy.desktop.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
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
        topBar = { ConnectionToolbar(connectionState) },
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
private fun ConnectionToolbar(state: ConnectionState) {
    TopAppBar(
        title = { Text("Bugsy Desktop") },
        actions = {
            val (label, color) = when (state) {
                ConnectionState.DISCONNECTED -> "Desconectado" to MaterialTheme.colorScheme.error
                ConnectionState.CONNECTING -> "Conectando..." to MaterialTheme.colorScheme.tertiary
                ConnectionState.CONNECTED -> "Conectado" to MaterialTheme.colorScheme.primary
                ConnectionState.RECONNECTING -> "Reconectando..." to MaterialTheme.colorScheme.tertiary
            }
            Text(label, color = color, modifier = Modifier.padding(end = 16.dp))
        },
    )
}
