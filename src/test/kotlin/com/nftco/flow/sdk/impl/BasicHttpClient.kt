package com.nftco.flow.sdk.impl

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.nftco.flow.sdk.Flow
import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.features.json.*
import io.ktor.client.features.logging.*
import io.ktor.client.request.*
import io.ktor.http.*
import kotlinx.coroutines.runBlocking

interface BasicHttpClient {

    fun get(url: String, headers: Map<String, List<String>> = emptyMap()): ByteArray

    fun getJson(url: String, headers: Map<String, List<String>> = emptyMap(), objectMapper: ObjectMapper = Flow.OBJECT_MAPPER): JsonNode {
        val bytes = get(url, headers)
        return objectMapper.readTree(bytes)
    }

    fun post(url: String, headers: Map<String, List<String>> = emptyMap(), body: () -> ByteArray): ByteArray

    fun post(url: String, headers: Map<String, List<String>> = emptyMap(), body: ByteArray): ByteArray = post(url, headers) { body }

    fun post(url: String, headers: Map<String, List<String>> = emptyMap(), body: String): ByteArray = post(url, headers) { body.toByteArray() }

    fun postJson(url: String, headers: Map<String, List<String>> = emptyMap(), json: () -> ByteArray): ByteArray = post(url, headers = (headers + ("Content-Type" to listOf("application/json"))), json)

    fun postJson(url: String, headers: Map<String, List<String>> = emptyMap(), json: ByteArray): ByteArray = post(url, headers = (headers + ("Content-Type" to listOf("application/json")))) { json }

    fun postJson(url: String, headers: Map<String, List<String>> = emptyMap(), json: String): ByteArray = postJson(url, headers, json.toByteArray())
}

fun createDefaultKtorClient(
    objectMapper: ObjectMapper = Flow.OBJECT_MAPPER
): HttpClient {
    return HttpClient(CIO) {
        install(JsonFeature) {
            serializer = JacksonSerializer(jackson = objectMapper)
        }
        install(Logging) {
            logger = Logger.DEFAULT
            level = LogLevel.ALL
        }
    }
}

class KtorBasicHttpClient(
    private val httpClient: HttpClient = createDefaultKtorClient()
) : BasicHttpClient {

    override fun get(url: String, headers: Map<String, List<String>>): ByteArray {
        return runBlocking {
            httpClient.request(url) {
                method = HttpMethod.Get
                headers {
                    headers.entries
                        .flatMap { pair -> pair.value.map { pair.key to it } }
                        .forEach { set(it.first, it.second) }
                }
            }
        }
    }

    override fun post(url: String, headers: Map<String, List<String>>, body: () -> ByteArray): ByteArray {
        return runBlocking {
            httpClient.request(url) {
                method = HttpMethod.Get
                headers {
                    headers.entries
                        .flatMap { pair -> pair.value.map { pair.key to it } }
                        .forEach { set(it.first, it.second) }
                }
                HttpRequestBuilder@this.body = body()
            }
        }
    }
}
