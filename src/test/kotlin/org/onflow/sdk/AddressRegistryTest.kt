package org.onflow.sdk

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

internal class AddressRegistryTest {

    @BeforeEach
    fun setup() {
        AddressRegistry
            .clear()
            .registerDefaults()
    }

    @Test
    fun `Can register an address`() {
        val name = "SOME_ADDRESS"
        val address = FlowAddress("0x1a1f2e458a098135")

        assertNull(AddressRegistry.addressOf(name))
        assertNull(AddressRegistry.addressOf(name, FlowChainId.MAINNET))
        assertNull(AddressRegistry.addressOf(name, FlowChainId.TESTNET))
        assertNull(AddressRegistry.addressOf(name, FlowChainId.EMULATOR))

        AddressRegistry.register(name, address)
        assertEquals(address, AddressRegistry.addressOf(name))
        assertEquals(address, AddressRegistry.addressOf(name, FlowChainId.MAINNET))
        assertNull(AddressRegistry.addressOf(name, FlowChainId.TESTNET))
        assertNull(AddressRegistry.addressOf(name, FlowChainId.EMULATOR))

        AddressRegistry.register(name, address, FlowChainId.TESTNET)
        assertEquals(address, AddressRegistry.addressOf(name))
        assertEquals(address, AddressRegistry.addressOf(name, FlowChainId.MAINNET))
        assertEquals(address, AddressRegistry.addressOf(name, FlowChainId.TESTNET))
        assertNull(AddressRegistry.addressOf(name, FlowChainId.EMULATOR))

        AddressRegistry.register(name, address, FlowChainId.EMULATOR)
        assertEquals(address, AddressRegistry.addressOf(name))
        assertEquals(address, AddressRegistry.addressOf(name, FlowChainId.MAINNET))
        assertEquals(address, AddressRegistry.addressOf(name, FlowChainId.TESTNET))
        assertEquals(address, AddressRegistry.addressOf(name, FlowChainId.EMULATOR))
    }

    @Test
    fun `Can deregister an address`() {
        assertNotNull(AddressRegistry.addressOf(AddressRegistry.FUNGIBLE_TOKEN, FlowChainId.MAINNET))
        assertNotNull(AddressRegistry.addressOf(AddressRegistry.FUNGIBLE_TOKEN, FlowChainId.TESTNET))
        assertNotNull(AddressRegistry.addressOf(AddressRegistry.FUNGIBLE_TOKEN, FlowChainId.EMULATOR))

        AddressRegistry.deregister(AddressRegistry.FUNGIBLE_TOKEN)
        assertNull(AddressRegistry.addressOf(AddressRegistry.FUNGIBLE_TOKEN, FlowChainId.MAINNET))
        assertNull(AddressRegistry.addressOf(AddressRegistry.FUNGIBLE_TOKEN, FlowChainId.TESTNET))
        assertNull(AddressRegistry.addressOf(AddressRegistry.FUNGIBLE_TOKEN, FlowChainId.EMULATOR))
    }

    @Test
    fun `Can deregister an address for a specific chain`() {
        assertNotNull(AddressRegistry.addressOf(AddressRegistry.FUNGIBLE_TOKEN, FlowChainId.MAINNET))
        assertNotNull(AddressRegistry.addressOf(AddressRegistry.FUNGIBLE_TOKEN, FlowChainId.TESTNET))
        assertNotNull(AddressRegistry.addressOf(AddressRegistry.FUNGIBLE_TOKEN, FlowChainId.EMULATOR))

        AddressRegistry.deregister(AddressRegistry.FUNGIBLE_TOKEN, FlowChainId.TESTNET)
        assertNotNull(AddressRegistry.addressOf(AddressRegistry.FUNGIBLE_TOKEN, FlowChainId.MAINNET))
        assertNull(AddressRegistry.addressOf(AddressRegistry.FUNGIBLE_TOKEN, FlowChainId.TESTNET))
        assertNotNull(AddressRegistry.addressOf(AddressRegistry.FUNGIBLE_TOKEN, FlowChainId.EMULATOR))
    }

    @Test
    fun `Can process a script`() {
        assertEquals(
            "fungibleToken: 0x9a0766d93b6608b7, flowToken: 0x7e60df042a9c0868",
            AddressRegistry.processScript(
                "fungibleToken: 0xFUNGIBLETOKEN, flowToken: 0xFLOWTOKEN",
                FlowChainId.EMULATOR
            )
        )
        assertEquals(
            "fungibleToken: 0xee82856bf20e2aa6, flowToken: 0x0ae53cb6e3f42a79",
            AddressRegistry.processScript(
                "fungibleToken: 0xFUNGIBLETOKEN, flowToken: 0xFLOWTOKEN",
                FlowChainId.TESTNET
            )
        )
        assertEquals(
            "fungibleToken: 0xf233dcee88fe0abe, flowToken: 0x1654653399040a61",
            AddressRegistry.processScript(
                "fungibleToken: 0xFUNGIBLETOKEN, flowToken: 0xFLOWTOKEN",
                FlowChainId.MAINNET
            )
        )
    }
}
