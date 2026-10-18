package com.codeforge.studio

import com.codeforge.studio.filesystem.FileManager
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class FileManagerTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    private val fileManager = FileManager()

    @Test
    fun testCreateFileAndWriteRead() = runBlocking {
        val root = tempFolder.newFolder("fs_test")
        val createRes = fileManager.createFile(root, "test.kt")
        assertTrue(createRes.isSuccess)
        val file = createRes.getOrThrow()
        assertTrue(file.exists())

        val writeRes = fileManager.writeFile(file, "fun main() = println(\"Hi\")")
        assertTrue(writeRes.isSuccess)

        val readRes = fileManager.readFile(file)
        assertTrue(readRes.isSuccess)
        assertEquals("fun main() = println(\"Hi\")", readRes.getOrThrow())
    }

    @Test
    fun testPreventPathTraversalInCreate() = runBlocking {
        val root = tempFolder.newFolder("fs_traversal")
        val res1 = fileManager.createFile(root, "../evil.txt")
        assertTrue(res1.isFailure)

        val res2 = fileManager.createFile(root, "sub/dir/test.txt")
        assertTrue(res2.isFailure)

        val res3 = fileManager.createDirectory(root, "../evil_dir")
        assertTrue(res3.isFailure)
    }

    @Test
    fun testRename() = runBlocking {
        val root = tempFolder.newFolder("fs_rename")
        val f = fileManager.createFile(root, "old.txt").getOrThrow()
        f.writeText("content")

        val renamedRes = fileManager.rename(f, "new.txt")
        assertTrue(renamedRes.isSuccess)
        val renamed = renamedRes.getOrThrow()
        assertEquals("new.txt", renamed.name)
        assertTrue(renamed.exists())
        assertFalse(f.exists())
        assertEquals("content", renamed.readText())

        // Test rename traversal rejection
        val badRename = fileManager.rename(renamed, "../bad.txt")
        assertTrue(badRename.isFailure)
    }

    @Test
    fun testMove() = runBlocking {
        val root = tempFolder.newFolder("fs_move")
        val subDir = fileManager.createDirectory(root, "subdir").getOrThrow()
        val file = fileManager.createFile(root, "moveme.txt").getOrThrow()
        file.writeText("to be moved")

        val moveRes = fileManager.move(file, subDir)
        assertTrue(moveRes.isSuccess)
        val moved = moveRes.getOrThrow()
        assertTrue(moved.exists())
        assertEquals(subDir.absolutePath, moved.parentFile?.absolutePath)
        assertFalse(file.exists())
        assertEquals("to be moved", moved.readText())
    }

    @Test
    fun testDelete() = runBlocking {
        val root = tempFolder.newFolder("fs_delete")
        val file = fileManager.createFile(root, "delete_me.txt").getOrThrow()
        assertTrue(file.exists())

        val delRes = fileManager.delete(file)
        assertTrue(delRes.isSuccess)
        assertFalse(file.exists())
    }

    @Test
    fun testLoadFileTree() = runBlocking {
        val root = tempFolder.newFolder("fs_tree")
        fileManager.createFile(root, "a.txt")
        fileManager.createFile(root, ".hidden.txt")
        val sub = fileManager.createDirectory(root, "sub").getOrThrow()
        fileManager.createFile(sub, "b.txt")

        val treeWithoutHidden = fileManager.loadFileTree(root, showHidden = false)
        assertFalse(treeWithoutHidden.children.any { it.file.name == ".hidden.txt" })
        assertTrue(treeWithoutHidden.children.any { it.file.name == "a.txt" })
        assertTrue(treeWithoutHidden.children.any { it.file.name == "sub" })

        val treeWithHidden = fileManager.loadFileTree(root, showHidden = true)
        assertTrue(treeWithHidden.children.any { it.file.name == ".hidden.txt" })
    }
}
