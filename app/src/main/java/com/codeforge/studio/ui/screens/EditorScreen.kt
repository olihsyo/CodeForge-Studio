package com.codeforge.studio.ui.screens

import android.annotation.SuppressLint
import android.view.ViewGroup
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CreateNewFolder
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Redo
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Undo
import androidx.compose.material.icons.filled.WrapText
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDrawerState
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import com.codeforge.studio.diagnostics.DiagnosticsManager
import com.codeforge.studio.editor.CodeEditorBridge
import com.codeforge.studio.editor.EditorTab
import com.codeforge.studio.filesystem.DraftManager
import com.codeforge.studio.filesystem.FileManager
import com.codeforge.studio.filesystem.FileNode
import com.codeforge.studio.settings.SettingsManager
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
import java.io.File

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun EditorScreen(
    currentProject: Project?,
    fileManager: FileManager,
    draftManager: DraftManager,
    diagnosticsManager: DiagnosticsManager,
    settingsManager: SettingsManager
) {
    val scope = rememberCoroutineScope()
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)

    val fontSizeSp by settingsManager.fontSizeSp.collectAsState()
    val tabWidth by settingsManager.tabWidth.collectAsState()
    val wordWrap by settingsManager.wordWrap.collectAsState()
    val autoSave by settingsManager.autoSaveEnabled.collectAsState()
    val showHidden by settingsManager.showHiddenFiles.collectAsState()

    var fileTree by remember { mutableStateOf<FileNode?>(null) }
    val openTabs = remember { mutableStateListOf<EditorTab>() }
    var activeTabIndex by remember { mutableStateOf(0) }
    var webViewInstance by remember { mutableStateOf<WebView?>(null) }
    var isEditorReady by remember { mutableStateOf(false) }

    // Dialog States
    var showNewFileDialog by remember { mutableStateOf(false) }
    var showNewFolderDialog by remember { mutableStateOf(false) }
    var fileToDelete by remember { mutableStateOf<File?>(null) }
    var showSearchDialog by remember { mutableStateOf(false) }
    var showGotoLineDialog by remember { mutableStateOf(false) }
    var newFileName by remember { mutableStateOf("") }
    var newFolderName by remember { mutableStateOf("") }
    var searchQuery by remember { mutableStateOf("") }
    var replaceQuery by remember { mutableStateOf("") }
    var gotoLineNumber by remember { mutableStateOf("") }

    // Refresh File Tree
    fun refreshTree() {
        currentProject?.rootDirectory?.let { root ->
            scope.launch {
                fileTree = fileManager.loadFileTree(root, showHidden)
            }
        }
    }

    LaunchedEffect(currentProject, showHidden) {
        refreshTree()
        // Open main.py or index.html by default if tabs empty
        if (openTabs.isEmpty() && currentProject != null) {
            val defaultFile = File(currentProject.rootDirectory, "main.py").takeIf { it.exists() }
                ?: File(currentProject.rootDirectory, "index.html").takeIf { it.exists() }
                ?: currentProject.rootDirectory.walkTopDown().firstOrNull { it.isFile && !it.name.startsWith(".") }
            defaultFile?.let { file ->
                val content = fileManager.readFile(file).getOrDefault("")
                openTabs.add(EditorTab(file = file, content = content))
                activeTabIndex = 0
            }
        }
    }

    // Function to open or switch tab
    fun openFile(file: File) {
        val existingIndex = openTabs.indexOfFirst { it.file.absolutePath == file.absolutePath }
        if (existingIndex != -1) {
            activeTabIndex = existingIndex
            val tab = openTabs[existingIndex]
            webViewInstance?.let { CodeEditorBridge.setContent(it, tab.content, tab.language) }
        } else {
            scope.launch {
                val content = fileManager.readFile(file).getOrDefault("")
                openTabs.add(EditorTab(file = file, content = content))
                activeTabIndex = openTabs.size - 1
                webViewInstance?.let {
                    CodeEditorBridge.setContent(it, content, openTabs.last().language)
                }
            }
        }
        scope.launch { drawerState.close() }
    }

    // Save Active File
    fun saveActiveFile() {
        if (activeTabIndex in openTabs.indices) {
            val tab = openTabs[activeTabIndex]
            scope.launch {
                fileManager.writeFile(tab.file, tab.content).onSuccess {
                    draftManager.clearDraft(tab.file.absolutePath)
                    openTabs[activeTabIndex] = tab.copy(isDirty = false)
                    diagnosticsManager.analyzeFile(tab.file, tab.content)
                }
            }
        }
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(
                drawerContainerColor = BgPanel,
                modifier = Modifier.width(300.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "PROJECT EXPLORER",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = AccentPrimary
                        )
                        Row {
                            IconButton(onClick = { showNewFileDialog = true }, modifier = Modifier.size(32.dp)) {
                                Icon(Icons.Default.Add, contentDescription = "New File", tint = TextPrimary, modifier = Modifier.size(18.dp))
                            }
                            IconButton(onClick = { showNewFolderDialog = true }, modifier = Modifier.size(32.dp)) {
                                Icon(Icons.Default.CreateNewFolder, contentDescription = "New Folder", tint = TextPrimary, modifier = Modifier.size(18.dp))
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    LazyColumn(modifier = Modifier.weight(1f)) {
                        fileTree?.let { root ->
                            item {
                                FileTreeNodeView(
                                    node = root,
                                    level = 0,
                                    onFileClick = { openFile(it) },
                                    onDeleteClick = { fileToDelete = it }
                                )
                            }
                        }
                    }
                }
            }
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(BgEditor)
        ) {
            // Editor Toolbar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp)
                    .background(BgPanel)
                    .padding(horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { scope.launch { drawerState.open() } }, modifier = Modifier.size(36.dp)) {
                    Icon(Icons.Default.Menu, contentDescription = "Open Drawer", tint = AccentPrimary, modifier = Modifier.size(20.dp))
                }

                Spacer(modifier = Modifier.width(4.dp))

                // Scrollable Tabs
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .horizontalScroll(rememberScrollState()),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    openTabs.forEachIndexed { index, tab ->
                        val isSelected = index == activeTabIndex
                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(if (isSelected) BgEditor else Color.Transparent)
                                .clickable {
                                    activeTabIndex = index
                                    webViewInstance?.let { CodeEditorBridge.setContent(it, tab.content, tab.language) }
                                }
                                .padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (tab.isDirty) {
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .clip(CircleShape)
                                        .background(AccentPrimary)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                            }
                            Text(
                                text = tab.title,
                                color = if (isSelected) TextPrimary else TextSecondary,
                                fontSize = 12.sp,
                                fontWeight = if (isSelected) FontWeight.Medium else FontWeight.Normal
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Close Tab",
                                tint = TextSecondary,
                                modifier = Modifier
                                    .size(14.dp)
                                    .clickable {
                                        if (openTabs.size > 1) {
                                            openTabs.removeAt(index)
                                            if (activeTabIndex >= openTabs.size) {
                                                activeTabIndex = openTabs.size - 1
                                            }
                                            webViewInstance?.let {
                                                val nextTab = openTabs[activeTabIndex]
                                                CodeEditorBridge.setContent(it, nextTab.content, nextTab.language)
                                            }
                                        }
                                    }
                            )
                        }
                        Spacer(modifier = Modifier.width(4.dp))
                    }
                }

                // Quick Tool Icons
                IconButton(onClick = { saveActiveFile() }, modifier = Modifier.size(36.dp)) {
                    Icon(Icons.Default.Save, contentDescription = "Save File", tint = StatusSuccess, modifier = Modifier.size(18.dp))
                }
                IconButton(onClick = { webViewInstance?.let { CodeEditorBridge.undo(it) } }, modifier = Modifier.size(36.dp)) {
                    Icon(Icons.Default.Undo, contentDescription = "Undo", tint = TextSecondary, modifier = Modifier.size(18.dp))
                }
                IconButton(onClick = { webViewInstance?.let { CodeEditorBridge.redo(it) } }, modifier = Modifier.size(36.dp)) {
                    Icon(Icons.Default.Redo, contentDescription = "Redo", tint = TextSecondary, modifier = Modifier.size(18.dp))
                }
                IconButton(onClick = { showSearchDialog = true }, modifier = Modifier.size(36.dp)) {
                    Icon(Icons.Default.Search, contentDescription = "Find & Replace", tint = TextSecondary, modifier = Modifier.size(18.dp))
                }
            }

            // Web-based Touch Editor Container
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
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
                                useWideViewPort = true
                                loadWithOverviewMode = true
                                defaultTextEncodingName = "utf-8"
                                cacheMode = WebSettings.LOAD_DEFAULT
                            }
                            setBackgroundColor(0xFF15181E.toInt())

                            val bridge = CodeEditorBridge(
                                onContentChangedCallback = { text, isDirty ->
                                    if (activeTabIndex in openTabs.indices) {
                                        val currentTab = openTabs[activeTabIndex]
                                        openTabs[activeTabIndex] = currentTab.copy(content = text, isDirty = isDirty)
                                        if (autoSave) {
                                            draftManager.saveDraft(currentTab.file.absolutePath, text)
                                        }
                                    }
                                },
                                onCursorChangedCallback = { line, col ->
                                    if (activeTabIndex in openTabs.indices) {
                                        val currentTab = openTabs[activeTabIndex]
                                        openTabs[activeTabIndex] = currentTab.copy(cursorLine = line, cursorCol = col)
                                    }
                                },
                                onReadyCallback = {
                                    isEditorReady = true
                                    post {
                                        if (activeTabIndex in openTabs.indices) {
                                            val tab = openTabs[activeTabIndex]
                                            CodeEditorBridge.setContent(this, tab.content, tab.language)
                                            CodeEditorBridge.setFontSize(this, fontSizeSp)
                                            CodeEditorBridge.setTabWidth(this, tabWidth)
                                            CodeEditorBridge.setWordWrap(this, wordWrap)
                                        }
                                    }
                                }
                            )

                            addJavascriptInterface(bridge, "AndroidBridge")
                            webViewClient = object : WebViewClient() {
                                override fun onPageFinished(view: WebView?, url: String?) {
                                    super.onPageFinished(view, url)
                                    if (activeTabIndex in openTabs.indices) {
                                        val tab = openTabs[activeTabIndex]
                                        CodeEditorBridge.setContent(this@apply, tab.content, tab.language)
                                    }
                                }
                            }

                            loadUrl("file:///android_asset/editor/index.html")
                            webViewInstance = this
                        }
                    },
                    update = { view ->
                        webViewInstance = view
                    }
                )
            }

            // Editor Bottom Status Bar
            val activeTab = openTabs.getOrNull(activeTabIndex)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(26.dp)
                    .background(BgPanel)
                    .padding(horizontal = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Ln ${activeTab?.cursorLine ?: 1}, Col ${activeTab?.cursorCol ?: 1}",
                    color = TextSecondary,
                    fontSize = 11.sp
                )
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "Spaces: $tabWidth",
                        color = TextSecondary,
                        fontSize = 11.sp,
                        modifier = Modifier.clickable {
                            val nextWidth = if (tabWidth == 4) 2 else 4
                            settingsManager.setTabWidth(nextWidth)
                            webViewInstance?.let { CodeEditorBridge.setTabWidth(it, nextWidth) }
                        }
                    )
                    Text(text = "UTF-8", color = TextSecondary, fontSize = 11.sp)
                    Text(
                        text = activeTab?.language?.uppercase() ?: "TEXT",
                        color = AccentPrimary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }

    // New File Dialog
    if (showNewFileDialog) {
        AlertDialog(
            onDismissRequest = { showNewFileDialog = false },
            title = { Text("Create New File", color = TextPrimary) },
            text = {
                OutlinedTextField(
                    value = newFileName,
                    onValueChange = { newFileName = it },
                    label = { Text("File Name (e.g. script.py)") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    )
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newFileName.isNotBlank() && currentProject != null) {
                            showNewFileDialog = false
                            scope.launch {
                                fileManager.createFile(currentProject.rootDirectory, newFileName).onSuccess { file ->
                                    newFileName = ""
                                    refreshTree()
                                    openFile(file)
                                }
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = AccentPrimary)
                ) { Text("Create", color = BgApp) }
            },
            dismissButton = {
                TextButton(onClick = { showNewFileDialog = false }) { Text("Cancel", color = TextSecondary) }
            },
            containerColor = BgPanel
        )
    }

    // New Folder Dialog
    if (showNewFolderDialog) {
        AlertDialog(
            onDismissRequest = { showNewFolderDialog = false },
            title = { Text("Create New Folder", color = TextPrimary) },
            text = {
                OutlinedTextField(
                    value = newFolderName,
                    onValueChange = { newFolderName = it },
                    label = { Text("Folder Name") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    )
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newFolderName.isNotBlank() && currentProject != null) {
                            showNewFolderDialog = false
                            scope.launch {
                                fileManager.createDirectory(currentProject.rootDirectory, newFolderName).onSuccess {
                                    newFolderName = ""
                                    refreshTree()
                                }
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = AccentPrimary)
                ) { Text("Create", color = BgApp) }
            },
            dismissButton = {
                TextButton(onClick = { showNewFolderDialog = false }) { Text("Cancel", color = TextSecondary) }
            },
            containerColor = BgPanel
        )
    }

    // Delete Confirmation Dialog
    if (fileToDelete != null) {
        AlertDialog(
            onDismissRequest = { fileToDelete = null },
            title = { Text("Delete Confirmation", color = StatusError) },
            text = { Text("Are you sure you want to delete '${fileToDelete?.name}'? This action cannot be undone.", color = TextPrimary) },
            confirmButton = {
                Button(
                    onClick = {
                        fileToDelete?.let { target ->
                            scope.launch {
                                fileManager.delete(target).onSuccess {
                                    refreshTree()
                                    openTabs.removeAll { it.file.absolutePath == target.absolutePath }
                                }
                            }
                        }
                        fileToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = StatusError)
                ) { Text("Delete", color = Color.White) }
            },
            dismissButton = {
                TextButton(onClick = { fileToDelete = null }) { Text("Cancel", color = TextSecondary) }
            },
            containerColor = BgPanel
        )
    }

    // Search and Replace Dialog
    if (showSearchDialog) {
        Dialog(onDismissRequest = { showSearchDialog = false }) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = BgPanel,
                border = androidx.compose.foundation.BorderStroke(1.dp, BorderColor),
                modifier = Modifier.padding(12.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Find and Replace", fontWeight = FontWeight.Bold, color = AccentPrimary)
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        label = { Text("Find") },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = TextPrimary, unfocusedTextColor = TextPrimary)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = replaceQuery,
                        onValueChange = { replaceQuery = it },
                        label = { Text("Replace With (optional)") },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = TextPrimary, unfocusedTextColor = TextPrimary)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) {
                        Button(
                            onClick = {
                                webViewInstance?.let { CodeEditorBridge.search(it, searchQuery) }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = BgEditor)
                        ) { Text("Find Next", color = TextPrimary) }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                webViewInstance?.let { CodeEditorBridge.search(it, searchQuery, replaceQuery, false) }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = AccentPrimary)
                        ) { Text("Replace", color = BgApp) }
                    }
                }
            }
        }
    }
}

