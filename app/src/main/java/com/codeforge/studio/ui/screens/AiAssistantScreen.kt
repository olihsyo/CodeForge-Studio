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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Difference
import androidx.compose.material.icons.filled.FindInPage
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material.icons.filled.Source
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.codeforge.studio.ai.AiAssistantManager
import com.codeforge.studio.ai.AiChangeProposal
import com.codeforge.studio.ai.AiMessage
import com.codeforge.studio.ai.MessageSender
import com.codeforge.studio.filesystem.FileManager
import com.codeforge.studio.sourcecontrol.DiffType
import com.codeforge.studio.ui.theme.AccentCyan
import com.codeforge.studio.ui.theme.AccentPrimary
import com.codeforge.studio.ui.theme.AccentSecondary
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
import com.codeforge.studio.workspace.Project
import kotlinx.coroutines.launch
import java.io.File

/**
 * Screen 11: AI Coding Assistant
 * Matches AI Assistant header with Local Model chip, glowing avatar,
 * "How can I help you today?", prompt cards list, chat messages,
 * input bar and "Your code stays on your device" footer.
 */
@Composable
fun AiAssistantScreen(
    currentProject: Project?,
    aiManager: AiAssistantManager,
    fileManager: FileManager
) {
    val scope = rememberCoroutineScope()
    var inputQuery by remember { mutableStateOf("") }
    var isThinking by remember { mutableStateOf(false) }

    val messages = remember { mutableStateListOf<AiMessage>() }

    val quickPrompts = listOf(
        Pair("Explain selected code", Icons.Default.Code),
        Pair("Suggest a fix for an error", Icons.Default.Build),
        Pair("Generate code snippet", Icons.Default.Lightbulb),
        Pair("Summarize project structure", Icons.Default.FindInPage),
        Pair("Inspect Git changes", Icons.Default.Source)
    )

    fun handleSend(query: String) {
        if (query.isBlank()) return
        messages.add(AiMessage(MessageSender.USER, query))
        inputQuery = ""

        val mainFile = currentProject?.rootDirectory?.let { root ->
            File(root, "main.py").takeIf { it.exists() }
                ?: File(root, "index.html").takeIf { it.exists() }
                ?: root.walkTopDown().firstOrNull { it.isFile && !it.name.startsWith(".") }
        }

        scope.launch {
            isThinking = true
            val code = if (mainFile != null) fileManager.readFile(mainFile).getOrDefault("") else ""

            when {
                query.contains("test", ignoreCase = true) && mainFile != null -> {
                    val proposal = aiManager.generateTests(code, mainFile.extension, mainFile)
                    messages.add(
                        AiMessage(
                            sender = MessageSender.ASSISTANT,
                            text = "Generated automated unit tests for ${mainFile.name}. Review proposal diff below:",
                            proposal = proposal
                        )
                    )
                }
                else -> {
                    val result = aiManager.queryAi(query, code)
                    val reply = result.getOrElse { "Could not process request: ${it.message}" }
                    messages.add(AiMessage(sender = MessageSender.ASSISTANT, text = reply))
                }
            }
            isThinking = false
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BgApp)
    ) {
        // Screen 11 Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(BgPanel)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "AI Assistant",
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )

            Row(verticalAlignment = Alignment.CenterVertically) {
                // Local Model Badge (Screen 11)
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(AccentSecondary.copy(alpha = 0.2f))
                        .border(1.dp, AccentSecondary.copy(alpha = 0.5f), RoundedCornerShape(6.dp))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "Local Model",
                        color = AccentSecondary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(modifier = Modifier.width(6.dp))
                IconButton(onClick = { /* Menu */ }, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Default.MoreVert, contentDescription = "Menu", tint = TextSecondary, modifier = Modifier.size(18.dp))
                }
            }
        }

        // Main Conversation or Greeting Hero Area
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // When no chat messages yet, show Greeting Hero matching Screen 11
            if (messages.isEmpty()) {
                item {
                    Spacer(modifier = Modifier.height(16.dp))
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // Purple/cyan glowing AI icon avatar
                        Box(
                            modifier = Modifier
                                .size(56.dp)
                                .clip(CircleShape)
                                .background(AccentSecondary.copy(alpha = 0.2f))
                                .border(1.5.dp, AccentSecondary, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = null,
                                tint = AccentSecondary,
                                modifier = Modifier.size(28.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        Text(
                            text = "How can I help you today?",
                            color = TextPrimary,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                }

                // Quick Prompt Cards (Screen 11)
                items(quickPrompts) { (promptText, icon) ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .clickable { handleSend(promptText) },
                        colors = CardDefaults.cardColors(containerColor = BgCard),
                        border = androidx.compose.foundation.BorderStroke(1.dp, BorderColor)
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = icon,
                                contentDescription = null,
                                tint = AccentCyan,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = promptText,
                                color = TextPrimary,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            } else {
                items(messages) { msg ->
                    val isUser = msg.sender == MessageSender.USER
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
                    ) {
                        Card(
                            modifier = Modifier.fillMaxWidth(0.9f),
                            colors = CardDefaults.cardColors(
                                containerColor = if (isUser) BgPanelElevated else BgCard
                            ),
                            border = androidx.compose.foundation.BorderStroke(1.dp, BorderColor)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(
                                    text = if (isUser) "You" else "CodeForge AI",
                                    color = if (isUser) AccentSecondary else AccentCyan,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = msg.text,
                                    color = TextPrimary,
                                    fontSize = 13.sp,
                                    lineHeight = 18.sp
                                )

                                msg.proposal?.let { proposal ->
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = "Proposal: ${proposal.file.name}",
                                        color = AccentPrimary,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 11.sp
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))

                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(BgEditor)
                                            .padding(6.dp)
                                    ) {
                                        proposal.diffLines.take(6).forEach { line ->
                                            val color = when (line.type) {
                                                DiffType.ADDED -> StatusSuccess
                                                DiffType.DELETED -> StatusError
                                                DiffType.HEADER -> AccentPrimary
                                                DiffType.UNCHANGED -> TextPrimary
                                            }
                                            Text(line.text, color = color, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(8.dp))
                                    Button(
                                        onClick = {
                                            scope.launch {
                                                fileManager.writeFile(proposal.file, proposal.proposedContent)
                                                messages.add(AiMessage(MessageSender.ASSISTANT, "Applied proposal to ${proposal.file.name}."))
                                            }
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = StatusSuccess)
                                    ) {
                                        Text("Approve & Apply", color = Color.White, fontSize = 11.sp)
                                    }
                                }
                            }
                        }
                    }
                }
            }

            if (isThinking) {
                item {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(8.dp)
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), color = AccentCyan, strokeWidth = 2.dp)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Thinking...", color = TextSecondary, fontSize = 12.sp)
                    }
                }
            }
        }

        // Screen 11 Bottom Chat Bar & Security Note
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(BgPanel)
                .padding(horizontal = 12.dp, vertical = 8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = inputQuery,
                    onValueChange = { inputQuery = it },
                    placeholder = { Text("Ask a question or describe what you need...", color = TextMuted, fontSize = 12.sp) },
                    singleLine = true,
                    modifier = Modifier.weight(1f),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                        focusedBorderColor = AccentPrimary,
                        unfocusedBorderColor = BorderColor,
                        focusedContainerColor = BgCard,
                        unfocusedContainerColor = BgCard
                    )
                )

                Spacer(modifier = Modifier.width(8.dp))

                IconButton(
                    onClick = { handleSend(inputQuery) },
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(AccentPrimary)
                ) {
                    Icon(
                        imageVector = Icons.Default.Send,
                        contentDescription = "Send",
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Footer note: "Your code stays on your device." (Screen 11)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(StatusSuccess)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Your code stays on your device.",
                    color = TextMuted,
                    fontSize = 11.sp
                )
            }
        }
    }
}
