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
                            template = if (obj.has("template")) obj.getString("template") else null
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

    suspend fun openDirectory(dir: File): Result<Project> = withContext(Dispatchers.IO) {
        runCatching {
            if (!dir.exists() || !dir.isDirectory) {
                throw IllegalArgumentException("Directory does not exist: ${dir.absolutePath}")
            }
            val mainFile = dir.walkTopDown().firstOrNull { it.isFile && !it.name.startsWith(".") }
            val lang = when (mainFile?.extension?.lowercase()) {
                "py" -> "python"
                "html", "js", "css" -> "web"
                "kt" -> "kotlin"
                "java" -> "java"
                "rs" -> "rust"
                "cpp", "c" -> "cpp"
                "go" -> "go"
                "lua" -> "lua"
                else -> "code"
            }
            val meta = ProjectMetadata(
                id = UUID.randomUUID().toString(),
                name = dir.name,
                path = dir.absolutePath,
                primaryLanguage = lang,
                lastOpened = System.currentTimeMillis()
            )
            addRecentProject(meta)
            val project = Project(meta, dir)
            _currentProject.value = project
            project
        }
    }

    suspend fun deleteProject(projectId: String): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val meta = _recentProjects.value.firstOrNull { it.id == projectId }
            if (meta != null) {
                val dir = File(meta.path)
                if (dir.exists()) {
                    dir.deleteRecursively()
                }
                _recentProjects.value = _recentProjects.value.filter { it.id != projectId }
                saveRecentProjects()
                if (_currentProject.value?.metadata?.id == projectId) {
                    _currentProject.value = null
                }
            }
        }
    }

    suspend fun createProject(name: String, language: String, templateType: String? = null): Result<Project> = withContext(Dispatchers.IO) {
        runCatching {
            workspaceBaseDir.mkdirs()
            val rawName = if (name.isBlank()) "Project_${System.currentTimeMillis() % 10000}" else name.trim()
            var sanitized = rawName.replace(Regex("[^\\p{L}\\p{N}._-]"), "_").trim('_')
            if (sanitized.isBlank()) {
                sanitized = "Project_${System.currentTimeMillis() % 10000}"
            }

            var projectDir = File(workspaceBaseDir, sanitized)
            var counter = 1
            while (projectDir.exists()) {
                projectDir = File(workspaceBaseDir, "${sanitized}_$counter")
                counter++
            }
            projectDir.mkdirs()

            val actualName = projectDir.name

            // Initialize files based on template
            when (templateType?.lowercase()) {
                "web" -> {
                    File(projectDir, "index.html").writeText(
                        "<!DOCTYPE html>\n<html>\n<head>\n  <meta charset=\"UTF-8\">\n  <meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">\n  <title>$actualName</title>\n  <link rel=\"stylesheet\" href=\"style.css\">\n</head>\n<body>\n  <h1>Welcome to $actualName</h1>\n  <p>Live preview ready inside CodeForge Studio!</p>\n  <script src=\"app.js\"></script>\n</body>\n</html>"
                    )
                    File(projectDir, "style.css").writeText(
                        "body { background: #101216; color: #58A6FF; font-family: sans-serif; padding: 20px; }\nh1 { color: #8B5CF6; }"
                    )
                    File(projectDir, "app.js").writeText(
                        "console.log('App initialized inside CodeForge Studio');"
                    )
                }
                "kotlin" -> {
                    File(projectDir, "Main.kt").writeText(
                        "fun main() {\n    println(\"Hello from Kotlin on CodeForge Studio!\")\n}\n"
                    )
                }
                "java" -> {
                    File(projectDir, "Main.java").writeText(
                        "public class Main {\n    public static void main(String[] args) {\n        System.out.println(\"Hello from Java on CodeForge Studio!\");\n    }\n}\n"
                    )
                }
                "rust" -> {
                    File(projectDir, "main.rs").writeText(
                        "fn main() {\n    println!(\"Hello from Rust on CodeForge Studio!\");\n}\n"
                    )
                }
                "go" -> {
                    File(projectDir, "main.go").writeText(
                        "package main\n\nimport \"fmt\"\n\nfunc main() {\n    fmt.Println(\"Hello from Go on CodeForge Studio!\")\n}\n"
                    )
                }
                "cpp" -> {
                    File(projectDir, "main.cpp").writeText(
                        "#include <iostream>\n\nint main() {\n    std::cout << \"Hello from C++ on CodeForge Studio!\" << std::endl;\n    return 0;\n}\n"
                    )
                }
                "c" -> {
                    File(projectDir, "main.c").writeText(
                        "#include <stdio.h>\n\nint main(void) {\n    printf(\"Hello from C on CodeForge Studio!\\n\");\n    return 0;\n}\n"
                    )
                }
                "lua" -> {
                    File(projectDir, "main.lua").writeText(
                        "print(\"Hello from Lua on CodeForge Studio!\")\n"
                    )
                }
                "csharp" -> {
                    File(projectDir, "Program.cs").writeText(
                        "using System;\n\nclass Program {\n    static void Main() {\n        Console.WriteLine(\"Hello from C# on CodeForge Studio!\");\n    }\n}\n"
                    )
                }
                "empty" -> {
                    // Blank project with just README
                }
                else -> {
                    // Default Python
                    File(projectDir, "main.py").writeText(
                        "# $actualName - CodeForge Studio\n\ndef main():\n    print(\"Hello from $actualName!\")\n\nif __name__ == '__main__':\n    main()\n"
                    )
                }
            }
            File(projectDir, "README.md").writeText("# $actualName\nCreated with CodeForge Studio.")

            val metadata = ProjectMetadata(
                id = UUID.randomUUID().toString(),
                name = actualName,
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

    suspend fun renameProject(projectId: String, newName: String): Result<Project> = withContext(Dispatchers.IO) {
        runCatching {
            val cleanName = newName.trim().replace(Regex("[^\\p{L}\\p{N}._-]"), "_").trim('_')
            if (cleanName.isBlank()) throw IllegalArgumentException("Project name cannot be empty")

            val currentMeta = _recentProjects.value.firstOrNull { it.id == projectId }
                ?: throw IllegalArgumentException("Project metadata not found for ID: $projectId")

            val oldDir = File(currentMeta.path)
            val targetDir = File(oldDir.parentFile ?: workspaceBaseDir, cleanName)

            if (targetDir.exists() && targetDir.absolutePath != oldDir.absolutePath) {
                throw IllegalArgumentException("A project or directory named '$cleanName' already exists.")
            }

            if (oldDir.exists() && oldDir.absolutePath != targetDir.absolutePath) {
                if (!oldDir.renameTo(targetDir)) {
                    throw IllegalStateException("Failed to rename project directory to '$cleanName'")
                }
            }

            val updatedMeta = currentMeta.copy(name = cleanName, path = targetDir.absolutePath)
            val updatedList = _recentProjects.value.map { if (it.id == projectId) updatedMeta else it }
            _recentProjects.value = updatedList
            saveRecentProjects()

            val project = Project(updatedMeta, targetDir)
            if (_currentProject.value?.metadata?.id == projectId) {
                _currentProject.value = project
            }
            project
        }
    }

    suspend fun exportProjectZip(project: Project, targetZipFile: File): Result<File> = withContext(Dispatchers.IO) {
        SafeZipManager.zipDirectory(project.rootDirectory, targetZipFile)
    }

    fun validateProject(project: Project): Boolean {
        return project.rootDirectory.exists() && project.rootDirectory.isDirectory
    }

    suspend fun importZip(zipStream: InputStream, projectName: String): Result<Project> = withContext(Dispatchers.IO) {
        runCatching {
            val sanitizedName = projectName.trim().replace(Regex("[^a-zA-Z0-9._-]"), "_")
            val targetDir = File(workspaceBaseDir, sanitizedName)
            if (targetDir.exists()) {
                throw IllegalArgumentException("Directory '$sanitizedName' already exists.")
            }
            SafeZipManager.unzipSafely(zipStream, targetDir).getOrThrow()

            val mainFile = targetDir.walkTopDown().firstOrNull { it.isFile && !it.name.startsWith(".") }
            val lang = when (mainFile?.extension?.lowercase()) {
                "py" -> "python"
                "html", "js", "css" -> "web"
                "kt" -> "kotlin"
                "java" -> "java"
                "rs" -> "rust"
                "cpp", "c" -> "cpp"
                "go" -> "go"
                "lua" -> "lua"
                else -> "python"
            }

            val meta = ProjectMetadata(
                id = UUID.randomUUID().toString(),
                name = sanitizedName,
                path = targetDir.absolutePath,
                primaryLanguage = lang
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
