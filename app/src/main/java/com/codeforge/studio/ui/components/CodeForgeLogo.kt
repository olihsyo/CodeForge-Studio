package com.codeforge.studio.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.codeforge.studio.ui.theme.AccentCyan
import com.codeforge.studio.ui.theme.AccentPrimary

/**
 * Modern vector illustration of the CodeForge Studio ribbon/chevron icon
 * matching the VS Code styled emblem in the mockups.
 */
@Composable
fun CodeForgeLogo(
    modifier: Modifier = Modifier,
    size: Dp = 48.dp
) {
    Box(
        modifier = modifier.size(size),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(size)) {
            val w = size.toPx()
            val h = size.toPx()

            val primaryGrad = Brush.linearGradient(
                colors = listOf(AccentCyan, AccentPrimary),
                start = Offset(0f, 0f),
                end = Offset(w, h)
            )

            val ribbonDark = Brush.linearGradient(
                colors = listOf(Color(0xFF005FB8), Color(0xFF007ACC)),
                start = Offset(0f, 0f),
                end = Offset(w, h)
            )

            // Right Chevron / Ribbon fold
            val backFold = Path().apply {
                moveTo(w * 0.72f, h * 0.12f)
                lineTo(w * 0.94f, h * 0.28f)
                lineTo(w * 0.94f, h * 0.72f)
                lineTo(w * 0.72f, h * 0.88f)
                close()
            }
            drawPath(backFold, brush = ribbonDark, style = Fill)

            // Upper fold
            val upperFold = Path().apply {
                moveTo(w * 0.72f, h * 0.12f)
                lineTo(w * 0.94f, h * 0.28f)
                lineTo(w * 0.24f, h * 0.80f)
                lineTo(w * 0.08f, h * 0.68f)
                close()
            }
            drawPath(upperFold, brush = primaryGrad, style = Fill)

            // Lower fold
            val lowerFold = Path().apply {
                moveTo(w * 0.72f, h * 0.88f)
                lineTo(w * 0.94f, h * 0.72f)
                lineTo(w * 0.24f, h * 0.20f)
                lineTo(w * 0.08f, h * 0.32f)
                close()
            }
            drawPath(lowerFold, brush = primaryGrad, style = Fill)

            // Center chevron overlap
            val centerFold = Path().apply {
                moveTo(w * 0.08f, h * 0.32f)
                lineTo(w * 0.30f, h * 0.50f)
                lineTo(w * 0.08f, h * 0.68f)
                lineTo(w * 0.05f, h * 0.64f)
                lineTo(w * 0.22f, h * 0.50f)
                lineTo(w * 0.05f, h * 0.36f)
                close()
            }
            drawPath(centerFold, color = Color.White.copy(alpha = 0.9f), style = Fill)
        }
    }
}
