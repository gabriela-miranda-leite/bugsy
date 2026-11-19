package com.bugsy.core.ws

/**
 * Builds a minimal self-signed X.509 certificate (v1, ECDSA P-256 with SHA-256) as DER bytes (DEB-67).
 *
 * iOS has no public API to make a certificate, and the BugsyWsServer needs one for TLS, so the few DER structures
 * are assembled here and the signature is delegated to [sign] (the platform's `SecKey`). Pure Kotlin so the result
 * can be parsed and verified against the JDK's own X.509 implementation in a unit test.
 */
internal fun buildSelfSignedCertificate(
    publicKeyPoint: ByteArray,
    serial: ByteArray,
    notBeforeEpochSeconds: Long,
    notAfterEpochSeconds: Long,
    commonName: String = "Bugsy",
    sign: (ByteArray) -> ByteArray,
): ByteArray {
    require(publicKeyPoint.size == 65 && publicKeyPoint[0] == 0x04.toByte()) { "expected an uncompressed P-256 point" }
    val signatureAlgorithm = der(0x30, OID_ECDSA_SHA256)
    val name = der(0x30, der(0x31, der(0x30, OID_COMMON_NAME + der(0x0C, commonName.encodeToByteArray()))))
    val validity = der(0x30, utcTime(notBeforeEpochSeconds) + utcTime(notAfterEpochSeconds))
    val subjectPublicKeyInfo = der(
        0x30,
        der(0x30, OID_EC_PUBLIC_KEY + OID_P256) + der(0x03, byteArrayOf(0) + publicKeyPoint),
    )
    val tbs = der(0x30, derInteger(serial) + signatureAlgorithm + name + validity + name + subjectPublicKeyInfo)
    val signature = sign(tbs)
    return der(0x30, tbs + signatureAlgorithm + der(0x03, byteArrayOf(0) + signature))
}

private val OID_ECDSA_SHA256 = byteArrayOf(0x06, 0x08, 0x2A, 0x86.toByte(), 0x48, 0xCE.toByte(), 0x3D, 0x04, 0x03, 0x02)
private val OID_EC_PUBLIC_KEY = byteArrayOf(0x06, 0x07, 0x2A, 0x86.toByte(), 0x48, 0xCE.toByte(), 0x3D, 0x02, 0x01)
private val OID_P256 = byteArrayOf(0x06, 0x08, 0x2A, 0x86.toByte(), 0x48, 0xCE.toByte(), 0x3D, 0x03, 0x01, 0x07)
private val OID_COMMON_NAME = byteArrayOf(0x06, 0x03, 0x55, 0x04, 0x03)

private fun der(tag: Int, content: ByteArray): ByteArray {
    val length = content.size
    val header = when {
        length < 0x80 -> byteArrayOf(tag.toByte(), length.toByte())
        length < 0x100 -> byteArrayOf(tag.toByte(), 0x81.toByte(), length.toByte())
        else -> byteArrayOf(tag.toByte(), 0x82.toByte(), (length shr 8).toByte(), length.toByte())
    }
    return header + content
}

/** A positive INTEGER: leading zeros dropped, and a 0x00 put in front when the high bit is set. */
private fun derInteger(value: ByteArray): ByteArray {
    var start = 0
    while (start < value.size - 1 && value[start] == 0.toByte()) start++
    val trimmed = value.copyOfRange(start, value.size)
    val positive = if (trimmed.isNotEmpty() && trimmed[0] < 0) byteArrayOf(0) + trimmed else trimmed
    return der(0x02, if (positive.isEmpty()) byteArrayOf(0) else positive)
}

/** UTCTime `YYMMDDHHMMSSZ`, valid for years 1950 to 2049. */
private fun utcTime(epochSeconds: Long): ByteArray {
    val days = epochSeconds.floorDiv(86_400L)
    val secondsOfDay = epochSeconds.mod(86_400L).toInt()
    // Civil date from days since 1970-01-01 (Howard Hinnant's algorithm).
    val z = days + 719_468
    val era = z.floorDiv(146_097L)
    val dayOfEra = (z - era * 146_097).toInt()
    val yearOfEra = (dayOfEra - dayOfEra / 1_460 + dayOfEra / 36_524 - dayOfEra / 146_096) / 365
    val dayOfYear = dayOfEra - (365 * yearOfEra + yearOfEra / 4 - yearOfEra / 100)
    val monthPrime = (5 * dayOfYear + 2) / 153
    val day = dayOfYear - (153 * monthPrime + 2) / 5 + 1
    val month = if (monthPrime < 10) monthPrime + 3 else monthPrime - 9
    val year = (yearOfEra + era * 400 + if (month <= 2) 1 else 0).toInt()
    require(year in 1950..2049) { "UTCTime holds years 1950 to 2049" }
    fun two(n: Int) = n.toString().padStart(2, '0')
    val text = two(year % 100) + two(month) + two(day) + two(secondsOfDay / 3_600) + two(secondsOfDay % 3_600 / 60) + two(secondsOfDay % 60) + "Z"
    return der(0x17, text.encodeToByteArray())
}
