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
            val newFile = File(parentDir, name)
            if (newFile.exists()) {
                throw IllegalArgumentException("File already exists: $name")
            }
            newFile.parentFile?.mkdirs()
            newFile.createNewFile()
            newFile
        }
    }

    suspend fun createDirectory(parentDir: File, name: String): Result<File> = withContext(Dispatchers.IO) {
        runCatching {
            val newDir = File(parentDir, name)
            if (newDir.exists()) {
                throw IllegalArgumentException("Directory already exists: $name")
            }
            newDir.mkdirs()
            newDir
        }
    }

    suspend fun rename(file: File, newName: String): Result<File> = withContext(Dispatchers.IO) {
        runCatching {
            val target = File(file.parentFile, newName)
            if (target.exists()) {
                throw IllegalArgumentException("Target name already exists: $newName")
            }
            if (!file.renameTo(target)) {
                throw IllegalStateException("Failed to rename file")
            }
            target
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
