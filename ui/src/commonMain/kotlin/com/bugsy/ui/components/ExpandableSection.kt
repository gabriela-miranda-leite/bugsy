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
import bugsy.ui.generated.resources.Res
import bugsy.ui.generated.resources.action_copy
import bugsy.ui.generated.resources.cd_collapse
import bugsy.ui.generated.resources.cd_expand
import com.bugsy.ui.theme.BugsyColors
import com.bugsy.ui.theme.BugsySpacing
import org.jetbrains.compose.resources.stringResource

/** @param onCopy when non-null, shows a copy icon button in the header, next to the expand/collapse chevron. */
@Composable
fun ExpandableSection(
    title: String,
    initiallyExpanded: Boolean = false,
    onCopy: (() -> Unit)? = null,
    content: @Composable () -> Unit,
) {
    var expanded by remember { mutableStateOf(initiallyExpanded) }
    Column {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth().clickable { expanded = !expanded }.padding(vertical = BugsySpacing.sm),
        ) {
            SectionLabel(title, modifier = Modifier.weight(1f))
            if (onCopy != null) {
                CopyIconButton(onCopy = onCopy, contentDescription = stringResource(Res.string.action_copy))
            }
            Icon(
                if (expanded) Icons.Outlined.ExpandLess else Icons.Outlined.ExpandMore,
                contentDescription = stringResource(if (expanded) Res.string.cd_collapse else Res.string.cd_expand),
                tint = BugsyColors.TextDimmed,
                modifier = Modifier.size(BugsySpacing.iconSizeMedium),
            )
        }
        AnimatedVisibility(visible = expanded) { content() }
    }
}
