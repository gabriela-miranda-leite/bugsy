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
import androidx.compose.ui.unit.dp
import com.bugsy.ui.theme.BugsyColors

@Composable
fun DragHandle(modifier: Modifier = Modifier) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier.fillMaxWidth().padding(top = 10.dp, bottom = 2.dp),
    ) {
        Box(
            Modifier.width(38.dp).height(4.dp)
                .background(BugsyColors.TextDisabled, RoundedCornerShape(2.dp)),
        )
    }
}
