package org.onflow.sdk

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import io.grpc.ManagedChannel
import io.grpc.ManagedChannelBuilder
import kotlin.reflect.KClass
import org.onflow.protobuf.access.AccessAPIGrpc
import org.onflow.sdk.cadence.Field
import org.onflow.sdk.cadence.Marshalling
import org.onflow.sdk.impl.AsyncFlowAccessApiImpl
import org.onflow.sdk.impl.FlowAccessApiImpl

object Flow {

    const val DEFAULT_USER_AGENT = "Flow JVM SDK"

    var OBJECT_MAPPER: ObjectMapper

    var DEFAULT_CHAIN_ID: FlowChainId = FlowChainId.MAINNET
        private set

    var DEFAULT_ADDRESS_REGISTRY: AddressRegistry = AddressRegistry()
        private set

    init {
        OBJECT_MAPPER = ObjectMapper()
        OBJECT_MAPPER.registerKotlinModule()
        OBJECT_MAPPER.findAndRegisterModules()
    }

    @JvmStatic
    @JvmOverloads
    fun configureDefaults(
        chainId: FlowChainId = DEFAULT_CHAIN_ID,
        addressRegistry: AddressRegistry = DEFAULT_ADDRESS_REGISTRY
    ) {
        this.DEFAULT_CHAIN_ID = chainId
        this.DEFAULT_ADDRESS_REGISTRY = addressRegistry
    }

    @JvmStatic
    @JvmOverloads
    fun newAccessApi(host: String, port: Int = 9000, secure: Boolean = false, userAgent: String = DEFAULT_USER_AGENT): FlowAccessApi {
        val channel = openChannel(host, port, secure, userAgent)
        return FlowAccessApiImpl(AccessAPIGrpc.newBlockingStub(channel))
    }

    @JvmStatic
    @JvmOverloads
    fun newAsyncAccessApi(host: String, port: Int = 9000, secure: Boolean = false, userAgent: String = DEFAULT_USER_AGENT): AsyncFlowAccessApi {
        val channel = openChannel(host, port, secure, userAgent)
        return AsyncFlowAccessApiImpl(AccessAPIGrpc.newFutureStub(channel))
    }

    @JvmStatic
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

    @JvmStatic
    fun <T : Field<*>> decodeCDIFs(string: String): List<T> = decodeCDIFs(string.toByteArray(Charsets.UTF_8))
    @JvmStatic
    fun <T : Field<*>> decodeCDIFs(bytes: ByteArray): List<T> = OBJECT_MAPPER.readValue(bytes, object : TypeReference<List<T>>() {})

    @JvmStatic
    fun <T : Field<*>> decodeCDIF(string: String): T = decodeCDIF(string.toByteArray(Charsets.UTF_8))
    @JvmStatic
    fun <T : Field<*>> decodeCDIF(bytes: ByteArray): T = OBJECT_MAPPER.readValue(bytes, object : TypeReference<T>() {})

    @JvmStatic
    fun <T : Field<*>> encodeCDIFs(cdifs: Iterable<T>): ByteArray = OBJECT_MAPPER.writeValueAsBytes(cdifs)
    @JvmStatic
    fun <T : Field<*>> encodeCDIF(cdif: T): ByteArray = OBJECT_MAPPER.writeValueAsBytes(cdif)

    @JvmStatic
    fun <T: Any> unmarshall(type: KClass<T>, value: Field<*>): T = Marshalling.unmarshall(type, value)

    @JvmStatic
    @JvmOverloads
    fun <T: Any> marshall(value: T, clazz: KClass<out T> = value::class): Field<*> = Marshalling.marshall(value, clazz)
}
