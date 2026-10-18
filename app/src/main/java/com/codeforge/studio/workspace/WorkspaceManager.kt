package com.codeforge.studio.workspace

import android.content.Context
import com.codeforge.studio.filesystem.SafeZipManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.InputStream
import java.util.UUID

class WorkspaceManager(private val context: Context) {

    private val workspaceBaseDir: File
        get() = File(context.filesDir, "workspaces").apply { if (!exists()) mkdirs() }

    private val recentProjectsFile: File
        get() = File(context.filesDir, "recent_projects.json")

    private val _currentProject = MutableStateFlow<Project?>(null)
    val currentProject: StateFlow<Project?> = _currentProject.asStateFlow()

    private val _recentProjects = MutableStateFlow<List<ProjectMetadata>>(emptyList())
    val recentProjects: StateFlow<List<ProjectMetadata>> = _recentProjects.asStateFlow()

    init {
        loadRecentProjects()
        // Ensure default starter project exists if workspace is fresh
        if (_recentProjects.value.isEmpty()) {
            createSampleProjectsIfEmpty()
        }
    }

    private fun loadRecentProjects() {
        if (!recentProjectsFile.exists()) return
        runCatching {
            val jsonStr = recentProjectsFile.readText()
            val array = JSONArray(jsonStr)
            val list = mutableListOf<ProjectMetadata>()
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                val path = obj.getString("path")
                if (File(path).exists()) {
                    list.add(
                        ProjectMetadata(
                            id = obj.getString("id"),
                            name = obj.getString("name"),
                            path = path,
                            primaryLanguage = obj.optString("primaryLanguage", "python"),
                            lastOpened = obj.optLong("lastOpened", System.currentTimeMillis()),
                            template = obj.optString("template", null)
                        )
                    )
                }
            }
            _recentProjects.value = list.sortedByDescending { it.lastOpened }
        }
    }

    private fun saveRecentProjects() {
        runCatching {
            val array = JSONArray()
            _recentProjects.value.forEach { meta ->
                val obj = JSONObject().apply {
                    put("id", meta.id)
                    put("name", meta.name)
                    put("path", meta.path)
                    put("primaryLanguage", meta.primaryLanguage)
                    put("lastOpened", meta.lastOpened)
                    put("template", meta.template)
                }
                array.put(obj)
            }
            recentProjectsFile.writeText(array.toString(2))
        }
    }

    private fun createSampleProjectsIfEmpty() {
        val sampleDir = File(workspaceBaseDir, "Demo-Python-Project")
        if (!sampleDir.exists()) {
            sampleDir.mkdirs()
            val mainPy = File(sampleDir, "main.py")
            mainPy.writeText(
                """
                # CodeForge Studio - Welcome Demo Project
                # You can run this file directly in the Run panel or Terminal!

                def greet(name: str) -> str:
                    return f"Hello, {name}! Welcome to CodeForge Studio."

                def fibonacci(n: int):
                    a, b = 0, 1
                    series = []
                    for _ in range(n):
                        series.append(a)
                        a, b = b, a + b
                    return series

                if __name__ == "__main__":
                    print("=" * 40)
                    print(greet("Mobile Developer"))
                    print("=" * 40)
                    print("Computing Fibonacci sequence (first 8 numbers):")
                    print("Result:", fibonacci(8))
                    print("\nAll systems operational!")
                """.trimIndent()
            )
            val readme = File(sampleDir, "README.md")
            readme.writeText(
                """
                # Welcome to CodeForge Studio
                An offline-first Android Mobile Development Studio.

                ### Features:
                - Multi-tab touch-optimized editor
                - Real terminal & script runner
                - Source control with Git diffs
                - 14 supported languages
                """.trimIndent()
            )
            val meta = ProjectMetadata(
                id = UUID.randomUUID().toString(),
                name = "Demo-Python-Project",
                path = sampleDir.absolutePath,
                primaryLanguage = "python"
            )
            _recentProjects.value = listOf(meta)
            saveRecentProjects()
        }
    }

    suspend fun createProject(name: String, language: String, templateType: String? = null): Result<Project> = withContext(Dispatchers.IO) {
        runCatching {
            val sanitizedName = name.trim().replace(Regex("[^a-zA-Z0-9._-]"), "_")
            val projectDir = File(workspaceBaseDir, sanitizedName)
            if (projectDir.exists()) {
                throw IllegalArgumentException("Project '$sanitizedName' already exists.")
            }
            projectDir.mkdirs()

            // Initialize files based on template
            when (templateType) {
                "web" -> {
                    File(projectDir, "index.html").writeText(
                        "<!DOCTYPE html>\n<html>\n<head>\n  <title>$name</title>\n  <link rel=\"stylesheet\" href=\"style.css\">\n</head>\n<body>\n  <h1>Welcome to $name</h1>\n  <p>Live preview ready!</p>\n  <script src=\"app.js\"></script>\n</body>\n</html>"
                    )
                    File(projectDir, "style.css").writeText(
                        "body { background: #101216; color: #58A6FF; font-family: sans-serif; padding: 20px; }"
                    )
                    File(projectDir, "app.js").writeText(
                        "console.log('App initialized inside CodeForge Studio');"
                    )
                }
                "kotlin" -> {
                    File(projectDir, "Main.kt").writeText(
                        "fun main() {\n    println(\"Hello from Kotlin on CodeForge Studio!\")\n}"
                    )
                }
                "rust" -> {
                    File(projectDir, "main.rs").writeText(
                        "fn main() {\n    println!(\"Hello from Rust on CodeForge Studio!\");\n}"
                    )
                }
                "go" -> {
                    File(projectDir, "main.go").writeText(
                        "package main\n\nimport \"fmt\"\n\nfunc main() {\n    fmt.Println(\"Hello from Go on CodeForge Studio!\")\n}"
                    )
                }
                "cpp" -> {
                    File(projectDir, "main.cpp").writeText(
                        "#include <iostream>\n\nint main() {\n    std::cout << \"Hello from C++ on CodeForge Studio!\" << std::endl;\n    return 0;\n}"
                    )
                }
                else -> {
                    // Default Python
                    File(projectDir, "main.py").writeText(
                        "# $name - CodeForge Studio\n\ndef main():\n    print(\"Hello from $name!\")\n\nif __name__ == '__main__':\n    main()\n"
                    )
                }
            }
            File(projectDir, "README.md").writeText("# $name\nCreated with CodeForge Studio.")

            val metadata = ProjectMetadata(
                id = UUID.randomUUID().toString(),
                name = sanitizedName,
                path = projectDir.absolutePath,
                primaryLanguage = language,
                template = templateType
            )
            addRecentProject(metadata)
            val project = Project(metadata, projectDir)
            _currentProject.value = project
            project
        }
    }

    suspend fun openProject(metadata: ProjectMetadata): Result<Project> = withContext(Dispatchers.IO) {
        runCatching {
            val dir = File(metadata.path)
            if (!dir.exists() || !dir.isDirectory) {
                throw IllegalArgumentException("Project directory does not exist: ${metadata.path}")
            }
            val updatedMeta = metadata.copy(lastOpened = System.currentTimeMillis())
            addRecentProject(updatedMeta)
            val project = Project(updatedMeta, dir)
            _currentProject.value = project
            project
        }
    }

    suspend fun importZip(zipStream: InputStream, projectName: String): Result<Project> = withContext(Dispatchers.IO) {
        runCatching {
            val sanitizedName = projectName.trim().replace(Regex("[^a-zA-Z0-9._-]"), "_")
            val targetDir = File(workspaceBaseDir, sanitizedName)
            if (targetDir.exists()) {
                throw IllegalArgumentException("Directory '$sanitizedName' already exists.")
            }
            SafeZipManager.unzipSafely(zipStream, targetDir).getOrThrow()

            val meta = ProjectMetadata(
                id = UUID.randomUUID().toString(),
                name = sanitizedName,
                path = targetDir.absolutePath,
                primaryLanguage = "python"
            )
            addRecentProject(meta)
            val project = Project(meta, targetDir)
            _currentProject.value = project
            project
        }
    }

    private fun addRecentProject(meta: ProjectMetadata) {
        val existing = _recentProjects.value.filter { it.path != meta.path }.toMutableList()
        existing.add(0, meta)
        _recentProjects.value = existing
        saveRecentProjects()
    }

    fun closeCurrentProject() {
        _currentProject.value = null
    }
}
