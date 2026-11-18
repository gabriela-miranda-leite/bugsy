package com.bugsy.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import bugsy.ui.generated.resources.Res
import bugsy.ui.generated.resources.search_placeholder
import com.bugsy.ui.theme.BugsyColors
import com.bugsy.ui.theme.BugsySpacing
import com.bugsy.ui.theme.BugsyTypography
import org.jetbrains.compose.resources.stringResource

@Composable
fun BugsySearchBar(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = stringResource(Res.string.search_placeholder),
) {
    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        textStyle = BugsyTypography.Code.copy(color = BugsyColors.TextSecondary),
        singleLine = true,
        cursorBrush = SolidColor(BugsyColors.Brand),
        decorationBox = { inner ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(BugsySpacing.controlHeight)
                    .background(BugsyColors.SurfaceCard, RoundedCornerShape(BugsySpacing.cornerCard))
                    .border(BugsySpacing.strokeBorder, BugsyColors.Border, RoundedCornerShape(BugsySpacing.cornerCard))
                    .padding(horizontal = BugsySpacing.searchBarPaddingHorizontal),
            ) {
                Icon(
                    Icons.Outlined.Search,
                    contentDescription = null,
                    tint = BugsyColors.TextDisabled,
                    modifier = Modifier.size(BugsySpacing.iconSizeMedium),
                )
                Spacer(Modifier.width(BugsySpacing.searchBarIconSpacing))
                Box(Modifier.weight(1f)) {
                    if (value.isEmpty()) {
                        Text(
                            placeholder,
                            style = BugsyTypography.Code.copy(color = BugsyColors.TextDisabled),
                        )
                    }
                    inner()
                }
            }
        },
        modifier = modifier.fillMaxWidth(),
    )
}
