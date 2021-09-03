package com.nftco.flow.sdk.test

import com.nftco.flow.sdk.AsyncFlowAccessApi
import com.nftco.flow.sdk.Flow
import com.nftco.flow.sdk.FlowAccessApi
import com.nftco.flow.sdk.impl.AsyncFlowAccessApiImpl
import com.nftco.flow.sdk.impl.FlowAccessApiImpl
import org.apiguardian.api.API
import org.junit.jupiter.api.extension.*
import java.io.File
import java.io.IOException
import java.lang.annotation.Inherited
import java.util.concurrent.TimeUnit

@Target(
    AnnotationTarget.CLASS,
    AnnotationTarget.FUNCTION
)
@Retention(AnnotationRetention.RUNTIME)
@MustBeDocumented
@Inherited
@ExtendWith(FlowEmulatorExtension::class)
@API(status = API.Status.STABLE, since = "5.0")
annotation class FlowEmulatorTest(

    val executable: String = "flow",

    val arguments: String = "--log debug --verbose",

    val host: String = "localhost",

    val port: Int = 3570,

    val httpPort: Int = 8182,

    val postStartCommands: Array<FlowEmulatorCommand> = [],

    /**
     * Location of flow.json, can also be in the classpath or
     * a directory containing flow.json.
     */
    val flowJsonLocation: String = "flow.json",

    val pidFilename: String = "flow-emulator.pid"
)

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

@Target(
    AnnotationTarget.FIELD
)
@Retention(AnnotationRetention.RUNTIME)
@MustBeDocumented
@Inherited
@API(status = API.Status.STABLE, since = "5.0")
annotation class FlowTestClient

class FlowEmulatorExtension : BeforeEachCallback, AfterEachCallback, TestExecutionExceptionHandler {

    var process: Process? = null
    var pidFile: File? = null
    var accessApi: FlowAccessApiImpl? = null
    var asyncAccessApi: AsyncFlowAccessApiImpl? = null

