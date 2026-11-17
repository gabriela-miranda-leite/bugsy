package com.bugsy.desktop.wire

import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
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
        assertEquals(WireEvent.Unhandled, parseWireEvent("""{"type":"something_else"}"""))
    }

    @Test
    fun `parses a flag_changed event`() {
        val text = """{"type":"flag_changed","flag":{"key":"featureNewCheckout","label":"featureNewCheckout","value":true,"isOverridden":true,"defaultValue":false}}"""
        val event = assertIs<WireEvent.FlagChanged>(parseWireEvent(text))
        assertEquals("featureNewCheckout", event.flag.key)
        assertEquals(true, event.flag.value)
        assertEquals(false, event.flag.defaultValue)
        assertEquals(true, event.flag.isOverridden)
    }

    @Test
    fun `treats a flag_changed with a malformed flag as unhandled`() {
        assertEquals(WireEvent.Unhandled, parseWireEvent("""{"type":"flag_changed","flag":{}}"""))
    }

    @Test
    fun `parses a snapshot's flags, including non-boolean values`() {
        val text = """{"type":"snapshot","requests":[],"mockRules":[],"flags":[
            {"key":"debugLogging","label":"debugLogging","value":true,"isOverridden":false},
            {"key":"apiEnvironment","label":"apiEnvironment","value":"sandbox","isOverridden":true,"defaultValue":"production"}
        ]}"""
        val event = assertIs<WireEvent.Snapshot>(parseWireEvent(text))
        assertEquals(2, event.flags.size)
        assertEquals("sandbox", event.flags.last().value)
        assertEquals("production", event.flags.last().defaultValue)
    }

    @Test
    fun `encodes a toggle_flag command`() {
        assertEquals("""{"type":"toggle_flag","key":"featureNewCheckout","enabled":true}""", encodeToggleFlag("featureNewCheckout", true))
    }

    @Test
    fun `encodes a reset_flag command`() {
        assertEquals("""{"type":"reset_flag","key":"featureNewCheckout"}""", encodeResetFlag("featureNewCheckout"))
    }

    @Test
    fun `encodes a reset_all_flags command`() {
        assertEquals("""{"type":"reset_all_flags"}""", encodeResetAllFlags())
    }

    @Test
    fun `parses a mock_rule_changed event`() {
        val text = """{"type":"mock_rule_changed","rule":{"id":"mock-1","urlPattern":"^/v1/users$","statusCode":401},"action":"added"}"""
        val event = assertIs<WireEvent.MockRuleChanged>(parseWireEvent(text))
        assertEquals("mock-1", event.rule.id)
        assertEquals(401, event.rule.statusCode)
        assertEquals("added", event.action)
    }

    @Test
    fun `treats a mock_rule_changed with a malformed rule as unhandled`() {
        assertEquals(WireEvent.Unhandled, parseWireEvent("""{"type":"mock_rule_changed","rule":{},"action":"added"}"""))
    }

    @Test
    fun `parses a snapshot's mock rules`() {
        val text = """{"type":"snapshot","requests":[],"mockRules":[{"id":"mock-1","urlPattern":"/v1/feed.*","isActive":false}],"flags":[]}"""
        val event = assertIs<WireEvent.Snapshot>(parseWireEvent(text))
        assertEquals(1, event.mockRules.size)
        assertEquals(false, event.mockRules.first().isActive)
    }

    @Test
    fun `encodes an add_mock_rule command`() {
        val rule = WireMockRule(id = "mock-1", urlPattern = "^/v1/login$", method = "POST", statusCode = 401, isOneShot = true)
        val encoded = encodeAddMockRule(rule)
        assertEquals("add_mock_rule", kotlinx.serialization.json.Json.parseToJsonElement(encoded).jsonObject["type"]?.jsonPrimitive?.content)
        val decodedRule = wireJson.decodeFromJsonElement(WireMockRule.serializer(), kotlinx.serialization.json.Json.parseToJsonElement(encoded).jsonObject.getValue("rule"))
        assertEquals(rule, decodedRule)
    }

    @Test
    fun `encodes a toggle_mock_rule command`() {
        assertEquals(
            """{"type":"toggle_mock_rule","id":"mock-1","enabled":false}""",
            encodeToggleMockRule("mock-1", false),
        )
    }

    @Test
    fun `encodes a delete_mock_rule command`() {
        assertEquals("""{"type":"delete_mock_rule","id":"mock-1"}""", encodeDeleteMockRule("mock-1"))
    }

    @Test
    fun `mock rule display name falls back to a de-anchored pattern`() {
        val rule = WireMockRule(id = "1", urlPattern = "^/v1/files/report\\.pdf$")
        assertEquals("/v1/files/report.pdf", rule.displayName)
    }

    @Test
    fun `mock rule display name prefers an explicit name`() {
        val rule = WireMockRule(id = "1", urlPattern = "/v1/login", name = "Login failure")
        assertEquals("Login failure", rule.displayName)
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
