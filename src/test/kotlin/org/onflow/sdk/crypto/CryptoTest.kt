package org.onflow.sdk.crypto

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.onflow.sdk.HashAlgorithm
import org.onflow.sdk.SignatureAlgorithm

internal class CryptoTest {

    @Test
    fun `Can generate KeyPair`() {
        val pair1 = Crypto.generateKeyPair(SignatureAlgorithm.ECDSA_P256)
        val privateKey1 = Crypto.decodePrivateKey(pair1.private.hex)
        val publicKey1 = Crypto.decodePublicKey(pair1.public.hex)
        assertThat(pair1.private.hex).isEqualTo(privateKey1.hex)
        assertThat(pair1.public.hex).isEqualTo(publicKey1.hex)

        val pair2 = Crypto.generateKeyPair(SignatureAlgorithm.ECDSA_P256)
        val privateKey2 = Crypto.decodePrivateKey(pair2.private.hex)
        val publicKey2 = Crypto.decodePublicKey(pair2.public.hex)
        assertThat(pair2.private.hex).isEqualTo(privateKey2.hex)
        assertThat(pair2.public.hex).isEqualTo(publicKey2.hex)
    }

    @Test
    fun `Can sign stuff`() {
        val pair = Crypto.generateKeyPair()
        val signer = Crypto.getSigner(pair.private, HashAlgorithm.SHA3_256)
        signer.sign("testing".toByteArray())
    }
}
