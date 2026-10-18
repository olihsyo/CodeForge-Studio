package com.codeforge.studio.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.codeforge.studio.navigation.Screen
import com.codeforge.studio.runtimes.LanguageRegistry
import com.codeforge.studio.ui.components.CodeForgeLogo
import com.codeforge.studio.ui.theme.AccentCyan
import com.codeforge.studio.ui.theme.AccentPrimary
import com.codeforge.studio.ui.theme.BgApp
import com.codeforge.studio.ui.theme.BgCard
import com.codeforge.studio.ui.theme.BgEditor
import com.codeforge.studio.ui.theme.BgPanel
import com.codeforge.studio.ui.theme.BorderColor
import com.codeforge.studio.ui.theme.BorderHighlight
import com.codeforge.studio.ui.theme.StatusError
import com.codeforge.studio.ui.theme.StatusSuccess
import com.codeforge.studio.ui.theme.StatusWarning
import com.codeforge.studio.ui.theme.TextMuted
import com.codeforge.studio.ui.theme.TextPrimary
import com.codeforge.studio.ui.theme.TextSecondary
import com.codeforge.studio.workspace.ProjectMetadata
import com.codeforge.studio.workspace.WorkspaceManager
import kotlinx.coroutines.launch

/**
 * Screen 2: Home Dashboard
 * Matches the exact layout, Quick Action Grid, Recent Projects with "See all",
 * Supported Languages row, and top search bar from the design mockup.
 */
@Composable
fun HomeScreen(
    workspaceManager: WorkspaceManager,
    onNavigate: (Screen) -> Unit,
    problemsCount: Int,
    onOpenCommandPalette: () -> Unit = {}
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val currentProject by workspaceManager.currentProject.collectAsState()
    val recentProjects by workspaceManager.recentProjects.collectAsState()

    var showCreateDialog by remember { mutableStateOf(false) }
    var projectName by remember { mutableStateOf("") }
    var selectedTemplate by remember { mutableStateOf("python") }
    var showAllProjects by remember { mutableStateOf(false) }

    // Zip File Picker
    val zipPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            scope.launch {
                runCatching {
                    context.contentResolver.openInputStream(uri)?.use { stream ->
                        val name = uri.lastPathSegment?.substringAfterLast('/')?.removeSuffix(".zip") ?: "Imported-Project"
                        workspaceManager.importZip(stream, name)
                    }
                }.onSuccess {
                    onNavigate(Screen.Editor)
                }
            }
        }
    }

    val displayProjects = remember(recentProjects, showAllProjects) {
        if (showAllProjects) recentProjects else recentProjects.take(4)
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(BgApp)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Top Header
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 2.dp, bottom = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CodeForgeLogo(size = 24.dp)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "CodeForge Studio",
                        color = TextPrimary,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onOpenCommandPalette, modifier = Modifier.size(36.dp)) {
                        Icon(Icons.Default.Search, contentDescription = "Search", tint = TextSecondary, modifier = Modifier.size(20.dp))
                    }
                    IconButton(onClick = { onNavigate(Screen.Settings) }, modifier = Modifier.size(36.dp)) {
                        Icon(Icons.Default.Settings, contentDescription = "Settings", tint = TextSecondary, modifier = Modifier.size(20.dp))
                    }
                }
            }

            // Search Bar (Screen 2: Search projects, files, commands...)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(BgCard)
                    .border(1.dp, BorderColor, RoundedCornerShape(10.dp))
                    .clickable { onOpenCommandPalette() }
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = null,
                    tint = TextMuted,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = "Search projects, files, commands...",
                    color = TextMuted,
                    fontSize = 12.sp
                )
            }
        }

        // Quick Action Grid (3 items row: + New Project, Open Project, Import ZIP)
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                QuickActionCard(
                    title = "+ New Project",
                    icon = Icons.Default.Add,
                    modifier = Modifier.weight(1f),
                    onClick = { onNavigate(Screen.NewProject) }
                )
                QuickActionCard(
                    title = "Open Project",
                    icon = Icons.Default.FolderOpen,
                    modifier = Modifier.weight(1f),
                    onClick = { onNavigate(Screen.Welcome) }
                )
                QuickActionCard(
                    title = "Import ZIP",
                    icon = Icons.Default.Archive,
                    modifier = Modifier.weight(1f),
                    onClick = { zipPicker.launch("application/zip") }
                )
            }
        }

        // Recent Projects Section with "See all"
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Recent Projects",
                    color = TextPrimary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold
                )
                if (recentProjects.size > 4) {
                    Text(
                        text = if (showAllProjects) "Collapse" else "See all (${recentProjects.size})",
                        color = AccentPrimary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.clickable { showAllProjects = !showAllProjects }
                    )
                }
            }
        }

        // Project Items List or Empty State
        if (recentProjects.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = BgCard),
                    border = androidx.compose.foundation.BorderStroke(1.dp, BorderColor)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.FolderOpen,
                            contentDescription = null,
                            tint = AccentPrimary.copy(alpha = 0.6f),
                            modifier = Modifier.size(36.dp)
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "No Projects Open Yet",
                            color = TextPrimary,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Create a project or import a ZIP archive to start coding.",
                            color = TextSecondary,
                            fontSize = 11.sp,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        androidx.compose.material3.Button(
                            onClick = { onNavigate(Screen.NewProject) },
                            colors = androidx.compose.material3.ButtonDefaults.buttonColors(containerColor = AccentPrimary),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Create Project", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        } else {
            items(displayProjects) { meta ->
                DashboardProjectItem(
                    metadata = meta,
                    onClick = {
                        scope.launch {
                            workspaceManager.openProject(meta).onSuccess {
                                onNavigate(Screen.Editor)
                            }
                        }
                    },
                    onDelete = {
                        scope.launch {
                            workspaceManager.deleteProject(meta.id)
                        }
                    }
                )
            }
        }

        // Supported Languages Section
        item {
            Spacer(modifier = Modifier.height(4.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Supported Languages",
                    color = TextPrimary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = "View Toolchains",
                    color = AccentPrimary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.clickable { onNavigate(Screen.Languages) }
                )
            }
            Spacer(modifier = Modifier.height(8.dp))

            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item { LanguageCard("Python", "PY", Color(0xFF3B82F6), Color(0xFFFBBF24)) { onNavigate(Screen.Languages) } }
                item { LanguageCard("JavaScript", "JS", Color(0xFFFBBF24), Color(0xFF000000)) { onNavigate(Screen.Languages) } }
                item { LanguageCard("TypeScript", "TS", Color(0xFF007ACC), Color(0xFFFFFFFF)) { onNavigate(Screen.Languages) } }
                item { LanguageCard("Rust", "RS", Color(0xFFF97316), Color(0xFFFFFFFF)) { onNavigate(Screen.Languages) } }
                item { LanguageCard("Kotlin", "KT", Color(0xFF8B5CF6), Color(0xFFFFFFFF)) { onNavigate(Screen.Languages) } }
                item { LanguageCard("C/C++", "C++", Color(0xFF0284C7), Color(0xFFFFFFFF)) { onNavigate(Screen.Languages) } }
                item { LanguageCard("Go", "GO", Color(0xFF00ADD8), Color(0xFFFFFFFF)) { onNavigate(Screen.Languages) } }
            }
        }

        // Workspace Diagnostics status
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .clickable { onNavigate(Screen.Problems) },
                colors = CardDefaults.cardColors(containerColor = BgCard),
                border = androidx.compose.foundation.BorderStroke(1.dp, BorderColor)
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (problemsCount > 0) StatusError.copy(alpha = 0.15f) else StatusSuccess.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = null,
                            tint = if (problemsCount > 0) StatusError else StatusSuccess,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (problemsCount > 0) "$problemsCount issues detected" else "Workspace clean & ready",
                            color = TextPrimary,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 13.sp
                        )
                        Text(
                            text = "Tap to review compiler problems and diagnostics",
                            color = TextSecondary,
                            fontSize = 11.sp
                        )
                    }
                    Icon(Icons.Default.ChevronRight, contentDescription = null, tint = TextMuted, modifier = Modifier.size(16.dp))
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }
    }

    if (showCreateDialog) {
        NewProjectDialog(
            projectName = projectName,
            onProjectNameChange = { projectName = it },
            selectedTemplate = selectedTemplate,
            onSelectTemplate = { selectedTemplate = it },
            onDismiss = { showCreateDialog = false },
            onCreate = {
                if (projectName.isNotBlank()) {
                    showCreateDialog = false
                    scope.launch {
                        workspaceManager.createProject(projectName, selectedTemplate, selectedTemplate).onSuccess {
                            onNavigate(Screen.Editor)
                        }
                    }
                }
            }
        )
    }
}

