package org.onflow.sdk

import com.google.protobuf.ByteString
import com.google.protobuf.UnsafeByteOperations
import java.math.BigInteger
import java.time.LocalDateTime
import org.onflow.protobuf.access.Access
import org.onflow.protobuf.entities.AccountOuterClass
import org.onflow.protobuf.entities.BlockHeaderOuterClass
import org.onflow.protobuf.entities.BlockOuterClass
import org.onflow.protobuf.entities.BlockSealOuterClass
import org.onflow.protobuf.entities.CollectionOuterClass
import org.onflow.protobuf.entities.EventOuterClass
import org.onflow.protobuf.entities.TransactionOuterClass
import org.tdf.rlp.RLP
import org.tdf.rlp.RLPCodec

enum class FlowTransactionStatus(val num: Int) {
    UNKNOWN(0),
    PENDING(1),
    FINALIZED(2),
    EXECUTED(3),
    SEALED(4),
    EXPIRED(5);

    companion object {
        @JvmStatic
        fun of(num: Int): FlowTransactionStatus = values()
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
        fun of(id: String): FlowChainId = values()
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
        fun of(value: AccountOuterClass.Account): FLowAccount = FLowAccount(
            address = FlowAddress.of(value.address.toByteArray()),
            balance = value.balance,
            code = FlowCode(value.code.toByteArray()),
            keys = value.keysList.map { FlowAccountKey.of(it) },
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
        fun of(value: AccountOuterClass.AccountKey): FlowAccountKey = FlowAccountKey(
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
            .setIndex(id)
            .setPublicKey(publicKey.byteStringValue)
            .setSignAlgo(signAlgo)
            .setHashAlgo(hashAlgo)
            .setWeight(weight)
            .setSequenceNumber(sequenceNumber)
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
        fun of(value: Access.EventsResponse.Result): FlowEventResult = FlowEventResult(
            blockId = FlowId.of(value.blockId.toByteArray()),
            blockHeight = value.blockHeight,
            events = value.eventsList.map { FlowEvent.of(it) },
            blockTimestamp = value.blockTimestamp.asLocalDateTime()
        )
    }

    @JvmOverloads
    fun builder(builder: Access.EventsResponse.Result.Builder = Access.EventsResponse.Result.newBuilder()): Access.EventsResponse.Result.Builder {
        return builder
            .setBlockId(blockId.byteStringValue)
            .setBlockHeight(blockHeight)
            .addAllEvents(events.map { it.builder().build() })
            .setBlockTimestamp(blockTimestamp.asTimestamp())
    }
}

// https://github.com/onflow/flow-go-sdk/blob/878e5e586e0f060b88c6036cf4b0f6a7ab66d198/client/client.go#L515

data class FlowEvent(
    val type: String,
    val transactionId: FlowId,
    val transactionIndex: Int,
    val eventIndex: Int,
    val payload: FlowEventPayload
) {
    companion object {
        @JvmStatic
        fun of(value: EventOuterClass.Event): FlowEvent = FlowEvent(
            type = value.type,
            transactionId = FlowId.of(value.transactionId.toByteArray()),
            transactionIndex = value.transactionIndex,
            eventIndex = value.eventIndex,
            payload = FlowEventPayload(value.payload.toByteArray())
        )
    }

    val id: String get() = event.id!!
    val event: EventField get() = payload.cdif as EventField

    fun <T : Field<*>> getField(name: String): T? = event[name]
    @Suppress("UNCHECKED")
    operator fun <T> get(name: String): T? = getField<Field<*>>(name) as T
    operator fun contains(name: String): Boolean = name in event

    @JvmOverloads
    fun builder(builder: EventOuterClass.Event.Builder = EventOuterClass.Event.newBuilder()): EventOuterClass.Event.Builder {
        return builder
            .setType(type)
            .setTransactionId(transactionId.byteStringValue)
            .setTransactionIndex(transactionIndex)
            .setEventIndex(eventIndex)
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
        fun of(value: Access.TransactionResultResponse): FlowTransactionResult = FlowTransactionResult(
            status = FlowTransactionStatus.of(value.statusValue),
            statusCode = value.statusCode,
            errorMessage = value.errorMessage,
            events = value.eventsList.map { FlowEvent.of(it) }
        )
    }

    @JvmOverloads
    fun builder(builder: Access.TransactionResultResponse.Builder = Access.TransactionResultResponse.newBuilder()): Access.TransactionResultResponse.Builder {
        return builder
            .setStatus(TransactionOuterClass.TransactionStatus.valueOf(status.name))
            .setStatusCode(statusCode)
            .setErrorMessage(errorMessage)
            .addAllEvents(events.map { it.builder().build() })
    }
}

internal class PayloadEnvelope(
    val script: ByteArray,
    val arguments: List<ByteArray>,
    val referenceBlockId: ByteArray,
    val gasLimit: Long,
    val proposalKeyAddress: ByteArray,
    val proposalKeyIndex: Long,
    val proposalKeySequenceNumber: Long,
    val payer: ByteArray,
    val authorizers: List<ByteArray>
)

internal class AuthorizationEnvelope(
    @RLP(0) val payloadEnvelope: PayloadEnvelope,
    @RLP(1) val signatures: List<CanonicalSignature>
)

internal class PaymentEnvelope(
    @RLP(0) val payloadEnvelope: PayloadEnvelope,
    @RLP(1) val authorizationEnvelope: AuthorizationEnvelope,
    @RLP(2) val signatures: List<CanonicalSignature>
)

internal class CanonicalSignature(
    val signerIndex: Int,
    val keyIndex: Int,
    val signature: ByteArray
)

data class FlowTransaction(
    val script: FlowScript,
    val arguments: List<FlowArgument>,
    val referenceBlockId: FlowId,
    val gasLimit: Long,
    val proposalKey: FlowTransactionProposalKey,
    val payerAddress: FlowAddress,
    val authorizers: List<FlowAddress>,
    val authorizationSignatures: List<FlowTransactionSignature> = emptyList(),
    val paymentSignatures: List<FlowTransactionSignature> = emptyList()
) {

    private val payload: PayloadEnvelope get() = PayloadEnvelope(
        script = script.bytes,
        arguments = arguments.map { it.bytes },
        referenceBlockId = referenceBlockId.bytes,
        gasLimit = gasLimit,
        proposalKeyAddress = proposalKey.address.bytes,
        proposalKeyIndex = proposalKey.keyIndex.toLong(), // TODO: type missmatch here
        proposalKeySequenceNumber = proposalKey.sequenceNumber,
        payer = payerAddress.bytes,
        authorizers = authorizers.map { it.bytes }
    )

    private val authorization: AuthorizationEnvelope get() = AuthorizationEnvelope(
        payloadEnvelope = payload,
        signatures = authorizationSignatures.map {
            CanonicalSignature(
                signerIndex = it.signerIndex,
                keyIndex = it.keyId,
                signature = it.signature.bytes
            )
        }
    )

    private val payment: PaymentEnvelope get() = PaymentEnvelope(
        payloadEnvelope = payload,
        authorizationEnvelope = authorization,
        signatures = paymentSignatures.map {
            CanonicalSignature(
                signerIndex = it.signerIndex,
                keyIndex = it.keyId,
                signature = it.signature.bytes
            )
        }
    )

    val payloadEnvelope: ByteArray get() = RLPCodec.encode(payload)
    val authorizationEnvelope: ByteArray get() = RLPCodec.encode(authorization)
    val paymentEnvelope: ByteArray get() = RLPCodec.encode(payment)

    companion object {
        @JvmStatic
        fun of(value: TransactionOuterClass.Transaction): FlowTransaction = FlowTransaction(
            script = FlowScript(value.script.toByteArray()),
            arguments = value.argumentsList.map { FlowArgument(it.toByteArray()) },
            referenceBlockId = FlowId.of(value.referenceBlockId.toByteArray()),
            gasLimit = value.gasLimit,
            proposalKey = FlowTransactionProposalKey.of(value.proposalKey),
            payerAddress = FlowAddress.of(value.payer.toByteArray()),
            authorizers = value.authorizersList.map { FlowAddress.of(it.toByteArray()) },
            authorizationSignatures = value.payloadSignaturesList.map { FlowTransactionSignature.of(it) },
            paymentSignatures = value.envelopeSignaturesList.map { FlowTransactionSignature.of(it) }
        )
    }

    @JvmOverloads
    fun builder(builder: TransactionOuterClass.Transaction.Builder = TransactionOuterClass.Transaction.newBuilder()): TransactionOuterClass.Transaction.Builder {
        return builder
            .setScript(script.byteStringValue)
            .addAllArguments(arguments.map { it.byteStringValue })
            .setReferenceBlockId(referenceBlockId.byteStringValue)
            .setGasLimit(gasLimit)
            .setProposalKey(proposalKey.builder().build())
            .setPayer(payerAddress.byteStringValue)
            .addAllAuthorizers(authorizers.map { it.byteStringValue })
            .addAllPayloadSignatures(authorizationSignatures.map { it.builder().build() })
    }

    fun signAsProposer(privateKey: String, address: FlowAddress, keyId: Int): FlowTransaction {
        return signAsProposer(Crypto.loadPrivateKey(privateKey), address, keyId)
    }

    fun signAsProposer(privateKey: PrivateKey, address: FlowAddress, keyId: Int): FlowTransaction {
        if (paymentSignatures.isNotEmpty()) {
            throw IllegalStateException("Transaction already has payment signatures")
        } else if (authorizationSignatures.find { it.address == address } != null) {
            throw IllegalStateException("Transaction has already been signed by the proposer")
        }
        val signature = FlowTransactionSignature(
            address = address,
            signerIndex = authorizationSignatures.size,
            keyId = keyId,
            signature = FlowSignature(privateKey.sign(payloadEnvelope))
        )
        return this.copy(
            authorizationSignatures = authorizationSignatures + signature
        )
    }

    fun signAsAuthorizer(privateKey: String, address: FlowAddress, keyId: Int): FlowTransaction {
        return signAsAuthorizer(Crypto.loadPrivateKey(privateKey), address, keyId)
    }

    fun signAsAuthorizer(privateKey: PrivateKey, address: FlowAddress, keyId: Int): FlowTransaction {
        if (paymentSignatures.isNotEmpty()) {
            throw IllegalStateException("Transaction already has payment signatures")
        } else if (authorizationSignatures.find { it.address == address } != null) {
            throw IllegalStateException("Transaction has already been signed by the authorizer")
        }
        val signature = FlowTransactionSignature(
            address = address,
            signerIndex = authorizationSignatures.size,
            keyId = keyId,
            signature = FlowSignature(privateKey.sign(payloadEnvelope))
        )
        return this.copy(
            authorizationSignatures = authorizationSignatures + signature
        )
    }

    fun signAsPayer(privateKey: PrivateKey, address: FlowAddress, keyId: Int): FlowTransaction {
        if (authorizationSignatures.size < 2) {
            throw IllegalStateException("Transaction doesn't have required authorization signatures")
        } else if (authorizationSignatures.find { it.address == proposalKey.address } == null) {
            throw IllegalStateException("Transaction has not yet been signed by the proposer")
        }
        val signature = FlowTransactionSignature(
            address = address,
            signerIndex = paymentSignatures.size,
            keyId = keyId,
            signature = FlowSignature(privateKey.sign(authorizationEnvelope))
        )
        return this.copy(
            paymentSignatures = paymentSignatures + signature
        )
    }

    fun signAsPayer(privateKey: String, address: FlowAddress, keyId: Int): FlowTransaction {
        return signAsPayer(Crypto.loadPrivateKey(privateKey), address, keyId)
    }
}

data class FlowTransactionProposalKey(
    val address: FlowAddress,
    val keyIndex: Int,
    val sequenceNumber: Long
) {
    companion object {
        @JvmStatic
        fun of(value: TransactionOuterClass.Transaction.ProposalKey): FlowTransactionProposalKey =
            FlowTransactionProposalKey(
                address = FlowAddress.of(value.address.toByteArray()),
                keyIndex = value.keyId,
                sequenceNumber = value.sequenceNumber
            )
    }

    @JvmOverloads
    fun builder(builder: TransactionOuterClass.Transaction.ProposalKey.Builder = TransactionOuterClass.Transaction.ProposalKey.newBuilder()): TransactionOuterClass.Transaction.ProposalKey.Builder {
        return builder
            .setAddress(address.byteStringValue)
            .setKeyId(keyIndex)
            .setSequenceNumber(sequenceNumber)
    }
}

data class FlowTransactionSignature(
    val address: FlowAddress,
    val signerIndex: Int,
    val keyId: Int,
    val signature: FlowSignature
) {
    companion object {
        @JvmStatic
        fun of(value: TransactionOuterClass.Transaction.Signature): FlowTransactionSignature =
            FlowTransactionSignature(
                address = FlowAddress.of(value.address.toByteArray()),
                signerIndex = value.keyId, // TODO: what is this vs. keyId
                keyId = value.keyId,
                signature = FlowSignature(value.signature.toByteArray())
            )
    }

    @JvmOverloads
    fun builder(builder: TransactionOuterClass.Transaction.Signature.Builder = TransactionOuterClass.Transaction.Signature.newBuilder()): TransactionOuterClass.Transaction.Signature.Builder {
        return builder
            .setAddress(address.byteStringValue)
            .setKeyId(keyId)
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
        fun of(value: BlockHeaderOuterClass.BlockHeader): FlowBlockHeader = FlowBlockHeader(
            id = FlowId.of(value.id.toByteArray()),
            parentId = FlowId.of(value.parentId.toByteArray()),
            height = value.height
        )
    }

    @JvmOverloads
    fun builder(builder: BlockHeaderOuterClass.BlockHeader.Builder = BlockHeaderOuterClass.BlockHeader.newBuilder()): BlockHeaderOuterClass.BlockHeader.Builder {
        return builder
            .setId(id.byteStringValue)
            .setParentId(parentId.byteStringValue)
            .setHeight(height)
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
        fun of(value: BlockOuterClass.Block) = FlowBlock(
            id = FlowId.of(value.id.toByteArray()),
            parentId = FlowId.of(value.parentId.toByteArray()),
            height = value.height,
            timestamp = value.timestamp.asLocalDateTime(),
            collectionGuarantees = value.collectionGuaranteesList.map { FlowCollectionGuarantee.of(it) },
            blockSeals = value.blockSealsList.map { FlowBlockSeal.of(it) },
            signatures = value.signaturesList.map { FlowSignature(it.toByteArray()) },
        )
    }

    @JvmOverloads
    fun builder(builder: BlockOuterClass.Block.Builder = BlockOuterClass.Block.newBuilder()): BlockOuterClass.Block.Builder {
        return builder
            .setId(id.byteStringValue)
            .setParentId(parentId.byteStringValue)
            .setHeight(height)
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
        fun of(value: CollectionOuterClass.CollectionGuarantee) = FlowCollectionGuarantee(
            id = FlowId.of(value.collectionId.toByteArray()),
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
        fun of(value: BlockSealOuterClass.BlockSeal) = FlowBlockSeal(
            id = FlowId.of(value.blockId.toByteArray()),
            executionReceiptId = FlowId.of(value.executionReceiptId.toByteArray()),
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
        fun of(value: CollectionOuterClass.Collection) = FlowCollection(
            id = FlowId.of(value.id.toByteArray()),
            transactionIds = value.transactionIdsList.map { FlowId.of(it.toByteArray()) }
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
    val base16Value: String get() = bytes.bytesToHex()
    val stringValue: String get() = String(bytes)
    val byteStringValue: ByteString get() = UnsafeByteOperations.unsafeWrap(bytes)
    val integerValue: BigInteger get() = BigInteger(base16Value, 16)
}

data class FlowAddress private constructor(override val bytes: ByteArray) : BytesHolder {
    companion object {
        @JvmStatic
        fun of(bytes: ByteArray): FlowAddress = FlowAddress(fixedSize(bytes, 8))
    }
    constructor(hex: String) : this(hex.hexToBytes())
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

    constructor(cdif: Field<*>) : this(Flow.encodeCDIF(cdif))

    private var _cdif: Field<*>? = null
    val cdif: Field<*> get() {
        if (_cdif == null) {
            _cdif = Flow.decodeCDIF(bytes)
        }
        return _cdif!!
    }

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
    constructor(scipt: String) : this(scipt.encodeToByteArray())
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

    constructor(cdif: Field<*>) : this(Flow.encodeCDIF(cdif))

    private var _cdif: Field<*>? = null
    val cdif: Field<*> get() {
        if (_cdif == null) {
            _cdif = Flow.decodeCDIF(bytes)
        }
        return _cdif!!
    }

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

data class FlowId private constructor(override val bytes: ByteArray) : BytesHolder {
    companion object {
        @JvmStatic
        fun of(bytes: ByteArray): FlowId = FlowId(fixedSize(bytes, 32))
    }
    constructor(hex: String) : this(hex.hexToBytes())
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

data class FlowEventPayload(override val bytes: ByteArray) : BytesHolder {

    constructor(cdif: Field<*>) : this(Flow.encodeCDIF(cdif))

    private var _cdif: Field<*>? = null
    val cdif: Field<*> get() {
        if (_cdif == null) {
            _cdif = Flow.decodeCDIF(bytes)
        }
        return _cdif!!
    }

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