    override fun beforeEach(context: ExtensionContext) {
        if (context.requiredTestClass.isAnnotationPresent(FlowEmulatorTest::class.java)) {
            val config = context.requiredTestClass.getAnnotation(FlowEmulatorTest::class.java)
            val (process, pidFile) = runFlow(
                executable = config.executable,
                arguments = config.arguments.trim().takeIf { it.isNotEmpty() },
                host = config.host,
                port = config.port,
                httpPort = config.httpPort,
                postStartCommands = config.postStartCommands,
                flowJsonLocation = config.flowJsonLocation.trim().takeIf { it.isNotEmpty() },
                pidFilename = config.pidFilename
            )
            this.process = process
            this.pidFile = pidFile
            this.accessApi = Flow.newAccessApi(
                host = config.host,
                port = config.port
            ) as FlowAccessApiImpl
            this.asyncAccessApi = Flow.newAsyncAccessApi(
                host = config.host,
                port = config.port
            ) as AsyncFlowAccessApiImpl

            val tests = (
                context.testInstances.map { it.allInstances.toSet() }.orElseGet { emptySet() }
                    + context.testInstance.map { setOf(it) }.orElseGet { emptySet() }
                )

            tests.map { it to it.javaClass.fields }
                .flatMap { it.second.map { f -> it.first to f } }
                .filter { it.second.isAnnotationPresent(FlowTestClient::class.java) }
                .forEach {
                    if (it.second.type.equals(FlowAccessApi::class.java)) {
                        it.second.isAccessible = true
                        it.second.set(it.first, accessApi)
                    } else if (it.second.type.equals(AsyncFlowAccessApi::class.java)) {
                        it.second.isAccessible = true
                        it.second.set(it.first, asyncAccessApi)
                    } else {
                        throw IllegalArgumentException(
                            "field ${it.second} is not of type FlowAccessApi or AsyncFlowAccessAPi"
                        )
                    }
                }

            Runtime.getRuntime().addShutdownHook(
                Thread(this::shutdownEmulator)
            )
        }
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
}

fun runFlow(
    executable: String = "flow",
    arguments: String? = null,
    host: String = "localhost",
    port: Int = 3570,
    httpPort: Int = 8081,
    flowJsonLocation: String? = null,
    postStartCommands: Array<FlowEmulatorCommand> = emptyArray(),
    classLoader: ClassLoader = FlowEmulatorExtension::class.java.classLoader,
    pidFilename: String = "flow-emulator.pid"
): Pair<Process, File> {

    var flowJson: String? = null

    val pidFile = File(System.getProperty("java.io.tmpdir"), pidFilename)
    if (pidFile.exists()) {
        // TODO: maybe a better way of doing this?
        // we only have to do this because sometimes the process
        // is left alive and there doesn't seem to be a way to
        // stop it remotely by connecting to it and issuing a
        // shutdown command or similar. The only other thing I
        // can think of is to start the emulator on a random port
        // and inject it into the test, but then we may leave
        // a bunch of rogue emulators running on the client machine.
        // The only time a rogue emulator is left running is when
        // the JVM is forcibly killed before the shutdownEmulator
        // method is called on the FlowEmulatorExtension, this seems to
        // only happen when debugging unit tests in the IDE.
        try {
            val pid = String(pidFile.readBytes()).toLong()
            ProcessHandle.of(pid).ifPresent { it.destroyForcibly() }
        } catch (e: Throwable) {
            println("Error forcibly killing a zombie emulator process, tests may fail")
        }
    }
    pidFile.delete()

    // is it a file?
    if (flowJson == null) {
        flowJson = flowJsonLocation?.let(::File)
            ?.takeIf { it.exists() }
            ?.takeIf { it.isFile }
            ?.absolutePath
    }

    // is it in the classpath?
    if (flowJson == null) {
        flowJson = flowJsonLocation?.let(classLoader::getResource)
            ?.openStream()
            ?.use { input ->
                val tmp = File.createTempFile("flow", ".json")
                tmp.deleteOnExit()
                tmp.outputStream().use { output -> input.copyTo(output) }
                tmp
            }
            ?.absolutePath
    }

    // is it a directory with a flow.json file
    if (flowJson == null) {
        flowJson = flowJsonLocation?.let(::File)
            ?.takeIf { it.exists() }
            ?.takeIf { it.isDirectory }
            ?.let { File(it, "flow.json") }
            ?.takeIf { it.exists() }
            ?.absolutePath
    }

    val configFile = if (flowJson != null) {
        "--config-path $flowJson"
    } else {
        "--init"
    }

    val cmd = if (File(executable).exists() && File(executable).isFile && File(executable).canExecute()) {
        executable
    } else {
        (
            listOf("${System.getProperty("user.home")}/.local/bin", "/usr/local/bin", "/usr/bin", "/bin")
                + (System.getenv()["PATH"]?.split(File.pathSeparator) ?: emptyList())
            )
            .map { File(it, "flow") }
            .find { it.exists() }
            ?: throw IOException("flow command not found")
    }

    val emulatorCommand = "$cmd emulator $arguments --port $port --http-port $httpPort $configFile"

    val start = System.currentTimeMillis()
    val ret = ProcessBuilder()
        .command(emulatorCommand.split(" "))
        .inheritIO()
        .start()
    if (ret.isAlive) {
        pidFile.writeBytes(ret.pid().toString().toByteArray())
    }

    val api = Flow.newAccessApi(host = host, port = port) as FlowAccessApiImpl
    while (true) {
        val elapsed = System.currentTimeMillis() - start
        try {
            api.ping()
            break
        } catch (t: Throwable) {
            if (elapsed > 25_000) {
                throw IllegalStateException("Unable to find flow process that was started after 25 seconds")
            }
        }
        if (!ret.isAlive) {
            throw IllegalStateException("Flow process died after $elapsed milliseconds")
        }
    }
    api.close()

    // run commands
    for (postStartCommand in postStartCommands) {
        val exec = "$cmd ${postStartCommand.value} -n emulator $configFile"
        val process = ProcessBuilder()
            .command(exec.split(" "))
            .inheritIO()
            .start()
        if (!process.waitFor(postStartCommand.timeout, postStartCommand.unit) && postStartCommand.throwOnError) {
            throw IllegalStateException("Waiting for command failed; $exec")
        } else if (process.exitValue() != postStartCommand.expectedExitValue && postStartCommand.throwOnError) {
            throw IllegalStateException("Expected exit value ${postStartCommand.expectedExitValue} but got ${process.exitValue()} for command: $exec")
        }
    }

    // we're g2g
    return ret to pidFile
}
