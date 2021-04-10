package org.onflow.sdk

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import io.grpc.ManagedChannel
import io.grpc.ManagedChannelBuilder
import java.math.BigInteger
import java.security.Security
import org.bouncycastle.jce.provider.BouncyCastleProvider
import org.onflow.protobuf.access.AccessAPIGrpc
import org.onflow.sdk.impl.AsyncFlowAccessApiImpl
import org.onflow.sdk.impl.ECDSAp256_SHA3_256PrivateKey
import org.onflow.sdk.impl.FlowAccessApiImpl

object Flow {

    const val DEFAULT_USER_AGENT = "Flow Java SDK"

    var objectMapper: ObjectMapper

    init {
        Security.addProvider(BouncyCastleProvider())
        objectMapper = ObjectMapper()
        objectMapper.registerKotlinModule()
        objectMapper.findAndRegisterModules()
    }

    fun newAccessApi(host: String, port: Int, secure: Boolean = false, userAgent: String = DEFAULT_USER_AGENT): FlowAccessApi {
        val channel = openChannel(host, port, secure, userAgent)
        return FlowAccessApiImpl(AccessAPIGrpc.newBlockingStub(channel))
    }

    fun newAsyncAccessApi(host: String, port: Int, secure: Boolean = false, userAgent: String = DEFAULT_USER_AGENT): AsyncFlowAccessApi {
        val channel = openChannel(host, port, secure, userAgent)
        return AsyncFlowAccessApiImpl(AccessAPIGrpc.newFutureStub(channel))
    }

    fun newAccessApi(chain: FlowChainId, secure: Boolean = false, userAgent: String = DEFAULT_USER_AGENT): FlowAccessApi {
        return newAccessApi(chain.endpoint, chain.poort, secure, userAgent)
    }

    fun newAsyncAccessApi(chain: FlowChainId, secure: Boolean = false, userAgent: String = DEFAULT_USER_AGENT): AsyncFlowAccessApi {
        return newAsyncAccessApi(chain.endpoint, chain.poort, secure, userAgent)
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

    fun loadPrivateKey(hex: String, algo: SignatureAlgorithm = SignatureAlgorithm.ECDSA_P256_ECDSA_P256): PrivateKey {
        val d = BigInteger(hex, 16)
        return when (algo) {
            SignatureAlgorithm.ECDSA_P256_ECDSA_P256 -> ECDSAp256_SHA3_256PrivateKey(d)
            SignatureAlgorithm.ECDSA_SECP256K1_ECDSA_SECP256K1 -> throw IllegalArgumentException("ECDSA_SECP256K1_ECDSA_SECP256K1 isn't supported yet")
        }
    }

    fun <T : Field<*>> decodeCDIFs(string: String): List<T> = decodeCDIFs(string.toByteArray(Charsets.UTF_8))
    fun <T : Field<*>> decodeCDIFs(bytes: ByteArray): List<T> = objectMapper.readValue(bytes, object : TypeReference<List<T>>() {})

    fun <T : Field<*>> decodeCDIF(string: String): T = decodeCDIF(string.toByteArray(Charsets.UTF_8))
    fun <T : Field<*>> decodeCDIF(bytes: ByteArray): T = objectMapper.readValue(bytes, object : TypeReference<T>() {})

    fun <T : Field<*>> encodeCDIFs(cdifs: Iterable<T>): ByteArray = objectMapper.writeValueAsBytes(cdifs)
    fun <T : Field<*>> encodeCDIF(cdif: T): ByteArray = objectMapper.writeValueAsBytes(cdif)
}
