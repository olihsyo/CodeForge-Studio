package com.codeforge.studio.runtimes

import java.io.File

enum class ExecutionMode {
    INTERPRETED,
    COMPILED,
    WEB_SANDBOX,
    SHELL_DELEGATE,
    UNSUPPORTED
}

data class LanguageCapability(
    val id: String,
    val displayName: String,
    val extensions: List<String>,
    val syntaxHighlighting: Boolean = true,
    val lspAvailable: Boolean = false,
    val formattingSupport: Boolean = false,
    val diagnosticsSupport: Boolean = true,
    val executionMode: ExecutionMode,
    val requiredBinaries: List<String>,
    val buildRequirements: String,
    val platformLimitations: String
) {
    /**
     * Checks if this runtime is actually installed and available on this Android host device or test host.
     * Searches system PATH, Termux toolchain (/data/data/com.termux/files/usr/bin), and standard Android paths.
     */
    fun isRuntimeInstalled(): Boolean {
        if (executionMode == ExecutionMode.WEB_SANDBOX) return true
        if (requiredBinaries.isEmpty()) return false

        val envPath = System.getenv("PATH") ?: ""
        val isWindows = System.getProperty("os.name")?.contains("Windows", ignoreCase = true) == true
        val standardAndroidDirs = listOf(
            "/system/bin",
            "/system/xbin",
            "/vendor/bin",
            "/data/data/com.termux/files/usr/bin"
        )

        val pathDirs = (envPath.split(File.pathSeparator).filter { it.isNotBlank() } + standardAndroidDirs).distinct()

        val extensionsToCheck = if (isWindows) listOf("", ".exe", ".cmd", ".bat") else listOf("")

        return requiredBinaries.any { bin ->
            pathDirs.any { dirPath ->
                val dir = File(dirPath)
                if (!dir.exists() || !dir.isDirectory) false
                else {
                    extensionsToCheck.any { ext ->
                        val target = File(dir, bin + ext)
                        target.exists() && (target.canExecute() || (!isWindows && target.isFile))
                    }
                }
            }
        }
    }
}

object LanguageRegistry {

