package com.nftco.flow.sdk.test

import com.nftco.flow.sdk.*
import com.nftco.flow.sdk.cadence.AddressField
import com.nftco.flow.sdk.cadence.EventField
import com.nftco.flow.sdk.crypto.Crypto
import com.nftco.flow.sdk.crypto.KeyPair
import com.nftco.flow.sdk.impl.AsyncFlowAccessApiImpl
import com.nftco.flow.sdk.impl.FlowAccessApiImpl
import org.apiguardian.api.API
import org.junit.jupiter.api.extension.AfterEachCallback
import org.junit.jupiter.api.extension.BeforeEachCallback
import org.junit.jupiter.api.extension.ExtensionContext
import org.junit.jupiter.api.extension.TestExecutionExceptionHandler
import java.io.File
import java.lang.annotation.Inherited
import java.lang.reflect.Field
import java.math.BigDecimal
import java.net.InetAddress
import java.net.ServerSocket
import java.util.concurrent.TimeUnit

@Target(AnnotationTarget.FIELD)
@Retention(AnnotationRetention.RUNTIME)
@MustBeDocumented
@Inherited
@API(status = API.Status.STABLE, since = "5.0")
annotation class FlowTestClient

@Retention(AnnotationRetention.RUNTIME)
@MustBeDocumented
@Inherited
@API(status = API.Status.STABLE, since = "5.0")
annotation class FlowEmulatorCommand(
    val value: String = "flow",
    val expectedExitValue: Int = 0,
    val throwOnError: Boolean = true,
    val timeout: Long = 10,
    val unit: TimeUnit = TimeUnit.SECONDS
)

@Target(AnnotationTarget.FIELD)
@Retention(AnnotationRetention.RUNTIME)
@MustBeDocumented
@Inherited
@API(status = API.Status.STABLE, since = "5.0")
annotation class FlowServiceAccountCredentials

@Target(AnnotationTarget.FIELD)
@Retention(AnnotationRetention.RUNTIME)
@MustBeDocumented
@Inherited
@API(status = API.Status.STABLE, since = "5.0")
annotation class FlowTestAccount(
    val signAlgo: SignatureAlgorithm = SignatureAlgorithm.ECDSA_SECP256k1,
    val hashAlgo: HashAlgorithm = HashAlgorithm.SHA3_256,
    val publicKey: String = "",
    val privateKey: String = "",
    val balance: Double = 0.01
)

data class TestAccount(
    val address: String,
    val privateKey: String,
    val publicKey: String,
    val signAlgo: SignatureAlgorithm,
    val hashAlgo: HashAlgorithm,
    val keyIndex: Int,
    val balance: BigDecimal
) {
    val signer: Signer
        get() = Crypto.getSigner(
            privateKey = Crypto.decodePrivateKey(privateKey),
            hashAlgo = hashAlgo
        )

    val flowAddress: FlowAddress get() = FlowAddress(address)

    val isValid: Boolean get() = !address.isEmpty()
        && !privateKey.isEmpty()
        && !publicKey.isEmpty()
        && signAlgo != SignatureAlgorithm.UNKNOWN
        && hashAlgo != HashAlgorithm.UNKNOWN
        && keyIndex >= 0
}

data class Emulator(
    val process: Process,
    val pidFile: File,
    val host: String,
    val port: Int,
    val httpPort: Int,
    val serviceAccount: TestAccount
)

abstract class AbstractFlowEmulatorExtension : BeforeEachCallback, AfterEachCallback, TestExecutionExceptionHandler {

    var process: Process? = null
    var pidFile: File? = null
    var accessApi: FlowAccessApiImpl? = null
    var asyncAccessApi: AsyncFlowAccessApiImpl? = null

    protected abstract fun launchEmulator(context: ExtensionContext): Emulator

    protected fun <T : Annotation> withAnnotatedTestFields(context: ExtensionContext, clazz: Class<T>, block: (Any, Field, T) -> Unit) {
        val tests = (
            context.testInstances.map { it.allInstances.toSet() }.orElseGet { emptySet() }
                + context.testInstance.map { setOf(it) }.orElseGet { emptySet() }
            )

        tests.map { it to it.javaClass.fields }
            .flatMap { it.second.map { f -> it.first to f } }
            .filter { it.second.isAnnotationPresent(clazz) }
            .map { block(it.first, it.second, it.second.getAnnotation(clazz)) }
    }

