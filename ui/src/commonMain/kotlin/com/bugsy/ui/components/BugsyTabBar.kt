package com.bugsy.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import org.jetbrains.compose.ui.tooling.preview.Preview
import bugsy.ui.generated.resources.Res
import bugsy.ui.generated.resources.tab_device
import bugsy.ui.generated.resources.tab_flags
import bugsy.ui.generated.resources.tab_mocks
import bugsy.ui.generated.resources.tab_requests
import com.bugsy.ui.theme.BugsyColors
import com.bugsy.ui.theme.BugsySpacing
import com.bugsy.ui.theme.BugsyTheme
import com.bugsy.ui.theme.BugsyTypography
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

enum class BugsyTab(val labelRes: StringResource) {
    Requests(Res.string.tab_requests),
    Mocks(Res.string.tab_mocks),
    Flags(Res.string.tab_flags),
    Device(Res.string.tab_device),
}

@Composable
fun BugsyTabBar(
    selected: BugsyTab,
    onSelect: (BugsyTab) -> Unit,
    modifier: Modifier = Modifier,
    tabs: List<BugsyTab> = BugsyTab.entries,
) {
    Column(modifier) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = BugsySpacing.sheetHorizontal),
            horizontalArrangement = Arrangement.spacedBy(BugsySpacing.tabItemSpacing),
        ) {
            tabs.forEach { tab ->
                val active = tab == selected
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .width(IntrinsicSize.Min)
                        .height(BugsySpacing.tabItemHeight)
                        .clickable { onSelect(tab) }
                        .padding(horizontal = BugsySpacing.tabItemPaddingHorizontal),
                ) {
                    if (active) {
                        Box(
                            Modifier.align(Alignment.BottomCenter)
                                .fillMaxWidth().height(BugsySpacing.tabIndicatorHeight)
                                .background(BugsyColors.Brand),
                        )
                    }
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(BugsySpacing.tabBadgeSpacing),
                    ) {
                        Text(
                            text = stringResource(tab.labelRes),
                            style = if (active) {
                                BugsyTypography.Body.copy(fontWeight = FontWeight.Medium)
                            } else {
                                BugsyTypography.Body
                            },
                            color = if (active) BugsyColors.TextPrimary else BugsyColors.TextDisabled,
                        )
                    }
                }
            }
        }
        HorizontalDivider(thickness = BugsySpacing.strokeBorder, color = BugsyColors.BorderHeader)
    }
}

@Preview
@Composable
private fun BugsyTabBarPreview() {
    BugsyTheme {
        Box(Modifier.background(BugsyColors.Surface)) {
            BugsyTabBar(selected = BugsyTab.Requests, onSelect = {})
        }
    }
}
