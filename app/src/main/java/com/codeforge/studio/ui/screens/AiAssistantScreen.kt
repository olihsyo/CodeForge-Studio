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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Difference
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.SmartToy
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
import com.codeforge.studio.ui.theme.AccentPrimary
import com.codeforge.studio.ui.theme.AccentSecondary
import com.codeforge.studio.ui.theme.BgApp
import com.codeforge.studio.ui.theme.BgEditor
import com.codeforge.studio.ui.theme.BgPanel
import com.codeforge.studio.ui.theme.BorderColor
import com.codeforge.studio.ui.theme.StatusError
import com.codeforge.studio.ui.theme.StatusSuccess
import com.codeforge.studio.ui.theme.TextPrimary
import com.codeforge.studio.ui.theme.TextSecondary
import com.codeforge.studio.workspace.Project
import kotlinx.coroutines.launch
import java.io.File

@Composable
fun AiAssistantScreen(
    currentProject: Project?,
    aiManager: AiAssistantManager,
    fileManager: FileManager
) {
    val scope = rememberCoroutineScope()
    var inputQuery by remember { mutableStateOf("") }
    var isThinking by remember { mutableStateOf(false) }

    val messages = remember {
        mutableStateListOf(
            AiMessage(
                sender = MessageSender.ASSISTANT,
                text = "Hello! I am your CodeForge AI Assistant. I can explain code, suggest bug fixes, generate tests, and propose file changes with diff review. I operate locally without leaking code."
            )
        )
    }

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
            when {
                query.contains("test", ignoreCase = true) && mainFile != null -> {
                    val code = fileManager.readFile(mainFile).getOrDefault("")
                    val proposal = aiManager.generateTests(code, mainFile.extension, mainFile)
                    messages.add(
                        AiMessage(
                            sender = MessageSender.ASSISTANT,
                            text = "I have generated a test suite for ${mainFile.name}. Review the proposal diff below before applying:",
                            proposal = proposal
                        )
                    )
                }
                query.contains("explain", ignoreCase = true) && mainFile != null -> {
                    val code = fileManager.readFile(mainFile).getOrDefault("")
                    val explanation = aiManager.explainCode(code, mainFile.extension)
                    messages.add(AiMessage(sender = MessageSender.ASSISTANT, text = explanation))
                }
                else -> {
                    messages.add(
                        AiMessage(
                            sender = MessageSender.ASSISTANT,
                            text = "Analyzing workspace '${currentProject?.name ?: "Current"}':\n- Ready for script execution\n- Use commands like 'generate tests' or 'explain code' for guided code reviews."
                        )
                    )
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
        // AI Header Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(BgPanel)
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.SmartToy,
                contentDescription = null,
                tint = AccentPrimary,
                modifier = Modifier.size(22.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Column {
                Text(
                    text = "AI CODING ASSISTANT",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = AccentPrimary
                )
                Text(
                    text = "Local Offline Engine (Safe & Private)",
                    fontSize = 12.sp,
                    color = TextSecondary
                )
            }
        }

        // Message Feed
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(messages) { msg ->
                val isUser = msg.sender == MessageSender.USER
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
                ) {
                    Card(
                        modifier = Modifier.fillMaxWidth(0.9f),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isUser) BgPanel else BgEditor
                        ),
                        border = androidx.compose.foundation.BorderStroke(1.dp, BorderColor)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                text = if (isUser) "You" else "CodeForge AI",
                                color = if (isUser) AccentSecondary else AccentPrimary,
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

                            // Show Proposal Diff if present
                            msg.proposal?.let { proposal ->
                                Spacer(modifier = Modifier.height(10.dp))
                                Text(
                                    text = "Proposed Change: ${proposal.file.name}",
                                    color = AccentPrimary,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 12.sp
                                )
                                Spacer(modifier = Modifier.height(6.dp))

                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(BgPanel)
                                        .padding(6.dp)
                                ) {
                                    proposal.diffLines.take(10).forEach { line ->
                                        val color = when (line.type) {
                                            DiffType.ADDED -> StatusSuccess
                                            DiffType.DELETED -> StatusError
                                            DiffType.HEADER -> AccentPrimary
                                            DiffType.UNCHANGED -> TextPrimary
                                        }
                                        Text(
                                            text = line.text,
                                            color = color,
                                            fontFamily = FontFamily.Monospace,
                                            fontSize = 11.sp
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(10.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.End
                                ) {
                                    Button(
                                        onClick = {
                                            scope.launch {
                                                fileManager.writeFile(proposal.file, proposal.proposedContent)
                                                messages.add(
                                                    AiMessage(
                                                        MessageSender.ASSISTANT,
                                                        "Approved & applied changes to ${proposal.file.name} successfully!"
                                                    )
                                                )
                                            }
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = StatusSuccess)
                                    ) {
                                        Icon(Icons.Default.Check, contentDescription = null, tint = BgApp, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Approve & Apply", color = BgApp, fontSize = 11.sp, fontWeight = FontWeight.Bold)
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
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), color = AccentPrimary, strokeWidth = 2.dp)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Thinking...", color = TextSecondary, fontSize = 12.sp)
                    }
                }
            }
        }

        // Quick Suggestion Prompts
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(BgPanel)
                .padding(horizontal = 8.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            listOf("Explain current file", "Generate unit tests", "Find syntax errors").forEach { prompt ->
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(BgEditor)
                        .border(1.dp, BorderColor, RoundedCornerShape(6.dp))
                        .clickable { handleSend(prompt) }
                        .padding(horizontal = 8.dp, vertical = 5.dp)
                ) {
                    Text(text = prompt, color = TextSecondary, fontSize = 11.sp)
                }
            }
        }

        // Input Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(BgPanel)
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = inputQuery,
                onValueChange = { inputQuery = it },
                placeholder = { Text("Ask AI to explain code, fix errors, or generate tests...", color = TextSecondary, fontSize = 13.sp) },
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
                onClick = { handleSend(inputQuery) },
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(AccentPrimary)
            ) {
                Icon(
                    imageVector = Icons.Default.Send,
                    contentDescription = "Send",
                    tint = BgApp,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}
