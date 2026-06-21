package com.franz.speedometer.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.franz.speedometer.data.SettingsState
import com.franz.speedometer.data.SpeedUnit
import com.franz.speedometer.data.ThemeMode
import kotlin.math.roundToInt

@Composable
fun SettingsScreen(
    settings: SettingsState,
    onChange: (SettingsState) -> Unit,
    onResetLifetime: () -> Unit,
    onBack: () -> Unit,
) {
    val scroll = rememberScrollState()
    Column(
        modifier = Modifier.fillMaxSize().systemBarsPadding().padding(16.dp).verticalScroll(scroll),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = onBack) { Text("‹ Back") }
            Text("Settings", fontSize = 22.sp, fontWeight = FontWeight.Bold)
        }

        Section("Display")
        ChoiceRow("Units", SpeedUnit.entries.map { it.speedLabel to it }, settings.unit) {
            onChange(settings.copy(unit = it))
        }
        ChoiceRow("Decimals", listOf("0" to 0, "0.1" to 1), settings.decimals) {
            onChange(settings.copy(decimals = it))
        }
        ChoiceRow(
            "Theme",
            listOf("Auto" to ThemeMode.AUTO, "Light" to ThemeMode.LIGHT, "Night" to ThemeMode.NIGHT),
            settings.theme,
        ) { onChange(settings.copy(theme = it)) }
        SwitchRow("Color by speed", settings.colorBySpeed) { onChange(settings.copy(colorBySpeed = it)) }
        SwitchRow("HUD mirror mode", settings.hudMirror) { onChange(settings.copy(hudMirror = it)) }
        SwitchRow("Keep screen on", settings.keepScreenOn) { onChange(settings.copy(keepScreenOn = it)) }
        SwitchRow("Show live graph", settings.showGraph) { onChange(settings.copy(showGraph = it)) }

        Section("Secondary metrics")
        SwitchRow("Average speed", settings.showAvg) { onChange(settings.copy(showAvg = it)) }
        SwitchRow("Max speed", settings.showMax) { onChange(settings.copy(showMax = it)) }
        SwitchRow("Trip distance", settings.showDistance) { onChange(settings.copy(showDistance = it)) }
        SwitchRow("Trip time", settings.showTime) { onChange(settings.copy(showTime = it)) }
        SwitchRow("Altitude", settings.showAltitude) { onChange(settings.copy(showAltitude = it)) }
        SwitchRow("Heading", settings.showHeading) { onChange(settings.copy(showHeading = it)) }

        Section("Alerts")
        SwitchRow("Speed alert", settings.alertEnabled) { onChange(settings.copy(alertEnabled = it)) }
        StepperRow(
            "Threshold",
            "${settings.alertThreshold.roundToInt()} ${settings.unit.speedLabel}",
            onMinus = { onChange(settings.copy(alertThreshold = (settings.alertThreshold - 5).coerceAtLeast(5f))) },
            onPlus = { onChange(settings.copy(alertThreshold = settings.alertThreshold + 5)) },
        )
        SwitchRow("Flash on alert", settings.alertFlash) { onChange(settings.copy(alertFlash = it)) }
        SwitchRow("Beep on alert", settings.alertBeep) { onChange(settings.copy(alertBeep = it)) }
        SwitchRow("Vibrate on alert", settings.alertVibrate) { onChange(settings.copy(alertVibrate = it)) }

        Section("Measurement")
        ChoiceRow("GPS update rate", listOf("1 Hz" to 1, "2 Hz" to 2), settings.updateRateHz) {
            onChange(settings.copy(updateRateHz = it))
        }
        SwitchRow("Suppress low-accuracy readings", settings.suppressLowAccuracy) {
            onChange(settings.copy(suppressLowAccuracy = it))
        }
        StepperRow(
            "Stationary cutoff",
            "${settings.stationaryCutoffKmh.roundToInt()} km/h",
            onMinus = { onChange(settings.copy(stationaryCutoffKmh = (settings.stationaryCutoffKmh - 1).coerceAtLeast(0f))) },
            onPlus = { onChange(settings.copy(stationaryCutoffKmh = (settings.stationaryCutoffKmh + 1).coerceAtMost(15f))) },
        )

        Section("Data")
        OutlinedButton(onClick = onResetLifetime, modifier = Modifier.padding(top = 4.dp)) {
            Text("Reset lifetime odometer")
        }
        Spacer(Modifier.size(24.dp))
    }
}

@Composable
private fun Section(title: String) {
    HorizontalDivider(modifier = Modifier.padding(top = 16.dp, bottom = 4.dp))
    Text(
        title.uppercase(),
        color = MaterialTheme.colorScheme.primary,
        fontSize = 13.sp,
        fontWeight = FontWeight.Bold,
    )
}

@Composable
private fun SwitchRow(label: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, modifier = Modifier.weight(1f), fontSize = 16.sp)
        Switch(checked = checked, onCheckedChange = onChange)
    }
}

@Composable
private fun <T> ChoiceRow(label: String, options: List<Pair<String, T>>, selected: T, onSelect: (T) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, modifier = Modifier.weight(1f), fontSize = 16.sp)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            options.forEach { (text, value) ->
                FilterChip(
                    selected = value == selected,
                    onClick = { onSelect(value) },
                    label = { Text(text) },
                )
            }
        }
    }
}

@Composable
private fun StepperRow(label: String, value: String, onMinus: () -> Unit, onPlus: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, modifier = Modifier.weight(1f), fontSize = 16.sp)
        OutlinedButton(onClick = onMinus) { Text("–") }
        Text(value, modifier = Modifier.padding(horizontal = 12.dp), fontSize = 16.sp)
        OutlinedButton(onClick = onPlus) { Text("+") }
    }
}
