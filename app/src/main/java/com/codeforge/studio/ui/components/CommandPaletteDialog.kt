package com.codeforge.studio.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Commit
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.FormatAlignLeft
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Tab
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.codeforge.studio.ui.theme.AccentCyan
import com.codeforge.studio.ui.theme.AccentPrimary
import com.codeforge.studio.ui.theme.BgApp
import com.codeforge.studio.ui.theme.BgCard
import com.codeforge.studio.ui.theme.BgEditor
import com.codeforge.studio.ui.theme.BgPanel
import com.codeforge.studio.ui.theme.BorderColor
import com.codeforge.studio.ui.theme.BorderHighlight
import com.codeforge.studio.ui.theme.TextMuted
import com.codeforge.studio.ui.theme.TextPrimary
import com.codeforge.studio.ui.theme.TextSecondary

data class CommandAction(
    val id: String,
    val title: String,
    val description: String,
    val icon: ImageVector,
    val shortcut: String? = null,
    val action: () -> Unit
)

/**
 * Screen 5: Command Palette
 * Matches search input, shortcut tags (Ctrl+P, Ctrl+G, etc.), and Recent section.
 */
@Composable
fun CommandPaletteDialog(
    actions: List<CommandAction>,
    onDismiss: () -> Unit
) {
    var query by remember { mutableStateOf("") }

    // Combine standard commands with shortcuts matching Screen 5
    val enrichedActions = remember(actions) {
        val shortcutsMap = mapOf(
            "open_editor" to "Ctrl+P",
            "goto_line" to "Ctrl+G",
            "switch_tab" to "Ctrl+Tab",
            "open_build" to "Ctrl+Shift+B",
            "open_terminal" to "Ctrl+'",
            "open_git" to "Ctrl+Shift+G",
            "open_settings" to "Ctrl+,"
        )
        actions.map { cmd ->
            cmd.copy(shortcut = cmd.shortcut ?: shortcutsMap[cmd.id])
        }
    }

    val filtered = remember(query, enrichedActions) {
        if (query.isBlank()) enrichedActions
        else enrichedActions.filter {
            it.title.contains(query, ignoreCase = true) || it.description.contains(query, ignoreCase = true)
        }
    }

    val recentCommands = listOf(
        "Start Debugging" to Icons.Default.PlayArrow,
        "Format Document" to Icons.Default.FormatAlignLeft,
        "Change Theme" to Icons.Default.Palette,
        "Find in Files" to Icons.Default.Search
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
                    .padding(14.dp)
            ) {
                // Top Search Bar (Screen 5: Search commands...)
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    placeholder = { Text("Search commands...", color = TextMuted, fontSize = 13.sp) },
                    leadingIcon = {
                        Icon(imageVector = Icons.Default.Search, contentDescription = null, tint = AccentCyan, modifier = Modifier.size(18.dp))
                    },
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

                Spacer(modifier = Modifier.height(10.dp))

                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 380.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    items(filtered) { cmd ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .clickable {
                                    onDismiss()
                                    cmd.action()
                                }
                                .padding(horizontal = 10.dp, vertical = 9.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = cmd.icon,
                                contentDescription = null,
                                tint = AccentPrimary,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = cmd.title,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium,
                                color = TextPrimary,
                                modifier = Modifier.weight(1f)
                            )
                            if (cmd.shortcut != null) {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(BgEditor)
                                        .border(1.dp, BorderColor, RoundedCornerShape(4.dp))
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = cmd.shortcut,
                                        fontSize = 10.sp,
                                        fontFamily = FontFamily.Monospace,
                                        color = TextSecondary
                                    )
                                }
                            }
                        }
                    }

                    if (query.isBlank()) {
                        item {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Recent",
                                color = TextMuted,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }

                        items(recentCommands) { (name, icon) ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable {
                                        onDismiss()
                                        val targetAction = enrichedActions.firstOrNull { it.title.contains(name, ignoreCase = true) }
                                            ?: when (name) {
                                                "Start Debugging" -> enrichedActions.firstOrNull { it.id == "open_problems" || it.id == "start_debugging" }
                                                "Format Document" -> enrichedActions.firstOrNull { it.id == "format_doc" || it.id == "open_editor" }
                                                "Change Theme" -> enrichedActions.firstOrNull { it.id == "change_theme" || it.id == "toggle_theme" }
                                                "Find in Files" -> enrichedActions.firstOrNull { it.id == "find_files" || it.id == "open_editor" }
                                                else -> null
                                            }
                                        targetAction?.action?.invoke()
                                    }
                                    .padding(horizontal = 10.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = icon,
                                    contentDescription = null,
                                    tint = TextSecondary,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    text = name,
                                    fontSize = 12.sp,
                                    color = TextSecondary
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
