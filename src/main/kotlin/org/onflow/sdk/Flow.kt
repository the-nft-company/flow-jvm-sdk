package org.onflow.sdk

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import io.grpc.ManagedChannel
import io.grpc.ManagedChannelBuilder
import org.onflow.protobuf.access.AccessAPIGrpc
import org.onflow.sdk.impl.AsyncFlowAccessApiImpl
import org.onflow.sdk.impl.FlowAccessApiImpl


object Flow {

    const val DEFAULT_USER_AGENT = "Flow JVM SDK"

    var objectMapper: ObjectMapper

    init {
        objectMapper = ObjectMapper()
        objectMapper.registerKotlinModule()
        objectMapper.findAndRegisterModules()
    }

    fun newAccessApi(host: String, port: Int = 9000, secure: Boolean = false, userAgent: String = DEFAULT_USER_AGENT): FlowAccessApi {
        val channel = openChannel(host, port, secure, userAgent)
        return FlowAccessApiImpl(AccessAPIGrpc.newBlockingStub(channel))
    }

    fun newAsyncAccessApi(host: String, port: Int = 9000, secure: Boolean = false, userAgent: String = DEFAULT_USER_AGENT): AsyncFlowAccessApi {
        val channel = openChannel(host, port, secure, userAgent)
        return AsyncFlowAccessApiImpl(AccessAPIGrpc.newFutureStub(channel))
    }

    private fun openChannel(host: String, port: Int, secure: Boolean, userAgent: String): ManagedChannel {
        var channelBuilder = ManagedChannelBuilder
            .forAddress(host, port)
            .userAgent(userAgent)

        channelBuilder = if (secure) {
            channelBuilder.useTransportSecurity()
        } else {
            channelBuilder.usePlaintext()
        }

        return channelBuilder.build()
    }

    fun <T : Field<*>> decodeCDIFs(string: String): List<T> = decodeCDIFs(string.toByteArray(Charsets.UTF_8))
    fun <T : Field<*>> decodeCDIFs(bytes: ByteArray): List<T> = objectMapper.readValue(bytes, object : TypeReference<List<T>>() {})

    fun <T : Field<*>> decodeCDIF(string: String): T = decodeCDIF(string.toByteArray(Charsets.UTF_8))
    fun <T : Field<*>> decodeCDIF(bytes: ByteArray): T = objectMapper.readValue(bytes, object : TypeReference<T>() {})

    fun <T : Field<*>> encodeCDIFs(cdifs: Iterable<T>): ByteArray = objectMapper.writeValueAsBytes(cdifs)
    fun <T : Field<*>> encodeCDIF(cdif: T): ByteArray = objectMapper.writeValueAsBytes(cdif)
}
