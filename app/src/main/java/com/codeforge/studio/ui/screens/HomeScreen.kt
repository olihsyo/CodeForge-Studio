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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Source
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.codeforge.studio.navigation.Screen
import com.codeforge.studio.runtimes.LanguageRegistry
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
import com.codeforge.studio.workspace.WorkspaceManager

@Composable
fun HomeScreen(
    workspaceManager: WorkspaceManager,
    onNavigate: (Screen) -> Unit,
    problemsCount: Int
) {
    val currentProject by workspaceManager.currentProject.collectAsState()
    val recentProjects by workspaceManager.recentProjects.collectAsState()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(BgApp)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Workspace Status Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = BgPanel),
                border = androidx.compose.foundation.BorderStroke(1.dp, BorderColor)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Active Workspace",
                            fontSize = 12.sp,
                            color = TextSecondary,
                            fontWeight = FontWeight.Medium
                        )
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(if (currentProject != null) StatusSuccess.copy(alpha = 0.2f) else StatusWarning.copy(alpha = 0.2f))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = if (currentProject != null) "READY" else "NO PROJECT OPEN",
                                color = if (currentProject != null) StatusSuccess else StatusWarning,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = currentProject?.name ?: "None selected",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Text(
                        text = currentProject?.path ?: "Tap 'Setup' to open or create a workspace",
                        fontSize = 11.sp,
                        color = TextSecondary,
                        maxLines = 1
                    )
                }
            }
        }

        // Quick Launch Grid
        item {
            Text("Quick Actions", color = TextPrimary, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                QuickTile(
                    title = "Code Editor",
                    subtitle = "Browse & edit",
                    icon = Icons.Default.Code,
                    color = AccentPrimary,
                    modifier = Modifier.weight(1f),
                    onClick = { onNavigate(Screen.Editor) }
                )
                QuickTile(
                    title = "Terminal",
                    subtitle = "Real shell session",
                    icon = Icons.Default.PlayArrow,
                    color = StatusSuccess,
                    modifier = Modifier.weight(1f),
                    onClick = { onNavigate(Screen.Terminal) }
                )
            }
            Spacer(modifier = Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                QuickTile(
                    title = "Build & Run",
                    subtitle = "Compile & execute",
                    icon = Icons.Default.Build,
                    color = AccentSecondary,
                    modifier = Modifier.weight(1f),
                    onClick = { onNavigate(Screen.Build) }
                )
                QuickTile(
                    title = "Git Control",
                    subtitle = "Diff & commits",
                    icon = Icons.Default.Source,
                    color = TextPrimary,
                    modifier = Modifier.weight(1f),
                    onClick = { onNavigate(Screen.SourceControl) }
                )
            }
        }

        // Supported Language Capabilities List
        item {
            Text("Supported Languages (14 Runtimes)", color = TextPrimary, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
            Spacer(modifier = Modifier.height(8.dp))
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(LanguageRegistry.languages) { lang ->
                    val isInstalled = lang.isRuntimeInstalled()
                    Column(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(BgPanel)
                            .border(1.dp, BorderColor, RoundedCornerShape(8.dp))
                            .padding(10.dp)
                            .width(130.dp)
                    ) {
                        Text(lang.displayName, fontWeight = FontWeight.Bold, color = AccentPrimary, fontSize = 13.sp)
                        Text(".${lang.extensions.joinToString(", .")}", color = TextSecondary, fontSize = 10.sp, maxLines = 1)
                        Spacer(modifier = Modifier.height(6.dp))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(if (isInstalled) StatusSuccess.copy(alpha = 0.15f) else BorderColor.copy(alpha = 0.5f))
                                .padding(horizontal = 4.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = if (isInstalled) "INSTALLED" else "AVAILABLE",
                                color = if (isInstalled) StatusSuccess else TextSecondary,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }

        // Diagnostics / Problems Summary
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onNavigate(Screen.Problems) },
                colors = CardDefaults.cardColors(containerColor = BgPanel),
                border = androidx.compose.foundation.BorderStroke(1.dp, BorderColor)
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = null,
                        tint = if (problemsCount > 0) StatusError else StatusSuccess,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Problems & Diagnostics", color = TextPrimary, fontWeight = FontWeight.Medium, fontSize = 14.sp)
                        Text(
                            text = if (problemsCount > 0) "$problemsCount issues detected across project" else "No syntax or compiler problems found",
                            color = TextSecondary,
                            fontSize = 11.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun QuickTile(
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: androidx.compose.ui.graphics.Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = BgPanel),
        border = androidx.compose.foundation.BorderStroke(1.dp, BorderColor)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Icon(imageVector = icon, contentDescription = null, tint = color, modifier = Modifier.size(22.dp))
            Spacer(modifier = Modifier.height(6.dp))
            Text(title, fontWeight = FontWeight.SemiBold, color = TextPrimary, fontSize = 13.sp)
            Text(subtitle, color = TextSecondary, fontSize = 10.sp)
        }
    }
}
