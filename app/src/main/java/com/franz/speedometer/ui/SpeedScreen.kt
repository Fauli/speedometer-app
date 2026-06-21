package com.franz.speedometer.ui

import android.content.res.Configuration
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.franz.speedometer.GpsStatus
import com.franz.speedometer.SpeedUiState
import com.franz.speedometer.data.DisplayStyle
import com.franz.speedometer.data.SettingsState
import com.franz.speedometer.data.SpeedUnit

@Composable
fun SpeedScreen(
    state: SpeedUiState,
    settings: SettingsState,
    onToggleRunning: () -> Unit,
    onResetTrip: () -> Unit,
    onOpenSettings: () -> Unit,
) {
    val onBg = MaterialTheme.colorScheme.onBackground
    val speedColor = speedColor(state.speedMps, settings, onBg)
    val flash by animateColorAsState(
        if (state.overLimit && settings.alertFlash) Color(0x55FF1744) else Color.Transparent,
        label = "flash",
    )
    val landscape = LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE

    Box(modifier = Modifier.fillMaxSize().background(flash)) {
        Column(modifier = Modifier.fillMaxSize().systemBarsPadding().padding(16.dp)) {
            StatusRow(state, onBg, onOpenSettings)

            if (landscape) {
                Row(modifier = Modifier.fillMaxWidth().weight(1f)) {
                    Box(modifier = Modifier.weight(1.1f).fillMaxHeight(), contentAlignment = Alignment.Center) {
                        SpeedBlock(state, settings, speedColor, onBg, 120.sp)
                    }
                    Column(
                        modifier = Modifier.weight(1f).fillMaxHeight(),
                        verticalArrangement = Arrangement.Center,
                    ) {
                        MetricsBlock(state, settings, onBg)
                        OdoText(state, settings, onBg)
                        GraphBlock(state, settings)
                        ControlsRow(state, onToggleRunning, onResetTrip)
                    }
                }
            } else {
                Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                    SpeedBlock(state, settings, speedColor, onBg, 150.sp)
                }
                MetricsBlock(state, settings, onBg)
                OdoText(state, settings, onBg)
                GraphBlock(state, settings)
                ControlsRow(state, onToggleRunning, onResetTrip)
            }
        }
    }
}

@Composable
private fun StatusRow(state: SpeedUiState, onBg: Color, onOpenSettings: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        GpsDot(state.status)
        Spacer(Modifier.size(8.dp))
        Text(statusText(state), color = onBg.copy(alpha = 0.7f), fontSize = 14.sp)
        Spacer(Modifier.weight(1f))
        TextButton(onClick = onOpenSettings) { Text("Settings") }
    }
}

@Composable
private fun SpeedBlock(
    state: SpeedUiState,
    settings: SettingsState,
    speedColor: Color,
    onBg: Color,
    fontSize: TextUnit,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.graphicsLayer { scaleX = if (settings.hudMirror) -1f else 1f },
    ) {
        if (settings.displayStyle == DisplayStyle.ANALOG) {
            AnalogDial(
                valueInUnit = if (state.hasPermission) speedInUnit(state.speedMps, settings.unit) else 0f,
                unit = settings.unit,
                lowInUnit = settings.unit.fromKmh(settings.colorLowKmh),
                midInUnit = settings.unit.fromKmh(settings.colorMidKmh),
                decimals = settings.decimals,
                needleColor = speedColor,
                onSurface = onBg,
                modifier = Modifier.fillMaxWidth(0.92f).aspectRatio(1f),
            )
        } else {
            Text(
                text = if (state.hasPermission) formatSpeed(state.speedMps, settings.unit, settings.decimals) else "—",
                color = speedColor,
                fontSize = fontSize,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
            )
            Text(settings.unit.speedLabel, color = onBg.copy(alpha = 0.6f), fontSize = 26.sp)
        }
        if (state.overLimit) {
            Text("OVER LIMIT", color = Color(0xFFFF1744), fontWeight = FontWeight.Bold, fontSize = 18.sp)
        }
    }
}

