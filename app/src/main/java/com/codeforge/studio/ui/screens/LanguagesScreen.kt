package com.codeforge.studio.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
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
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.OpenInBrowser
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.ui.window.Dialog
import com.codeforge.studio.runtimes.LanguageRegistry
import com.codeforge.studio.ui.theme.AccentCyan
import com.codeforge.studio.ui.theme.AccentPrimary
import com.codeforge.studio.ui.theme.BgApp
import com.codeforge.studio.ui.theme.BgCard
import com.codeforge.studio.ui.theme.BgEditor
import com.codeforge.studio.ui.theme.BgPanel
import com.codeforge.studio.ui.theme.BorderColor
import com.codeforge.studio.ui.theme.BorderHighlight
import com.codeforge.studio.ui.theme.StatusError
import com.codeforge.studio.ui.theme.StatusSuccess
import com.codeforge.studio.ui.theme.StatusWarning
import com.codeforge.studio.ui.theme.TextMuted
import com.codeforge.studio.ui.theme.TextPrimary
import com.codeforge.studio.ui.theme.TextSecondary

data class LanguageToolchainInfo(
    val id: String,
    val name: String,
    val version: String,
    val badge: String,
    val badgeBg: Color,
    val badgeTextColor: Color,
    val ext: String,
    val isInstalled: Boolean,
    val status: String,
    val statusColor: Color,
    val termuxCommand: String,
    val downloadUrl: String,
    val description: String
)

/**
 * Screen 12: Language Toolchains & Download Guides
 * Live detects installed runtimes and provides setup/download instructions for missing toolchains.
 */
