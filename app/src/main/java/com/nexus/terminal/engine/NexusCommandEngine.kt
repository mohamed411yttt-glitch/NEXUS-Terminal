package com.nexus.terminal.engine

import java.io.File
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

data class EngineResult(
    val output: List<String>,
    val currentDirectory: String,
    val clearScreen: Boolean = false
)

class NexusCommandEngine(homePath: String) {

    private val home = File(homePath).canonicalFile

    var currentDirectory: String = home.absolutePath
        private set

    init {
        home.mkdirs()
    }

    fun execute(
        commandLine: String,
        history: List<String> = emptyList()
    ): EngineResult {
        val args = tokenize(commandLine)

        if (args.isEmpty()) return result()

        val command = args.first()
        val params = args.drop(1)

        return try {
            when (command) {
                "help" -> result(
                    listOf(
                        "NEXUS COMMAND ENGINE",
                        "",
                        "help       Show available commands",
                        "pwd        Show current directory",
                        "ls         List files",
                        "cd         Change directory",
                        "cat        Read a text file",
                        "mkdir      Create directories",
                        "touch      Create files",
                        "echo       Print text",
                        "date       Show date and time",
                        "whoami     Show current user",
                        "uname      Show environment information",
                        "history    Show command history",
                        "clear      Clear terminal output",
                        "",
                        "All file operations are restricted to NEXUS home."
                    )
                )

                "pwd" -> result(listOf(currentDirectory))

                "whoami" -> result(listOf("nexus"))

                "date" -> result(
                    listOf(
                        LocalDateTime.now().format(
                            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
                        )
                    )
                )

                "uname" -> result(
                    listOf(
                        "NEXUS Android Environment",
                        "Architecture: ${System.getProperty("os.arch") ?: "unknown"}",
                        "OS: ${System.getProperty("os.name") ?: "Android"}"
                    )
                )

                "echo" -> result(listOf(params.joinToString(" ")))

                "clear" -> EngineResult(
                    output = emptyList(),
                    currentDirectory = currentDirectory,
                    clearScreen = true
                )

                "history" -> result(
                    if (history.isEmpty()) {
                        listOf("No command history.")
                    } else {
                        history.mapIndexed { index, item ->
                            "${index + 1}  $item"
                        }
                    }
                )

                "cd" -> changeDirectory(params.firstOrNull() ?: "~")

                "ls" -> listFiles(params)

                "cat" -> readFiles(params)

                "mkdir" -> makeDirectories(params)

                "touch" -> createFiles(params)

                else -> result(
                    listOf(
                        "ERROR: Command not available: $command",
                        "Type 'help' to see available commands."
                    )
                )
            }
        } catch (e: Exception) {
            result(
                listOf("ERROR: ${e.message ?: "Command failed."}")
            )
        }
    }

    private fun changeDirectory(path: String): EngineResult {
        val target = resolvePath(path)

        if (target == null || !target.exists() || !target.isDirectory) {
            return result(listOf("ERROR: Directory not found or access denied."))
        }

        currentDirectory = target.absolutePath
        return result()
    }

    private fun listFiles(params: List<String>): EngineResult {
        val showHidden = params.any {
            it == "-a" || it == "-la" || it == "-al"
        }

        val targetPath = params.firstOrNull {
            !it.startsWith("-")
        } ?: "."

        val target = resolvePath(targetPath)
            ?: return result(listOf("ERROR: Access denied."))

        if (!target.exists()) {
            return result(listOf("ERROR: Path not found."))
        }

        if (target.isFile) {
            return result(listOf(target.name))
        }

        val files = target.listFiles()
            ?: return result(listOf("ERROR: Cannot read directory."))

        val names = files
            .filter { showHidden || !it.name.startsWith(".") }
            .sortedWith(
                compareBy<File> { !it.isDirectory }
                    .thenBy { it.name.lowercase() }
            )
            .map {
                if (it.isDirectory) "${it.name}/" else it.name
            }

        return result(names.ifEmpty { listOf("(empty directory)") })
    }

    private fun readFiles(params: List<String>): EngineResult {
        if (params.isEmpty()) {
            return result(listOf("ERROR: Specify a filename."))
        }

        val output = mutableListOf<String>()

        for (path in params) {
            val file = resolvePath(path)
                ?: return result(listOf("ERROR: Access denied: $path"))

            if (!file.exists() || !file.isFile) {
                return result(listOf("ERROR: File not found: $path"))
            }

            if (file.length() > 1024 * 1024) {
                return result(listOf("ERROR: File is larger than 1 MB."))
            }

            if (params.size > 1) {
                output.add("==> $path <==")
            }

            val content = file.readText(Charsets.UTF_8)
            output.addAll(content.lines())
        }

        return result(output)
    }

    private fun makeDirectories(params: List<String>): EngineResult {
        val paths = params.filter { it != "-p" }

        if (paths.isEmpty()) {
            return result(listOf("ERROR: Specify a directory."))
        }

        val output = paths.map { path ->
            val directory = resolvePath(path)

            when {
                directory == null ->
                    "ERROR: Access denied: $path"

                directory.exists() && directory.isDirectory ->
                    "Already exists: $path"

                directory.exists() ->
                    "ERROR: A file already exists at this path."

                directory.mkdirs() ->
                    "Created: $path"

                else ->
                    "ERROR: Could not create: $path"
            }
        }

        return result(output)
    }

    private fun createFiles(params: List<String>): EngineResult {
        if (params.isEmpty()) {
            return result(listOf("ERROR: Specify a filename."))
        }

        val output = params.map { path ->
            val file = resolvePath(path)

            when {
                file == null ->
                    "ERROR: Access denied: $path"

                file.exists() && file.isFile ->
                    "Already exists: $path"

                file.exists() ->
                    "ERROR: A directory already exists at this path."

                file.parentFile?.isDirectory != true ->
                    "ERROR: Parent directory does not exist."

                file.createNewFile() ->
                    "Created: $path"

                else ->
                    "ERROR: Could not create: $path"
            }
        }

        return result(output)
    }

    private fun resolvePath(path: String): File? {
        return try {
            val current = File(currentDirectory).canonicalFile

            val candidate = when {
                path == "~" -> home
                path.startsWith("~/") ->
                    File(home, path.removePrefix("~/"))
                else ->
                    File(current, path)
            }.canonicalFile

            val basePath = home.path
            val targetPath = candidate.path

            if (
                targetPath == basePath ||
                targetPath.startsWith(basePath + File.separator)
            ) {
                candidate
            } else {
                null
            }
        } catch (_: Exception) {
            null
        }
    }

    private fun result(
        output: List<String> = emptyList()
    ) = EngineResult(
        output = output,
        currentDirectory = currentDirectory
    )

    private fun tokenize(input: String): List<String> {
        val tokens = mutableListOf<String>()
        val current = StringBuilder()

        var quote: Char? = null
        var escaped = false

        for (char in input) {
            when {
                escaped -> {
                    current.append(char)
                    escaped = false
                }

                char == '\\' && quote != '\'' -> {
                    escaped = true
                }

                quote != null && char == quote -> {
                    quote = null
                }

                quote == null && (char == '"' || char == '\'') -> {
                    quote = char
                }

                quote == null && char.isWhitespace() -> {
                    if (current.isNotEmpty()) {
                        tokens.add(current.toString())
                        current.clear()
                    }
                }

                else -> current.append(char)
            }
        }

        if (escaped) current.append('\\')

        if (quote != null) {
            throw IllegalArgumentException("Unclosed quotation mark.")
        }

        if (current.isNotEmpty()) {
            tokens.add(current.toString())
        }

        return tokens
    }
}
