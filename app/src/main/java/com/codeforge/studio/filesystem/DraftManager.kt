package com.codeforge.studio.filesystem

import android.content.Context
import java.io.File
import java.security.MessageDigest

class DraftManager(private val context: Context) {

    private val draftsDir: File
        get() = File(context.filesDir, "drafts").apply { if (!exists()) mkdirs() }

    private fun hashPath(filePath: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val hash = digest.digest(filePath.toByteArray(Charsets.UTF_8))
        return hash.joinToString("") { "%02x".format(it) }
    }

    fun saveDraft(filePath: String, content: String): Boolean {
        return runCatching {
            val draftFile = File(draftsDir, "${hashPath(filePath)}.draft")
            val metaFile = File(draftsDir, "${hashPath(filePath)}.meta")
            draftFile.writeText(content, Charsets.UTF_8)
            metaFile.writeText("$filePath\n${System.currentTimeMillis()}", Charsets.UTF_8)
            true
        }.getOrDefault(false)
    }

    fun getDraft(filePath: String): String? {
        val draftFile = File(draftsDir, "${hashPath(filePath)}.draft")
        return if (draftFile.exists()) {
            runCatching { draftFile.readText(Charsets.UTF_8) }.getOrNull()
        } else {
            null
        }
    }

    fun clearDraft(filePath: String) {
        val hash = hashPath(filePath)
        File(draftsDir, "$hash.draft").delete()
        File(draftsDir, "$hash.meta").delete()
    }

    fun listUnsavedDrafts(): List<DraftInfo> {
        val list = mutableListOf<DraftInfo>()
        draftsDir.listFiles { _, name -> name.endsWith(".meta") }?.forEach { metaFile ->
            val hash = metaFile.name.removeSuffix(".meta")
            val draftFile = File(draftsDir, "$hash.draft")
            if (draftFile.exists()) {
                val lines = metaFile.readLines()
                if (lines.isNotEmpty()) {
                    val originalPath = lines[0]
                    val timestamp = lines.getOrNull(1)?.toLongOrNull() ?: metaFile.lastModified()
                    list.add(DraftInfo(originalPath, draftFile, timestamp))
                }
            }
        }
        return list
    }

    data class DraftInfo(
        val originalPath: String,
        val draftFile: File,
        val timestamp: Long
    )
}
