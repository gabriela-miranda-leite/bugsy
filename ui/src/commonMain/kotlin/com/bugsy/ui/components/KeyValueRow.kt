package com.bugsy.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.ui.Alignment
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import com.bugsy.ui.theme.BugsyColors
import com.bugsy.ui.theme.BugsySpacing
import com.bugsy.ui.theme.BugsyTypography

@Composable
fun KeyValueRow(
    key: String,
    value: String,
    keyMinWidth: Dp = BugsySpacing.keyValueKeyMinWidthDefault,
    modifier: Modifier = Modifier,
    onCopy: (() -> Unit)? = null,
) {
    val flash = rememberCopyFlash()
    Column(modifier) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(BugsySpacing.sm),
            modifier = Modifier
                .fillMaxWidth()
                .then(if (onCopy != null) Modifier.clickable { onCopy(); flash.flash() } else Modifier)
                .padding(vertical = BugsySpacing.clickableRowVertical),
        ) {
            Text(
                key,
                style = BugsyTypography.Code.copy(color = BugsyColors.TextDimmed),
                modifier = Modifier.alignByBaseline().widthIn(min = keyMinWidth),
            )
            Text(
                value,
                style = BugsyTypography.Code.copy(
                    color = if (flash.copied) BugsyColors.Status2xx else BugsyColors.TextSecondary,
                    fontWeight = FontWeight.Medium,
                ),
                modifier = Modifier.alignByBaseline().weight(1f),
            )
            if (onCopy != null) CopyAffordance(copied = flash.copied, modifier = Modifier.align(Alignment.CenterVertically))
        }
        HorizontalDivider(thickness = BugsySpacing.rowDividerThickness, color = BugsyColors.BorderHeader)
    }
}
