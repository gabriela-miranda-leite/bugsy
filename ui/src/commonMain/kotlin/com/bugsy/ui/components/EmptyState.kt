package com.bugsy.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.bugsy.ui.theme.BugsyColors
import com.bugsy.ui.theme.BugsyTypography

@Composable
fun EmptyState(message: String, modifier: Modifier = Modifier) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier.fillMaxWidth().padding(vertical = 40.dp),
    ) {
        Text(message, style = BugsyTypography.Body, color = BugsyColors.TextDisabled)
    }
}
