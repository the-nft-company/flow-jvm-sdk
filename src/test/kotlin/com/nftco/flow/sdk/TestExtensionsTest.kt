package com.nftco.flow.sdk

import com.nftco.flow.sdk.test.*
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

@FlowEmulatorTest
class TestExtensionsTest {

    @FlowTestClient
    lateinit var accessAPI: FlowAccessApi

    @FlowTestClient
    lateinit var asyncAccessApi: AsyncFlowAccessApi

    @FlowServiceAccountCredentials
    lateinit var serviceAccount: TestAccount

    @FlowTestAccount
    lateinit var account0: TestAccount

    @FlowTestAccount(
        signAlgo = SignatureAlgorithm.ECDSA_P256,
        balance = 69.0
    )
    lateinit var account1: TestAccount

    @FlowTestAccount(
        signAlgo = SignatureAlgorithm.ECDSA_SECP256k1,
        balance = 420.0
    )
    lateinit var account2: TestAccount

    @Test
    fun `Test extensions work`() {
        accessAPI.ping()
        asyncAccessApi.ping().get()
        assertTrue(serviceAccount.isValid)
        assertTrue(account0.isValid)
        assertTrue(account1.isValid)
        assertTrue(account2.isValid)

        val addresses = setOf(account0.flowAddress, account1.flowAddress, account2.flowAddress)
        assertEquals(3, addresses.size)
    }
}
