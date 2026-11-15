package com.bugsy.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.sp
import com.bugsy.ui.theme.BugsyColors
import com.bugsy.ui.theme.BugsySpacing
import com.bugsy.ui.theme.BugsyTypography

/**
 * @param icon optional large icon shown above the title.
 * @param actionLabel/[onAction] optional primary call-to-action button.
 * @param secondaryLabel/[onSecondaryAction] optional secondary text link below the primary action.
 */
@Composable
fun EmptyState(
    title: String,
    subtitle: String? = null,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
    secondaryLabel: String? = null,
    onSecondaryAction: (() -> Unit)? = null,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(BugsySpacing.emptyStateTitleSpacing),
        modifier = modifier.fillMaxWidth().padding(
            horizontal = BugsySpacing.lg,
            vertical = BugsySpacing.emptyStatePaddingVertical,
        ),
    ) {
        if (icon != null) {
            Icon(
                icon,
                contentDescription = null,
                tint = BugsyColors.Brand,
                modifier = Modifier.size(BugsySpacing.emptyStateIconSize),
            )
        }
        Text(
            title,
            style = BugsyTypography.Body.copy(fontSize = 20.sp, fontWeight = FontWeight.Medium),
            color = BugsyColors.TextPrimary,
            textAlign = TextAlign.Center,
        )
        if (subtitle != null) {
            Text(
                subtitle,
                style = BugsyTypography.BodySmall,
                color = BugsyColors.TextDisabled,
                textAlign = TextAlign.Center,
            )
        }
        if (actionLabel != null && onAction != null) {
            Button(
                onClick = onAction,
                colors = ButtonDefaults.buttonColors(containerColor = BugsyColors.Brand, contentColor = BugsyColors.Surface),
                shape = RoundedCornerShape(BugsySpacing.cornerButtonLarge),
                modifier = Modifier.fillMaxWidth().height(BugsySpacing.resetAllButtonHeight),
            ) {
                Text(actionLabel, style = BugsyTypography.Body.copy(fontWeight = FontWeight.Medium))
            }
        }
        if (secondaryLabel != null && onSecondaryAction != null) {
            Text(
                secondaryLabel,
                style = BugsyTypography.BodySmall,
                color = BugsyColors.TextDisabled,
                textAlign = TextAlign.Center,
                modifier = Modifier.clickable(onClick = onSecondaryAction),
            )
        }
    }
}
