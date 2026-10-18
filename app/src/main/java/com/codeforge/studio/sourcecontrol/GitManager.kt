package com.codeforge.studio.sourcecontrol

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

data class GitFileStatus(
    val file: File,
    val relativePath: String,
    val isStaged: Boolean,
    val status: String // "M" (Modified), "A" (Added), "D" (Deleted), "?" (Untracked)
)

data class GitCommit(
    val hash: String,
    val author: String,
    val message: String,
    val timestamp: Long,
    val dateString: String
)

data class GitDiff(
    val filePath: String,
    val oldContent: String,
    val newContent: String,
    val diffLines: List<DiffLine>
)

data class DiffLine(
    val type: DiffType,
    val text: String,
    val oldLineNumber: Int?,
    val newLineNumber: Int?
)

enum class DiffType {
    UNCHANGED,
    ADDED,
    DELETED,
    HEADER
}

class GitManager(private val projectDir: File) {

    val isGitRepo: Boolean
        get() = File(projectDir, ".git").exists()

    private val stagedFile: File
        get() = File(projectDir, ".codeforge/staged.txt")

    suspend fun getBranchName(): String = withContext(Dispatchers.IO) {
        val headFile = File(projectDir, ".git/HEAD")
        if (headFile.exists()) {
            val content = headFile.readText().trim()
            if (content.startsWith("ref: refs/heads/")) {
                return@withContext content.removePrefix("ref: refs/heads/")
            }
        }
        "main"
    }

    suspend fun getBranches(): List<String> = withContext(Dispatchers.IO) {
        val headsDir = File(projectDir, ".git/refs/heads")
        if (headsDir.exists() && headsDir.isDirectory) {
            val list = headsDir.walkTopDown()
                .filter { it.isFile }
                .map { it.relativeTo(headsDir).path.replace("\\", "/") }
                .toList()
            if (list.isNotEmpty()) return@withContext list
        }
        listOf("main")
    }

