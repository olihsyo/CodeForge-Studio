package com.codeforge.studio

import com.codeforge.studio.filesystem.SafeZipManager
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

class SafeZipManagerTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    @Test
    fun testZipAndUnzipNormalArchive() {
        val sourceDir = tempFolder.newFolder("source")
        File(sourceDir, "main.py").writeText("print('Hello CodeForge')")
        File(sourceDir, "README.md").writeText("# Test Project")

        val targetZip = File(tempFolder.root, "project.zip")
        val zipResult = SafeZipManager.zipDirectory(sourceDir, targetZip)
        assertTrue(zipResult.isSuccess)
        assertTrue(targetZip.exists())

        val destDir = tempFolder.newFolder("extracted")
        val unzipResult = targetZip.inputStream().use {
            SafeZipManager.unzipSafely(it, destDir)
        }
        assertTrue(unzipResult.isSuccess)
        assertEquals(2, unzipResult.getOrNull())
        assertTrue(File(destDir, "main.py").exists())
        assertEquals("print('Hello CodeForge')", File(destDir, "main.py").readText())
    }

    @Test
    fun testZipSlipPathTraversalRejected() {
        val destDir = tempFolder.newFolder("safe_dest")

        // Construct a malicious in-memory zip with '../escape.txt'
        val baos = ByteArrayOutputStream()
        ZipOutputStream(baos).use { zos ->
            val maliciousEntry = ZipEntry("../../escape.txt")
            zos.putNextEntry(maliciousEntry)
            zos.write("Malicious payload".toByteArray())
            zos.closeEntry()
        }

        val bais = ByteArrayInputStream(baos.toByteArray())
        val result = SafeZipManager.unzipSafely(bais, destDir)

        assertTrue("Malicious Zip Slip archive must be rejected", result.isFailure)
        assertTrue(result.exceptionOrNull() is SecurityException)
    }
}
