package com.bugsy.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bugsy.ui.theme.BugsyColors
import com.bugsy.ui.theme.BugsySpacing
import com.bugsy.ui.theme.BugsyTypography

@Composable
fun MetricCard(
    label: String,
    value: String,
    valueColor: Color = BugsyColors.TextPrimary,
    modifier: Modifier = Modifier,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = modifier
            .background(BugsyColors.SurfaceCard, RoundedCornerShape(BugsySpacing.cornerCard))
            .border(BugsySpacing.strokeBorder, BugsyColors.Border, RoundedCornerShape(BugsySpacing.cornerCard))
            .padding(BugsySpacing.cardPadding),
    ) {
        Text(label, style = BugsyTypography.Caption, color = BugsyColors.TextDimmed)
        Spacer(Modifier.height(3.dp))
        Text(value, style = TextStyle(fontSize = 20.sp, fontWeight = FontWeight.Medium), color = valueColor)
    }
}
