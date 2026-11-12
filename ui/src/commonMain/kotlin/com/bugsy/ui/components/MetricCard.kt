package com.bugsy.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import org.jetbrains.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.bugsy.ui.theme.BugsyColors
import com.bugsy.ui.theme.BugsySpacing
import com.bugsy.ui.theme.BugsyTheme
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
        Spacer(Modifier.height(BugsySpacing.metricLabelSpacing))
        Text(
            value,
            style = TextStyle(fontSize = BugsySpacing.metricValueTextSize, fontWeight = FontWeight.Medium),
            color = valueColor,
        )
    }
}

@Preview
@Composable
private fun MetricCardPreview() {
    BugsyTheme {
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.background(BugsyColors.Surface).padding(8.dp),
        ) {
            MetricCard("Status", "200", valueColor = BugsyColors.Status2xx, modifier = Modifier.width(80.dp))
            MetricCard("Duration", "128ms", modifier = Modifier.width(80.dp))
        }
    }
}
