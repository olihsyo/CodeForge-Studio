package com.codeforge.studio.editor

import java.io.File

data class EditorTab(
    val file: File,
    val title: String = file.name,
    val content: String = "",
    val isDirty: Boolean = false,
    val cursorLine: Int = 1,
    val cursorCol: Int = 1,
    val language: String = file.extension.lowercase().let { ext ->
        when (ext) {
            "py", "pyw" -> "python"
            "js", "mjs" -> "javascript"
            "ts", "tsx" -> "typescript"
            "html", "htm" -> "html"
            "css" -> "css"
            "json" -> "json"
            "kt", "kts" -> "kotlin"
            "java" -> "java"
            "c", "h" -> "c"
            "cpp", "hpp" -> "cpp"
            "rs" -> "rust"
            "go" -> "go"
            "lua" -> "lua"
            "cs" -> "csharp"
            else -> "plaintext"
        }
    }
)
