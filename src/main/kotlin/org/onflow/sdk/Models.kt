package org.onflow.sdk

import com.google.common.io.BaseEncoding
import com.google.protobuf.ByteString
import com.google.protobuf.Timestamp
import org.onflow.protobuf.entities.BlockHeaderOuterClass
import org.onflow.protobuf.entities.BlockOuterClass
import org.onflow.protobuf.entities.BlockSealOuterClass
import org.onflow.protobuf.entities.CollectionOuterClass
import java.math.BigInteger
import java.time.LocalDateTime
import java.time.ZoneOffset

enum class TransactionStatus(val num: Int) {
    UNKNOWN(0),
    PENDING(1),
    FINALIZED(2),
    EXECUTED(3),
    SEALED(4),
    EXPIRED(5);
    companion object {
        @JvmStatic
        fun fromNum(num: Int): TransactionStatus = values()
            .find { it.num == num }
            ?: throw IllegalArgumentException("Unknown TransactionStatus: $num")
    }
}

enum class ChainId(val id: String) {
    UNKNOWN("unknown"),
    PENDING("flow-mainnet"),
    FINALIZED("flow-testnet"),
    EXECUTED("flow-emulator");
    companion object {
        @JvmStatic
        fun fromId(id: String): ChainId = values()
            .find { it.id == id }
            ?: UNKNOWN
    }
}

data class FLowAccount(
    val address: FlowAddress,
    val balance: BigInteger,
    @Deprecated(
        message = "use contracts instead",
        replaceWith = ReplaceWith("contracts")
    )
    val code: FlowCode,
    val keys: List<AccountKey>,
    val contracts: Map<String, FlowCode>
)

data class AccountKey(
    val id: BigInteger,
    val publicKey: FlowPublicKey,
    val signAlgo: BigInteger,
    val hashAlgo: BigInteger,
    val weight: BigInteger,
    val sequenceNumber: BigInteger,
    val revoked: Boolean
)

data class FlowEventResult(
    val blockId: FlowId,
    val blockHeight: BigInteger,
    val events: List<FlowEvent>,
    val blockTimestamp: LocalDateTime
)

data class FlowEvent(
    val type: String,
    val transactionId: FlowId,
    val transactionIndex: BigInteger,
    val eventIndex: BigInteger,
    val payload: FlowEventPayload
)

data class FlowTransactionResult(
    val status: TransactionStatus,
    val statusCode: BigInteger,
    val errorMessage: String,
    val events: List<FlowEvent>
)

data class FlowTransaction(
    val script: FlowScript,
    val arguments: List<FlowArgument>,
    val referenceBlockId: FlowId,
    val gasLimit: BigInteger,
    val proposalKey: FlowTransactionProposalKey,
    val payerAddress: Address,
    val authorizers: List<Address>,
    val payloadSignatures: List<TransactionSignature>,
    val envelopeSignatures: List<TransactionSignature>
)

data class FlowTransactionProposalKey(
    val address: Address,
    val keyId: BigInteger,
    val sequenceNumber: BigInteger
)

data class FlowTransactionSignature(
    val address: Address,
    val keyId: BigInteger,
    val signature: FlowSignature
)

data class FlowBlockHeader(
    val id: FlowId,
    val parentId: FlowId,
    val height: BigInteger
) {
    companion object {
        @JvmStatic
        fun from(value: BlockHeaderOuterClass.BlockHeader): FlowBlockHeader = FlowBlockHeader(
            id = FlowId(value.id.toByteArray()),
            parentId = FlowId(value.parentId.toByteArray()),
            height = value.height.toBigInteger()
        )
    }
}

data class FlowBlock(
    val id: FlowId,
    val parentId: FlowId,
    val height: BigInteger,
    val timestamp: LocalDateTime,
    val collectionGuarantees: List<FlowCollectionGuarantee>,
    val blockSeals: List<FlowBlockSeal>,
    val signatures: List<FlowSignature>
) {
    companion object {
        @JvmStatic
        fun from(value: BlockOuterClass.Block) = FlowBlock(
            id = FlowId(value.id.toByteArray()),
            parentId = FlowId(value.parentId.toByteArray()),
            height = value.height.toBigInteger(),
            timestamp = value.timestamp.asLocalDateTime(),
            collectionGuarantees = value.collectionGuaranteesList.map { FlowCollectionGuarantee.from(it) },
            blockSeals = value.blockSealsList.map { FlowBlockSeal.from(it) },
            signatures = value.signaturesList.map { FlowSignature(it.toByteArray()) },
        )
    }
}

