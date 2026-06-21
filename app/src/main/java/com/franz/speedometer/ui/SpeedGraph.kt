package com.franz.speedometer.ui

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import com.franz.speedometer.data.SpeedUnit

/** Live speed-over-time histograph for the current trip. */
@Composable
fun SpeedGraph(
    history: List<Float>,    // m/s samples, oldest first
    unit: SpeedUnit,
    lineColor: Color,
    modifier: Modifier = Modifier,
) {
    Canvas(modifier = modifier) {
        if (history.size < 2) return@Canvas
        val maxV = (history.maxOrNull() ?: 0f).coerceAtLeast(0.1f)
        val w = size.width
        val h = size.height
        val stepX = w / (history.size - 1)

        // Baseline
        drawLine(
            color = lineColor.copy(alpha = 0.25f),
            start = Offset(0f, h),
            end = Offset(w, h),
            strokeWidth = 2f,
        )

        val path = Path()
        history.forEachIndexed { i, mps ->
            val x = i * stepX
            val y = h - (mps / maxV) * h
            if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        drawPath(path = path, color = lineColor, style = Stroke(width = 4f))
    }
}
