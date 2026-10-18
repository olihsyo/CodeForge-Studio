package com.codeforge.studio.filesystem

import java.io.File

data class FileNode(
    val file: File,
    val name: String = file.name,
    val isDirectory: Boolean = file.isDirectory,
    val extension: String = file.extension.lowercase(),
    val sizeBytes: Long = if (file.isFile) file.length() else 0L,
    val lastModified: Long = file.lastModified(),
    val children: List<FileNode> = emptyList(),
    val isExpanded: Boolean = false
) {
    val language: String
        get() = when (extension) {
            "py", "pyw" -> "python"
            "js", "mjs", "cjs" -> "javascript"
            "ts", "tsx" -> "typescript"
            "html", "htm" -> "html"
            "css", "scss", "sass" -> "css"
            "json" -> "json"
            "kt", "kts" -> "kotlin"
            "java" -> "java"
            "c", "h" -> "c"
            "cpp", "cc", "cxx", "hpp" -> "cpp"
            "rs" -> "rust"
            "go" -> "go"
            "lua" -> "lua"
            "cs" -> "csharp"
            "md", "markdown" -> "markdown"
            "xml", "svg" -> "xml"
            "sh", "bash" -> "shell"
            else -> "plaintext"
        }
}