    override fun beforeEach(context: ExtensionContext) {

        Flow.configureDefaults(chainId = FlowChainId.EMULATOR)

        val emulator = launchEmulator(context)
        this.process = emulator.process
        this.pidFile = emulator.pidFile

        this.accessApi = Flow.newAccessApi(
            host = emulator.host,
            port = emulator.port
        ) as FlowAccessApiImpl

        this.asyncAccessApi = Flow.newAsyncAccessApi(
            host = emulator.host,
            port = emulator.port
        ) as AsyncFlowAccessApiImpl

        withAnnotatedTestFields(context, FlowTestClient::class.java) { instance, field, annotation ->
            if (field.type.equals(FlowAccessApi::class.java)) {
                field.isAccessible = true
                field.set(instance, accessApi)
            } else if (field.type.equals(AsyncFlowAccessApi::class.java)) {
                field.isAccessible = true
                field.set(instance, asyncAccessApi)
            } else {
                throw IllegalArgumentException(
                    "field $field is not of type FlowAccessApi or AsyncFlowAccessAPi"
                )
            }
        }

        withAnnotatedTestFields(context, FlowServiceAccountCredentials::class.java) { instance, field, _ ->
            if (!field.type.equals(TestAccount::class.java)) {
                throw IllegalArgumentException("field $field is not of type TestAccount")
            } else if (!emulator.serviceAccount.isValid) {
                throw IllegalArgumentException(
                    "FLOW Service account configuration is not valid"
                )
            }
            field.isAccessible = true
            field.set(instance, emulator.serviceAccount)
        }

        withAnnotatedTestFields(context, FlowTestAccount::class.java) { instance, field, annotation ->
            if (!field.type.equals(TestAccount::class.java)) {
                throw IllegalArgumentException("field $field is not of type TestAccount")
            } else if (!emulator.serviceAccount.isValid) {
                throw IllegalArgumentException(
                    "FLOW Service account configuration is not valid, cannot create a FlowTestAccount"
                )
            }

            val keyPair = if (annotation.privateKey.isEmpty() && annotation.publicKey.isEmpty()) {
                Crypto.generateKeyPair(annotation.signAlgo)
            } else {
                KeyPair(
                    private = Crypto.decodePrivateKey(annotation.privateKey, annotation.signAlgo),
                    public = Crypto.decodePublicKey(annotation.publicKey, annotation.signAlgo)
                )
            }

            val result = this.accessApi!!.simpleFlowTransaction(
                address = emulator.serviceAccount.flowAddress,
                signer = emulator.serviceAccount.signer,
                keyIndex = emulator.serviceAccount.keyIndex
            ) {
                script {
                    """
                    import FlowToken from 0xFLOWTOKEN
                    import FungibleToken from 0xFUNGIBLETOKEN
                    
                    transaction(startingBalance: UFix64, publicKey: String, signatureAlgorithm: UInt8, hashAlgorithm: UInt8) {
                        prepare(signer: AuthAccount) {

                            let newAccount = AuthAccount(payer: signer)
                            
                            let provider = signer.borrow<&FlowToken.Vault>(from: /storage/flowTokenVault)
                                ?? panic("Could not borrow FlowToken.Vault reference")
                            
                            let newVault = newAccount
                                .getCapability(/public/flowTokenReceiver)
                                .borrow<&{FungibleToken.Receiver}>()
                                ?? panic("Could not borrow FungibleToken.Receiver reference")

                            let coin <- provider.withdraw(amount: startingBalance)
                            newVault.deposit(from: <- coin)
                            
                            newAccount.keys.add(
                                publicKey: PublicKey(
                                    publicKey: publicKey.decodeHex(),
                                    signatureAlgorithm: SignatureAlgorithm(rawValue: signatureAlgorithm)!
                                ),
                                hashAlgorithm: HashAlgorithm(rawValue: hashAlgorithm)!,
                                weight: UFix64(1000)
                            )
                        }
                    }
                """
                }
                gasLimit(1000)
                arguments {
                    arg { ufix64(annotation.balance) }
                    arg { string(keyPair.public.hex) }
                    arg { uint8(annotation.signAlgo.index) }
                    arg { uint8(annotation.hashAlgo.index) }
                }
            }.sendAndWaitForSeal()
                .throwOnError()

            val address = result.events
                .find { it.type == "flow.AccountCreated" }
                ?.payload
                ?.let { (it.jsonCadence as EventField).value }
                ?.getRequiredField<AddressField>("address")
                ?.value
                ?: throw FlowException("Couldn't find AccountCreated event with address for account that was created")

            val testAccount = TestAccount(
                address = address, // TODO: is this a safe assumption?
                privateKey = keyPair.private.hex,
                publicKey = keyPair.public.hex,
                signAlgo = annotation.signAlgo,
                hashAlgo = annotation.hashAlgo,
                keyIndex = 0,
                balance = BigDecimal(annotation.balance)
            )

            field.isAccessible = true
            field.set(instance, testAccount)
        }

        Runtime.getRuntime().addShutdownHook(
            Thread(this::shutdownEmulator)
        )
    }

    override fun afterEach(context: ExtensionContext) = shutdownEmulator()

    override fun handleTestExecutionException(context: ExtensionContext, throwable: Throwable) {
        try {
            shutdownEmulator()
        } finally {
            throw throwable
        }
    }

    private fun shutdownEmulator() {
        val api = accessApi
        if (api != null) {
            api.close()
            this.accessApi = null
        }
        val asyncApi = asyncAccessApi
        if (asyncApi != null) {
            asyncApi.close()
            this.asyncAccessApi = null
        }
        val proc = process
        if (proc != null) {
            proc.destroy()
            var count = 0
            while (!proc.waitFor(1, TimeUnit.SECONDS)) {
                proc.destroyForcibly()
                count++
                if (count >= 60) {
                    throw IllegalStateException("Unable to terminate flow emulator process")
                }
            }
        }
        if (pidFile != null) {
            pidFile?.delete()
            pidFile = null
        }
        process = null
    }

    protected fun findFreePort(host: String): Int {
        return ServerSocket(0, 50, InetAddress.getByName(host)).use { it.localPort }
    }
}
