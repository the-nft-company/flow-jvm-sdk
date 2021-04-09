package org.onflow.sdk.impl

import java.math.BigInteger
import java.security.KeyFactory
import java.security.Signature
import kotlin.experimental.and
import kotlin.math.max
import org.bouncycastle.jce.ECNamedCurveTable
import org.bouncycastle.jce.spec.ECParameterSpec
import org.bouncycastle.jce.spec.ECPrivateKeySpec
import org.onflow.sdk.PrivateKey

class ECDSAp256_SHA3_256PrivateKey(d: BigInteger) : PrivateKey {

    private val privateKey: java.security.PrivateKey

    init {
        val ecPrivateKeySpec = ECPrivateKeySpec(d, ecParameterSpec)
        privateKey = keyFactory.generatePrivate(ecPrivateKeySpec)
    }

    private companion object {
        val ecParameterSpec: ECParameterSpec = ECNamedCurveTable.getParameterSpec("P-256")
        val keyFactory: KeyFactory = KeyFactory.getInstance("ECDSA", "BC")
    }

    override fun sign(bytes: ByteArray): ByteArray {

        val ecdsaSign = Signature.getInstance("SHA3-256withECDSA", "BC")
        ecdsaSign.initSign(privateKey)
        ecdsaSign.update(bytes)

        val signature = ecdsaSign.sign()
        val (r, s) = extractRS(signature)

        val nlen = (ecParameterSpec.n.bitLength() + 7) shr 3
        val flowBytes = ByteArray(2 * nlen)

        val rBytes = r.toByteArray()
        val sBytes = s.toByteArray()

        // occasionally R/S bytes representation has leading zeroes, so make sure we trim them appropriately
        rBytes.copyInto(flowBytes, max(nlen - rBytes.size, 0), max(0, rBytes.size - nlen))
        sBytes.copyInto(flowBytes, max(2 * nlen - sBytes.size, nlen), max(0, sBytes.size - nlen))

        return flowBytes
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
