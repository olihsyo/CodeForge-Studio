package com.codeforge.studio

import com.codeforge.studio.sourcecontrol.DiffType
import com.codeforge.studio.sourcecontrol.GitManager
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
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
}
