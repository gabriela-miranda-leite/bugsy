package com.bugsy.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import bugsy.ui.generated.resources.Res
import bugsy.ui.generated.resources.action_copied
import bugsy.ui.generated.resources.action_copy
import com.bugsy.ui.theme.BugsyColors
import com.bugsy.ui.theme.BugsySpacing
import com.bugsy.ui.theme.BugsyTypography
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource

private const val FEEDBACK_MS = 1_600L

/** Botão "Copiar" com feedback ("Copiado!") por 1,6 s. */
@Composable
fun CopyButton(
    onCopy: () -> Unit,
    modifier: Modifier = Modifier,
    label: String = stringResource(Res.string.action_copy),
) {
    var copied by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val tint = if (copied) BugsyColors.Status2xx else BugsyColors.Brand

    OutlinedButton(
        onClick = {
            onCopy()
            copied = true
            scope.launch {
                delay(FEEDBACK_MS)
                copied = false
            }
        },
        border = BorderStroke(BugsySpacing.strokeBorder, if (copied) BugsyColors.Status2xx else BugsyColors.BorderButton),
        contentPadding = PaddingValues(
            horizontal = BugsySpacing.codeBlockButtonPaddingHorizontal,
            vertical = BugsySpacing.codeBlockButtonPaddingVertical,
        ),
        shape = RoundedCornerShape(BugsySpacing.cornerButtonSmall),
        modifier = modifier,
    ) {
        Icon(
            if (copied) Icons.Outlined.Check else Icons.Outlined.ContentCopy,
            contentDescription = null,
            tint = tint,
            modifier = Modifier.size(BugsySpacing.iconSizeSmall),
        )
        Spacer(Modifier.width(BugsySpacing.codeBlockButtonIconSpacing))
        Text(
            if (copied) stringResource(Res.string.action_copied) else label,
            style = BugsyTypography.Caption,
            color = tint,
            maxLines = 1,
        )
    }
}

/** Versão só com ícone, para linhas apertadas (ex.: ao lado da URL). */
@Composable
fun CopyIconButton(onCopy: () -> Unit, contentDescription: String, modifier: Modifier = Modifier) {
    var copied by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    IconButton(
        onClick = {
            onCopy()
            copied = true
            scope.launch {
                delay(FEEDBACK_MS)
                copied = false
            }
        },
        modifier = modifier.size(BugsySpacing.copyIconButtonSize),
    ) {
        Icon(
            if (copied) Icons.Outlined.Check else Icons.Outlined.ContentCopy,
            contentDescription = contentDescription,
            tint = if (copied) BugsyColors.Status2xx else BugsyColors.Brand,
            modifier = Modifier.size(BugsySpacing.iconSizeMedium),
        )
    }
}
