package com.bugsy.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.sp
import com.bugsy.ui.theme.BugsyColors
import com.bugsy.ui.theme.BugsySpacing
import com.bugsy.ui.theme.BugsyTypography

/** Same layout as [EmptyState], with a spinner instead of just text. */
@Composable
fun LoadingState(title: String, subtitle: String? = null, modifier: Modifier = Modifier) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(BugsySpacing.emptyStateTitleSpacing),
        modifier = modifier.fillMaxWidth().padding(
            horizontal = BugsySpacing.lg,
            vertical = BugsySpacing.emptyStatePaddingVertical,
        ),
    ) {
        CircularProgressIndicator(
            color = BugsyColors.Brand,
            strokeWidth = BugsySpacing.loadingSpinnerStroke,
            modifier = Modifier.size(BugsySpacing.loadingSpinnerSize),
        )
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
    }
}
