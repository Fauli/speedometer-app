package com.franz.speedometer.ui

import com.franz.speedometer.data.SpeedUnit
import java.util.Locale
import kotlin.math.roundToInt

fun speedInUnit(mps: Float, unit: SpeedUnit): Float = mps * unit.speedFactor

fun formatSpeed(mps: Float, unit: SpeedUnit, decimals: Int): String {
    val v = speedInUnit(mps, unit)
    return if (decimals <= 0) v.roundToInt().toString()
    else String.format(Locale.US, "%.${decimals}f", v)
}

fun formatDistance(meters: Double, unit: SpeedUnit): String {
    val v = meters * unit.distFactor
    return String.format(Locale.US, "%.2f %s", v, unit.distLabel)
}

fun formatDuration(ms: Long): String {
    val totalSec = ms / 1000
    val h = totalSec / 3600
    val m = (totalSec % 3600) / 60
    val sec = totalSec % 60
    return if (h > 0) String.format(Locale.US, "%d:%02d:%02d", h, m, sec)
    else String.format(Locale.US, "%02d:%02d", m, sec)
}

fun headingLabel(bearing: Float?): String {
    if (bearing == null) return "—"
    val dirs = listOf("N", "NE", "E", "SE", "S", "SW", "W", "NW")
    val idx = (((bearing % 360) + 360) % 360 / 45f).roundToInt() % 8
    return String.format(Locale.US, "%s %03d°", dirs[idx], bearing.roundToInt() % 360)
}

fun formatAltitude(meters: Double?): String =
    if (meters == null) "—" else String.format(Locale.US, "%d m", meters.roundToInt())
