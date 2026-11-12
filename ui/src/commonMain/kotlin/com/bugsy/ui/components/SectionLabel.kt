package com.bugsy.ui.components

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.bugsy.ui.theme.BugsyColors
import com.bugsy.ui.theme.BugsySpacing
import com.bugsy.ui.theme.BugsyTypography

@Composable
fun SectionLabel(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text.uppercase(),
        style = BugsyTypography.Label,
        color = BugsyColors.TextDisabled,
        modifier = modifier.padding(bottom = BugsySpacing.sm),
    )
}
