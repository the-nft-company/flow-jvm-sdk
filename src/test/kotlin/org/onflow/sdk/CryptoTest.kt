package org.onflow.sdk

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

internal class CryptoTest {

    @Test
    fun `Can generate KeyPair`() {
        val pair1 = Crypto.generateKeyPair(SignatureAlgorithm.ECDSA_SHA3_P256)
        val privateKey1 = Crypto.loadPrivateKey(pair1.privateKey)
        assertThat(pair1.privateKey).isEqualTo(privateKey1.hex)

        val pair2 = Crypto.generateKeyPair(SignatureAlgorithm.ECDSA_SHA3_SECP256K1)
        val privateKey2 = Crypto.loadPrivateKey(pair2.privateKey)
        assertThat(pair2.privateKey).isEqualTo(privateKey2.hex)
    }


}
