package com.codeforge.studio.ui.screens

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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AltRoute
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Commit
import androidx.compose.material.icons.filled.Difference
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Source
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.Undo
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.codeforge.studio.sourcecontrol.DiffLine
import com.codeforge.studio.sourcecontrol.DiffType
import com.codeforge.studio.sourcecontrol.GitCommit
import com.codeforge.studio.sourcecontrol.GitDiff
import com.codeforge.studio.sourcecontrol.GitFileStatus
import com.codeforge.studio.sourcecontrol.GitManager
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
import com.codeforge.studio.workspace.Project
import kotlinx.coroutines.launch
import java.io.File

/**
 * Screen 9: Source Control (Git)
 * Matches Git header with branch dropdown, 3 stat summary cards (Changes, Staged, Untracked),
 * file change list with badges (M, A, U), Stage All, and Commit action button.
 */
@Composable
fun SourceControlScreen(
    currentProject: Project?,
    gitManager: GitManager?
) {
    val scope = rememberCoroutineScope()
    var commitMessage by remember { mutableStateOf("Update UI and fix issues") }
    var branchName by remember { mutableStateOf("main") }
    var showBranchDialog by remember { mutableStateOf(false) }
    var newBranchInput by remember { mutableStateOf("") }
    val availableBranches = remember { mutableStateListOf<String>() }

    val changedFiles = remember { mutableStateListOf<GitFileStatus>() }
    val commitHistory = remember { mutableStateListOf<GitCommit>() }
    var activeDiff by remember { mutableStateOf<GitDiff?>(null) }
    var isRefreshing by remember { mutableStateOf(false) }

    fun refreshGit() {
        if (gitManager != null && gitManager.isGitRepo) {
            scope.launch {
                isRefreshing = true
                branchName = gitManager.getBranchName()
                val branches = gitManager.getBranches()
                availableBranches.clear()
                availableBranches.addAll(branches)

                val status = gitManager.getStatus()
                changedFiles.clear()
                changedFiles.addAll(status)

                val history = gitManager.getCommitHistory()
                commitHistory.clear()
                commitHistory.addAll(history)
                isRefreshing = false
            }
        } else {
            changedFiles.clear()
            commitHistory.clear()
            availableBranches.clear()
            isRefreshing = false
        }
    }

    LaunchedEffect(currentProject) {
        refreshGit()
    }

    val modifiedCount = changedFiles.count { it.status == "M" }
    val addedCount = changedFiles.count { it.status == "A" }
    val untrackedCount = changedFiles.count { it.status == "U" || it.status == "?" }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BgApp)
    ) {
        // Top Header (Screen 9: Git & branch selector)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(BgPanel)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "Git",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                Spacer(modifier = Modifier.width(12.dp))

                // Branch chip: clickable to switch or create branch
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(BgCard)
                        .border(1.dp, BorderColor, RoundedCornerShape(6.dp))
                        .clickable { showBranchDialog = true }
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.AltRoute,
                        contentDescription = "Switch Branch",
                        tint = AccentCyan,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = branchName,
                        color = TextPrimary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = { refreshGit() }, modifier = Modifier.size(36.dp)) {
                    Icon(Icons.Default.Sync, contentDescription = "Sync", tint = TextSecondary, modifier = Modifier.size(18.dp))
                }
                IconButton(onClick = { /* Menu */ }, modifier = Modifier.size(36.dp)) {
                    Icon(Icons.Default.MoreVert, contentDescription = "Menu", tint = TextSecondary, modifier = Modifier.size(18.dp))
                }
            }
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Screen 9: 3 Stat Summary Cards in a row
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    val stagedCount = changedFiles.count { it.isStaged }
                    val untrackedCount = changedFiles.count { it.status == "?" }

                    // Card 1: Changes (Green)
                    GitStatCard(
                        count = changedFiles.size.toString(),
                        label = "Changes",
                        color = StatusSuccess,
                        modifier = Modifier.weight(1f)
                    )

                    // Card 2: Staged (Blue)
                    GitStatCard(
                        count = stagedCount.toString(),
                        label = "Staged",
                        color = AccentPrimary,
                        modifier = Modifier.weight(1f)
                    )

                    // Card 3: Untracked (Orange)
                    GitStatCard(
                        count = untrackedCount.toString(),
                        label = "Untracked",
                        color = StatusWarning,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            if (gitManager == null || !gitManager.isGitRepo) {
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
                                imageVector = Icons.Default.Source,
                                contentDescription = null,
                                tint = AccentPrimary,
                                modifier = Modifier.size(36.dp)
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = "Git Repository Not Initialized",
                                color = TextPrimary,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Initialize Git to track file revisions, stage snapshots, and commit changes.",
                                color = TextSecondary,
                                fontSize = 11.sp,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(14.dp))
                            Button(
                                onClick = {
                                    scope.launch {
                                        gitManager?.initRepository()?.onSuccess {
                                            refreshGit()
                                        }
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = AccentPrimary),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Icon(Icons.Default.AltRoute, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Initialize Git (git init)", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            } else if (changedFiles.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = BgCard),
                        border = androidx.compose.foundation.BorderStroke(1.dp, BorderColor)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(StatusSuccess.copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = null,
                                    tint = StatusSuccess,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "Working tree clean",
                                    color = TextPrimary,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = "No uncommitted changes on branch '$branchName'.",
                                    color = TextSecondary,
                                    fontSize = 11.sp
                                )
                            }
                        }
                    }
                }
            } else {
                // Section: Changes
                item {
                    Text(
                        text = "Changes (${changedFiles.size})",
                        color = TextPrimary,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 14.sp
                    )
                }
            }

            // List of changed files matching Screen 9
            items(changedFiles) { fileStatus ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .clickable {
                            scope.launch {
                                activeDiff = gitManager?.computeDiff(fileStatus.file) ?: GitDiff(
                                    filePath = fileStatus.relativePath,
                                    oldContent = "// Original content",
                                    newContent = "// Updated content",
                                    diffLines = listOf(
                                        DiffLine(DiffType.HEADER, "@@ -1,5 +1,6 @@", null, null),
                                        DiffLine(DiffType.DELETED, "- // Old code line", 1, null),
                                        DiffLine(DiffType.ADDED, "+ // Updated code line with new features", null, 1),
                                        DiffLine(DiffType.UNCHANGED, "  fun main() {", 2, 2)
                                    )
                                )
                            }
                        },
                    colors = CardDefaults.cardColors(containerColor = BgCard),
                    border = androidx.compose.foundation.BorderStroke(1.dp, BorderColor)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Status badge (M, A, U, D)
                        val badgeBg = when (fileStatus.status) {
                            "M" -> StatusWarning.copy(alpha = 0.2f)
                            "A" -> StatusSuccess.copy(alpha = 0.2f)
                            "D" -> StatusError.copy(alpha = 0.2f)
                            else -> TextSecondary.copy(alpha = 0.2f)
                        }
                        val badgeColor = when (fileStatus.status) {
                            "M" -> StatusWarning
                            "A" -> StatusSuccess
                            "D" -> StatusError
                            else -> TextSecondary
                        }

                        Box(
                            modifier = Modifier
                                .size(22.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(badgeBg),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = fileStatus.status,
                                color = badgeColor,
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp
                            )
                        }

                        Spacer(modifier = Modifier.width(10.dp))

                        Text(
                            text = fileStatus.relativePath,
                            color = TextPrimary,
                            fontSize = 12.sp,
                            fontFamily = FontFamily.Monospace,
                            modifier = Modifier.weight(1f)
                        )

                        // Per-file actions: Stage/Unstage, Discard, Diff
                        IconButton(
                            onClick = {
                                scope.launch {
                                    if (fileStatus.isStaged) {
                                        gitManager?.unstageFile(fileStatus.file)
                                    } else {
                                        gitManager?.stageFile(fileStatus.file)
                                    }
                                    refreshGit()
                                }
                            },
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                imageVector = if (fileStatus.isStaged) Icons.Default.Remove else Icons.Default.Add,
                                contentDescription = if (fileStatus.isStaged) "Unstage" else "Stage",
                                tint = if (fileStatus.isStaged) StatusWarning else AccentPrimary,
                                modifier = Modifier.size(16.dp)
                            )
                        }

                        IconButton(
                            onClick = {
                                scope.launch {
                                    gitManager?.discardChanges(fileStatus.file)
                                    refreshGit()
                                }
                            },
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Undo,
                                contentDescription = "Discard Changes",
                                tint = StatusError,
                                modifier = Modifier.size(16.dp)
                            )
                        }

                        IconButton(
                            onClick = {
                                scope.launch {
                                    activeDiff = gitManager?.computeDiff(fileStatus.file)
                                }
                            },
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Difference,
                                contentDescription = "Diff",
                                tint = AccentPrimary,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }

            // Action Buttons: Stage All | Unstage All
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = {
                            scope.launch {
                                gitManager?.stageAll()
                                refreshGit()
                            }
                        },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = BgCard),
                        border = androidx.compose.foundation.BorderStroke(1.dp, BorderColor)
                    ) {
                        Text("Stage All", color = TextPrimary, fontSize = 12.sp)
                    }

                    Button(
                        onClick = {
                            scope.launch {
                                gitManager?.unstageAll()
                                refreshGit()
                            }
                        },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = AccentPrimary)
                    ) {
                        Text("Unstage All", color = Color.White, fontSize = 12.sp)
                    }
                }
            }

            // Commit Message Text Box
            item {
                Spacer(modifier = Modifier.height(4.dp))
                OutlinedTextField(
                    value = commitMessage,
                    onValueChange = { commitMessage = it },
                    label = { Text("Commit Message", fontSize = 12.sp) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                        focusedBorderColor = AccentPrimary,
                        unfocusedBorderColor = BorderColor,
                        focusedContainerColor = BgCard,
                        unfocusedContainerColor = BgCard
                    )
                )
            }

            // Large Full-Width Commit Button (Screen 9 bottom)
            item {
                Button(
                    onClick = {
                        if (commitMessage.isNotBlank()) {
                            scope.launch {
                                gitManager?.commit(commitMessage)?.onSuccess {
                                    commitMessage = ""
                                    refreshGit()
                                }
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = AccentPrimary)
                ) {
                    Text("Commit", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }
            }
        }
    }

    // Diff Viewer Modal
    if (activeDiff != null) {
        Dialog(onDismissRequest = { activeDiff = null }) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = BgEditor,
                border = androidx.compose.foundation.BorderStroke(1.dp, BorderHighlight),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Diff: ${activeDiff?.filePath}",
                            color = TextPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                        IconButton(onClick = { activeDiff = null }, modifier = Modifier.size(28.dp)) {
                            Icon(Icons.Default.Check, contentDescription = "Close", tint = AccentPrimary, modifier = Modifier.size(16.dp))
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 350.dp)
                    ) {
                        items(activeDiff?.diffLines ?: emptyList()) { line ->
                            val bgColor = when (line.type) {
                                DiffType.ADDED -> StatusSuccess.copy(alpha = 0.15f)
                                DiffType.DELETED -> StatusError.copy(alpha = 0.15f)
                                DiffType.HEADER -> AccentPrimary.copy(alpha = 0.1f)
                                DiffType.UNCHANGED -> Color.Transparent
                            }
                            val textColor = when (line.type) {
                                DiffType.ADDED -> StatusSuccess
                                DiffType.DELETED -> StatusError
                                DiffType.HEADER -> AccentPrimary
                                DiffType.UNCHANGED -> TextPrimary
                            }

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(bgColor)
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = line.text,
                                    color = textColor,
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 11.sp
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // Branch Selection and Creation Dialog
    if (showBranchDialog) {
        Dialog(onDismissRequest = { showBranchDialog = false }) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = BgCard,
                border = androidx.compose.foundation.BorderStroke(1.dp, BorderHighlight),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Switch or Create Branch",
                            color = TextPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                        IconButton(onClick = { showBranchDialog = false }, modifier = Modifier.size(24.dp)) {
                            Icon(Icons.Default.Close, contentDescription = "Close", tint = TextSecondary, modifier = Modifier.size(16.dp))
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Text("Branches:", color = TextSecondary, fontSize = 11.sp)
                    Spacer(modifier = Modifier.height(6.dp))

                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 140.dp)
                    ) {
                        items(availableBranches) { b ->
                            val isActive = b == branchName
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(if (isActive) AccentPrimary.copy(alpha = 0.2f) else Color.Transparent)
                                    .clickable {
                                        scope.launch {
                                            gitManager?.switchBranch(b)
                                            branchName = b
                                            showBranchDialog = false
                                            refreshGit()
                                        }
                                    }
                                    .padding(horizontal = 8.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AltRoute,
                                    contentDescription = null,
                                    tint = if (isActive) AccentPrimary else TextSecondary,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = b,
                                    color = if (isActive) AccentPrimary else TextPrimary,
                                    fontSize = 12.sp,
                                    fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal,
                                    modifier = Modifier.weight(1f)
                                )
                                if (isActive) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = "Active",
                                        tint = AccentPrimary,
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    Text("Create New Branch:", color = TextSecondary, fontSize = 11.sp)
                    Spacer(modifier = Modifier.height(6.dp))

                    OutlinedTextField(
                        value = newBranchInput,
                        onValueChange = { newBranchInput = it },
                        placeholder = { Text("e.g. feature/new-logic", fontSize = 11.sp) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary,
                            focusedBorderColor = AccentPrimary,
                            unfocusedBorderColor = BorderColor,
                            focusedContainerColor = BgEditor,
                            unfocusedContainerColor = BgEditor
                        )
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Button(
                        onClick = {
                            if (newBranchInput.isNotBlank()) {
                                scope.launch {
                                    gitManager?.createBranch(newBranchInput.trim())
                                    gitManager?.switchBranch(newBranchInput.trim())
                                    branchName = newBranchInput.trim()
                                    newBranchInput = ""
                                    showBranchDialog = false
                                    refreshGit()
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = AccentPrimary),
                        shape = RoundedCornerShape(6.dp),
                        enabled = newBranchInput.isNotBlank()
                    ) {
                        Text("Create & Checkout", color = Color.White, fontSize = 12.sp)
                    }
                }
            }
        }
    }
}

/**
 * Colored Git Stat summary card matching Screen 9
 */
@Composable
fun GitStatCard(
    count: String,
    label: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = BgCard),
        border = androidx.compose.foundation.BorderStroke(1.dp, BorderColor)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.Start
        ) {
            Text(
                text = count,
                color = color,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = label,
                color = TextSecondary,
                fontSize = 11.sp
            )
        }
    }
}
