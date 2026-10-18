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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.CreateNewFolder
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.DriveFileRenameOutline
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Redo
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Undo
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.ui.text.font.FontFamily
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
import com.codeforge.studio.navigation.Screen
import com.codeforge.studio.ui.components.AppSideRail
import com.codeforge.studio.workspace.Project
import kotlinx.coroutines.launch
import java.io.File

/**
 * Screen 3 (Project Explorer) & Screen 4 (Code Editor)
 * Comprehensive implementation with Project Explorer drawer/panel,
 * Tab Bar, Touch Keyboard Accessory Bar, Quick Save, Undo/Redo, and Status Bar.
 */
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun EditorScreen(
    currentProject: Project?,
    fileManager: FileManager,
    draftManager: DraftManager,
    diagnosticsManager: DiagnosticsManager,
    settingsManager: SettingsManager,
    onQuickRun: (File?) -> Unit = {},
    onNavigateTo: (Screen) -> Unit = {}
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
    var showRenameDialog by remember { mutableStateOf(false) }
    var fileToRename by remember { mutableStateOf<File?>(null) }
    var renameNewName by remember { mutableStateOf("") }
    var fileToDelete by remember { mutableStateOf<File?>(null) }
    var showSearchDialog by remember { mutableStateOf(false) }
    var newFileName by remember { mutableStateOf("") }
    var newFolderName by remember { mutableStateOf("") }
    var searchQuery by remember { mutableStateOf("") }
    var replaceQuery by remember { mutableStateOf("") }

    // Selected file in explorer for actions
    var selectedExplorerFile by remember { mutableStateOf<File?>(null) }

    var tabToCloseWithUnsavedChanges by remember { mutableStateOf<Pair<EditorTab, Int>?>(null) }

    fun refreshTree() {
        currentProject?.rootDirectory?.let { root ->
            scope.launch {
                fileTree = fileManager.loadFileTree(root, showHidden)
            }
        }
    }

    LaunchedEffect(currentProject?.metadata?.id, showHidden) {
        refreshTree()
        if (currentProject != null) {
            val root = currentProject.rootDirectory
            val isWeb = currentProject.metadata.primaryLanguage.equals("web", ignoreCase = true) ||
                    currentProject.metadata.template.equals("web", ignoreCase = true)

            // Reset tabs for new project if needed
            val needsDefaultLoad = openTabs.isEmpty() || openTabs.none { it.file.exists() && it.file.startsWith(root) }
            if (needsDefaultLoad) {
                openTabs.clear()
                val defaultFile = if (isWeb) {
                    File(root, "index.html").takeIf { it.exists() }
                        ?: root.walkTopDown().firstOrNull { it.isFile && it.extension.lowercase() == "html" }
                        ?: File(root, "app.js").takeIf { it.exists() }
                        ?: root.walkTopDown().firstOrNull { it.isFile && !it.name.startsWith(".") && it.extension.lowercase() != "css" }
                        ?: File(root, "style.css").takeIf { it.exists() }
                } else {
                    File(root, "main.py").takeIf { it.exists() }
                        ?: File(root, "index.html").takeIf { it.exists() }
                        ?: File(root, "Main.kt").takeIf { it.exists() }
                        ?: File(root, "main.rs").takeIf { it.exists() }
                        ?: File(root, "main.go").takeIf { it.exists() }
                        ?: File(root, "main.cpp").takeIf { it.exists() }
                        ?: root.walkTopDown().firstOrNull { it.isFile && !it.name.startsWith(".") && it.extension.lowercase() != "css" }
                } ?: root.walkTopDown().firstOrNull { it.isFile && !it.name.startsWith(".") }

                defaultFile?.let { file ->
                    val content = fileManager.readFile(file).getOrDefault("")
                    val tab = EditorTab(file = file, content = content)
                    openTabs.add(tab)
                    activeTabIndex = 0
                    webViewInstance?.let { CodeEditorBridge.setContent(it, content, tab.language) }
                }
            }
        }
    }

    fun openFile(file: File) {
        val existingIndex = openTabs.indexOfFirst { it.file.absolutePath == file.absolutePath }
        if (existingIndex != -1) {
            activeTabIndex = existingIndex
            val tab = openTabs[existingIndex]
            webViewInstance?.let { CodeEditorBridge.setContent(it, tab.content, tab.language) }
        } else {
            scope.launch {
                val content = fileManager.readFile(file).getOrDefault("")
                val tab = EditorTab(file = file, content = content)
                openTabs.add(tab)
                activeTabIndex = openTabs.size - 1
                webViewInstance?.let {
                    CodeEditorBridge.setContent(it, content, tab.language)
                }
            }
        }
        scope.launch { drawerState.close() }
    }

    fun closeTab(index: Int) {
        if (index in openTabs.indices) {
            val closedTab = openTabs[index]
            scope.launch { draftManager.clearDraft(closedTab.file.absolutePath) }
            openTabs.removeAt(index)
            if (openTabs.isNotEmpty()) {
                if (activeTabIndex >= openTabs.size) {
                    activeTabIndex = openTabs.size - 1
                }
                webViewInstance?.let {
                    val nextTab = openTabs[activeTabIndex]
                    CodeEditorBridge.setContent(it, nextTab.content, nextTab.language)
                }
            }
        }
    }

    fun saveFile(tab: EditorTab, index: Int, onSaved: (() -> Unit)? = null) {
        scope.launch {
            fileManager.writeFile(tab.file, tab.content).onSuccess {
                draftManager.clearDraft(tab.file.absolutePath)
                if (index in openTabs.indices && openTabs[index].file.absolutePath == tab.file.absolutePath) {
                    openTabs[index] = tab.copy(isDirty = false)
                }
                diagnosticsManager.analyzeFile(tab.file, tab.content)
                onSaved?.invoke()
            }.onFailure {
                onSaved?.invoke()
            }
        }
    }

    fun saveActiveFile(onSaved: (() -> Unit)? = null) {
        if (activeTabIndex in openTabs.indices) {
            saveFile(openTabs[activeTabIndex], activeTabIndex, onSaved)
        } else {
            onSaved?.invoke()
        }
    }

    // Modal Drawer representing Screen 3: Project Explorer
    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(
                drawerContainerColor = BgPanel,
                modifier = Modifier.width(320.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 14.dp, vertical = 12.dp)
                ) {
                    // Screen 3 Header: Project title and breadcrumb path
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = currentProject?.name ?: "Project Explorer",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                            Text(
                                text = currentProject?.path ?: "/storage/emulated/0/CodeForge",
                                fontSize = 10.sp,
                                color = TextSecondary,
                                maxLines = 1
                            )
                        }

                        IconButton(onClick = { scope.launch { drawerState.close() } }, modifier = Modifier.size(32.dp)) {
                            Icon(Icons.Default.Close, contentDescription = "Close", tint = TextSecondary, modifier = Modifier.size(18.dp))
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Project Root node banner (Screen 3)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(BgCard)
                            .border(1.dp, BorderColor, RoundedCornerShape(8.dp))
                            .padding(horizontal = 10.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.FolderOpen, contentDescription = null, tint = AccentPrimary, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = currentProject?.name ?: "Workspace",
                                color = AccentCyan,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = currentProject?.rootDirectory?.absolutePath ?: "",
                                color = TextMuted,
                                fontSize = 9.sp,
                                maxLines = 1
                            )
                        }
                        Icon(Icons.Default.ExpandMore, contentDescription = null, tint = TextMuted, modifier = Modifier.size(16.dp))
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Hierarchical File Tree
                    LazyColumn(modifier = Modifier.weight(1f)) {
                        fileTree?.let { root ->
                            item {
                                FileTreeNodeView(
                                    node = root,
                                    level = 0,
                                    selectedFile = selectedExplorerFile,
                                    onFileClick = {
                                        selectedExplorerFile = it
                                        openFile(it)
                                    },
                                    onSelectFile = { selectedExplorerFile = it }
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Screen 3 Bottom Action Bar: New File, New Folder, Rename, Delete
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(1.dp)
                            .background(BorderColor)
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceAround,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        ExplorerBottomAction(
                            title = "New File",
                            icon = Icons.Default.Add,
                            onClick = { showNewFileDialog = true }
                        )
                        ExplorerBottomAction(
                            title = "New Folder",
                            icon = Icons.Default.CreateNewFolder,
                            onClick = { showNewFolderDialog = true }
                        )
                        ExplorerBottomAction(
                            title = "Rename",
                            icon = Icons.Default.DriveFileRenameOutline,
                            enabled = selectedExplorerFile != null,
                            onClick = {
                                selectedExplorerFile?.let {
                                    fileToRename = it
                                    renameNewName = it.name
                                    showRenameDialog = true
                                }
                            }
                        )
                        ExplorerBottomAction(
                            title = "Delete",
                            icon = Icons.Default.Delete,
                            enabled = selectedExplorerFile != null,
                            onClick = {
                                selectedExplorerFile?.let {
                                    fileToDelete = it
                                }
                            }
                        )
                    }
                }
            }
        }
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .background(BgEditor)
        ) {
            // Screen 4: Left Activity Bar (Side Rail)
            AppSideRail(
                currentRoute = Screen.Editor.route,
                problemsCount = 0,
                onToggleExplorer = { scope.launch { drawerState.open() } },
                onNavigate = onNavigateTo,
                onOpenSearch = { showSearchDialog = true }
            )

            // Screen 4: Code Editor Surface
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .background(BgEditor)
            ) {
            // Editor Tab Bar & Controls (Screen 4 top)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(38.dp)
                    .background(BgPanel)
                    .padding(horizontal = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Menu icon to open Project Explorer (Screen 3)
                IconButton(onClick = { scope.launch { drawerState.open() } }, modifier = Modifier.size(36.dp)) {
                    Icon(Icons.Default.Menu, contentDescription = "Explorer", tint = AccentPrimary, modifier = Modifier.size(20.dp))
                }

                // Scrollable File Tabs
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
                                .clip(RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp))
                                .background(if (isSelected) BgEditor else Color.Transparent)
                                .border(
                                    1.dp,
                                    if (isSelected) BorderHighlight else Color.Transparent,
                                    RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp)
                                )
                                .clickable {
                                    activeTabIndex = index
                                    webViewInstance?.let { CodeEditorBridge.setContent(it, tab.content, tab.language) }
                                }
                                .padding(horizontal = 10.dp, vertical = 7.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (tab.isDirty) {
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .clip(CircleShape)
                                        .background(AccentCyan)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                            }
                            Text(
                                text = tab.title,
                                color = if (isSelected) TextPrimary else TextSecondary,
                                fontSize = 12.sp,
                                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Close Tab",
                                tint = TextMuted,
                                modifier = Modifier
                                    .size(13.dp)
                                    .clickable {
                                        if (tab.isDirty) {
                                            tabToCloseWithUnsavedChanges = tab to index
                                        } else {
                                            closeTab(index)
                                        }
                                    }
                            )
                        }
                        Spacer(modifier = Modifier.width(2.dp))
                    }
                }

                // Quick Tool Action Icons
                IconButton(onClick = { saveActiveFile() }, modifier = Modifier.size(34.dp)) {
                    Icon(Icons.Default.Save, contentDescription = "Save", tint = StatusSuccess, modifier = Modifier.size(17.dp))
                }
                IconButton(onClick = { webViewInstance?.let { CodeEditorBridge.undo(it) } }, modifier = Modifier.size(34.dp)) {
                    Icon(Icons.Default.Undo, contentDescription = "Undo", tint = TextSecondary, modifier = Modifier.size(17.dp))
                }
                IconButton(onClick = { webViewInstance?.let { CodeEditorBridge.redo(it) } }, modifier = Modifier.size(34.dp)) {
                    Icon(Icons.Default.Redo, contentDescription = "Redo", tint = TextSecondary, modifier = Modifier.size(17.dp))
                }
                IconButton(onClick = { showSearchDialog = true }, modifier = Modifier.size(34.dp)) {
                    Icon(Icons.Default.Search, contentDescription = "Search", tint = TextSecondary, modifier = Modifier.size(17.dp))
                }
                IconButton(
                    onClick = {
                        val currentFile = openTabs.getOrNull(activeTabIndex)?.file
                        saveActiveFile {
                            onQuickRun(currentFile)
                        }
                    },
                    modifier = Modifier.size(34.dp)
                ) {
                    Icon(Icons.Default.PlayArrow, contentDescription = "Run", tint = AccentCyan, modifier = Modifier.size(19.dp))
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
                                useWideViewPort = false
                                loadWithOverviewMode = false
                                textZoom = 100
                                setSupportZoom(false)
                                builtInZoomControls = false
                                displayZoomControls = false
                                defaultTextEncodingName = "utf-8"
                                cacheMode = WebSettings.LOAD_DEFAULT
                            }
                            setBackgroundColor(0xFF0F141E.toInt())

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

            // Screen 4: Quick Symbol Virtual Keyboard Accessory Bar
            val symbols = listOf("(", ")", "{", "}", "[", "]", ";", "=", "<", ">", ".", "\"", "'", ":", "!", "+", "-", "_", "Tab")
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(32.dp)
                    .background(BgPanel)
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 4.dp, vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                symbols.forEach { sym ->
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(BgEditor)
                            .border(1.dp, BorderColor, RoundedCornerShape(4.dp))
                            .clickable {
                                val textToInsert = if (sym == "Tab") "    " else sym
                                webViewInstance?.let { CodeEditorBridge.insertText(it, textToInsert) }
                            }
                            .padding(horizontal = 10.dp, vertical = 4.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = sym,
                            color = AccentCyan,
                            fontSize = 12.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }

            // Screen 4: Editor Bottom Status Bar (Ln 1, Col 1 | UTF-8 | Kotlin | Space: 4)
            val activeTab = openTabs.getOrNull(activeTabIndex)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(26.dp)
                    .background(BgApp)
                    .border(androidx.compose.foundation.BorderStroke(1.dp, BorderColor))
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
}

    // New File Dialog
    if (showNewFileDialog) {
        AlertDialog(
            onDismissRequest = { showNewFileDialog = false },
            title = { Text("Create New File", color = TextPrimary, fontSize = 16.sp) },
            text = {
                OutlinedTextField(
                    value = newFileName,
                    onValueChange = { newFileName = it },
                    label = { Text("File Name (e.g. MainActivity.kt)") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                        focusedBorderColor = AccentPrimary,
                        unfocusedBorderColor = BorderColor
                    )
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newFileName.isNotBlank() && currentProject != null) {
                            showNewFileDialog = false
                            scope.launch {
                                val targetDir = if (selectedExplorerFile?.isDirectory == true) selectedExplorerFile!! else currentProject.rootDirectory
                                fileManager.createFile(targetDir, newFileName).onSuccess { file ->
                                    newFileName = ""
                                    refreshTree()
                                    openFile(file)
                                }
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = AccentPrimary)
                ) { Text("Create", color = Color.White) }
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
            title = { Text("Create New Folder", color = TextPrimary, fontSize = 16.sp) },
            text = {
                OutlinedTextField(
                    value = newFolderName,
                    onValueChange = { newFolderName = it },
                    label = { Text("Folder Name") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                        focusedBorderColor = AccentPrimary,
                        unfocusedBorderColor = BorderColor
                    )
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newFolderName.isNotBlank() && currentProject != null) {
                            showNewFolderDialog = false
                            scope.launch {
                                val targetDir = if (selectedExplorerFile?.isDirectory == true) selectedExplorerFile!! else currentProject.rootDirectory
                                fileManager.createDirectory(targetDir, newFolderName).onSuccess {
                                    newFolderName = ""
                                    refreshTree()
                                }
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = AccentPrimary)
                ) { Text("Create", color = Color.White) }
            },
            dismissButton = {
                TextButton(onClick = { showNewFolderDialog = false }) { Text("Cancel", color = TextSecondary) }
            },
            containerColor = BgPanel
        )
    }

    // Rename Dialog
    if (showRenameDialog && fileToRename != null) {
        AlertDialog(
            onDismissRequest = { showRenameDialog = false },
            title = { Text("Rename Item", color = TextPrimary, fontSize = 16.sp) },
            text = {
                OutlinedTextField(
                    value = renameNewName,
                    onValueChange = { renameNewName = it },
                    label = { Text("New Name") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                        focusedBorderColor = AccentPrimary,
                        unfocusedBorderColor = BorderColor
                    )
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        val file = fileToRename
                        if (file != null && renameNewName.isNotBlank()) {
                            showRenameDialog = false
                            scope.launch {
                                fileManager.rename(file, renameNewName.trim()).onSuccess { dest ->
                                    refreshTree()
                                    openTabs.forEachIndexed { idx, tab ->
                                        if (tab.file.absolutePath == file.absolutePath) {
                                            openTabs[idx] = tab.copy(file = dest, title = dest.name)
                                        } else if (file.isDirectory && tab.file.startsWith(file)) {
                                            val rel = tab.file.relativeTo(file).path
                                            val newFile = File(dest, rel)
                                            openTabs[idx] = tab.copy(file = newFile)
                                        }
                                    }
                                }
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = AccentPrimary)
                ) { Text("Rename", color = Color.White) }
            },
            dismissButton = {
                TextButton(onClick = { showRenameDialog = false }) { Text("Cancel", color = TextSecondary) }
            },
            containerColor = BgPanel
        )
    }

    // Delete Confirmation Dialog
    if (fileToDelete != null) {
        AlertDialog(
            onDismissRequest = { fileToDelete = null },
            title = { Text("Delete Confirmation", color = StatusError, fontSize = 16.sp) },
            text = { Text("Delete '${fileToDelete?.name}'? This action cannot be undone.", color = TextPrimary) },
            confirmButton = {
                Button(
                    onClick = {
                        fileToDelete?.let { target ->
                            scope.launch {
                                fileManager.delete(target).onSuccess {
                                    refreshTree()
                                    val toRemove = openTabs.filter {
                                        it.file.absolutePath == target.absolutePath || (target.isDirectory && it.file.startsWith(target))
                                    }
                                    toRemove.forEach { tab ->
                                        draftManager.clearDraft(tab.file.absolutePath)
                                    }
                                    openTabs.removeAll(toRemove)
                                    if (openTabs.isNotEmpty()) {
                                        if (activeTabIndex >= openTabs.size) {
                                            activeTabIndex = openTabs.size - 1
                                        }
                                        webViewInstance?.let {
                                            val nextTab = openTabs[activeTabIndex]
                                            CodeEditorBridge.setContent(it, nextTab.content, nextTab.language)
                                        }
                                    }
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

    // Unsaved Changes Confirmation Dialog
    tabToCloseWithUnsavedChanges?.let { (tab, index) ->
        AlertDialog(
            onDismissRequest = { tabToCloseWithUnsavedChanges = null },
            title = { Text("Unsaved Changes", color = StatusWarning, fontSize = 16.sp) },
            text = { Text("Save changes to '${tab.title}' before closing?", color = TextPrimary) },
            confirmButton = {
                Button(
                    onClick = {
                        val pending = tabToCloseWithUnsavedChanges
                        tabToCloseWithUnsavedChanges = null
                        if (pending != null) {
                            saveFile(pending.first, pending.second) {
                                closeTab(pending.second)
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = AccentPrimary)
                ) {
                    Text("Save & Close", color = Color.White)
                }
            },
            dismissButton = {
                Row {
                    TextButton(onClick = {
                        val pending = tabToCloseWithUnsavedChanges
                        tabToCloseWithUnsavedChanges = null
                        if (pending != null) {
                            closeTab(pending.second)
                        }
                    }) {
                        Text("Don't Save", color = StatusError)
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                    TextButton(onClick = { tabToCloseWithUnsavedChanges = null }) {
                        Text("Cancel", color = TextSecondary)
                    }
                }
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
                    Text("Find & Replace", fontWeight = FontWeight.Bold, color = AccentPrimary)
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
                            onClick = { webViewInstance?.let { CodeEditorBridge.search(it, searchQuery) } },
                            colors = ButtonDefaults.buttonColors(containerColor = BgEditor)
                        ) { Text("Find Next", color = TextPrimary) }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = { webViewInstance?.let { CodeEditorBridge.search(it, searchQuery, replaceQuery, false) } },
                            colors = ButtonDefaults.buttonColors(containerColor = AccentPrimary)
                        ) { Text("Replace", color = Color.White) }
                    }
                }
            }
        }
    }
}

/**
 * Bottom action icon on Screen 3 (Project Explorer)
 */
@Composable
fun ExplorerBottomAction(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    val tint = if (enabled) TextPrimary else TextMuted
    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .clickable(enabled = enabled) { onClick() }
            .padding(horizontal = 8.dp, vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(imageVector = icon, contentDescription = title, tint = tint, modifier = Modifier.size(18.dp))
        Spacer(modifier = Modifier.height(2.dp))
        Text(text = title, color = tint, fontSize = 10.sp)
    }
}

/**
 * Hierarchical File Tree Node for Project Explorer (Screen 3)
 */
@Composable
fun FileTreeNodeView(
    node: FileNode,
    level: Int,
    selectedFile: File?,
    onFileClick: (File) -> Unit,
    onSelectFile: (File) -> Unit
) {
    var isExpanded by remember { mutableStateOf(level < 2) }
    val isSelected = selectedFile?.absolutePath == node.file.absolutePath

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(6.dp))
                .background(if (isSelected) AccentPrimary.copy(alpha = 0.15f) else Color.Transparent)
                .clickable {
                    onSelectFile(node.file)
                    if (node.isDirectory) {
                        isExpanded = !isExpanded
                    } else {
                        onFileClick(node.file)
                    }
                }
                .padding(start = (level * 14).dp, top = 5.dp, bottom = 5.dp, end = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Folder or file icon
            if (node.isDirectory) {
                Icon(
                    imageVector = if (isExpanded) Icons.Default.FolderOpen else Icons.Default.Folder,
                    contentDescription = null,
                    tint = AccentCyan,
                    modifier = Modifier.size(16.dp)
                )
            } else {
                val fileTint = when (node.file.extension.lowercase()) {
                    "kt", "java" -> Color(0xFF8B5CF6)
                    "py" -> Color(0xFFFBBF24)
                    "js", "ts" -> Color(0xFF38BDF8)
                    "html", "css" -> Color(0xFFF97316)
                    "rs", "c", "cpp" -> Color(0xFF0091FF)
                    else -> TextSecondary
                }
                Icon(
                    imageVector = Icons.Default.Description,
                    contentDescription = null,
                    tint = fileTint,
                    modifier = Modifier.size(16.dp)
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            Text(
                text = node.name,
                color = if (isSelected) AccentCyan else TextPrimary,
                fontSize = 12.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = if (node.isDirectory) FontWeight.SemiBold else FontWeight.Normal,
                modifier = Modifier.weight(1f)
            )
        }

        if (node.isDirectory && isExpanded) {
            node.children.forEach { child ->
                FileTreeNodeView(
                    node = child,
                    level = level + 1,
                    selectedFile = selectedFile,
                    onFileClick = onFileClick,
                    onSelectFile = onSelectFile
                )
            }
        }
    }
}
