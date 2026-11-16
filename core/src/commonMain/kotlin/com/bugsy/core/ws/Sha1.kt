package com.bugsy.core.ws

/**
 * Minimal SHA-1 (RFC 3174), needed to compute `Sec-WebSocket-Accept` for the iOS BugsyWsServer's handshake
 * (DEB-58) — no raw-socket WebSocket handshake to speak of without it, and this project doesn't carry a crypto
 * dependency. Not for anything security-sensitive; SHA-1 here is just the protocol's required digest.
 */
internal object Sha1 {
    private const val H0 = 0x67452301
    private const val H1 = -0x10325477 // 0xEFCDAB89
    private const val H2 = -0x67452302 // 0x98BADCFE
    private const val H3 = 0x10325476
    private const val H4 = -0x3c2d1e10 // 0xC3D2E1F0

    fun hash(message: ByteArray): ByteArray {
        var h0 = H0
        var h1 = H1
        var h2 = H2
        var h3 = H3
        var h4 = H4

        val padded = pad(message)
        val w = IntArray(80)

        var offset = 0
        while (offset < padded.size) {
            for (i in 0 until 16) {
                val base = offset + i * 4
                w[i] = ((padded[base].toInt() and 0xFF) shl 24) or
                    ((padded[base + 1].toInt() and 0xFF) shl 16) or
                    ((padded[base + 2].toInt() and 0xFF) shl 8) or
                    (padded[base + 3].toInt() and 0xFF)
            }
            for (i in 16 until 80) {
                val v = w[i - 3] xor w[i - 8] xor w[i - 14] xor w[i - 16]
                w[i] = (v shl 1) or (v ushr 31)
            }

            var a = h0
            var b = h1
            var c = h2
            var d = h3
            var e = h4

            for (i in 0 until 80) {
                val f: Int
                val k: Int
                when {
                    i < 20 -> {
                        f = (b and c) or (b.inv() and d)
                        k = 0x5A827999
                    }
                    i < 40 -> {
                        f = b xor c xor d
                        k = 0x6ED9EBA1
                    }
                    i < 60 -> {
                        f = (b and c) or (b and d) or (c and d)
                        k = -0x70e44324 // 0x8F1BBCDC
                    }
                    else -> {
                        f = b xor c xor d
                        k = -0x359d3e2a // 0xCA62C1D6
                    }
                }
                val temp = ((a shl 5) or (a ushr 27)) + f + e + k + w[i]
                e = d
                d = c
                c = (b shl 30) or (b ushr 2)
                b = a
                a = temp
            }

            h0 += a
            h1 += b
            h2 += c
            h3 += d
            h4 += e
            offset += 64
        }

        return intsToBytes(h0, h1, h2, h3, h4)
    }

    private fun pad(message: ByteArray): ByteArray {
        val bitLength = message.size.toLong() * 8
        // original length + 0x80 byte + zero padding so the result is 56 (mod 64), then 8 bytes of bit length.
        val paddingZeros = ((55 - message.size % 64) + 64) % 64
        val result = ByteArray(message.size + 1 + paddingZeros + 8)
        message.copyInto(result)
        result[message.size] = 0x80.toByte()
        for (i in 0 until 8) {
            result[result.size - 1 - i] = ((bitLength ushr (i * 8)) and 0xFF).toByte()
        }
        return result
    }

    private fun intsToBytes(vararg ints: Int): ByteArray {
        val result = ByteArray(ints.size * 4)
        ints.forEachIndexed { index, value ->
            result[index * 4] = (value ushr 24).toByte()
            result[index * 4 + 1] = (value ushr 16).toByte()
            result[index * 4 + 2] = (value ushr 8).toByte()
            result[index * 4 + 3] = value.toByte()
        }
        return result
    }
}