@Composable
fun LanguagesScreen() {
    val context = LocalContext.current
    var searchQuery by remember { mutableStateOf("") }
    var isSearchOpen by remember { mutableStateOf(false) }
    var selectedLanguageForDialog by remember { mutableStateOf<LanguageToolchainInfo?>(null) }

    fun checkInstalled(ext: String, isBuiltIn: Boolean = false): Boolean {
        if (isBuiltIn) return true
        val cap = LanguageRegistry.findByExtension(ext)
        return cap?.isRuntimeInstalled() ?: false
    }

    val pyInstalled = checkInstalled("py")
    val tsInstalled = checkInstalled("ts")
    val ktInstalled = checkInstalled("kt")
    val javaInstalled = checkInstalled("java")
    val cppInstalled = checkInstalled("cpp")
    val rustInstalled = checkInstalled("rs")
    val goInstalled = checkInstalled("go")

    val languages = remember(pyInstalled, tsInstalled, ktInstalled, javaInstalled, cppInstalled, rustInstalled, goInstalled) {
        listOf(
            LanguageToolchainInfo(
                id = "python",
                name = "Python",
                version = "v3.11+",
                badge = "PY",
                badgeBg = Color(0xFF3B82F6),
                badgeTextColor = Color(0xFFFBBF24),
                ext = "py",
                isInstalled = pyInstalled,
                status = if (pyInstalled) "Installed & Ready" else "Download Required",
                statusColor = if (pyInstalled) StatusSuccess else StatusWarning,
                termuxCommand = "pkg install python",
                downloadUrl = "https://www.python.org/downloads/",
                description = "Python 3 high-level interpreted programming language. Supports offline execution via Termux or system PATH."
            ),
            LanguageToolchainInfo(
                id = "javascript",
                name = "JavaScript",
                version = "v20 (V8 Engine)",
                badge = "JS",
                badgeBg = Color(0xFFFBBF24),
                badgeTextColor = Color(0xFF000000),
                ext = "js",
                isInstalled = true,
                status = "Built-in / Ready",
                statusColor = StatusSuccess,
                termuxCommand = "pkg install nodejs",
                downloadUrl = "https://nodejs.org/",
                description = "Offline JavaScript execution engine powered by built-in Android WebView sandbox and optional Node.js."
            ),
            LanguageToolchainInfo(
                id = "html",
                name = "HTML5",
                version = "v5.0",
                badge = "HTML",
                badgeBg = Color(0xFFE34F26),
                badgeTextColor = Color(0xFFFFFFFF),
                ext = "html",
                isInstalled = true,
                status = "Built-in / Ready",
                statusColor = StatusSuccess,
                termuxCommand = "None (Built-in Web Sandbox)",
                downloadUrl = "https://developer.mozilla.org/en-US/docs/Web/HTML",
                description = "Standard web markup with direct live in-app Web Sandbox preview."
            ),
            LanguageToolchainInfo(
                id = "css",
                name = "CSS3",
                version = "v3.0",
                badge = "CSS",
                badgeBg = Color(0xFF1572B6),
                badgeTextColor = Color(0xFFFFFFFF),
                ext = "css",
                isInstalled = true,
                status = "Built-in / Ready",
                statusColor = StatusSuccess,
                termuxCommand = "None (Built-in Web Sandbox)",
                downloadUrl = "https://developer.mozilla.org/en-US/docs/Web/CSS",
                description = "Styling engine previewed live alongside HTML templates."
            ),
            LanguageToolchainInfo(
                id = "typescript",
                name = "TypeScript",
                version = "v5.2+",
                badge = "TS",
                badgeBg = Color(0xFF007ACC),
                badgeTextColor = Color(0xFFFFFFFF),
                ext = "ts",
                isInstalled = tsInstalled,
                status = if (tsInstalled) "Installed & Ready" else "Download Required",
                statusColor = if (tsInstalled) StatusSuccess else StatusWarning,
                termuxCommand = "pkg install nodejs && npm install -g typescript",
                downloadUrl = "https://www.typescriptlang.org/",
                description = "Typed superset of JavaScript that compiles down to clean JavaScript."
            ),
            LanguageToolchainInfo(
                id = "kotlin",
                name = "Kotlin",
                version = "v1.9+",
                badge = "KT",
                badgeBg = Color(0xFF7F52FF),
                badgeTextColor = Color(0xFFFFFFFF),
                ext = "kt",
                isInstalled = ktInstalled,
                status = if (ktInstalled) "Installed & Ready" else "Download Required",
                statusColor = if (ktInstalled) StatusSuccess else StatusWarning,
                termuxCommand = "pkg install openjdk-17 kotlin",
                downloadUrl = "https://kotlinlang.org/",
                description = "Modern JVM and Android language. Builds with kotlinc and runs automatically via JVM."
            ),
            LanguageToolchainInfo(
                id = "java",
                name = "Java",
                version = "v17+",
                badge = "JAVA",
                badgeBg = Color(0xFFEA2D2E),
                badgeTextColor = Color(0xFFFFFFFF),
                ext = "java",
                isInstalled = javaInstalled,
                status = if (javaInstalled) "Installed & Ready" else "Download Required",
                statusColor = if (javaInstalled) StatusSuccess else StatusWarning,
                termuxCommand = "pkg install openjdk-17",
                downloadUrl = "https://openjdk.org/",
                description = "Standard Java compiler (javac) and runtime environment (java)."
            ),
            LanguageToolchainInfo(
                id = "cpp",
                name = "C/C++",
                version = "v13+ (Clang)",
                badge = "C++",
                badgeBg = Color(0xFF00599C),
                badgeTextColor = Color(0xFFFFFFFF),
                ext = "cpp",
                isInstalled = cppInstalled,
                status = if (cppInstalled) "Installed & Ready" else "Download Required",
                statusColor = if (cppInstalled) StatusSuccess else StatusWarning,
                termuxCommand = "pkg install clang",
                downloadUrl = "https://llvm.org/",
                description = "Clang/LLVM C and C++ compiler. Compiles to binary and executes automatically."
            ),
            LanguageToolchainInfo(
                id = "rust",
                name = "Rust",
                version = "v1.73+",
                badge = "RS",
                badgeBg = Color(0xFFF97316),
                badgeTextColor = Color(0xFFFFFFFF),
                ext = "rs",
                isInstalled = rustInstalled,
                status = if (rustInstalled) "Installed & Ready" else "Download Required",
                statusColor = if (rustInstalled) StatusSuccess else StatusWarning,
                termuxCommand = "pkg install rust",
                downloadUrl = "https://www.rust-lang.org/tools/install",
                description = "Memory-safe systems programming language. Builds with rustc and executes automatically."
            ),
            LanguageToolchainInfo(
                id = "go",
                name = "Go",
                version = "v1.21+",
                badge = "GO",
                badgeBg = Color(0xFF00ADD8),
                badgeTextColor = Color(0xFFFFFFFF),
                ext = "go",
                isInstalled = goInstalled,
                status = if (goInstalled) "Installed & Ready" else "Download Required",
                statusColor = if (goInstalled) StatusSuccess else StatusWarning,
                termuxCommand = "pkg install golang",
                downloadUrl = "https://go.dev/dl/",
                description = "Fast, statically typed compiled language designed for concurrency."
            )
        )
    }

    val filtered = remember(searchQuery, languages) {
        if (searchQuery.isBlank()) languages
        else languages.filter { it.name.contains(searchQuery, ignoreCase = true) || it.ext.contains(searchQuery, ignoreCase = true) }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BgApp)
    ) {
        // Screen 12 Header
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
                    text = "Supported Languages & Toolchains",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                Text(
                    text = "Live environment status & download setup guides",
                    fontSize = 10.sp,
                    color = TextMuted
                )
            }

            IconButton(onClick = { isSearchOpen = !isSearchOpen }, modifier = Modifier.size(36.dp)) {
                Icon(Icons.Default.Search, contentDescription = "Search", tint = TextSecondary, modifier = Modifier.size(20.dp))
            }
        }

        if (isSearchOpen) {
            Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Filter languages (e.g. Python, Rust)...", color = TextMuted, fontSize = 12.sp) },
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

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(filtered) { lang ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { selectedLanguageForDialog = lang },
                    colors = CardDefaults.cardColors(containerColor = BgCard),
                    border = androidx.compose.foundation.BorderStroke(1.dp, if (lang.isInstalled) BorderColor else BorderHighlight)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(lang.badgeBg),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = lang.badge,
                                color = lang.badgeTextColor,
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp
                            )
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = lang.name,
                                    color = TextPrimary,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 13.sp
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = lang.version,
                                    color = TextMuted,
                                    fontSize = 10.sp
                                )
                            }
                            Spacer(modifier = Modifier.height(2.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .clip(RoundedCornerShape(3.dp))
                                        .background(lang.statusColor)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = lang.status,
                                    color = lang.statusColor,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }

                        Icon(
                            imageVector = if (lang.isInstalled) Icons.Default.CheckCircle else Icons.Default.Download,
                            contentDescription = null,
                            tint = if (lang.isInstalled) StatusSuccess else AccentCyan,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Icon(Icons.Default.ChevronRight, contentDescription = null, tint = TextMuted, modifier = Modifier.size(16.dp))
                    }
                }
            }
        }
    }

    // Detail & Download Dialog
    selectedLanguageForDialog?.let { lang ->
        LanguageDetailDialog(
            lang = lang,
            onDismiss = { selectedLanguageForDialog = null },
            onCopyCommand = { cmd ->
                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                val clip = ClipData.newPlainText("Termux Command", cmd)
                clipboard.setPrimaryClip(clip)
                Toast.makeText(context, "Command copied: $cmd", Toast.LENGTH_SHORT).show()
            },
            onOpenUrl = { url ->
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                runCatching { context.startActivity(intent) }
            }
        )
    }
}

