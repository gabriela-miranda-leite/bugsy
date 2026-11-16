package com.bugsy.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.AnnotatedString
import org.jetbrains.compose.ui.tooling.preview.Preview
import bugsy.ui.generated.resources.Res
import bugsy.ui.generated.resources.label_curl
import com.bugsy.ui.theme.BugsyColors
import com.bugsy.ui.theme.BugsySpacing
import com.bugsy.ui.theme.BugsyTheme
import com.bugsy.ui.theme.BugsyTypography
import com.bugsy.ui.theme.highlightJsonPretty
import org.jetbrains.compose.resources.stringResource

/** @param json when true, [text] is pretty-printed and syntax-highlighted as JSON (unchanged if it isn't valid JSON). */
@Composable
fun CodeBlock(
    text: String,
    onCopy: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
    json: Boolean = false,
) {
    Column(modifier) {
        if (onCopy != null) {
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth().padding(bottom = BugsySpacing.sm),
            ) {
                SectionLabel(stringResource(Res.string.label_curl))
                CopyButton(onCopy = onCopy)
            }
        }
        Box(
            Modifier
                .fillMaxWidth()
                .background(BugsyColors.Background, RoundedCornerShape(BugsySpacing.cornerCard))
                .border(BugsySpacing.strokeBorder, BugsyColors.Border, RoundedCornerShape(BugsySpacing.cornerCard))
                .padding(BugsySpacing.md),
        ) {
            val content = if (json) highlightJsonPretty(text) else AnnotatedString(text)
            SelectionContainer {
                Text(content, style = BugsyTypography.CodeSmall.copy(color = BugsyColors.TextMuted))
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
