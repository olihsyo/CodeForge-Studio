package com.codeforge.studio.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.codeforge.studio.settings.SettingsManager
import com.codeforge.studio.ui.theme.AccentPrimary
import com.codeforge.studio.ui.theme.BgApp
import com.codeforge.studio.ui.theme.BgEditor
import com.codeforge.studio.ui.theme.BgPanel
import com.codeforge.studio.ui.theme.BorderColor
import com.codeforge.studio.ui.theme.TextPrimary
import com.codeforge.studio.ui.theme.TextSecondary

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

    var remoteUrlInput by remember(remoteUrl) { mutableStateOf(remoteUrl) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(BgApp)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(
                text = "Settings & Preferences",
                style = MaterialTheme.typography.headlineMedium.copy(color = TextPrimary, fontWeight = FontWeight.Bold)
            )
            Text(
                text = "Customize editor appearance, runtime options, and storage settings.",
                color = TextSecondary,
                fontSize = 12.sp
            )
        }

        // Appearance Section
        item {
            SettingSectionCard(title = "Appearance & Theme", icon = Icons.Default.Palette) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Dark Theme", color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                        Text("Deep Charcoal (#101216) & (#15181E)", color = TextSecondary, fontSize = 11.sp)
                    }
                    Switch(
                        checked = darkTheme,
                        onCheckedChange = { settingsManager.setDarkTheme(it) },
                        colors = SwitchDefaults.colors(checkedThumbColor = AccentPrimary)
                    )
                }
            }
        }

        // Editor Options
        item {
            SettingSectionCard(title = "Code Editor", icon = Icons.Default.Code) {
                // Font Size Slider
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Font Size", color = TextPrimary, fontSize = 14.sp)
                        Text("${fontSizeSp}sp", color = AccentPrimary, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    }
                    Slider(
                        value = fontSizeSp.toFloat(),
                        onValueChange = { settingsManager.setFontSize(it.toInt()) },
                        valueRange = 10f..26f,
                        steps = 8,
                        colors = SliderDefaults.colors(thumbColor = AccentPrimary, activeTrackColor = AccentPrimary)
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Tab Width
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Tab Indentation Width", color = TextPrimary, fontSize = 14.sp)
                        Text("Number of spaces per Tab key", color = TextSecondary, fontSize = 11.sp)
                    }
                    Text(
                        text = "$tabWidth spaces",
                        color = AccentPrimary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Word Wrap
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Word Wrap", color = TextPrimary, fontSize = 14.sp)
                        Text("Soft wrap long code lines in editor", color = TextSecondary, fontSize = 11.sp)
                    }
                    Switch(
                        checked = wordWrap,
                        onCheckedChange = { settingsManager.setWordWrap(it) },
                        colors = SwitchDefaults.colors(checkedThumbColor = AccentPrimary)
                    )
                }
            }
        }

        // Storage & Recovery
        item {
            SettingSectionCard(title = "Files & Storage", icon = Icons.Default.Storage) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Autosave & Draft Recovery", color = TextPrimary, fontSize = 14.sp)
                        Text("Persist unsaved changes in app cache", color = TextSecondary, fontSize = 11.sp)
                    }
                    Switch(
                        checked = autoSave,
                        onCheckedChange = { settingsManager.setAutoSave(it) },
                        colors = SwitchDefaults.colors(checkedThumbColor = AccentPrimary)
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Show Hidden Files", color = TextPrimary, fontSize = 14.sp)
                        Text("Display dotfiles (.git, .codeforge)", color = TextSecondary, fontSize = 11.sp)
                    }
                    Switch(
                        checked = showHidden,
                        onCheckedChange = { settingsManager.setShowHiddenFiles(it) },
                        colors = SwitchDefaults.colors(checkedThumbColor = AccentPrimary)
                    )
                }
            }
        }

        // Security & Remote Execution
        item {
            SettingSectionCard(title = "Security & Remote Execution", icon = Icons.Default.Security) {
                Text(
                    text = "Remote execution is strictly optional and requires explicit configuration.",
                    color = TextSecondary,
                    fontSize = 11.sp
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = remoteUrlInput,
                    onValueChange = {
                        remoteUrlInput = it
                        settingsManager.setRemoteExecutionUrl(it)
                    },
                    label = { Text("Remote Build Server URL (optional)") },
                    placeholder = { Text("https://my-build-server.example.com") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                        focusedBorderColor = AccentPrimary,
                        unfocusedBorderColor = BorderColor,
                        focusedContainerColor = BgEditor,
                        unfocusedContainerColor = BgEditor
                    )
                )
            }
        }
    }
}

@Composable
fun SettingSectionCard(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    content: @Composable () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = BgPanel),
        border = androidx.compose.foundation.BorderStroke(1.dp, BorderColor)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(imageVector = icon, contentDescription = null, tint = AccentPrimary, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(text = title, fontWeight = FontWeight.Bold, color = TextPrimary, fontSize = 14.sp)
            }
            Spacer(modifier = Modifier.height(14.dp))
            content()
        }
    }
}