@Composable
private fun OdoText(state: SpeedUiState, settings: SettingsState, onBg: Color) {
    Text(
        "ODO  ${formatDistance(state.lifetimeM, settings.unit)}",
        color = onBg.copy(alpha = 0.5f),
        fontSize = 13.sp,
        fontFamily = FontFamily.Monospace,
        modifier = Modifier.padding(top = 4.dp),
    )
}

@Composable
private fun GraphBlock(state: SpeedUiState, settings: SettingsState) {
    if (!settings.showGraph) return
    SpeedGraph(
        history = state.history,
        unit = settings.unit,
        lineColor = MaterialTheme.colorScheme.primary,
        modifier = Modifier.fillMaxWidth().height(72.dp).padding(top = 8.dp),
    )
}

@Composable
private fun ControlsRow(state: SpeedUiState, onToggleRunning: () -> Unit, onResetTrip: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        OutlinedButton(onClick = onToggleRunning, modifier = Modifier.weight(1f)) {
            Text(if (state.running) "Pause" else "Start")
        }
        OutlinedButton(onClick = onResetTrip, modifier = Modifier.weight(1f)) {
            Text("Reset trip")
        }
    }
}

@Composable
private fun MetricsBlock(state: SpeedUiState, settings: SettingsState, onBg: Color) {
    val avgMps = if (state.movingMs > 0) (state.distanceM / (state.movingMs / 1000.0)).toFloat() else 0f
    val cells = buildList {
        if (settings.showAvg) add("AVG" to "${formatSpeed(avgMps, settings.unit, settings.decimals)} ${settings.unit.speedLabel}")
        if (settings.showMax) add("MAX" to "${formatSpeed(state.maxMps, settings.unit, settings.decimals)} ${settings.unit.speedLabel}")
        if (settings.showDistance) add("TRIP" to formatDistance(state.distanceM, settings.unit))
        if (settings.showTime) add("TIME" to formatDuration(state.totalMs))
        if (settings.showAltitude) add("ALT" to formatAltitude(state.altitudeM))
        if (settings.showHeading) add("HDG" to headingLabel(state.bearingDeg))
    }
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        cells.chunked(3).forEach { row ->
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                row.forEach { (label, value) ->
                    Column(modifier = Modifier.weight(1f)) {
                        Text(label, color = onBg.copy(alpha = 0.5f), fontSize = 12.sp)
                        Text(value, color = onBg, fontSize = 18.sp, fontFamily = FontFamily.Monospace)
                    }
                }
                repeat(3 - row.size) { Spacer(Modifier.weight(1f)) }
            }
        }
    }
}

@Composable
private fun GpsDot(status: GpsStatus) {
    val color = when (status) {
        GpsStatus.OK -> Color(0xFF4CAF50)
        GpsStatus.WEAK -> Color(0xFFFFC107)
        GpsStatus.WAITING -> Color(0xFF9E9E9E)
    }
    Box(modifier = Modifier.size(12.dp).clip(CircleShape).background(color))
}

private fun statusText(state: SpeedUiState): String = when {
    !state.hasPermission -> "Location permission needed"
    state.status == GpsStatus.WAITING -> "Acquiring GPS…"
    state.status == GpsStatus.WEAK -> "GPS weak (±${state.accuracyM.toInt()} m)"
    else -> "GPS OK (±${state.accuracyM.toInt()} m)"
}

private fun speedColor(mps: Float, settings: SettingsState, default: Color): Color {
    if (!settings.colorBySpeed) return default
    val kmh = mps * 3.6f
    return when {
        kmh < settings.colorLowKmh -> Color(0xFF4CAF50)
        kmh < settings.colorMidKmh -> Color(0xFFFFC107)
        else -> Color(0xFFFF5252)
    }
}
