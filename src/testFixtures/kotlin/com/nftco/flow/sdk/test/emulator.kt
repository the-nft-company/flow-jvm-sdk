package com.nftco.flow.sdk.test

import com.nftco.flow.sdk.Flow
import com.nftco.flow.sdk.impl.FlowAccessApiImpl
import java.io.File
import java.io.IOException
import kotlin.io.path.createTempDirectory

fun runFlow(
    executable: String = "flow",
    arguments: String? = null,
    host: String = "localhost",
    port: Int = 3570,
    httpPort: Int = 8081,
    flowJsonLocation: String? = null,
    postStartCommands: Array<FlowEmulatorCommand> = emptyArray(),
    classLoader: ClassLoader = AbstractFlowEmulatorExtension::class.java.classLoader,
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

    var workingDirectory: File? = null

    val configFile = if (flowJson != null) {
        "--config-path $flowJson"
    } else {
        workingDirectory = createTempDirectory("flow-emulator").toFile()
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
    var proc = ProcessBuilder()
        .command(emulatorCommand.split(" "))
        .inheritIO()
    if (workingDirectory != null) {
        proc = proc.directory(workingDirectory)
    }
    val ret = proc.start()

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
