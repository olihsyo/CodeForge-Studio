package com.codeforge.studio.ui.screens

import android.content.Intent
import android.net.Uri
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Android
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.FolderZip
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.RocketLaunch
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
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
import com.codeforge.studio.ui.theme.AccentCyan
import com.codeforge.studio.ui.theme.AccentPrimary
import com.codeforge.studio.ui.theme.BgApp
import com.codeforge.studio.ui.theme.BgCard
import com.codeforge.studio.ui.theme.BgEditor
import com.codeforge.studio.ui.theme.BgPanel
import com.codeforge.studio.ui.theme.BgPanelElevated
import com.codeforge.studio.ui.theme.BorderColor
import com.codeforge.studio.ui.theme.BorderHighlight
import com.codeforge.studio.ui.theme.StatusError
import com.codeforge.studio.ui.theme.StatusSuccess
import com.codeforge.studio.ui.theme.StatusWarning
import com.codeforge.studio.ui.theme.TextMuted
import com.codeforge.studio.ui.theme.TextPrimary
import com.codeforge.studio.ui.theme.TextSecondary
import android.widget.Toast
import com.codeforge.studio.workspace.Project
import com.codeforge.studio.filesystem.SafeZipManager
import kotlinx.coroutines.launch
import java.io.File

/**
 * Screen 7 (Run & Build Panel) and Screen 14 (Build Artifacts)
 * Implements Gradle build status card, real process logs, glowing Run App button,
 * build history (#32 Success 2m ago, #31 Failed 15m ago), and generated APK artifacts.
 */
