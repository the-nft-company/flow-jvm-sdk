package com.nftco.flow.sdk

import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Test

class JsonCadenceTest {

    @Test
    fun `Can parse new JSON Cadence`() {
        val flow = TestUtils.newMainnetAccessApi()
        val tx = flow.getTransactionResultById(FlowId("273f68ffe175a0097db60bc7cf5e92c5a775d189af3f5636f5432c1206be771a"))!!
        val events = tx.events.map { it.payload.jsonCadence }
        Assertions.assertThat(events).hasSize(7)
    }
}
