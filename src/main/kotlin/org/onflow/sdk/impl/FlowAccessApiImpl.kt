package org.onflow.sdk.impl

import org.onflow.protobuf.access.Access
import org.onflow.protobuf.access.AccessAPIGrpc
import org.onflow.sdk.FlowAccount
import org.onflow.sdk.FlowAccessApi
import org.onflow.sdk.FlowAddress
import org.onflow.sdk.FlowBlock
import org.onflow.sdk.FlowBlockHeader
import org.onflow.sdk.FlowChainId
import org.onflow.sdk.FlowCollection
import org.onflow.sdk.FlowEventResult
import org.onflow.sdk.FlowId
import org.onflow.sdk.FlowScript
import org.onflow.sdk.FlowScriptResponse
import org.onflow.sdk.FlowSnapshot
import org.onflow.sdk.FlowTransaction
import org.onflow.sdk.FlowTransactionResult

class FlowAccessApiImpl(
    private val api: AccessAPIGrpc.AccessAPIBlockingStub
) : FlowAccessApi {

    override fun ping() {
        api.ping(
            Access.PingRequest.newBuilder()
                .build()
        )
    }

    override fun getLatestBlockHeader(): FlowBlockHeader {
        val ret = api.getLatestBlockHeader(
            Access.GetLatestBlockHeaderRequest.newBuilder()
                .build()
        )
        return FlowBlockHeader.of(ret.block)
    }

    override fun getBlockHeaderById(id: FlowId): FlowBlockHeader? {
        val ret = api.getBlockHeaderByID(
            Access.GetBlockHeaderByIDRequest.newBuilder()
                .setId(id.byteStringValue)
                .build()
        )
        return if (ret.hasBlock()) {
            FlowBlockHeader.of(ret.block)
        } else {
            null
        }
    }

    override fun getBlockHeaderByHeight(height: Long): FlowBlockHeader? {
        val ret = api.getBlockHeaderByHeight(
            Access.GetBlockHeaderByHeightRequest.newBuilder()
                .setHeight(height)
                .build()
        )
        return if (ret.hasBlock()) {
            FlowBlockHeader.of(ret.block)
        } else {
            null
        }
    }

    override fun getLatestBlock(sealed: Boolean): FlowBlock {
        val ret = api.getLatestBlock(
            Access.GetLatestBlockRequest.newBuilder()
                .build()
        )
        return FlowBlock.of(ret.block)
    }

    override fun getBlockById(id: FlowId): FlowBlock? {
        val ret = api.getBlockByID(
            Access.GetBlockByIDRequest.newBuilder()
                .setId(id.byteStringValue)
                .build()
        )
        return if (ret.hasBlock()) {
            FlowBlock.of(ret.block)
        } else {
            null
        }
    }

    override fun getBlockByHeight(height: Long): FlowBlock? {
        val ret = api.getBlockByHeight(
            Access.GetBlockByHeightRequest.newBuilder()
                .setHeight(height)
                .build()
        )
        return if (ret.hasBlock()) {
            FlowBlock.of(ret.block)
        } else {
            null
        }
    }

    override fun getCollectionById(id: FlowId): FlowCollection? {
        val ret = api.getCollectionByID(
            Access.GetCollectionByIDRequest.newBuilder()
                .setId(id.byteStringValue)
                .build()
        )
        return if (ret.hasCollection()) {
            FlowCollection.of(ret.collection)
        } else {
            null
        }
    }

    override fun sendTransaction(transaction: FlowTransaction): FlowId {
        val ret = api.sendTransaction(
            Access.SendTransactionRequest.newBuilder()
                .setTransaction(transaction.builder().build())
                .build()
        )
        return FlowId.of(ret.id.toByteArray())
    }

    override fun getTransactionById(id: FlowId): FlowTransaction? {
        val ret = api.getTransaction(
            Access.GetTransactionRequest.newBuilder()
                .setId(id.byteStringValue)
                .build()
        )
        return if (ret.hasTransaction()) {
            FlowTransaction.of(ret.transaction)
        } else {
            null
        }
    }

    override fun getTransactionResultById(id: FlowId): FlowTransactionResult? {
        val ret = api.getTransactionResult(
            Access.GetTransactionRequest.newBuilder()
                .setId(id.byteStringValue)
                .build()
        )
        return FlowTransactionResult.of(ret)
    }

    override fun getAccountByAddress(addresss: FlowAddress): FlowAccount? {
        val ret = api.getAccount(
            Access.GetAccountRequest.newBuilder()
                .setAddress(addresss.byteStringValue)
                .build()
        )
        return if (ret.hasAccount()) {
            FlowAccount.of(ret.account)
        } else {
            null
        }
    }

    override fun getAccountAtLatestBlock(addresss: FlowAddress): FlowAccount? {
        val ret = api.getAccountAtLatestBlock(
            Access.GetAccountAtLatestBlockRequest.newBuilder()
                .setAddress(addresss.byteStringValue)
                .build()
        )
        return if (ret.hasAccount()) {
            FlowAccount.of(ret.account)
        } else {
            null
        }
    }

    override fun getAccountByBlockHeight(addresss: FlowAddress, height: Long): FlowAccount? {
        val ret = api.getAccountAtBlockHeight(
            Access.GetAccountAtBlockHeightRequest.newBuilder()
                .setAddress(addresss.byteStringValue)
                .setBlockHeight(height)
                .build()
        )
        return if (ret.hasAccount()) {
            FlowAccount.of(ret.account)
        } else {
            null
        }
    }

    override fun executeScriptAtLatestBlock(script: FlowScript): FlowScriptResponse {
        val ret = api.executeScriptAtLatestBlock(
            Access.ExecuteScriptAtLatestBlockRequest.newBuilder()
                .setScript(script.byteStringValue)
                .build()
        )
        return FlowScriptResponse(ret.value.toByteArray())
    }

    override fun executeScriptAtBlockId(script: FlowScript, blockId: FlowId): FlowScriptResponse {
        val ret = api.executeScriptAtBlockID(
            Access.ExecuteScriptAtBlockIDRequest.newBuilder()
                .setScript(script.byteStringValue)
                .build()
        )
        return FlowScriptResponse(ret.value.toByteArray())
    }

    override fun executeScriptAtBlockHeight(script: FlowScript, height: Long): FlowScriptResponse {
        val ret = api.executeScriptAtBlockHeight(
            Access.ExecuteScriptAtBlockHeightRequest.newBuilder()
                .setScript(script.byteStringValue)
                .build()
        )
        return FlowScriptResponse(ret.value.toByteArray())
    }

    override fun getEventsForHeightRange(type: String, range: ClosedRange<Long>): List<FlowEventResult> {
        val ret = api.getEventsForHeightRange(
            Access.GetEventsForHeightRangeRequest.newBuilder()
                .setType(type)
                .setStartHeight(range.start)
                .setEndHeight(range.endInclusive)
                .build()
        )
        return ret.resultsList
            .map { FlowEventResult.of(it) }
    }

    override fun getEventsForBlockIds(type: String, ids: Set<FlowId>): List<FlowEventResult> {
        val ret = api.getEventsForBlockIDs(
            Access.GetEventsForBlockIDsRequest.newBuilder()
                .setType(type)
                .addAllBlockIds(ids.map { it.byteStringValue })
                .build()
        )
        return ret.resultsList
            .map { FlowEventResult.of(it) }
    }

    override fun getNetworkParameters(): FlowChainId {
        val ret = api.getNetworkParameters(
            Access.GetNetworkParametersRequest.newBuilder()
                .build()
        )
        return FlowChainId.of(ret.chainId)
    }

    override fun getLatestProtocolStateSnapshot(): FlowSnapshot {
        val ret = api.getLatestProtocolStateSnapshot(
            Access.GetLatestProtocolStateSnapshotRequest.newBuilder()
                .build()
        )
        return FlowSnapshot(ret.serializedSnapshot.toByteArray())
    }
}
