package com.codeforge.studio

import com.codeforge.studio.sourcecontrol.DiffType
import com.codeforge.studio.sourcecontrol.GitManager
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class GitManagerTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    @Test
    fun testInitCommitAndHistory() = runBlocking {
        val projectDir = tempFolder.newFolder("test_git_project")
        val mainFile = File(projectDir, "main.py")
        mainFile.writeText("print('Initial commit')")

        val git = GitManager(projectDir)
        git.initRepository()

        val commitResult = git.commit("Initial commit", "Test Engineer")
        assertTrue(commitResult.isSuccess)

        val commit = commitResult.getOrNull()
        assertNotNull(commit)
        assertEquals("Initial commit", commit?.message)

        val history = git.getCommitHistory()
        assertEquals(1, history.size)
        assertEquals("Initial commit", history[0].message)

        // Modify file and test diff
        mainFile.writeText("print('Initial commit')\nprint('Line 2')")
        val diff = git.computeDiff(mainFile)
        assertTrue(diff.diffLines.any { it.type == DiffType.ADDED && it.text.contains("Line 2") })
    }

    @Test
    fun testStagingAndUnstaging() = runBlocking {
        val projectDir = tempFolder.newFolder("test_git_staging")
        val f1 = File(projectDir, "file1.txt").apply { writeText("hello") }
        val f2 = File(projectDir, "file2.txt").apply { writeText("world") }

        val git = GitManager(projectDir)
        git.initRepository()

        // Initially untracked
        var status = git.getStatus()
        assertEquals(2, status.size)
        assertFalse(status[0].isStaged)
        assertFalse(status[1].isStaged)

        // Stage file1
        git.stageFile(f1)
        status = git.getStatus()
        val s1 = status.first { it.relativePath == "file1.txt" }
        val s2 = status.first { it.relativePath == "file2.txt" }
        assertTrue(s1.isStaged)
        assertFalse(s2.isStaged)

        // Stage all
        git.stageAll()
        status = git.getStatus()
        assertTrue(status.all { it.isStaged })

        // Unstage file1
        git.unstageFile(f1)
        status = git.getStatus()
        assertFalse(status.first { it.relativePath == "file1.txt" }.isStaged)
        assertTrue(status.first { it.relativePath == "file2.txt" }.isStaged)

        // Unstage all
        git.unstageAll()
        status = git.getStatus()
        assertTrue(status.none { it.isStaged })
    }

    @Test
    fun testDiscardChanges() = runBlocking {
        val projectDir = tempFolder.newFolder("test_git_discard")
        val f1 = File(projectDir, "app.py").apply { writeText("original content") }

        val git = GitManager(projectDir)
        git.initRepository()
        git.commit("Initial", "Engineer")

        // Modify app.py
        f1.writeText("modified content")
        assertEquals("modified content", f1.readText())

        // Discard
        git.discardChanges(f1)
        assertEquals("original content", f1.readText())
    }

    @Test
    fun testBranchManagement() = runBlocking {
        val projectDir = tempFolder.newFolder("test_git_branches")
        val f1 = File(projectDir, "README.md").apply { writeText("# Welcome") }

        val git = GitManager(projectDir)
        git.initRepository()
        git.commit("Initial commit", "Dev")

        assertEquals("main", git.getBranchName())

        // Create new branch
        val createRes = git.createBranch("feature/dark-mode")
        assertTrue(createRes.isSuccess)

        val branches = git.getBranches()
        assertTrue(branches.contains("main"))
        assertTrue(branches.contains("feature-dark-mode") || branches.contains("feature/dark-mode"))

        // Switch to branch
        val switchRes = git.switchBranch("feature/dark-mode")
        assertTrue(switchRes.isSuccess)
        assertEquals("feature/dark-mode", git.getBranchName())
    }
}
