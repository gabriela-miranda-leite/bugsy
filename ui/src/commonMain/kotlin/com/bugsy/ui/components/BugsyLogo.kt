package com.bugsy.ui.components

import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import bugsy.ui.generated.resources.Res
import bugsy.ui.generated.resources.bugsy_icon
import bugsy.ui.generated.resources.cd_bugsy_icon
import com.bugsy.ui.theme.BugsyColors
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

/**
 * The Bugsy mark. Every place in the overlay that shows the logo uses this, so there is one place to look. The drawing
 * itself (`bugsy_icon.xml`) is generated from `assets/brand/bugsy-mark.svg`, the one file the brand lives in: change the
 * mark there and run `./gradlew :bugsy-desktop:generateBrandAssets` (DEB-68).
 */
@Composable
fun BugsyLogo(modifier: Modifier = Modifier, tint: Color = BugsyColors.Brand) {
    Icon(
        painter = painterResource(Res.drawable.bugsy_icon),
        contentDescription = stringResource(Res.string.cd_bugsy_icon),
        tint = tint,
        modifier = modifier,
    )
}
