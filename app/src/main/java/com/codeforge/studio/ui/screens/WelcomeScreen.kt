package com.codeforge.studio.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.codeforge.studio.ui.components.CodeForgeLogo
import com.codeforge.studio.ui.theme.AccentCyan
import com.codeforge.studio.ui.theme.AccentPrimary
import com.codeforge.studio.ui.theme.BgApp
import com.codeforge.studio.ui.theme.BgCard
import com.codeforge.studio.ui.theme.BgEditor
import com.codeforge.studio.ui.theme.BgPanel
import com.codeforge.studio.ui.theme.BgPanelElevated
import com.codeforge.studio.ui.theme.BorderColor
import com.codeforge.studio.ui.theme.BorderHighlight
import com.codeforge.studio.ui.theme.StatusError
import com.codeforge.studio.ui.theme.StatusSuccess
import com.codeforge.studio.ui.theme.TextMuted
import com.codeforge.studio.ui.theme.TextPrimary
import com.codeforge.studio.ui.theme.TextSecondary
import com.codeforge.studio.workspace.ProjectMetadata
import com.codeforge.studio.workspace.WorkspaceManager
import kotlinx.coroutines.launch
import java.io.File

/**
 * Screen 1: Welcome / Workspace Setup
 * Matching the exact visual styling and layout from the design mockups.
 */
