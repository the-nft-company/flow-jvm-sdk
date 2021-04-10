package org.onflow.sdk

import java.util.concurrent.CompletableFuture

interface AsyncFlowAccessApi {

    fun ping(): CompletableFuture<Unit>

    fun getLatestBlockHeader(): CompletableFuture<FlowBlockHeader>

    fun getBlockHeaderById(id: FlowId): CompletableFuture<FlowBlockHeader?>

    fun getBlockHeaderByHeight(height: Long): CompletableFuture<FlowBlockHeader?>

    fun getLatestBlock(sealed: Boolean = true): CompletableFuture<FlowBlock>

    fun getBlockById(id: FlowId): CompletableFuture<FlowBlock?>

    fun getBlockByHeight(height: Long): CompletableFuture<FlowBlock?>

    fun getCollectionById(id: FlowId): CompletableFuture<FlowCollection?>

    fun sendTransaction(transaction: FlowTransaction): CompletableFuture<FlowId>

    fun getTransactionById(id: FlowId): CompletableFuture<FlowTransaction?>

    fun getTransactionResultById(id: FlowId): CompletableFuture<FlowTransactionResult?>

    @Deprecated(
        message = "Behaves identically to getAccountAtLatestBlock",
        replaceWith = ReplaceWith("getAccountAtLatestBlock")
    )
    fun getAccountByAddress(addresss: FlowAddress): CompletableFuture<FLowAccount?>

    fun getAccountAtLatestBlock(addresss: FlowAddress): CompletableFuture<FLowAccount?>

    fun getAccountByBlockHeight(addresss: FlowAddress, height: Long): CompletableFuture<FLowAccount?>

    fun executeScriptAtLatestBlock(script: FlowScript): CompletableFuture<FlowScriptResponse>

    fun executeScriptAtBlockId(script: FlowScript, blockId: FlowId): CompletableFuture<FlowScriptResponse>

    fun executeScriptAtBlockHeight(script: FlowScript, height: Long): CompletableFuture<FlowScriptResponse>

    fun getEventsForHeightRange(type: String, range: ClosedRange<Long>): CompletableFuture<List<FlowEventResult>>

    fun getEventsForBlockIds(type: String, ids: Set<FlowId>): CompletableFuture<List<FlowEventResult>>

    fun getNetworkParameters(): CompletableFuture<FlowChainId>

    fun getLatestProtocolStateSnapshot(): CompletableFuture<FlowSnapshot>
}
