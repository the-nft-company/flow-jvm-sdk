package org.onflow.sdk

import com.google.common.io.BaseEncoding
import com.google.protobuf.ByteString
import com.google.protobuf.Timestamp
import com.google.protobuf.UnsafeByteOperations
import org.onflow.protobuf.access.Access
import org.onflow.protobuf.entities.AccountOuterClass
import org.onflow.protobuf.entities.BlockHeaderOuterClass
import org.onflow.protobuf.entities.BlockOuterClass
import org.onflow.protobuf.entities.BlockSealOuterClass
import org.onflow.protobuf.entities.CollectionOuterClass
import org.onflow.protobuf.entities.EventOuterClass
import org.onflow.protobuf.entities.TransactionOuterClass
import java.math.BigInteger
import java.time.LocalDateTime
import java.time.ZoneOffset

enum class FlowTransactionStatus(val num: Int) {
    UNKNOWN(0),
    PENDING(1),
    FINALIZED(2),
    EXECUTED(3),
    SEALED(4),
    EXPIRED(5);

    companion object {
        @JvmStatic
        fun fromNum(num: Int): FlowTransactionStatus = values()
            .find { it.num == num }
            ?: throw IllegalArgumentException("Unknown TransactionStatus: $num")
    }
}

enum class FlowChainId(
    val id: String,
    val endpoint: String,
    val poort: Int
) {
    UNKNOWN("unknown", "", -1),
    MAINNET("flow-mainnet", "access.mainnet.nodes.onflow.org", 9000),
    TESTNET("flow-testnet", "access.devnet.nodes.onflow.org", 9000),
    CANARYNET("flow-canarynet", "access.canary.nodes.onflow.org", 9000),
    EMULATOR("flow-emulator", "localhost", 3569);

    companion object {
        @JvmStatic
        fun fromId(id: String): FlowChainId = values()
            .find { it.id == id }
            ?: UNKNOWN
    }
}

data class FLowAccount(
    val address: FlowAddress,
    val balance: Long,
    @Deprecated(
        message = "use contracts instead",
        replaceWith = ReplaceWith("contracts")
    )
    val code: FlowCode,
    val keys: List<FlowAccountKey>,
    val contracts: Map<String, FlowCode>
) {
    companion object {
        @JvmStatic
        fun from(value: AccountOuterClass.Account): FLowAccount = FLowAccount(
            address = FlowAddress(value.address.toByteArray()),
            balance = value.balance,
            code = FlowCode(value.code.toByteArray()),
            keys = value.keysList.map { FlowAccountKey.from(it) },
            contracts = value.contractsMap.mapValues { FlowCode(it.value.toByteArray()) }
        )
    }

    @JvmOverloads
    fun builder(builder: AccountOuterClass.Account.Builder = AccountOuterClass.Account.newBuilder()): AccountOuterClass.Account.Builder {
        return builder
            .setAddress(address.byteStringValue)
            .setBalance(balance)
            .setCode(code.byteStringValue)
            .addAllKeys(keys.map { it.builder().build() })
            .putAllContracts(contracts.mapValues { it.value.byteStringValue })
    }
}

data class FlowAccountKey(
    val id: Int,
    val publicKey: FlowPublicKey,
    val signAlgo: Int,
    val hashAlgo: Int,
    val weight: Int,
    val sequenceNumber: Int,
    val revoked: Boolean
) {
    companion object {
        @JvmStatic
        fun from(value: AccountOuterClass.AccountKey): FlowAccountKey = FlowAccountKey(
            id = value.index,
            publicKey = FlowPublicKey(value.publicKey.toByteArray()),
            signAlgo = value.index,
            hashAlgo = value.index,
            weight = value.index,
            sequenceNumber = value.index,
            revoked = value.revoked
        )
    }

    @JvmOverloads
    fun builder(builder: AccountOuterClass.AccountKey.Builder = AccountOuterClass.AccountKey.newBuilder()): AccountOuterClass.AccountKey.Builder {
        return builder
            .setIndex(id.toInt())
            .setPublicKey(publicKey.byteStringValue)
            .setSignAlgo(signAlgo.toInt())
            .setHashAlgo(hashAlgo.toInt())
            .setWeight(weight.toInt())
            .setSequenceNumber(sequenceNumber.toInt())
            .setRevoked(revoked)
    }
}