data class FlowCollectionGuarantee(
    val id: FlowId,
    val signatures: List<FlowSignature>
) {
    companion object {
        @JvmStatic
        fun from(value: CollectionOuterClass.CollectionGuarantee) = FlowCollectionGuarantee(
            id = FlowId(value.collectionId.toByteArray()),
            signatures = value.signaturesList.map { FlowSignature(it.toByteArray()) }
        )
    }
}

data class FlowBlockSeal(
    val id: FlowId,
    val executionReceiptId: FlowId,
    val executionReceiptSignatures: List<FlowSignature>,
    val resultApprovalSignatures: List<FlowSignature>
) {
    companion object {
        @JvmStatic
        fun from(value: BlockSealOuterClass.BlockSeal) = FlowBlockSeal(
            id = FlowId(value.blockId.toByteArray()),
            executionReceiptId = FlowId(value.executionReceiptId.toByteArray()),
            executionReceiptSignatures = value.executionReceiptSignaturesList.map { FlowSignature(it.toByteArray()) },
            resultApprovalSignatures = value.executionReceiptSignaturesList.map { FlowSignature(it.toByteArray()) }
        )
    }
}

data class FlowCollection(
    val id: FlowId,
    val transactionIds: List<FlowId>
)

interface BytesHolder {
    val bytes: ByteArray
    val base16Value: String get() = bytes.base16Encode()
    val stringValue: String get() = String(bytes)
    val byteStringValue: ByteString get() = ByteString.copyFrom(bytes)
    val integerValue: BigInteger get() = BigInteger(base16Value, 16)
}

data class FlowAddress(override val bytes: ByteArray) : BytesHolder {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as FlowAddress
        if (!bytes.contentEquals(other.bytes)) return false
        return true
    }
    override fun hashCode(): Int {
        return bytes.contentHashCode()
    }
}

data class FlowArgument(override val bytes: ByteArray) : BytesHolder {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as FlowArgument
        if (!bytes.contentEquals(other.bytes)) return false
        return true
    }
    override fun hashCode(): Int {
        return bytes.contentHashCode()
    }
}

data class FlowScript(override val bytes: ByteArray) : BytesHolder {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as FlowScript
        if (!bytes.contentEquals(other.bytes)) return false
        return true
    }
    override fun hashCode(): Int {
        return bytes.contentHashCode()
    }
}

data class FlowScriptResponse(override val bytes: ByteArray) : BytesHolder {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as FlowScriptResponse
        if (!bytes.contentEquals(other.bytes)) return false
        return true
    }
    override fun hashCode(): Int {
        return bytes.contentHashCode()
    }
}

data class FlowSignature(override val bytes: ByteArray) : BytesHolder {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as FlowSignature
        if (!bytes.contentEquals(other.bytes)) return false
        return true
    }
    override fun hashCode(): Int {
        return bytes.contentHashCode()
    }
}

data class FlowId(override val bytes: ByteArray) : BytesHolder {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as FlowId
        if (!bytes.contentEquals(other.bytes)) return false
        return true
    }
    override fun hashCode(): Int {
        return bytes.contentHashCode()
    }
}

data class FlowEventPayload(override val bytes: ByteArray) : BytesHolder {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as FlowEventPayload
        if (!bytes.contentEquals(other.bytes)) return false
        return true
    }
    override fun hashCode(): Int {
        return bytes.contentHashCode()
    }
}

data class FlowCode(override val bytes: ByteArray) : BytesHolder {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as FlowCode
        if (!bytes.contentEquals(other.bytes)) return false
        return true
    }
    override fun hashCode(): Int {
        return bytes.contentHashCode()
    }
}

data class FlowPublicKey(override val bytes: ByteArray) : BytesHolder {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as FlowPublicKey
        if (!bytes.contentEquals(other.bytes)) return false
        return true
    }
    override fun hashCode(): Int {
        return bytes.contentHashCode()
    }
}

data class FlowSnapshot(override val bytes: ByteArray) : BytesHolder {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as FlowSnapshot
        if (!bytes.contentEquals(other.bytes)) return false
        return true
    }
    override fun hashCode(): Int {
        return bytes.contentHashCode()
    }
}

fun ByteArray.base16Encode() = BaseEncoding.base16().lowerCase().encode(this)

fun String.base16Decode() = BaseEncoding.base16().lowerCase().decode(this)

fun Timestamp.asLocalDateTime(): LocalDateTime = LocalDateTime.ofEpochSecond(this.seconds, this.nanos, ZoneOffset.UTC)
