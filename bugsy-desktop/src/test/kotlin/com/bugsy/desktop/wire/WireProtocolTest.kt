package com.bugsy.desktop.wire

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class WireProtocolTest {

    @Test
    fun `parses a snapshot with requests`() {
        val text = """{"type":"snapshot","requests":[{"id":"1","url":"https://api.bugsy.dev/v1/users","method":"GET"}],"mockRules":[],"flags":[]}"""
        val event = assertIs<WireEvent.Snapshot>(parseWireEvent(text))
        assertEquals(1, event.requests.size)
        assertEquals("1", event.requests.first().id)
    }

    @Test
    fun `parses a snapshot with no requests yet`() {
        val text = """{"type":"snapshot","requests":[],"mockRules":[],"flags":[]}"""
        val event = assertIs<WireEvent.Snapshot>(parseWireEvent(text))
        assertEquals(emptyList(), event.requests)
    }

    @Test
    fun `parses a request_recorded event`() {
        val text = """{"type":"request_recorded","request":{"id":"2","url":"https://api.bugsy.dev/v1/users","method":"POST","statusCode":201,"durationMs":42}}"""
        val event = assertIs<WireEvent.RequestRecorded>(parseWireEvent(text))
        assertEquals("2", event.request.id)
        assertEquals(201, event.request.statusCode)
        assertEquals(42, event.request.durationMs)
    }

    @Test
    fun `treats an unrecognized event type as unhandled instead of crashing`() {
        val text = """{"type":"mock_rule_changed","rule":{},"action":"added"}"""
        assertEquals(WireEvent.Unhandled, parseWireEvent(text))
    }

    @Test
    fun `treats malformed json as unhandled instead of crashing`() {
        assertEquals(WireEvent.Unhandled, parseWireEvent("not json"))
    }

    @Test
    fun `treats a request_recorded with a missing request field as unhandled`() {
        assertEquals(WireEvent.Unhandled, parseWireEvent("""{"type":"request_recorded"}"""))
    }

    @Test
    fun `encodes a handshake with the given token`() {
        assertEquals("""{"value":"abc123"}""", encodeHandshake("abc123"))
    }
}
