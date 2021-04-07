package org.onflow.sdk

import kotlinx.coroutines.flow.Flow
import java.math.BigInteger

interface ReactiveFlowAccessApi {

    fun ping()

    fun getLatestBlockHeader(): Flow<FlowBlockHeader>

    fun getBlockHeaderById(id: FlowId): Flow<FlowBlockHeader?>

    fun getBlockHeaderByHeight(height: BigInteger): Flow<FlowBlockHeader?>

    fun getLatestBlock(sealed: Boolean = true): Flow<FlowBlock>

    fun getBlockById(id: FlowId): Flow<FlowBlock?>

    fun getBlockByHeight(height: BigInteger): Flow<FlowBlock?>

    fun getCollectionById(id: FlowId): Flow<FlowCollection?>

    fun getTransactionById(id: FlowId): Flow<Transaction?>

    fun getTransactionResultById(id: FlowId): Flow<Transaction?>

    @Deprecated(
        message = "Behaves identically to getAccountAtLatestBlock",
        replaceWith = ReplaceWith("getAccountAtLatestBlock")
    )
    fun getAccountByAddress(addresss: FlowAddress): Flow<FLowAccount?>

    fun getAccountAtLatestBlock(addresss: FlowAddress): Flow<FLowAccount?>

    fun getAccountByBlockHeight(addresss: FlowAddress, height: BigInteger): Flow<FLowAccount?>

    fun executeScriptAtLatestBlock(script: FlowScript): Flow<FlowScriptResponse?>

    fun executeScriptAtBlockId(script: FlowScript, blockId: FlowId): Flow<FlowScriptResponse?>

    fun executeScriptAtBlockHeight(script: FlowScript, height: BigInteger): Flow<FlowScriptResponse?>

    fun getEventsForHeightRange(type: String, range: ClosedRange<BigInteger>): Flow<List<FlowEventResult>>

    fun getEventsForBlockIds(type: String, ids: Set<FlowId>): Flow<List<FlowEventResult>>

    fun getNetworkParameters(): Flow<ChainId>

    fun getLatestProtocolStateSnapshot(): Flow<FlowSnapshot>
}
