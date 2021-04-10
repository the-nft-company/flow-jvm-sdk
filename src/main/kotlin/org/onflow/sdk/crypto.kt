package org.onflow.sdk

import java.math.BigInteger
import java.security.KeyFactory
import java.security.SecureRandom
import java.security.Security
import java.security.Signature
import kotlin.experimental.and
import kotlin.math.max
import org.bouncycastle.crypto.generators.ECKeyPairGenerator
import org.bouncycastle.crypto.params.ECDomainParameters
import org.bouncycastle.crypto.params.ECKeyGenerationParameters
import org.bouncycastle.crypto.params.ECPrivateKeyParameters
import org.bouncycastle.crypto.params.ECPublicKeyParameters
import org.bouncycastle.jce.ECNamedCurveTable
import org.bouncycastle.jce.interfaces.ECPrivateKey
import org.bouncycastle.jce.provider.BouncyCastleProvider
import org.bouncycastle.jce.spec.ECParameterSpec
import org.bouncycastle.jce.spec.ECPrivateKeySpec


enum class SignatureAlgorithm(
    val algorithm: String,
    val hash: String,
    val curve: String
) {
    // TODO: Support these methods?
    // ECDSA_SHA2_P256("ECDSA", "SHA2-256withECDSA", "P-256"),
    // ECDSA_SHA2_SECP256K1("ECDSA", "SHA2-256withECDSA", "secp256k1"),

    ECDSA_SHA3_P256("ECDSA", "SHA3-256withECDSA", "P-256"),
    ECDSA_SHA3_SECP256K1("ECDSA", "SHA3-256withECDSA", "secp256k1");
}

interface PrivateKey {
    val hex: String
    fun sign(bytes: ByteArray): ByteArray
}

data class KeyPair(
    val publicKey: String,
    val privateKey: String
)

object Crypto {

    init {
        Security.addProvider(BouncyCastleProvider())
    }

    fun generateKeyPair(algo: SignatureAlgorithm = SignatureAlgorithm.ECDSA_SHA3_P256): KeyPair {

        val curve = ECNamedCurveTable.getParameterSpec(algo.curve)
        val domainParams = ECDomainParameters(curve.curve, curve.g, curve.n, curve.h, curve.seed)
        val random = SecureRandom()
        val keyParams = ECKeyGenerationParameters(domainParams, random)

        val generator = ECKeyPairGenerator()
        generator.init(keyParams)

        val keyPair = generator.generateKeyPair()

        return KeyPair(
            privateKey = (keyPair.private as ECPrivateKeyParameters).d.toByteArray().bytesToHex(),
            publicKey = (keyPair.public as ECPublicKeyParameters).q.getEncoded(false).bytesToHex()
        )
    }

    fun loadPrivateKey(hex: String, algo: SignatureAlgorithm = SignatureAlgorithm.ECDSA_SHA3_P256): PrivateKey {
        return PrivateKeyImpl(hex, algo)
    }

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

internal class PrivateKeyImpl(key: String, private val algo: SignatureAlgorithm) : PrivateKey {

    private val privateKey: ECPrivateKey
    private val ecParameterSpec: ECParameterSpec

    override val hex: String get() = privateKey.d.toByteArray().bytesToHex()

    init {
        ecParameterSpec = ECNamedCurveTable.getParameterSpec(algo.curve)
        val keyFactory = KeyFactory.getInstance(algo.algorithm, "BC")
        val ecPrivateKeySpec = ECPrivateKeySpec(BigInteger(key, 16), ecParameterSpec)
        privateKey = keyFactory.generatePrivate(ecPrivateKeySpec) as ECPrivateKey
    }

    override fun sign(bytes: ByteArray): ByteArray {

        val ecdsaSign = Signature.getInstance(algo.hash, "BC")
        ecdsaSign.initSign(privateKey)
        ecdsaSign.update(bytes)

        val signature = ecdsaSign.sign()
        val (r, s) = Crypto.extractRS(signature)

        val nlen = (ecParameterSpec.n.bitLength() + 7) shr 3
        val flowBytes = ByteArray(2 * nlen)

        val rBytes = r.toByteArray()
        val sBytes = s.toByteArray()

        // occasionally R/S bytes representation has leading zeroes, so make sure we trim them appropriately
        rBytes.copyInto(flowBytes, max(nlen - rBytes.size, 0), max(0, rBytes.size - nlen))
        sBytes.copyInto(flowBytes, max(2 * nlen - sBytes.size, nlen), max(0, sBytes.size - nlen))

        return flowBytes
    }
}
