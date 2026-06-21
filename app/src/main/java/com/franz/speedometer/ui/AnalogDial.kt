package com.franz.speedometer.ui

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import com.franz.speedometer.data.SpeedUnit
import java.util.Locale
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sin

private const val START_ANGLE = 135f   // degrees, clockwise from 3 o'clock
private const val SWEEP = 270f

/** Classic car-style speedometer gauge. [valueInUnit] is in the display unit. */
@Composable
fun AnalogDial(
    valueInUnit: Float,
    unit: SpeedUnit,
    lowInUnit: Float,   // green/amber boundary, in this unit
    midInUnit: Float,   // amber/red boundary, in this unit
    decimals: Int,
    needleColor: Color,
    onSurface: Color,
    modifier: Modifier = Modifier,
) {
    val green = Color(0xFF4CAF50)
    val amber = Color(0xFFFFC107)
    val red = Color(0xFFFF5252)

    Canvas(modifier = modifier) {
        val cx = size.width / 2f
        val cy = size.height / 2f
        val radius = min(cx, cy) * 0.95f
        val band = radius * 0.09f
        val arcR = radius - band / 2f
        val arcTopLeft = Offset(cx - arcR, cy - arcR)
        val arcSize = Size(arcR * 2f, arcR * 2f)

        fun frac(v: Float) = (v / unit.dialMax).coerceIn(0f, 1f)
        fun arc(color: Color, from: Float, to: Float) {
            drawArc(
                color = color,
                startAngle = START_ANGLE + SWEEP * frac(from),
                sweepAngle = SWEEP * (frac(to) - frac(from)),
                useCenter = false,
                topLeft = arcTopLeft,
                size = arcSize,
                style = Stroke(width = band, cap = StrokeCap.Butt),
            )
        }

        // Colored zones (user-configurable thresholds)
        arc(green, 0f, lowInUnit)
        arc(amber, lowInUnit, midInUnit)
        arc(red, midInUnit, unit.dialMax)

        fun pointAt(deg: Float, r: Float): Offset {
            val rad = Math.toRadians(deg.toDouble())
            return Offset(cx + r * cos(rad).toFloat(), cy + r * sin(rad).toFloat())
        }

        // Ticks + labels
        val tickPaint = android.graphics.Paint().apply {
            color = onSurface.copy(alpha = 0.75f).toArgb()
            textAlign = android.graphics.Paint.Align.CENTER
            textSize = radius * 0.11f
            isAntiAlias = true
        }
        var v = 0f
        while (v <= unit.dialMax + 0.1f) {
            val deg = START_ANGLE + SWEEP * frac(v)
            val outer = pointAt(deg, arcR - band)
            val inner = pointAt(deg, arcR - band - radius * 0.05f)
            drawLine(onSurface.copy(alpha = 0.6f), outer, inner, strokeWidth = radius * 0.012f)
            val label = pointAt(deg, arcR - band - radius * 0.16f)
            drawContext.canvas.nativeCanvas.drawText(
                v.roundToInt().toString(), label.x, label.y + tickPaint.textSize / 3f, tickPaint,
            )
            v += unit.tickStep
        }

        // Needle
        val needleDeg = START_ANGLE + SWEEP * frac(valueInUnit)
        val tip = pointAt(needleDeg, radius * 0.62f)
        val tail = pointAt(needleDeg + 180f, radius * 0.10f)
        drawLine(needleColor, tail, tip, strokeWidth = radius * 0.025f, cap = StrokeCap.Round)
        drawCircle(needleColor, radius = radius * 0.05f, center = Offset(cx, cy))

        // Center digital readout + unit
        val valuePaint = android.graphics.Paint().apply {
            color = needleColor.toArgb()
            textAlign = android.graphics.Paint.Align.CENTER
            textSize = radius * 0.30f
            isAntiAlias = true
            typeface = android.graphics.Typeface.create(android.graphics.Typeface.MONOSPACE, android.graphics.Typeface.BOLD)
        }
        val unitPaint = android.graphics.Paint().apply {
            color = onSurface.copy(alpha = 0.6f).toArgb()
            textAlign = android.graphics.Paint.Align.CENTER
            textSize = radius * 0.12f
            isAntiAlias = true
        }
        val text = if (decimals <= 0) valueInUnit.roundToInt().toString()
        else String.format(Locale.US, "%.${decimals}f", valueInUnit)
        drawContext.canvas.nativeCanvas.drawText(text, cx, cy + radius * 0.42f, valuePaint)
        drawContext.canvas.nativeCanvas.drawText(unit.speedLabel, cx, cy + radius * 0.60f, unitPaint)
    }
}
