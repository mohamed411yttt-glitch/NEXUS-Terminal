package com.nexus.terminal.runtime

import java.io.File

enum class NexusRuntimeType(val displayName: String) {
    PYTHON("Python"),
    NODE("Node.js")
}

enum class NexusRuntimeStatus {
    AVAILABLE,
    NOT_INSTALLED,
    INVALID
}

data class NexusRuntimeInfo(
    val type: NexusRuntimeType,
    val status: NexusRuntimeStatus,
    val executable: File? = null,
    val message: String
)

class NexusRuntimeManager(runtimePath: String) {

    private val runtimeRoot = File(runtimePath).canonicalFile

    private val candidates = mapOf(
        NexusRuntimeType.PYTHON to listOf(
            "python/bin/python3",
            "python/bin/python"
        ),
        NexusRuntimeType.NODE to listOf(
            "node/bin/node",
            "node/node"
        )
    )

    init {
        runtimeRoot.mkdirs()
    }

    fun inspect(type: NexusRuntimeType): NexusRuntimeInfo {
        val paths = candidates[type] ?: emptyList()

        for (relativePath in paths) {
            val executable = File(runtimeRoot, relativePath)
            
            val canonical = try {
                executable.canonicalFile
            } catch (_: Exception) {
                continue
            }

            if (!canonical.path.startsWith(
                    runtimeRoot.path + File.separator
                )) {
                continue
            }

            if (canonical.exists()) {
                if (canonical.isFile &&
                    canonical.length() > 0 &&
                    canonical.canExecute()
                ) {
                    return NexusRuntimeInfo(
                        type = type,
                        status = NexusRuntimeStatus.AVAILABLE,
                        executable = canonical,
                        message = "${type.displayName} is available."
                    )
                }

                return NexusRuntimeInfo(
                    type = type,
                    status = NexusRuntimeStatus.INVALID,
                    message = "${type.displayName} exists but is not executable."
                )
            }
        }

        return NexusRuntimeInfo(
            type = type,
            status = NexusRuntimeStatus.NOT_INSTALLED,
            message = "${type.displayName} is not installed."
        )
    }

    fun inspectAll(): List<NexusRuntimeInfo> {
        return NexusRuntimeType.values().map { inspect(it) }
    }

    fun getExecutable(type: NexusRuntimeType): File? {
        val info = inspect(type)

        return if (info.status == NexusRuntimeStatus.AVAILABLE) {
            info.executable
        } else {
            null
        }
    }

    fun getRuntimeDirectory(): File {
        return runtimeRoot
    }

    fun getAvailableSpaceBytes(): Long {
        return runtimeRoot.usableSpace
    }

    fun hasEnoughSpace(requiredBytes: Long): Boolean {
        if (requiredBytes < 0) return false
        return getAvailableSpaceBytes() >= requiredBytes
    }
}
