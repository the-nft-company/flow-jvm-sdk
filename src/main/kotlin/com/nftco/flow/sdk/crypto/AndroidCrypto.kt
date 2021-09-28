package com.nftco.flow.sdk.crypto

import com.nftco.flow.sdk.*
import com.nftco.flow.sdk.Signer
import java.math.BigInteger
import java.security.*
import java.security.interfaces.ECPrivateKey
import java.security.interfaces.ECPublicKey
import java.security.spec.ECGenParameterSpec
import java.security.spec.ECParameterSpec
import java.security.spec.ECPrivateKeySpec
import java.security.spec.ECPublicKeySpec
import kotlin.experimental.and
import kotlin.math.max

object AndroidCrypto {

    @JvmStatic
    @JvmOverloads
    fun generateKeyPair(algo: SignatureAlgorithm = SignatureAlgorithm.ECDSA_P256): KeyPair {
        val generator = KeyPairGenerator.getInstance("EC")
        generator.initialize(ECGenParameterSpec(algo.curve), SecureRandom())
        val keyPair = generator.generateKeyPair()
        val privateKey = keyPair.private
        val publicKey = keyPair.public
        return KeyPair(
            private = PrivateKey(
                key = keyPair.private,
                ecCoupleComponentSize = if (privateKey is ECPrivateKey) {
                    privateKey.params.order.bitLength() / 8
                } else {
                    0
                },
                hex = if (privateKey is ECPrivateKey) {
                    privateKey.s.toByteArray().bytesToHex()
                } else {
                    throw IllegalArgumentException("PrivateKey must be an ECPublicKey")
                }
            ),
            public = PublicKey(
                key = publicKey,
                hex = if (publicKey is ECPublicKey) {
                    (publicKey.params.generator.affineX.toByteArray() + publicKey.params.generator.affineY.toByteArray()).bytesToHex()
                } else {
                    throw IllegalArgumentException("PublicKey must be an ECPublicKey")
                }
            )
        )
    }

    @JvmStatic
    @JvmOverloads
    fun decodePrivateKey(key: String, algo: SignatureAlgorithm = SignatureAlgorithm.ECDSA_P256): PrivateKey {
        val parameters = AlgorithmParameters.getInstance("EC")
        parameters.init(ECGenParameterSpec(algo.curve))
        val params = parameters.getParameterSpec(ECParameterSpec::class.java)
        val keyFactory = KeyFactory.getInstance(algo.algorithm)
        val ecPrivateKeySpec = ECPrivateKeySpec(BigInteger(key, 16), params)
        val pk = keyFactory.generatePrivate(ecPrivateKeySpec)
        return PrivateKey(
            key = pk,
            ecCoupleComponentSize = if (pk is ECPrivateKey) {
                pk.params.order.bitLength() / 8
            } else {
                0
            },
            hex = if (pk is ECPrivateKey) {
                pk.s.toByteArray().bytesToHex()
            } else {
                throw IllegalArgumentException("PrivateKey must be an ECPublicKey")
            }
        )
    }

    @JvmStatic
    @JvmOverloads
    fun decodePublicKey(key: String, algo: SignatureAlgorithm = SignatureAlgorithm.ECDSA_P256): PublicKey {
        val parameters = AlgorithmParameters.getInstance("EC")
        parameters.init(ECGenParameterSpec(algo.curve))
        val keyFactory = KeyFactory.getInstance("EC")
        val params = parameters.getParameterSpec(ECParameterSpec::class.java)
        // can't use this ECPointUtil class because it's BC
        val point = ECPointUtil.decodePoint(params.curve, byteArrayOf(0x04) + key.hexToBytes())
        val pubKeySpec = ECPublicKeySpec(point, params)
        val publicKey = keyFactory.generatePublic(pubKeySpec)
        return PublicKey(
            key = publicKey,
            hex = if (publicKey is ECPublicKey) {
                (publicKey.q.xCoord.encoded + publicKey.q.yCoord.encoded).bytesToHex()
            } else {
                throw IllegalArgumentException("PublicKey must be an ECPublicKey")
            }
        )
    }

    @JvmStatic
    @JvmOverloads
    fun getSigner(privateKey: PrivateKey, hashAlgo: HashAlgorithm = HashAlgorithm.SHA3_256): Signer {
        return AndroidSignerImpl(privateKey, hashAlgo)
    }

    @JvmStatic
    @JvmOverloads
    fun getHasher(hashAlgo: HashAlgorithm = HashAlgorithm.SHA3_256): Hasher {
        return AndroidHasherImpl(hashAlgo)
    }

    @JvmStatic
    fun normalizeSignature(signature: ByteArray, ecCoupleComponentSize: Int): ByteArray {
        val (r, s) = extractRS(signature)

        val nLen = ecCoupleComponentSize
        val paddedSignature = ByteArray(2 * nLen)

        val rBytes = r.toByteArray()
        val sBytes = s.toByteArray()

        // occasionally R/S bytes representation has leading zeroes, so make sure we trim them appropriately
        rBytes.copyInto(paddedSignature, max(nLen - rBytes.size, 0), max(0, rBytes.size - nLen))
        sBytes.copyInto(paddedSignature, max(2 * nLen - sBytes.size, nLen), max(0, sBytes.size - nLen))

        return paddedSignature
    }

    @JvmStatic
    fun extractRS(signature: ByteArray): Pair<BigInteger, BigInteger> {
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

internal class AndroidHasherImpl(
    private val hashAlgo: HashAlgorithm
) : Hasher {

    override fun hash(bytes: ByteArray): ByteArray {
        val digest = MessageDigest.getInstance(hashAlgo.algorithm)
        return digest.digest(bytes)
    }
}

internal class AndroidSignerImpl(
    private val privateKey: PrivateKey,
    private val hashAlgo: HashAlgorithm,
    override val hasher: Hasher = AndroidHasherImpl(hashAlgo)
) : Signer {

    override fun sign(bytes: ByteArray): ByteArray {

        val ecdsaSign = Signature.getInstance(hashAlgo.id)
        ecdsaSign.initSign(privateKey.key)
        ecdsaSign.update(bytes)

        val signature = ecdsaSign.sign()
        if (privateKey.ecCoupleComponentSize <= 0) {
            return signature
        }

        return Crypto.normalizeSignature(signature, privateKey.ecCoupleComponentSize)
    }
}
