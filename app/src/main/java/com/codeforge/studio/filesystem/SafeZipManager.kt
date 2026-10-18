package com.codeforge.studio.filesystem

import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.InputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

object SafeZipManager {

    private const val MAX_ENTRIES = 10_000
    private const val MAX_TOTAL_SIZE = 250 * 1024 * 1024L // 250MB limit

    /**
     * Extracts a zip archive safely into [destDir].
     * Protects against Zip Slip path traversal and Zip Bomb attacks.
     */
    fun unzipSafely(zipInputStream: InputStream, destDir: File): Result<Int> {
        return runCatching {
            if (!destDir.exists()) {
                destDir.mkdirs()
            }
            val canonicalDestDirPath = destDir.canonicalPath
            var entriesCount = 0
            var totalBytes = 0L

            ZipInputStream(zipInputStream).use { zis ->
                var entry: ZipEntry? = zis.nextEntry
                while (entry != null) {
                    entriesCount++
                    if (entriesCount > MAX_ENTRIES) {
                        throw SecurityException("Zip archive contains too many entries (exceeds $MAX_ENTRIES)")
                    }

                    // Strict path traversal validation
                    val entryName = entry.name
                    if (entryName.contains("..") || entryName.startsWith("/") || entryName.startsWith("\\")) {
                        throw SecurityException("Malicious path detected in zip entry: $entryName")
                    }

                    val destFile = File(destDir, entryName)
                    val canonicalDestFile = destFile.canonicalPath
                    if (!canonicalDestFile.startsWith(canonicalDestDirPath + File.separator) &&
                        canonicalDestFile != canonicalDestDirPath
                    ) {
                        throw SecurityException("Path traversal attempt in zip entry: $entryName")
                    }

                    if (entry.isDirectory) {
                        destFile.mkdirs()
                    } else {
                        destFile.parentFile?.mkdirs()
                        FileOutputStream(destFile).use { fos ->
                            val buffer = ByteArray(8192)
                            var read: Int
                            while (zis.read(buffer).also { read = it } != -1) {
                                totalBytes += read
                                if (totalBytes > MAX_TOTAL_SIZE) {
                                    throw SecurityException("Extracted zip data exceeds limit of 250MB")
                                }
                                fos.write(buffer, 0, read)
                            }
                        }
                    }
                    zis.closeEntry()
                    entry = zis.nextEntry
                }
            }
            entriesCount
        }
    }

    /**
     * Compresses [sourceDir] into [targetZipFile].
     */
    fun createZipFromDirectory(sourceDir: File, targetZipFile: File): Result<File> = zipDirectory(sourceDir, targetZipFile)

    fun zipDirectory(sourceDir: File, targetZipFile: File): Result<File> {
        return runCatching {
            targetZipFile.parentFile?.mkdirs()
            ZipOutputStream(FileOutputStream(targetZipFile)).use { zos ->
                val basePath = sourceDir.canonicalPath
                sourceDir.walkTopDown().forEach { file ->
                    val relativePath = file.canonicalPath.removePrefix(basePath)
                        .removePrefix(File.separator)
                        .replace("\\", "/")
                    if (relativePath.isNotEmpty() && !relativePath.startsWith(".git/") && !relativePath.startsWith(".codeforge/")) {
                        if (file.isDirectory) {
                            val dirEntry = ZipEntry("$relativePath/")
                            zos.putNextEntry(dirEntry)
                            zos.closeEntry()
                        } else {
                            val fileEntry = ZipEntry(relativePath)
                            zos.putNextEntry(fileEntry)
                            FileInputStream(file).use { fis ->
                                fis.copyTo(zos)
                            }
                            zos.closeEntry()
                        }
                    }
                }
            }
            targetZipFile
        }
    }
}
