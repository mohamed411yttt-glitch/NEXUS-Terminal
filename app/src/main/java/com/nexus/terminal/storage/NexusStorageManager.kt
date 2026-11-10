package com.nexus.terminal.storage

import java.io.File

data class NexusFileInfo(
    val name: String,
    val path: String,
    val isDirectory: Boolean,
    val sizeBytes: Long,
    val lastModified: Long
)

class NexusStorageManager(homePath: String) {

    private val home = File(homePath).canonicalFile

    init {
        home.mkdirs()
    }

    private fun resolve(path: String): File {
        val target = File(home, path).canonicalFile

        require(
            target.path == home.path ||
            target.path.startsWith(home.path + File.separator)
        ) {
            "Access outside NEXUS home is not allowed."
        }

        return target
    }

    fun list(path: String = "."): List<NexusFileInfo> {
        val directory = resolve(path)

        require(directory.isDirectory) {
            "Directory does not exist."
        }

        return (directory.listFiles()
            ?: throw IllegalStateException("Cannot read directory."))
            .sortedWith(
                compareBy<File> { !it.isDirectory }
                    .thenBy { it.name.lowercase() }
            )
            .map { file ->
                NexusFileInfo(
                    name = file.name,
                    path = file.relativeTo(home).path,
                    isDirectory = file.isDirectory,
                    sizeBytes = if (file.isFile) file.length() else 0L,
                    lastModified = file.lastModified()
                )
            }
    }

    fun createDirectory(path: String): Boolean {
        val directory = resolve(path)

        if (directory.exists()) return false

        return directory.mkdirs()
    }

    fun createFile(path: String): Boolean {
        val file = resolve(path)

        if (file.exists()) return false

        file.parentFile?.mkdirs()

        return file.createNewFile()
    }

    fun readText(
        path: String,
        maxBytes: Long = 2L * 1024L * 1024L
    ): String {
        val file = resolve(path)

        require(file.isFile) {
            "File does not exist."
        }

        require(file.length() <= maxBytes) {
            "File is too large to read."
        }

        return file.readText(Charsets.UTF_8)
    }

    fun writeText(path: String, content: String): Boolean {
        val file = resolve(path)

        require(!file.isDirectory) {
            "Cannot write text to a directory."
        }

        file.parentFile?.mkdirs()
        file.writeText(content, Charsets.UTF_8)

        return true
    }

    fun delete(path: String): Boolean {
        val file = resolve(path)

        require(file.path != home.path) {
            "Cannot delete NEXUS home directory."
        }

        if (!file.exists()) return false

        require(!file.isDirectory || file.list()?.isEmpty() == true) {
            "Directory must be empty before deletion."
        }

        return file.delete()
    }

    fun exists(path: String): Boolean {
        return resolve(path).exists()
    }

    fun getHomeDirectory(): File {
        return home
    }
}
