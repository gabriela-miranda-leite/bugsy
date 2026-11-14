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
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import bugsy.ui.generated.resources.cd_clear_requests
import com.bugsy.ui.settings.BugsySettings
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
fun RequestListScreen(requests: List<DebugRequest>, onClear: () -> Unit = {}) {
    var filter by remember { mutableStateOf("") }
    var selected by remember { mutableStateOf<DebugRequest?>(null) }
    // A escolha de mostrar/ocultar dados sensíveis vale para todas as requests enquanto o overlay está aberto.
    // O ponto de partida (e qualquer mudança) vem das Configurações.
    val revealDefault by BugsySettings.revealSensitiveByDefault.collectAsState()
    var revealed by remember { mutableStateOf(revealDefault) }
    LaunchedEffect(revealDefault) { revealed = revealDefault }

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
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(BugsySpacing.sm),
            modifier = Modifier.padding(horizontal = BugsySpacing.lg, vertical = BugsySpacing.searchBarVerticalMargin),
        ) {
            BugsySearchBar(value = filter, onValueChange = { filter = it }, modifier = Modifier.weight(1f))
            IconButton(
                onClick = onClear,
                enabled = requests.isNotEmpty(),
                modifier = Modifier
                    .size(BugsySpacing.controlHeight)
                    .border(BugsySpacing.strokeBorder, BugsyColors.Border, RoundedCornerShape(BugsySpacing.cornerCard))
                    .background(BugsyColors.SurfaceCard, RoundedCornerShape(BugsySpacing.cornerCard)),
            ) {
                Icon(
                    Icons.Outlined.DeleteOutline,
                    contentDescription = stringResource(Res.string.cd_clear_requests),
                    tint = if (requests.isNotEmpty()) BugsyColors.Status4xx else BugsyColors.TextDisabled,
                    modifier = Modifier.size(BugsySpacing.iconSizeMedium),
                )
            }
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
