package com.bugsy.core

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class BodyFieldMaskerTest {

    @Test
    fun `masks default sensitive fields in a JSON body`() {
        val body = """{"username":"gabi","password":"1234","token":"abc.def"}"""

        val masked = BodyFieldMasker.mask(body)

        assertEquals(
            """{"username":"gabi","password":"***","token":"***"}""",
            masked
        )
    }

    @Test
    fun `returns null when body is null`() {
        assertNull(BodyFieldMasker.mask(null))
    }
}
