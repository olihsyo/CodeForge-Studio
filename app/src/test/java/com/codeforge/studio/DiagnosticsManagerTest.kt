package com.codeforge.studio

import com.codeforge.studio.diagnostics.DiagnosticsManager
import com.codeforge.studio.diagnostics.Severity
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class DiagnosticsManagerTest {

    private val diagnosticsManager = DiagnosticsManager()

    @Test
    fun testJsonDiagnostics() = runBlocking {
        val validFile = File("valid.json")
        val validIssues = diagnosticsManager.analyzeFile(validFile, "{\"name\": \"CodeForge\", \"version\": 1}")
        assertTrue(validIssues.isEmpty())

        val invalidFile = File("invalid.json")
        val invalidIssues = diagnosticsManager.analyzeFile(invalidFile, "{ \"name\": }")
        assertEquals(1, invalidIssues.size)
        assertEquals(Severity.ERROR, invalidIssues[0].severity)
        assertEquals("JSON Validator", invalidIssues[0].source)
    }

    @Test
    fun testHtmlTagDiagnostics() = runBlocking {
        val validFile = File("index.html")
        val validHtml = "<html><head><title>App</title></head><body><h1>Hello</h1></body></html>"
        val validIssues = diagnosticsManager.analyzeFile(validFile, validHtml)
        assertTrue(validIssues.isEmpty())

        val unclosedFile = File("broken.html")
        val unclosedHtml = "<div><p>Unclosed paragraph</div>"
        val issues = diagnosticsManager.analyzeFile(unclosedFile, unclosedHtml)
        assertTrue(issues.isNotEmpty())
        assertTrue(issues.any { it.message.contains("Mismatched") || it.message.contains("Unclosed") })
    }

    @Test
    fun testPythonDelimiterDiagnostics() = runBlocking {
        val validPy = File("valid.py")
        val validContent = "def add(a, b):\n    return (a + b)\n"
        val validIssues = diagnosticsManager.analyzeFile(validPy, validContent)
        assertTrue(validIssues.isEmpty())

        val invalidPy = File("invalid.py")
        val invalidContent = "items = [1, 2, 3\nprint(items)"
        val invalidIssues = diagnosticsManager.analyzeFile(invalidPy, invalidContent)
        assertTrue(invalidIssues.isNotEmpty())
        assertTrue(invalidIssues.any { it.message.contains("Unclosed delimiter") })
    }

    @Test
    fun testCompilerDiagnosticsParsing() {
        val srcFile = File("Main.kt")
        val stderr = listOf(
            "Main.kt:14: error: unresolved reference: foo",
            "Main.kt:22: warning: variable 'bar' is never used"
        )

        diagnosticsManager.addCompilerDiagnostics(srcFile, stderr, "Kotlin Compiler")
        val issues = diagnosticsManager.issues.value.filter { it.file.name == "Main.kt" }

        assertEquals(2, issues.size)
        val err = issues.first { it.severity == Severity.ERROR }
        assertEquals(14, err.line)
        assertTrue(err.message.contains("unresolved reference: foo"))

        val warn = issues.first { it.severity == Severity.WARNING }
        assertEquals(22, warn.line)
        assertTrue(warn.message.contains("variable 'bar' is never used"))

        diagnosticsManager.clearForFile(srcFile)
        val cleared = diagnosticsManager.issues.value.filter { it.file.name == "Main.kt" }
        assertTrue(cleared.isEmpty())
    }
}
