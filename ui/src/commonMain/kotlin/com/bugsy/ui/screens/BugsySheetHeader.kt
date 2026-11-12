package com.bugsy.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.BugReport
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import org.jetbrains.compose.ui.tooling.preview.Preview
import bugsy.ui.generated.resources.Res
import bugsy.ui.generated.resources.action_clear
import bugsy.ui.generated.resources.bugsy_title
import bugsy.ui.generated.resources.cd_bugsy_icon
import bugsy.ui.generated.resources.cd_close
import com.bugsy.ui.theme.BugsyColors
import com.bugsy.ui.theme.BugsySpacing
import com.bugsy.ui.theme.BugsyTheme
import com.bugsy.ui.theme.BugsyTypography
import org.jetbrains.compose.resources.stringResource

@Composable
fun BugsySheetHeader(requestCount: Int, onClear: () -> Unit, onDismiss: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(BugsySpacing.sm),
        modifier = Modifier.fillMaxWidth()
            .padding(horizontal = BugsySpacing.sheetHorizontal, vertical = BugsySpacing.headerPaddingVertical),
    ) {
        Icon(
            Icons.Outlined.BugReport,
            contentDescription = stringResource(Res.string.cd_bugsy_icon),
            tint = BugsyColors.Brand,
            modifier = Modifier.size(BugsySpacing.iconSizeLarge),
        )
        Text(stringResource(Res.string.bugsy_title), style = BugsyTypography.Title, color = BugsyColors.TextPrimary)
        if (requestCount > 0) {
            Box(
                Modifier.background(BugsyColors.Brand, CircleShape)
                    .padding(
                        horizontal = BugsySpacing.countBadgePaddingHorizontal,
                        vertical = BugsySpacing.countBadgePaddingVertical,
                    ),
            ) {
                Text(
                    requestCount.toString(),
                    style = BugsyTypography.Caption.copy(fontWeight = FontWeight.Medium),
                    color = Color.White,
                )
            }
        }
        Spacer(Modifier.weight(1f))
        OutlinedButton(
            onClick = onClear,
            border = BorderStroke(BugsySpacing.strokeBorder, BugsyColors.Border),
            shape = RoundedCornerShape(BugsySpacing.cornerButton),
            contentPadding = PaddingValues(
                horizontal = BugsySpacing.clearButtonPaddingHorizontal,
                vertical = BugsySpacing.clearButtonPaddingVertical,
            ),
        ) {
            Text(stringResource(Res.string.action_clear), style = BugsyTypography.Caption, color = BugsyColors.TextMuted)
        }
        Spacer(Modifier.width(BugsySpacing.dismissButtonSpacing))
        IconButton(
            onClick = onDismiss,
            modifier = Modifier.size(BugsySpacing.dismissButtonSize).background(BugsyColors.SurfaceElevated, CircleShape),
        ) {
            Icon(
                Icons.Outlined.Close,
                contentDescription = stringResource(Res.string.cd_close),
                tint = BugsyColors.TextMuted,
                modifier = Modifier.size(BugsySpacing.iconSizeMedium),
            )
        }
    }
}

@Preview
@Composable
private fun BugsySheetHeaderPreview() {
    BugsyTheme {
        Box(Modifier.background(BugsyColors.Surface)) {
            BugsySheetHeader(requestCount = 12, onClear = {}, onDismiss = {})
        }
    }
}
