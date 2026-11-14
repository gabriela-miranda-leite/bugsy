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
import androidx.compose.foundation.background
import androidx.compose.ui.Modifier
import org.jetbrains.compose.ui.tooling.preview.Preview
import bugsy.ui.generated.resources.Res
import bugsy.ui.generated.resources.empty_no_requests_filtered
import bugsy.ui.generated.resources.empty_no_requests_subtitle
import bugsy.ui.generated.resources.empty_no_requests_title
import com.bugsy.core.model.DebugRequest
import com.bugsy.ui.PreviewFixtures
import com.bugsy.ui.components.BugsySearchBar
import com.bugsy.ui.components.EmptyState
import com.bugsy.ui.components.RequestRow
import com.bugsy.ui.theme.BugsyColors
import com.bugsy.ui.theme.BugsySpacing
import com.bugsy.ui.theme.BugsyTheme
import org.jetbrains.compose.resources.stringResource

fun filterRequests(requests: List<DebugRequest>, filter: String): List<DebugRequest> =
    if (filter.isBlank()) {
        requests
    } else {
        requests.filter {
            it.url.contains(filter, ignoreCase = true) || it.method.contains(filter, ignoreCase = true)
        }
    }

@Composable
fun RequestListScreen(requests: List<DebugRequest>) {
    var filter by remember { mutableStateOf("") }
    var selected by remember { mutableStateOf<DebugRequest?>(null) }
    // A escolha de mostrar/ocultar dados sensíveis vale para todas as requests enquanto o overlay está aberto.
    var revealed by remember { mutableStateOf(false) }

    selected?.let { req ->
        RequestDetailScreen(
            request = req,
            onBack = { selected = null },
            revealed = revealed,
            onRevealedChange = { revealed = it },
        )
        return
    }

    val filtered = remember(requests, filter) { filterRequests(requests, filter) }

    Column {
        Box(Modifier.padding(horizontal = BugsySpacing.lg, vertical = BugsySpacing.searchBarVerticalMargin)) {
            BugsySearchBar(value = filter, onValueChange = { filter = it })
        }
        if (filtered.isEmpty()) {
            if (filter.isNotBlank()) {
                EmptyState(title = stringResource(Res.string.empty_no_requests_filtered, filter))
            } else {
                EmptyState(
                    title = stringResource(Res.string.empty_no_requests_title),
                    subtitle = stringResource(Res.string.empty_no_requests_subtitle),
                )
            }
        } else {
            LazyColumn(Modifier.fillMaxWidth()) {
                items(filtered, key = { it.id }) { req ->
                    RequestRow(request = req, onClick = { selected = req })
                }
            }
        }
    }
}

@Preview
@Composable
private fun RequestListScreenPreview() {
    BugsyTheme {
        Box(Modifier.background(BugsyColors.Surface)) {
            RequestListScreen(requests = PreviewFixtures.requests)
        }
    }
}

@Preview
@Composable
private fun RequestListScreenEmptyPreview() {
    BugsyTheme {
        Box(Modifier.background(BugsyColors.Surface)) {
            RequestListScreen(requests = emptyList())
        }
    }
}
