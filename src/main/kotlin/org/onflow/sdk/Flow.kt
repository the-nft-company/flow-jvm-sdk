package org.onflow.sdk

import io.grpc.ManagedChannel
import io.grpc.ManagedChannelBuilder
import java.security.Security
import org.bouncycastle.jce.provider.BouncyCastleProvider
import org.onflow.protobuf.access.AccessAPIGrpc
import org.onflow.sdk.impl.AsyncFlowAccessApiImpl
import org.onflow.sdk.impl.FlowAccessApiImpl

object Flow {

    const val DEFAULT_USER_AGENT = "Flow Java SDK"

    init {
        Security.addProvider(BouncyCastleProvider())
    }

    fun newAccessApi(host: String, port: Int, secure: Boolean = true, userAgent: String = DEFAULT_USER_AGENT): FlowAccessApi {
        val channel = openChannel(host, port, secure, userAgent)
        return FlowAccessApiImpl(AccessAPIGrpc.newBlockingStub(channel))
    }

    fun newAsyncAccessApi(host: String, port: Int, secure: Boolean = true, userAgent: String = DEFAULT_USER_AGENT): AsyncFlowAccessApi {
        val channel = openChannel(host, port, secure, userAgent)
        return AsyncFlowAccessApiImpl(AccessAPIGrpc.newFutureStub(channel))
    }

    fun newAccessApi(chain: FlowChainId, secure: Boolean = true, userAgent: String = DEFAULT_USER_AGENT): FlowAccessApi {
        return newAccessApi(chain.endpoint, chain.poort, secure, userAgent)
    }

    fun newAsyncAccessApi(chain: FlowChainId, secure: Boolean = true, userAgent: String = DEFAULT_USER_AGENT): AsyncFlowAccessApi {
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
}
