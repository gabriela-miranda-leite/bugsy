package com.bugsy.desktop.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ListItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.bugsy.desktop.wire.WireDebugRequest

@Composable
fun RequestListScreen(requests: List<WireDebugRequest>) {
    if (requests.isEmpty()) {
        Box(Modifier.fillMaxSize()) {
            Text("Nenhuma requisição ainda.", modifier = Modifier.align(Alignment.Center))
        }
        return
    }

    LazyColumn(Modifier.fillMaxSize()) {
        items(requests, key = { it.id }) { request ->
            ListItem(
                headlineContent = { Text("${request.method} ${request.url}") },
                supportingContent = { Text("${request.statusCode ?: "-"} · ${request.durationMs}ms") },
            )
            HorizontalDivider()
        }
    }
}
