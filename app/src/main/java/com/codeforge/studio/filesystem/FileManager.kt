package com.codeforge.studio.filesystem

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

class FileManager {

    suspend fun loadFileTree(rootDirectory: File, showHidden: Boolean = false): FileNode = withContext(Dispatchers.IO) {
        buildNode(rootDirectory, showHidden)
    }

    private fun buildNode(file: File, showHidden: Boolean): FileNode {
        val children = if (file.isDirectory) {
            val list = file.listFiles() ?: emptyArray()
            list.filter { showHidden || !it.name.startsWith(".") }
                .sortedWith(compareBy({ !it.isDirectory }, { it.name.lowercase() }))
                .map { buildNode(it, showHidden) }
        } else {
            emptyList()
        }
        return FileNode(file = file, children = children)
    }

    suspend fun readFile(file: File): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            file.readText(Charsets.UTF_8)
        }
    }

    suspend fun writeFile(file: File, content: String): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            file.parentFile?.mkdirs()
            file.writeText(content, Charsets.UTF_8)
        }
    }

    suspend fun createFile(parentDir: File, name: String): Result<File> = withContext(Dispatchers.IO) {
        runCatching {
            val cleanName = name.trim()
            if (cleanName.isBlank() || cleanName.contains("/") || cleanName.contains("\\") || cleanName.contains("..")) {
                throw IllegalArgumentException("Invalid file name: $name")
            }
            val newFile = File(parentDir, cleanName)
            if (newFile.exists()) {
                throw IllegalArgumentException("File already exists: $cleanName")
            }
            newFile.parentFile?.mkdirs()
            if (!newFile.createNewFile()) {
                throw IllegalStateException("Failed to create file: $cleanName")
            }
            newFile
        }
    }

    suspend fun createDirectory(parentDir: File, name: String): Result<File> = withContext(Dispatchers.IO) {
        runCatching {
            val cleanName = name.trim()
            if (cleanName.isBlank() || cleanName.contains("/") || cleanName.contains("\\") || cleanName.contains("..")) {
                throw IllegalArgumentException("Invalid directory name: $name")
            }
            val newDir = File(parentDir, cleanName)
            if (newDir.exists()) {
                throw IllegalArgumentException("Directory already exists: $cleanName")
            }
            if (!newDir.mkdirs() && !newDir.exists()) {
                throw IllegalStateException("Failed to create directory: $cleanName")
            }
            newDir
        }
    }

    suspend fun rename(file: File, newName: String): Result<File> = withContext(Dispatchers.IO) {
        runCatching {
            val cleanName = newName.trim()
            if (cleanName.isBlank() || cleanName.contains("/") || cleanName.contains("\\") || cleanName.contains("..")) {
                throw IllegalArgumentException("Invalid new name: $newName")
            }
            val target = File(file.parentFile, cleanName)
            if (target.exists()) {
                throw IllegalArgumentException("Target name already exists: $cleanName")
            }
            if (!file.renameTo(target)) {
                throw IllegalStateException("Failed to rename file")
            }
            target
        }
    }

    suspend fun move(source: File, targetDir: File): Result<File> = withContext(Dispatchers.IO) {
        runCatching {
            if (!source.exists()) {
                throw IllegalArgumentException("Source does not exist: ${source.absolutePath}")
            }
            if (!targetDir.exists() || !targetDir.isDirectory) {
                throw IllegalArgumentException("Target directory does not exist: ${targetDir.absolutePath}")
            }
            if (source.isDirectory && targetDir.canonicalPath.startsWith(source.canonicalPath + File.separator)) {
                throw IllegalArgumentException("Cannot move a directory inside itself")
            }
            val targetFile = File(targetDir, source.name)
            if (targetFile.exists()) {
                throw IllegalArgumentException("Item with name '${source.name}' already exists in target directory")
            }
            if (!source.renameTo(targetFile)) {
                // Fallback copy and delete
                if (source.isDirectory) {
                    source.copyRecursively(targetFile, overwrite = false)
                    source.deleteRecursively()
                } else {
                    source.copyTo(targetFile, overwrite = false)
                    source.delete()
                }
            }
            targetFile
        }
    }

    suspend fun delete(file: File): Result<Boolean> = withContext(Dispatchers.IO) {
        runCatching {
            if (file.isDirectory) {
                file.deleteRecursively()
            } else {
                file.delete()
            }
        }
    }

    suspend fun searchFiles(rootDirectory: File, query: String, searchContent: Boolean = false): List<FileNode> = withContext(Dispatchers.IO) {
        val results = mutableListOf<FileNode>()
        if (query.isBlank() || !rootDirectory.exists()) return@withContext results

        rootDirectory.walkTopDown()
            .filter { !it.name.startsWith(".") && it.isFile }
            .forEach { file ->
                val nameMatch = file.name.contains(query, ignoreCase = true)
                val contentMatch = if (searchContent && !nameMatch && file.length() < 1_000_000L) {
                    runCatching { file.readText().contains(query, ignoreCase = true) }.getOrDefault(false)
                } else false

                if (nameMatch || contentMatch) {
                    results.add(FileNode(file))
                }
            }
        results
    }
}
