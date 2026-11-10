package com.nexus.terminal.engine

import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit
import kotlin.concurrent.thread

data class NexusProcess(
    val id: String,
    val name: String,
    val process: Process
)

class NexusProcessManager(
    private val homePath: String,
    private val runtimePath: String
) {
    private val home = File(homePath).canonicalFile
    private val runtime = File(runtimePath).canonicalFile

    private val processes =
        ConcurrentHashMap<String, NexusProcess>()

    private val allowedExecutables = setOf(
        "python",
        "python3",
        "node"
    )

    init {
        home.mkdirs()
        runtime.mkdirs()
    }

    private fun emit(callback: (String) -> Unit, message: String) {
        try {
            callback(message)
        } catch (_: Exception) {
            // A callback failure must not stop process monitoring.
        }
    }

    @Synchronized
    fun start(
        executable: File,
        arguments: List<String> = emptyList(),
        workingDirectory: File = home,
        onOutput: (String) -> Unit,
        onExit: (Int) -> Unit = {}
    ): String {
        val binary = executable.canonicalFile
        val directory = workingDirectory.canonicalFile

        require(binary.name in allowedExecutables) {
            "Executable is not allowed."
        }

        require(binary.isFile && binary.canExecute()) {
            "Runtime executable is missing or not executable."
        }

        require(binary.path.startsWith(runtime.path + File.separator)) {
            "Executable must be inside NEXUS runtime directory."
        }

        require(
            directory.path == home.path ||
            directory.path.startsWith(home.path + File.separator)
        ) {
            "Working directory must be inside NEXUS home."
        }

        require(directory.isDirectory) {
            "Working directory does not exist."
        }

        val process = ProcessBuilder(
            listOf(binary.absolutePath) + arguments
        )
            .directory(directory)
            .redirectErrorStream(true)
            .start()

        val id = UUID.randomUUID().toString().take(8)

        processes[id] = NexusProcess(
            id = id,
            name = binary.name,
            process = process
        )

        thread(name = "nexus-process-$id") {
            try {
                BufferedReader(
                    InputStreamReader(process.inputStream)
                ).use { reader ->
                    while (true) {
                        val line = reader.readLine() ?: break
                        emit(onOutput, line)
                    }
                }
            } catch (error: Exception) {
                if (process.isAlive) {
                    emit(
                        onOutput,
                        "Process output error: ${error.message}"
                    )
                }
            } finally {
                val exitCode = try {
                    process.waitFor()
                } catch (_: InterruptedException) {
                    Thread.currentThread().interrupt()
                    -1
                }

                processes.remove(id)
                try {
                    onExit(exitCode)
                } catch (_: Exception) {
                    // Ignore callback failures.
                }
            }
        }

        return id
    }

    fun writeInput(id: String, input: String): Boolean {
        val session = processes[id] ?: return false

        if (!session.process.isAlive) return false

        return try {
            session.process.outputStream.apply {
                write(input.toByteArray(Charsets.UTF_8))
                flush()
            }
            true
        } catch (_: Exception) {
            false
        }
    }

    fun stop(id: String): Boolean {
        val session = processes[id] ?: return false
        val process = session.process

        return try {
            process.destroy()

            if (!process.waitFor(300, TimeUnit.MILLISECONDS)) {
                process.destroyForcibly()
            }

            true
        } catch (_: InterruptedException) {
            Thread.currentThread().interrupt()
            process.destroyForcibly()
            true
        } catch (_: Exception) {
            false
        }
    }

    fun stopAll() {
        processes.keys.toList().forEach { id ->
            stop(id)
        }
    }

    fun isRunning(id: String): Boolean {
        return processes[id]?.process?.isAlive == true
    }

    fun listRunning(): List<String> {
        return processes.values
            .filter { it.process.isAlive }
            .map { "${it.id}  ${it.name}" }
    }
}
