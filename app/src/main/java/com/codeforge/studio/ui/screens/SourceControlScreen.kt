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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Commit
import androidx.compose.material.icons.filled.Difference
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Source
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
import com.codeforge.studio.ui.theme.AccentPrimary
import com.codeforge.studio.ui.theme.AccentSecondary
import com.codeforge.studio.ui.theme.BgApp
import com.codeforge.studio.ui.theme.BgEditor
import com.codeforge.studio.ui.theme.BgPanel
import com.codeforge.studio.ui.theme.BorderColor
import com.codeforge.studio.ui.theme.StatusError
import com.codeforge.studio.ui.theme.StatusSuccess
import com.codeforge.studio.ui.theme.StatusWarning
import com.codeforge.studio.ui.theme.TextPrimary
import com.codeforge.studio.ui.theme.TextSecondary
import com.codeforge.studio.workspace.Project
import kotlinx.coroutines.launch
import java.io.File

@Composable
fun SourceControlScreen(
    currentProject: Project?,
    gitManager: GitManager?
) {
    val scope = rememberCoroutineScope()
    var selectedTab by remember { mutableStateOf(0) } // 0: Changes, 1: History
    var commitMessage by remember { mutableStateOf("") }
    var branchName by remember { mutableStateOf("main") }

    val changedFiles = remember { mutableStateListOf<GitFileStatus>() }
    val commitHistory = remember { mutableStateListOf<GitCommit>() }
    var activeDiff by remember { mutableStateOf<GitDiff?>(null) }
    var isRefreshing by remember { mutableStateOf(false) }

    fun refreshGit() {
        if (gitManager != null) {
            scope.launch {
                isRefreshing = true
                branchName = gitManager.getBranchName()
                val status = gitManager.getStatus()
                changedFiles.clear()
                changedFiles.addAll(status)

                val history = gitManager.getCommitHistory()
                commitHistory.clear()
                commitHistory.addAll(history)
                isRefreshing = false
            }
        }
    }

    LaunchedEffect(currentProject) {
        refreshGit()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BgApp)
    ) {
        // Git Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(BgPanel)
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Source,
                    contentDescription = null,
                    tint = AccentPrimary,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(
                        text = "SOURCE CONTROL",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = AccentPrimary
                    )
                    Text(
                        text = "Branch: $branchName",
                        fontSize = 13.sp,
                        color = TextPrimary,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            IconButton(onClick = { refreshGit() }, modifier = Modifier.size(36.dp)) {
                Icon(Icons.Default.Refresh, contentDescription = "Refresh Git", tint = TextSecondary, modifier = Modifier.size(20.dp))
            }
        }

        // Tabs
        TabRow(
            selectedTabIndex = selectedTab,
            containerColor = BgPanel,
            contentColor = AccentPrimary
        ) {
            Tab(
                selected = selectedTab == 0,
                onClick = { selectedTab = 0 },
                text = { Text("Changes (${changedFiles.size})", fontSize = 13.sp) }
            )
            Tab(
                selected = selectedTab == 1,
                onClick = { selectedTab = 1 },
                text = { Text("History (${commitHistory.size})", fontSize = 13.sp) }
            )
        }

        if (selectedTab == 0) {
            // Commit Box
            Column(modifier = Modifier.padding(16.dp)) {
                OutlinedTextField(
                    value = commitMessage,
                    onValueChange = { commitMessage = it },
                    placeholder = { Text("Commit message (e.g. Update main script)", color = TextSecondary, fontSize = 13.sp) },
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
                        if (commitMessage.isNotBlank() && gitManager != null) {
                            scope.launch {
                                gitManager.commit(commitMessage).onSuccess {
                                    commitMessage = ""
                                    refreshGit()
                                }
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = commitMessage.isNotBlank() && changedFiles.isNotEmpty(),
                    colors = ButtonDefaults.buttonColors(containerColor = AccentPrimary)
                ) {
                    Icon(Icons.Default.Commit, contentDescription = null, tint = BgApp, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Commit Changes", color = BgApp, fontWeight = FontWeight.Bold)
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Working Tree Changes",
                    color = TextPrimary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold
                )

                Spacer(modifier = Modifier.height(8.dp))

                if (changedFiles.isEmpty()) {
                    Text(
                        text = "Working tree clean. No uncommitted modifications.",
                        color = TextSecondary,
                        fontSize = 12.sp
                    )
                } else {
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(changedFiles) { fileStatus ->
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable {
                                        scope.launch {
                                            activeDiff = gitManager?.computeDiff(fileStatus.file)
                                        }
                                    },
                                colors = CardDefaults.cardColors(containerColor = BgPanel),
                                border = androidx.compose.foundation.BorderStroke(1.dp, BorderColor)
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(24.dp)
                                            .clip(RoundedCornerShape(4.dp))
                                            .background(
                                                when (fileStatus.status) {
                                                    "M" -> StatusWarning.copy(alpha = 0.2f)
                                                    "A" -> StatusSuccess.copy(alpha = 0.2f)
                                                    else -> AccentPrimary.copy(alpha = 0.2f)
                                                }
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = fileStatus.status,
                                            color = when (fileStatus.status) {
                                                "M" -> StatusWarning
                                                "A" -> StatusSuccess
                                                else -> AccentPrimary
                                            },
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 11.sp
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text(
                                        text = fileStatus.relativePath,
                                        color = TextPrimary,
                                        fontSize = 13.sp,
                                        modifier = Modifier.weight(1f)
                                    )
                                    Icon(
                                        imageVector = Icons.Default.Difference,
                                        contentDescription = "View Diff",
                                        tint = AccentPrimary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        } else {
            // Commit History
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (commitHistory.isEmpty()) {
                    item {
                        Text(
                            text = "No commits recorded yet. Make changes and commit to build history.",
                            color = TextSecondary,
                            fontSize = 12.sp
                        )
                    }
                } else {
                    items(commitHistory) { commit ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = BgPanel),
                            border = androidx.compose.foundation.BorderStroke(1.dp, BorderColor)
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = commit.message,
                                        color = TextPrimary,
                                        fontWeight = FontWeight.Medium,
                                        fontSize = 14.sp
                                    )
                                    Text(
                                        text = commit.hash,
                                        color = AccentPrimary,
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "${commit.author} • ${commit.dateString}",
                                    color = TextSecondary,
                                    fontSize = 11.sp
                                )
                            }
                        }
                    }
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
                border = androidx.compose.foundation.BorderStroke(1.dp, BorderColor),
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
                        Button(
                            onClick = { activeDiff = null },
                            colors = ButtonDefaults.buttonColors(containerColor = BgPanel)
                        ) { Text("Close", color = TextSecondary, fontSize = 11.sp) }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f, fill = false)
                            .heightIn(max = 350.dp)
                    ) {
                        items(activeDiff?.diffLines ?: emptyList()) { line ->
                            val bgColor = when (line.type) {
                                DiffType.ADDED -> StatusSuccess.copy(alpha = 0.15f)
                                DiffType.DELETED -> StatusError.copy(alpha = 0.15f)
                                DiffType.HEADER -> AccentPrimary.copy(alpha = 0.1f)
                                DiffType.UNCHANGED -> androidx.compose.ui.graphics.Color.Transparent
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
}
