package org.onflow.sdk

import com.google.common.io.BaseEncoding
import com.google.protobuf.ByteString
import io.grpc.ManagedChannelBuilder
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.onflow.protobuf.access.Access
import org.onflow.protobuf.access.AccessAPIGrpc
import java.math.BigInteger

class TransactionTest {

    init {
        InitCrypto()
    }

    fun String.hexToBytes(): ByteArray {
        return this.chunked(2).map { it.toInt(16).toByte() }.toByteArray()
    }

    fun ByteArray.toHex(): String {
        return BaseEncoding.base16().lowerCase().encode(this)
    }

    val tx = Transaction(
        script = "import 0xsomething \n {}".encodeToByteArray(),
        arguments = listOf(byteArrayOf(2, 2, 3), byteArrayOf(3, 3, 3)),
        authorizers = listOf(Address(byteArrayOf(9, 9, 9, 9, 9)), Address(byteArrayOf(8, 9, 9, 9, 9))),
        proposalKey = ProposalKey(Address(byteArrayOf(4, 5, 4, 5, 4, 5)), 11, 7),
        payer = Address(byteArrayOf(6, 5, 4, 3, 2)),
        gasLimit = 44,
        referenceBlockID = Identifier(byteArrayOf(3, 3, 3, 6, 6, 6))
    )

    @Test
    fun testCanonicalForm() {

        val payloadCanonical = tx.payloadCanonicalForm()

        // those values were generated from Go implementation for the same transaction input data
        val payloadExpectedHex =
            "f86a97696d706f7274203078736f6d657468696e67200a207b7dc88302020383030303a000000000000000000000000000000000000000000000000000000303030606062c8800000405040504050b07880000000605040302d2880000000909090909880000000809090909"
        val envelopeExpectedHex =
            "f883f86a97696d706f7274203078736f6d657468696e67200a207b7dc88302020383030303a000000000000000000000000000000000000000000000000000000303030606062c8800000405040504050b07880000000605040302d2880000000909090909880000000809090909d6ce80808b0404040404040404040404c6040583030303"

        assertThat(payloadCanonical).isEqualTo(payloadExpectedHex.hexToBytes())


        val address = Address("f8d6e0586b0a20c7")

        val fooSignature = byteArrayOf(4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4)
        val barSignature = byteArrayOf(3, 3, 3)

        val signedTx = tx.copy(
            payloadSignatures = listOf(
                TransactionSignature(address, 0, 0, fooSignature),
                TransactionSignature(address, 4, 5, barSignature)
            )
        )


        val envelopeCanonical = signedTx.envelopCanonicalForm()
        assertThat(envelopeCanonical).isEqualTo(envelopeExpectedHex.hexToBytes())
    }

    @Test
    fun testHashAndSign() {

        val address = Address("f8d6e0586b0a20c7")

        val fooSignature = byteArrayOf(4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4)
        val barSignature = byteArrayOf(3, 3, 3)

        val signedTx = tx.copy(
            payloadSignatures = listOf(
                TransactionSignature(address, 0, 0, fooSignature),
                TransactionSignature(address, 4, 5, barSignature)
            )
        )

        val d = BigInteger("749024acd97d0b72448f1baf600314cfc28d97a4e1816e0578f4ae3befcf4e26".hexToBytes())

        val privateKey = ECDSAp256_SHA3_256PrivateKey(d)

        val payloadCanonical = tx.payloadCanonicalForm()
        val payloadSignature = privateKey.Sign(payloadCanonical)

        println("Payload signature ${payloadSignature.toHex()}")


        val envelopeCanonical = signedTx.envelopCanonicalForm()
        val envelopeSignature = privateKey.Sign(envelopeCanonical)

        println("Envelope signature ${envelopeSignature.toHex()}")

        // Signatures above can used in Flow Go implementation tests to check for correctness
    }

    @Test
    fun connectingToMainnet() {
        val managedChannel =
            ManagedChannelBuilder.forAddress("access.mainnet.nodes.onflow.org", 9000).usePlaintext().build()

        val accessAPI = AccessAPIGrpc.newBlockingStub(managedChannel)

        // Ping to make sure service is there
        val pingResponse = accessAPI.ping(Access.PingRequest.newBuilder().build())

        val grpcAddress = ByteString.copyFrom(BaseEncoding.base16().lowerCase().decode("e467b9dd11fa00df"))

        val getAccountRequest = Access.GetAccountRequest.newBuilder().setAddress(grpcAddress).build()

        val accountResponse = accessAPI.getAccount(getAccountRequest)

        println(accountResponse.account)

        // We can safely assume service account has some keys
        assertThat(accountResponse.account.keysList).isNotEmpty
    }

}