data class FlowEventResult(
    val blockId: FlowId,
    val blockHeight: Long,
    val events: List<FlowEvent>,
    val blockTimestamp: LocalDateTime
) {
    companion object {
        @JvmStatic
        fun from(value: Access.EventsResponse.Result): FlowEventResult = FlowEventResult(
            blockId = FlowId(value.blockId.toByteArray()),
            blockHeight = value.blockHeight,
            events = value.eventsList.map { FlowEvent.from(it) },
            blockTimestamp = value.blockTimestamp.asLocalDateTime()
        )
    }

    @JvmOverloads
    fun builder(builder: Access.EventsResponse.Result.Builder = Access.EventsResponse.Result.newBuilder()): Access.EventsResponse.Result.Builder {
        return builder
            .setBlockId(blockId.byteStringValue)
            .setBlockHeight(blockHeight.toLong())
            .addAllEvents(events.map { it.builder().build() })
            .setBlockTimestamp(blockTimestamp.asTimestamp())
    }
}

data class FlowEvent(
    val type: String,
    val transactionId: FlowId,
    val transactionIndex: Int,
    val eventIndex: Int,
    val payload: FlowEventPayload
) {
    companion object {
        @JvmStatic
        fun from(value: EventOuterClass.Event): FlowEvent = FlowEvent(
            type = value.type,
            transactionId = FlowId(value.transactionId.toByteArray()),
            transactionIndex = value.transactionIndex,
            eventIndex = value.eventIndex,
            payload = FlowEventPayload(value.payload.toByteArray())
        )
    }

    @JvmOverloads
    fun builder(builder: EventOuterClass.Event.Builder = EventOuterClass.Event.newBuilder()): EventOuterClass.Event.Builder {
        return builder
            .setType(type)
            .setTransactionId(transactionId.byteStringValue)
            .setTransactionIndex(transactionIndex.toInt())
            .setEventIndex(eventIndex.toInt())
            .setPayload(payload.byteStringValue)
    }
}

data class FlowTransactionResult(
    val status: FlowTransactionStatus,
    val statusCode: Int,
    val errorMessage: String,
    val events: List<FlowEvent>
) {
    companion object {
        @JvmStatic
        fun from(value: Access.TransactionResultResponse): FlowTransactionResult = FlowTransactionResult(
            status = FlowTransactionStatus.fromNum(value.statusValue),
            statusCode = value.statusCode,
            errorMessage = value.errorMessage,
            events = value.eventsList.map { FlowEvent.from(it) }
        )
    }

    @JvmOverloads
    fun builder(builder: Access.TransactionResultResponse.Builder = Access.TransactionResultResponse.newBuilder()): Access.TransactionResultResponse.Builder {
        return builder
            .setStatus(TransactionOuterClass.TransactionStatus.valueOf(status.name))
            .setStatusCode(statusCode.toInt())
            .setErrorMessage(errorMessage)
            .addAllEvents(events.map { it.builder().build() })
    }
}

