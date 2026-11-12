package com.bugsy.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.HorizontalDivider
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

enum class BugsyTab { Requests, Flags, Device }

@Composable
fun BugsyTabBar(
    selected: BugsyTab,
    requestCount: Int,
    onSelect: (BugsyTab) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = BugsySpacing.sheetHorizontal),
            horizontalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            BugsyTab.entries.forEach { tab ->
                val active = tab == selected
                Box(
                    modifier = Modifier
                        .clickable { onSelect(tab) }
                        .padding(horizontal = 14.dp, vertical = 7.dp),
                ) {
                    if (active) {
                        Box(
                            Modifier.align(Alignment.BottomCenter)
                                .fillMaxWidth().height(2.dp)
                                .background(BugsyColors.Brand),
                        )
                    }
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp),
                    ) {
                        Text(
                            text = tab.name,
                            style = if (active) {
                                BugsyTypography.Body.copy(fontWeight = FontWeight.Medium)
                            } else {
                                BugsyTypography.Body
                            },
                            color = if (active) BugsyColors.TextPrimary else BugsyColors.TextDimmed,
                        )
                        if (tab == BugsyTab.Requests && requestCount > 0) {
                            Box(
                                Modifier.background(BugsyColors.Brand, CircleShape)
                                    .padding(horizontal = 6.dp, vertical = 1.dp),
                            ) {
                                Text(
                                    requestCount.toString(),
                                    style = BugsyTypography.Caption.copy(fontWeight = FontWeight.Medium),
                                    color = Color.White,
                                )
                            }
                        }
                    }
                }
            }
        }
        HorizontalDivider(thickness = BugsySpacing.strokeBorder, color = BugsyColors.Border)
    }
}
