package com.bugsy.core.ws

import java.io.ByteArrayInputStream
import java.security.KeyPairGenerator
import java.security.Signature
import java.security.cert.CertificateFactory
import java.security.cert.X509Certificate
import java.security.interfaces.ECPublicKey
import java.security.spec.ECGenParameterSpec
import java.util.Date
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** The certificate iOS builds by hand has to be something a real X.509 parser accepts and a real verifier trusts. */
class SelfSignedCertificateTest {
    private val keys = KeyPairGenerator.getInstance("EC").apply { initialize(ECGenParameterSpec("secp256r1")) }.generateKeyPair()

    private fun point(): ByteArray {
        val w = (keys.public as ECPublicKey).w
        fun fixed(n: java.math.BigInteger) = n.toByteArray().let { if (it.size > 32) it.copyOfRange(it.size - 32, it.size) else ByteArray(32 - it.size) + it }
        return byteArrayOf(0x04) + fixed(w.affineX) + fixed(w.affineY)
    }

    private fun build(serial: ByteArray = byteArrayOf(0x7F, 0x01, 0x02, 0x03), from: Long = 1_700_000_000L, to: Long = 1_700_000_000L + 86_400L * 30): X509Certificate {
        val der = buildSelfSignedCertificate(point(), serial, from, to) { tbs ->
            Signature.getInstance("SHA256withECDSA").apply { initSign(keys.private); update(tbs) }.sign()
        }
        return CertificateFactory.getInstance("X.509").generateCertificate(ByteArrayInputStream(der)) as X509Certificate
    }

    @Test
    fun `a real X509 parser accepts it and the signature verifies against its own key`() {
        val certificate = build()

        certificate.verify(certificate.publicKey)
        assertEquals(keys.public, certificate.publicKey)
        assertEquals("CN=Bugsy", certificate.subjectX500Principal.name)
        assertEquals(certificate.subjectX500Principal, certificate.issuerX500Principal)
        assertEquals("SHA256withECDSA", certificate.sigAlgName)
    }

    @Test
    fun `the validity dates and the serial come out as given`() {
        val certificate = build(serial = byteArrayOf(0x00, 0x00, 0xFF.toByte(), 0x10))

        assertEquals(Date(1_700_000_000_000L), certificate.notBefore)
        assertEquals(Date((1_700_000_000L + 86_400L * 30) * 1000), certificate.notAfter)
        assertEquals(java.math.BigInteger(1, byteArrayOf(0xFF.toByte(), 0x10)), certificate.serialNumber)
        assertTrue(certificate.serialNumber.signum() > 0)
    }

    @Test
    fun `it is valid for a few days around now`() {
        val now = System.currentTimeMillis() / 1000
        build(from = now - 3_600, to = now + 86_400L * 365).checkValidity()
    }
}
