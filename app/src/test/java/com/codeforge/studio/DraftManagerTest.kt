package com.codeforge.studio

import com.codeforge.studio.filesystem.DraftManager
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class DraftManagerTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    @Test
    fun testSaveAndRetrieveDraft() {
        val draftsFolder = tempFolder.newFolder("drafts")
        val manager = DraftManager(draftsFolder)

        val filePath = "/workspace/project/main.kt"
        val unsavedContent = "fun main() { println(\"unsaved\") }"

        assertTrue(manager.saveDraft(filePath, unsavedContent))

        val retrieved = manager.getDraft(filePath)
        assertNotNull(retrieved)
        assertEquals(unsavedContent, retrieved)
    }

    @Test
    fun testClearDraft() {
        val draftsFolder = tempFolder.newFolder("drafts_clear")
        val manager = DraftManager(draftsFolder)

        val filePath = "/workspace/project/script.py"
        manager.saveDraft(filePath, "print('draft')")
        assertNotNull(manager.getDraft(filePath))

        manager.clearDraft(filePath)
        assertNull(manager.getDraft(filePath))
    }

    @Test
    fun testListUnsavedDrafts() {
        val draftsFolder = tempFolder.newFolder("drafts_list")
        val manager = DraftManager(draftsFolder)

        val file1 = "/workspace/project/file1.txt"
        val file2 = "/workspace/project/file2.txt"

        manager.saveDraft(file1, "content 1")
        manager.saveDraft(file2, "content 2")

        val drafts = manager.listUnsavedDrafts()
        assertEquals(2, drafts.size)
        assertTrue(drafts.any { it.originalPath == file1 })
        assertTrue(drafts.any { it.originalPath == file2 })

        manager.clearDraft(file1)
        val updatedDrafts = manager.listUnsavedDrafts()
        assertEquals(1, updatedDrafts.size)
        assertEquals(file2, updatedDrafts[0].originalPath)
    }
}
