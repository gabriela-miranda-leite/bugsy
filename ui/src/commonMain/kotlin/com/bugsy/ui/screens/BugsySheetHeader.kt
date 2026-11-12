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
import androidx.compose.ui.unit.dp
import com.bugsy.ui.theme.BugsyColors
import com.bugsy.ui.theme.BugsySpacing
import com.bugsy.ui.theme.BugsyTypography

@Composable
fun BugsySheetHeader(requestCount: Int, onClear: () -> Unit, onDismiss: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(BugsySpacing.sm),
        modifier = Modifier.fillMaxWidth().padding(horizontal = BugsySpacing.sheetHorizontal, vertical = 10.dp),
    ) {
        Icon(Icons.Outlined.BugReport, contentDescription = "Bugsy", tint = BugsyColors.Brand, modifier = Modifier.size(21.dp))
        Text("Bugsy", style = BugsyTypography.Title, color = BugsyColors.TextPrimary)
        if (requestCount > 0) {
            Box(
                Modifier.background(BugsyColors.Brand, CircleShape).padding(horizontal = 8.dp, vertical = 1.dp),
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
            shape = RoundedCornerShape(7.dp),
            contentPadding = PaddingValues(horizontal = 11.dp, vertical = 5.dp),
        ) {
            Text("Clear", style = BugsyTypography.Caption, color = BugsyColors.TextMuted)
        }
        Spacer(Modifier.width(4.dp))
        IconButton(
            onClick = onDismiss,
            modifier = Modifier.size(30.dp).background(BugsyColors.SurfaceElevated, CircleShape),
        ) {
            Icon(Icons.Outlined.Close, contentDescription = "Fechar", tint = BugsyColors.TextMuted, modifier = Modifier.size(16.dp))
        }
    }
}
