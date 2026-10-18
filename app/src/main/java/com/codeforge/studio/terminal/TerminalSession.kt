package com.codeforge.studio.terminal

import com.codeforge.studio.execution.OutputLine
import com.codeforge.studio.execution.OutputType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.BufferedWriter
import java.io.File
import java.io.InputStreamReader
import java.io.OutputStreamWriter

class TerminalSession(initialDir: File) {

    private val _lines = MutableStateFlow<List<OutputLine>>(emptyList())
    val lines: StateFlow<List<OutputLine>> = _lines.asStateFlow()

    private val _currentDir = MutableStateFlow(initialDir)
    val currentDir: StateFlow<File> = _currentDir.asStateFlow()

    private val _isRunning = MutableStateFlow(false)
    val isRunning: StateFlow<Boolean> = _isRunning.asStateFlow()

    private val _history = MutableStateFlow<List<String>>(emptyList())
    val history: StateFlow<List<String>> = _history.asStateFlow()

    private var process: Process? = null
    private var writer: BufferedWriter? = null
    private var stdoutReader: BufferedReader? = null
    private var stderrReader: BufferedReader? = null

    val environmentInfo: String
        get() = if (System.getProperty("os.name")?.contains("Windows", ignoreCase = true) == true) {
            "Host Shell (cmd.exe)"
        } else {
            "Android Shell (/system/bin/sh)"
        }

    init {
        appendLine("CodeForge Studio Terminal [Environment: $environmentInfo]", OutputType.SYSTEM_INFO)
        appendLine("Working directory: ${initialDir.absolutePath}", OutputType.SYSTEM_INFO)
        appendLine("Type commands (e.g. ls, pwd, cat, env) or run scripts.", OutputType.SYSTEM_INFO)
        startShellProcess()
    }

    fun restartSession() {
        kill()
        _isRunning.value = false
        appendLine("Restarting terminal session...", OutputType.SYSTEM_INFO)
        startShellProcess()
    }

    private fun startShellProcess() {
        runCatching {
            val isWindows = System.getProperty("os.name")?.contains("Windows", ignoreCase = true) == true
            val shellCommand = when {
                isWindows -> listOf("cmd.exe")
                File("/system/bin/sh").canExecute() -> listOf("/system/bin/sh")
                File("/bin/sh").canExecute() -> listOf("/bin/sh")
                else -> listOf("sh")
            }

            val pb = ProcessBuilder(shellCommand)
                .directory(_currentDir.value)
                .redirectErrorStream(false)

            if (!isWindows) {
                pb.environment()["TERM"] = "xterm-256color"
            }

            val p = pb.start()
            process = p
            _isRunning.value = true

            writer = BufferedWriter(OutputStreamWriter(p.outputStream, Charsets.UTF_8))
            stdoutReader = BufferedReader(InputStreamReader(p.inputStream, Charsets.UTF_8))
            stderrReader = BufferedReader(InputStreamReader(p.errorStream, Charsets.UTF_8))

            // Start reading stdout
            Thread {
                val reader = stdoutReader ?: return@Thread
                runCatching {
                    var line: String?
                    while (reader.readLine().also { line = it } != null) {
                        appendLine(line ?: "", OutputType.STDOUT)
                    }
                }
                _isRunning.value = false
            }.apply { isDaemon = true; start() }

            // Start reading stderr
            Thread {
                val reader = stderrReader ?: return@Thread
                runCatching {
                    var line: String?
                    while (reader.readLine().also { line = it } != null) {
                        appendLine(line ?: "", OutputType.STDERR)
                    }
                }
            }.apply { isDaemon = true; start() }

        }.onFailure { e ->
            _isRunning.value = false
            appendLine("Failed to spawn local terminal process: ${e.message}", OutputType.SYSTEM_ERROR)
        }
    }

    suspend fun sendCommand(command: String) = withContext(Dispatchers.IO) {
        val trimmed = command.trim()
        if (trimmed.isEmpty()) return@withContext

        // Record in history
        val hist = _history.value.toMutableList()
        hist.remove(trimmed)
        hist.add(trimmed)
        _history.value = hist

        appendLine("$ ${_currentDir.value.name} % $trimmed", OutputType.SYSTEM_INFO)

        // Custom built-in handlers
        if (trimmed.startsWith("cd ")) {
            val target = trimmed.removePrefix("cd ").trim()
            val newDir = if (target == ".." || target == "../") {
                _currentDir.value.parentFile ?: _currentDir.value
            } else {
                val candidate = File(_currentDir.value, target)
                if (candidate.exists() && candidate.isDirectory) candidate else File(target)
            }

            if (newDir.exists() && newDir.isDirectory) {
                _currentDir.value = newDir
                appendLine("Changed directory to: ${newDir.absolutePath}", OutputType.SYSTEM_INFO)
            } else {
                appendLine("cd: no such file or directory: $target", OutputType.STDERR)
            }
            return@withContext
        }

        if (trimmed == "clear") {
            _lines.value = emptyList()
            return@withContext
        }

        val w = writer
        if (w != null && _isRunning.value) {
            runCatching {
                w.write(trimmed)
                w.newLine()
                w.flush()
            }.onFailure { e ->
                appendLine("Write error: ${e.message}", OutputType.SYSTEM_ERROR)
            }
        } else {
            // If shell exited, try running single command directly via ProcessBuilder
            runSingleCommand(trimmed)
        }
    }

    private suspend fun runSingleCommand(cmd: String) = withContext(Dispatchers.IO) {
        runCatching {
            val isWindows = System.getProperty("os.name")?.contains("Windows", ignoreCase = true) == true
            val commandList = if (isWindows) {
                listOf("cmd.exe", "/c", cmd)
            } else {
                val shell = when {
                    File("/system/bin/sh").canExecute() -> "/system/bin/sh"
                    File("/bin/sh").canExecute() -> "/bin/sh"
                    else -> "sh"
                }
                listOf(shell, "-c", cmd)
            }
            val p = ProcessBuilder(commandList)
                .directory(_currentDir.value)
                .start()

            val out = p.inputStream.bufferedReader().readLines()
            val err = p.errorStream.bufferedReader().readLines()
            out.forEach { appendLine(it, OutputType.STDOUT) }
            err.forEach { appendLine(it, OutputType.STDERR) }
            val code = p.waitFor()
            if (code != 0) {
                appendLine("[Process exited with code $code]", OutputType.SYSTEM_ERROR)
            }
        }.onFailure { e ->
            appendLine("Execution failed: ${e.message}", OutputType.SYSTEM_ERROR)
        }
    }

    private fun appendLine(text: String, type: OutputType) {
        val line = OutputLine(text, type)
        val current = _lines.value.toMutableList()
        current.add(line)
        if (current.size > 2000) current.removeAt(0)
        _lines.value = current
    }

    fun kill() {
        runCatching {
            process?.destroyForcibly()
            writer?.close()
            stdoutReader?.close()
            stderrReader?.close()
        }
        _isRunning.value = false
        appendLine("Terminal session terminated.", OutputType.SYSTEM_INFO)
    }
}
