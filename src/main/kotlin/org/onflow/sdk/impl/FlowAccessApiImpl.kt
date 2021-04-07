package org.onflow.sdk.impl

import org.onflow.protobuf.access.Access
import org.onflow.protobuf.access.AccessAPIGrpc
import org.onflow.sdk.*
import java.math.BigInteger

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
        return FlowBlockHeader.from(ret.block)
    }

    override fun getBlockHeaderById(id: FlowId): FlowBlockHeader? {
        val ret = api.getBlockHeaderByID(
            Access.GetBlockHeaderByIDRequest.newBuilder()
                .setId(id.byteStringValue)
                .build()
        )
        return if (ret.hasBlock()) {
            FlowBlockHeader.from(ret.block)
        } else {
            null
        }
    }

    override fun getBlockHeaderByHeight(height: BigInteger): FlowBlockHeader? {
        val ret = api.getBlockHeaderByHeight(
            Access.GetBlockHeaderByHeightRequest.newBuilder()
                .setHeight(height.toLong())
                .build()
        )
        return if (ret.hasBlock()) {
            FlowBlockHeader.from(ret.block)
        } else {
            null
        }
    }

    override fun getLatestBlock(sealed: Boolean): FlowBlock {
        val ret = api.getLatestBlock(
            Access.GetLatestBlockRequest.newBuilder()
                .build()
        )
        return FlowBlock.from(ret.block)
    }

    override fun getBlockById(id: FlowId): FlowBlock? {
        val ret = api.getBlockByID(
            Access.GetBlockByIDRequest.newBuilder()
                .setId(id.byteStringValue)
                .build()
        )
        return if (ret.hasBlock()) {
            FlowBlock.from(ret.block)
        } else {
            null
        }
    }

    override fun getBlockByHeight(height: BigInteger): FlowBlock? {
        val ret = api.getBlockByHeight(
            Access.GetBlockByHeightRequest.newBuilder()
                .setHeight(height.toLong())
                .build()
        )
        return if (ret.hasBlock()) {
            FlowBlock.from(ret.block)
        } else {
            null
        }
    }

    override fun getCollectionById(id: FlowId): FlowCollection? {
        TODO("Not yet implemented")
    }

    override fun getTransactionById(id: FlowId): Transaction? {
        TODO("Not yet implemented")
    }

    override fun getTransactionResultById(id: FlowId): Transaction? {
        TODO("Not yet implemented")
    }

    override fun getAccountByAddress(addresss: FlowAddress): FLowAccount? {
        TODO("Not yet implemented")
    }

    override fun getAccountAtLatestBlock(addresss: FlowAddress): FLowAccount? {
        TODO("Not yet implemented")
    }

    override fun getAccountByBlockHeight(addresss: FlowAddress, height: BigInteger): FLowAccount? {
        TODO("Not yet implemented")
    }

    override fun executeScriptAtLatestBlock(script: FlowScript): FlowScriptResponse? {
        TODO("Not yet implemented")
    }

    override fun executeScriptAtBlockId(script: FlowScript, blockId: FlowId): FlowScriptResponse? {
        TODO("Not yet implemented")
    }

    override fun executeScriptAtBlockHeight(script: FlowScript, height: BigInteger): FlowScriptResponse? {
        TODO("Not yet implemented")
    }

    override fun getEventsForHeightRange(type: String, range: ClosedRange<BigInteger>): List<FlowEventResult> {
        TODO("Not yet implemented")
    }

    override fun getEventsForBlockIds(type: String, ids: Set<FlowId>): List<FlowEventResult> {
        TODO("Not yet implemented")
    }

    override fun getNetworkParameters(): ChainId {
        TODO("Not yet implemented")
    }

    override fun getLatestProtocolStateSnapshot(): FlowSnapshot {
        TODO("Not yet implemented")
    }
}
