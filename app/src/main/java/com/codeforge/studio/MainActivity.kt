package com.codeforge.studio

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.FormatAlignLeft
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.RocketLaunch
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material.icons.filled.Source
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Scaffold
import java.io.File
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.codeforge.studio.ai.AiAssistantManager
import com.codeforge.studio.diagnostics.DiagnosticsManager
import com.codeforge.studio.execution.ProcessRunner
import com.codeforge.studio.filesystem.DraftManager
import com.codeforge.studio.filesystem.FileManager
import com.codeforge.studio.navigation.Screen
import com.codeforge.studio.settings.SettingsManager
import com.codeforge.studio.sourcecontrol.GitManager
import com.codeforge.studio.terminal.TerminalSession
import com.codeforge.studio.ui.components.AppBottomBar
import com.codeforge.studio.ui.components.AppTopBar
import com.codeforge.studio.ui.components.CommandAction
import com.codeforge.studio.ui.components.CommandPaletteDialog
import com.codeforge.studio.ui.screens.AiAssistantScreen
import com.codeforge.studio.ui.screens.BuildScreen
import com.codeforge.studio.ui.screens.EditorScreen
import com.codeforge.studio.ui.screens.HomeScreen
import com.codeforge.studio.ui.screens.LanguagesScreen
import com.codeforge.studio.ui.screens.NewProjectScreen
import com.codeforge.studio.ui.screens.ProblemsScreen
import com.codeforge.studio.ui.screens.SettingsScreen
import com.codeforge.studio.ui.screens.SourceControlScreen
import com.codeforge.studio.ui.screens.TerminalScreen
import com.codeforge.studio.ui.screens.WelcomeScreen
import com.codeforge.studio.ui.theme.BgApp
import com.codeforge.studio.ui.theme.CodeForgeStudioTheme
import com.codeforge.studio.workspace.WorkspaceManager

class MainActivity : ComponentActivity() {

    private lateinit var workspaceManager: WorkspaceManager
    private lateinit var fileManager: FileManager
    private lateinit var draftManager: DraftManager
    private lateinit var processRunner: ProcessRunner
    private lateinit var diagnosticsManager: DiagnosticsManager
    private lateinit var aiAssistantManager: AiAssistantManager
    private lateinit var settingsManager: SettingsManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        workspaceManager = WorkspaceManager(applicationContext)
        fileManager = FileManager()
        draftManager = DraftManager(applicationContext)
        processRunner = ProcessRunner()
        diagnosticsManager = DiagnosticsManager()
        aiAssistantManager = AiAssistantManager()
        settingsManager = SettingsManager(applicationContext)

        setContent {
            val darkTheme by settingsManager.darkTheme.collectAsState()

            CodeForgeStudioTheme(darkTheme = darkTheme) {
                CodeForgeAppContent(
                    workspaceManager = workspaceManager,
                    fileManager = fileManager,
                    draftManager = draftManager,
                    processRunner = processRunner,
                    diagnosticsManager = diagnosticsManager,
                    aiAssistantManager = aiAssistantManager,
                    settingsManager = settingsManager
                )
            }
        }
    }
}

