package com.bugsy.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import com.bugsy.ui.theme.BugsySpacing
import com.bugsy.ui.theme.BugsyTypography
import com.bugsy.ui.theme.methodColors

@Composable
fun MethodBadge(method: String, modifier: Modifier = Modifier) {
    val (textColor, bgColor) = methodColors(method)
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .background(bgColor, RoundedCornerShape(BugsySpacing.cornerBadge))
            .defaultMinSize(minWidth = 52.dp)
            .padding(horizontal = 6.dp, vertical = 2.dp),
    ) {
        Text(
            text = method.uppercase(),
            style = BugsyTypography.Label.copy(fontFamily = FontFamily.Monospace),
            color = textColor,
            maxLines = 1,
        )
    }
}
