package com.codeforge.studio.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
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
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.OpenInBrowser
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.window.Dialog
import com.codeforge.studio.runtimes.LanguageRegistry
import com.codeforge.studio.ui.theme.BorderHighlight
import com.codeforge.studio.ui.theme.StatusWarning
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.codeforge.studio.ui.theme.AccentCyan
import com.codeforge.studio.ui.theme.AccentPrimary
import com.codeforge.studio.ui.theme.BgApp
import com.codeforge.studio.ui.theme.BgCard
import com.codeforge.studio.ui.theme.BgEditor
import com.codeforge.studio.ui.theme.BgPanel
import com.codeforge.studio.ui.theme.BgPanelElevated
import com.codeforge.studio.ui.theme.BorderColor
import com.codeforge.studio.ui.theme.StatusError
import com.codeforge.studio.ui.theme.StatusSuccess
import com.codeforge.studio.ui.theme.TextMuted
import com.codeforge.studio.ui.theme.TextPrimary
import com.codeforge.studio.ui.theme.TextSecondary
import com.codeforge.studio.workspace.WorkspaceManager
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Screen 13: Project Management ("New Project")
 * Fully functional with live creation animations, template selection,
 * smart auto-naming, and responsive sizing.
 */
@Composable
fun NewProjectScreen(
    workspaceManager: WorkspaceManager,
    onProjectCreated: () -> Unit,
    onBack: () -> Unit
) {
    val scope = rememberCoroutineScope()
    var selectedTemplate by remember { mutableStateOf("python") }
    var projectName by remember { mutableStateOf("PythonProject") }
    var activeTab by remember { mutableStateOf(0) } // 0: Templates, 1: Custom
    var isLoading by remember { mutableStateOf(false) }
    var statusText by remember { mutableStateOf<String?>(null) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isSuccess by remember { mutableStateOf(false) }

    val context = LocalContext.current
    var showDownloadGuideDialog by remember { mutableStateOf(false) }

    val currentExt = when (selectedTemplate) {
        "python" -> "py"
        "kotlin" -> "kt"
        "rust" -> "rs"
        "go" -> "go"
        "java" -> "java"
        "cpp" -> "cpp"
        "web" -> "html"
        else -> null
    }
    val currentCap = currentExt?.let { LanguageRegistry.findByExtension(it) }
    val isRuntimeReady = currentCap?.isRuntimeInstalled() ?: true

    val templateList = listOf(
        Triple("python", "Python Project", "Python 3 script / package"),
        Triple("web", "Web Project", "HTML5, CSS3, JavaScript"),
        Triple("kotlin", "Kotlin Project", "Kotlin JVM / Multiplatform"),
        Triple("java", "Java Project", "Standard Java / Gradle"),
        Triple("cpp", "C/C++ Project", "CMake & C++20 standard"),
        Triple("rust", "Rust Project", "Cargo & Rust toolchain"),
        Triple("go", "Go Project", "Go Modules & standard library"),
        Triple("empty", "Empty Project", "Clean blank workspace")
    )

    fun defaultNameFor(template: String): String = when (template.lowercase()) {
        "python" -> "PythonProject"
        "web" -> "WebProject"
        "kotlin" -> "KotlinProject"
        "java" -> "JavaProject"
        "cpp" -> "CppProject"
        "rust" -> "RustProject"
        "go" -> "GoProject"
        else -> "MyProject"
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BgApp)
    ) {
        // Screen 13 Top Bar (Compact, fits all screens)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(BgPanel)
                .padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack, modifier = Modifier.size(36.dp)) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = TextPrimary, modifier = Modifier.size(20.dp))
            }
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = "New Project",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 14.dp, vertical = 10.dp)
        ) {
            // Tabs: Templates | Custom
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(BgEditor)
                    .padding(2.dp)
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(6.dp))
                        .background(if (activeTab == 0) AccentPrimary else Color.Transparent)
                        .clickable { activeTab = 0 }
                        .padding(vertical = 6.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Templates",
                        color = if (activeTab == 0) Color.White else TextSecondary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(6.dp))
                        .background(if (activeTab == 1) AccentPrimary else Color.Transparent)
                        .clickable { activeTab = 1 }
                        .padding(vertical = 6.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Custom",
                        color = if (activeTab == 1) Color.White else TextSecondary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Project Name Input
            OutlinedTextField(
                value = projectName,
                onValueChange = {
                    projectName = it
                    errorMessage = null
                },
                label = { Text("Project Name", fontSize = 11.sp) },
                placeholder = { Text("MyNewProject", color = TextMuted, fontSize = 12.sp) },
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

            Spacer(modifier = Modifier.height(8.dp))

            // Live Animated Creation / Status Card
            AnimatedVisibility(
                visible = isLoading || isSuccess || errorMessage != null,
                enter = fadeIn() + slideInVertically(),
                exit = fadeOut() + slideOutVertically()
            ) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = when {
                            errorMessage != null -> StatusError.copy(alpha = 0.15f)
                            isSuccess -> StatusSuccess.copy(alpha = 0.15f)
                            else -> AccentPrimary.copy(alpha = 0.15f)
                        }
                    ),
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        when {
                            errorMessage != null -> StatusError
                            isSuccess -> StatusSuccess
                            else -> AccentPrimary
                        }
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        when {
                            errorMessage != null -> {
                                Icon(Icons.Default.ErrorOutline, contentDescription = null, tint = StatusError, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(errorMessage ?: "An error occurred", color = StatusError, fontSize = 11.sp, modifier = Modifier.weight(1f))
                            }
                            isSuccess -> {
                                Icon(Icons.Default.CheckCircle, contentDescription = null, tint = StatusSuccess, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(statusText ?: "Project ready! Opening editor...", color = StatusSuccess, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                            }
                            else -> {
                                CircularProgressIndicator(color = AccentPrimary, strokeWidth = 2.dp, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(statusText ?: "Creating project...", color = AccentCyan, fontSize = 11.sp, fontWeight = FontWeight.Medium)
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Template Cards List (Compact, responsive)
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                items(templateList) { (type, name, desc) ->
                    val isSelected = selectedTemplate == type
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .clickable {
                                val prevDefault = defaultNameFor(selectedTemplate)
                                selectedTemplate = type
                                if (projectName.isBlank() || projectName == prevDefault) {
                                    projectName = defaultNameFor(type)
                                }
                            },
                        colors = CardDefaults.cardColors(
                            containerColor = if (isSelected) BgPanelElevated else BgCard
                        ),
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            if (isSelected) AccentPrimary else BorderColor
                        )
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(30.dp)
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(if (isSelected) AccentPrimary.copy(alpha = 0.2f) else BorderColor.copy(alpha = 0.4f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = if (type == "web") Icons.Default.Code else Icons.Default.Folder,
                                    contentDescription = null,
                                    tint = if (isSelected) AccentCyan else TextSecondary,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = name,
                                    color = if (isSelected) AccentCyan else TextPrimary,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = desc,
                                    color = TextSecondary,
                                    fontSize = 10.sp
                                )
                            }
                        }
                    }
                }
            }

            if (!isRuntimeReady && currentCap != null) {
                Spacer(modifier = Modifier.height(6.dp))
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showDownloadGuideDialog = true },
                    colors = CardDefaults.cardColors(containerColor = StatusWarning.copy(alpha = 0.12f)),
                    border = androidx.compose.foundation.BorderStroke(1.dp, StatusWarning.copy(alpha = 0.5f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Warning, contentDescription = null, tint = StatusWarning, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "${currentCap.displayName} runtime not installed",
                                color = StatusWarning,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "You can write code now. Tap to view download guide to execute scripts.",
                                color = TextSecondary,
                                fontSize = 10.sp
                            )
                        }
                        Spacer(modifier = Modifier.width(6.dp))
                        Icon(Icons.Default.Download, contentDescription = null, tint = AccentCyan, modifier = Modifier.size(16.dp))
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Screen 13 Bottom Button: Create Project (Interactive with live feedback)
            Button(
                onClick = {
                    if (!isLoading) {
                        scope.launch {
                            isLoading = true
                            errorMessage = null
                            isSuccess = false
                            statusText = "Creating workspace folder..."
                            delay(180) // Visual feedback step
                            statusText = "Generating $selectedTemplate templates..."

                            val finalName = if (projectName.isBlank()) defaultNameFor(selectedTemplate) else projectName.trim()
                            workspaceManager.createProject(finalName, selectedTemplate, selectedTemplate).onSuccess {
                                statusText = "Workspace ready! Opening..."
                                isSuccess = true
                                delay(350)
                                isLoading = false
                                onProjectCreated()
                            }.onFailure { err ->
                                isLoading = false
                                statusText = null
                                errorMessage = err.localizedMessage ?: "Failed to create project"
                            }
                        }
                    }
                },
                enabled = !isLoading,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp),
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isSuccess) StatusSuccess else AccentPrimary,
                    disabledContainerColor = AccentPrimary.copy(alpha = 0.7f)
                )
            ) {
                if (isLoading) {
                    CircularProgressIndicator(color = Color.White, modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(statusText ?: "Creating...", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                } else if (isSuccess) {
                    Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Project Created!", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                } else {
                    Text("Create Project", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }
            }
        }

        if (showDownloadGuideDialog && currentCap != null) {
            val termuxCmd = when (selectedTemplate) {
                "python" -> "pkg install python"
                "rust" -> "pkg install rust"
                "go" -> "pkg install golang"
                "kotlin" -> "pkg install openjdk-17 kotlin"
                "java" -> "pkg install openjdk-17"
                "cpp" -> "pkg install clang"
                else -> "pkg install ${currentCap.requiredBinaries.firstOrNull() ?: "python"}"
            }
            val officialUrl = when (selectedTemplate) {
                "python" -> "https://www.python.org/downloads/"
                "rust" -> "https://www.rust-lang.org/tools/install"
                "go" -> "https://go.dev/dl/"
                "kotlin" -> "https://kotlinlang.org/"
                "java" -> "https://openjdk.org/"
                "cpp" -> "https://llvm.org/"
                else -> "https://www.python.org/downloads/"
            }
            Dialog(onDismissRequest = { showDownloadGuideDialog = false }) {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = BgPanel,
                    border = androidx.compose.foundation.BorderStroke(1.dp, BorderHighlight),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Download, contentDescription = null, tint = AccentCyan, modifier = Modifier.size(24.dp))
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "Download ${currentCap.displayName}",
                                color = TextPrimary,
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "To run ${currentCap.displayName} code on this device, install the toolchain:",
                            color = TextSecondary,
                            fontSize = 11.sp
                        )

                        Spacer(modifier = Modifier.height(10.dp))
                        Text("Option 1: Install via Termux / Terminal", color = TextPrimary, fontWeight = FontWeight.SemiBold, fontSize = 11.sp)
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(6.dp))
                                .background(BgEditor)
                                .border(1.dp, BorderColor, RoundedCornerShape(6.dp))
                                .padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(text = termuxCmd, color = AccentCyan, fontSize = 11.sp, fontFamily = FontFamily.Monospace, modifier = Modifier.weight(1f))
                            IconButton(
                                onClick = {
                                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                    val clip = ClipData.newPlainText("Command", termuxCmd)
                                    clipboard.setPrimaryClip(clip)
                                    Toast.makeText(context, "Command copied: $termuxCmd", Toast.LENGTH_SHORT).show()
                                },
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(Icons.Default.ContentCopy, contentDescription = "Copy", tint = TextSecondary, modifier = Modifier.size(15.dp))
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))
                        Text("Option 2: Download Official Package", color = TextPrimary, fontWeight = FontWeight.SemiBold, fontSize = 11.sp)
                        Spacer(modifier = Modifier.height(4.dp))
                        Button(
                            onClick = {
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(officialUrl)).apply { flags = Intent.FLAG_ACTIVITY_NEW_TASK }
                                runCatching { context.startActivity(intent) }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = AccentPrimary),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(Icons.Default.OpenInBrowser, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Open Official Download Site", color = Color.White, fontSize = 12.sp)
                        }

                        Spacer(modifier = Modifier.height(10.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                            TextButton(onClick = { showDownloadGuideDialog = false }) {
                                Text("Dismiss", color = TextSecondary)
                            }
                        }
                    }
                }
            }
        }
    }
}
