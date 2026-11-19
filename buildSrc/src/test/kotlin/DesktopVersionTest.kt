import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class DesktopVersionTest {
    @Test
    fun `an alpha of the 0 series drops the suffix and shows a major of 1`() {
        assertEquals("1.3.0", desktopPackageVersion("0.3.0-alpha"))
    }

    @Test
    fun `any suffix is dropped, including ones with dots and digits`() {
        assertEquals("1.3.0", desktopPackageVersion("0.3.0-alpha.2"))
        assertEquals("2.0.0", desktopPackageVersion("2.0.0-rc1"))
        assertEquals("1.4.1", desktopPackageVersion("0.4.1-SNAPSHOT"))
    }

    @Test
    fun `a stable 0 version also gets a major of 1`() {
        assertEquals("1.3.0", desktopPackageVersion("0.3.0"))
        assertEquals("1.0.1", desktopPackageVersion("0.0.1"))
    }

    @Test
    fun `from 1_0_0 on the desktop has the same number as the library`() {
        assertEquals("1.0.0", desktopPackageVersion("1.0.0"))
        assertEquals("1.2.3", desktopPackageVersion("1.2.3"))
        assertEquals("12.30.400", desktopPackageVersion("12.30.400-beta"))
    }

    @Test
    fun `surrounding whitespace is ignored`() {
        assertEquals("1.3.0", desktopPackageVersion("  0.3.0-alpha \n"))
    }

    @Test
    fun `a version that is not X_Y_Z fails with a message that names VERSION_NAME`() {
        listOf("", "abc", "1.2", "1.2.3.4", "v1.2.3", "1.2.3-", "1.2.3-al pha", "1.2.x", "-1.2.3", "1.2.3+build").forEach { bad ->
            val error = assertFailsWith<IllegalArgumentException>("'$bad' should not parse") { desktopPackageVersion(bad) }
            assertTrue("VERSION_NAME" in error.message.orEmpty(), error.message)
        }
    }

    @Test
    fun `parts the installers can't hold fail instead of reaching the packaging`() {
        assertFailsWith<IllegalArgumentException> { desktopPackageVersion("256.0.0") }
        assertFailsWith<IllegalArgumentException> { desktopPackageVersion("1.256.0") }
        assertFailsWith<IllegalArgumentException> { desktopPackageVersion("1.0.65536") }
        assertFailsWith<IllegalArgumentException> { desktopPackageVersion("99999999999999999999.0.0") }
        assertEquals("255.255.65535", desktopPackageVersion("255.255.65535"))
    }
}
