package com.nftco.flow.sdk

import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Test

class JsonCadenceTest {

    @Test
    fun `Can parse new JSON Cadence`() {
        val flow = TestUtils.newMainnetAccessApi()
        val tx = flow.getTransactionResultById(FlowId("663869d910278d7b6caf793396f6f2c5b91aace7180c2c70cfb3b0b6efd7a049"))!!
        val events = tx.events.map { it.payload.jsonCadence }
        Assertions.assertThat(events).hasSize(4)
    }

    @Test
    fun `Can parse JSON Cadence Restriction Type`() {
        val flow = TestUtils.newMainnetAccessApi()
        val tx = flow.getTransactionResultById(FlowId("a47ebd7480b22f8f27cd9c5b28280bd994a93ed88054c0b7bd824633841ab04b"))!!
        val events = tx.events.map { it.payload.jsonCadence }
        Assertions.assertThat(events).hasSize(4)
    }
}
