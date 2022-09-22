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
}
