package com.bugsy.ui.components

import androidx.compose.material3.Text
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import kotlin.test.BeforeTest
import kotlin.test.Test
import org.junit.Rule
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExpandableSectionTest {
    @get:Rule
    val composeRule = createComposeRule()

    @BeforeTest
    fun setUp() {
        // See ToggleRowTest for why this is needed: Robolectric doesn't run the compose-resources
        // AndroidContextProvider on its own, so stringResource() throws without it.
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        context.contentResolver
            .acquireContentProviderClient("${context.packageName}.resources.AndroidContextProvider")
            ?.close()
    }

    @Test
    fun `content is hidden until the header is clicked`() {
        composeRule.setContent {
            ExpandableSection(title = "Headers") { Text("body content") }
        }

        composeRule.onNodeWithText("body content").assertDoesNotExist()

        composeRule.onNodeWithText("Headers", ignoreCase = true).performClick()

        composeRule.onNodeWithText("body content").assertExists()
    }

    @Test
    fun `clicking the header again collapses the content`() {
        composeRule.setContent {
            ExpandableSection(title = "Headers", initiallyExpanded = true) { Text("body content") }
        }

        composeRule.onNodeWithText("body content").assertExists()

        composeRule.onNodeWithText("Headers", ignoreCase = true).performClick()

        composeRule.onNodeWithText("body content").assertDoesNotExist()
    }

    @Test
    fun `clicking the copy icon calls onCopy without toggling the expanded state`() {
        var copyCount = 0
        composeRule.setContent {
            ExpandableSection(title = "Headers", onCopy = { copyCount++ }) { Text("body content") }
        }

        composeRule.onNodeWithText("body content").assertDoesNotExist()

        composeRule.onNodeWithContentDescription("Copy").performClick()

        assert(copyCount == 1) { "expected onCopy to be called once, was called $copyCount times" }
        composeRule.onNodeWithText("body content").assertDoesNotExist()
    }
}