@Composable
fun FileTreeNodeView(
    node: FileNode,
    level: Int,
    onFileClick: (File) -> Unit,
    onDeleteClick: (File) -> Unit
) {
    var isExpanded by remember { mutableStateOf(level == 0) }

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(6.dp))
                .clickable {
                    if (node.isDirectory) {
                        isExpanded = !isExpanded
                    } else {
                        onFileClick(node.file)
                    }
                }
                .padding(start = (level * 16).dp, top = 4.dp, bottom = 4.dp, end = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = if (node.isDirectory) {
                    if (isExpanded) Icons.Default.FolderOpen else Icons.Default.Folder
                } else Icons.Default.Description,
                contentDescription = null,
                tint = if (node.isDirectory) AccentPrimary else TextSecondary,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = node.name,
                color = TextPrimary,
                fontSize = 13.sp,
                modifier = Modifier.weight(1f)
            )
            if (level > 0) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Delete",
                    tint = TextSecondary.copy(alpha = 0.5f),
                    modifier = Modifier
                        .size(16.dp)
                        .clickable { onDeleteClick(node.file) }
                )
            }
        }

        if (node.isDirectory && isExpanded) {
            node.children.forEach { child ->
                FileTreeNodeView(
                    node = child,
                    level = level + 1,
                    onFileClick = onFileClick,
                    onDeleteClick = onDeleteClick
                )
            }
        }
    }
}
