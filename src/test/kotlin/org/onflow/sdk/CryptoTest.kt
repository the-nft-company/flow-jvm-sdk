package org.onflow.sdk

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

internal class CryptoTest {

    @Test
    fun `Can generate KeyPair`() {
        val pair1 = Crypto.generateKeyPair(SignatureAlgorithm.ECDSA_P256)
        val privateKey1 = Crypto.decodePrivateKey(pair1.private.hex)
        assertThat(pair1.private.hex).isEqualTo(privateKey1.hex)

        val pair2 = Crypto.generateKeyPair(SignatureAlgorithm.ECDSA_SECP256k1)
        val privateKey2 = Crypto.decodePrivateKey(pair2.private.hex)
        assertThat(pair2.private.hex).isEqualTo(privateKey2.hex)
    }

    @Test
    fun `Test`() {
        val pair = Crypto.generateKeyPair()
        val signer = Crypto.getSigner(pair.private, HashAlgorithm.SHA3_256)
        val ret = signer.sign("penis".toByteArray())
    }
}