data class FlowTransaction(
    val script: FlowScript,
    val arguments: List<FlowArgument>,
    val referenceBlockId: FlowId,
    val gasLimit: Long,
    val proposalKey: FlowTransactionProposalKey,
    val payerAddress: FlowAddress,
    val authorizers: List<FlowAddress>,
    val payloadSignatures: List<FlowTransactionSignature>,
    val envelopeSignatures: List<FlowTransactionSignature>
) {
    companion object {
        @JvmStatic
        fun from(value: TransactionOuterClass.Transaction): FlowTransaction = FlowTransaction(
            script = FlowScript(value.script.toByteArray()),
            arguments = value.argumentsList.map { FlowArgument(it.toByteArray()) },
            referenceBlockId = FlowId(value.referenceBlockId.toByteArray()),
            gasLimit = value.gasLimit,
            proposalKey = FlowTransactionProposalKey.from(value.proposalKey),
            payerAddress = FlowAddress(value.toByteArray()),
            authorizers = value.authorizersList.map { FlowAddress(it.toByteArray()) },
            payloadSignatures = value.payloadSignaturesList.map { FlowTransactionSignature.from(it) },
            envelopeSignatures = value.envelopeSignaturesList.map { FlowTransactionSignature.from(it) }
        )
    }

    @JvmOverloads
    fun builder(builder: TransactionOuterClass.Transaction.Builder = TransactionOuterClass.Transaction.newBuilder()): TransactionOuterClass.Transaction.Builder {
        return builder
            .setScript(script.byteStringValue)
            .addAllArguments(arguments.map { it.byteStringValue })
            .setReferenceBlockId(referenceBlockId.byteStringValue)
            .setGasLimit(gasLimit.toLong())
            .setProposalKey(proposalKey.builder().build())
            .setPayer(payerAddress.byteStringValue)
            .addAllAuthorizers(authorizers.map { it.byteStringValue })
            .addAllPayloadSignatures(payloadSignatures.map { it.builder().build() })
    }
}

data class FlowTransactionProposalKey(
    val address: FlowAddress,
    val keyId: Int,
    val sequenceNumber: Long
) {
    companion object {
        @JvmStatic
        fun from(value: TransactionOuterClass.Transaction.ProposalKey): FlowTransactionProposalKey =
            FlowTransactionProposalKey(
                address = FlowAddress(value.address.toByteArray()),
                keyId = value.keyId,
                sequenceNumber = value.sequenceNumber
            )
    }

    @JvmOverloads
    fun builder(builder: TransactionOuterClass.Transaction.ProposalKey.Builder = TransactionOuterClass.Transaction.ProposalKey.newBuilder()): TransactionOuterClass.Transaction.ProposalKey.Builder {
        return builder
            .setAddress(address.byteStringValue)
            .setKeyId(keyId.toInt())
            .setSequenceNumber(sequenceNumber.toLong())
    }
}

data class FlowTransactionSignature(
    val address: FlowAddress,
    val keyId: Int,
    val signature: FlowSignature
) {
    companion object {
        @JvmStatic
        fun from(value: TransactionOuterClass.Transaction.Signature): FlowTransactionSignature =
            FlowTransactionSignature(
                address = FlowAddress(value.address.toByteArray()),
                keyId = value.keyId,
                signature = FlowSignature(value.signature.toByteArray())
            )
    }

    @JvmOverloads
    fun builder(builder: TransactionOuterClass.Transaction.Signature.Builder = TransactionOuterClass.Transaction.Signature.newBuilder()): TransactionOuterClass.Transaction.Signature.Builder {
        return builder
            .setAddress(address.byteStringValue)
            .setKeyId(keyId.toInt())
            .setSignature(signature.byteStringValue)
    }
}

data class FlowBlockHeader(
    val id: FlowId,
    val parentId: FlowId,
    val height: Long
) {
    companion object {
        @JvmStatic
        fun from(value: BlockHeaderOuterClass.BlockHeader): FlowBlockHeader = FlowBlockHeader(
            id = FlowId(value.id.toByteArray()),
            parentId = FlowId(value.parentId.toByteArray()),
            height = value.height
        )
    }

    @JvmOverloads
    fun builder(builder: BlockHeaderOuterClass.BlockHeader.Builder = BlockHeaderOuterClass.BlockHeader.newBuilder()): BlockHeaderOuterClass.BlockHeader.Builder {
        return builder
            .setId(id.byteStringValue)
            .setParentId(parentId.byteStringValue)
            .setHeight(height.toLong())
    }
}

