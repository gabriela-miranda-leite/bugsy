package com.bugsy.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.bugsy.ui.theme.BugsyColors
import com.bugsy.ui.theme.BugsySpacing

@Composable
fun DragHandle(modifier: Modifier = Modifier) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier.fillMaxWidth()
            .padding(top = BugsySpacing.dragHandlePaddingTop, bottom = BugsySpacing.dragHandlePaddingBottom),
    ) {
        Box(
            Modifier.width(BugsySpacing.dragHandleWidth).height(BugsySpacing.dragHandleHeight)
                .background(BugsyColors.DragHandle, RoundedCornerShape(BugsySpacing.dragHandleCorner)),
        )
    }
}
