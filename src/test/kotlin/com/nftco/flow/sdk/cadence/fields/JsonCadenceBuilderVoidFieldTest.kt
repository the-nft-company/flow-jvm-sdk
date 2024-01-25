package com.nftco.flow.sdk.cadence.fields

import com.nftco.flow.sdk.cadence.*
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Test

class JsonCadenceBuilderVoidFieldTest {

    @Test
    fun `Test equality of VoidField instances`() {
        val voidField1 = VoidField()
        val voidField2 = VoidField()

        assertEquals(voidField1, voidField2)
    }

    @Test
    fun `Test hashing of VoidField instances`() {
        val voidField1 = VoidField()
        val voidField2 = VoidField()

        assertEquals(voidField1.hashCode(), voidField2.hashCode())
    }

    @Test
    fun `Test equality with other Field types`() {
        val voidField = VoidField()
        val stringField = StringField("test")

        assertNotEquals(voidField, stringField)
    }
}
