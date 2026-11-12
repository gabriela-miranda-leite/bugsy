package com.bugsy.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ExpandLess
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.bugsy.ui.theme.BugsyColors
import com.bugsy.ui.theme.BugsySpacing

@Composable
fun ExpandableSection(title: String, initiallyExpanded: Boolean = false, content: @Composable () -> Unit) {
    var expanded by remember { mutableStateOf(initiallyExpanded) }
    Column {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth().clickable { expanded = !expanded }.padding(vertical = BugsySpacing.sm),
        ) {
            SectionLabel(title, modifier = Modifier.weight(1f))
            Icon(
                if (expanded) Icons.Outlined.ExpandLess else Icons.Outlined.ExpandMore,
                contentDescription = if (expanded) "Recolher" else "Expandir",
                tint = BugsyColors.TextDimmed,
                modifier = Modifier.size(16.dp),
            )
        }
        AnimatedVisibility(visible = expanded) { content() }
    }
}
