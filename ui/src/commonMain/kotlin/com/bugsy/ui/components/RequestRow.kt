package com.bugsy.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.sp
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.ui.tooling.preview.Preview
import bugsy.ui.generated.resources.Res
import bugsy.ui.generated.resources.tag_slow_request
import com.bugsy.core.DebugRepository
import com.bugsy.core.model.DebugRequest
import com.bugsy.ui.PreviewFixtures
import com.bugsy.ui.theme.BugsyColors
import com.bugsy.ui.theme.BugsySpacing
import com.bugsy.ui.theme.BugsyTheme
import com.bugsy.ui.theme.BugsyTypography
import com.bugsy.ui.theme.formatDuration
import com.bugsy.ui.theme.formatUrl
import com.bugsy.ui.theme.statusColor

private const val ACCENT_TINT_ALPHA = 0.06f
private const val SLOW_TAG_ALPHA = 0.16f

/**
 * Clickable list row: method, path (highlighted) with host below, status and duration on the right, and the
 * chevron indicating it opens the detail screen.
 *
 * Gets a tinted background and an accent bar on the left edge when the request errored (4xx/5xx, in the
 * matching status color) or is slow (at or above [DebugRepository.slowRequestThresholdMs], in amber); an error
 * takes priority over slow for which color is used. A slow request also gets a "SLOW" tag under the duration,
 * regardless of which accent color won.
 */
@Composable
fun RequestRow(
    request: DebugRequest,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val (host, path) = splitHostAndPath(formatUrl(request.url))
    val threshold = DebugRepository.slowRequestThresholdMs
    val isSlow = threshold > 0 && request.durationMs >= threshold
    val isError = (request.statusCode ?: 0) >= 400
    val accentColor = when {
        isError -> statusColor(request.statusCode)
        isSlow -> BugsyColors.Status3xx
        else -> null
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(BugsySpacing.md),
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .then(if (accentColor != null) Modifier.background(accentColor.copy(alpha = ACCENT_TINT_ALPHA)) else Modifier)
            // The divider and the accent bar are drawn before the padding: they span the full row, on its
            // bottom edge and left edge respectively.
            .drawBehind {
                val s = BugsySpacing.rowDividerThickness.toPx()
                drawLine(
                    BugsyColors.BorderHeader,
                    Offset(0f, size.height - s / 2),
                    Offset(size.width, size.height - s / 2),
                    s,
                )
                if (accentColor != null) {
                    val accentWidth = BugsySpacing.requestRowAccentWidth.toPx()
                    drawLine(
                        accentColor,
                        Offset(accentWidth / 2, 0f),
                        Offset(accentWidth / 2, size.height),
                        accentWidth,
                    )
                }
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
            if (isSlow) SlowTag()
        }
        Icon(
            Icons.AutoMirrored.Outlined.KeyboardArrowRight,
            contentDescription = null,
            tint = BugsyColors.TextDisabled,
            modifier = Modifier.size(BugsySpacing.requestRowChevronSize),
        )
    }
}

@Composable
private fun SlowTag() {
    Box(
        modifier = Modifier
            .background(BugsyColors.Status3xx.copy(alpha = SLOW_TAG_ALPHA), RoundedCornerShape(BugsySpacing.cornerBadge))
            .padding(horizontal = BugsySpacing.badgePaddingHorizontal, vertical = BugsySpacing.badgePaddingVertical),
    ) {
        Text(
            text = stringResource(Res.string.tag_slow_request),
            style = TextStyle(fontWeight = FontWeight.Bold, fontSize = 10.sp),
            color = BugsyColors.Status3xx,
        )
    }
}

/** "api.dev/v1/users" -> ("api.dev", "/v1/users"); with no path, the path is "/". */
private fun splitHostAndPath(hostAndPath: String): Pair<String, String> {
    val slash = hostAndPath.indexOf('/')
    return if (slash < 0) hostAndPath to "/" else hostAndPath.substring(0, slash) to hostAndPath.substring(slash)
}

@Preview
@Composable
private fun RequestRowPreview() {
    BugsyTheme {
        Column(Modifier.background(BugsyColors.Surface)) {
            listOf(
                PreviewFixtures.getRequest,
                PreviewFixtures.postRequestUnauthorized,
                PreviewFixtures.getRequestSlow,
                PreviewFixtures.postRequestError,
            ).forEach { request ->
                RequestRow(request = request, onClick = {})
            }
        }
    }
}
