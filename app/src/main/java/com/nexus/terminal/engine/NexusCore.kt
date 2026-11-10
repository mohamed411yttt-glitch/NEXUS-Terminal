package com.nexus.terminal.engine

import com.nexus.terminal.runtime.NexusRuntimeInfo
import com.nexus.terminal.runtime.NexusRuntimeManager
import com.nexus.terminal.runtime.NexusRuntimeType
import com.nexus.terminal.storage.NexusStorageManager
import java.io.File

class NexusCore(dataDirectory: File) {

    private val root = File(dataDirectory, "nexus").canonicalFile

    val homeDirectory = File(root, "home")
    val runtimeDirectory = File(root, "runtimes")

    val sessions: NexusSessionManager
    val processes: NexusProcessManager
    val runtimes: NexusRuntimeManager
    val storage: NexusStorageManager

    init {
        root.mkdirs()
        homeDirectory.mkdirs()
        runtimeDirectory.mkdirs()

        sessions = NexusSessionManager(
            homeDirectory.absolutePath
        )

        processes = NexusProcessManager(
            homeDirectory.absolutePath,
            runtimeDirectory.absolutePath
        )

        runtimes = NexusRuntimeManager(
            runtimeDirectory.absolutePath
        )

        storage = NexusStorageManager(
            homeDirectory.absolutePath
        )

        sessions.createSession("main")
    }

    fun execute(command: String): EngineResult {
        val session = sessions.getActiveSession()
            ?: sessions.createSession("main")

        if (command.isBlank()) {
            return session.engine.execute(command, session.history)
        }

        session.history.add(command)

        return session.engine.execute(
            command,
            session.history.toList()
        )
    }

    fun createSession(name: String? = null): NexusSession {
        return sessions.createSession(name)
    }

    fun switchSession(id: String): Boolean {
        return sessions.switchSession(id)
    }

    fun closeSession(id: String): Boolean {
        return sessions.closeSession(id)
    }

    fun getActiveSession(): NexusSession? {
        return sessions.getActiveSession()
    }

    fun getSessions(): List<NexusSession> {
        return sessions.listSessions()
    }

    fun inspectRuntimes(): List<NexusRuntimeInfo> {
        return runtimes.inspectAll()
    }

    fun startRuntime(
        type: NexusRuntimeType,
        arguments: List<String> = emptyList(),
        onOutput: (String) -> Unit,
        onExit: (Int) -> Unit = {}
    ): String {
        val executable = runtimes.getExecutable(type)
            ?: throw IllegalStateException(
                "${type.displayName} is not installed or is not executable."
            )

        return processes.start(
            executable = executable,
            arguments = arguments,
            workingDirectory = homeDirectory,
            onOutput = onOutput,
            onExit = onExit
        )
    }

    fun sendProcessInput(id: String, input: String): Boolean {
        return processes.writeInput(id, input)
    }

    fun stopProcess(id: String): Boolean {
        return processes.stop(id)
    }

    fun getRunningProcesses(): List<String> {
        return processes.listRunning()
    }

    fun getAvailableStorageBytes(): Long {
        return homeDirectory.usableSpace
    }

    fun shutdown() {
        processes.stopAll()
    }
}