@Composable
fun WelcomeScreen(
    workspaceManager: WorkspaceManager,
    onProjectReady: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val recentProjects by workspaceManager.recentProjects.collectAsState()

    var showCreateDialog by remember { mutableStateOf(false) }
    var showRecentDialog by remember { mutableStateOf(false) }
    var projectName by remember { mutableStateOf("PythonProject") }
    var selectedTemplate by remember { mutableStateOf("python") }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(false) }

    // Zip File Picker
    val zipPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            scope.launch {
                isLoading = true
                runCatching {
                    context.contentResolver.openInputStream(uri)?.use { stream ->
                        val name = uri.lastPathSegment?.substringAfterLast('/')?.removeSuffix(".zip") ?: "Imported-Project"
                        workspaceManager.importZip(stream, name)
                    }
                }.onSuccess {
                    isLoading = false
                    onProjectReady()
                }.onFailure { e ->
                    isLoading = false
                    errorMessage = "ZIP Import failed: ${e.message}"
                }
            }
        }
    }

    // Directory Picker
    val dirPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { uri: Uri? ->
        if (uri != null) {
            scope.launch {
                isLoading = true
                runCatching {
                    val takeFlags: Int = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                    runCatching {
                        context.contentResolver.takePersistableUriPermission(uri, takeFlags)
                    }
                    val folderName = uri.lastPathSegment?.substringAfterLast(':')?.substringAfterLast('/')?.takeIf { it.isNotBlank() } ?: "Workspace"
                    workspaceManager.createProject(folderName, "empty", "empty")
                }.onSuccess {
                    isLoading = false
                    onProjectReady()
                }.onFailure { e ->
                    isLoading = false
                    errorMessage = "Directory open failed: ${e.message}"
                }
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BgApp)
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Header Section
            item {
                Spacer(modifier = Modifier.height(8.dp))
                CodeForgeLogo(size = 38.dp)
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "CodeForge Studio",
                    style = MaterialTheme.typography.headlineMedium.copy(
                        color = TextPrimary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp
                    )
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "Build · Code · Create",
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = AccentPrimary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium
                    )
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Your mobile development studio\nfor a smarter tomorrow.",
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = TextSecondary,
                        fontSize = 11.sp,
                        textAlign = TextAlign.Center,
                        lineHeight = 15.sp
                    )
                )
                Spacer(modifier = Modifier.height(8.dp))
            }

            if (isLoading) {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CircularProgressIndicator(color = AccentPrimary, modifier = Modifier.size(24.dp))
                        Spacer(modifier = Modifier.width(12.dp))
                        Text("Setting up workspace...", color = TextSecondary, fontSize = 12.sp)
                    }
                }
            }

            if (errorMessage != null) {
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = BgPanel),
                        border = androidx.compose.foundation.BorderStroke(1.dp, StatusError),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = errorMessage ?: "",
                                color = StatusError,
                                fontSize = 12.sp,
                                modifier = Modifier.weight(1f)
                            )
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = "Dismiss",
                                tint = StatusError,
                                modifier = Modifier
                                    .size(16.dp)
                                    .clickable { errorMessage = null }
                            )
                        }
                    }
                }
            }

            // 4 Primary Action Cards
            item {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // 1. Create New Workspace
                    WelcomeActionTile(
                        title = "Create New Workspace",
                        subtitle = "Start a new scratch or template project",
                        icon = Icons.Default.Add,
                        onClick = { showCreateDialog = true }
                    )

                    // 2. Open Project
                    WelcomeActionTile(
                        title = "Open Project",
                        subtitle = "Open an existing project",
                        icon = Icons.Default.FolderOpen,
                        onClick = {
                            if (recentProjects.isNotEmpty()) {
                                showRecentDialog = true
                            } else {
                                showCreateDialog = true
                            }
                        }
                    )

                    // 3. Import ZIP
                    WelcomeActionTile(
                        title = "Import ZIP",
                        subtitle = "Import project from .zip archive",
                        icon = Icons.Default.Archive,
                        onClick = { zipPicker.launch("application/zip") }
                    )

                    // 4. Select Directory
                    WelcomeActionTile(
                        title = "Select Directory",
                        subtitle = "Choose a folder from your device",
                        icon = Icons.Default.PhoneAndroid,
                        onClick = { dirPicker.launch(null) }
                    )
                }
            }

            // Optional Environments Section
            item {
                Spacer(modifier = Modifier.height(10.dp))
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.Start
                ) {
                    Text(
                        text = "Optional Environments",
                        color = TextSecondary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        item { EnvironmentBadge("Python", Color(0xFF3B82F6), Color(0xFFFBBF24)) }
                        item { EnvironmentBadge("JavaScript", Color(0xFFFBBF24), Color(0xFF000000)) }
                        item { EnvironmentBadge("TypeScript", Color(0xFF007ACC), Color(0xFFFFFFFF)) }
                        item { EnvironmentBadge("Node.js", Color(0xFF22C55E), Color(0xFFFFFFFF)) }
                        item { EnvironmentBadge("Kotlin", Color(0xFF7F52FF), Color(0xFFFFFFFF)) }
                        item { EnvironmentBadge("Rust", Color(0xFFF97316), Color(0xFFFFFFFF)) }
                    }
                }
            }

            // Bottom Wave Decorative space
            item {
                Spacer(modifier = Modifier.height(16.dp))
                BottomWaveGraphic(modifier = Modifier.fillMaxWidth().height(60.dp))
            }
        }
    }

    // New Project / Workspace Creation Dialog (Matching Screen 13)
    if (showCreateDialog) {
        NewProjectDialog(
            projectName = projectName,
            onProjectNameChange = { projectName = it },
            selectedTemplate = selectedTemplate,
            onSelectTemplate = { selectedTemplate = it },
            isLoading = isLoading,
            onDismiss = { showCreateDialog = false },
            onCreate = {
                val finalName = if (projectName.isBlank()) "PythonProject" else projectName.trim()
                scope.launch {
                    isLoading = true
                    errorMessage = null
                    workspaceManager.createProject(finalName, selectedTemplate, selectedTemplate).onSuccess {
                        isLoading = false
                        showCreateDialog = false
                        onProjectReady()
                    }.onFailure {
                        isLoading = false
                        errorMessage = it.message ?: "Failed to create project"
                    }
                }
            }
        )
    }

    // Recent Projects Sheet Dialog
    if (showRecentDialog) {
        Dialog(onDismissRequest = { showRecentDialog = false }) {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = BgPanel,
                border = androidx.compose.foundation.BorderStroke(1.dp, BorderHighlight),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Open Recent Workspace",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = AccentPrimary
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    LazyColumn(
                        modifier = Modifier.fillMaxWidth().height(260.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(recentProjects) { meta ->
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        showRecentDialog = false
                                        scope.launch {
                                            workspaceManager.openProject(meta).onSuccess {
                                                onProjectReady()
                                            }.onFailure {
                                                errorMessage = it.message
                                            }
                                        }
                                    },
                                colors = CardDefaults.cardColors(containerColor = BgCard),
                                border = androidx.compose.foundation.BorderStroke(1.dp, BorderColor)
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Folder,
                                        contentDescription = null,
                                        tint = AccentPrimary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(meta.name, color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                                        Text(meta.path, color = TextSecondary, fontSize = 10.sp, maxLines = 1)
                                    }
                                    Icon(Icons.Default.ChevronRight, contentDescription = null, tint = TextMuted, modifier = Modifier.size(16.dp))
                                }
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(onClick = { showRecentDialog = false }) {
                            Text("Cancel", color = TextSecondary)
                        }
                    }
                }
            }
        }
    }
}

/**
 * Clean Action Tile matching Screen 1 in mockups.
 */
@Composable
fun WelcomeActionTile(
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = BgCard),
        border = androidx.compose.foundation.BorderStroke(1.dp, BorderColor)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Bright blue rounded square with icon
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(AccentPrimary.copy(alpha = 0.18f))
                    .border(1.dp, AccentPrimary.copy(alpha = 0.6f), RoundedCornerShape(8.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = AccentCyan,
                    modifier = Modifier.size(18.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    color = TextPrimary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(1.dp))
                Text(
                    text = subtitle,
                    color = TextSecondary,
                    fontSize = 10.sp
                )
            }
        }
    }
}

/**
 * Environment badge for Python, JS, etc.
 */
