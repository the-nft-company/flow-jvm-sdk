package com.nftco.flow.sdk.impl

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

internal class KtorBasicHttpClientTest {

    @Test
    fun `can GET`() {
        val client = KtorBasicHttpClient()
        val bytes = client.get("https://reddit.com/")
        assertTrue(bytes.isNotEmpty())
        println(String(bytes))
    }

    @Test
    fun `can GET JSON`() {
        val client = KtorBasicHttpClient()
        val json = client.getJson("https://reddit.com/.json")
        assertEquals("Listing", json.get("kind").textValue())
        println(json)
    }

    @Test
    fun `can POST`() {
        val client = KtorBasicHttpClient()
        val bytes = client.post("https://reddit.com/") { "Whatever".toByteArray() }
        assertTrue(bytes.isNotEmpty())
        println(String(bytes))
    }

    @Test
    fun `can POST JSON`() {
        val client = KtorBasicHttpClient()
        val bytes = client.postJson("https://reddit.com/.json") { "Whatever".toByteArray() }
        assertTrue(bytes.isNotEmpty())
        println(String(bytes))
    }
}
