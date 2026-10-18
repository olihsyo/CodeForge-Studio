package com.codeforge.studio.workspace

import java.io.File

data class ProjectMetadata(
    val id: String,
    val name: String,
    val path: String,
    val primaryLanguage: String,
    val lastOpened: Long = System.currentTimeMillis(),
    val template: String? = null
)

data class Project(
    val metadata: ProjectMetadata,
    val rootDirectory: File
) {
    val name: String get() = metadata.name
    val path: String get() = metadata.path
}