@Composable
fun EnvironmentBadge(
    name: String,
    dotColor: Color,
    textColor: Color
) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(BgCard)
            .border(1.dp, BorderColor, RoundedCornerShape(8.dp))
            .padding(horizontal = 10.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(7.dp)
                .clip(RoundedCornerShape(3.5.dp))
                .background(dotColor)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = name,
            color = TextSecondary,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

/**
 * Bottom aesthetic wavy glowing curves matching mockup.
 */
@Composable
fun BottomWaveGraphic(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height

        val wavePath1 = Path().apply {
            moveTo(0f, h * 0.8f)
            cubicTo(w * 0.3f, h * 0.2f, w * 0.7f, h * 0.95f, w, h * 0.4f)
        }

        val wavePath2 = Path().apply {
            moveTo(0f, h * 0.95f)
            cubicTo(w * 0.35f, h * 0.4f, w * 0.65f, h * 1.1f, w, h * 0.6f)
        }

        drawPath(
            wavePath1,
            color = AccentPrimary.copy(alpha = 0.3f),
            style = Stroke(width = 2.dp.toPx())
        )

        drawPath(
            wavePath2,
            color = AccentCyan.copy(alpha = 0.2f),
            style = Stroke(width = 1.5.dp.toPx())
        )
    }
}

/**
 * Template Selection & Project Creation Dialog matching Screen 13
 */
@Composable
fun NewProjectDialog(
    projectName: String,
    onProjectNameChange: (String) -> Unit,
    selectedTemplate: String,
    onSelectTemplate: (String) -> Unit,
    isLoading: Boolean = false,
    onDismiss: () -> Unit,
    onCreate: () -> Unit
) {
    var activeTab by remember { mutableStateOf(0) } // 0: Templates, 1: Custom

    val templateList = listOf(
        Triple("empty", "Empty Project", "Basic project structure"),
        Triple("python", "Python Project", "Python 3 script / package"),
        Triple("web", "Web Project", "HTML, CSS, JavaScript"),
        Triple("java", "Java Project", "Standard Gradle"),
        Triple("kotlin", "Kotlin Project", "Kotlin JVM / Multiplatform"),
        Triple("cpp", "C/C++ Project", "CMake project"),
        Triple("rust", "Rust Project", "Cargo project")
    )

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = BgPanel,
            border = androidx.compose.foundation.BorderStroke(1.dp, BorderHighlight),
            modifier = Modifier
                .fillMaxWidth()
                .padding(4.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                // Header
                Text(
                    text = "New Project",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                Spacer(modifier = Modifier.height(10.dp))

                // Tabs: Templates | Custom
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(BgEditor)
                        .padding(3.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(6.dp))
                            .background(if (activeTab == 0) AccentPrimary else Color.Transparent)
                            .clickable { activeTab = 0 }
                            .padding(vertical = 6.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Templates",
                            color = if (activeTab == 0) Color.White else TextSecondary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(6.dp))
                            .background(if (activeTab == 1) AccentPrimary else Color.Transparent)
                            .clickable { activeTab = 1 }
                            .padding(vertical = 6.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Custom",
                            color = if (activeTab == 1) Color.White else TextSecondary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = projectName,
                    onValueChange = onProjectNameChange,
                    label = { Text("Project Name", fontSize = 12.sp) },
                    placeholder = { Text("MyApp", color = TextMuted) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                        focusedBorderColor = AccentPrimary,
                        unfocusedBorderColor = BorderColor,
                        focusedContainerColor = BgCard,
                        unfocusedContainerColor = BgCard
                    )
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Template cards list
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(220.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(templateList) { (type, name, desc) ->
                        val isSelected = selectedTemplate == type
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { onSelectTemplate(type) },
                            colors = CardDefaults.cardColors(
                                containerColor = if (isSelected) BgPanelElevated else BgCard
                            ),
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                if (isSelected) AccentPrimary else BorderColor
                            )
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(32.dp)
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(if (isSelected) AccentPrimary.copy(alpha = 0.2f) else BorderColor.copy(alpha = 0.4f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Folder,
                                        contentDescription = null,
                                        tint = if (isSelected) AccentPrimary else TextSecondary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = name,
                                        color = if (isSelected) AccentCyan else TextPrimary,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Text(
                                        text = desc,
                                        color = TextSecondary,
                                        fontSize = 10.sp
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Action buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = BgEditor)
                    ) {
                        Text("Cancel", color = TextSecondary, fontSize = 12.sp)
                    }

                    Button(
                        onClick = onCreate,
                        enabled = !isLoading,
                        modifier = Modifier.weight(1.5f),
                        colors = ButtonDefaults.buttonColors(containerColor = AccentPrimary)
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(color = Color.White, modifier = Modifier.size(14.dp), strokeWidth = 2.dp)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Creating...", color = Color.White, fontSize = 11.sp)
                        } else {
                            Text("Create Project", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                    }
                }
            }
        }
    }
}
