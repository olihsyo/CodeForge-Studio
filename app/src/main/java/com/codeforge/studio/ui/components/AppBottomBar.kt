package com.codeforge.studio.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SmartToy
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
import com.codeforge.studio.ui.theme.TextPrimary
import com.codeforge.studio.ui.theme.TextSecondary

private data class NavItemData(
    val screen: Screen,
    val title: String,
    val icon: ImageVector,
    val isSelected: Boolean,
    val badgeCount: Int = 0
)

/**
 * Clean 4-Item Bottom Navigation Bar matching Screen 2, 8, 10, 11 of the design mockup.
 * Displays: Home, Projects, Builds (or AI / Problems depending on context), Settings.
 */
@Composable
fun AppBottomBar(
    currentRoute: String?,
    problemsCount: Int,
    onNavigate: (Screen) -> Unit
) {
    // 3rd item dynamically adjusts between Builds, AI, and Problems based on current screen
    val thirdItem = when (currentRoute) {
        Screen.AiAssistant.route -> NavItemData(
            screen = Screen.AiAssistant,
            title = "AI",
            icon = Icons.Default.SmartToy,
            isSelected = true
        )
        Screen.Problems.route -> NavItemData(
            screen = Screen.Problems,
            title = "Problems",
            icon = Icons.Default.Warning,
            isSelected = true,
            badgeCount = problemsCount
        )
        else -> NavItemData(
            screen = Screen.Build,
            title = "Builds",
            icon = Icons.Default.Build,
            isSelected = currentRoute == Screen.Build.route
        )
    }

    val navItems = listOf(
        NavItemData(
            screen = Screen.Home,
            title = "Home",
            icon = Icons.Default.Home,
            isSelected = currentRoute == Screen.Home.route
        ),
        NavItemData(
            screen = Screen.Editor,
            title = "Projects",
            icon = Icons.Default.Folder,
            isSelected = currentRoute == Screen.Editor.route
        ),
        thirdItem,
        NavItemData(
            screen = Screen.Settings,
            title = "Settings",
            icon = Icons.Default.Settings,
            isSelected = currentRoute == Screen.Settings.route
        )
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(BgPanel)
    ) {
        // Subtle top separator line
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(BorderColor)
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
        ) {
            navItems.forEach { item ->
                val isSelected = item.isSelected
                val itemTint = if (isSelected) AccentPrimary else TextSecondary.copy(alpha = 0.7f)

                Column(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { onNavigate(item.screen) }
                        .padding(horizontal = 10.dp, vertical = 2.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    if (item.badgeCount > 0) {
                        BadgedBox(
                            badge = {
                                Badge(
                                    containerColor = StatusError,
                                    contentColor = Color.White
                                ) {
                                    Text("${item.badgeCount}", fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        ) {
                            Icon(
                                imageVector = item.icon,
                                contentDescription = item.title,
                                tint = itemTint,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    } else {
                        Icon(
                            imageVector = item.icon,
                            contentDescription = item.title,
                            tint = itemTint,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(3.dp))

                    Text(
                        text = item.title,
                        color = if (isSelected) AccentPrimary else TextSecondary,
                        fontSize = 10.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                    )

                    // Active dot indicator
                    if (isSelected) {
                        Spacer(modifier = Modifier.height(2.dp))
                        Box(
                            modifier = Modifier
                                .size(4.dp)
                                .clip(CircleShape)
                                .background(AccentCyan)
                        )
                    }
                }
            }
        }
    }
}
