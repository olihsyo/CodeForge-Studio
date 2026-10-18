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

    private val isGitRepo: Boolean
        get() = File(projectDir, ".git").exists()

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

    suspend fun initRepository(): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val gitDir = File(projectDir, ".git")
            if (!gitDir.exists()) {
                gitDir.mkdirs()
                File(gitDir, "HEAD").writeText("ref: refs/heads/main\n")
                File(gitDir, "config").writeText("[core]\n\trepositoryformatversion = 0\n\tfilemode = true\n\tbare = false\n")
                File(gitDir, "objects").mkdirs()
                File(gitDir, "refs/heads").mkdirs()
            }
        }
    }

    suspend fun getStatus(): List<GitFileStatus> = withContext(Dispatchers.IO) {
        val list = mutableListOf<GitFileStatus>()
        val gitDir = File(projectDir, ".git")
        if (!gitDir.exists()) return@withContext list

        val snapshotsDir = File(projectDir, ".codeforge/snapshots")
        projectDir.walkTopDown()
            .filter { !it.name.startsWith(".") && it.isFile }
            .forEach { file ->
                val rel = file.relativeTo(projectDir).path.replace("\\", "/")
                val snapshotFile = File(snapshotsDir, rel)
                if (!snapshotFile.exists()) {
                    list.add(GitFileStatus(file, rel, isStaged = false, status = "?"))
                } else if (snapshotFile.readText() != file.readText()) {
                    list.add(GitFileStatus(file, rel, isStaged = false, status = "M"))
                }
            }
        list
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
        val maxLines = maxOf(oldLines.size, newLines.size)
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

            // Update snapshot of current working files
            projectDir.walkTopDown()
                .filter { !it.name.startsWith(".") && it.isFile }
                .forEach { file ->
                    val rel = file.relativeTo(projectDir).path
                    val target = File(snapshotsDir, rel)
                    target.parentFile?.mkdirs()
                    file.copyTo(target, overwrite = true)
                }

            val hash = java.util.UUID.randomUUID().toString().substring(0, 7)
            val logFile = File(projectDir, ".codeforge/commits.log")
            logFile.parentFile?.mkdirs()

            val timestamp = System.currentTimeMillis()
            val dateStr = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm", java.util.Locale.US).format(java.util.Date(timestamp))
            val line = "$hash|$author|$timestamp|$dateStr|$message\n"
            logFile.appendText(line)

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
