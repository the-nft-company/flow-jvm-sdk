package org.onflow.sdk

import com.google.common.io.BaseEncoding
import org.bouncycastle.jce.ECNamedCurveTable
import org.bouncycastle.jce.provider.BouncyCastleProvider
import org.bouncycastle.jce.spec.ECParameterSpec
import org.bouncycastle.jce.spec.ECPrivateKeySpec
import java.math.BigInteger
import java.security.KeyFactory
import java.security.MessageDigest
import java.security.Security
import java.security.Signature
import kotlin.experimental.and
import kotlin.math.max


interface PrivateKey {
    fun Sign(bytes: ByteArray): ByteArray
    fun Hash(bytes: ByteArray): ByteArray
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

fun ByteArray.toHex(): String {
    return BaseEncoding.base16().lowerCase().encode(this)
}

private fun bitsToBytes(bits: Int): Int {
    return (bits + 7) shr 3
}

fun InitCrypto() {
    Security.addProvider(BouncyCastleProvider())
}

class ECDSAp256_SHA3_256PrivateKey(d: BigInteger) : PrivateKey {

    val privateKey: java.security.PrivateKey

    init {
        val ecPrivateKeySpec = ECPrivateKeySpec(d, ecParameterSpec)

        privateKey = keyFactory.generatePrivate(ecPrivateKeySpec)
    }

    private companion object {
        val ecParameterSpec: ECParameterSpec = ECNamedCurveTable.getParameterSpec("P-256")
        val keyFactory: KeyFactory = KeyFactory.getInstance("ECDSA", "BC")
    }

    override fun Sign(bytes: ByteArray): ByteArray {

        val ecdsaSign = Signature.getInstance("SHA3-256withECDSA", "BC")

        ecdsaSign.initSign(privateKey)
        ecdsaSign.update(bytes)
        val signature = ecdsaSign.sign()

        val (r, s) = extractRS(signature)

        val NLen = bitsToBytes(ecParameterSpec.n.bitLength())

        val flowBytes = ByteArray(2 * NLen)

        val rBytes = r.toByteArray()
        val sBytes = s.toByteArray()

        // occasionally R/S bytes representation has leading zeroes, so make sure we trim them appropriately
        rBytes.copyInto(flowBytes, max(NLen - rBytes.size, 0), max(0, rBytes.size - NLen))
        sBytes.copyInto(flowBytes, max(2 * NLen - sBytes.size, NLen), max(0, sBytes.size - NLen))

        return flowBytes
    }

    override fun Hash(bytes: ByteArray): ByteArray {
        val messageDigest = MessageDigest.getInstance("SHA3-256", "BC")
        return messageDigest.digest(bytes)
    }
}