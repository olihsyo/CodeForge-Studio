package com.codeforge.studio.ui.screens

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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.codeforge.studio.diagnostics.DiagnosticIssue
import com.codeforge.studio.diagnostics.DiagnosticsManager
import com.codeforge.studio.diagnostics.Severity
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
import java.io.File

/**
 * Screen 8: Problems Panel
 * Matches header, filter chips (All 6, Errors 2, Warnings 3, Info 1),
 * problem items (red error, orange warning, file names, lines/cols).
 */
@Composable
fun ProblemsScreen(
    diagnosticsManager: DiagnosticsManager,
    onNavigateToFileLine: (File, Int) -> Unit
) {
    val realIssues by diagnosticsManager.issues.collectAsState()
    var selectedFilter by remember { mutableStateOf(0) } // 0: All, 1: Errors, 2: Warnings, 3: Info
    var searchQuery by remember { mutableStateOf("") }
    var isSearchVisible by remember { mutableStateOf(false) }

    // Real issues from DiagnosticsManager
    val displayIssues = realIssues

    val errorCount = displayIssues.count { it.severity == Severity.ERROR }
    val warningCount = displayIssues.count { it.severity == Severity.WARNING }
    val infoCount = displayIssues.count { it.severity == Severity.INFO }

    val filteredIssues = remember(displayIssues, selectedFilter, searchQuery) {
        displayIssues.filter { issue ->
            val matchesFilter = when (selectedFilter) {
                1 -> issue.severity == Severity.ERROR
                2 -> issue.severity == Severity.WARNING
                3 -> issue.severity == Severity.INFO
                else -> true
            }
            val matchesSearch = searchQuery.isBlank() ||
                issue.file.name.contains(searchQuery, ignoreCase = true) ||
                issue.message.contains(searchQuery, ignoreCase = true)
            matchesFilter && matchesSearch
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BgApp)
    ) {
        // Top Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(BgPanel)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "Problems",
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )

            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = { isSearchVisible = !isSearchVisible }, modifier = Modifier.size(36.dp)) {
                    Icon(Icons.Default.Search, contentDescription = "Search", tint = TextSecondary, modifier = Modifier.size(20.dp))
                }
                IconButton(onClick = { /* Filter */ }, modifier = Modifier.size(36.dp)) {
                    Icon(Icons.Default.FilterList, contentDescription = "Filter", tint = TextSecondary, modifier = Modifier.size(20.dp))
                }
            }
        }

        if (isSearchVisible) {
            Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Filter problems...", color = TextMuted, fontSize = 12.sp) },
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

        // Screen 8 Filter Chips: All 6 | Errors 2 | Warnings 3 | Info 1
        LazyRow(
            modifier = Modifier
                .fillMaxWidth()
                .background(BgPanel)
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item {
                ProblemFilterChip(
                    label = "All ${displayIssues.size}",
                    isSelected = selectedFilter == 0,
                    onClick = { selectedFilter = 0 }
                )
            }
            item {
                ProblemFilterChip(
                    label = "Errors $errorCount",
                    color = StatusError,
                    isSelected = selectedFilter == 1,
                    onClick = { selectedFilter = 1 }
                )
            }
            item {
                ProblemFilterChip(
                    label = "Warnings $warningCount",
                    color = StatusWarning,
                    isSelected = selectedFilter == 2,
                    onClick = { selectedFilter = 2 }
                )
            }
            item {
                ProblemFilterChip(
                    label = "Info $infoCount",
                    color = AccentCyan,
                    isSelected = selectedFilter == 3,
                    onClick = { selectedFilter = 3 }
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Problem Cards List
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (filteredIssues.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 40.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = StatusSuccess, modifier = Modifier.size(36.dp))
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("No problems or syntax issues detected in workspace", color = TextSecondary, fontSize = 13.sp)
                        }
                    }
                }
            } else {
                items(filteredIssues) { issue ->
                    ProblemCard(
                        issue = issue,
                        onClick = { onNavigateToFileLine(issue.file, issue.line) }
                    )
                }
            }
        }
    }
}

/**
 * Filter Chip matching Screen 8
 */
@Composable
fun ProblemFilterChip(
    label: String,
    color: Color = AccentPrimary,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(if (isSelected) color.copy(alpha = 0.2f) else BgCard)
            .border(1.dp, if (isSelected) color else BorderColor, RoundedCornerShape(6.dp))
            .clickable { onClick() }
            .padding(horizontal = 10.dp, vertical = 6.dp)
    ) {
        Text(
            text = label,
            color = if (isSelected) color else TextSecondary,
            fontSize = 11.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
        )
    }
}

/**
 * Problem item card matching Screen 8
 */
@Composable
fun ProblemCard(
    issue: DiagnosticIssue,
    onClick: () -> Unit
) {
    val iconColor = when (issue.severity) {
        Severity.ERROR -> StatusError
        Severity.WARNING -> StatusWarning
        Severity.INFO -> AccentCyan
    }

    val iconVector = when (issue.severity) {
        Severity.ERROR -> Icons.Default.Error
        Severity.WARNING -> Icons.Default.Warning
        Severity.INFO -> Icons.Default.Info
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = BgCard),
        border = androidx.compose.foundation.BorderStroke(1.dp, BorderColor)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.Top
        ) {
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .clip(CircleShape)
                    .background(iconColor.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = iconVector,
                    contentDescription = null,
                    tint = iconColor,
                    modifier = Modifier.size(16.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = issue.file.name,
                        color = TextPrimary,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 13.sp
                    )
                    Text(
                        text = "Ln ${issue.line}, Col ${issue.column}",
                        color = TextMuted,
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }

                Spacer(modifier = Modifier.height(2.dp))

                Text(
                    text = issue.message,
                    color = TextSecondary,
                    fontSize = 12.sp,
                    lineHeight = 16.sp
                )
            }
        }
    }
}
