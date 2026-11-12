package com.bugsy.ui.theme

import kotlin.test.Test
import kotlin.test.assertEquals

class BugsyColorUtilsTest {

    @Test
    fun `methodColors returns the right pair for each known HTTP method`() {
        assertEquals(BugsyColors.GetText to BugsyColors.GetBg, methodColors("GET"))
        assertEquals(BugsyColors.PostText to BugsyColors.PostBg, methodColors("POST"))
        assertEquals(BugsyColors.PutText to BugsyColors.PutBg, methodColors("PUT"))
        assertEquals(BugsyColors.DeleteText to BugsyColors.DeleteBg, methodColors("DELETE"))
        assertEquals(BugsyColors.PatchText to BugsyColors.PatchBg, methodColors("PATCH"))
    }

    @Test
    fun `methodColors is case-insensitive`() {
        assertEquals(methodColors("get"), methodColors("GET"))
        assertEquals(methodColors("Post"), methodColors("POST"))
    }

    @Test
    fun `methodColors falls back to the head palette for unknown methods`() {
        assertEquals(BugsyColors.HeadText to BugsyColors.HeadBg, methodColors("HEAD"))
        assertEquals(BugsyColors.HeadText to BugsyColors.HeadBg, methodColors("OPTIONS"))
        assertEquals(BugsyColors.HeadText to BugsyColors.HeadBg, methodColors(""))
    }

    @Test
    fun `statusColor maps status code ranges to the right color`() {
        assertEquals(BugsyColors.Status2xx, statusColor(200))
        assertEquals(BugsyColors.Status2xx, statusColor(299))
        assertEquals(BugsyColors.Status3xx, statusColor(301))
        assertEquals(BugsyColors.Status4xx, statusColor(404))
        assertEquals(BugsyColors.Status5xx, statusColor(503))
    }

    @Test
    fun `statusColor treats null as disabled`() {
        assertEquals(BugsyColors.TextDisabled, statusColor(null))
    }

    @Test
    fun `formatDuration shows milliseconds under one second`() {
        assertEquals("0ms", formatDuration(0))
        assertEquals("999ms", formatDuration(999))
    }

    @Test
    fun `formatDuration shows seconds with one decimal from one second up`() {
        assertEquals("1.0s", formatDuration(1_000))
        assertEquals("2.3s", formatDuration(2_340))
        assertEquals("12.0s", formatDuration(12_000))
    }

    @Test
    fun `formatUrl strips scheme and query params`() {
        assertEquals(
            "api.example.com/users/1",
            formatUrl("https://api.example.com/users/1?page=2&limit=10"),
        )
    }

    @Test
    fun `formatUrl keeps host and path when there is no query`() {
        assertEquals("localhost:8080/path", formatUrl("http://localhost:8080/path"))
    }

    @Test
    fun `formatUrl strips fragment as well as query`() {
        assertEquals("host/path", formatUrl("https://host/path?x=1#section"))
    }

    @Test
    fun `formatUrl leaves a schemeless url untouched aside from its query`() {
        assertEquals("/relative/path", formatUrl("/relative/path?x=1"))
    }
}
