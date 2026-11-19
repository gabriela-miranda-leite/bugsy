package com.bugsy.core.pairing

/** No AES-GCM on iOS yet (DEB-67 follow-up): the iOS device keeps sending the unencrypted `pair`. */
internal actual fun sealForPeer(peerPublicKey: ByteArray, aad: ByteArray, plaintext: ByteArray): PeerSealed? = null

internal actual fun sealPairingPayload(secret: ByteArray, aad: ByteArray, plaintext: ByteArray): SealedPayload? = null
