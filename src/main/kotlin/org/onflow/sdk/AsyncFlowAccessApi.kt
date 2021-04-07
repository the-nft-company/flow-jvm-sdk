package org.onflow.sdk

import java.math.BigInteger
import java.util.concurrent.CompletableFuture

interface AsyncFlowAccessApi {

    fun ping()

    fun getLatestBlockHeader(): CompletableFuture<FlowBlockHeader>

    fun getBlockHeaderById(id: FlowId): CompletableFuture<FlowBlockHeader?>

    fun getBlockHeaderByHeight(height: BigInteger): CompletableFuture<FlowBlockHeader?>

    fun getLatestBlock(sealed: Boolean = true): CompletableFuture<FlowBlock>

    fun getBlockById(id: FlowId): CompletableFuture<FlowBlock?>

    fun getBlockByHeight(height: BigInteger): CompletableFuture<FlowBlock?>

    fun getCollectionById(id: FlowId): CompletableFuture<FlowCollection?>

    fun getTransactionById(id: FlowId): CompletableFuture<Transaction?>

    fun getTransactionResultById(id: FlowId): CompletableFuture<Transaction?>

    @Deprecated(
        message = "Behaves identically to getAccountAtLatestBlock",
        replaceWith = ReplaceWith("getAccountAtLatestBlock")
    )
    fun getAccountByAddress(addresss: FlowAddress): CompletableFuture<FLowAccount?>

    fun getAccountAtLatestBlock(addresss: FlowAddress): CompletableFuture<FLowAccount?>

    fun getAccountByBlockHeight(addresss: FlowAddress, height: BigInteger): CompletableFuture<FLowAccount?>

    fun executeScriptAtLatestBlock(script: FlowScript): CompletableFuture<FlowScriptResponse?>

    fun executeScriptAtBlockId(script: FlowScript, blockId: FlowId): CompletableFuture<FlowScriptResponse?>

    fun executeScriptAtBlockHeight(script: FlowScript, height: BigInteger): CompletableFuture<FlowScriptResponse?>

    fun getEventsForHeightRange(type: String, range: ClosedRange<BigInteger>): CompletableFuture<List<FlowEventResult>>

    fun getEventsForBlockIds(type: String, ids: Set<FlowId>): CompletableFuture<List<FlowEventResult>>

    fun getNetworkParameters(): CompletableFuture<ChainId>

    fun getLatestProtocolStateSnapshot(): CompletableFuture<FlowSnapshot>
}
