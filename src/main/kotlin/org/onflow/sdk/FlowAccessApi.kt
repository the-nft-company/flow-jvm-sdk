package org.onflow.sdk

import java.math.BigInteger

interface FlowAccessApi {

    fun ping()

    fun getLatestBlockHeader(): FlowBlockHeader

    fun getBlockHeaderById(id: FlowId): FlowBlockHeader?

    fun getBlockHeaderByHeight(height: BigInteger): FlowBlockHeader?

    fun getLatestBlock(sealed: Boolean = true): FlowBlock

    fun getBlockById(id: FlowId): FlowBlock?

    fun getBlockByHeight(height: BigInteger): FlowBlock?

    fun getCollectionById(id: FlowId): FlowCollection?

    fun sendTransaction(transaction: FlowTransaction): FlowId

    fun getTransactionById(id: FlowId): FlowTransaction?

    fun getTransactionResultById(id: FlowId): FlowTransactionResult?

    @Deprecated(
        message = "Behaves identically to getAccountAtLatestBlock",
        replaceWith = ReplaceWith("getAccountAtLatestBlock")
    )
    fun getAccountByAddress(addresss: FlowAddress): FLowAccount?

    fun getAccountAtLatestBlock(addresss: FlowAddress): FLowAccount?

    fun getAccountByBlockHeight(addresss: FlowAddress, height: BigInteger): FLowAccount?

    fun executeScriptAtLatestBlock(script: FlowScript): FlowScriptResponse

    fun executeScriptAtBlockId(script: FlowScript, blockId: FlowId): FlowScriptResponse

    fun executeScriptAtBlockHeight(script: FlowScript, height: BigInteger): FlowScriptResponse

    fun getEventsForHeightRange(type: String, range: ClosedRange<BigInteger>): List<FlowEventResult>

    fun getEventsForBlockIds(type: String, ids: Set<FlowId>): List<FlowEventResult>

    fun getNetworkParameters(): FlowChainId

    fun getLatestProtocolStateSnapshot(): FlowSnapshot
}
