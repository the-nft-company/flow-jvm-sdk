package org.onflow.sdk

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class TransactionTest {

    private var transaction = FlowTransaction(
        script = FlowScript("import 0xsomething \n {}"),
        arguments = listOf(FlowArgument(byteArrayOf(2, 2, 3)), FlowArgument(byteArrayOf(3, 3, 3))),
        referenceBlockId = FlowId.of(byteArrayOf(3, 3, 3, 6, 6, 6)),
        gasLimit = 44,
        proposalKey = FlowTransactionProposalKey(
            address = FlowAddress.of(byteArrayOf(4, 5, 4, 5, 4, 5)),
            keyIndex = 11,
            sequenceNumber = 7
        ),
        payerAddress = FlowAddress.of(byteArrayOf(6, 5, 4, 3, 2)),
        authorizers = listOf(FlowAddress.of(byteArrayOf(9, 9, 9, 9, 9)), FlowAddress.of(byteArrayOf(8, 9, 9, 9, 9)))
    )

    @Test
    fun `Can sign transactions`() {

        val pk1 = Crypto.generateKeyPair().privateKey
        val pk2 = Crypto.generateKeyPair().privateKey

        val address1 = FlowAddress("f8d6e0586b0a20c7")
        val address2 = FlowAddress("f8d6e0586b0a20c7")

        transaction = transaction.signAsAuthorizer(pk1, address1, 3)
        println("Authorization signature ${transaction.authorizationSignatures[0].signature.base16Value}")
        println("Authorization envelope ${transaction.authorizationEnvelope.bytesToHex()}")

        transaction = transaction.signAsPayer(pk2, address2, 3)
        println("Payment signature ${transaction.paymentSignatures[0].signature.base16Value}")
        println("Payment envelope ${transaction.paymentEnvelope.bytesToHex()}")
    }

    @Test
    fun `Canonical transaction form is accurate`() {

        val payloadEnvelope = transaction.payloadEnvelope

        // those values were generated from Go implementation for the same transaction input data
        val payloadExpectedHex =
            "f86a97696d706f7274203078736f6d657468696e67200a207b7dc88302020383030303a000000000000000000000000000000000000000000000000000000303030606062c8800000405040504050b07880000000605040302d2880000000909090909880000000809090909"
        val envelopeExpectedHex =
            "f883f86a97696d706f7274203078736f6d657468696e67200a207b7dc88302020383030303a000000000000000000000000000000000000000000000000000000303030606062c8800000405040504050b07880000000605040302d2880000000909090909880000000809090909d6ce80808b0404040404040404040404c6040583030303"

        assertThat(payloadEnvelope).isEqualTo(payloadExpectedHex.hexToBytes())

        val address = FlowAddress("f8d6e0586b0a20c7")

        val fooSignature = byteArrayOf(4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4)
        val barSignature = byteArrayOf(3, 3, 3)

        val signedTx = transaction.copy(
            authorizationSignatures = listOf(
                FlowTransactionSignature(address, 0, 0, FlowSignature(fooSignature)),
                FlowTransactionSignature(address, 4, 5, FlowSignature(barSignature))
            )
        )

        val authorizationEnvelope = signedTx.authorizationEnvelope
        assertThat(authorizationEnvelope).isEqualTo(envelopeExpectedHex.hexToBytes())
    }

    @Test
    fun `Can connect to mainnet`() {

        val accessAPI = Flow.newAccessApi(FlowChainId.MAINNET)
        accessAPI.ping()

        val address = FlowAddress("e467b9dd11fa00df")
        val account = accessAPI.getAccountAtLatestBlock(address)
        assertThat(account).isNotNull
        println(account!!)
        assertThat(account.keys).isNotEmpty
    }

    @Test
    fun `Can parse events`() {
        val accessApi = Flow.newAccessApi(FlowChainId.MAINNET)

        val tx = accessApi.getTransactionById(FlowId("5e6ef76c524dd131bbab5f9965493b7830bb784561ca6391b320ec60fa5c395e"))
        assertThat(tx).isNotNull

        val results = accessApi.getTransactionResultById(FlowId("5e6ef76c524dd131bbab5f9965493b7830bb784561ca6391b320ec60fa5c395e"))!!
        assertThat(results.events).hasSize(4)
        assertThat(results.events[0].event.id).isEqualTo("A.0b2a3299cc857e29.TopShot.Withdraw")
        assertThat(results.events[1].event.id).isEqualTo("A.c1e4f4f4c4257510.Market.CutPercentageChanged")
        assertThat(results.events[2].event.id).isEqualTo("A.0b2a3299cc857e29.TopShot.Deposit")

        assertThat(results.events[3].event.id).isEqualTo("A.c1e4f4f4c4257510.Market.MomentListed")
        assertThat("id" in results.events[3].event).isTrue
        assertThat("price" in results.events[3].event).isTrue
        assertThat("seller" in results.events[3].event).isTrue
        assertThat("id" in results.events[3].event.value!!).isTrue
        assertThat("price" in results.events[3].event.value!!).isTrue
        assertThat("seller" in results.events[3].event.value!!).isTrue

        val block = accessApi.getBlockById(tx!!.referenceBlockId)
        assertThat(block).isNotNull

        val events = accessApi.getEventsForBlockIds("A.0b2a3299cc857e29.TopShot.Withdraw", setOf(block!!.id))
        assertThat(events).isNotNull
    }
}

// {"type":"Event","value":{"id":"A.0b2a3299cc857e29.TopShot.Withdraw","fields":[{"name":"id","value":{"type":"UInt64","value":"855038"}},{"name":"from","value":{"type":"Optional","value":{"type":"Address","value":"0xd1b16e03b5558c03"}}}]}}

