package com.bugsy.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import org.jetbrains.compose.ui.tooling.preview.Preview
import bugsy.ui.generated.resources.Res
import bugsy.ui.generated.resources.bugsy_title
import bugsy.ui.generated.resources.cd_close
import bugsy.ui.generated.resources.cd_settings
import com.bugsy.ui.components.BugsyLogo
import com.bugsy.ui.theme.BugsyColors
import com.bugsy.ui.theme.BugsySpacing
import com.bugsy.ui.theme.BugsyTheme
import com.bugsy.ui.theme.BugsyTypography
import org.jetbrains.compose.resources.stringResource

@Composable
fun BugsySheetHeader(onSettings: () -> Unit, onDismiss: () -> Unit, settingsActive: Boolean = false) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(BugsySpacing.sm),
        modifier = Modifier.fillMaxWidth()
            .padding(horizontal = BugsySpacing.sheetHorizontal, vertical = BugsySpacing.headerPaddingVertical),
    ) {
        BugsyLogo(Modifier.size(BugsySpacing.iconSizeLarge))
        Text(stringResource(Res.string.bugsy_title), style = BugsyTypography.Title, color = BugsyColors.TextPrimary)
        Spacer(Modifier.weight(1f))
        IconButton(
            onClick = onSettings,
            modifier = Modifier.size(BugsySpacing.dismissButtonSize)
                .background(if (settingsActive) BugsyColors.Brand.copy(alpha = 0.18f) else BugsyColors.SurfaceElevated, CircleShape),
        ) {
            Icon(
                Icons.Outlined.Settings,
                contentDescription = stringResource(Res.string.cd_settings),
                tint = if (settingsActive) BugsyColors.Brand else BugsyColors.TextMuted,
                modifier = Modifier.size(BugsySpacing.iconSizeMedium),
            )
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
            BugsySheetHeader(onSettings = {}, onDismiss = {})
        }
    }
}
