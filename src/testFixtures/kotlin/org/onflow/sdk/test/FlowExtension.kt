package org.onflow.sdk.test

import org.apiguardian.api.API
import org.junit.jupiter.api.extension.AfterEachCallback
import org.junit.jupiter.api.extension.BeforeEachCallback
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.api.extension.ExtensionContext
import org.junit.jupiter.api.extension.TestExecutionExceptionHandler
import org.onflow.sdk.Flow
import java.io.File
import java.io.IOException
import java.lang.annotation.Inherited
import java.util.concurrent.TimeUnit

class FlowExtension : BeforeEachCallback, AfterEachCallback, TestExecutionExceptionHandler {

    var process: Process? = null

    override fun beforeEach(context: ExtensionContext) {
        if (context.requiredTestClass.isAnnotationPresent(FlowEmulatorTest::class.java)) {
            val config = context.requiredTestClass.getAnnotation(FlowEmulatorTest::class.java)
            process = runFlow(
                executable = config.executable,
                arguments = config.arguments.trim().takeIf { it.isNotEmpty() },
                host = config.host,
                port = config.port,
                httpPort = config.httpPort,
                flowJsonLocation = config.flowJsonLocation.trim().takeIf { it.isNotEmpty() }
            )
            Runtime.getRuntime().addShutdownHook(
                Thread(this::shutdownEmulator)
            )
        }
    }

    override fun afterEach(context: ExtensionContext) = shutdownEmulator()

    override fun handleTestExecutionException(context: ExtensionContext, throwable: Throwable) = shutdownEmulator()

    private fun shutdownEmulator() {
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
        process = null
    }
}

@Target(
    AnnotationTarget.CLASS,
    AnnotationTarget.FUNCTION
)
@Retention(AnnotationRetention.RUNTIME)
@MustBeDocumented
@Inherited
@ExtendWith(FlowExtension::class)
@API(status = API.Status.STABLE, since = "5.0")
annotation class FlowEmulatorTest(

    val executable: String = "flow",

    val arguments: String = "--log debug",

    val host: String = "localhost",

    val port: Int = 3570,

    val httpPort: Int = 8081,

    /**
     * Location of flow.json, can also be in the classpath or
     * a directory containing flow.json.
     */
    val flowJsonLocation: String = "flow.json"
)

fun runFlow(
    executable: String = "flow",
    arguments: String? = null,
    host: String = "localhost",
    port: Int = 3570,
    httpPort: Int = 8081,
    flowJsonLocation: String? = null,
    classLoader: ClassLoader = FlowExtension::class.java.classLoader
): Process {

    var flowJson: String? = null

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

    val cmd = if (File(executable).exists()) {
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
        .redirectOutput(ProcessBuilder.Redirect.INHERIT)
        .redirectError(ProcessBuilder.Redirect.INHERIT)
        .start()

    val api = Flow.newAccessApi(host = host, port = port)
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

    return ret
}
