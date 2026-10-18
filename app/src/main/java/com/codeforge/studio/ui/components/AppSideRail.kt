package com.codeforge.studio.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material.icons.filled.Source
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.codeforge.studio.navigation.Screen
import com.codeforge.studio.ui.theme.AccentCyan
import com.codeforge.studio.ui.theme.AccentPrimary
import com.codeforge.studio.ui.theme.BgPanel
import com.codeforge.studio.ui.theme.BorderColor
import com.codeforge.studio.ui.theme.StatusError
import com.codeforge.studio.ui.theme.TextSecondary

/**
 * VS Code style Left Activity Bar (Side Rail)
 * Matches the left navigation strip in Screen 3 (Explorer), Screen 4 (Editor),
 * Screen 8 (Problems), Screen 9 (Git) of the design mockup.
 */
@Composable
fun AppSideRail(
    currentRoute: String?,
    problemsCount: Int = 0,
    onToggleExplorer: () -> Unit,
    onNavigate: (Screen) -> Unit,
    onOpenSearch: () -> Unit
) {
    Column(
        modifier = Modifier
            .width(42.dp)
            .fillMaxHeight()
            .background(BgPanel)
            .border(width = 1.dp, color = BorderColor)
            .padding(vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            // 1. Project Explorer Toggle (Screen 3)
            ActivityRailItem(
                icon = Icons.Default.Folder,
                description = "Explorer",
                isSelected = false,
                onClick = onToggleExplorer
            )

            // 2. Search / Find (Screen 5)
            ActivityRailItem(
                icon = Icons.Default.Search,
                description = "Search",
                isSelected = false,
                onClick = onOpenSearch
            )

            // 3. Source Control (Git) (Screen 9)
            ActivityRailItem(
                icon = Icons.Default.Source,
                description = "Git",
                isSelected = currentRoute == Screen.SourceControl.route,
                onClick = { onNavigate(Screen.SourceControl) }
            )

            // 4. Run & Build (Screen 7)
            ActivityRailItem(
                icon = Icons.Default.Build,
                description = "Build & Run",
                isSelected = currentRoute == Screen.Build.route,
                onClick = { onNavigate(Screen.Build) }
            )

            // 5. Problems & Diagnostics (Screen 8)
            ActivityRailItem(
                icon = Icons.Default.Warning,
                description = "Problems",
                badgeCount = problemsCount,
                isSelected = currentRoute == Screen.Problems.route,
                onClick = { onNavigate(Screen.Problems) }
            )

            // 6. AI Assistant (Screen 11)
            ActivityRailItem(
                icon = Icons.Default.SmartToy,
                description = "AI Assistant",
                isSelected = currentRoute == Screen.AiAssistant.route,
                onClick = { onNavigate(Screen.AiAssistant) }
            )

            // 7. Interactive Terminal
            ActivityRailItem(
                icon = Icons.Default.PlayArrow,
                description = "Terminal",
                isSelected = currentRoute == Screen.Terminal.route,
                onClick = { onNavigate(Screen.Terminal) }
            )
        }

        // Bottom: Settings (Screen 10)
        ActivityRailItem(
            icon = Icons.Default.Settings,
            description = "Settings",
            isSelected = currentRoute == Screen.Settings.route,
            onClick = { onNavigate(Screen.Settings) }
        )
    }
}

@Composable
private fun ActivityRailItem(
    icon: ImageVector,
    description: String,
    isSelected: Boolean,
    badgeCount: Int = 0,
    onClick: () -> Unit
) {
    val tint = if (isSelected) AccentPrimary else TextSecondary.copy(alpha = 0.75f)
    val bg = if (isSelected) AccentPrimary.copy(alpha = 0.15f) else Color.Transparent

    Box(
        modifier = Modifier
            .size(34.dp)
            .clip(RoundedCornerShape(6.dp))
            .background(bg)
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        if (badgeCount > 0) {
            BadgedBox(
                badge = {
                    Badge(
                        containerColor = StatusError,
                        contentColor = Color.White
                    ) {
                        Text("$badgeCount", fontSize = 8.sp, fontWeight = FontWeight.Bold)
                    }
                }
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = description,
                    tint = tint,
                    modifier = Modifier.size(17.dp)
                )
            }
        } else {
            Icon(
                imageVector = icon,
                contentDescription = description,
                tint = tint,
                modifier = Modifier.size(17.dp)
            )
        }

        // Indicator bar on left edge if selected
        if (isSelected) {
            Box(
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .width(2.dp)
                    .height(20.dp)
                    .clip(RoundedCornerShape(topEnd = 2.dp, bottomEnd = 2.dp))
                    .background(AccentCyan)
            )
        }
    }
}