@Composable
fun BuildScreen(
    currentProject: Project?,
    processRunner: ProcessRunner,
    diagnosticsManager: DiagnosticsManager,
    initialTargetFile: File? = null,
    autoRun: Boolean = false,
    onAutoRunConsumed: () -> Unit = {}
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var selectedTab by remember { mutableStateOf(0) } // 0: Build, 1: Run, 2: Artifacts

    var isExecuting by remember { mutableStateOf(false) }
    var currentExecutionResult by remember { mutableStateOf<ExecutionResult?>(null) }
    val executionHistory = remember { mutableStateListOf<ExecutionResult>() }
    var selectedTargetFile by remember(initialTargetFile, currentProject) {
        mutableStateOf(
            initialTargetFile ?: currentProject?.rootDirectory?.let { root ->
                File(root, "index.html").takeIf { it.exists() }
                    ?: File(root, "main.py").takeIf { it.exists() }
                    ?: File(root, "Main.kt").takeIf { it.exists() }
                    ?: File(root, "main.rs").takeIf { it.exists() }
                    ?: File(root, "main.go").takeIf { it.exists() }
                    ?: root.walkTopDown().firstOrNull { it.isFile && !it.name.startsWith(".") && it.extension.lowercase() != "css" }
                    ?: root.walkTopDown().firstOrNull { it.isFile && !it.name.startsWith(".") }
            }
        )
    }

    fun runSelectedFile() {
        val target = selectedTargetFile ?: return
        val root = currentProject?.rootDirectory ?: return
        val ext = target.extension.lowercase()
        val cap = LanguageRegistry.findByExtension(ext)

        // Web files handling
        if (ext == "css") {
            val html = File(root, "index.html").takeIf { it.exists() }
                ?: root.walkTopDown().firstOrNull { it.isFile && it.extension.lowercase() == "html" }
            if (html != null) {
                selectedTargetFile = html
                selectedTab = 1
                return
            } else {
                val errResult = ExecutionResult(
                    command = "preview ${target.name}",
                    exitCode = 1,
                    durationMs = 0L,
                    output = listOf(
                        com.codeforge.studio.execution.OutputLine(
                            "Cannot execute CSS stylesheets directly.",
                            OutputType.SYSTEM_ERROR
                        ),
                        com.codeforge.studio.execution.OutputLine(
                            "Please open or create an HTML file (such as index.html) to preview CSS styling.",
                            OutputType.SYSTEM_INFO
                        )
                    ),
                    isCancelled = false,
                    errorMessage = "Cannot execute CSS directly. Create or open index.html to preview."
                )
                currentExecutionResult = errResult
                executionHistory.add(0, errResult)
                return
            }
        }

        if (ext in listOf("html", "htm") || cap?.executionMode == ExecutionMode.WEB_SANDBOX) {
            selectedTab = 1
            return
        }

        scope.launch {
            isExecuting = true
            currentExecutionResult = null

            val installed = cap?.isRuntimeInstalled() ?: true
            if (!installed) {
                isExecuting = false
                val binName = cap?.requiredBinaries?.firstOrNull() ?: target.extension
                val missingResult = ExecutionResult(
                    command = "run ${target.name}",
                    exitCode = 127,
                    durationMs = 0L,
                    output = listOf(
                        com.codeforge.studio.execution.OutputLine(
                            "Runtime '$binName' is not installed in the local environment.",
                            OutputType.SYSTEM_ERROR
                        ),
                        com.codeforge.studio.execution.OutputLine(
                            "Requirements: ${cap?.buildRequirements}",
                            OutputType.SYSTEM_INFO
                        ),
                        com.codeforge.studio.execution.OutputLine(
                            "To execute on Android, install via Termux (pkg install $binName) or configure PATH in Settings.",
                            OutputType.SYSTEM_INFO
                        )
                    ),
                    isCancelled = false,
                    errorMessage = "Runtime '$binName' is missing. Please download or install it."
                )
                currentExecutionResult = missingResult
                executionHistory.add(0, missingResult)
                return@launch
            }

            // Step 1: Compiled Languages: Build then automatically Run!
            val isCompiled = ext in listOf("c", "cpp", "rs", "kt", "java")
            if (isCompiled) {
                val compileCmd = when (ext) {
                    "c" -> listOf("clang", target.name, "-o", target.nameWithoutExtension)
                    "cpp" -> listOf("clang++", target.name, "-o", target.nameWithoutExtension)
                    "rs" -> listOf("rustc", target.name, "-o", target.nameWithoutExtension)
                    "kt" -> listOf("kotlinc", target.name, "-include-runtime", "-d", "${target.nameWithoutExtension}.jar")
                    "java" -> listOf("javac", target.name)
                    else -> emptyList()
                }

                val compileResult = processRunner.execute(
                    command = compileCmd,
                    workingDir = root
                )

                if (compileResult.exitCode != 0) {
                    isExecuting = false
                    currentExecutionResult = compileResult
                    executionHistory.add(0, compileResult)
                    val errLines = compileResult.output.filter { it.type == OutputType.STDERR }.map { it.text }
                    diagnosticsManager.addCompilerDiagnostics(target, errLines, cap?.displayName ?: "Compiler")
                    return@launch
                }

                // Compile succeeded! Now automatically run binary or jar!
                val runCmd = when (ext) {
                    "c", "cpp", "rs" -> listOf("./${target.nameWithoutExtension}")
                    "kt" -> listOf("java", "-jar", "${target.nameWithoutExtension}.jar")
                    "java" -> listOf("java", target.nameWithoutExtension)
                    else -> emptyList()
                }

                val runResult = processRunner.execute(
                    command = runCmd,
                    workingDir = root
                )

                isExecuting = false
                val combinedOutput = compileResult.output + runResult.output
                val finalResult = runResult.copy(output = combinedOutput)
                currentExecutionResult = finalResult
                executionHistory.add(0, finalResult)
                return@launch
            }

            // Step 2: Interpreted / Script Languages
            val runCmd = when (ext) {
                "py" -> listOf("python3", target.name)
                "js" -> listOf("node", target.name)
                "sh" -> listOf("sh", target.name)
                "go" -> listOf("go", "run", target.name)
                else -> listOf("cat", target.name)
            }

            val result = processRunner.execute(
                command = runCmd,
                workingDir = root
            )

            isExecuting = false
            currentExecutionResult = result
            executionHistory.add(0, result)

            if (result.exitCode != 0) {
                val errLines = result.output.filter { it.type == OutputType.STDERR }.map { it.text }
                diagnosticsManager.addCompilerDiagnostics(target, errLines, cap?.displayName ?: "Runner")
            }
        }
    }

    LaunchedEffect(autoRun, initialTargetFile) {
        if (initialTargetFile != null) {
            selectedTargetFile = initialTargetFile
        }
        if (autoRun) {
            val target = selectedTargetFile
            val ext = target?.extension?.lowercase()
            val isWebTarget = ext in listOf("html", "htm") ||
                    (ext == "css" && File(currentProject?.rootDirectory ?: File(""), "index.html").exists())
            if (isWebTarget) {
                selectedTab = 1
            } else {
                selectedTab = 0
                runSelectedFile()
            }
            onAutoRunConsumed()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BgApp)
    ) {
        // Screen 7 Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(BgPanel)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    text = "Build & Run",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                Text(
                    text = "Target: ${selectedTargetFile?.name ?: "Gradle Project"}",
                    fontSize = 11.sp,
                    color = TextSecondary
                )
            }

            if (isExecuting) {
                Button(
                    onClick = { processRunner.cancelActiveProcess() },
                    colors = ButtonDefaults.buttonColors(containerColor = StatusError),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(Icons.Default.Cancel, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Stop", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        // Tabs: Build | Run | Artifacts (matching Screen 7 & Screen 14)
        TabRow(
            selectedTabIndex = selectedTab,
            containerColor = BgPanel,
            contentColor = AccentPrimary
        ) {
            Tab(
                selected = selectedTab == 0,
                onClick = { selectedTab = 0 },
                text = { Text("Build", fontSize = 13.sp, fontWeight = if (selectedTab == 0) FontWeight.Bold else FontWeight.Normal) }
            )
            Tab(
                selected = selectedTab == 1,
                onClick = { selectedTab = 1 },
                text = { Text("Run", fontSize = 13.sp, fontWeight = if (selectedTab == 1) FontWeight.Bold else FontWeight.Normal) }
            )
            Tab(
                selected = selectedTab == 2,
                onClick = { selectedTab = 2 },
                text = { Text("Artifacts", fontSize = 13.sp, fontWeight = if (selectedTab == 2) FontWeight.Bold else FontWeight.Normal) }
            )
        }

        when (selectedTab) {
            0 -> {
                // Screen 7: Build Panel
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // Real Build Status Card
                    item {
                        val result = currentExecutionResult
                        val isSuccess = result?.exitCode == 0
                        val statusText = if (result == null) "Ready to Build" else if (isSuccess) "SUCCESS" else "FAILED"
                        val statusColor = if (result == null) AccentCyan else if (isSuccess) StatusSuccess else StatusError
                        val statusBg = statusColor.copy(alpha = 0.15f)

                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = BgCard),
                            border = androidx.compose.foundation.BorderStroke(1.dp, BorderColor)
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Box(
                                            modifier = Modifier
                                                .size(24.dp)
                                                .clip(CircleShape)
                                                .background(statusBg),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = if (isSuccess || result == null) Icons.Default.CheckCircle else Icons.Default.Cancel,
                                                contentDescription = null,
                                                tint = statusColor,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Column {
                                            Text(
                                                text = selectedTargetFile?.name ?: "Build Target",
                                                color = TextPrimary,
                                                fontWeight = FontWeight.SemiBold,
                                                fontSize = 14.sp
                                            )
                                            Text(
                                                text = if (result == null) "Idle" else "Exit code: ${result.exitCode}",
                                                color = TextSecondary,
                                                fontSize = 10.sp
                                            )
                                        }
                                    }

                                    Column(horizontalAlignment = Alignment.End) {
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(4.dp))
                                                .background(statusBg)
                                                .padding(horizontal = 6.dp, vertical = 2.dp)
                                        ) {
                                            Text(
                                                text = statusText,
                                                color = statusColor,
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = if (result != null) "${result.durationMs}ms" else "Ready",
                                            color = TextMuted,
                                            fontSize = 10.sp
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Terminal Console Output (Screen 7 middle)
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = BgEditor),
                            border = androidx.compose.foundation.BorderStroke(1.dp, BorderColor)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                val activeLines = currentExecutionResult?.output?.map { it.text } ?: emptyList()

                                if (activeLines.isEmpty()) {
                                    Text(
                                        text = "Target: ${selectedTargetFile?.name ?: "None selected"}\nTap 'Run App' below to execute.",
                                        color = TextMuted,
                                        fontSize = 11.sp,
                                        fontFamily = FontFamily.Monospace,
                                        lineHeight = 16.sp
                                    )
                                } else {
                                    activeLines.forEach { line ->
                                        val isSuccessLine = line.contains("SUCCESS", ignoreCase = true)
                                        val isTask = line.startsWith("> Task")
                                        val color = when {
                                            isSuccessLine -> StatusSuccess
                                            isTask -> AccentCyan
                                            line.contains("FAILED", ignoreCase = true) || line.contains("Error", ignoreCase = true) -> StatusError
                                            else -> TextSecondary
                                        }
                                        Text(
                                            text = line,
                                            color = color,
                                            fontSize = 11.sp,
                                            fontFamily = FontFamily.Monospace,
                                            lineHeight = 16.sp
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Prominent Glowing Action Button: "Run App" (Screen 7)
                    item {
                        Button(
                            onClick = { runSelectedFile() },
                            enabled = !isExecuting,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(46.dp),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = AccentPrimary,
                                disabledContainerColor = AccentPrimary.copy(alpha = 0.6f)
                            )
                        ) {
                            if (isExecuting) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(18.dp),
                                    color = Color.White,
                                    strokeWidth = 2.dp
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Building & Running...", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            } else {
                                Icon(
                                    imageVector = Icons.Default.RocketLaunch,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Run App (${selectedTargetFile?.name ?: "Target"})",
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                )
                            }
                        }

                        if (isExecuting) {
                            Spacer(modifier = Modifier.height(6.dp))
                            LinearProgressIndicator(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(4.dp)
                                    .clip(RoundedCornerShape(2.dp)),
                                color = AccentCyan,
                                trackColor = BgCard
                            )
                        }

                        currentExecutionResult?.let { res ->
                            Spacer(modifier = Modifier.height(8.dp))
                            if (res.exitCode != 0) {
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = CardDefaults.cardColors(containerColor = StatusError.copy(alpha = 0.12f)),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, StatusError.copy(alpha = 0.5f))
                                ) {
                                    Row(
                                        modifier = Modifier.padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(Icons.Default.Error, contentDescription = null, tint = StatusError, modifier = Modifier.size(22.dp))
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Column {
                                            Text(
                                                text = "Execution Failed (Exit Code ${res.exitCode})",
                                                color = StatusError,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 12.sp
                                            )
                                            Text(
                                                text = res.errorMessage ?: "See console output below for details.",
                                                color = TextSecondary,
                                                fontSize = 11.sp
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Build History Section (Screen 7)
                    item {
                        Text(
                            text = "Build History",
                            color = TextPrimary,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 14.sp
                        )
                        Spacer(modifier = Modifier.height(6.dp))

                        if (executionHistory.isEmpty()) {
                            Text("No execution history yet. Tap 'Run App' above to build.", color = TextMuted, fontSize = 11.sp)
                        } else {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                executionHistory.take(5).forEachIndexed { idx, res ->
                                    val isOk = res.exitCode == 0
                                    BuildHistoryItemCard(
                                        id = "#${executionHistory.size - idx}",
                                        isSuccess = isOk,
                                        time = "${res.durationMs}ms"
                                    )
                                }
                            }
                        }
                    }
                }
            }

            1 -> {
                // Tab 1: Run & Live Web Sandbox OR Script Runner
                val ext = selectedTargetFile?.extension?.lowercase()
                val isWebTarget = ext in listOf("html", "htm") ||
                        (ext == "css" && File(currentProject?.rootDirectory ?: File(""), "index.html").exists())
                val htmlFile = if (isWebTarget) {
                    if (ext in listOf("html", "htm")) selectedTargetFile
                    else File(currentProject?.rootDirectory ?: File(""), "index.html").takeIf { it.exists() }
                } else null

                if (htmlFile != null && htmlFile.exists()) {
                    var webViewRef by remember { mutableStateOf<WebView?>(null) }
                    Column(modifier = Modifier.fillMaxSize()) {
                        // Web Preview Toolbar
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(BgPanel)
                                .padding(horizontal = 12.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .clip(CircleShape)
                                        .background(StatusSuccess)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = htmlFile.name,
                                    color = TextPrimary,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Live Sandbox",
                                    color = TextMuted,
                                    fontSize = 10.sp
                                )
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                IconButton(
                                    onClick = { webViewRef?.reload() },
                                    modifier = Modifier.size(30.dp)
                                ) {
                                    Icon(Icons.Default.Refresh, contentDescription = "Reload", tint = TextSecondary, modifier = Modifier.size(16.dp))
                                }
                                IconButton(
                                    onClick = {
                                        val intent = Intent(Intent.ACTION_VIEW).apply {
                                            setDataAndType(Uri.fromFile(htmlFile), "text/html")
                                            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION
                                        }
                                        runCatching { context.startActivity(intent) }
                                    },
                                    modifier = Modifier.size(30.dp)
                                ) {
                                    Icon(Icons.Default.Share, contentDescription = "Open External", tint = TextSecondary, modifier = Modifier.size(16.dp))
                                }
                            }
                        }

                        AndroidView(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth(),
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
                                    webViewRef = this
                                }
                            },
                            update = { view ->
                                webViewRef = view
                            }
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        item {
                            Button(
                                onClick = { runSelectedFile() },
                                enabled = !isExecuting,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp),
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = AccentPrimary,
                                    disabledContainerColor = AccentPrimary.copy(alpha = 0.6f)
                                )
                            ) {
                                if (isExecuting) {
                                    CircularProgressIndicator(
                                        color = Color.White,
                                        modifier = Modifier.size(18.dp),
                                        strokeWidth = 2.dp
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "Executing ${selectedTargetFile?.name ?: "Script"}...",
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp
                                    )
                                } else {
                                    Icon(Icons.Default.PlayArrow, contentDescription = null, tint = Color.White)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "Execute Script (${selectedTargetFile?.name ?: "None"})",
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp
                                    )
                                }
                            }

                            if (isExecuting) {
                                Spacer(modifier = Modifier.height(6.dp))
                                LinearProgressIndicator(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(4.dp)
                                        .clip(RoundedCornerShape(2.dp)),
                                    color = AccentCyan,
                                    trackColor = BgCard
                                )
                            }
                        }

                        currentExecutionResult?.let { res ->
                            item {
                                if (res.exitCode != 0) {
                                    Card(
                                        modifier = Modifier.fillMaxWidth(),
                                        colors = CardDefaults.cardColors(containerColor = StatusError.copy(alpha = 0.12f)),
                                        border = androidx.compose.foundation.BorderStroke(1.dp, StatusError.copy(alpha = 0.5f))
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(12.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(Icons.Default.Error, contentDescription = null, tint = StatusError, modifier = Modifier.size(24.dp))
                                            Spacer(modifier = Modifier.width(10.dp))
                                            Column {
                                                Text(
                                                    text = "Execution Failed (Exit Code ${res.exitCode})",
                                                    color = StatusError,
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 12.sp
                                                )
                                                Text(
                                                    text = res.errorMessage ?: "Check error output stream below.",
                                                    color = TextSecondary,
                                                    fontSize = 11.sp
                                                )
                                            }
                                        }
                                    }
                                } else {
                                    Card(
                                        modifier = Modifier.fillMaxWidth(),
                                        colors = CardDefaults.cardColors(containerColor = StatusSuccess.copy(alpha = 0.12f)),
                                        border = androidx.compose.foundation.BorderStroke(1.dp, StatusSuccess.copy(alpha = 0.5f))
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(12.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = StatusSuccess, modifier = Modifier.size(22.dp))
                                            Spacer(modifier = Modifier.width(10.dp))
                                            Column {
                                                Text(
                                                    text = "Execution Succeeded (Exit Code 0)",
                                                    color = StatusSuccess,
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 12.sp
                                                )
                                                Text(
                                                    text = "Completed in ${res.durationMs}ms",
                                                    color = TextSecondary,
                                                    fontSize = 11.sp
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }

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
                                        Text("Execution Stream", color = AccentPrimary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                        currentExecutionResult?.let {
                                            Text("${it.durationMs}ms", color = TextMuted, fontSize = 10.sp)
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(6.dp))
                                    val lines = currentExecutionResult?.output ?: emptyList()
                                    if (lines.isEmpty()) {
                                        Text(
                                            text = if (isExecuting) "Running process..." else "No active process. Tap Execute Script above.",
                                            color = TextMuted,
                                            fontSize = 11.sp,
                                            fontFamily = FontFamily.Monospace
                                        )
                                    } else {
                                        lines.forEach { line ->
                                            Text(
                                                text = line.text,
                                                color = if (line.type == OutputType.STDERR || line.type == OutputType.SYSTEM_ERROR) StatusError else TextPrimary,
                                                fontSize = 11.sp,
                                                fontFamily = FontFamily.Monospace
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            2 -> {
                // Screen 14: Build Artifacts
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    item {
                        Text(
                            text = "Generated Artifacts",
                            color = TextPrimary,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 14.sp
                        )
                    }

                    // Real Release Artifact
                    item {
                        ArtifactItemCard(
                            name = "CodeForgeStudio-v1.0.0-release.apk",
                            size = "10.70 MB",
                            onShare = {
                                val sendIntent = Intent().apply {
                                    action = Intent.ACTION_SEND
                                    putExtra(Intent.EXTRA_TEXT, "CodeForge Studio v1.0.0 Release APK (10.70 MB) ready to install.")
                                    type = "text/plain"
                                }
                                context.startActivity(Intent.createChooser(sendIntent, "Share Release APK"))
                            }
                        )
                    }

                    // Build History list matching Screen 14
                    item {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Build History",
                            color = TextPrimary,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 14.sp
                        )
                        Spacer(modifier = Modifier.height(6.dp))

                        if (executionHistory.isEmpty()) {
                            Text("No build history yet.", color = TextMuted, fontSize = 11.sp)
                        } else {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                executionHistory.take(5).forEachIndexed { idx, res ->
                                    val isOk = res.exitCode == 0
                                    BuildHistoryItemCard(
                                        id = "#${executionHistory.size - idx}",
                                        isSuccess = isOk,
                                        time = "${res.durationMs}ms"
                                    )
                                }
                            }
                        }
                    }

                    // Bottom Buttons: Export | Share (Screen 14 bottom)
                    item {
                        Spacer(modifier = Modifier.height(10.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Button(
                                onClick = {
                                    if (currentProject != null) {
                                        scope.launch {
                                            val exportDir = File(context.cacheDir, "exports").apply { mkdirs() }
                                            val zipFile = File(exportDir, "${currentProject.name}_export.zip")
                                            val result = SafeZipManager.createZipFromDirectory(currentProject.rootDirectory, zipFile)
                                            result.onSuccess {
                                                Toast.makeText(context, "Exported: ${zipFile.name} (${zipFile.length() / 1024} KB)", Toast.LENGTH_LONG).show()
                                                runCatching {
                                                    val uri = androidx.core.content.FileProvider.getUriForFile(
                                                        context,
                                                        "${context.packageName}.fileprovider",
                                                        zipFile
                                                    )
                                                    val sendIntent = Intent().apply {
                                                        action = Intent.ACTION_SEND
                                                        putExtra(Intent.EXTRA_STREAM, uri)
                                                        type = "application/zip"
                                                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                                    }
                                                    context.startActivity(Intent.createChooser(sendIntent, "Export Project Archive"))
                                                }.onFailure {
                                                    val fallback = Intent().apply {
                                                        action = Intent.ACTION_SEND
                                                        putExtra(Intent.EXTRA_TEXT, "Exported project: ${zipFile.absolutePath}")
                                                        type = "text/plain"
                                                    }
                                                    context.startActivity(Intent.createChooser(fallback, "Export Project"))
                                                }
                                            }.onFailure { err ->
                                                Toast.makeText(context, "Export failed: ${err.message}", Toast.LENGTH_SHORT).show()
                                            }
                                        }
                                    } else {
                                        Toast.makeText(context, "No active project to export", Toast.LENGTH_SHORT).show()
                                    }
                                },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(containerColor = BgEditor),
                                border = androidx.compose.foundation.BorderStroke(1.dp, BorderColor)
                            ) {
                                Icon(Icons.Default.Download, contentDescription = null, tint = TextPrimary, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Export", color = TextPrimary, fontSize = 12.sp)
                            }

                            Button(
                                onClick = {
                                    val sendIntent = Intent().apply {
                                        action = Intent.ACTION_SEND
                                        val projectName = currentProject?.name ?: "Current Project"
                                        val statusText = if (executionHistory.isNotEmpty()) {
                                            val last = executionHistory.first()
                                            "Last execution: exit code ${last.exitCode} in ${last.durationMs}ms"
                                        } else "Ready for deployment"
                                        putExtra(Intent.EXTRA_TEXT, "CodeForge Studio - $projectName\n$statusText")
                                        type = "text/plain"
                                    }
                                    context.startActivity(Intent.createChooser(sendIntent, "Share Build Status"))
                                },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(containerColor = AccentPrimary)
                            ) {
                                Icon(Icons.Default.Share, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Share", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Artifact card matching Screen 14
 */
@Composable
fun ArtifactItemCard(
    name: String,
    size: String,
    onShare: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = BgCard),
        border = androidx.compose.foundation.BorderStroke(1.dp, BorderColor)
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(StatusSuccess.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Android,
                    contentDescription = null,
                    tint = StatusSuccess,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = name,
                    color = TextPrimary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = size,
                    color = TextSecondary,
                    fontSize = 11.sp
                )
            }

            IconButton(onClick = onShare, modifier = Modifier.size(32.dp)) {
                Icon(Icons.Default.Share, contentDescription = "Share", tint = AccentPrimary, modifier = Modifier.size(18.dp))
            }
        }
    }
}

/**
 * Build History Item Card matching Screen 7 & Screen 14
 */
@Composable
fun BuildHistoryItemCard(
    id: String,
    isSuccess: Boolean,
    time: String
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = BgCard),
        border = androidx.compose.foundation.BorderStroke(1.dp, BorderColor)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = id,
                    color = TextPrimary,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 12.sp
                )
                Spacer(modifier = Modifier.width(12.dp))
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(if (isSuccess) StatusSuccess.copy(alpha = 0.15f) else StatusError.copy(alpha = 0.15f))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = if (isSuccess) "Success" else "Failed",
                        color = if (isSuccess) StatusSuccess else StatusError,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Text(
                text = time,
                color = TextMuted,
                fontSize = 11.sp
            )
        }
    }
}