data class FlowBlock(
    val id: FlowId,
    val parentId: FlowId,
    val height: Long,
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
            height = value.height,
            timestamp = value.timestamp.asLocalDateTime(),
            collectionGuarantees = value.collectionGuaranteesList.map { FlowCollectionGuarantee.from(it) },
            blockSeals = value.blockSealsList.map { FlowBlockSeal.from(it) },
            signatures = value.signaturesList.map { FlowSignature(it.toByteArray()) },
        )
    }

    @JvmOverloads
    fun builder(builder: BlockOuterClass.Block.Builder = BlockOuterClass.Block.newBuilder()): BlockOuterClass.Block.Builder {
        return builder
            .setId(id.byteStringValue)
            .setParentId(parentId.byteStringValue)
            .setHeight(height.toLong())
            .setTimestamp(timestamp.asTimestamp())
            .addAllCollectionGuarantees(collectionGuarantees.map { it.builder().build() })
            .addAllBlockSeals(blockSeals.map { it.builder().build() })
            .addAllSignatures(signatures.map { it.byteStringValue })
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

    @JvmOverloads
    fun builder(builder: CollectionOuterClass.CollectionGuarantee.Builder = CollectionOuterClass.CollectionGuarantee.newBuilder()): CollectionOuterClass.CollectionGuarantee.Builder {
        return builder
            .setCollectionId(id.byteStringValue)
            .addAllSignatures(signatures.map { it.byteStringValue })
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

    @JvmOverloads
    fun builder(builder: BlockSealOuterClass.BlockSeal.Builder = BlockSealOuterClass.BlockSeal.newBuilder()): BlockSealOuterClass.BlockSeal.Builder {
        return builder
            .setBlockId(id.byteStringValue)
            .setExecutionReceiptId(executionReceiptId.byteStringValue)
            .addAllExecutionReceiptSignatures(executionReceiptSignatures.map { it.byteStringValue })
            .addAllResultApprovalSignatures(resultApprovalSignatures.map { it.byteStringValue })
    }
}

data class FlowCollection(
    val id: FlowId,
    val transactionIds: List<FlowId>
) {
    companion object {
        @JvmStatic
        fun from(value: CollectionOuterClass.Collection) = FlowCollection(
            id = FlowId(value.id.toByteArray()),
            transactionIds = value.transactionIdsList.map { FlowId(it.toByteArray()) }
        )
    }

    @JvmOverloads
    fun builder(builder: CollectionOuterClass.Collection.Builder = CollectionOuterClass.Collection.newBuilder()): CollectionOuterClass.Collection.Builder {
        return builder
            .setId(id.byteStringValue)
            .addAllTransactionIds(transactionIds.map { it.byteStringValue })
    }
}

interface BytesHolder {
    val bytes: ByteArray
    val base16Value: String get() = bytes.base16Encode()
    val stringValue: String get() = String(bytes)
    val byteStringValue: ByteString get() = UnsafeByteOperations.unsafeWrap(bytes)
    val integerValue: BigInteger get() = BigInteger(base16Value, 16)
}

abstract class SizeEnforcingBytesHolder(bytes: ByteArray, size: Int) : BytesHolder {
    override val bytes: ByteArray

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

data class FlowAddress(override val bytes: ByteArray) : SizeEnforcingBytesHolder(bytes, 8) {
    constructor(hex: String) : this(hex.base16Decode())

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

data class FlowId(override val bytes: ByteArray) : SizeEnforcingBytesHolder(bytes, 32) {
    constructor(hex: String) : this(hex.base16Decode())

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

fun LocalDateTime.asTimestamp(): Timestamp = Timestamp.newBuilder()
    .setSeconds(this.toEpochSecond(ZoneOffset.UTC))
    .setNanos(this.nano)
    .build()