@Composable
fun LanguageDetailDialog(
    lang: LanguageToolchainInfo,
    onDismiss: () -> Unit,
    onCopyCommand: (String) -> Unit,
    onOpenUrl: (String) -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
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
                    .padding(18.dp)
            ) {
                // Header
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(lang.badgeBg),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = lang.badge,
                            color = lang.badgeTextColor,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = lang.name,
                            color = TextPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                        Text(
                            text = "${lang.version} • Status: ${lang.status}",
                            color = lang.statusColor,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = lang.description,
                    color = TextSecondary,
                    fontSize = 11.sp,
                    lineHeight = 16.sp
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Status Banner
                if (lang.isInstalled) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = StatusSuccess.copy(alpha = 0.12f)),
                        border = androidx.compose.foundation.BorderStroke(1.dp, StatusSuccess.copy(alpha = 0.4f))
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = StatusSuccess, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Toolchain is ready! You can run and build ${lang.name} code directly.",
                                color = TextPrimary,
                                fontSize = 11.sp
                            )
                        }
                    }
                } else {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = StatusWarning.copy(alpha = 0.12f)),
                        border = androidx.compose.foundation.BorderStroke(1.dp, StatusWarning.copy(alpha = 0.4f))
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Warning, contentDescription = null, tint = StatusWarning, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "${lang.name} runtime is missing. Install via Termux or download official package to execute scripts.",
                                color = TextPrimary,
                                fontSize = 11.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "1. Install via Termux / Shell:",
                        color = TextPrimary,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 11.sp
                    )
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
                        Text(
                            text = lang.termuxCommand,
                            color = AccentCyan,
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace,
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(
                            onClick = { onCopyCommand(lang.termuxCommand) },
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(Icons.Default.ContentCopy, contentDescription = "Copy", tint = TextSecondary, modifier = Modifier.size(15.dp))
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = "2. Official Website Download:",
                        color = TextPrimary,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 11.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))

                    Button(
                        onClick = { onOpenUrl(lang.downloadUrl) },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = AccentPrimary),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(Icons.Default.OpenInBrowser, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Open Official Download Site", color = Color.White, fontSize = 12.sp)
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Close", color = TextSecondary)
                    }
                }
            }
        }
    }
}
