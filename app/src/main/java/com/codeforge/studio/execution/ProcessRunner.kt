package com.codeforge.studio.execution

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader
import java.util.concurrent.TimeUnit

class ProcessRunner {

    private val _liveOutput = MutableSharedFlow<OutputLine>(replay = 100)
    val liveOutput: SharedFlow<OutputLine> = _liveOutput.asSharedFlow()

    @Volatile
    private var activeProcess: Process? = null

    suspend fun execute(
        command: List<String>,
        workingDir: File,
        environment: Map<String, String> = emptyMap(),
        timeoutSeconds: Long = 60
    ): ExecutionResult = withContext(Dispatchers.IO) {
        val startTime = System.currentTimeMillis()
        val outputLines = mutableListOf<OutputLine>()
        var isCancelled = false
        var exitCode = -1
        var errorMsg: String? = null

        val cmdStr = command.joinToString(" ")
        emitLine("Executing: $cmdStr in ${workingDir.absolutePath}", OutputType.SYSTEM_INFO, outputLines)

        try {
            val pb = ProcessBuilder(command).apply {
                directory(workingDir)
                redirectErrorStream(false)
                environment().putAll(environment)
            }

            val process = pb.start()
            activeProcess = process

            // Read stdout in background
            val stdoutReader = BufferedReader(InputStreamReader(process.inputStream, Charsets.UTF_8))
            val stderrReader = BufferedReader(InputStreamReader(process.errorStream, Charsets.UTF_8))

            // Thread for stdout
            val stdoutThread = Thread {
                runCatching {
                    var line: String?
                    while (stdoutReader.readLine().also { line = it } != null) {
                        emitLine(line ?: "", OutputType.STDOUT, outputLines)
                    }
                }
            }.apply { isDaemon = true; start() }

            // Thread for stderr
            val stderrThread = Thread {
                runCatching {
                    var line: String?
                    while (stderrReader.readLine().also { line = it } != null) {
                        emitLine(line ?: "", OutputType.STDERR, outputLines)
                    }
                }
            }.apply { isDaemon = true; start() }

            val completed = process.waitFor(timeoutSeconds, TimeUnit.SECONDS)
            if (!completed) {
                process.destroyForcibly()
                isCancelled = true
                errorMsg = "Execution timed out after $timeoutSeconds seconds."
                emitLine(errorMsg, OutputType.SYSTEM_ERROR, outputLines)
            } else {
                exitCode = process.exitValue()
            }

            stdoutThread.join(1000)
            stderrThread.join(1000)

        } catch (e: CancellationException) {
            isCancelled = true
            activeProcess?.destroyForcibly()
            errorMsg = "Process cancelled by user."
            emitLine(errorMsg, OutputType.SYSTEM_INFO, outputLines)
        } catch (e: Exception) {
            exitCode = -1
            errorMsg = "Failed to launch process: ${e.message}"
            emitLine(errorMsg, OutputType.SYSTEM_ERROR, outputLines)
        } finally {
            activeProcess = null
        }

        val duration = System.currentTimeMillis() - startTime
        emitLine("Process exited with code $exitCode ($duration ms)", OutputType.SYSTEM_INFO, outputLines)

        ExecutionResult(
            command = cmdStr,
            exitCode = exitCode,
            durationMs = duration,
            output = outputLines,
            isCancelled = isCancelled,
            errorMessage = errorMsg
        )
    }

    private fun emitLine(text: String, type: OutputType, list: MutableList<OutputLine>) {
        val line = OutputLine(text, type)
        list.add(line)
        _liveOutput.tryEmit(line)
    }

    fun cancelActiveProcess() {
        activeProcess?.destroyForcibly()
        activeProcess = null
    }
}
