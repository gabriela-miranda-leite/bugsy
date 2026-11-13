package com.bugsy.ui

import com.bugsy.core.Bugsy
import com.bugsy.core.BugsyConfig
import com.bugsy.core.DebugRepository
import com.bugsy.core.model.DebugFlag
import com.bugsy.core.model.DebugRequest
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class DebugViewModelTest {

    private val viewModel = DebugViewModel()

    @BeforeTest
    @AfterTest
    fun clearRepository() {
        DebugRepository.clear()
    }

    @Test
    fun `requests reflects what is in the repository`() {
        val request = fakeRequest(id = "1")

        DebugRepository.add(request)

        assertEquals(listOf(request), viewModel.requests.value)
    }

    @Test
    fun `clearRequests empties the repository`() {
        DebugRepository.add(fakeRequest(id = "1"))
        DebugRepository.add(fakeRequest(id = "2"))

        viewModel.clearRequests()

        assertTrue(viewModel.requests.value.isEmpty())
    }

    @Test
    fun `overrideFlag and resetFlag update the flags exposed to the UI`() {
        Bugsy.initialize(BugsyConfig(flagsProvider = { listOf(DebugFlag("feature_x", "Feature X", false, false)) }))

        viewModel.overrideFlag("feature_x", true)
        assertEquals(true, viewModel.flags.value.single().value)

        viewModel.resetFlag("feature_x")
        assertEquals(false, viewModel.flags.value.single().value)

        Bugsy.destroy()
    }

    private fun fakeRequest(id: String) = DebugRequest(
        id = id,
        url = "https://example.com",
        method = "GET",
        requestHeaders = emptyMap(),
        requestBody = null,
        statusCode = 200,
        responseHeaders = emptyMap(),
        responseBody = null,
        durationMs = 0,
        timestamp = 0,
        curlCommand = "",
    )
}
