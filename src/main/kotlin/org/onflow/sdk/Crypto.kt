package org.onflow.sdk

import java.math.BigInteger
import java.security.KeyFactory
import java.security.KeyPairGenerator
import java.security.Security
import java.security.Signature
import kotlin.experimental.and
import kotlin.math.max
import org.bouncycastle.jce.ECNamedCurveTable
import org.bouncycastle.jce.interfaces.ECPrivateKey
import org.bouncycastle.jce.interfaces.ECPublicKey
import org.bouncycastle.jce.provider.BouncyCastleProvider
import org.bouncycastle.jce.spec.ECPrivateKeySpec

data class KeyPair(
    val private: PrivateKey,
    val public: PublicKey
)

data class PrivateKey(
    val key: java.security.PrivateKey,
    val ecCoupleComponentSize: Int,
    val hex: String
)

data class PublicKey(
    val key: java.security.PublicKey,
    val hex: String
)

object Crypto {

    init {
        Security.addProvider(BouncyCastleProvider())
    }

    fun generateKeyPair(algo: SignatureAlgorithm = SignatureAlgorithm.ECDSA_P256): KeyPair {
        val generator = KeyPairGenerator.getInstance("EC", "BC")
        generator.initialize(ECNamedCurveTable.getParameterSpec(algo.curve))
        val keyPair = generator.generateKeyPair()
        val privateKey = keyPair.private
        val publicKey = keyPair.public
        return KeyPair(
            private = PrivateKey(
                key = keyPair.private,
                ecCoupleComponentSize = if (privateKey is ECPrivateKey) {
                    privateKey.parameters.n.bitLength() / 8
                } else {
                    0
                },
                hex = if (privateKey is ECPrivateKey) {
                    privateKey.d.toByteArray().bytesToHex()
                } else {
                    privateKey.encoded.bytesToHex()
                }
            ),
            public = PublicKey(
                key = publicKey,
                hex = if (publicKey is ECPublicKey) {
                    publicKey.q.getEncoded(false).bytesToHex()
                } else {
                    publicKey.encoded.bytesToHex()
                }
            )
        )
    }

    fun decodePrivateKey(privateKey: String, algo: SignatureAlgorithm = SignatureAlgorithm.ECDSA_P256): PrivateKey {
        val ecParameterSpec = ECNamedCurveTable.getParameterSpec(algo.curve)
        val keyFactory = KeyFactory.getInstance(algo.algorithm, "BC")
        val ecPrivateKeySpec = ECPrivateKeySpec(BigInteger(privateKey, 16), ecParameterSpec)
        val pk = keyFactory.generatePrivate(ecPrivateKeySpec)
        return PrivateKey(
            key = pk,
            ecCoupleComponentSize = if (pk is ECPrivateKey) {
                pk.parameters.n.bitLength() / 8
            } else {
                0
            },
            hex = if (pk is ECPrivateKey) {
                pk.d.toByteArray().bytesToHex()
            } else {
                pk.encoded.bytesToHex()
            }
        )
    }

    fun getSigner(privateKey: PrivateKey, hashAlgo: HashAlgorithm = HashAlgorithm.SHA3_256): Signer {
        return SignerImpl(privateKey, hashAlgo)
    }
}

internal class SignerImpl(
    private val privateKey: PrivateKey,
    private val hashAlgo: HashAlgorithm
) : Signer {

    override fun sign(bytes: ByteArray): ByteArray {

        val ecdsaSign = Signature.getInstance(hashAlgo.id)
        ecdsaSign.initSign(privateKey.key)
        ecdsaSign.update(bytes)

        val signature = ecdsaSign.sign()
        if (privateKey.ecCoupleComponentSize <= 0) {
            return signature
        }

        val (r, s) = extractRS(signature)

        val nLen = privateKey.ecCoupleComponentSize
        val paddedSignature = ByteArray(2 * nLen)

        val rBytes = r.toByteArray()
        val sBytes = s.toByteArray()

        // occasionally R/S bytes representation has leading zeroes, so make sure we trim them appropriately
        rBytes.copyInto(paddedSignature, max(nLen - rBytes.size, 0), max(0, rBytes.size - nLen))
        sBytes.copyInto(paddedSignature, max(2 * nLen - sBytes.size, nLen), max(0, sBytes.size - nLen))

        return paddedSignature
    }

    private fun extractRS(signature: ByteArray): Pair<BigInteger, BigInteger> {
        val startR = if ((signature[1] and 0x80.toByte()) != 0.toByte()) 3 else 2
        val lengthR = signature[startR + 1].toInt()
        val startS = startR + 2 + lengthR
        val lengthS = signature[startS + 1].toInt()
        return Pair(
            BigInteger(signature.copyOfRange(startR + 2, startR + 2 + lengthR)),
            BigInteger(signature.copyOfRange(startS + 2, startS + 2 + lengthS))
        )
    }
}