    val languages = listOf(
        LanguageCapability(
            id = "python",
            displayName = "Python",
            extensions = listOf("py", "pyw"),
            syntaxHighlighting = true,
            lspAvailable = false,
            formattingSupport = true,
            diagnosticsSupport = true,
            executionMode = ExecutionMode.INTERPRETED,
            requiredBinaries = listOf("python3", "python"),
            buildRequirements = "Python interpreter (local binary, Termux environment, or system PATH)",
            platformLimitations = "Requires Python interpreter in PATH or Android environment."
        ),
        LanguageCapability(
            id = "javascript",
            displayName = "JavaScript",
            extensions = listOf("js", "mjs", "cjs"),
            syntaxHighlighting = true,
            lspAvailable = false,
            formattingSupport = true,
            diagnosticsSupport = true,
            executionMode = ExecutionMode.WEB_SANDBOX,
            requiredBinaries = listOf("node"),
            buildRequirements = "In-app V8 / WebView sandbox or optional Node.js runtime",
            platformLimitations = "Executes offline via Android WebView JS engine or system Node.js."
        ),
        LanguageCapability(
            id = "typescript",
            displayName = "TypeScript",
            extensions = listOf("ts", "tsx"),
            syntaxHighlighting = true,
            lspAvailable = false,
            formattingSupport = true,
            diagnosticsSupport = true,
            executionMode = ExecutionMode.COMPILED,
            requiredBinaries = listOf("tsc", "node", "deno", "bun"),
            buildRequirements = "TypeScript compiler (tsc) or Deno/Bun runtime",
            platformLimitations = "Needs tsc installed in user environment to compile to JavaScript."
        ),
        LanguageCapability(
            id = "html",
            displayName = "HTML",
            extensions = listOf("html", "htm"),
            syntaxHighlighting = true,
            lspAvailable = false,
            formattingSupport = true,
            diagnosticsSupport = true,
            executionMode = ExecutionMode.WEB_SANDBOX,
            requiredBinaries = emptyList(),
            buildRequirements = "Built-in Android WebView renderer",
            platformLimitations = "Full offline DOM and CSS/JS live preview supported directly."
        ),
        LanguageCapability(
            id = "css",
            displayName = "CSS",
            extensions = listOf("css", "scss", "sass"),
            syntaxHighlighting = true,
            lspAvailable = false,
            formattingSupport = true,
            diagnosticsSupport = true,
            executionMode = ExecutionMode.WEB_SANDBOX,
            requiredBinaries = emptyList(),
            buildRequirements = "Live Web Preview Engine",
            platformLimitations = "Rendered directly alongside HTML preview."
        ),
        LanguageCapability(
            id = "json",
            displayName = "JSON",
            extensions = listOf("json"),
            syntaxHighlighting = true,
            lspAvailable = false,
            formattingSupport = true,
            diagnosticsSupport = true,
            executionMode = ExecutionMode.WEB_SANDBOX,
            requiredBinaries = emptyList(),
            buildRequirements = "Built-in JSON validation parser",
            platformLimitations = "Offline parsing and structure validation supported."
        ),
        LanguageCapability(
            id = "kotlin",
            displayName = "Kotlin",
            extensions = listOf("kt", "kts"),
            syntaxHighlighting = true,
            lspAvailable = false,
            formattingSupport = true,
            diagnosticsSupport = true,
            executionMode = ExecutionMode.COMPILED,
            requiredBinaries = listOf("kotlinc", "kotlinc-jvm"),
            buildRequirements = "Kotlin compiler (kotlinc) and JVM runtime",
            platformLimitations = "Needs installed kotlinc or configured remote build server."
        ),
        LanguageCapability(
            id = "java",
            displayName = "Java",
            extensions = listOf("java"),
            syntaxHighlighting = true,
            lspAvailable = false,
            formattingSupport = true,
            diagnosticsSupport = true,
            executionMode = ExecutionMode.COMPILED,
            requiredBinaries = listOf("javac", "java"),
            buildRequirements = "Java Development Kit (javac & java)",
            platformLimitations = "Requires JDK installed on device or remote build."
        ),
        LanguageCapability(
            id = "c",
            displayName = "C",
            extensions = listOf("c", "h"),
            syntaxHighlighting = true,
            lspAvailable = false,
            formattingSupport = false,
            diagnosticsSupport = true,
            executionMode = ExecutionMode.COMPILED,
            requiredBinaries = listOf("clang", "gcc"),
            buildRequirements = "Clang or GCC C compiler and Android NDK toolchain",
            platformLimitations = "Android security policies restrict arbitrary unsigned binary execution."
        ),
        LanguageCapability(
            id = "cpp",
            displayName = "C++",
            extensions = listOf("cpp", "cc", "cxx", "hpp"),
            syntaxHighlighting = true,
            lspAvailable = false,
            formattingSupport = false,
            diagnosticsSupport = true,
            executionMode = ExecutionMode.COMPILED,
            requiredBinaries = listOf("clang++", "g++"),
            buildRequirements = "Clang++ or G++ compiler with C++17/20 standard library",
            platformLimitations = "Needs native compiler in user environment or remote runner."
        ),
        LanguageCapability(
            id = "rust",
            displayName = "Rust",
            extensions = listOf("rs"),
            syntaxHighlighting = true,
            lspAvailable = false,
            formattingSupport = false,
            diagnosticsSupport = true,
            executionMode = ExecutionMode.COMPILED,
            requiredBinaries = listOf("rustc", "cargo"),
            buildRequirements = "Rust compiler (rustc) and Cargo package manager",
            platformLimitations = "Requires Cargo toolchain on device or remote execution."
        ),
        LanguageCapability(
            id = "go",
            displayName = "Go",
            extensions = listOf("go"),
            syntaxHighlighting = true,
            lspAvailable = false,
            formattingSupport = true,
            diagnosticsSupport = true,
            executionMode = ExecutionMode.COMPILED,
            requiredBinaries = listOf("go"),
            buildRequirements = "Go toolchain (go build / go run)",
            platformLimitations = "Requires Go binary distribution in PATH."
        ),
        LanguageCapability(
            id = "lua",
            displayName = "Lua",
            extensions = listOf("lua"),
            syntaxHighlighting = true,
            lspAvailable = false,
            formattingSupport = false,
            diagnosticsSupport = true,
            executionMode = ExecutionMode.INTERPRETED,
            requiredBinaries = listOf("lua", "luajit"),
            buildRequirements = "Lua or LuaJIT interpreter",
            platformLimitations = "Requires Lua binary in PATH."
        ),
        LanguageCapability(
            id = "csharp",
            displayName = "C#",
            extensions = listOf("cs"),
            syntaxHighlighting = true,
            lspAvailable = false,
            formattingSupport = false,
            diagnosticsSupport = true,
            executionMode = ExecutionMode.COMPILED,
            requiredBinaries = listOf("dotnet", "mcs"),
            buildRequirements = ".NET SDK or Mono compiler",
            platformLimitations = "Requires .NET SDK or configured remote runner."
        )
    )

    fun findByExtension(ext: String): LanguageCapability? {
        val clean = ext.lowercase().trimStart('.')
        return languages.firstOrNull { it.extensions.contains(clean) }
    }

    fun findById(id: String): LanguageCapability? {
        return languages.firstOrNull { it.id.equals(id, ignoreCase = true) }
    }
}
