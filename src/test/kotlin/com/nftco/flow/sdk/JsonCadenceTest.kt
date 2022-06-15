package com.nftco.flow.sdk

import com.nftco.flow.sdk.cadence.CompositeValue
import com.nftco.flow.sdk.cadence.EventField
import com.nftco.flow.sdk.cadence.TypeField
import com.nftco.flow.sdk.cadence.TypeValue
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class JsonCadenceTest {

    @Test
    fun testParsingTypeField() {
        val p = FlowEventPayload(JSON_STRING.toByteArray())
        assertThat(p.jsonCadence).isInstanceOf(EventField::class.java)
        (p.jsonCadence.value as? CompositeValue)?.let {
            val result = it.getField<TypeField>("nftType")
            assertThat(result).isNotNull
            assertThat(result?.value).isInstanceOf(TypeValue::class.java)
            assertThat(result?.value?.staticType?.typeID).isEqualTo("A.9f2eb43b6df02730.Momentables.NFT")
        }
    }

    companion object {
        private const val JSON_STRING =
            "{\"type\":\"Event\",\"value\":{\"id\":\"A.94b06cfca1d8a476.NFTStorefront.ListingCompleted\",\"fields\":[{\"name\":\"listingResourceID\",\"value\":{\"type\":\"UInt64\",\"value\":\"97372981\"}},{\"name\":\"storefrontResourceID\",\"value\":{\"type\":\"UInt64\",\"value\":\"91117831\"}},{\"name\":\"purchased\",\"value\":{\"type\":\"Bool\",\"value\":false}},{\"name\":\"nftType\",\"value\":{\"type\":\"Type\",\"value\":{\"staticType\":{\"kind\":\"Resource\",\"typeID\":\"A.9f2eb43b6df02730.Momentables.NFT\",\"fields\":[{\"id\":\"uuid\",\"type\":{\"kind\":\"UInt64\"}},{\"id\":\"id\",\"type\":{\"kind\":\"UInt64\"}},{\"id\":\"momentableId\",\"type\":{\"kind\":\"String\"}},{\"id\":\"name\",\"type\":{\"kind\":\"String\"}},{\"id\":\"description\",\"type\":{\"kind\":\"String\"}},{\"id\":\"imageCID\",\"type\":{\"kind\":\"String\"}},{\"id\":\"directoryPath\",\"type\":{\"kind\":\"String\"}},{\"id\":\"traits\",\"type\":{\"kind\":\"Dictionary\",\"key\":{\"kind\":\"String\"},\"value\":{\"kind\":\"Dictionary\",\"key\":{\"kind\":\"String\"},\"value\":{\"kind\":\"String\"}}}},{\"id\":\"creator\",\"type\":{\"kind\":\"Struct\",\"typeID\":\"A.9f2eb43b6df02730.Momentables.Creator\",\"fields\":[{\"id\":\"creatorName\",\"type\":{\"kind\":\"String\"}},{\"id\":\"creatorWallet\",\"type\":{\"kind\":\"Capability\",\"type\":{\"kind\":\"Reference\",\"type\":{\"kind\":\"Restriction\",\"typeID\":\"AnyResource{A.9a0766d93b6608b7.FungibleToken.Receiver}\",\"type\":{\"kind\":\"AnyResource\"},\"restrictions\":[{\"kind\":\"ResourceInterface\",\"typeID\":\"A.9a0766d93b6608b7.FungibleToken.Receiver\",\"fields\":[{\"id\":\"uuid\",\"type\":{\"kind\":\"UInt64\"}}],\"initializers\":[],\"type\":\"\"}]},\"authorized\":false}}},{\"id\":\"creatorRoyalty\",\"type\":{\"kind\":\"UFix64\"}}],\"initializers\":[],\"type\":\"\"}},{\"id\":\"collaborators\",\"type\":{\"kind\":\"VariableSizedArray\",\"type\":{\"kind\":\"Struct\",\"typeID\":\"A.9f2eb43b6df02730.Momentables.Collaborator\",\"fields\":[{\"id\":\"collaboratorName\",\"type\":{\"kind\":\"String\"}},{\"id\":\"collaboratorWallet\",\"type\":{\"kind\":\"Capability\",\"type\":{\"kind\":\"Reference\",\"type\":{\"kind\":\"Restriction\",\"typeID\":\"AnyResource{A.9a0766d93b6608b7.FungibleToken.Receiver}\",\"type\":{\"kind\":\"AnyResource\"},\"restrictions\":[\"A.9a0766d93b6608b7.FungibleToken.Receiver\"]},\"authorized\":false}}},{\"id\":\"collaboratorRoyalty\",\"type\":{\"kind\":\"UFix64\"}}],\"initializers\":[],\"type\":\"\"}}},{\"id\":\"momentableCollectionDetails\",\"type\":{\"kind\":\"Dictionary\",\"key\":{\"kind\":\"String\"},\"value\":{\"kind\":\"String\"}}}],\"initializers\":[],\"type\":\"\"}}}},{\"name\":\"nftID\",\"value\":{\"type\":\"UInt64\",\"value\":\"99\"}}]}}"
    }
}
