package com.bugsy.ui.components

import androidx.compose.ui.test.junit4.createComposeRule
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
class CopyButtonTest {
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
    fun `clicking calls onCopy and shows feedback that reverts after the delay`() {
        composeRule.mainClock.autoAdvance = false
        var copyCount = 0

        composeRule.setContent {
            CopyButton(onCopy = { copyCount++ })
        }

        composeRule.onNodeWithText("Copy").assertExists()

        composeRule.onNodeWithText("Copy").performClick()
        composeRule.mainClock.advanceTimeByFrame()

        assert(copyCount == 1) { "expected onCopy to be called once, was called $copyCount times" }
        composeRule.onNodeWithText("Copied!").assertExists()

        // The feedback label reverts to "Copy" ~1.6s after the click (FEEDBACK_MS in CopyButton.kt).
        composeRule.mainClock.advanceTimeBy(2_000L)

        composeRule.onNodeWithText("Copy").assertExists()
    }

    @Test
    fun `clicking does not call onCopy twice for a single click`() {
        composeRule.mainClock.autoAdvance = false
        var copyCount = 0

        composeRule.setContent {
            CopyButton(onCopy = { copyCount++ })
        }

        composeRule.onNodeWithText("Copy").performClick()
        composeRule.mainClock.advanceTimeByFrame()

        assert(copyCount == 1) { "expected onCopy to be called exactly once, was called $copyCount times" }
    }
}
