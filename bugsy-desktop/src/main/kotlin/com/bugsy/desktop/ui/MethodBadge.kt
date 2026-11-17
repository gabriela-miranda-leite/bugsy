package com.bugsy.desktop.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bugsy.desktop.ui.theme.methodColors

@Composable
fun MethodBadge(method: String, modifier: Modifier = Modifier) {
    val (textColor, bgColor) = methodColors(method)
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .background(bgColor, RoundedCornerShape(4.dp))
            .defaultMinSize(minWidth = 52.dp)
            .padding(horizontal = 6.dp, vertical = 2.dp),
    ) {
        Text(
            text = method.uppercase(),
            style = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.Bold, fontSize = 12.sp),
            color = textColor,
            maxLines = 1,
        )
    }
}