    suspend fun createBranch(branchName: String): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val sanitized = branchName.trim().replace(Regex("[^a-zA-Z0-9._/-]"), "-")
            if (sanitized.isBlank()) throw IllegalArgumentException("Branch name cannot be empty")
            initRepository()
            val headsDir = File(projectDir, ".git/refs/heads").apply { mkdirs() }
            val newBranchFile = File(headsDir, sanitized)
            newBranchFile.parentFile?.mkdirs()
            val currentCommit = getCommitHistory().firstOrNull()?.hash ?: "0000000"
            newBranchFile.writeText(currentCommit + "\n")
        }
    }

    suspend fun switchBranch(branchName: String): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val sanitized = branchName.trim()
            initRepository()
            val headFile = File(projectDir, ".git/HEAD")
            headFile.writeText("ref: refs/heads/$sanitized\n")
            val branchFile = File(projectDir, ".git/refs/heads/$sanitized")
            if (!branchFile.exists()) {
                branchFile.parentFile?.mkdirs()
                val currentCommit = getCommitHistory().firstOrNull()?.hash ?: "0000000"
                branchFile.writeText(currentCommit + "\n")
            }
        }
    }

    suspend fun initRepository(): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val gitDir = File(projectDir, ".git")
            if (!gitDir.exists()) {
                gitDir.mkdirs()
                File(gitDir, "HEAD").writeText("ref: refs/heads/main\n")
                File(gitDir, "config").writeText("[core]\n\trepositoryformatversion = 0\n\tfilemode = true\n\tbare = false\n")
                File(gitDir, "objects").mkdirs()
                val headsDir = File(gitDir, "refs/heads").apply { mkdirs() }
                File(headsDir, "main").writeText("0000000\n")
            }
        }
    }

    private fun loadStagedPaths(): MutableSet<String> {
        if (!stagedFile.exists()) return mutableSetOf()
        return stagedFile.readLines().map { it.trim() }.filter { it.isNotEmpty() }.toMutableSet()
    }

    private fun saveStagedPaths(paths: Set<String>) {
        stagedFile.parentFile?.mkdirs()
        stagedFile.writeText(paths.joinToString("\n"))
    }

    suspend fun stageFile(file: File): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val rel = file.relativeTo(projectDir).path.replace("\\", "/")
            val staged = loadStagedPaths()
            staged.add(rel)
            saveStagedPaths(staged)
        }
    }

    suspend fun unstageFile(file: File): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val rel = file.relativeTo(projectDir).path.replace("\\", "/")
            val staged = loadStagedPaths()
            staged.remove(rel)
            saveStagedPaths(staged)
        }
    }

    suspend fun stageAll(): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val statusList = getStatus()
            val staged = loadStagedPaths()
            statusList.forEach {
                staged.add(it.relativePath)
            }
            saveStagedPaths(staged)
        }
    }

    suspend fun unstageAll(): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            if (stagedFile.exists()) {
                stagedFile.delete()
            }
        }
    }

    suspend fun discardChanges(file: File): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val rel = file.relativeTo(projectDir).path.replace("\\", "/")
            val snapshotFile = File(projectDir, ".codeforge/snapshots/$rel")
            if (snapshotFile.exists()) {
                file.parentFile?.mkdirs()
                snapshotFile.copyTo(file, overwrite = true)
            } else {
                if (file.exists()) {
                    file.delete()
                }
            }
            val staged = loadStagedPaths()
            staged.remove(rel)
            saveStagedPaths(staged)
        }
    }

    suspend fun getStatus(): List<GitFileStatus> = withContext(Dispatchers.IO) {
        val list = mutableListOf<GitFileStatus>()
        val gitDir = File(projectDir, ".git")
        if (!gitDir.exists()) return@withContext list

        val snapshotsDir = File(projectDir, ".codeforge/snapshots")
        val stagedPaths = loadStagedPaths()

        // Check working tree files
        projectDir.walkTopDown()
            .filter { file ->
                val rel = file.relativeTo(projectDir).path.replace("\\", "/")
                !rel.startsWith(".git") && !rel.startsWith(".codeforge") && !file.name.startsWith(".") && file.isFile
            }
            .forEach { file ->
                val rel = file.relativeTo(projectDir).path.replace("\\", "/")
                val snapshotFile = File(snapshotsDir, rel)
                val isStaged = stagedPaths.contains(rel)

                if (!snapshotFile.exists()) {
                    val status = if (isStaged) "A" else "?"
                    list.add(GitFileStatus(file, rel, isStaged = isStaged, status = status))
                } else if (snapshotFile.readText() != file.readText()) {
                    list.add(GitFileStatus(file, rel, isStaged = isStaged, status = "M"))
                }
            }

        // Check deleted files that were in snapshots
        if (snapshotsDir.exists()) {
            snapshotsDir.walkTopDown()
                .filter { !it.name.startsWith(".") && it.isFile }
                .forEach { snapFile ->
                    val rel = snapFile.relativeTo(snapshotsDir).path.replace("\\", "/")
                    val currentFile = File(projectDir, rel)
                    if (!currentFile.exists()) {
                        val isStaged = stagedPaths.contains(rel)
                        list.add(GitFileStatus(currentFile, rel, isStaged = isStaged, status = "D"))
                    }
                }
        }

        list.distinctBy { it.relativePath }
    }

    suspend fun computeDiff(file: File): GitDiff = withContext(Dispatchers.IO) {
        val rel = file.relativeTo(projectDir).path.replace("\\", "/")
        val snapshotFile = File(projectDir, ".codeforge/snapshots/$rel")
        val oldContent = if (snapshotFile.exists()) snapshotFile.readText() else ""
        val newContent = if (file.exists()) file.readText() else ""

        val oldLines = oldContent.lines()
        val newLines = newContent.lines()
        val diffList = mutableListOf<DiffLine>()

        diffList.add(DiffLine(DiffType.HEADER, "--- a/$rel", null, null))
        diffList.add(DiffLine(DiffType.HEADER, "+++ b/$rel", null, null))

        // Simple line diff calculation
        var oldIdx = 0
        var newIdx = 0

        while (oldIdx < oldLines.size || newIdx < newLines.size) {
            val oldLine = oldLines.getOrNull(oldIdx)
            val newLine = newLines.getOrNull(newIdx)

            if (oldLine == newLine && oldLine != null) {
                diffList.add(DiffLine(DiffType.UNCHANGED, " $oldLine", oldIdx + 1, newIdx + 1))
                oldIdx++
                newIdx++
            } else {
                if (oldLine != null && (newLine == null || !newLines.contains(oldLine))) {
                    diffList.add(DiffLine(DiffType.DELETED, "-$oldLine", oldIdx + 1, null))
                    oldIdx++
                }
                if (newLine != null && (oldLine == null || !oldLines.contains(newLine))) {
                    diffList.add(DiffLine(DiffType.ADDED, "+$newLine", null, newIdx + 1))
                    newIdx++
                }
            }
        }

        GitDiff(rel, oldContent, newContent, diffList)
    }

    suspend fun commit(message: String, author: String = "Mobile Developer"): Result<GitCommit> = withContext(Dispatchers.IO) {
        runCatching {
            if (message.isBlank()) throw IllegalArgumentException("Commit message cannot be empty")

            initRepository()

            val snapshotsDir = File(projectDir, ".codeforge/snapshots")
            snapshotsDir.mkdirs()

            val stagedPaths = loadStagedPaths()
            val filesToSnapshot = if (stagedPaths.isNotEmpty()) {
                stagedPaths.map { File(projectDir, it) }
            } else {
                projectDir.walkTopDown().filter { !it.name.startsWith(".") && it.isFile }.toList()
            }

            // Update snapshot of committed files
            filesToSnapshot.forEach { file ->
                val rel = file.relativeTo(projectDir).path.replace("\\", "/")
                val target = File(snapshotsDir, rel)
                if (file.exists()) {
                    target.parentFile?.mkdirs()
                    file.copyTo(target, overwrite = true)
                } else {
                    if (target.exists()) target.delete()
                }
            }

            // Clear staged paths on commit
            unstageAll()

            val hash = java.util.UUID.randomUUID().toString().substring(0, 7)
            val logFile = File(projectDir, ".codeforge/commits.log")
            logFile.parentFile?.mkdirs()

            val timestamp = System.currentTimeMillis()
            val dateStr = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm", java.util.Locale.US).format(java.util.Date(timestamp))
            val line = "$hash|$author|$timestamp|$dateStr|$message\n"
            logFile.appendText(line)

            // Update current branch ref file
            val branch = getBranchName()
            val branchRef = File(projectDir, ".git/refs/heads/$branch")
            branchRef.parentFile?.mkdirs()
            branchRef.writeText(hash + "\n")

            GitCommit(hash, author, message, timestamp, dateStr)
        }
    }

    suspend fun getCommitHistory(): List<GitCommit> = withContext(Dispatchers.IO) {
        val logFile = File(projectDir, ".codeforge/commits.log")
        if (!logFile.exists()) return@withContext emptyList()

        logFile.readLines().reversed().mapNotNull { line ->
            val parts = line.split("|")
            if (parts.size >= 5) {
                GitCommit(
                    hash = parts[0],
                    author = parts[1],
                    timestamp = parts[2].toLongOrNull() ?: 0L,
                    dateString = parts[3],
                    message = parts.subList(4, parts.size).joinToString("|")
                )
            } else null
        }
    }
}
