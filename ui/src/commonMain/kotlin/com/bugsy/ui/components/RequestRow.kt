package com.bugsy.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import org.jetbrains.compose.ui.tooling.preview.Preview
import com.bugsy.core.model.DebugRequest
import com.bugsy.ui.PreviewFixtures
import com.bugsy.ui.theme.BugsyColors
import com.bugsy.ui.theme.BugsySpacing
import com.bugsy.ui.theme.BugsyTheme
import com.bugsy.ui.theme.BugsyTypography
import com.bugsy.ui.theme.formatDuration
import com.bugsy.ui.theme.formatUrl
import com.bugsy.ui.theme.statusColor

/**
 * Linha clicável da lista: método, caminho (em destaque) e host embaixo, status e duração à direita, e a seta que
 * indica que abre o detalhe.
 */
@Composable
fun RequestRow(
    request: DebugRequest,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val (host, path) = splitHostAndPath(formatUrl(request.url))

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(BugsySpacing.md),
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            // O divisor é desenhado antes do padding: ocupa a largura toda, na borda inferior da linha.
            .drawBehind {
                val s = BugsySpacing.rowDividerThickness.toPx()
                drawLine(
                    BugsyColors.BorderHeader,
                    Offset(0f, size.height - s / 2),
                    Offset(size.width, size.height - s / 2),
                    s,
                )
            }
            .padding(horizontal = BugsySpacing.rowHorizontal, vertical = BugsySpacing.requestRowVertical),
    ) {
        MethodBadge(request.method)
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(BugsySpacing.xs)) {
            Text(
                text = path,
                style = BugsyTypography.Code.copy(color = BugsyColors.TextSecondary, fontWeight = FontWeight.Medium),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = host,
                style = BugsyTypography.Caption,
                color = BugsyColors.TextDisabled,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(BugsySpacing.xs)) {
            Text(
                text = request.statusCode?.toString() ?: "—",
                style = BugsyTypography.Body.copy(fontWeight = FontWeight.Medium),
                color = statusColor(request.statusCode),
            )
            Text(
                text = formatDuration(request.durationMs),
                style = BugsyTypography.Caption,
                color = BugsyColors.TextDisabled,
            )
        }
        Icon(
            Icons.AutoMirrored.Outlined.KeyboardArrowRight,
            contentDescription = null,
            tint = BugsyColors.TextDisabled,
            modifier = Modifier.size(BugsySpacing.requestRowChevronSize),
        )
    }
}

/** "api.dev/v1/users" -> ("api.dev", "/v1/users"); sem caminho, o caminho é "/". */
private fun splitHostAndPath(hostAndPath: String): Pair<String, String> {
    val slash = hostAndPath.indexOf('/')
    return if (slash < 0) hostAndPath to "/" else hostAndPath.substring(0, slash) to hostAndPath.substring(slash)
}

@Preview
@Composable
private fun RequestRowPreview() {
    BugsyTheme {
        Column(Modifier.background(BugsyColors.Surface)) {
            PreviewFixtures.requests.forEach { request ->
                RequestRow(request = request, onClick = {})
            }
        }
    }
}
