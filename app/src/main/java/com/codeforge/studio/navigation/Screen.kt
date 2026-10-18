package com.codeforge.studio.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material.icons.filled.Source
import androidx.compose.material.icons.filled.Warning
import androidx.compose.ui.graphics.vector.ImageVector

sealed class Screen(val route: String, val title: String, val icon: ImageVector) {
    object Welcome : Screen("welcome", "Setup", Icons.Default.Folder)
    object Home : Screen("home", "Home", Icons.Default.Home)
    object Editor : Screen("editor", "Editor", Icons.Default.Code)
    object Terminal : Screen("terminal", "Terminal", Icons.Default.PlayArrow)
    object Build : Screen("build", "Run & Build", Icons.Default.Build)
    object Problems : Screen("problems", "Problems", Icons.Default.Warning)
    object SourceControl : Screen("git", "Git", Icons.Default.Source)
    object AiAssistant : Screen("ai", "AI Assistant", Icons.Default.SmartToy)
    object Settings : Screen("settings", "Settings", Icons.Default.Settings)
}

val BottomNavItems = listOf(
    Screen.Home,
    Screen.Editor,
    Screen.Terminal,
    Screen.Build,
    Screen.Problems,
    Screen.SourceControl
)
