package com.codeforge.studio.ui.screens

import android.view.ViewGroup
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
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
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
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
import androidx.compose.ui.viewinterop.AndroidView
import com.codeforge.studio.diagnostics.DiagnosticsManager
import com.codeforge.studio.execution.ExecutionResult
import com.codeforge.studio.execution.OutputType
import com.codeforge.studio.execution.ProcessRunner
import com.codeforge.studio.runtimes.ExecutionMode
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
import com.codeforge.studio.workspace.Project
import kotlinx.coroutines.launch
import java.io.File

@Composable
fun BuildScreen(
    currentProject: Project?,
    processRunner: ProcessRunner,
    diagnosticsManager: DiagnosticsManager
) {
    val scope = rememberCoroutineScope()
    var selectedTab by remember { mutableStateOf(0) } // 0: Output & History, 1: Live Web Preview

    var isExecuting by remember { mutableStateOf(false) }
    var currentExecutionResult by remember { mutableStateOf<ExecutionResult?>(null) }
    val executionHistory = remember { mutableStateListOf<ExecutionResult>() }
    var selectedTargetFile by remember {
        mutableStateOf(
            currentProject?.rootDirectory?.let { root ->
                File(root, "main.py").takeIf { it.exists() }
                    ?: File(root, "index.html").takeIf { it.exists() }
                    ?: root.walkTopDown().firstOrNull { it.isFile && !it.name.startsWith(".") }
            }
        )
    }

    fun runSelectedFile() {
        val target = selectedTargetFile ?: return
        val root = currentProject?.rootDirectory ?: return
        val cap = LanguageRegistry.findByExtension(target.extension)

        if (cap?.executionMode == ExecutionMode.WEB_SANDBOX) {
            selectedTab = 1 // Switch directly to live web preview
            return
        }

        scope.launch {
            isExecuting = true
            val command = when (target.extension.lowercase()) {
                "py" -> listOf("python3", target.name)
                "js" -> listOf("node", target.name)
                "sh" -> listOf("sh", target.name)
                "kt" -> listOf("kotlinc", target.name, "-include-runtime", "-d", "${target.nameWithoutExtension}.jar")
                "c" -> listOf("clang", target.name, "-o", target.nameWithoutExtension)
                "cpp" -> listOf("clang++", target.name, "-o", target.nameWithoutExtension)
                "rs" -> listOf("rustc", target.name)
                "go" -> listOf("go", "run", target.name)
                else -> listOf("cat", target.name)
            }

            // Check runtime availability
            val installed = cap?.isRuntimeInstalled() ?: true
            if (!installed) {
                isExecuting = false
                val missingResult = ExecutionResult(
                    command = command.joinToString(" "),
                    exitCode = 127,
                    durationMs = 0L,
                    output = listOf(
                        com.codeforge.studio.execution.OutputLine(
                            "Runtime Not Found: '${cap?.requiredBinaries?.firstOrNull()}' is not installed in the Android environment.",
                            OutputType.SYSTEM_ERROR
                        ),
                        com.codeforge.studio.execution.OutputLine(
                            "Requirements: ${cap?.buildRequirements}",
                            OutputType.SYSTEM_INFO
                        ),
                        com.codeforge.studio.execution.OutputLine(
                            "Limitations: ${cap?.platformLimitations}",
                            OutputType.SYSTEM_INFO
                        )
                    ),
                    isCancelled = false,
                    errorMessage = "Missing Runtime"
                )
                currentExecutionResult = missingResult
                executionHistory.add(0, missingResult)
                return@launch
            }

            val result = processRunner.execute(
                command = command,
                workingDir = root
            )

            isExecuting = false
            currentExecutionResult = result
            executionHistory.add(0, result)

            // Extract diagnostics if failed
            if (result.exitCode != 0) {
                val errLines = result.output.filter { it.type == OutputType.STDERR }.map { it.text }
                diagnosticsManager.addCompilerDiagnostics(target, errLines, cap?.displayName ?: "Compiler")
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BgApp)
    ) {
        // Run Action Header Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(BgPanel)
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "RUN & BUILD CONTROLLER",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = AccentPrimary
                )
                Text(
                    text = "Target: ${selectedTargetFile?.name ?: "No file selected"}",
                    fontSize = 14.sp,
                    color = TextPrimary,
                    fontWeight = FontWeight.Medium
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (isExecuting) {
                    Button(
                        onClick = { processRunner.cancelActiveProcess() },
                        colors = ButtonDefaults.buttonColors(containerColor = StatusError)
                    ) {
                        Icon(Icons.Default.Cancel, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Stop", color = TextPrimary, fontSize = 12.sp)
                    }
                } else {
                    Button(
                        onClick = { runSelectedFile() },
                        colors = ButtonDefaults.buttonColors(containerColor = StatusSuccess),
                        enabled = selectedTargetFile != null
                    ) {
                        Icon(Icons.Default.PlayArrow, contentDescription = null, tint = BgApp, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Run", color = BgApp, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                }
            }
        }

        // Tabs: Console vs Live Preview
        TabRow(
            selectedTabIndex = selectedTab,
            containerColor = BgPanel,
            contentColor = AccentPrimary
        ) {
            Tab(
                selected = selectedTab == 0,
                onClick = { selectedTab = 0 },
                text = { Text("Output Log", fontSize = 13.sp) }
            )
            Tab(
                selected = selectedTab == 1,
                onClick = { selectedTab = 1 },
                text = { Text("Web Live Preview", fontSize = 13.sp) }
            )
        }

        if (selectedTab == 0) {
            // Execution Console Output
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Latest Output Box
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = BgEditor),
                        border = androidx.compose.foundation.BorderStroke(1.dp, BorderColor)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Execution Stream",
                                    color = TextSecondary,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                if (currentExecutionResult != null) {
                                    val isOk = currentExecutionResult?.isSuccess == true
                                    Text(
                                        text = if (isOk) "EXIT 0 (SUCCESS)" else "EXIT ${currentExecutionResult?.exitCode} (FAILED)",
                                        color = if (isOk) StatusSuccess else StatusError,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            if (isExecuting) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(vertical = 8.dp)
                                ) {
                                    CircularProgressIndicator(modifier = Modifier.size(16.dp), color = AccentPrimary, strokeWidth = 2.dp)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Process running...", color = AccentPrimary, fontSize = 12.sp)
                                }
                            }

                            val lines = currentExecutionResult?.output ?: emptyList()
                            if (lines.isEmpty() && !isExecuting) {
                                Text(
                                    text = "Ready to execute. Press 'Run' above to run ${selectedTargetFile?.name}.",
                                    color = TextSecondary,
                                    fontSize = 12.sp,
                                    fontFamily = FontFamily.Monospace
                                )
                            } else {
                                lines.forEach { line ->
                                    val color = when (line.type) {
                                        OutputType.STDOUT -> TextPrimary
                                        OutputType.STDERR -> StatusError
                                        OutputType.SYSTEM_INFO -> AccentPrimary
                                        OutputType.SYSTEM_ERROR -> StatusError
                                    }
                                    Text(
                                        text = line.text,
                                        color = color,
                                        fontSize = 12.sp,
                                        fontFamily = FontFamily.Monospace
                                    )
                                }
                            }
                        }
                    }
                }

                // Execution History Section
                if (executionHistory.isNotEmpty()) {
                    item {
                        Text(
                            text = "Build History (${executionHistory.size})",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = TextPrimary
                        )
                    }

                    items(executionHistory) { hist ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { currentExecutionResult = hist },
                            colors = CardDefaults.cardColors(containerColor = BgPanel),
                            border = androidx.compose.foundation.BorderStroke(1.dp, BorderColor)
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = if (hist.isSuccess) Icons.Default.CheckCircle else Icons.Default.Error,
                                    contentDescription = null,
                                    tint = if (hist.isSuccess) StatusSuccess else StatusError,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(hist.command, color = TextPrimary, fontSize = 12.sp, fontWeight = FontWeight.Medium, maxLines = 1)
                                    Text("Exit Code: ${hist.exitCode} • ${hist.durationMs}ms", color = TextSecondary, fontSize = 10.sp)
                                }
                            }
                        }
                    }
                }
            }
        } else {
            // Live Web Preview Sandbox
            val htmlFile = remember(currentProject) {
                currentProject?.rootDirectory?.let { root ->
                    File(root, "index.html").takeIf { it.exists() }
                }
            }

            if (htmlFile != null && htmlFile.exists()) {
                AndroidView(
                    modifier = Modifier.fillMaxSize(),
                    factory = { ctx ->
                        WebView(ctx).apply {
                            layoutParams = ViewGroup.LayoutParams(
                                ViewGroup.LayoutParams.MATCH_PARENT,
                                ViewGroup.LayoutParams.MATCH_PARENT
                            )
                            settings.apply {
                                javaScriptEnabled = true
                                domStorageEnabled = true
                                allowFileAccess = true
                                allowContentAccess = true
                            }
                            webViewClient = WebViewClient()
                            loadUrl("file://${htmlFile.absolutePath}")
                        }
                    }
                )
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No 'index.html' file found in current workspace. Create an HTML file or use the Web starter template to activate live web preview.",
                        color = TextSecondary,
                        fontSize = 13.sp
                    )
                }
            }
        }
    }
}
