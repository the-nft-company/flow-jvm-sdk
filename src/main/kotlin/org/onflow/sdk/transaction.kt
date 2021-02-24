package org.onflow.sdk

import com.google.common.io.BaseEncoding
import org.tdf.rlp.RLP
import org.tdf.rlp.RLPCodec
import org.tdf.rlp.RLPDecoding

abstract class ByteWrapper(bytes: ByteArray, size: Int) {
    val bytes: ByteArray

    init {
        if (bytes.size > size) {
            throw IllegalArgumentException("${this.javaClass.name} must have no more than $size bytes long")
        }
        if (bytes.size < size) {
            this.bytes = ByteArray(size - bytes.size).plus(bytes)
        } else {
            this.bytes = bytes
        }
    }
}

class Identifier(bytes: ByteArray) : ByteWrapper(bytes, size) {
    companion object {
        const val size = 32
    }

    constructor(hex: String) : this(BaseEncoding.base16().lowerCase().decode(hex.toLowerCase()))

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        return true
    }

    override fun hashCode(): Int {
        return javaClass.hashCode()
    }
}

class Address(bytes: ByteArray) : ByteWrapper(bytes, size) {

    companion object {
        const val size = 8
    }

    constructor(hex: String) : this(BaseEncoding.base16().lowerCase().decode(hex.toLowerCase()))

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        return true
    }

    override fun hashCode(): Int {
        return javaClass.hashCode()
    }
}

data class ProposalKey(
    val Address: Address,
    val KeyIndex: Long,
    val SequenceNumber: Long,
)

data class TransactionSignature(
    val address: Address,
    val signerIndex: Int,
    val keyIndex: Int,
    val signature: ByteArray
) {

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as TransactionSignature

        if (address != other.address) return false
        if (signerIndex != other.signerIndex) return false
        if (keyIndex != other.keyIndex) return false
        if (!signature.contentEquals(other.signature)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = address.hashCode()
        result = 31 * result + signerIndex
        result = 31 * result + keyIndex
        result = 31 * result + signature.contentHashCode()
        return result
    }


    class Wrapper(
        val signerIndex: Int,
        val keyIndex: Int,
        val signature: ByteArray
    )

    fun wrapper(): Wrapper {
        return Wrapper(
            this.signerIndex,
            this.keyIndex,
            this.signature
        )
    }

}

data class Transaction(
    val script: ByteArray,
    val arguments: List<ByteArray>,
    val referenceBlockID: Identifier,
    val gasLimit: Long,
    val proposalKey: ProposalKey,
    val payer: Address,
    val authorizers: List<Address>,
    val payloadSignatures: List<TransactionSignature> = emptyList(),
    val envelopSignatures: List<TransactionSignature> = emptyList(),
) {

    class PayloadWrapper(
        val script: ByteArray,
        val arguments: List<ByteArray>,
        val referenceBlockID: ByteArray,
        val gasLimit: Long,
        val proposalKeyAddress: ByteArray,
        val proposalKeyIndex: Long,
        val proposalKeySequenceNumber: Long,
        val payer: ByteArray,
        val authorizers: List<ByteArray>
    )

    class EnvelopeWrapper(
        @RLP(0)
        val payloadWrapper: PayloadWrapper,

        @RLP(1)
        val payloadSignatures: List<TransactionSignature.Wrapper>
    )


    fun payloadWrapper(): PayloadWrapper {
        return PayloadWrapper(
            this.script,
            this.arguments,
            this.referenceBlockID.bytes,
            this.gasLimit,
            this.proposalKey.Address.bytes,
            this.proposalKey.KeyIndex,
            this.proposalKey.SequenceNumber,
            this.payer.bytes,
            this.authorizers.map { it.bytes },
        )
    }

    fun envelopeWrapper(): EnvelopeWrapper {
        return EnvelopeWrapper(
            payloadWrapper(),
            this.payloadSignatures.map { it.wrapper() }
        )
    }

    fun envelopCanonicalForm(): ByteArray {
        val ew = envelopeWrapper()
        return RLPCodec.encode(ew)
    }

    fun payloadCanonicalForm(): ByteArray {
        val pw = payloadWrapper()
        return RLPCodec.encode(pw)
    }


    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Transaction

        if (!script.contentEquals(other.script)) return false
        if (arguments != other.arguments) return false
        if (referenceBlockID != other.referenceBlockID) return false
        if (gasLimit != other.gasLimit) return false
        if (proposalKey != other.proposalKey) return false
        if (payer != other.payer) return false
        if (authorizers != other.authorizers) return false
        if (payloadSignatures != other.payloadSignatures) return false
        if (envelopSignatures != other.envelopSignatures) return false

        return true
    }

    override fun hashCode(): Int {
        var result = script.contentHashCode()
        result = 31 * result + arguments.hashCode()
        result = 31 * result + referenceBlockID.hashCode()
        result = 31 * result + gasLimit.hashCode()
        result = 31 * result + proposalKey.hashCode()
        result = 31 * result + payer.hashCode()
        result = 31 * result + authorizers.hashCode()
        result = 31 * result + payloadSignatures.hashCode()
        result = 31 * result + envelopSignatures.hashCode()
        return result
    }
}


