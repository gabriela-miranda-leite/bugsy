package com.bugsy.desktop.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun ConnectionScreen(onConnect: (host: String, port: Int, token: String) -> Unit) {
    var host by remember { mutableStateOf("localhost") }
    var port by remember { mutableStateOf("7788") }
    var token by remember { mutableStateOf("") }
    val portValue = port.toIntOrNull()

    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(
            modifier = Modifier.width(360.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text("Conectar ao app", style = MaterialTheme.typography.headlineSmall)
            OutlinedTextField(
                value = host,
                onValueChange = { host = it },
                label = { Text("IP do celular") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = port,
                onValueChange = { port = it },
                label = { Text("Porta") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = token,
                onValueChange = { token = it },
                label = { Text("Token (mostrado no overlay do app)") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            Button(
                onClick = { portValue?.let { onConnect(host, it, token) } },
                enabled = host.isNotBlank() && token.isNotBlank() && portValue != null,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Conectar")
            }
        }
    }
}