/**
 * 3-Card Quick Action Item for Home Dashboard
 */
@Composable
fun QuickActionCard(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = BgCard),
        border = androidx.compose.foundation.BorderStroke(1.dp, BorderColor)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp, horizontal = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(AccentPrimary.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = AccentCyan,
                    modifier = Modifier.size(16.dp)
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = title,
                color = TextPrimary,
                fontSize = 10.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1
            )
        }
    }
}

/**
 * Project item card on dashboard matching Screen 2
 */
@Composable
fun DashboardProjectItem(
    metadata: ProjectMetadata,
    onClick: () -> Unit,
    onDelete: () -> Unit = {}
) {
    val timeLabel = remember(metadata.lastOpened) {
        val diffHours = (System.currentTimeMillis() - metadata.lastOpened) / 3600000L
        if (diffHours < 1L) "Just now"
        else if (diffHours < 24L) "${diffHours}h ago"
        else "${diffHours / 24L}d ago"
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = BgCard),
        border = androidx.compose.foundation.BorderStroke(1.dp, BorderColor)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(AccentPrimary.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Folder,
                    contentDescription = null,
                    tint = AccentPrimary,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = metadata.name,
                    color = TextPrimary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = metadata.path,
                    color = TextSecondary,
                    fontSize = 10.sp,
                    maxLines = 1
                )
            }

            Text(
                text = timeLabel,
                color = TextMuted,
                fontSize = 11.sp
            )

            Spacer(modifier = Modifier.width(4.dp))

            IconButton(
                onClick = onDelete,
                modifier = Modifier.size(28.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Delete Project",
                    tint = TextMuted.copy(alpha = 0.6f),
                    modifier = Modifier.size(15.dp)
                )
            }
        }
    }
}

/**
 * Supported Language card with badge
 */
@Composable
fun LanguageCard(
    name: String,
    badgeText: String,
    badgeBg: Color,
    badgeTextColor: Color,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(BgCard)
            .border(1.dp, BorderColor, RoundedCornerShape(8.dp))
            .clickable { onClick() }
            .padding(horizontal = 10.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(22.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(badgeBg),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = badgeText,
                color = badgeTextColor,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold
            )
        }
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = name,
            color = TextPrimary,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium
        )
    }
}
