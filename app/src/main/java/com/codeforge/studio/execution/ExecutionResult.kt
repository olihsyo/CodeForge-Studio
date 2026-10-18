package com.codeforge.studio.execution

enum class OutputType {
    STDOUT,
    STDERR,
    SYSTEM_INFO,
    SYSTEM_ERROR
}

data class OutputLine(
    val text: String,
    val type: OutputType,
    val timestamp: Long = System.currentTimeMillis()
)

data class ExecutionResult(
    val command: String,
    val exitCode: Int,
    val durationMs: Long,
    val output: List<OutputLine>,
    val isCancelled: Boolean = false,
    val errorMessage: String? = null
) {
    val isSuccess: Boolean get() = exitCode == 0 && !isCancelled && errorMessage == null
}