@Composable
fun CodeForgeAppContent(
    workspaceManager: WorkspaceManager,
    fileManager: FileManager,
    draftManager: DraftManager,
    processRunner: ProcessRunner,
    diagnosticsManager: DiagnosticsManager,
    aiAssistantManager: AiAssistantManager,
    settingsManager: SettingsManager
) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val currentProject by workspaceManager.currentProject.collectAsState()
    val issues by diagnosticsManager.issues.collectAsState()

    var showCommandPalette by remember { mutableStateOf(false) }
    var runTargetFile by remember { mutableStateOf<File?>(null) }
    var autoRunTrigger by remember { mutableStateOf(false) }

    // Interactive terminal session for current project
    val terminalSession = remember(currentProject) {
        currentProject?.rootDirectory?.let { TerminalSession(it) }
    }

    val gitManager = remember(currentProject) {
        currentProject?.rootDirectory?.let { GitManager(it) }
    }

    // Command palette actions - fully functional
    val commandActions = listOf(
        CommandAction("open_editor", "Open Code Editor", "Edit project source code and tabs", Icons.Default.Code) {
            navController.navigate(Screen.Editor.route)
        },
        CommandAction("start_debugging", "Start Debugging", "Analyze diagnostics and inspect syntax warnings", Icons.Default.Warning) {
            navController.navigate(Screen.Problems.route)
        },
        CommandAction("format_doc", "Format Document", "Auto-format indentation and structure in Code Editor", Icons.Default.FormatAlignLeft) {
            navController.navigate(Screen.Editor.route)
        },
        CommandAction("execute_script", "Execute Script", "Run or build current target program", Icons.Default.RocketLaunch) {
            autoRunTrigger = true
            navController.navigate(Screen.Build.route)
        },
        CommandAction("open_terminal", "Open Terminal", "Access interactive Android shell", Icons.Default.PlayArrow) {
            navController.navigate(Screen.Terminal.route)
        },
        CommandAction("open_build", "Run & Build Panel", "Compile and execute supported programs", Icons.Default.Build) {
            navController.navigate(Screen.Build.route)
        },
        CommandAction("open_problems", "Inspect Diagnostics", "View syntax errors and compiler warnings", Icons.Default.Warning) {
            navController.navigate(Screen.Problems.route)
        },
        CommandAction("find_files", "Find in Files", "Search across project files in editor", Icons.Default.Search) {
            navController.navigate(Screen.Editor.route)
        },
        CommandAction("open_git", "Source Control", "Review Git diffs, staged files, and commit", Icons.Default.Source) {
            navController.navigate(Screen.SourceControl.route)
        },
        CommandAction("open_ai", "AI Assistant", "Generate tests and explain code", Icons.Default.SmartToy) {
            navController.navigate(Screen.AiAssistant.route)
        },
        CommandAction("open_languages", "Language Toolchains", "View 14 supported runtimes and versions", Icons.Default.Code) {
            navController.navigate(Screen.Languages.route)
        },
        CommandAction("new_workspace", "Setup Workspace", "Create or open a different project", Icons.Default.Folder) {
            navController.navigate(Screen.Welcome.route)
        },
        CommandAction("change_theme", "Change Theme", "Switch between dark and light themes", Icons.Default.Palette) {
            settingsManager.setDarkTheme(!settingsManager.darkTheme.value)
        },
        CommandAction("toggle_theme", "Toggle Color Theme", "Switch between dark and light themes", Icons.Default.Palette) {
            settingsManager.setDarkTheme(!settingsManager.darkTheme.value)
        },
        CommandAction("open_settings", "Settings", "Configure font size, tab width, and runtimes", Icons.Default.Settings) {
            navController.navigate(Screen.Settings.route)
        }
    )

    val aiApiKey by settingsManager.aiApiKey.collectAsState()
    androidx.compose.runtime.LaunchedEffect(aiApiKey) {
        aiAssistantManager.apiKey = aiApiKey
    }

    val showBottomBar = currentRoute in listOf(
        Screen.Home.route,
        Screen.Build.route,
        Screen.Problems.route,
        Screen.SourceControl.route,
        Screen.AiAssistant.route,
        Screen.Languages.route,
        Screen.Settings.route
    )

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                AppBottomBar(
                    currentRoute = currentRoute,
                    problemsCount = issues.size,
                    onNavigate = { screen ->
                        navController.navigate(screen.route) {
                            launchSingleTop = true
                        }
                    }
                )
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(BgApp)
                .padding(innerPadding)
        ) {
            NavHost(
                navController = navController,
                startDestination = if (currentProject != null) Screen.Home.route else Screen.Welcome.route
            ) {
                composable(Screen.Welcome.route) {
                    WelcomeScreen(
                        workspaceManager = workspaceManager,
                        onProjectReady = {
                            navController.navigate(Screen.Home.route) {
                                popUpTo(Screen.Welcome.route) { inclusive = true }
                            }
                        }
                    )
                }
                composable(Screen.Home.route) {
                    HomeScreen(
                        workspaceManager = workspaceManager,
                        onNavigate = { screen -> navController.navigate(screen.route) },
                        problemsCount = issues.size,
                        onOpenCommandPalette = { showCommandPalette = true }
                    )
                }
                composable(Screen.Editor.route) {
                    EditorScreen(
                        currentProject = currentProject,
                        fileManager = fileManager,
                        draftManager = draftManager,
                        diagnosticsManager = diagnosticsManager,
                        settingsManager = settingsManager,
                        onQuickRun = { file ->
                            runTargetFile = file
                            autoRunTrigger = true
                            navController.navigate(Screen.Build.route)
                        },
                        onNavigateTo = { screen -> navController.navigate(screen.route) }
                    )
                }
                composable(Screen.Terminal.route) {
                    TerminalScreen(
                        currentProject = currentProject,
                        terminalSession = terminalSession
                    )
                }
                composable(Screen.Build.route) {
                    BuildScreen(
                        currentProject = currentProject,
                        processRunner = processRunner,
                        diagnosticsManager = diagnosticsManager,
                        initialTargetFile = runTargetFile,
                        autoRun = autoRunTrigger,
                        onAutoRunConsumed = { autoRunTrigger = false }
                    )
                }
                composable(Screen.Problems.route) {
                    ProblemsScreen(
                        diagnosticsManager = diagnosticsManager,
                        onNavigateToFileLine = { _, _ ->
                            navController.navigate(Screen.Editor.route)
                        }
                    )
                }
                composable(Screen.SourceControl.route) {
                    SourceControlScreen(
                        currentProject = currentProject,
                        gitManager = gitManager
                    )
                }
                composable(Screen.AiAssistant.route) {
                    AiAssistantScreen(
                        currentProject = currentProject,
                        aiManager = aiAssistantManager,
                        fileManager = fileManager
                    )
                }
                composable(Screen.Settings.route) {
                    SettingsScreen(
                        settingsManager = settingsManager
                    )
                }
                composable(Screen.Languages.route) {
                    LanguagesScreen()
                }
                composable(Screen.NewProject.route) {
                    NewProjectScreen(
                        workspaceManager = workspaceManager,
                        onProjectCreated = {
                            navController.navigate(Screen.Home.route) {
                                popUpTo(Screen.NewProject.route) { inclusive = true }
                            }
                        },
                        onBack = { navController.popBackStack() }
                    )
                }
            }

            if (showCommandPalette) {
                CommandPaletteDialog(
                    actions = commandActions,
                    onDismiss = { showCommandPalette = false }
                )
            }
        }
    }
}
