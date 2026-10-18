package com.codeforge.studio

import com.codeforge.studio.runtimes.ExecutionMode
import com.codeforge.studio.runtimes.LanguageRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class LanguageRegistryTest {

    @Test
    fun testLanguageCountAndTargetCoverage() {
        val all = LanguageRegistry.languages
        assertTrue("Must support at least 14 initial target languages", all.size >= 14)

        val targetIds = listOf(
            "python", "javascript", "typescript", "html", "css", "json",
            "java", "kotlin", "c", "cpp", "rust", "go", "lua", "csharp"
        )

        targetIds.forEach { id ->
            val lang = LanguageRegistry.findById(id)
            assertNotNull("Language '$id' must be registered", lang)
            assertTrue("Language '$id' must declare file extensions", lang!!.extensions.isNotEmpty())
            assertTrue("Language '$id' must declare platform limitations", lang.platformLimitations.isNotBlank())
            assertTrue("Language '$id' must declare build requirements", lang.buildRequirements.isNotBlank())
        }
    }

    @Test
    fun testFindExtensions() {
        assertEquals("python", LanguageRegistry.findByExtension("py")?.id)
        assertEquals("javascript", LanguageRegistry.findByExtension("js")?.id)
        assertEquals("typescript", LanguageRegistry.findByExtension("ts")?.id)
        assertEquals("kotlin", LanguageRegistry.findByExtension("kt")?.id)
        assertEquals("rust", LanguageRegistry.findByExtension("rs")?.id)
        assertEquals("go", LanguageRegistry.findByExtension("go")?.id)
        assertEquals("c", LanguageRegistry.findByExtension("c")?.id)
        assertEquals("cpp", LanguageRegistry.findByExtension("cpp")?.id)
    }

    @Test
    fun testWebSandboxExecutionModes() {
        val html = LanguageRegistry.findById("html")
        assertEquals(ExecutionMode.WEB_SANDBOX, html?.executionMode)

        val js = LanguageRegistry.findById("javascript")
        assertEquals(ExecutionMode.WEB_SANDBOX, js?.executionMode)
    }
}
