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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
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
import com.codeforge.studio.execution.OutputType
import com.codeforge.studio.terminal.TerminalSession
import com.codeforge.studio.ui.theme.AccentPrimary
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

@Composable
fun TerminalScreen(
    currentProject: Project?,
    terminalSession: TerminalSession?
) {
    val scope = rememberCoroutineScope()
    var inputCommand by remember { mutableStateOf("") }
    val listState = rememberLazyListState()

    val lines by terminalSession?.lines?.collectAsState() ?: remember { mutableStateOf(emptyList()) }
    val isRunning by terminalSession?.isRunning?.collectAsState() ?: remember { mutableStateOf(false) }
    val currentDir by terminalSession?.currentDir?.collectAsState() ?: remember { mutableStateOf(currentProject?.rootDirectory) }

    // Auto-scroll to bottom on new output
    LaunchedEffect(lines.size) {
        if (lines.isNotEmpty()) {
            listState.animateScrollToItem(lines.size - 1)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BgEditor)
    ) {
        // Terminal Header Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(44.dp)
                .background(BgPanel)
                .padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(if (isRunning) StatusSuccess else StatusWarning)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "SHELL (${terminalSession?.environmentInfo ?: "sh"})",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
            }

            Row {
                IconButton(
                    onClick = { scope.launch { terminalSession?.sendCommand("clear") } },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(Icons.Default.Clear, contentDescription = "Clear", tint = TextSecondary, modifier = Modifier.size(16.dp))
                }
                IconButton(
                    onClick = { terminalSession?.kill() },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(Icons.Default.Stop, contentDescription = "Kill Process", tint = StatusError, modifier = Modifier.size(16.dp))
                }
            }
        }

        // Terminal Log Area
        LazyColumn(
            state = listState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp)
        ) {
            items(lines) { line ->
                val color = when (line.type) {
                    OutputType.STDOUT -> TextPrimary
                    OutputType.STDERR -> StatusError
                    OutputType.SYSTEM_INFO -> AccentPrimary
                    OutputType.SYSTEM_ERROR -> StatusError
                }
                Text(
                    text = line.text,
                    color = color,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 12.sp,
                    lineHeight = 18.sp
                )
            }
        }

        // Quick Command Chips
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(BgPanel)
                .padding(horizontal = 8.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            listOf("ls -la", "pwd", "python3 main.py", "git status").forEach { cmd ->
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(BgEditor)
                        .border(1.dp, BorderColor, RoundedCornerShape(4.dp))
                        .clickable { scope.launch { terminalSession?.sendCommand(cmd) } }
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(text = cmd, color = TextSecondary, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                }
            }
        }

        // Command Input Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(BgPanel)
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = inputCommand,
                onValueChange = { inputCommand = it },
                placeholder = { Text("Enter command...", color = TextSecondary, fontSize = 13.sp) },
                singleLine = true,
                modifier = Modifier.weight(1f),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary,
                    focusedBorderColor = AccentPrimary,
                    unfocusedBorderColor = BorderColor,
                    focusedContainerColor = BgEditor,
                    unfocusedContainerColor = BgEditor
                )
            )

            Spacer(modifier = Modifier.width(8.dp))

            IconButton(
                onClick = {
                    if (inputCommand.isNotBlank()) {
                        val cmd = inputCommand
                        inputCommand = ""
                        scope.launch { terminalSession?.sendCommand(cmd) }
                    }
                },
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(AccentPrimary)
            ) {
                Icon(
                    imageVector = Icons.Default.Send,
                    contentDescription = "Send Command",
                    tint = BgApp,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}
