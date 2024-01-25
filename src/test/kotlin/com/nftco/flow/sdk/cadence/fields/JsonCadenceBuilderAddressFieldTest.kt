package com.nftco.flow.sdk.cadence.fields

import com.nftco.flow.sdk.cadence.AddressField
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Test

class JsonCadenceBuilderAddressFieldTest {

    @Test
    fun `Test equality for AddressField with the same value`() {
        val address1 = AddressField("0x123")
        val address2 = AddressField("0x123")
        assertEquals(address1, address2)
        assertEquals(address1.hashCode(), address2.hashCode())
    }

    @Test
    fun `Test equality for AddressField with different values`() {
        val address1 = AddressField("0x123")
        val address2 = AddressField("0x456")
        assertNotEquals(address1, address2)
        assertNotEquals(address1.hashCode(), address2.hashCode())
    }

    @Test
    fun `Test equality for AddressField with lowercase value`() { // need to investigate this case
        val address1 = AddressField("0xABC")
        val address2 = AddressField("0xabc")
        assertEquals(address1, address2)
        assertEquals(address1.hashCode(), address2.hashCode())
    }

    @Test
    fun `Test equality for AddressField with bytes`() {
        val bytes = byteArrayOf(1, 2, 3)
        val address1 = AddressField(bytes)
        val address2 = AddressField("0x010203")
        assertEquals(address1, address2)
        assertEquals(address1.hashCode(), address2.hashCode())
    }

    @Test
    fun `Test equality for AddressField with bytes and different values`() {
        val bytes = byteArrayOf(1, 2, 3)
        val address1 = AddressField(bytes)
        val address2 = AddressField("0x040506")
        assertNotEquals(address1, address2)
        assertNotEquals(address1.hashCode(), address2.hashCode())
    }
}
