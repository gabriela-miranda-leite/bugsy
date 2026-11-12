package com.bugsy.core

import com.bugsy.core.model.DebugRequest
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class DebugRepositoryTest {

    @BeforeTest
    fun setUp() {
        DebugRepository.clear()
        DebugRepository.configureMaxEntries(200)
    }

    @AfterTest
    fun tearDown() {
        DebugRepository.clear()
        DebugRepository.configureMaxEntries(200)
    }

    @Test
    fun `discards oldest entry when exceeding max entries`() {
        DebugRepository.configureMaxEntries(200)

        repeat(201) { index -> DebugRepository.add(fakeRequest(id = index.toString())) }

        val captured = DebugRepository.getAll()
        assertEquals(200, captured.size)
        assertEquals("1", captured.first().id)
        assertEquals("200", captured.last().id)
    }

    @Test
    fun `clear empties the list and StateFlow emits empty list`() {
        DebugRepository.add(fakeRequest(id = "1"))
        DebugRepository.add(fakeRequest(id = "2"))

        DebugRepository.clear()

        assertTrue(DebugRepository.getAll().isEmpty())
        assertTrue(DebugRepository.requests.value.isEmpty())
    }

    @Test
    fun `concurrent writes and reads do not throw and respect max entries`() {
        DebugRepository.configureMaxEntries(200)
        val writerThreads = 8
        val entriesPerThread = 100
        val executor = Executors.newFixedThreadPool(writerThreads + 1)

        val readerDone = java.util.concurrent.atomic.AtomicBoolean(false)
        val readerFailure = java.util.concurrent.atomic.AtomicReference<Throwable?>(null)
        executor.submit {
            try {
                while (!readerDone.get()) {
                    DebugRepository.requests.value.forEach { it.id }
                    DebugRepository.getAll().forEach { it.id }
                }
            } catch (t: Throwable) {
                readerFailure.set(t)
            }
        }

        val writers = (0 until writerThreads).map { threadIndex ->
            executor.submit {
                repeat(entriesPerThread) { entryIndex ->
                    DebugRepository.add(fakeRequest(id = "$threadIndex-$entryIndex"))
                }
            }
        }

        writers.forEach { it.get(10, TimeUnit.SECONDS) }
        readerDone.set(true)
        executor.shutdown()
        executor.awaitTermination(10, TimeUnit.SECONDS)

        readerFailure.get()?.let { throw it }
        assertEquals(200, DebugRepository.getAll().size)
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
        curlCommand = ""
    )
}
