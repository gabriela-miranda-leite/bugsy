package com.bugsy.ui.components

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import com.bugsy.core.model.DebugFlag
import kotlin.test.BeforeTest
import kotlin.test.Test
import org.junit.Rule
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ToggleRowTest {
    @get:Rule
    val composeRule = createComposeRule()

    @BeforeTest
    fun setUp() {
        // Robolectric doesn't instantiate manifest-declared providers on its own; the compose-resources
        // AndroidContextProvider needs to actually run (via attachInfo/onCreate) before stringResource()
        // works, otherwise it throws "Android context is not initialized".
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        context.contentResolver
            .acquireContentProviderClient("${context.packageName}.resources.AndroidContextProvider")
            ?.close()
    }

    @Test
    fun `clicking the switch calls onToggle with the flipped value`() {
        var toggledTo: Boolean? = null
        val flag = DebugFlag(key = "new_checkout", label = "New checkout", value = false, isOverridden = false)

        composeRule.setContent {
            ToggleRow(flag = flag, onToggle = { toggledTo = it })
        }

        composeRule.onNodeWithTag("toggleRow_switch").performClick()

        assert(toggledTo == true) { "expected onToggle(true), got $toggledTo" }
    }

    @Test
    fun `override badge shows when the flag is overridden`() {
        val overridden = DebugFlag(key = "f", label = "F", value = true, isOverridden = true)
        composeRule.setContent { ToggleRow(flag = overridden, onToggle = {}) }
        composeRule.onNodeWithText("override").assertExists()
    }

    @Test
    fun `override badge is absent when the flag is not overridden`() {
        val notOverridden = DebugFlag(key = "f", label = "F", value = true, isOverridden = false)
        composeRule.setContent { ToggleRow(flag = notOverridden, onToggle = {}) }
        composeRule.onNodeWithText("override").assertDoesNotExist()
    }
}
