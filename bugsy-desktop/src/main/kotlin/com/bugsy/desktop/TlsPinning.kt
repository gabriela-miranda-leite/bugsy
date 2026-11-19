package com.bugsy.desktop

import java.security.MessageDigest
import java.security.cert.CertificateException
import java.security.cert.X509Certificate
import javax.net.ssl.X509TrustManager

/**
 * The name every Bugsy phone certificate carries (same value as `:core`). The desktop's own client does not need it
 * (it trusts the pinned fingerprint and skips the hostname check), but a certificate that names something lets any
 * standards-following TLS client verify it.
 */
const val BUGSY_TLS_SERVER_NAME = "bugsy.local"

/** SHA-256 of a certificate's DER encoding, lowercase hex: the same value the phone reports as its fingerprint. */
fun certificateFingerprint(certificate: X509Certificate): String =
    MessageDigest.getInstance("SHA-256").digest(certificate.encoded).joinToString("") { "%02x".format(it) }

/**
 * Accepts exactly one certificate, the one whose fingerprint the phone gave us while pairing (DEB-67). The phone's
 * certificate is self-signed, so there is no chain to validate: matching the pin is the whole check.
 */
class PinnedTrustManager(expectedFingerprint: String) : X509TrustManager {
    private val expected = expectedFingerprint.trim().lowercase().encodeToByteArray()

    override fun checkServerTrusted(chain: Array<out X509Certificate>?, authType: String?) {
        val leaf = chain?.firstOrNull() ?: throw CertificateException("no server certificate")
        val actual = certificateFingerprint(leaf).encodeToByteArray()
        if (!MessageDigest.isEqual(expected, actual)) throw CertificateException("server certificate does not match the pinned fingerprint")
    }

    override fun checkClientTrusted(chain: Array<out X509Certificate>?, authType: String?) =
        throw CertificateException("this trust manager is for servers only")

    override fun getAcceptedIssuers(): Array<X509Certificate> = emptyArray()
}

/** Trust on first use, for connections without a pairing: accepts the certificate and reports its fingerprint so it can be pinned from then on. */
class LearningTrustManager(private val onSeen: (String) -> Unit) : X509TrustManager {
    override fun checkServerTrusted(chain: Array<out X509Certificate>?, authType: String?) {
        val leaf = chain?.firstOrNull() ?: throw CertificateException("no server certificate")
        onSeen(certificateFingerprint(leaf))
    }

    override fun checkClientTrusted(chain: Array<out X509Certificate>?, authType: String?) =
        throw CertificateException("this trust manager is for servers only")

    override fun getAcceptedIssuers(): Array<X509Certificate> = emptyArray()
}
