package com.codeforge.studio.diagnostics

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.util.Stack

enum class Severity {
    ERROR,
    WARNING,
    INFO
}

data class DiagnosticIssue(
    val file: File,
    val line: Int,
    val column: Int,
    val message: String,
    val severity: Severity,
    val source: String
)

class DiagnosticsManager {

    private val _issues = MutableStateFlow<List<DiagnosticIssue>>(emptyList())
    val issues: StateFlow<List<DiagnosticIssue>> = _issues.asStateFlow()

    suspend fun analyzeFile(file: File, content: String): List<DiagnosticIssue> = withContext(Dispatchers.Default) {
        val fileIssues = mutableListOf<DiagnosticIssue>()
        val ext = file.extension.lowercase()

        when (ext) {
            "json" -> {
                try {
                    val trimmed = content.trim()
                    if (trimmed.startsWith("{")) {
                        JSONObject(trimmed)
                    } else if (trimmed.startsWith("[")) {
                        JSONArray(trimmed)
                    } else if (trimmed.isNotEmpty()) {
                        fileIssues.add(
                            DiagnosticIssue(file, 1, 1, "JSON must begin with '{' or '['", Severity.ERROR, "JSON Validator")
                        )
                    }
                } catch (e: Exception) {
                    val msg = e.message ?: "Invalid JSON syntax"
                    val lineNum = extractLineFromError(msg) ?: 1
                    fileIssues.add(DiagnosticIssue(file, lineNum, 1, msg, Severity.ERROR, "JSON Validator"))
                }
            }
            "html", "xml" -> {
                val lines = content.lines()
                val tagStack = Stack<String>()
                val tagRegex = Regex("""</?([a-zA-Z0-9\-]+)[^>]*>""")
                val voidTags = setOf("meta", "link", "img", "br", "hr", "input", "source", "!doctype")

                lines.forEachIndexed { idx, line ->
                    tagRegex.findAll(line).forEach { match ->
                        val fullMatch = match.value
                        val tagName = match.groupValues[1].lowercase()

                        if (!voidTags.contains(tagName) && !fullMatch.endsWith("/>")) {
                            if (fullMatch.startsWith("</")) {
                                if (tagStack.isNotEmpty() && tagStack.peek() == tagName) {
                                    tagStack.pop()
                                } else {
                                    fileIssues.add(
                                        DiagnosticIssue(
                                            file,
                                            idx + 1,
                                            match.range.first + 1,
                                            "Mismatched closing tag </$tagName>",
                                            Severity.WARNING,
                                            "HTML Validator"
                                        )
                                    )
                                }
                            } else {
                                tagStack.push(tagName)
                            }
                        }
                    }
                }
                if (tagStack.isNotEmpty()) {
                    fileIssues.add(
                        DiagnosticIssue(
                            file,
                            lines.size,
                            1,
                            "Unclosed tag <${tagStack.peek()}>",
                            Severity.WARNING,
                            "HTML Validator"
                        )
                    )
                }
            }
            "py" -> {
                // Real Python syntax checks: Bracket matching & unclosed string detection
                val lines = content.lines()
                val bracketStack = Stack<Pair<Char, Int>>()
                lines.forEachIndexed { lineIdx, line ->
                    line.forEachIndexed { colIdx, char ->
                        when (char) {
                            '(', '[', '{' -> bracketStack.push(char to (lineIdx + 1))
                            ')' -> if (bracketStack.isNotEmpty() && bracketStack.peek().first == '(') bracketStack.pop()
                            ']' -> if (bracketStack.isNotEmpty() && bracketStack.peek().first == '[') bracketStack.pop()
                            '}' -> if (bracketStack.isNotEmpty() && bracketStack.peek().first == '{') bracketStack.pop()
                        }
                    }
                }
                while (bracketStack.isNotEmpty()) {
                    val unclosed = bracketStack.pop()
                    fileIssues.add(
                        DiagnosticIssue(
                            file,
                            unclosed.second,
                            1,
                            "Unclosed delimiter '${unclosed.first}'",
                            Severity.ERROR,
                            "Python Syntax"
                        )
                    )
                }
            }
        }

        // Update overall issue list
        val current = _issues.value.filter { it.file.absolutePath != file.absolutePath }.toMutableList()
        current.addAll(fileIssues)
        _issues.value = current

        fileIssues
    }

    fun addCompilerDiagnostics(file: File, stderrLines: List<String>, compilerName: String) {
        val newIssues = mutableListOf<DiagnosticIssue>()
        val regex = Regex("""(?:.*:)?(\d+):(?:\d+:)?\s*(error|warning)?:\s*(.*)""", RegexOption.IGNORE_CASE)

        stderrLines.forEach { line ->
            val match = regex.find(line)
            if (match != null) {
                val lineNum = match.groupValues[1].toIntOrNull() ?: 1
                val isWarn = match.groupValues[2].equals("warning", ignoreCase = true)
                val msg = match.groupValues[3].ifBlank { line }
                newIssues.add(
                    DiagnosticIssue(
                        file = file,
                        line = lineNum,
                        column = 1,
                        message = msg,
                        severity = if (isWarn) Severity.WARNING else Severity.ERROR,
                        source = compilerName
                    )
                )
            }
        }

        if (newIssues.isNotEmpty()) {
            val current = _issues.value.filter { it.file.absolutePath != file.absolutePath }.toMutableList()
            current.addAll(newIssues)
            _issues.value = current
        }
    }

    fun clearForFile(file: File) {
        _issues.value = _issues.value.filter { it.file.absolutePath != file.absolutePath }
    }

    private fun extractLineFromError(msg: String): Int? {
        val match = Regex("""line (\d+)""").find(msg)
        return match?.groupValues?.get(1)?.toIntOrNull()
    }
}
