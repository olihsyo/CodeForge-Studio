package com.codeforge.studio.ui.screens

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material.icons.filled.Source
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.codeforge.studio.settings.SettingsManager
import com.codeforge.studio.ui.theme.AccentCyan
import com.codeforge.studio.ui.theme.AccentPrimary
import com.codeforge.studio.ui.theme.BgApp
import com.codeforge.studio.ui.theme.BgCard
import com.codeforge.studio.ui.theme.BgEditor
import com.codeforge.studio.ui.theme.BgPanel
import com.codeforge.studio.ui.theme.BorderColor
import com.codeforge.studio.ui.theme.TextMuted
import com.codeforge.studio.ui.theme.TextPrimary
import com.codeforge.studio.ui.theme.TextSecondary

/**
 * Screen 10: Settings
 * Matches Appearance, Editor, Workspace, Runtimes & Build, Terminal, Git, AI Assistant, Security list.
 */
@Composable
fun SettingsScreen(
    settingsManager: SettingsManager
) {
    val darkTheme by settingsManager.darkTheme.collectAsState()
    val fontSizeSp by settingsManager.fontSizeSp.collectAsState()
    val tabWidth by settingsManager.tabWidth.collectAsState()
    val wordWrap by settingsManager.wordWrap.collectAsState()
    val autoSave by settingsManager.autoSaveEnabled.collectAsState()
    val showHidden by settingsManager.showHiddenFiles.collectAsState()
    val remoteUrl by settingsManager.remoteExecutionUrl.collectAsState()
    val gitName by settingsManager.gitUserName.collectAsState()
    val gitEmail by settingsManager.gitUserEmail.collectAsState()
    val aiApiKey by settingsManager.aiApiKey.collectAsState()
    val aiModel by settingsManager.aiModel.collectAsState()

    var remoteUrlInput by remember(remoteUrl) { mutableStateOf(remoteUrl) }
    var gitNameInput by remember(gitName) { mutableStateOf(gitName) }
    var gitEmailInput by remember(gitEmail) { mutableStateOf(gitEmail) }
    var aiKeyInput by remember(aiApiKey) { mutableStateOf(aiApiKey) }
    var aiModelInput by remember(aiModel) { mutableStateOf(aiModel) }
    var expandedSection by remember { mutableStateOf<String?>("Appearance") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BgApp)
    ) {
        // Screen 10 Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(BgPanel)
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Settings",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // 1. Appearance (Themes, Fonts, Editor)
            item {
                SettingsCategoryCard(
                    title = "Appearance",
                    subtitle = "Themes, Fonts, Editor",
                    icon = Icons.Default.Palette,
                    isExpanded = expandedSection == "Appearance",
                    onClick = { expandedSection = if (expandedSection == "Appearance") null else "Appearance" }
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Dark Theme", color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                            Text("Deep Charcoal (#0B0E14) & (#151B26)", color = TextSecondary, fontSize = 11.sp)
                        }
                        Switch(
                            checked = darkTheme,
                            onCheckedChange = { settingsManager.setDarkTheme(it) },
                            colors = SwitchDefaults.colors(checkedThumbColor = AccentCyan, checkedTrackColor = AccentPrimary)
                        )
                    }
                }
            }

            // 2. Editor (Font size, Tab width, Word wrap)
            item {
                SettingsCategoryCard(
                    title = "Editor",
                    subtitle = "Font size, Tab width, Word wrap",
                    icon = Icons.Default.Code,
                    isExpanded = expandedSection == "Editor",
                    onClick = { expandedSection = if (expandedSection == "Editor") null else "Editor" }
                ) {
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Editor Font Size", color = TextPrimary, fontSize = 13.sp)
                            Text("${fontSizeSp}sp", color = AccentCyan, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }
                        Slider(
                            value = fontSizeSp.toFloat(),
                            onValueChange = { settingsManager.setFontSize(it.toInt()) },
                            valueRange = 10f..26f,
                            steps = 8,
                            colors = SliderDefaults.colors(thumbColor = AccentCyan, activeTrackColor = AccentPrimary)
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Tab Width", color = TextPrimary, fontSize = 13.sp)
                            Text(
                                text = "$tabWidth spaces",
                                color = AccentCyan,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                modifier = Modifier.clickable {
                                    val nextWidth = if (tabWidth == 4) 2 else 4
                                    settingsManager.setTabWidth(nextWidth)
                                }
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Soft Word Wrap", color = TextPrimary, fontSize = 13.sp)
                            Switch(
                                checked = wordWrap,
                                onCheckedChange = { settingsManager.setWordWrap(it) },
                                colors = SwitchDefaults.colors(checkedThumbColor = AccentCyan, checkedTrackColor = AccentPrimary)
                            )
                        }
                    }
                }
            }

            // 3. Workspace (Project directories, Autosave)
            item {
                SettingsCategoryCard(
                    title = "Workspace",
                    subtitle = "Project directories, Autosave",
                    icon = Icons.Default.Folder,
                    isExpanded = expandedSection == "Workspace",
                    onClick = { expandedSection = if (expandedSection == "Workspace") null else "Workspace" }
                ) {
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text("Autosave & Drafts", color = TextPrimary, fontSize = 13.sp)
                                Text("Persist unsaved changes in local cache", color = TextSecondary, fontSize = 10.sp)
                            }
                            Switch(
                                checked = autoSave,
                                onCheckedChange = { settingsManager.setAutoSave(it) },
                                colors = SwitchDefaults.colors(checkedThumbColor = AccentCyan, checkedTrackColor = AccentPrimary)
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text("Show Hidden Files", color = TextPrimary, fontSize = 13.sp)
                                Text("Show dotfiles (.git, .gradle)", color = TextSecondary, fontSize = 10.sp)
                            }
                            Switch(
                                checked = showHidden,
                                onCheckedChange = { settingsManager.setShowHiddenFiles(it) },
                                colors = SwitchDefaults.colors(checkedThumbColor = AccentCyan, checkedTrackColor = AccentPrimary)
                            )
                        }
                    }
                }
            }

            // 4. Runtimes & Build (Toolchains, Dependencies)
            item {
                SettingsCategoryCard(
                    title = "Runtimes & Build",
                    subtitle = "Toolchains, Dependencies",
                    icon = Icons.Default.Memory,
                    isExpanded = expandedSection == "Runtimes",
                    onClick = { expandedSection = if (expandedSection == "Runtimes") null else "Runtimes" }
                ) {
                    Column {
                        Text("Configured Toolchains: 14 Languages", color = TextPrimary, fontSize = 13.sp)
                        Text("Python, Node.js, Kotlin, Clang, Rust, Go, Java, Web", color = TextSecondary, fontSize = 11.sp)
                    }
                }
            }

            // 5. Terminal (Shell, Environment)
            item {
                SettingsCategoryCard(
                    title = "Terminal",
                    subtitle = "Shell, Environment",
                    icon = Icons.Default.Terminal,
                    isExpanded = expandedSection == "Terminal",
                    onClick = { expandedSection = if (expandedSection == "Terminal") null else "Terminal" }
                ) {
                    Text("Interactive PTY Shell: /system/bin/sh", color = TextSecondary, fontSize = 12.sp)
                }
            }

            // 6. Git (Configuration, Authentication)
            item {
                SettingsCategoryCard(
                    title = "Git",
                    subtitle = "Configuration, Authentication",
                    icon = Icons.Default.Source,
                    isExpanded = expandedSection == "Git",
                    onClick = { expandedSection = if (expandedSection == "Git") null else "Git" }
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Git Author Identity", color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                        OutlinedTextField(
                            value = gitNameInput,
                            onValueChange = {
                                gitNameInput = it
                                settingsManager.setGitConfig(it, gitEmailInput)
                            },
                            label = { Text("Author Name", fontSize = 11.sp) },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = TextPrimary,
                                unfocusedTextColor = TextPrimary,
                                focusedBorderColor = AccentPrimary,
                                unfocusedBorderColor = BorderColor
                            )
                        )
                        OutlinedTextField(
                            value = gitEmailInput,
                            onValueChange = {
                                gitEmailInput = it
                                settingsManager.setGitConfig(gitNameInput, it)
                            },
                            label = { Text("Author Email", fontSize = 11.sp) },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = TextPrimary,
                                unfocusedTextColor = TextPrimary,
                                focusedBorderColor = AccentPrimary,
                                unfocusedBorderColor = BorderColor
                            )
                        )
                    }
                }
            }

            // 7. AI Assistant (Model, API key, Privacy)
            item {
                SettingsCategoryCard(
                    title = "AI Assistant",
                    subtitle = "Model, API key, Privacy",
                    icon = Icons.Default.SmartToy,
                    isExpanded = expandedSection == "AI",
                    onClick = { expandedSection = if (expandedSection == "AI") null else "AI" }
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Cloud AI Configuration (Gemini / OpenAI)", color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                        Text("Leave blank to use local offline analysis.", color = TextSecondary, fontSize = 11.sp)

                        OutlinedTextField(
                            value = aiKeyInput,
                            onValueChange = {
                                aiKeyInput = it
                                settingsManager.setAiApiKey(it)
                            },
                            label = { Text("Gemini API Key", fontSize = 11.sp) },
                            placeholder = { Text("AIzaSy...") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = TextPrimary,
                                unfocusedTextColor = TextPrimary,
                                focusedBorderColor = AccentPrimary,
                                unfocusedBorderColor = BorderColor
                            )
                        )

                        OutlinedTextField(
                            value = aiModelInput,
                            onValueChange = {
                                aiModelInput = it
                                settingsManager.setAiModel(it)
                            },
                            label = { Text("AI Model", fontSize = 11.sp) },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = TextPrimary,
                                unfocusedTextColor = TextPrimary,
                                focusedBorderColor = AccentPrimary,
                                unfocusedBorderColor = BorderColor
                            )
                        )
                    }
                }
            }

            // 8. Security (Permissions, Storage, Privacy)
            item {
                SettingsCategoryCard(
                    title = "Security",
                    subtitle = "Permissions, Storage, Privacy",
                    icon = Icons.Default.Security,
                    isExpanded = expandedSection == "Security",
                    onClick = { expandedSection = if (expandedSection == "Security") null else "Security" }
                ) {
                    Column {
                        Text("Storage Sandbox: App Internal & Document Tree", color = TextSecondary, fontSize = 11.sp)
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = remoteUrlInput,
                            onValueChange = {
                                remoteUrlInput = it
                                settingsManager.setRemoteExecutionUrl(it)
                            },
                            label = { Text("Remote Server URL (optional)", fontSize = 11.sp) },
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
                    }
                }
            }
        }
    }
}

/**
 * Settings category card matching Screen 10
 */
@Composable
fun SettingsCategoryCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    isExpanded: Boolean,
    onClick: () -> Unit,
    content: @Composable () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = BgCard),
        border = androidx.compose.foundation.BorderStroke(1.dp, BorderColor)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(AccentPrimary.copy(alpha = 0.12f)),
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
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = subtitle,
                        color = TextSecondary,
                        fontSize = 11.sp
                    )
                }

                Icon(
                    imageVector = if (isExpanded) Icons.Default.ExpandMore else Icons.Default.ChevronRight,
                    contentDescription = null,
                    tint = TextMuted,
                    modifier = Modifier.size(18.dp)
                )
            }

            AnimatedVisibility(visible = isExpanded) {
                Column(modifier = Modifier.padding(top = 14.dp)) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(1.dp)
                            .background(BorderColor)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    content()
                }
            }
        }
    }
}
