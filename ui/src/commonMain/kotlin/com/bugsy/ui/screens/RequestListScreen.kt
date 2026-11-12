package com.bugsy.ui.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.bugsy.core.model.DebugRequest
import com.bugsy.ui.components.BugsySearchBar
import com.bugsy.ui.components.EmptyState
import com.bugsy.ui.components.RequestRow
import com.bugsy.ui.theme.BugsySpacing

@Composable
fun RequestListScreen(requests: List<DebugRequest>) {
    var filter by remember { mutableStateOf("") }
    var selected by remember { mutableStateOf<DebugRequest?>(null) }

    selected?.let { req ->
        RequestDetailScreen(request = req, onBack = { selected = null })
        return
    }

    val filtered = remember(requests, filter) {
        if (filter.isBlank()) {
            requests
        } else {
            requests.filter {
                it.url.contains(filter, ignoreCase = true) || it.method.contains(filter, ignoreCase = true)
            }
        }
    }

    Column {
        Box(Modifier.padding(horizontal = BugsySpacing.lg, vertical = BugsySpacing.sm + 1.dp)) {
            BugsySearchBar(value = filter, onValueChange = { filter = it })
        }
        if (filtered.isEmpty()) {
            EmptyState(if (filter.isNotBlank()) "Nenhum request para \"$filter\"" else "Nenhum request capturado")
        } else {
            LazyColumn(Modifier.fillMaxWidth()) {
                items(filtered, key = { it.id }) { req ->
                    RequestRow(request = req, onClick = { selected = req })
                }
            }
        }
    }
}
