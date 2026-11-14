package com.bugsy.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import org.jetbrains.compose.ui.tooling.preview.Preview
import bugsy.ui.generated.resources.Res
import bugsy.ui.generated.resources.action_copied
import bugsy.ui.generated.resources.action_copy
import bugsy.ui.generated.resources.action_copy_masked
import bugsy.ui.generated.resources.action_copy_with_secrets
import bugsy.ui.generated.resources.label_curl
import com.bugsy.ui.theme.BugsyColors
import com.bugsy.ui.theme.BugsySpacing
import com.bugsy.ui.theme.BugsyTheme
import com.bugsy.ui.theme.BugsyTypography
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource

@Composable
fun CodeBlock(
    text: String,
    onCopy: (() -> Unit)? = null,
    onCopyWithSecrets: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    var copied by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    Column(modifier) {
        if (onCopy != null && onCopyWithSecrets != null) {
            SectionLabel(stringResource(Res.string.label_curl), Modifier.padding(bottom = BugsySpacing.xs))
            Row(
                horizontalArrangement = Arrangement.spacedBy(BugsySpacing.sm),
                modifier = Modifier.fillMaxWidth().padding(bottom = BugsySpacing.sm),
            ) {
                CopyButton(
                    label = stringResource(Res.string.action_copy_masked),
                    copiedLabel = stringResource(Res.string.action_copied),
                    onCopy = onCopy,
                    accent = BugsyColors.TextDisabled,
                    modifier = Modifier.weight(1f),
                )
                CopyButton(
                    label = stringResource(Res.string.action_copy_with_secrets),
                    copiedLabel = stringResource(Res.string.action_copied),
                    onCopy = onCopyWithSecrets,
                    accent = BugsyColors.Brand,
                    modifier = Modifier.weight(1f),
                )
            }
        } else if (onCopy != null) {
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth().padding(bottom = BugsySpacing.sm),
            ) {
                SectionLabel(stringResource(Res.string.label_curl))
                val tint = if (copied) BugsyColors.Status2xx else BugsyColors.Brand
                OutlinedButton(
                    onClick = {
                        onCopy()
                        copied = true
                        scope.launch {
                            delay(1_600)
                            copied = false
                        }
                    },
                    border = BorderStroke(
                        BugsySpacing.strokeBorder,
                        if (copied) BugsyColors.Status2xx else BugsyColors.BorderButton,
                    ),
                    contentPadding = PaddingValues(
                        horizontal = BugsySpacing.codeBlockButtonPaddingHorizontal,
                        vertical = BugsySpacing.codeBlockButtonPaddingVertical,
                    ),
                    shape = RoundedCornerShape(BugsySpacing.cornerButtonSmall),
                ) {
                    Icon(
                        if (copied) Icons.Outlined.Check else Icons.Outlined.ContentCopy,
                        contentDescription = null,
                        tint = tint,
                        modifier = Modifier.size(BugsySpacing.iconSizeSmall),
                    )
                    Spacer(Modifier.width(BugsySpacing.codeBlockButtonIconSpacing))
                    Text(
                        stringResource(if (copied) Res.string.action_copied else Res.string.action_copy),
                        style = BugsyTypography.Caption,
                        color = tint,
                    )
                }
            }
        }
        Box(
            Modifier
                .fillMaxWidth()
                .background(BugsyColors.Background, RoundedCornerShape(BugsySpacing.cornerCard))
                .border(BugsySpacing.strokeBorder, BugsyColors.Border, RoundedCornerShape(BugsySpacing.cornerCard))
                .padding(BugsySpacing.md),
        ) {
            SelectionContainer {
                Text(text, style = BugsyTypography.CodeSmall.copy(color = BugsyColors.TextMuted))
            }
        }
    }
}

@Preview
@Composable
private fun CodeBlockPreview() {
    BugsyTheme {
        Box(Modifier.background(BugsyColors.Surface).padding(BugsySpacing.lg)) {
            CodeBlock(text = "curl https://api.bugsy.dev/v1/users/42", onCopy = {})
        }
    }
}

@Composable
private fun CopyButton(
    label: String,
    copiedLabel: String,
    onCopy: () -> Unit,
    accent: Color,
    modifier: Modifier = Modifier,
) {
    var copied by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val tint = if (copied) BugsyColors.Status2xx else accent

    OutlinedButton(
        onClick = {
            onCopy()
            copied = true
            scope.launch {
                delay(1_600)
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
        Text(if (copied) copiedLabel else label, style = BugsyTypography.Caption, color = tint, maxLines = 1)
    }
}
