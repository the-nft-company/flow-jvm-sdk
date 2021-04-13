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

    var objectMapper: ObjectMapper

    init {
        objectMapper = ObjectMapper()
        objectMapper.registerKotlinModule()
        objectMapper.findAndRegisterModules()
    }

    fun newAccessApi(host: String, port: Int = 9000, secure: Boolean = false): FlowAccessApi {
        val channel = openChannel(host, port, secure)
        return FlowAccessApiImpl(AccessAPIGrpc.newBlockingStub(channel))
    }

    fun newAsyncAccessApi(host: String, port: Int = 9000, secure: Boolean = false): AsyncFlowAccessApi {
        val channel = openChannel(host, port, secure)
        return AsyncFlowAccessApiImpl(AccessAPIGrpc.newFutureStub(channel))
    }

    private fun openChannel(host: String, port: Int, secure: Boolean): ManagedChannel {
        var channelBuilder = ManagedChannelBuilder
            .forAddress(host, port)

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
